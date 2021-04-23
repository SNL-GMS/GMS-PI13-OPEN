package gms.shared.frameworks.osd.control.utils;

import com.google.common.hash.Hashing;
import gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects.ChannelDao;
import gms.shared.frameworks.osd.coi.signaldetection.MeasuredChannelSegmentDescriptor;
import gms.shared.frameworks.osd.coi.signaldetection.repository.jpa.dataaccessobjects.MeasuredChannelSegmentDescriptorDao;
import javax.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.UUID;

public class MeasuredChannelSegmentDescriptorDaoUtility {

  public static MeasuredChannelSegmentDescriptorDao fromCoi(MeasuredChannelSegmentDescriptor descriptor,
      EntityManager entityManager) {
    MeasuredChannelSegmentDescriptorDao dao = entityManager.find(MeasuredChannelSegmentDescriptorDao.class,
        buildId(descriptor));

    if (dao == null) {
      ChannelDao channelDao = entityManager.find(ChannelDao.class, descriptor.getChannelName());

      Objects.requireNonNull(channelDao,
          "Cannot insert MeasuredChannelSegmentDescriptorDao for a channel that does not exist");

      dao = new MeasuredChannelSegmentDescriptorDao();
      dao.setId(buildId(descriptor));
      dao.setChannel(channelDao);
      dao.setMeasuredChannelSegmentStartTime(descriptor.getMeasuredChannelSegmentStartTime());
      dao.setMeasuredChannelSegmentEndTime(descriptor.getMeasuredChannelSegmentEndTime());
      dao.setMeasuredChannelSegmentCreationTime(descriptor.getMeasuredChannelSegmentCreationTime());
      entityManager.persist(dao);
    }

    return dao;
  }

  public static MeasuredChannelSegmentDescriptor toCoi(MeasuredChannelSegmentDescriptorDao dao) {
    return MeasuredChannelSegmentDescriptor.builder()
        .setChannelName(dao.getChannel().getName())
        .setMeasuredChannelSegmentStartTime(dao.getMeasuredChannelSegmentStartTime())
        .setMeasuredChannelSegmentEndTime(dao.getMeasuredChannelSegmentEndTime())
        .setMeasuredChannelSegmentCreationTime(dao.getMeasuredChannelSegmentCreationTime())
        .build();
  }


  public static UUID buildId(MeasuredChannelSegmentDescriptor descriptor) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ObjectOutput objectOutput = new ObjectOutputStream(out);

      objectOutput.writeObject(descriptor.getChannelName());
      objectOutput.writeObject(descriptor.getMeasuredChannelSegmentStartTime());
      objectOutput.writeObject(descriptor.getMeasuredChannelSegmentEndTime());
      objectOutput.writeObject(descriptor.getMeasuredChannelSegmentCreationTime());

      objectOutput.flush();

      byte[] objectBytes = out.toByteArray();

      Objects.requireNonNull(objectBytes,
          "Could not hash MeasuredChannelSegmentDescriptor attributes; byte array containing " +
              "serialized MeasuredChannelSegmentDescriptor output is null");

      return UUID.nameUUIDFromBytes(Hashing.sha256().hashBytes(objectBytes).asBytes());
    } catch (IOException e) {
      throw new IllegalStateException("Error serializing MeasuredChannelSegmentDescriptor to " +
          "byte array", e);
    }
  }

}
