package gms.dataacquisition.stationreceiver.cd11.injector;

import gms.dataacquisition.stationreceiver.cd11.common.Cd11FrameFactory;
import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame.FrameType;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import io.vertx.reactivex.ext.unit.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that injects the Raw Station Data Frame stream, converting it to CD1.1 frames in the
 * current timeframe and metering them to the same rate they were initially received
 */
class InjectionDataHandler {

  // limit on ready to send queue; exists for bounding the memory footprint
  private static final int READY_TO_SEND_QUEUE_MAX_SIZE = 100;
  private final Logger logger;
  private boolean readyToSendQueuePrimed = false;

  // Queue on which processed data (time-corrected cd11 frame with possible modified seqno) is
  // queued
  private final ConcurrentLinkedQueue<ReceivedFrame<Cd11DataFrame>> readyToSendQueue =
      new ConcurrentLinkedQueue<>();

  private final Cd11FrameClient connectionManager;
  private final Cd11FrameFactory frameFactory;

  // Subscription holder
  private Disposable subscription;

  private final Vertx vertx;
  private long sendDataTimerId;

  private final Flowable<RawStationDataFrame> rsdfDataStream; // The data stream of all injection
  // frames
  private final Instant referenceTime;           // Data reference time
  private final Instant startTime;               // Time in current context injection will start
  private Duration referenceTimeDelta;     // Difference between start time and data's reference
  // time
  private final long sequenceNumberOffset;       // What to add to the existing data frame's seq
  // nos if looping data
  private final boolean meterDataStream;         // Whether to meter the injection at the same
  // rate data originally arrived
  private Handler<AsyncResult<Void>> onComplete;        // Callback on completion of injection
  private InjectionState injectionState = InjectionState.NOT_STARTED;

  InjectionDataHandler(Flowable<RawStationDataFrame> dataStream, Instant startTime,
      Instant referenceTime,
      long sequenceNumberOffset, boolean meterDataStream,
      Cd11FrameClient connectionManager, Vertx vertx) {
    this.rsdfDataStream = dataStream;
    this.startTime = startTime;
    this.referenceTime = referenceTime;
    this.connectionManager = connectionManager;
    this.frameFactory = connectionManager.getCd11FrameFactory();
    this.meterDataStream = meterDataStream;
    this.vertx = vertx;
    this.sequenceNumberOffset = sequenceNumberOffset;
    // add station name to the log
    logger = LoggerFactory
        .getLogger(String
            .format("%s|%s", InjectionDataHandler.class, frameFactory.getResponderName()));
  }

  /**
   * Starts the injection setup; injection starts on this.startTime
   */
  void start(Handler<AsyncResult<Void>> onComplete) {
    injectionState = InjectionState.IN_PROGRESS;
    this.onComplete = onComplete;
    calculateTimeDelta();
    subscription = prepareRSDFSubscription(rsdfDataStream);
    sendDataTimerId = vertx.setTimer(1, this::sendData);
  }

  /**
   * Stops the handler
   */
  void stop() {
    vertx.cancelTimer(sendDataTimerId);
  }

  void close() {
    stop();
    if (!subscription.isDisposed()) {
      subscription.dispose();
    }
  }

  private void calculateTimeDelta() {
    referenceTimeDelta = Duration.between(referenceTime, startTime);
  }

  private Instant adjustToReferenceTime(Instant time) {
    return time.plus(referenceTimeDelta);
  }

  /**
   * Helper that adjusts the times and sequence numbers in the Cd11DataFrame
   *
   * @param frame to be adjusted
   * @return the newly adjusted frame
   */
  private Cd11DataFrame adjustDataToReferenceTime(Cd11DataFrame frame) {

    ArrayList<Cd11ChannelSubframe> newSubframes = new ArrayList<>();

    for (Cd11ChannelSubframe subFrame : frame.channelSubframes) {
      Instant timeStamp = adjustToReferenceTime(subFrame.timeStamp);
      newSubframes.add(FrameUtilities.cloneAndModifyChannelSubframe(subFrame, timeStamp));
    }

    Cd11ChannelSubframe[] subframeArray = newSubframes.toArray(new Cd11ChannelSubframe[0]);

    Cd11DataFrame newFrame = null;
    try {
      long sequenceNumber = frame.getFrameHeader().sequenceNumber + sequenceNumberOffset;
      newFrame = frameFactory.createCd11DataFrame(subframeArray, sequenceNumber);
    } catch (IllegalArgumentException ioe) {
      logger.error("Could not create CD11 Frame", ioe);
    }

    return newFrame;
  }

  /**
   * Prepare the subscription-based processing: create the CD11 frame from the RSDF, modifying it as
   * needed, typically changing time and sequence number
   */

