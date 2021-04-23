package gms.dataacquisition.stationreceiver.cd11.dataman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11FrameFactory;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11GapList;
import gms.dataacquisition.stationreceiver.cd11.common.FrameParsingDecoder;
import gms.dataacquisition.stationreceiver.cd11.common.FrameParsingUtility;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11AcknackFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11CommandResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame.FrameType;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11OptionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.PartialFrame;
import gms.dataacquisition.stationreceiver.cd11.dataman.logging.StructuredLoggingWrapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.utilities.kafka.KafkaConfiguration;
import gms.shared.utilities.kafka.reactor.ReactorKafkaFactory;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.Disposable.Composite;
import reactor.core.Disposables;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.kafka.sender.TransactionManager;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import reactor.netty.channel.AbortedException;

import static gms.dataacquisition.stationreceiver.cd11.parser.Cd11RawStationDataFrameUtility.parseAcquiredStationDataPacket;

public class Cd11StationConnectionHandler {

  public static final String STATION_NAME_KEY = "station";
  private final StructuredLoggingWrapper logger = StructuredLoggingWrapper.create(LoggerFactory.getLogger(Cd11StationConnectionHandler.class));

  private static final int ACKNACK_TIME_SECONDS = 55;
  private static final String MALFORMED_FRAME_TOPIC = "malformed-frames";
  private final DataFrameReceiverConfiguration receiverConfig;
  private final KafkaConfiguration kafkaConfiguration;

  private final TransactionManager transactionManager;

  private final Cd11FrameFactory cd11FrameFactory;

  private final Cd11GapList cd11GapList;

  private final String stationName;

  final Disposable.Composite disposableComposite;

  private final ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();

  //resources
  public final KafkaSender<String, String> recordSender;

  public Cd11StationConnectionHandler(String stationName,
      DataFrameReceiverConfiguration receiverConfig,
      KafkaConfiguration kafkaConfiguration,
      Cd11GapList cd11GapList) {
    this.stationName = stationName;
    this.receiverConfig = receiverConfig;
    this.kafkaConfiguration = kafkaConfiguration;
    this.cd11GapList = cd11GapList;

    logger.addKeyValueArgument(STATION_NAME_KEY, stationName);

    // create the frame factory
    cd11FrameFactory = Cd11FrameFactory.builderWithDefaults()
        .setResponderName(stationName).build();

    // create the kafka reactor factory
    ReactorKafkaFactory reactorKafkaFactory = new ReactorKafkaFactory(kafkaConfiguration);

    // Kafka sender and transaction manager for publishing Cd11DataFrames
    String senderName = String.format("%s-%s", kafkaConfiguration.getApplicationId(), stationName);
      recordSender = reactorKafkaFactory.makeSender(senderName);
    transactionManager = recordSender.transactionManager();

    disposableComposite = Disposables.composite();
    ResourceLeakDetector.setLevel(Level.DISABLED);
  }

  public static Cd11StationConnectionHandler create(String stationName,
      DataFrameReceiverConfiguration receiverConfig,
      KafkaConfiguration kafkaConfiguration,
      Cd11GapList cd11GapList) {
    return new Cd11StationConnectionHandler(stationName, receiverConfig, kafkaConfiguration,
        cd11GapList);
  }

  public Mono<Void> handleFrames(NettyInbound inbound, NettyOutbound outbound,
      boolean processFrames) {

    logger.info("Starting handling of connection!!!!");
    disposableComposite.add(sendAcknackPeriodically(outbound, cd11GapList)
        .doOnError(e -> logger.warn("ACKNACK Periodic Err:", e))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe());

    Flux<Cd11Frame> framesToHandle = parseIncomingFramesIntoCd11Frames(inbound)
        .map(frame -> checkForHaltingFrame(inbound, outbound, frame));

    if (processFrames) {
      ConnectableFlux<Cd11Frame> connectableFrameFluxByType = setupFrameHandlingByType(
          framesToHandle, outbound, cd11GapList, disposableComposite);

      disposableComposite.add(connectableFrameFluxByType.connect());

      return connectableFrameFluxByType
          .doOnSubscribe(f -> logger.debug("Connectable FrameFlux SUBSCRIBE!"))
          .then();
    } else {
      Mono<Cd11Frame> ignoredFlux = framesToHandle
          .map(frame -> {
            logger.debug("Frame processing is disabled for station {}. Dropping frame.",
                stationName);
            return frame;
          })
          .ignoreElements();
      return ignoredFlux.then();
    }
  }

