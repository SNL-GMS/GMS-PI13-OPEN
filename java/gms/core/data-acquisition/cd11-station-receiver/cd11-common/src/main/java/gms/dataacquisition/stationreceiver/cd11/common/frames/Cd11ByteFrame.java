package gms.dataacquisition.stationreceiver.cd11.common.frames;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import gms.dataacquisition.stationreceiver.cd11.common.FrameUtilities;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame.FrameType;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Contains the byte representation of a parsed CD 1.1 frame.
 */
public class Cd11ByteFrame {

  private DataInputStream dataInputStream;
  private ByteBuffer headerByteBuffer;
  private ByteBuffer bodyByteBuffer;
  private ByteBuffer trailerSegment1ByteBuffer;
  private ByteBuffer trailerSegment2ByteBuffer;
  private int frameTypeInt;
  private int trailerOffset;
  private int trailerAuthKeyIdentifier;
  private int trailerAuthSize;
  private BooleanSupplier haltReadOperation;

  private static final Logger logger = LoggerFactory.getLogger(Cd11ByteFrame.class);

  private static final int TRAILER_HEADER_1_SIZE =
      Integer.BYTES * 2;  // (int) Auth key + (int) auth size.
  private static final int COMM_VERIFICATION_SIZE = Long.BYTES;
  private static final long READ_SLEEP_TIME_MS = 5; // 5 ms.
  private static final String INTERRUPTED_READ_ERROR =
      "Network read was interrupted before the CD 1.1 frame could be fully read. " +
          "The frame offset is now broken, and the CD 1.1 connection needs to be restarted.";

  /**
   * Reads a CD 1.1 frame from a data input stream, but quits when the haltReadOperation returns
   * true.
   *
   * @param dataInputStream The data source.
   * @param haltReadOperation Lambda that returns true when the read operation must be halted.
   * @throws InterruptedIOException Thrown when haltReadOperation returns true, before any data has
   * been read from the data input stream.
   * @throws IOException Thrown on read or parsing error.
   */
  public Cd11ByteFrame(DataInputStream dataInputStream, BooleanSupplier haltReadOperation)
      throws IOException {

    checkNotNull(dataInputStream, "Data input stream parameter is null.");
    checkNotNull(haltReadOperation, "The haltReadOperation function is null.");

    this.dataInputStream = dataInputStream;
    this.haltReadOperation = haltReadOperation;

    populateHeader();
    populateBody();
    populateTrailerSegment1();
    populateTrailerSegment2();
  }

  protected Cd11ByteFrame(){}

  /**
   * Cd11ByteFrame class for reading input stream of bytes
   */
  public Cd11ByteFrame(DataInputStream dataInputStream) throws IOException {
    this.dataInputStream = dataInputStream;

    populateHeaderNetty();
    populateBodyNetty();
    populateTrailerSegment1Netty();
    populateTrailerSegment2Netty();
  }

  /**
   * Reads the CD 1.1 frame body from the data input stream.
   *
   * @throws IOException Throws I/O exceptions.
   */
  private void populateBody() throws IOException {
    int requiredBodyBytes = trailerOffset - Cd11FrameHeader.FRAME_LENGTH;
    byte[] bodyBytes = new byte[requiredBodyBytes];

    while (true) {
      if (dataInputStream.available() >= requiredBodyBytes) {
        // Read the data.
        int bytes = dataInputStream.read(bodyBytes);
        if (bytes != requiredBodyBytes) {
          throw new IOException(String.format(
                  "Error while reading CD 1.1 frame body (expected %d bytes, received %d bytes).",
                  requiredBodyBytes, bytes));
        }

        // Bytes successfully read, now break from the loop.
        break;
      } else if (!haltReadOperation.getAsBoolean()) {
        // Try again, after a period of time.
        try {
          Thread.sleep(READ_SLEEP_TIME_MS);
        } catch (InterruptedException e) {
          // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
          Thread.currentThread().interrupt();
          throw new IOException(INTERRUPTED_READ_ERROR);
        }
      } else {
        // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
        throw new IOException(INTERRUPTED_READ_ERROR);
      }
    }

    bodyByteBuffer = ByteBuffer.wrap(bodyBytes);
    bodyByteBuffer.rewind();
  }

