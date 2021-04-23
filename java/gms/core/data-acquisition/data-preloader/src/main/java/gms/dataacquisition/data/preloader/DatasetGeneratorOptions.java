package gms.dataacquisition.data.preloader;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command Line Options for the DataGenerationManager, managing command-line execution of the manager
 * with the following options:
 * <pre>
 *   {@literal
 *   --dataType=(ACEI_ANALOG|ACEI_BOOLEAN|ROLLUP|RSDF|STATION_SOH)
 *   --startTime=<start_time>
 *   --sampleDuration=<duration-of-sample>
 *   --duration=<duration>
 *   --stationGroups=<set-of-station-groups>
 *   [--receptionDelay=<reception_time_seed>]
 *   }
 * </pre>
 */
public class DatasetGeneratorOptions {

  private static final String ISO_DURATION = "ISO duration";

  private static final String DATA_TYPE_NAME = "dataType";
  private static final Option dataType = Option.builder()
      .longOpt(DATA_TYPE_NAME)
      .desc("Comma separated list of data Types to generate")
      .hasArg()
      .argName("ACEI_ANALOG|ACEI_BOOLEAN|ROLLUP|RSDF|STATION_SOH")
      .required()
      .build();

  private static final String START_TIME_NAME = "startTime";
  private static final Option startTime = Option.builder()
      .longOpt(START_TIME_NAME)
      .desc("Start time of seed data, Duration string from present")
      .hasArg()
      .argName("ISO instant")
      .required()
      .build();

  private static final String SAMPLE_DURATION_NAME = "sampleDuration";
  private static final Option sampleDuration = Option.builder()
      .longOpt(SAMPLE_DURATION_NAME)
      .desc("How long each sample should be")
      .hasArg()
      .argName(ISO_DURATION)
      .required()
      .build();

  private static final String DURATION_NAME = "duration";
  private static final Option duration = Option.builder()
      .longOpt(DURATION_NAME)
      .desc("Amount of time for which to generate data as a Duration string")
      .hasArg()
      .argName(ISO_DURATION)
      .required()
      .build();

  private static final String STATION_GROUPS_NAME = "stationGroups";
  private static final Option stationGroups = Option.builder()
      .longOpt(STATION_GROUPS_NAME)
      .desc("Comma-separated station groups for which to generate data")
      .hasArgs()
      .argName("groups")
      .required()
      .build();

  private static final String RECEPTION_DELAY_NAME = "receptionDelay";
  private static final Option receptionDelay = Option.builder()
      .longOpt(RECEPTION_DELAY_NAME)
      .desc("Time delay seed object was received")
      .hasArg()
      .argName(ISO_DURATION)
      .build();

  public static final Options options = new Options()
      .addOption(dataType)
      .addOption(startTime)
      .addOption(sampleDuration)
      .addOption(duration)
      .addOption(stationGroups)
      .addOption(receptionDelay);

  /**
   * Generates an initial {@link GenerationSpec} for in the incoming command line arguments,
   * containing all information provided at runtime.
   * @param args Command line arguments
   * @return Initial GenerationSpec
   */
  public static GenerationSpec parse(String[] args) {
    CommandLineParser parser = new DefaultParser();

    try {
      CommandLine line = parser.parse(DatasetGeneratorOptions.options, args);
      return parseSpec(line);
    } catch (ParseException | DateTimeParseException e) {
      throw new IllegalArgumentException("Error parsing arguments", e);
    }

  }

  private static GenerationSpec parseSpec(CommandLine line) {
    GenerationType dataType = getOption(line, DATA_TYPE_NAME, GenerationType::parseType);
    Instant startTime = getOption(line, START_TIME_NAME, Instant::parse);
    Duration sampleDuration = getOption(line, SAMPLE_DURATION_NAME, Duration::parse);
    Duration duration = getOption(line, DURATION_NAME, Duration::parse);

    GenerationSpec.Builder specBuilder = GenerationSpec.builder()
        .setType(dataType)
        .setStartTime(startTime)
        .setSampleDuration(sampleDuration)
        .setDuration(duration)
        .setBatchSize(100); //TODO: SHOULD THIS BE PARSED OR NOT???

    String stationGroups = line.getOptionValue(STATION_GROUPS_NAME);
    Optional<Duration> receptionDelay = getOption(line, RECEPTION_DELAY_NAME,
        val -> Optional.ofNullable(val).map(Duration::parse));

    specBuilder.addInitialCondition(InitialCondition.STATION_GROUPS, stationGroups);
    receptionDelay.ifPresent(reception -> specBuilder
        .addInitialCondition(InitialCondition.RECEPTION_DELAY, reception.toString()));

    return specBuilder.build();
  }

  private static <T> T getOption(CommandLine line, String option,
      Function<String, T> valueParser) {
    return valueParser.apply(line.getOptionValue(option));
  }

  private DatasetGeneratorOptions() {
  }
}
