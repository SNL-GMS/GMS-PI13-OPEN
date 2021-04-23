package gms.dataacquisition.css.processingconverter.commandline;

import org.kohsuke.args4j.Option;


public class CssEventAndSignalDetectionConverterArguments {

  @Option(name = "-wfidToChannel", required = true, usage = "Path to JSON file mapping wfid=>Channel")
  private String wfidToChannelFile;

  @Option(name = "-aridToWfid", required = true, usage = "Path to JSON file mapping arid=>wfid")
  private String aridToWfidFile;

  @Option(name = "-event", required = true, usage = "Path to CSS event file")
  private String eventFile;

  @Option(name = "-origin", required = true, usage = "Path to CSS origin file")
  private String originFile;

  @Option(name = "-origerr", required = true, usage = "Path to CSS origerr file")
  private String origerrFile;

  @Option(name = "-assoc", required = true, usage = "Path to CSS assoc file")
  private String assocFile;

  @Option(name = "-arrival", required = true, usage = "Path to CSS arrival file")
  private String arrivalFile;

  @Option(name = "-amplitude", required = true, usage = "Path to CSS amplitude file")
  private String amplitudeFile;

  @Option(name = "-netmag", required = true, usage = "Path to CSS netmag file")
  private String netmagFile;

  @Option(name = "-stamag", required = true, usage = "Path to CSS stamag file")
  private String stamagFile;

  @Option(name = "-wfdisc", required = true, usage = "Path to CSS wfdisc file")
  private String wfdiscFile;

  @Option(name = "-outputDir", usage = "Output directory for data files from the converter")
  private String outputDir;

  public String getWfidToChannelFile() {
    return wfidToChannelFile;
  }

  public String getAridToWfidFile() { return aridToWfidFile; }

  public String getEventFile() {
    return eventFile;
  }

  public String getOriginFile() {
    return originFile;
  }

  public String getOrigerrFile() {
    return origerrFile;
  }

  public String getAssocFile() {
    return assocFile;
  }

  public String getArrivalFile() {
    return arrivalFile;
  }

  public String getAmplitudeFile() {
    return amplitudeFile;
  }

  public String getNetmagFile() {
    return netmagFile;
  }

  public String getStamagFile() {
    return stamagFile;
  }

  public String getWfdiscFile() {
    return wfdiscFile;
  }

  public String getOutputDir() {
    return outputDir;
  }
}
