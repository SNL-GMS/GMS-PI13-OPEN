package gms.dataacquisition.stationreceiver.cd11.connman;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import gms.dataacquisition.stationreceiver.cd11.connman.configuration.Cd11ConnManConfig;
import gms.dataacquisition.stationreceiver.cd11.connman.util.Cd11ConnManUtil;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.net.InetAddress;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpServer;

/**
 * Cd11 ConnMan TCP Server using Reactor Netty
 */
public class ReactorCd11ConnMan implements Cd11ConnMan {
    private static final Logger logger = LoggerFactory.getLogger(ReactorCd11ConnMan.class);

    // Connman and System config
    private Cd11ConnManConfig cd11ConnManProcessingConfiguration;
    private SystemConfig cd11ConnManSystemConfiguration;
    private int connectionManagerWellKnownPort;

    // Connman configuration keys
    private static final String CONNECTION_MANAGER_PORT_KEY = "connection-manager-well-known-port";
    private static final String DATA_MANAGER_ADDRESS_KEY = "data-manager-ip-address";
    private static final String DATA_PROVIDER_KEY = "data-provider-ip-address";

    // Number of retries for restarting the Netty server
    private static int failureRetries = 4;

    // Cd11Station lookup and inet addresses for ports
    private ImmutableMap<String, Cd11Station> cd11StationsLookup;
    private ImmutableMap<String, InetAddress> inetAddressMap;
    private ImmutableMap<String, Boolean> ignoredStationsMap;

    // TCP Server and inbound/outbound handler
    private final Cd11ConnManNettyHandler handler = new Cd11ConnManNettyHandler();

    private ReactorCd11ConnMan(SystemConfig systemConfig,
                               Cd11ConnManConfig processingConfig) {

        // Configuration setup
        this.cd11ConnManSystemConfiguration = systemConfig;
        this.cd11ConnManProcessingConfiguration = processingConfig;

        connectionManagerWellKnownPort = cd11ConnManSystemConfiguration
                .getValueAsInt(CONNECTION_MANAGER_PORT_KEY);
        logger.info(
                "Using well known port of {} to receive queries from stations looking to create a data connection",
                connectionManagerWellKnownPort);
    }

    /**
     * Factory method for creating ConnMan
     *
     * @param systemConfig System configuration
     * @param cd11ConnManConfig ConnMan configuration
     * information and channel information
     * @return The processor
     */
    public static ReactorCd11ConnMan create(
            SystemConfig systemConfig,
            Cd11ConnManConfig cd11ConnManConfig) {
        checkNotNull(cd11ConnManConfig, "Cannot create Cd11ConnManServer with null processing config");
        checkNotNull(systemConfig, "Cannot create Cd11ConnManServer with null system config");

        return new ReactorCd11ConnMan(systemConfig, cd11ConnManConfig);
    }

    /**
     * Starts the Netty application using the Controls Framework
     */
    public void start() {

        // Initialize Inet Addresses using the Connman System config
        validateRequiredServicesRegisteredFailsafe();

        // Query the OSD for all registered CD 1.1 stations.
        this.initializeCd11StationsLookup();

        // Start the Netty TCP Server
        Mono<?> monoTcpServer = TcpServer.create()
                .port(connectionManagerWellKnownPort)
                .wiretap(logger.isDebugEnabled())
                .handle(handler.handleInboundOutbound(cd11ConnManProcessingConfiguration,
                        this::lookupCd11Station,
                        ignoredStationsMap))
                .metrics(true)
                .doOnConnection(connection -> {
                    logger.info(
                            "Connection Manager is now listening on well-known port {} for incoming data connection requests",
                            connectionManagerWellKnownPort);

                    connection.onDispose(() ->
                            logger.debug("Connection Manager closed on well-known port {}", connectionManagerWellKnownPort));
                })
                .bind();

        // Retry on failure or error on initial connection
        monoTcpServer.retry(failureRetries)
                .doOnError(e -> logger.error("Netty retries failed:", e))
                .doOnSuccess(s -> logger.info("Successfully connected on well-known port {}", connectionManagerWellKnownPort))
                .subscribe();

    }

    //-------------------- CD 1.1 Station Registration Methods --------------------

