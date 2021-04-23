package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.shared.utilities.javautilities.gracefulthread.GracefulThread;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Cd11DataConsumerConnectionExpiredThread extends GracefulThread {

  private static Logger logger =
      LoggerFactory.getLogger(Cd11DataConsumerConnectionExpiredThread.class);

  private static final long MIN_SLEEP_TIME_MS = 1000;

  private final BlockingQueue<Message> eventQueue;
  private final Cd11Socket cd11Socket;
  private final long connectionExpiredTimeLimitSec;

  public Cd11DataConsumerConnectionExpiredThread(
      String threadName, BlockingQueue<Message> eventQueue,
      Cd11Socket cd11Socket, long connectionExpiredTimeLimitSec) {
    super(threadName, true, false);

    this.eventQueue = eventQueue;
    this.cd11Socket = cd11Socket;
    this.connectionExpiredTimeLimitSec = connectionExpiredTimeLimitSec;
  }

  @Override
  protected void onStart() {
    try {
      while (this.keepThreadRunning()) {
        // Check whether the connection has expired.
        long seconds = cd11Socket.secondsSinceLastContact();
        if (seconds > connectionExpiredTimeLimitSec) {
          logger.warn("Sending shutdown event due to timeout: {} > {} configured seconds",
              seconds, connectionExpiredTimeLimitSec);
          // Generate an event.
          eventQueue.put(new Message(MessageType.SHUTDOWN));

          // Shut down the thread.
          break;
        }

        long potentialSleepTime = (connectionExpiredTimeLimitSec - seconds) * 1000;
        final long sleepTime = Math.max(potentialSleepTime, MIN_SLEEP_TIME_MS);
        logger.info("connection expired thread; connection did not expire, sleeping {} ms",
                sleepTime);
        Thread.sleep(sleepTime);
      }
    } catch (InterruptedException e) {
      //TODO: We need to handle this exception more gracefully. This likely requires a refactor of our threading model.
      logger.debug(String.format("InterruptedException thrown in thread %s, closing thread.",
          this.getThreadName()), e);
      Thread.currentThread().interrupt();
    }
  }
}