  /**
   * Reads the CD 1.1 frame body from the data input stream.
   *
   * @throws IOException Throws I/O exceptions.
   */
  private void populateBodyNetty() throws IOException {
    int requiredBodyBytes = trailerOffset - Cd11FrameHeader.FRAME_LENGTH;
    byte[] bodyBytes = new byte[requiredBodyBytes];

    if (dataInputStream.available() >= requiredBodyBytes) {
      // Read the data.
      int bytes = dataInputStream.read(bodyBytes);
      if (bytes != requiredBodyBytes) {
        throw new IOException(String.format(
                "Error while reading CD 1.1 frame body (expected %d bytes, received %d bytes).",
                requiredBodyBytes, bytes));
      }
    } else {
      // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
      throw new IOException(INTERRUPTED_READ_ERROR);
    }

    bodyByteBuffer = ByteBuffer.wrap(bodyBytes);
    bodyByteBuffer.rewind();
  }

  /**
   * Reads the CD 1.1 frame header from the data input stream.
   *
   * @throws IOException Throws I/O exceptions.
   */
  private void populateHeader() throws IOException {
    byte[] headerBytes = new byte[Cd11FrameHeader.FRAME_LENGTH];

    while (true) {
      if (dataInputStream.available() >= Cd11FrameHeader.FRAME_LENGTH) {
        // Read the data.
        int bytes = dataInputStream.read(headerBytes);
        if (bytes != Cd11FrameHeader.FRAME_LENGTH) {
          throw new IOException(String.format(
              "Error while reading CD 1.1 frame header (expected %d bytes, received %d bytes).",
              Cd11FrameHeader.FRAME_LENGTH, bytes));
        }

        // Bytes successfully read, now break from the loop.
        break;
      } else if (!haltReadOperation.getAsBoolean()) {
        // Try again, after a period of time.
        try {
          Thread.sleep(READ_SLEEP_TIME_MS);
        } catch (InterruptedException e) {
          // No data came in from the Data Provider.
          Thread.currentThread().interrupt();
          throw new InterruptedIOException(
              "CD 1.1 read method was interrupted before any data was read from the network stream.");
        }
      } else {
        // No data came in from the Data Provider.
        Thread.currentThread().interrupt();
        throw new InterruptedIOException(
            "CD 1.1 read method was interrupted before any data was read from the network stream.");
      }
    }

    headerByteBuffer = ByteBuffer.wrap(headerBytes);
    headerByteBuffer.rewind();
    frameTypeInt = headerByteBuffer.getInt();
    trailerOffset = headerByteBuffer.getInt();
    FrameType.fromInt(frameTypeInt);
    checkArgument((trailerOffset - Cd11FrameHeader.FRAME_LENGTH) >= 0,
          "The offset of the frame trailer must be at least the size of the header");

    logger.debug("Trailer offset for frame is {}", trailerOffset);
    headerByteBuffer.rewind();
  }

  /**
   * Reads the CD 1.1 frame header from the data input stream.
   *
   * @throws IOException Throws I/O exceptions.
   */
  private void populateHeaderNetty() throws IOException {
    byte[] headerBytes = new byte[Cd11FrameHeader.FRAME_LENGTH];

    if (dataInputStream.available() >= Cd11FrameHeader.FRAME_LENGTH) {
      // Read the data.
      int bytes = dataInputStream.read(headerBytes);
      if (bytes != Cd11FrameHeader.FRAME_LENGTH) {
        throw new IOException(String.format(
                "Error while reading CD 1.1 frame header (expected %d bytes, received %d bytes).",
                Cd11FrameHeader.FRAME_LENGTH, bytes));
      }
    } else {
      // no byte data came in from the byte stream
      throw new InterruptedIOException(
              "CD 1.1 read method was interrupted before any data was read from the input stream.");
    }

    headerByteBuffer = ByteBuffer.wrap(headerBytes);
    headerByteBuffer.rewind();
    frameTypeInt = headerByteBuffer.getInt();
    trailerOffset = headerByteBuffer.getInt();
    headerByteBuffer.rewind();
  }