    /**
     * Register a new CD 1.1 station.
     *
     * @param stationName Name of the station.
     * @param expectedDataProviderIpAddress Expected IP Address of the Data Provider connecting to
     * this Connection Manager.
     * @param dataConsumerIpAddress IP Address of the Data Consumer to redirect this request to.
     * @param dataConsumerPort Port number of the Data Consumer to redirect this request to.
     */
    private void addCd11Station(
            Map<String, Cd11Station> cd11StationMap,
            String stationName,
            InetAddress expectedDataProviderIpAddress,
            InetAddress dataConsumerIpAddress,
            int dataConsumerPort) {

        // Add a new station to the list, or replace an existing station.
        cd11StationMap.put(stationName, new Cd11Station(
                expectedDataProviderIpAddress,
                dataConsumerIpAddress,
                dataConsumerPort));
    }


    // Initialize the cd11 stations lookup map
    private void initializeCd11StationsLookup() {

        // Initialize the data structures.
        Map<String, Boolean> ignoredStationsHashMap = new HashMap<>();
        Map<String, Cd11Station> cd11StationHashMap = new HashMap<>();

        //need to only add stations that are being acquired, ie isAcquired set to true
        // TODO: This provider IP address will be used in the future for validation, currently unused logically
        cd11ConnManProcessingConfiguration.getCd11StationParameters()
                .forEach(cd11Param -> {
                    if (cd11Param.isAcquired()) {
                        this.addCd11Station(
                                cd11StationHashMap,
                                cd11Param.getStationName(),
                                inetAddressMap.get(DATA_PROVIDER_KEY),
                                inetAddressMap.get(DATA_MANAGER_ADDRESS_KEY),
                                cd11Param.getPort());
                    } else {
                        // Check if we have added this station to the ignored map,
                        // meaning we don't need to log for the nth time when stations attempt to connect
                        if (!ignoredStationsHashMap.containsKey(cd11Param.getStationName())) {
                            logger.info(
                                    "Station {} is configured to not be acquired, ignoring connection requests",
                                    cd11Param.getStationName());
                            ignoredStationsHashMap.put(cd11Param.getStationName(), true);
                        }
                    }
                });

        ignoredStationsMap = ImmutableMap.copyOf(ignoredStationsHashMap);
        cd11StationsLookup = ImmutableMap.copyOf(cd11StationHashMap);

        logger.info("Ignored stations map size (HashMap/Immutable): {} - {}",
                ignoredStationsHashMap.size(), ignoredStationsMap.size());
        logger.info("Cd11 stations map size (HashMap/Immutable): {} - {}",
                cd11StationHashMap.size(), cd11StationsLookup.size());
    }

    /**
     * Initialize inet addresses using the Connman System Config
     */
    private void initializeInetAddresses() {
        // Initialize the inet addresses for dataman and data provider
        Map<String, InetAddress> inetAddressHashMap = new HashMap<>();
        inetAddressHashMap.put(DATA_MANAGER_ADDRESS_KEY, null);
        inetAddressHashMap.put(DATA_PROVIDER_KEY, null);

        // Replace key value pairs of ip addresses using system config
        Cd11ConnManUtil.replaceInetAdresses(inetAddressHashMap, cd11ConnManSystemConfiguration);

        inetAddressMap = ImmutableMap.copyOf(inetAddressHashMap);
        logger.info("Inet addresses map size (HashMap/Immutable): {} - {}",
                inetAddressHashMap.size(), inetAddressMap.size());
    }

    /**
     * Looks up the CD 1.1 Station, and returns connection information (or null if it does not
     * exist).
     *
     * @param stationName Name of the station.
     * @return Cd11Station info.
     */
    private Cd11Station lookupCd11Station(String stationName) {
        return cd11StationsLookup.getOrDefault(stationName, null);
    }

    /**
     * Check to see if we can resolve data consumer information
     */
    private void validateRequiredServicesRegisteredFailsafe() {
        logger.info("Validate required service registered file!");
        final RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .withMaxAttempts(15)
                .handle(List.of(IllegalStateException.class))
                .onFailedAttempt(e -> logger.warn(
                        "Invalid state, necessary hosts may be unavailable: {}, checking again",
                        e));
        Failsafe.with(retryPolicy).run(this::initializeInetAddresses);
    }
}
