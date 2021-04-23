package gms.dataacquisition.stationreceiver.cd11.dataman;

import gms.dataacquisition.stationreceiver.cd11.common.Cd11Socket;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.shared.utilities.javautilities.gracefulthread.GracefulThread;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Cd11DataConsumerNewFrameReceivedThread extends GracefulThread {

  private static Logger logger = LoggerFactory
      .getLogger(Cd11DataConsumerNewFrameReceivedThread.class);

  private final Cd11Socket cd11Socket;
  private final BlockingQueue<Message> eventQueue;

  public Cd11DataConsumerNewFrameReceivedThread(
      String threadName, BlockingQueue<Message> eventQueue, Cd11Socket cd11Socket) {
    super(threadName, true, false);

    this.eventQueue = eventQueue;
    this.cd11Socket = cd11Socket;
  }

  @Override
  protected void onStart() throws Exception {
    try {
      listenToCd11Socket();
    } catch (InterruptedException e) {
      //TODO: We need to handle this exception more gracefully. This likely requires a refactor of our threading model.
      logger.debug("InterruptedException thrown in thread {}, closing thread.",
          this.getThreadName(), e);
      Thread.currentThread().interrupt();
    }
  }

  private void listenToCd11Socket() throws IOException, InterruptedException {
    while (this.keepThreadRunning()) {
      try {
        // Check if we've received anything from the Data Consumer.
        logger.info("new frame received thread: reading from socket for data frame");
        Cd11Frame cd11Frame = this.cd11Socket.read(this::shutThreadDown);
        logger.info("new frame received thread: read data frame from socket");

        // Check if we need to shutdown.
        if (this.shutThreadDown()) {
          logger.info("new frame received thread: shutting down");
          break;
        }

        // Generate an event.
        eventQueue.put(new Message(MessageType.NEW_FRAME_RECEIVED, cd11Frame));
      } catch (IllegalArgumentException | NullPointerException e) {
        logger.warn(
            "Malformed data encountered when reading CD1.1 frame from socket. Frame not processed. cause: {}",
            e.getMessage());
      }
    }
  }
}