  Cd11Frame checkForHaltingFrame(NettyInbound inbound, NettyOutbound outbound,
      Cd11Frame frame) {
    FrameType frameType = frame.frameType;
    if (frameType.equals(FrameType.ALERT) || frameType
        .equals(FrameType.CUSTOM_RESET_FRAME)) {
      if (frameType.equals(FrameType.CUSTOM_RESET_FRAME)) {
        Cd11GapListUtility.clearGapState(stationName);
        cd11GapList.resetGapsList();
      }
      alertAndShutDown(inbound, outbound);
    }
    return frame;
  }

  private void alertAndShutDown(NettyInbound inbound, NettyOutbound outbound) {
    sendCd11Frame(outbound, cd11FrameFactory.createCd11AlertFrame("Shutting down connection"))
        .doOnSuccess(s -> {
          shutDownResources();
          closeNettyConnections(inbound, outbound);
        })
        .doOnError(e -> logger.error("Alert and shutdown error:", e))
        .subscribe();
  }

  public void shutDownResources() {
    if (!disposableComposite.isDisposed()) {
      disposableComposite.dispose();
    }
  }

  private ConnectableFlux<Cd11Frame> setupFrameHandlingByType(
      Flux<Cd11Frame> framesToHandle, NettyOutbound outbound,
      Cd11GapList gapList, Composite disposableComposite) {

    var connectableFrameFluxByType = framesToHandle.publish();

    disposableComposite.add(handleAcknackFrames(connectableFrameFluxByType, gapList)
        .subscribe(cd11Frame -> {

              if (cd11Frame.getFrameType().equals(FrameType.MALFORMED_FRAME)) {
                handleMalformedFrame(cd11Frame.asFrameType(PartialFrame.class));
              } else {
                logger.debug("Received {} frame. Frame Set Acked: {}",
                    FrameType.ACKNACK,
                    cd11Frame.asFrameType(Cd11AcknackFrame.class).framesetAcked);
              }
            },
            error -> handleError(FrameType.ACKNACK, error)));

    disposableComposite.add(recordSender.sendTransactionally(
        handleDataFrames(connectableFrameFluxByType, gapList)
                .publishOn(transactionManager.scheduler())
                .window(1))
        .onErrorResume(e -> transactionManager.abort().then(Mono.error(e)))
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSubscribe(x -> transactionManager.commit())
        .subscribe(
            this::handleSenderResult,
            error -> handleError(FrameType.DATA, error)));

    disposableComposite.add(connectableFrameFluxByType
        .filter(frame -> frame.getFrameType().equals(FrameType.CD_ONE_ENCAPSULATION))
        .subscribe(frame -> logger
            .warn("Received {} frame, which is not yet supported!",
                FrameType.CD_ONE_ENCAPSULATION)));

    disposableComposite.add(connectableFrameFluxByType
        .filter(frame -> frame.getFrameType().equals(FrameType.COMMAND_REQUEST))
        .subscribe(frame -> logger.warn(
            "Received {} frame, which should never have been sent by the Data Provider! Ignoring this frame.",
            FrameType.COMMAND_REQUEST)));

    disposableComposite.add(handleCommandResponseFrames(connectableFrameFluxByType, gapList)
        .subscribe(frame -> logger.warn(
            "Received COMMAND_RESPONSE frame, recorded the sequence number to gap list"),
            error -> handleError(FrameType.COMMAND_RESPONSE, error)));

    disposableComposite.add(connectableFrameFluxByType
        .filter(frame -> frame.getFrameType().equals(FrameType.CONNECTION_REQUEST))
        .subscribe(frame -> logger.warn(
            "Received CONNECTION_REQUEST frame, which should never have been sent by the Data Provider! Ignoring this frame.")));

    disposableComposite.add(connectableFrameFluxByType
        .filter(frame -> frame.getFrameType().equals(FrameType.CONNECTION_RESPONSE))
        .subscribe(frame -> logger.warn(
            "Received CONNECTION_RESPONSE frame, which should never have been sent by the Data Provider! Ignoring this frame.")));

    disposableComposite.add(recordSender.sendTransactionally(
        handleMalformedFrames(connectableFrameFluxByType)
                .publishOn(transactionManager.scheduler())
                .window(1))
        .onErrorResume(e -> transactionManager.abort().then(Mono.error(e)))
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSubscribe(x -> transactionManager.commit())
        .subscribe(
            this::handleSenderResult,
            error -> handleError(FrameType.MALFORMED_FRAME, error)));

    disposableComposite.add(handleOptionRequestFrames(connectableFrameFluxByType, outbound)
        .subscribe(frame -> logger.info("Received OPTION_REQUEST frame. "
                + "Sent back OPTION_REQUEST frame as an OPTION RESPONSE frame without modification."),
            error -> handleError(FrameType.OPTION_REQUEST, error)));

    disposableComposite.add(connectableFrameFluxByType
        .filter(frame -> frame.getFrameType().equals(FrameType.OPTION_RESPONSE))
        .subscribe(frame -> logger.info("Received OPTION_RESPONSE frame, ignoring frame."),
            error -> handleError(FrameType.OPTION_RESPONSE, error)));

    return connectableFrameFluxByType;
  }

