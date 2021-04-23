package gms.shared.frameworks.osd.coi.channel.repository.jpa.dataaccessobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.Orientation;
import gms.shared.frameworks.osd.coi.signaldetection.Location;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

class ChannelConfiguredInputsDaoTests {
  @Test
  void testGettersAndSetters() {
    final Channel channel = Mockito.mock(Channel.class);
    final Channel parent = Mockito.mock(Channel.class);
    final Location location = Mockito.mock(Location.class);
    final Orientation orientation = Mockito.mock(Orientation.class);

    BDDMockito.when(channel.getLocation()).thenReturn(location);
    BDDMockito.when(channel.getOrientationAngles()).thenReturn(orientation);
    BDDMockito.when(parent.getLocation()).thenReturn(location);
    BDDMockito.when(parent.getOrientationAngles()).thenReturn(orientation);

    final ChannelDao channelDao = ChannelDao.from(channel);
    final ChannelDao parentDao = ChannelDao.from(parent);

    final ChannelConfiguredInputsDao testObject = new ChannelConfiguredInputsDao();
    testObject.setId(1);
    testObject.setChannelName(channelDao);
    testObject.setRelatedChannelName(parentDao);
    assertAll(
        () -> assertEquals(1, testObject.getId()),
        () -> assertEquals(channelDao, testObject.getChannelName()),
        () -> assertEquals(parentDao, testObject.getRelatedChannelName())
    );
  }
}
