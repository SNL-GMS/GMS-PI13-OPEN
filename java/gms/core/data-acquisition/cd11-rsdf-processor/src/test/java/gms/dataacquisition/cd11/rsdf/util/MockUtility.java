package gms.dataacquisition.cd11.rsdf.util;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

import gms.core.dataacquisition.receiver.DataFrameReceiverConfiguration;
import gms.shared.frameworks.osd.api.channel.ChannelRepositoryInterface;
import gms.shared.frameworks.osd.coi.channel.Channel;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MockUtility {

  public static Channel mockChannel(String name) {
    Channel channel = mock(Channel.class);
    willReturn(name).given(channel).getName();
    return channel;
  }

  public static void configureMockRepository(ChannelRepositoryInterface repository,
      Channel... mockChannels) {

    List<String> mockChannelNames = Arrays.stream(mockChannels).map(Channel::getName)
        .collect(toList());

    given(repository.retrieveChannels(eq(mockChannelNames)))
        .willReturn(Arrays.asList(mockChannels));
  }
}
