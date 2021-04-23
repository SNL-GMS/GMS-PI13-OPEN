package gms.dataacquisition.csswaveformconverter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.dataacquisition.cssreader.data.WfdiscRecord;
import gms.dataacquisition.cssreader.flatfilereaders.FlatFileWfdiscReader;
import gms.dataacquisition.csswaveformconverter.data.CssWaveformFileWriterCommandLineArgs;
import gms.shared.frameworks.osd.coi.channel.Channel;
import gms.shared.frameworks.osd.coi.channel.soh.AcquiredChannelEnvironmentIssue;
import gms.shared.frameworks.osd.coi.dataacquisition.SegmentClaimCheck;
import gms.shared.frameworks.osd.coi.dataacquisition.WaveformAcquiredChannelSohPair;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command-line application to load data from CSS flat files.
 */
public class CssWaveformFileWriter {

  private static final Logger logger = LoggerFactory.getLogger(CssWaveformFileWriter.class);

  private static final String OUTPUT_LEAF_DIR_NAME = "gms_test_data_set";
  private static final String SEGMENTS_AND_SOH_DIR_NAME = "segments-and-soh/";
  private static final ObjectMapper objMapper = CoiObjectMapperFactory.getJsonObjectMapper();
  private static String segAndSohOutputDir;

  public static void main(String[] args) {

    // Read command line args
    final CssWaveformFileWriterCommandLineArgs cmdLineArgs
        = new CssWaveformFileWriterCommandLineArgs();
    CmdLineParser parser = new CmdLineParser(cmdLineArgs);
    try {
      parser.parseArgument(args);
    } catch (Exception ex) {
      logger.error("Error parsing out arguments", ex);
      System.exit(-1);
    }

    //Check Prams for validity
    boolean validated = validateArgs(cmdLineArgs);
    if (!validated) {
      logger.error("Invalid command-line argument(s) received.");
      System.exit(1);
    }

    //Check if outputDirectory exists
    if (!cmdLineArgs.getOutputDir().endsWith(OUTPUT_LEAF_DIR_NAME)) {
      throw new IllegalArgumentException(
          String.format("Data must be output to directory named %s. " +
              "Arg provided was %s", OUTPUT_LEAF_DIR_NAME, cmdLineArgs.getOutputDir()));
    }

    //All checks passed, can convert waveforms now
    try {
      execute(cmdLineArgs);
    } catch (Exception ex) {
      logger.error("Error in Application.execute", ex);
      System.exit(-1);
    }
  }