  private void handleSenderResult(Flux<SenderResult<String>> result) {
      result.map(r -> {
          RecordMetadata metadata = r.recordMetadata();
          if (metadata != null) {
            logger.debug(
                  "Sent kakfka message: to topic {} at offset {}",
                  metadata.topic(),
                  metadata.offset());
          }
          return Mono.empty();
      }).subscribe();
  }

  Flux<Cd11AcknackFrame> handleAcknackFrames(
      Flux<Cd11Frame> framesFlux,
      Cd11GapList gapList) {

    return framesFlux
        .filter(frame -> frame.getFrameType().equals(FrameType.ACKNACK))
        .map(frame -> {
          Cd11AcknackFrame acknackFrame = frame.asFrameType(Cd11AcknackFrame.class);

          //set framesetAcked in frame factory
          cd11FrameFactory.setFramesetAcked(acknackFrame.framesetAcked);

          //We only use the Acknack to check for a reset, see if highest seq num is below current low.
          gapList.checkForReset(acknackFrame);

          return acknackFrame;
        }).onErrorContinue((e, val) ->
            logger.error("Error parsing ACKNACK frame {}, frame dropped from transaction", val, e));
  }

  Flux<Cd11CommandResponseFrame> handleCommandResponseFrames(Flux<Cd11Frame> framesFlux,
      Cd11GapList gapList) {
    return framesFlux
        .filter(frame -> frame.getFrameType().equals(FrameType.COMMAND_RESPONSE))
        .map(frame -> {
          Cd11CommandResponseFrame responseFrame = frame
              .asFrameType(Cd11CommandResponseFrame.class);
          gapList.addSequenceNumber(responseFrame);
          return responseFrame;
        });
  }

  Flux<SenderRecord<String, String, String>> handleDataFrames(Flux<Cd11Frame> framesFlux,
      Cd11GapList gapList) {
    return framesFlux
        .filter(frame -> frame.getFrameType().equals(FrameType.DATA))
        .flatMap(frame -> {
          Cd11DataFrame dataFrame = frame.asFrameType(Cd11DataFrame.class);
          logger.info("Received data frame from {}", stationName);
          return handleDataFrame(dataFrame, gapList);
        }).onErrorContinue((e, val) ->
            logger.error("Error parsing data frame from {}, frame dropped from transaction", stationName, e));
  }

  private Flux<SenderRecord<String, String, String>> handleDataFrame(Cd11DataFrame dataFrame,
      Cd11GapList gapList) {

    final RawStationDataFrame frame = parseAcquiredStationDataPacket(receiverConfig, dataFrame,
        Instant.now(), stationName);

    gapList.addSequenceNumber(dataFrame);

    logger.info("Publishing DATA frame");
    // create reactor kafka sender record with the rsdf json string
    return writeJson(frame)
        .flatMapMany(rsdf -> Flux.just(SenderRecord
            .create(new ProducerRecord<>(kafkaConfiguration.getInputRsdfTopic(),
                frame.getId().toString(), rsdf), frame.getId().toString())));

  }

