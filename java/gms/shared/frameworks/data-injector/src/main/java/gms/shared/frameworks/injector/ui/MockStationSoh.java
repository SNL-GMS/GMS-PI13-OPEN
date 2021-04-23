package gms.shared.frameworks.injector.ui;

import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.soh.SohMonitorType;
import gms.shared.frameworks.osd.coi.soh.SohStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.Validate;

/**
 * MockStationSOH class Mocks changes to the SOH values for a station. This then
 * generates a {@link Station}. MockStationSOHs randomly walk their values on
 * update, based on their variability.
 */
class MockStationSOH {

    private static int mockNumber = 0;

    private double variability;

    private UUID id;

    private SohStatus status;

    private long lag;

    private double missing;

    private long timeliness;

    private double environment;

    private long baseLag;

    private double baseMissing;

    private long baseTimeliness;

    private double baseEnvironment;

    private ArrayList<SohMonitorType> contributingMonitorTypes;

    private final Station station;

    /**
     * getter for the {@link UUID} that the mock station updates
     *
     * @return the UUID
     */
    UUID getId() {
        return id;
    }

    /**
     * setter for the {@link UUID} that the mock station updates
     *
     * @param id the UUID to set
     */
    void setId(UUID id) {
        this.id = id;
    }

    /**
     * Get a Set of {@link SohMonitorType} that contribute to this station's rollup
     * status.
     *
     * @return a Set of the {@link SohMonitorType}s that contribut
     */
    Set<SohMonitorType> getContributingMonitorTypes() {
        return new HashSet<>(contributingMonitorTypes);
    }

    /**
     * getter for the {@link Station} that the mock station updates
     *
     * @return the Station
     */
    Station getStation() {
        return station;
    }

    /**
     * getter for the {@link SohStatus} rollup value
     *
     * @return the status rollup for the station
     */
    SohStatus getStatus() {
        return status;
    }

    /**
     * getter for the lag value
     *
     * @return the lag value
     */
    long getLag() {
        return lag;
    }

    /**
     * getter for the missing value
     *
     * @return the missing value
     */
    double getMissing() {
        return missing;
    }

    /**
     * getter for the timeliness value
     *
     * @return the timeliness value
     */
    long getTimeliness() {
        return timeliness;
    }

    /**
     * getter for the environment value
     *
     * @return the environment value
     */
    double getEnvironment() {
        return environment;
    }

    /**
     * Constructor generates a MockStationSoh object MockStationSOH stations
     * randomly walk a random set of monitor values. The more variable the station,
     * the more the values will change on update.
     *
     * //@param fileName the base station file to use //@param stationName the
     * station name to use (overwrites the name in the file)
     *
     * @param variability a double between 0 and 1 that determines how dramatically
     *                    the station changes on update
     * @param lag     the initial lag value to start with
     * @param missing     the initial missing value to start with
     * @param environment the initial environment value to start with
     */
    MockStationSOH(Station station, double variability, long lag, double missing, long timeliness,
            double environment) {
        this.station = station;
        this.variability = variability;
        this.lag = lag;
        this.missing = missing;
        this.timeliness = timeliness;
        this.environment = environment;

        this.baseLag = (long) (this.lag * this.variability);
        this.baseMissing = this.missing * this.variability;
        this.baseTimeliness = (long) (this.timeliness * this.variability);
        this.baseEnvironment = this.environment * this.variability;

        final double contributingChance = 1 - (Math.random() * this.variability * .5);
        this.contributingMonitorTypes = new ArrayList<>();
        if (Math.random() < contributingChance) {
            this.contributingMonitorTypes.add(SohMonitorType.LAG);
        }
        if (Math.random() < contributingChance) {
            this.contributingMonitorTypes.add(SohMonitorType.TIMELINESS);
        }
        if (Math.random() < contributingChance) {
            this.contributingMonitorTypes.add(SohMonitorType.MISSING);
        }
        if (Math.random() < contributingChance) {
            this.contributingMonitorTypes.add(SohMonitorType.TIMELINESS);
        }
        if (Math.random() < contributingChance || this.contributingMonitorTypes.isEmpty()) {
            Arrays.stream(SohMonitorType.values())
                    .filter(sohMonitorType -> sohMonitorType.isEnvironmentIssue()
                            && (sohMonitorType.getSohValueType() != SohMonitorType.SohValueType.INVALID))
                    .forEach(monitor -> this.contributingMonitorTypes.add(monitor));
        }
        update();
    }

