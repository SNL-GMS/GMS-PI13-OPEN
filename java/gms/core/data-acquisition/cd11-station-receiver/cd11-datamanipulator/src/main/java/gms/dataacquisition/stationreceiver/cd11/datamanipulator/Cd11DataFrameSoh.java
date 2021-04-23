package gms.dataacquisition.stationreceiver.cd11.datamanipulator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ByteFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ChannelSubframe;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11DataFrame;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.dataacquisition.cd11.rsdf.processor.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Cd11DataFrameSoh {

    public final List<List<AcquiredChannelEnvironmentIssue>> cd11AcquiredSohList;
    public final Cd11DataFrame dataFrame;

    Cd11DataFrameSoh(Cd11ByteFrame cd11ByteFrame) {
        this.cd11AcquiredSohList = new ArrayList<>();
        this.dataFrame = new Cd11DataFrame(cd11ByteFrame);
        for (Cd11ChannelSubframe frame : dataFrame.channelSubframes) {
            List<AcquiredChannelEnvironmentIssue> soh = Cd11AcquiredChannelEnvironmentIssuesParser
                .parseAcquiredChannelSoh(frame.channelStatusData, frame.channelName,
                    frame.timeStamp, frame.endTime);
            cd11AcquiredSohList.add(soh);
        }
    }

    @JsonCreator
    public Cd11DataFrameSoh(
            @JsonProperty("cd11AcquiredSohList") List<List<AcquiredChannelEnvironmentIssue>> cd11AcquiredSohList,
            @JsonProperty("dataFrame") Cd11DataFrame dataFrame
    ) {
        this.cd11AcquiredSohList = cd11AcquiredSohList;
        this.dataFrame = dataFrame;
    }

    public List<List<AcquiredChannelEnvironmentIssue>> getCd11AcquiredSohList() {
        return cd11AcquiredSohList;
    }

    Cd11DataFrame getDataFrame() { return dataFrame; }


    /**
     * Takes the internal state of health and changes the bytes and returns a new Cd11DataFrame
     * @return Cd11DataFrame with new bytes to match soh object
     */
    Cd11DataFrame transcribeSohToBytes() {
        Cd11ChannelSubframe[] newChannelSubframes = new Cd11ChannelSubframe[dataFrame.channelSubframes.length];
        for (int i = 0; i < dataFrame.channelSubframes.length; i++) {

            byte[] channelStatusData = acquiredSohListToBytes(this.cd11AcquiredSohList.get(i), dataFrame.channelSubframes[i].channelStatusData);
            Cd11ChannelSubframe newChannelSubframe = new Cd11ChannelSubframe(
                    dataFrame.channelSubframes[i], channelStatusData.length, channelStatusData);
            newChannelSubframes[i] = newChannelSubframe;
        }
        return new Cd11DataFrame(dataFrame.chanSubframeHeader, newChannelSubframes, dataFrame.getFrameTrailer(), dataFrame.getFrameHeader());
    }

    /**
     * Convert acquired soh list back to bytes that it was originally derived from This was back
     * converted from the toChannelStatusList method in Cd11RawStationDataFrameUtility
     *
     * @param sohList list of AcquireChannelSoh that represents the status of t
     * @param oldBytes old bytes from the json file
     * @return List of bytes that is written to
     */
    private byte[] acquiredSohListToBytes(List<AcquiredChannelEnvironmentIssue> sohList, byte[] oldBytes) {

        byte[] newSohBytes = oldBytes.clone();
        int byteIdx = 1;
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(0).getStatus(), newSohBytes[byteIdx], 0);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(1).getStatus(), newSohBytes[byteIdx], 1);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(2).getStatus(), newSohBytes[byteIdx], 2);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(3).getStatus(), newSohBytes[byteIdx], 3);

        byteIdx = 2;
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(4).getStatus(), newSohBytes[byteIdx], 0);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(5).getStatus(), newSohBytes[byteIdx], 1);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(6).getStatus(), newSohBytes[byteIdx], 2);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(7).getStatus(), newSohBytes[byteIdx], 3);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(8).getStatus(), newSohBytes[byteIdx], 4);

        byteIdx = 3;
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(9).getStatus(), newSohBytes[byteIdx], 0);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(10).getStatus(), newSohBytes[byteIdx], 1);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(11).getStatus(), newSohBytes[byteIdx], 2);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(12).getStatus(), newSohBytes[byteIdx], 3);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(13).getStatus(), newSohBytes[byteIdx], 4);

        byteIdx = 4;
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(14).getStatus(), newSohBytes[byteIdx], 0);
        newSohBytes[byteIdx] = getNewByteFromSohStatus(sohList.get(15).getStatus(), newSohBytes[byteIdx], 1);

        byteIdx = 28;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(((Double)sohList.get(16).getStatus()).intValue());
        buffer.flip();
        byte[] byteIntArr = buffer.array();

        for(int i =0; i < byteIntArr.length; i++) {
            newSohBytes[byteIdx] = byteIntArr[i];
            byteIdx++;
        }

        return newSohBytes;
    }

    /**
     * Makes a new byte from a SOH status
     * @param status Object that comes from the AcquiredSohStatus
     * @param oldByte the byte that's being converted
     * @param bitLocation location of bit to change
     * @return new changed byte
     */
    private static byte getNewByteFromSohStatus(Object status, byte oldByte, int bitLocation) {
        int bitValue = (boolean) status ? 0x1  : 0x0;
        int mask = ~(0x1 << bitLocation);
        return (byte) ((mask & oldByte) | (bitValue << bitLocation));
    }
}
