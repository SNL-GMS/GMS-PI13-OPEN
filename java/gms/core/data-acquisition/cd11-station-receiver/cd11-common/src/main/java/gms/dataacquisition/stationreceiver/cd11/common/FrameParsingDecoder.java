package gms.dataacquisition.stationreceiver.cd11.common;

import static com.google.common.base.Preconditions.checkArgument;

import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame.FrameType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;


public class FrameParsingDecoder extends ByteToMessageDecoder {


  //constants for size of header/body/trailer fields
  private static final int HEADER_SIZE= (Integer.BYTES * 3) + Long.BYTES + 8 + 8;

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception{

    if(in.readableBytes()<HEADER_SIZE){
      return;
    }

    try{
      int frameTypeInt = in.getInt(0);
      FrameType.fromInt(frameTypeInt);
      int trailerOffset = in.getInt(Integer.BYTES);
      checkArgument((trailerOffset - HEADER_SIZE) >= 0,
          "The offset of the frame trailer must be at least the size of the header");

      if(in.readableBytes()<trailerOffset+2*Integer.BYTES) {
        return;
      }
      int trailerAuthSize = in.getInt(trailerOffset + Integer.BYTES);
      checkArgument(trailerAuthSize >= 0,
          "The offset of the frame trailer must be at least the size of the header");
      int paddedAuthValSize = FrameUtilities
          .calculatePaddedLength(trailerAuthSize, Integer.BYTES);

      int totalSize = trailerOffset+2*Integer.BYTES+paddedAuthValSize+Long.BYTES;
      if(in.readableBytes()<totalSize) {
        return;
      }


      byte[] send = new byte[totalSize];
      in.readBytes(send);
      in.discardReadBytes();
      out.add(send);

    } catch (IllegalArgumentException e ) {


      byte[] send = new byte[in.readableBytes()];
      in.readBytes(send);
      in.discardReadBytes();
      out.add(send);
    }
  }
}