    /**
     * Returns a {@link SohStatus} for the provided monitor type and value.
     *
     * @param sohMonitorType the {@link SohMonitorType} corresponding to the value
     * @param value          the value of the monitor
     * @return the status for this value and monitor type
     */
    static SohStatus getStatusForSohMonitorValue(SohMonitorType sohMonitorType, Number value) {

        Validate.notNull(sohMonitorType, "null monitor type");

        final List<SohMonitorType> monitorTypes = Arrays.asList(SohMonitorType.values());
        Validate.isTrue(monitorTypes.contains(sohMonitorType));

        if (sohMonitorType == SohMonitorType.LAG || sohMonitorType == SohMonitorType.TIMELINESS) {
            if (value.longValue() == -1) {
                return SohStatus.MARGINAL;
            }
            if (value.longValue() > 7000) {
                return SohStatus.BAD;
            }
            if (value.longValue() > 3500) {
                return SohStatus.MARGINAL;
            }
            return SohStatus.GOOD;
        } else {
            if (value.doubleValue() > 90.0) {
                return SohStatus.BAD;
            }
            if (value.doubleValue() > 70.0) {
                return SohStatus.MARGINAL;
            }
            return SohStatus.GOOD;
        }
    }

    /**
     * Generates a number between 1 and 2, or -1 and -2, with a higher probability
     * of numbers farther from 0 depending on the variability. This multiplier is
     * used to randomly walk the values of the monitor types
     *
     * @return the double multiplier value
     */
    private double getMultiplier() {
        double multiplier = Math.random() * this.variability + 1;
        if (Math.random() > 0.5) {
            multiplier *= -1;
        }
        return multiplier;
    }

    /**
     * Updates this station, with each monitor type having a chance of updating. The
     * more variable the station, the more likely that each monitor type will
     * change, and the more drastically it will change.
     * <p>
     * For each monitor type that is changed, the underlying data undergoes a random
     * walk, moving some amount up or down, with more variable stations having a
     * higher range they can move.
     * <p>
     * Logs the values of the station's monitor types, and the status.
     */
    void update() {

        if (Math.random() > this.variability) {
            lag = Math.min(Math.max(0, this.lag + (long) (this.baseLag * getMultiplier())), 10000);

            // Set every 200th to unknown (value -1)
            if (Math.random() > 0.95) {
                lag = -1;
            }
        }
        if (Math.random() > this.variability) {
            this.missing = Math.min(Math.max(0, this.missing + (this.baseMissing * getMultiplier())), 100);
        }
        if (Math.random() > this.variability) {
            timeliness = Math.min(Math.max(0, this.timeliness + (long) (this.baseTimeliness * getMultiplier())), 10000);

            // Set every 200th to unknown (value -1)
            if (Math.random() > 0.95) {
                lag = -1;
            }
        }
        if (Math.random() > this.variability) {
            this.environment = Math.min(Math.max(0, this.environment + (this.baseEnvironment * getMultiplier())), 100);
        }

        final boolean lagEnabled = this.contributingMonitorTypes.contains(SohMonitorType.LAG);
        final boolean missingEnabled = this.contributingMonitorTypes.contains(SohMonitorType.MISSING);
        final boolean timelinessEnabled = this.contributingMonitorTypes.contains(SohMonitorType.TIMELINESS);
        final boolean environmentEnabled = this.contributingMonitorTypes
                .contains(SohMonitorType.ENV_BACKUP_POWER_UNSTABLE);

        this.status = SohStatus.GOOD;
        if (lagEnabled && lag > 3500 || missingEnabled && this.missing > 70.0
                || timelinessEnabled && timeliness > 3500
                || environmentEnabled && this.environment > 70.0) {
            this.status = SohStatus.MARGINAL;
        }
        if (lagEnabled && this.lag > 7000 || missingEnabled && this.missing > 90.0
                || timelinessEnabled && this.timeliness > 7000
                || environmentEnabled && this.environment > 90.0) {
            this.status = SohStatus.BAD;
        }

        // When lag is not available the status is always Marginal
        if (lag == -1) {
            this.status = SohStatus.MARGINAL;
        }
    }

    public static String generateMockStationName() {
        return "MOCK" + ++mockNumber;
    }
}
