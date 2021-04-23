package gms.dataacquisition.stationreceiver.cd11.common.frames;

//this is a placeholder for frames that we have not created methods to create yet. Currently the CD 1 encapsulation frame


public class Cd11DummyFrame extends Cd11Frame {

  public Cd11DummyFrame(FrameType frameType) {

    super(frameType);

  }

  public byte[] getFrameBodyBytes() {

    return new byte[0];
  }


}
