package gms.shared.frameworks.osd.coi.signaldetection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.Units;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementChannelDefinition.BeamParameters;
import gms.shared.frameworks.osd.coi.signaldetection.AmplitudeMeasurementChannelDefinition.FilterParameters;
import gms.shared.frameworks.osd.coi.test.utils.UtilsTestFixtures;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AmplitudeMeasurementChannelDefinitionTests {

  private static final List<String> BEAMABLE_CHANNEL_ID_ARRAY = List
      .of("ac1", "ac2");

  private static final FrequencyAmplitudePhase EMPTY_FAP = FrequencyAmplitudePhase.builder()
      .setFrequencies(new double[]{})
      .setAmplitudeResponseUnits(Units.UNITLESS)
      .setAmplitudeResponse(new double[]{})
      .setAmplitudeResponseStdDev(new double[]{})
      .setPhaseResponseUnits(Units.UNITLESS)
      .setPhaseResponse(new double[]{})
      .setPhaseResponseStdDev(new double[]{})
      .build();

  @Test
  void testJsonDeserialize() throws IOException {
    ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    AmplitudeMeasurementChannelDefinition expected = AmplitudeMeasurementChannelDefinition.builder()
        .setBeamParameters(BeamParameters.from("test", SignalDetectionTestFixtures.BEAM_DEFINITION))
        .setFilterParameters(FilterParameters.from("test", UtilsTestFixtures.filterDefinition, EMPTY_FAP))
        .setChannelNames(List.of(UtilsTestFixtures.CHANNEL.getName()))
        .setRawWaveformBufferLead(Duration.ZERO)
        .setRawWaveformBufferLag(Duration.ZERO)
        .build();

    assertEquals(expected, objectMapper.readValue(objectMapper.writeValueAsString(expected),
        AmplitudeMeasurementChannelDefinition.class));
  }

  @ParameterizedTest
  @MethodSource("handlerInvalidArguments")
  void testInvalidArguments(BeamParameters beamParameters, List<String> channelIds) {
    assertThrows(IllegalStateException.class, () ->
        AmplitudeMeasurementChannelDefinition.builder()
            .setFilterParameters(FilterParameters.from("test", UtilsTestFixtures.filterDefinition, EMPTY_FAP))
            .setBeamParameters(Optional.ofNullable(beamParameters))
            .setChannelNames(channelIds)
            .setRawWaveformBufferLead(Duration.ZERO)
            .setRawWaveformBufferLag(Duration.ZERO)
            .build());
  }

  private static Stream<Arguments> handlerInvalidArguments() {
    return Stream.of(
        arguments(null, BEAMABLE_CHANNEL_ID_ARRAY)
    );
  }
}