  /**
   * Performs the load.  Implemented as an instance method since the fields that are annotated with
   *
   * @param args command-line args
   */
  private static void execute(CssWaveformFileWriterCommandLineArgs args)
      throws IOException {

    Objects.requireNonNull(args, "Cannot take null arguments");

    //Check if the output directory already exists, we refuse to write overwrite files for security
    //prepare base dir, argument, for appending the segment and SOH dir
    String baseDirString = args.getOutputDir();
    if (!baseDirString.endsWith(File.separator)) {
      baseDirString += File.separator;
    }

    // add 'segments-and-soh' folder to end of base directory to create full output path
    segAndSohOutputDir = baseDirString + SEGMENTS_AND_SOH_DIR_NAME;
    if (new File(segAndSohOutputDir).exists()) {
      throw new IllegalArgumentException(String.format("Cannot create sub directory %s " +
              "in base directory %s. %s already exists. Please specify a new base " +
              "directory or remove the sub directory", SEGMENTS_AND_SOH_DIR_NAME,
          baseDirString, segAndSohOutputDir));
    }

    // Get arguments and convert into proper formats.
    List<String> stationList = null;
    List<String> channelList = null;
    Instant startTime = null;
    Instant endTime = null;

    // split the comma separated list of stations into a list
    String stationsArg = args.getStations();
    if (stationsArg != null && stationsArg.length() > 0) {
      stationList = Arrays.asList(stationsArg.trim().split(","));
    }

    // split the comma separated list of channels into a list
    String channelsArg = args.getChannels();
    if (channelsArg != null && channelsArg.length() > 0) {
      channelList = Arrays.asList(channelsArg.trim().split(","));
    }

    // parse out the start time from the epoch argument
    long timeEpochArg = args.getTimeEpoch();
    String timeDateArg = args.getTimeDate();
    if (timeEpochArg > -1) {
      startTime = Instant.ofEpochSecond(timeEpochArg);
    } else if (timeDateArg != null && timeDateArg.length() > 0) {
      startTime = Instant.parse(timeDateArg);
    }

    // parse out the end time from the epoch argument
    long endtimeEpochArg = args.getEndtimeEpoch();
    String endtimeDateArg = args.getEndtimeDate();
    if (endtimeEpochArg > -1) {
      endTime = Instant.ofEpochSecond(endtimeEpochArg);
    } else if (endtimeDateArg != null && endtimeDateArg.length() > 0) {
      endTime = Instant.parse(endtimeDateArg);
    }

    // read in the station group records
    Map<String, Channel> processingGroupInfo =
        readProcessingChannelsFile(args.getStationGroupFile());

    // read in the derived channel records
    String wfidChannelMapFile = args.getWfidToChannelFile();
    final ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();
    final JavaType t = mapper.getTypeFactory()
        .constructMapType(HashMap.class, Long.class, Channel.class);
    final Map<Long, Channel> derivedChannelData = mapper.readValue(new File(wfidChannelMapFile), t);

    //read in the wfdisk data
    List<WfdiscRecord> wfdiscs = new FlatFileWfdiscReader(
        stationList, channelList, startTime, endTime).read(args.getWfdiscFile());

    // check if anything from wfdisk was loaded.  If not, log a warning and exit.
    if (wfdiscs.isEmpty()) {
      logger.warn("No records loaded from {}; exiting", args.getWfdiscFile());
      return;
    }

    // create the claim checks from the wfdis and statiomn group and derived channel data
    final List<SegmentClaimCheck> segmentClaimChecks = new WfdiscToSegmentClaimCheckConverter()
        .convert(wfdiscs, processingGroupInfo, derivedChannelData);

    writeFiles(args, segmentClaimChecks);
  }

  /**
   * Handles the file writing
   *
   * @param args the input command line args
   * @param segmentClaimChecks claim checks read from wfdiscs
   * @throws IOException if writing fails
   */
  private static void writeFiles(CssWaveformFileWriterCommandLineArgs args,
      List<SegmentClaimCheck> segmentClaimChecks)
      throws IOException {
    final String waveformsDir = args.getWaveformsDir();
    if (!new File(segAndSohOutputDir).mkdir()) {
      logger.error("Could not create output directory {}", segAndSohOutputDir);
    }

    if (waveformsDir == null) {
      objMapper.writeValue(createFile(segAndSohOutputDir + "segment-claim-checks.json"),
          segmentClaimChecks);
    } else {
      // Load the WF Disc file.
      final SegmentClaimCheckConverter segmentClaimCheckConverter = new SegmentClaimCheckConverter(
          segmentClaimChecks, waveformsDir, args.getBatchSize());
      int batchNumber = 1;
      final List<AcquiredChannelEnvironmentIssue> sohs = new ArrayList<>();
      while (segmentClaimCheckConverter.nextBatchExists()) {
        final WaveformAcquiredChannelSohPair batch = segmentClaimCheckConverter.readNextBatch();
        if (!batch.getAcquiredChannelEnvironmentIssues().isEmpty()) {
          sohs.addAll(batch.getAcquiredChannelEnvironmentIssues());
        }
        if (!batch.getWaveforms().isEmpty()) {
          // write the segment batch to an output file
          objMapper
              .writeValue(createFile(segAndSohOutputDir + "segments-" + batchNumber++ + ".json"),
                  batch.getWaveforms());
        }
      }
      // write the SOH's into a single JSON file
      objMapper.writeValue(createFile(segAndSohOutputDir + "state-of-health.json"), sohs);
    }
    logger.info("Processed {} wfdisc records", segmentClaimChecks.size());
  }

