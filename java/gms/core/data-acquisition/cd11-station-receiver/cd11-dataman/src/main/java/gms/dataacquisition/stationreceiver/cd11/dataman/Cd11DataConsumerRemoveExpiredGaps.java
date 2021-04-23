package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.shared.utilities.javautilities.gracefulthread.GracefulThread;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Cd11DataConsumerRemoveExpiredGaps extends GracefulThread {

  private static Logger logger = LoggerFactory.getLogger(Cd11DataConsumerRemoveExpiredGaps.class);

  private final BlockingQueue<Message> eventQueue;

  public Cd11DataConsumerRemoveExpiredGaps(String threadName, BlockingQueue<Message> eventQueue) {
    super(threadName, true, false);

    this.eventQueue = eventQueue;
  }

  @Override
  protected void onStart() {
    try {
      while (this.keepThreadRunning()) {
        Thread.sleep(60*60*1000L); // 1 hour
        logger.info("Putting remove expired gaps event onto queue");
        Message msg = new Message(MessageType.REMOVE_EXPIRED_GAPS);
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
