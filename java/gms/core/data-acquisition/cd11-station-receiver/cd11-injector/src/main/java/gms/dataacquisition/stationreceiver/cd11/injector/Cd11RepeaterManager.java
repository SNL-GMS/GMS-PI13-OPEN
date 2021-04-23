package gms.dataacquisition.stationreceiver.cd11.injector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.injector.configuration.RepeaterConfig;
import gms.dataacquisition.stationreceiver.cd11.injector.configuration.StationRepeaterConfig;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.flowables.GroupedFlowable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Cd11RepeaterManager {
  private static final String SAMPLE_LPAZ_RSDF = "{\"id\":\"3d2b87eb-193d-41a3-8fb0-b31c7417b920" +
      "\",\"rawPayload\":\"AAAABQAAB" +
      "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\",\"metadata\":{\"payloadFormat\":\"CD11\",\"authenticationStatus\":\"NOT_YET_AUTHENTICATED\",\"channelNames\":[\"LPAZ.LPAZ1.SHZ\",\"LPAZ.LPAZB.BHZ\",\"LPAZ.LPAZB.BHN\",\"LPAZ.LPAZB.BHE\"],\"receptionTime\":\"2019-11-27T20:05:52.416351Z\",\"stationName\":\"LPAZ\",\"waveformSummaries\":{\"LPAZ.LPAZB.BHN\":{\"channelName\":\"LPAZ.LPAZB.BHN\",\"startTime\":\"2019-11-27T20:03:30Z\",\"endTime\":\"2019-11-27T20:03:39.975Z\"},\"LPAZ.LPAZ1.SHZ\":{\"channelName\":\"LPAZ.LPAZ1.SHZ\",\"startTime\":\"2019-11-27T20:03:30Z\",\"endTime\":\"2019-11-27T20:03:39.975Z\"},\"LPAZ.LPAZB.BHZ\":{\"channelName\":\"LPAZ.LPAZB.BHZ\",\"startTime\":\"2019-11-27T20:03:30Z\",\"endTime\":\"2019-11-27T20:03:39.975Z\"},\"LPAZ.LPAZB.BHE\":{\"channelName\":\"LPAZ.LPAZB.BHE\",\"startTime\":\"2019-11-27T20:03:30Z\",\"endTime\":\"2019-11-27T20:03:39.975Z\"}},\"payloadStartTime\":\"2019-11-27T20:03:30Z\",\"payloadEndTime\":\"2019-11-27T20:03:40Z\"}}";
  private static final int RSDF_PUBLISHER_DELAY_MS = 30000;

  //What the manager should use as its group ID for the Kafka broker management/offset
  private static final String KAFKA_GROUP_PREFIX = "soh.rsdf.repeatermanager.";

  private static final Logger logger = LoggerFactory.getLogger(Cd11RepeaterManager.class);
  private static final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();

  private final RepeaterConfig config;
  private final Vertx vertx;
  private final Cd11FrameClientele frameClientele;
  private final Map<String, Future<Cd11FrameClient>> clientsByStation;
  private final CompositeDisposable stationDatamanDisposables;

  private Disposable connectionDisposable;

  Cd11RepeaterManager(RepeaterConfig config, Vertx vertx) {
    this.vertx = vertx;
    this.config = config;
    this.frameClientele = new Cd11FrameClientele(config.getFrameCreator(),
        config.getFrameDestination(), vertx);
    this.clientsByStation = new ConcurrentHashMap<>();
    this.stationDatamanDisposables = new CompositeDisposable();
  }

  /**
   * RepeaterManager handles connecting to the RSDF producer Kafka Broker, and recognizing new
   * RSDF station sources.
   * When a new station is discovered, the manager uses connman to determine the address to send
   * this data to, and then
   * creates a verticle that handles data for just that station in the future.
   */
  void start() {
    if (Boolean.TRUE.equals(config.getRunDebugKafkaPublisher())) {
      deployDebugKafkaProducer();
    }

    String consumerID = config.getKafkaConsumerID();

    Map<String, String> consumerConfig = new HashMap<>();
    consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaRepeaterServers());
    consumerConfig
        .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumerConfig
        .put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_GROUP_PREFIX + consumerID);
    consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

    logger.info("Establishing Kafka connection...");
    KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, consumerConfig);

    logger.info("Initializing RSDF flowable...");
    Flowable<RawStationDataFrame> baseRsdfFlowable = consumer.toFlowable()
        .map(record -> objectMapper.readValue(record.value(), RawStationDataFrame.class))
        .replay()
        .autoConnect();

    Flowable<GroupedFlowable<String, RawStationDataFrame>> filteredFlowable =
        baseRsdfFlowable.groupBy(rsdf -> rsdf.getMetadata().getStationName());

    logger.info("Initializing connection flowable...");
    connectionDisposable = baseRsdfFlowable
        .map(rsdf -> rsdf.getMetadata().getStationName())
        .distinct()
        .filter(stationName -> !clientsByStation.containsKey(stationName))
        .observeOn(Schedulers.io())
        .retryWhen(errors ->
            errors.zipWith(Flowable.range(1, 10),
                (error, retryCount) -> retryCount)
                .flatMap(retryCount -> Flowable.timer((long) Math.pow(2.0, retryCount),
                    TimeUnit.SECONDS)))
        .subscribe(stationName -> clientsByStation
                .computeIfAbsent(stationName, this::establishDatamanConnection)
                .onSuccess(client -> {
                  logger.info("Dataman connection established, subscribing for {} RSDFs...",
                      stationName);
                  client.setOnClose(v -> {
                    logger.info("Client socket closed, removing {} from known clients...", stationName);
                    clientsByStation.remove(stationName);
                  });
                  stationDatamanDisposables.add(filteredFlowable
                      .filter(groupedFlowable -> groupedFlowable.getKey().equals(stationName))
                      .flatMap(flowable -> flowable)
                      .throttleLast(10, TimeUnit.SECONDS)
                      .subscribeOn(Schedulers.io())
                      .subscribe(innerRsdf -> {
                            logger.info("Received and forwarding rsdf {} for {}[{}:{}]",
                                innerRsdf.getId(), innerRsdf.getMetadata().getStationName(),
                                innerRsdf.getMetadata().getPayloadStartTime(),
                                innerRsdf.getMetadata().getPayloadEndTime());
                            client.sendData(innerRsdf.getRawPayload());
                          },
                          throwable -> logger
                              .error("Error Sending to Dataman for station {}", stationName),
                          () -> logger
                              .info("Dataman RSDF subscription has completed for station {}",
                                  stationName)));
                }).onFailure(Exceptions::propagate),
            throwable -> logger.error("Error setting up connections", throwable),
            () -> {
              logger.info("Station subscription has completed");
              connectionDisposable.dispose();
            });

    consumer.subscribe(config.getKafkaConsumerTopic());
  }

  public void close() {
    if (!connectionDisposable.isDisposed()) {
      connectionDisposable.dispose();
    }

    if (!stationDatamanDisposables.isDisposed()) {
      stationDatamanDisposables.dispose();
    }
  }

  private void deployDebugKafkaProducer() {
    Map<String, String> producerConfig = new HashMap<>();
    producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaRepeaterServers());
    producerConfig
        .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class.getName());
    producerConfig.put(ProducerConfig.ACKS_CONFIG, "1");

    KafkaProducer<String, String> producer = KafkaProducer.create(vertx, producerConfig);

    KafkaProducerRecord<String, String> producerRecord =
        KafkaProducerRecord.create(config.getKafkaConsumerTopic(), "LPAZ", SAMPLE_LPAZ_RSDF);

    vertx.setPeriodic(RSDF_PUBLISHER_DELAY_MS,
        handler -> producer.write(producerRecord, res -> {
          if (res.succeeded()) {
            logger.info("Publishing LPAZ message to Kafka broker success!");
          } else {
            logger.error(res.cause().getMessage());
          }
        })
    );
  }

  private Future<Cd11FrameClient> establishDatamanConnection(String stationName) {
    logger.info("Establishing Dataman connection for station {}...", stationName);

    Future<Cd11FrameClient> connectionResponseFuture = frameClientele
        .establishCd11Connection(config.getConnManAddress(), config.getConnManPort(), stationName);

    return connectionResponseFuture
        .flatMap(Cd11FrameClient::sendConnectionRequest)
        .flatMap(responseFrame -> convertToStationRepeaterConfig(stationName, responseFrame))
        .flatMap(stationRepeaterConfig -> frameClientele.establishCd11Connection(
            InetAddresses.toAddrString(stationRepeaterConfig.getConsumerAddress()),
            stationRepeaterConfig.getConsumerPort(), stationRepeaterConfig.getStationName()));
  }

  private Future<StationRepeaterConfig> convertToStationRepeaterConfig(String stationId,
      Cd11ConnectionResponseFrame responseFrame) {
    String ip = InetAddresses.fromInteger(responseFrame.ipAddress).toString();
    if (ip.charAt(0) == '/') {
      ip = ip.substring(1);
    }
    int port = responseFrame.port;
    logger.info("{} injector has received {}:{} as the destination address", stationId, ip, port);

    StationRepeaterConfig repeaterConfig = StationRepeaterConfig.builder()
        .setConsumerAddress(InetAddresses.forString(ip))
        .setConsumerPort(port)
        .setStationName(stationId)
        .setKafkaConsumerID(config.getKafkaConsumerID())
        .setRepeaterTimeoutSeconds(config.getRepeaterTimeoutSeconds())
        .setKafkaConsumerTopic(config.getKafkaConsumerTopic())
        .setKafkaRepeaterServers(config.getKafkaRepeaterServers())
        .setFrameCreator(config.getFrameCreator())
        .setFrameDestination(config.getFrameDestination())
        .build();

    return Future.succeededFuture(repeaterConfig);
  }
}
