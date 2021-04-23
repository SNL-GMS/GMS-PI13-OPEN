package gms.shared.frameworks.osd.coi.event;

public enum MagnitudeModel {
  RICHTER("Richter"),
  VEITH_CLAWSON("VeithClawson72"),
  REZAPOUR_PEARCE("Reazpour-Pearce"),
  NUTTLI("Nuttli"),
  UNKNOWN("Unknown");

  private final String earthModel;

  MagnitudeModel(String earthModel) {
    this.earthModel = earthModel;
  }

  public String getEarthModel() {
    return earthModel;
  }

}