  /**
   * Reads the first two bytes of the CD 1.1 frame trailer from the data input stream.
   *
   * @throws IOException Throws I/O exceptions.
   */
  private void populateTrailerSegment1() throws IOException {
    byte[] trailerSegment1Bytes = new byte[TRAILER_HEADER_1_SIZE];

    while (true) {
      if (dataInputStream.available() >= TRAILER_HEADER_1_SIZE) {
        // Read the data.
        int bytes = dataInputStream.read(trailerSegment1Bytes);
        if (bytes != TRAILER_HEADER_1_SIZE) {
          throw new IOException(String.format(
              "Error while reading CD 1.1 frame trailer segment 1 (expected %d bytes, received %d bytes).",
              TRAILER_HEADER_1_SIZE, bytes));
        }

        // Bytes successfully read, now break from the loop.
        break;
      } else if (!haltReadOperation.getAsBoolean()) {
        // Try again, after a period of time.
        try {
          Thread.sleep(READ_SLEEP_TIME_MS);
        } catch (InterruptedException e) {
          // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
          Thread.currentThread().interrupt();
          throw new IOException(INTERRUPTED_READ_ERROR);
        }
      } else {
        // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
        throw new IOException(INTERRUPTED_READ_ERROR);
      }
    }

    trailerSegment1ByteBuffer = ByteBuffer.wrap(trailerSegment1Bytes);
    trailerSegment1ByteBuffer.rewind();
    trailerAuthKeyIdentifier = trailerSegment1ByteBuffer.getInt();
    trailerAuthSize = trailerSegment1ByteBuffer.getInt();
    trailerSegment1ByteBuffer.rewind();
  }

  /**
   * Reads the remaining bytes of the CD 1.1 frame trailer from the data input stream.
   *
   * @throws IOException Throws I/O exceptions.
   */
  private void populateTrailerSegment2() throws IOException {
    int paddedAuthValSize = FrameUtilities.calculatePaddedLength(trailerAuthSize, Integer.BYTES);
    paddedAuthValSize = paddedAuthValSize + COMM_VERIFICATION_SIZE;

    byte[] trailerSegment2Bytes = new byte[paddedAuthValSize];

    while (true) {
      if (dataInputStream.available() >= paddedAuthValSize) {
        // Read the data.
        int bytes = dataInputStream.read(trailerSegment2Bytes);
        if (bytes != paddedAuthValSize) {
          throw new IOException(String.format(
              "Error while reading CD 1.1 frame trailer segment 2 (expected %d bytes, received %d bytes).",
              paddedAuthValSize, bytes));
        }

        // Bytes successfully read, now break from the loop.
        break;
      } else if (!haltReadOperation.getAsBoolean()) {
        // Try again, after a period of time.
        try {
          Thread.sleep(READ_SLEEP_TIME_MS);
        } catch (InterruptedException e) {
          // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
          Thread.currentThread().interrupt();
          throw new IOException(INTERRUPTED_READ_ERROR);
        }
      } else {
        // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
        throw new IOException(INTERRUPTED_READ_ERROR);
      }
    }

    trailerSegment2ByteBuffer = ByteBuffer.wrap(trailerSegment2Bytes);
    trailerSegment2ByteBuffer.rewind();
  }

  /**
   * Reads the first two bytes of the CD 1.1 frame trailer from the data input stream.
   *
   * @throws IOException Throws I/O exceptions.
   */
  private void populateTrailerSegment1Netty() throws IOException {
    byte[] trailerSegment1Bytes = new byte[TRAILER_HEADER_1_SIZE];

    if (dataInputStream.available() >= TRAILER_HEADER_1_SIZE) {
      // Read the data.
      int bytes = dataInputStream.read(trailerSegment1Bytes);
      if (bytes != TRAILER_HEADER_1_SIZE) {
        throw new IOException(String.format(
                "Error while reading CD 1.1 frame trailer segment 1 (expected %d bytes, received %d bytes).",
                TRAILER_HEADER_1_SIZE, bytes));
      }
    } else {
      // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
      throw new IOException(INTERRUPTED_READ_ERROR);
    }

    trailerSegment1ByteBuffer = ByteBuffer.wrap(trailerSegment1Bytes);
    trailerSegment1ByteBuffer.rewind();
    trailerAuthKeyIdentifier = trailerSegment1ByteBuffer.getInt();
    trailerAuthSize = trailerSegment1ByteBuffer.getInt();
    trailerSegment1ByteBuffer.rewind();
  }