  Flux<SenderRecord<String, String, String>> handleMalformedFrames(
      Flux<Cd11Frame> framesFlux) {
    return framesFlux
        .filter(frame -> frame.getFrameType().equals(FrameType.MALFORMED_FRAME))
        .flatMap(frame -> {
          PartialFrame malformedFrame = frame.asFrameType(PartialFrame.class);
          return handleMalformedFrame(malformedFrame);
        });
  }

  private Mono<SenderRecord<String, String, String>> handleMalformedFrame(
      PartialFrame malformedFrame) {
    // create reactor kafka sender record with the malformed frame json string
    return Mono.just(malformedFrame.toString())
        .map(malformed -> SenderRecord.create(MALFORMED_FRAME_TOPIC,
            null, null, null, malformed, null));
  }

  Flux<Cd11OptionResponseFrame> handleOptionRequestFrames(Flux<Cd11Frame> framesFlux,
      NettyOutbound outbound) {
    return framesFlux
        .filter(frame -> frame.getFrameType().equals(FrameType.OPTION_REQUEST))
        .map(frame -> {
          Cd11OptionRequestFrame optionRequestFrame = frame
              .asFrameType(Cd11OptionRequestFrame.class);
          Cd11OptionResponseFrame responseFrame = cd11FrameFactory.createCd11OptionResponseFrame(
              optionRequestFrame.optionType, optionRequestFrame.optionRequest
          );

          sendCd11Frame(outbound, responseFrame)
              .doOnError(e -> logger.warn("Failed to send OPTION_RESPONSE", e))
              .subscribe();
          return responseFrame;
        });
  }

  void handleError(FrameType frameTypeHandled, Throwable error) {
    String errStr = String
        .format("Irrecoverable error encountered in %s frame handler", frameTypeHandled);
    logger.error(errStr, error);
  }

  private void closeNettyConnections(NettyInbound inbound, NettyOutbound outbound) {

    int timeout = 10;
    inbound.withConnection(d -> d.disposeNow(Duration.ofSeconds(timeout)));
    outbound.withConnection(d -> d.disposeNow(Duration.ofSeconds(timeout)));
  }

  private Mono<Void> sendCd11Frame(NettyOutbound outbound, Cd11Frame frame) {
    try {
      return outbound.sendByteArray(Mono.just(frame.toBytes())).then();
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

  public Mono<String> writeJson(Object obj) {
    try {
      String mapperString = mapper.writeValueAsString(obj);
      return Mono.just(mapperString);
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  private Flux<Cd11Frame> parseIncomingFramesIntoCd11Frames(NettyInbound inbound) {

    return inbound
        .withConnection(x ->
            x.addHandlerFirst(new FrameParsingDecoder()))
        .receive()
        .asByteBuffer()
        .map(FrameParsingUtility::parseByteBuffer)
        .map(FrameParsingUtility::createCd11Frame);

  }

  private Flux<Void> sendAcknackPeriodically(NettyOutbound outbound,
      Cd11GapList gapList) {

    logger.info("Starting periodic ACKNACK sending");

    //Periodically send acknacks
    return Flux.interval(Duration.ofSeconds(ACKNACK_TIME_SECONDS), Duration.ofSeconds(ACKNACK_TIME_SECONDS))
        .flatMap(
            i -> {
              logger.debug("sending {}th acknack with GapList - min: {}, max: {}", i,
                  gapList.getLowestSequenceNumber(), gapList.getHighestSequenceNumber());

              Cd11AcknackFrame acknackFrame =
                  cd11FrameFactory.createCd11AcknackFrame(gapList.getLowestSequenceNumber(),
                      gapList.getHighestSequenceNumber(), gapList.getGaps());

              try {
                return sendCd11Frame(outbound, acknackFrame);
              } catch (AbortedException e){
                logger.warn("Issue sending the acknack frame", e);
              }
              return Mono.empty();
            });
  }
}