  private Disposable prepareRSDFSubscription(Flowable<RawStationDataFrame> rsdfFlowable) {
    return rsdfFlowable
        .concatMap(rsdf -> {
          // And flow it downstream, delaying it until close to time of injection if the
          // send queue is primed
          // This won't stop the queue from growing but will at least slow its growth
          // to a rate where the consumer might keep up
          readyToSendQueuePrimed =
              readyToSendQueue.size() > InjectionDataHandler.READY_TO_SEND_QUEUE_MAX_SIZE;

          if (readyToSendQueuePrimed) {
            Instant delayTime = adjustToReferenceTime(rsdf.getMetadata().getReceptionTime())
                .minusSeconds(15);
            long delayMs = Duration.between(Instant.now(), delayTime).toMillis();
            if (delayMs > 100) {
              logger.info("Send queue primed, delaying data preparation stream by ms: {}", delayMs);
              return Flowable.just(rsdf).delay(delayMs, TimeUnit.MILLISECONDS);
            } else {
              logger.info("Send queue primed but data still needs to be prepared");
              return Flowable.just(rsdf);
            }
          } else {
            return Flowable.just(rsdf);
          }
        }).observeOn(Schedulers.io())
        .subscribe(rsdFrame -> {
              Cd11ByteFrame byteFrame = new Cd11ByteFrame(
                  new DataInputStream(new ByteArrayInputStream(rsdFrame.getRawPayload()))
                  , () -> true);
              Cd11Frame frame = frameFactory.createCd11Frame(byteFrame);
              if (frame.frameType == FrameType.DATA) {

                Instant newReceptionTime =
                    adjustToReferenceTime(rsdFrame.getMetadata().getReceptionTime());
                logger.info("Adjusting data element with reception time: {} to {}",
                    rsdFrame.getMetadata().getReceptionTime(),
                    newReceptionTime);
                Cd11DataFrame adjustedFrame =
                    adjustDataToReferenceTime((Cd11DataFrame) frame);

                readyToSendQueue.offer(new ReceivedFrame<>(newReceptionTime,
                    adjustedFrame));

              } else {
                logger.error("Found non-data data frame in stream, frame type: {}",
                    frame.frameType);
              }
            }, e -> logger.error("Error preparing injection data", e), // error handler
            // for subscription
            () -> { // completion handler for subscription
              injectionState = InjectionState.ALL_DATA_PROCESSED;
              logger
                  .info("Station injector has prepared all data for sending.  Items still" +
                          " to send: {} ",
                      readyToSendQueue.size());
            });
  }

  /**
   * Timer-invoked callback to send the data when it's time to send it
   *
   * @param timerId timerId for this timer as provided by vert.x
   */
  private void sendData(@SuppressWarnings("squid:S1172") long timerId) {
    try {
      Instant nowPlusTimeBuffer = Instant.now().plusMillis(100);
      ReceivedFrame<Cd11DataFrame> receivedFrame = readyToSendQueue.peek();

      // Send all frames whose reception time < now-ish
      while (receivedFrame != null
          && (!meterDataStream || nowPlusTimeBuffer.compareTo(receivedFrame.receptionTime) >= 0)) {

        // Remove the frame and sent it
        receivedFrame = readyToSendQueue.poll();
        logger.info("Sending data for time: {} at time: {}", receivedFrame.receptionTime,
            nowPlusTimeBuffer);
        connectionManager.sendData(receivedFrame.frame)
            .onComplete(res -> {
              if (res.failed()) {// && result.cause() instanceof ClosedChannelException) {
                logger.error("On complete with error hit: Sending frame failed", res.cause());
                onComplete.handle(Future.failedFuture(res.cause()));
              }
            });

        // Check next frame
        receivedFrame = readyToSendQueue.peek();
      }

      if (receivedFrame == null) { // Nothing left on the queue

        if (injectionState == InjectionState.ALL_DATA_PROCESSED) { // All done
          logger.info("All data is processed for this station.");
          injectionState = InjectionState.ALL_DATA_SENT;
          subscription.dispose();
          onComplete.handle(Future.succeededFuture());
        } else { // there is still work to do
          sendDataTimerId = vertx.setTimer(100, this::sendData);
        }
      } else {
        Duration callback = Duration.between(Instant.now(), receivedFrame.receptionTime);
        // It's possible that looping through the ready queue and sending has taken >
        // 100 ms
        // and so there's still items to send. If so, callbackMs < 0 so reinvoke invoke
        // immediately
        long callbackMs = callback.toMillis();
        logger.info("Next send data scheduled for: {} - {}", receivedFrame.receptionTime,
            callback);
        sendDataTimerId = vertx.setTimer(callbackMs >= 0 ? callbackMs : 1, this::sendData);
      }
    } catch (Exception e) {
      logger.error("Error sending data", e);
    }
  }

  enum InjectionState {
    IN_PROGRESS, ALL_DATA_PROCESSED, ALL_DATA_SENT, NOT_STARTED
  }

}