  /**
   * Reads the remaining bytes of the CD 1.1 frame trailer from the data input stream.
   *
   * @throws IOException Throws I/O exceptions.
   */
  private void populateTrailerSegment2Netty() throws IOException {
    int paddedAuthValSize = FrameUtilities.calculatePaddedLength(trailerAuthSize, Integer.BYTES);
    paddedAuthValSize = paddedAuthValSize + COMM_VERIFICATION_SIZE;

    byte[] trailerSegment2Bytes = new byte[paddedAuthValSize];

    if (dataInputStream.available() >= paddedAuthValSize) {
      // Read the data.
      int bytes = dataInputStream.read(trailerSegment2Bytes);
      if (bytes != paddedAuthValSize) {
        throw new IOException(String.format(
                "Error while reading CD 1.1 frame trailer segment 2 (expected %d bytes, received %d bytes).",
                paddedAuthValSize, bytes));
      }
    } else {
      // CD 1.1 frame is partially read, and our offset is now corrupted; the network stream can no longer be read!
      throw new IOException(INTERRUPTED_READ_ERROR);
    }

    trailerSegment2ByteBuffer = ByteBuffer.wrap(trailerSegment2Bytes);
    trailerSegment2ByteBuffer.rewind();
  }

  public FrameType getFrameType() {
    return FrameType.fromInt(this.frameTypeInt);
  }

  /**
   * Returns the raw byte frame read from the network.
   *
   * @return Raw network bytes.
   */
  public byte[] getRawReceivedBytes() {
    return ByteBuffer
        .allocate(
            headerByteBuffer.array().length +
                bodyByteBuffer.array().length +
                trailerSegment1ByteBuffer.array().length +
                trailerSegment2ByteBuffer.array().length)
        .put(getFrameHeaderByteBuffer())
        .put(getFrameBodyByteBuffer())
        .put(getFrameTrailerSegment1ByteBuffer())
        .put(getFrameTrailerSegment2ByteBuffer())
        .array();

  }

  public ByteBuffer getFrameHeaderByteBuffer() {
    return this.headerByteBuffer.rewind();
  }

  public ByteBuffer getFrameBodyByteBuffer() {
    return this.bodyByteBuffer.rewind();
  }

  public ByteBuffer getFrameTrailerSegment1ByteBuffer() {
    return this.trailerSegment1ByteBuffer.rewind();
  }

  public ByteBuffer getFrameTrailerSegment2ByteBuffer() {
    return this.trailerSegment2ByteBuffer.rewind();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Cd11ByteFrame that = (Cd11ByteFrame) o;

    if (frameTypeInt != that.frameTypeInt) {
      return false;
    }
    if (trailerOffset != that.trailerOffset) {
      return false;
    }
    if (trailerAuthKeyIdentifier != that.trailerAuthKeyIdentifier) {
      return false;
    }
    if (trailerAuthSize != that.trailerAuthSize) {
      return false;
    }
    if (!Objects.equals(dataInputStream, that.dataInputStream)) {
      return false;
    }
    if (!Objects.equals(headerByteBuffer, that.headerByteBuffer)) {
      return false;
    }
    if (bodyByteBuffer != null ? !bodyByteBuffer.equals(that.bodyByteBuffer)
        : that.bodyByteBuffer != null) {
      return false;
    }
    if (trailerSegment1ByteBuffer != null ? !trailerSegment1ByteBuffer
        .equals(that.trailerSegment1ByteBuffer) : that.trailerSegment1ByteBuffer != null) {
      return false;
    }
    return trailerSegment2ByteBuffer != null ? trailerSegment2ByteBuffer
        .equals(that.trailerSegment2ByteBuffer) : that.trailerSegment2ByteBuffer == null;
  }

  @Override
  public int hashCode() {
    int result = dataInputStream != null ? dataInputStream.hashCode() : 0;
    result = 31 * result + (headerByteBuffer != null ? headerByteBuffer.hashCode() : 0);
    result = 31 * result + (bodyByteBuffer != null ? bodyByteBuffer.hashCode() : 0);
    result = 31 * result + (trailerSegment1ByteBuffer != null ? trailerSegment1ByteBuffer.hashCode()
        : 0);
    result = 31 * result + (trailerSegment2ByteBuffer != null ? trailerSegment2ByteBuffer.hashCode()
        : 0);
    result = 31 * result + frameTypeInt;
    result = 31 * result + trailerOffset;
    result = 31 * result + trailerAuthKeyIdentifier;
    result = 31 * result + trailerAuthSize;
    result = 31 * result + TRAILER_HEADER_1_SIZE;
    result = 31 * result + COMM_VERIFICATION_SIZE;
    return result;
  }
}
