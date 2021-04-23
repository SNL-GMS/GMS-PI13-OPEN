package gms.dataacquisition.stationreceiver.cd11.injector;

import java.io.IOError;
import java.nio.ByteBuffer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.parsetools.RecordParser;

import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11FrameHeader;

/**
 * Read stream for CD1.1 data.  Produces the bytes that are a full CD1.1 frame of some kind
 */
class Cd11ReadStream implements ReadStream<Buffer> {
  private static final int TRAILER_HEADER_1_SIZE = Integer.BYTES * 2;  // (int) Auth key + (int) auth size.
  private static final int COMM_VERIFICATION_SIZE = Long.BYTES;


  enum Location {
    HEADER, BODY, TRAILER1, TRAILER2, NONE
  }

  private final RecordParser recordParser;
  private Buffer cd11Buffer = Buffer.buffer();
  private Location location = Location.HEADER;
  public Cd11ReadStream(ReadStream<Buffer> stream) {
    recordParser = RecordParser.newFixed(Cd11FrameHeader.FRAME_LENGTH, stream);
  }

  @Override
  public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    recordParser.exceptionHandler(handler);
    return this;
  }

  @Override
  public ReadStream<Buffer> fetch(long amount) {
    recordParser.fetch(amount);
    return this;
  }


  /**
   * Set a handler that is invoked when bytes that are a full CD1.1 frame have been read
   * @param handler
   * @return this to enable a fluent API
   */
  @Override
  public ReadStream<Buffer> handler(Handler<Buffer> handler) {
    if (handler == null) {
      recordParser.handler(null);
      return this;
    }
    recordParser.handler(buffer -> {
      if (location == Location.HEADER) { // read header
        cd11Buffer = Buffer.buffer();
        cd11Buffer.appendBuffer(buffer);
        Cd11FrameHeader frameHeader = new Cd11FrameHeader(ByteBuffer.wrap(cd11Buffer.getBytes()));
        recordParser.fixedSizeMode(frameHeader.trailerOffset - Cd11FrameHeader.FRAME_LENGTH);
        location = Location.BODY;
      } else if (location == Location.BODY) { // read body

        cd11Buffer.appendBuffer(buffer);
        recordParser.fixedSizeMode(TRAILER_HEADER_1_SIZE);
        location = Location.TRAILER1;

      } else if (location == Location.TRAILER1) { // read trailer part 1 (auth key id + auth size)

        cd11Buffer.appendBuffer(buffer);
        recordParser.fixedSizeMode(buffer.getInt(Integer.BYTES) + COMM_VERIFICATION_SIZE);
        location = Location.TRAILER2;

      } else if (location == Location.TRAILER2) { // read second part of trailer (auth + comm ver)

        cd11Buffer.appendBuffer(buffer);
        recordParser.fixedSizeMode(Cd11FrameHeader.FRAME_LENGTH);
        location = Location.HEADER;
        handler.handle(cd11Buffer);

      } else {
        throw new IOError(new Throwable("Parser failed on CD11 byte stream"));
      }
    });
    return this;
  }

  @Override
  public ReadStream<Buffer> pause() {
    recordParser.pause();
    return this;
  }

  @Override
  public ReadStream<Buffer> resume() {
    recordParser.resume();
    return this;
  }

  @Override
  public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
    recordParser.endHandler(endHandler);
    return this;
  }
}
