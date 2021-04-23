package gms.dataacquisition.stationreceiver.cd11.common.frames;


import java.util.Arrays;

/**
 * PartialFrame is used as an intermediate step between a fully parsed frame and the
 * bytebuffer received on the network
 */
public class PartialFrame extends Cd11Frame{

  private boolean isMalformed;
  private Exception parsingError;
  public final byte[] bodyBytes;
  public final byte[] unparsedBytes;

  public PartialFrame(Cd11FrameHeader frameHeader, Cd11FrameTrailer frameTrailer, byte[] bodyBytes,
      Exception parsingError, boolean isMalformed, byte[] unparsedBytes){
    super(frameHeader, frameTrailer, bodyBytes, isMalformed);
    this.isMalformed=isMalformed;
    this.parsingError=parsingError;
    this.bodyBytes=bodyBytes;
    this.unparsedBytes=unparsedBytes;
  }

  public boolean isMalformed(){return isMalformed;}

  public void setMalformed(boolean isMalformed){this.isMalformed=isMalformed;}

  public byte[] getFrameBodyBytes(){
    return bodyBytes;
  }

  public byte[] getUnparsedBytes(){ return unparsedBytes; }

  public Exception getParsingError(){return  parsingError;}

  public void setParsingError(Exception parsingError){this.parsingError= parsingError;}

  @Override
  public FrameType getFrameType(){
    if(isMalformed){
      return FrameType.MALFORMED_FRAME;
    } else{
      return  super.getFrameType();
    }
  }

  @Override
  public String toString() {

    String partialFrameToString="[ ";

    partialFrameToString+= ("Is malformed: " + this.isMalformed + ", ");

    if(this.frameHeaderExists()){
      partialFrameToString+=this.getFrameHeader().toString()+", ";
    }

    if(this.bodyBytes != null) {
      partialFrameToString += ("Body Bytes { [ "
          + Arrays.toString(this.bodyBytes) + "] }, ");
    }

    if(this.frameTrailerExists()){
      partialFrameToString+=this.getFrameTrailer().toString()+", ";
    }

    if(this.unparsedBytes!=null) {
      partialFrameToString += ("Unparsed Bytes { [ "
          + Arrays.toString(this.unparsedBytes) + "] }, ");
    }

    return partialFrameToString;
  }

}
