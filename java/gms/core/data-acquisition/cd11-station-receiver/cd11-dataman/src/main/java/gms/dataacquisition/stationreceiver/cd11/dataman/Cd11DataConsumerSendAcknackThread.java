package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.shared.utilities.javautilities.gracefulthread.GracefulThread;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Cd11DataConsumerSendAcknackThread extends GracefulThread {

  private static Logger logger = LoggerFactory.getLogger(Cd11DataConsumerSendAcknackThread.class);

  private final BlockingQueue<Message> eventQueue;
  private final Cd11Socket cd11Socket;

  public Cd11DataConsumerSendAcknackThread(
      String threadName,
      BlockingQueue<Message> eventQueue, Cd11Socket cd11Socket) {
    super(threadName, true, false);

    this.eventQueue = eventQueue;
    this.cd11Socket = cd11Socket;
  }

  @Override
  protected void onStart() {
    try {
      while (this.keepThreadRunning()) {
        long seconds = cd11Socket.secondsSinceLastAcknackSent();
        if (seconds > 55) {
          // Generate an event, if one does not already exist in the queue.
          logger.info("Generating send acknack event");
          Message msg = new Message(MessageType.SEND_ACKNACK);
          if (!eventQueue.contains(msg)) {
            eventQueue.put(msg);
          }

          Thread.sleep(56000);
        } else {
          Thread.sleep((56 - seconds) * 1000);
        }
      }
    } catch (InterruptedException e) {
      //TODO: We need to handle this exception more gracefully. This likely requires a refactor of our threading model.
      logger.debug("InterruptedException thrown in thread {}, closing thread. Exception: {}",
          this.getThreadName(), e);
      Thread.currentThread().interrupt();

    } catch (Exception e) {
      logger.error(
          "Unexpected exception thrown in thread {}, and thread must now close. Exception: {}",
          this.getThreadName(), e);
    }
  }
}