  /**
   * Validate arguments, then print usage and exit if found any problems.
   *
   * @param cmdLineArgs the command-line arguments
   * @return true = arguments validated, false = there is an error.
   */
  private static boolean validateArgs(
      CssWaveformFileWriterCommandLineArgs cmdLineArgs) {

    if (cmdLineArgs.getBatchSize() < 1) {
      logger.error("The batchSize value must be greater than zero.");
      return false;
    }

    if ((cmdLineArgs.getTimeEpoch() > -1) &&
        (cmdLineArgs.getTimeDate() != null && cmdLineArgs.getTimeDate().length() > 0)) {
      logger.error("Cannot use both timeEpoch and timeDate in same call");
      return false;
    }

    if ((cmdLineArgs.getEndtimeEpoch() > -1) &&
        (cmdLineArgs.getEndtimeDate() != null && cmdLineArgs.getEndtimeDate().length() > 0)) {
      logger.error("Cannot use both endtimeEpoch and endtimeDate in same call");
      return false;
    }

    if (cmdLineArgs.getTimeDate() != null && cmdLineArgs.getTimeDate().length() > 0) {
      try {
        Instant.parse(cmdLineArgs.getTimeDate());
      } catch (Exception e) {
        logger.error("Invalid format for timeDate: {}", e.getLocalizedMessage());
        return false;
      }
    }

    if (cmdLineArgs.getEndtimeDate() != null && cmdLineArgs.getEndtimeDate().length() > 0) {
      try {
        Instant.parse(cmdLineArgs.getEndtimeDate());
      } catch (Exception e) {
        logger.error("Invalid format for endtimeDate: {}", e.getLocalizedMessage());
        return false;
      }
    }

    //get the station list and warn user if there are more than 6
    String stations = cmdLineArgs.getStations();
    warnStringListSize(stations, 6);

    //get the channel list and warn user if there are more than 6
    String channels = cmdLineArgs.getChannels();
    warnStringListSize(channels, 8);
    return true;
  }

  /**
   * Logs a warning if there are too many args
   *
   * @param stringListToValidate Comma delimited string of channel or station names
   * @param acceptableSize max acceptable size
   */
  private static void warnStringListSize(String stringListToValidate, int acceptableSize) {
    if (stringListToValidate != null && stringListToValidate.length() > 0 &&
        (stringListToValidate.length() > acceptableSize) && (stringListToValidate.indexOf(',')
        <= 0)) {
      logger.warn("{} is unusually long without any commas, "
              + "but assuming user knows what they are doing and continuing anyaways.",
          stringListToValidate);
    }
  }

  private static File createFile(String path) throws IOException {
    logger.info("Creating file {}", path);
    final File f = new File(path);
    if (!f.createNewFile()) {
      logger.error("Could not create file {}", path);
    }
    return f;
  }

  /**
   * Take the command line arg "processingChannelsFile", deserialize it into a Channel[] object,
   * then create a Map containing every channel and its name. This map is used in the {@link
   * SegmentClaimCheckConverter} to quickly lookup Channels via their name for creation of Channel
   * Segments
   *
   * @param processingChannelsFile The path to the raw-channel.json file
   * @return A Map of every channel name to its Channel
   */
  private static Map<String, Channel> readProcessingChannelsFile(String processingChannelsFile)
      throws IOException {
    final Map<String, Channel> chanNameToChanMap = new HashMap<>();
    Arrays.stream(objMapper.readValue(new File(processingChannelsFile), StationGroup[].class))
        .forEach(stationGroup -> stationGroup.getStations()
            .forEach(stations -> stations.getChannels()
                .forEach(channel -> chanNameToChanMap
                    .put(channel.getName().substring(channel.getName().indexOf('.') + 1),
                        channel))));
    return chanNameToChanMap;
  }
}
