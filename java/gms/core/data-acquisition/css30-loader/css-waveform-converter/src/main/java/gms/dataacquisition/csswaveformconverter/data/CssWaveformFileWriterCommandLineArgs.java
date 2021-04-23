package gms.dataacquisition.csswaveformconverter.data;

import org.kohsuke.args4j.Option;

/**
 * Created by jwvicke on 11/28/17.
 */
public class CssWaveformFileWriterCommandLineArgs {

  @Option(name = "-wfDiscFile", required = true, usage = "Path to CSS wfdiscfile")
  private String wfdiscFile;

  @Option(name = "-waveformsDir", usage = "Location of the waveform files. If not specified, a file is written that references the .w files instead of the values.")
  private String waveformsDir = null;

  @Option(name = "-stationGroupFile", required = true, usage = "Path to a JSON file containing a list of the Station Groups created by the CSSReferenceReader")
  private String stationGroupFile;

  @Option(name = "-wfidToChannelFile", required = true, usage = "Path to a JSON file containing a mapping of wfid to derived channel object a map Map<Long , Channel>")
  private String wfidToChannelFile;

  @Option(name = "-outputDir", required = true, usage = "Output directory for data files from the converter")
  private String outputDir;

  @Option(name = "-batchSize", usage = "Sets the maximum number of updates to transmit to the OSD Gateway Service in a single transaction (default 1).")
  private int batchSize = 1;

  @Option(name = "-batchInterval", usage = "Number of milliseconds to wait before sending the next batch of data to the OSD Gateway Service (default 0).")
  private int batchInterval = 0;

  @Option(name = "-stations", usage = "Comma-separated list of stations")
  private String stations = "";

  @Option(name = "-channels", usage = "Comma-separated list of channels")
  private String channels = "";

  @Option(name = "-timeEpoch", usage = "Start time in epoch seconds to load waveforms")
  private long timeEpoch = -1;

  @Option(name = "-timeDate", usage = "Start time in date format YYYY-MM-DDTHH:MM:SS.SSSZ to load waveforms")
  private String timeDate = "";

  @Option(name = "-endtimeEpoch", usage = "End time in epoch seconds to load waveforms")
  private long endtimeEpoch = -1;

  @Option(name = "-endtimeDate", usage = "End time in date format YYYY-MM-DDTHH:MM:SS.SSSZ to load waveforms")
  private String endtimeDate = "";

  public String getWfdiscFile() {
    return wfdiscFile;
  }

  public String getWaveformsDir() {
    return waveformsDir;
  }

  public String getStationGroupFile() { return stationGroupFile; }

  public String getWfidToChannelFile() { return wfidToChannelFile; }

  public String getOutputDir() { return outputDir; }

  public int getBatchSize() {
    return batchSize;
  }

  public String getStations() {
    return stations;
  }

  public String getChannels() {
    return channels;
  }

  public long getTimeEpoch() {
    return timeEpoch;
  }

  public String getTimeDate() {
    return timeDate;
  }

  public long getEndtimeEpoch() {
    return endtimeEpoch;
  }

  public String getEndtimeDate() {
    return endtimeDate;
  }
}
