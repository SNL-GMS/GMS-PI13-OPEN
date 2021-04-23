package gms.dataacquisition.stationreceiver.cd11.dataman;

import static com.google.common.base.Preconditions.checkState;

import gms.shared.utilities.javautilities.gracefulthread.GracefulThread;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Cd11DataConsumerPersistGapStateThread extends GracefulThread {

  private static Logger logger = LoggerFactory
      .getLogger(Cd11DataConsumerPersistGapStateThread.class);

  private final BlockingQueue<Message> eventQueue;
  private final long storeGapStateTimeoutMs;

  public Cd11DataConsumerPersistGapStateThread(
      String threadName, BlockingQueue<Message> eventQueue,
      long storeGapStateTimeoutInMinutes) {
    super(threadName, true, false);

    checkState(storeGapStateTimeoutInMinutes > 0);

    this.eventQueue = eventQueue;
    this.storeGapStateTimeoutMs = storeGapStateTimeoutInMinutes * 60 * 1000;
  }

  @Override
  protected void onStart() {
    try {
      while (this.keepThreadRunning()) {
        Thread.sleep(storeGapStateTimeoutMs);
        logger.info("Putting persist gap state event onto queue");
        Message msg = new Message(MessageType.PERSIST_GAP_STATE);
        if (!eventQueue.contains(msg)) {
          eventQueue.put(msg);
        }
      }
    } catch (InterruptedException e) {
      //TODO: We need to handle this exception more gracefully. This likely requires a refactor of our threading model.
      logger.debug(String.format("InterruptedException thrown in thread %s, closing thread.",
          this.getThreadName()), e);
      Thread.currentThread().interrupt();
    }
  }
}
