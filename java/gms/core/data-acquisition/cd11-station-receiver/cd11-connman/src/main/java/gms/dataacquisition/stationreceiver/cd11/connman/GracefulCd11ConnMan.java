package gms.dataacquisition.stationreceiver.cd11.connman;

import com.google.common.collect.EvictingQueue;
import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.connman.configuration.Cd11ConnManConfig;
import gms.dataacquisition.stationreceiver.cd11.connman.configuration.Cd11ConnectionConfig;
import gms.dataacquisition.stationreceiver.cd11.connman.util.Cd11ConnManUtil;
import gms.shared.frameworks.control.ControlContext;
import gms.shared.frameworks.systemconfig.SystemConfig;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * CD11 ConnMan TCP Server using Java Server Sockets
 */
public class GracefulCd11ConnMan implements Cd11ConnMan {
    private static final Logger logger = LoggerFactory.getLogger(GracefulCd11ConnMan.class);

    private Cd11ConnManConfig cd11ConnManProcessingConfiguration;

    private SystemConfig cd11ConnManSystemConfiguration;
    private int connectionManagerWellKnownPort;

    private static final String CONNECTION_MANAGER_PORT_KEY = "connection-manager-well-known-port";
    private static final String DATA_MANAGER_ADDRESS_KEY = "data-manager-ip-address";
    private static final String DATA_PROVIDER_KEY = "data-provider-ip-address";

    public static final int MAX_CONNECTION_LOGS = 100;

    private volatile boolean keepRunning = true;

    private ServerSocket serverSocket;
    private ConcurrentMap<String, Cd11Station> cd11StationsLookup;
    private EvictingQueue<ConnectionLog> connectionLogs;
    // TODO: In the future, this should come from the OSD, on a per-station basis!
    //  set this via dataProviderIpAddress

    private Map<String, InetAddress> inetAddressMap;

    private Map<String, Boolean> ignoredStationsMap;


    private GracefulCd11ConnMan(SystemConfig systemConfig,
                                Cd11ConnManConfig processingConfig) {

        this.cd11ConnManSystemConfiguration = systemConfig;
        this.cd11ConnManProcessingConfiguration = processingConfig;

        // Initialize the data structures.
        cd11StationsLookup = new ConcurrentHashMap<>();
        ignoredStationsMap = new HashMap<>();

        inetAddressMap = new HashMap<>();
        inetAddressMap.put(DATA_MANAGER_ADDRESS_KEY, null);
        inetAddressMap.put(DATA_PROVIDER_KEY, null);

        // Initialize the connection log.
        connectionLogs = EvictingQueue.create(MAX_CONNECTION_LOGS);

        connectionManagerWellKnownPort = cd11ConnManSystemConfiguration
                .getValueAsInt(CONNECTION_MANAGER_PORT_KEY);
        logger.info(
                "Using well known port of {} to receive queries from stations looking to create a data connection",
                connectionManagerWellKnownPort);

        Runtime.getRuntime().addShutdownHook(new Thread(this::onStop));
    }

    /**
     * Create {@link GracefulCd11ConnMan} from config objects {@link ControlContext}
     * NOTE: This is used in unit tests for Mockup ConnMan services
     *
     * @param systemConfig the system config, not null
     * @param processingConfig the processing config , not null
     * @return an instance of Cd11ConnManServer
     */
    public static GracefulCd11ConnMan create(SystemConfig systemConfig,
                                             Cd11ConnManConfig processingConfig) {
        checkNotNull(processingConfig, "Cannot create Cd11ConnManServer with null processing config");
        checkNotNull(systemConfig, "Cannot create Cd11ConnManServer with null system config");
        return new GracefulCd11ConnMan(systemConfig, processingConfig);
    }

    @Override
    public void start() {
        validateRequiredServicesRegisteredFailsafe();

        // Query the OSD for all registered CD 1.1 stations.
        this.initializeCd11StationsLookup();

        // Create a ServerSocket to listen for Data Provider connection requests.
        try {
            serverSocket = new ServerSocket(connectionManagerWellKnownPort);
            logger.info("Server Socket created using well-known port {}", connectionManagerWellKnownPort);
        } catch (IOException e) {
            logger.error("Error binding to socket on port {}: {}", connectionManagerWellKnownPort, e);
            return;
        }

        logger.info(
                "Connection Manager is now listening on well-know port {} for incoming data connection requests",
                connectionManagerWellKnownPort);

        // Listen for incoming connections.
        while (keepRunning) {
            try {
                // Listen for the desired connection to arrive.
                listenToConnection();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void listenToConnection() throws IOException {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            // Blocking call.
            socket.setSoLinger(true, 3);
            logger.info(
                    "A data provider successfully connected to the Connection Manager, presumably for setting up a new data stream.");
            // Pass the socket connection to a Connection Handler.
            startCd11Connection(socket, ignoredStationsMap);
        } catch (SocketException e) {
            logger.info("ConnMan received a SocketException, shutting down.");
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                logger.error("Socket threw IOException during close.");
            }
            throw e;

        } catch (Exception e) {
            logger.error(
                    "ConnMan's server socket threw an exception while listening for new connections. Closing socket.");
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                logger.error("Socket threw IOException during close.", ex);
            }
            throw e;
        }
    }

    private void startCd11Connection(Socket socket, Map<String, Boolean> ignoredStationsMap) {
        try {
            Cd11ConnectionConfig cd11ConnectionConfig = Cd11ConnectionConfig
                    .builder()
                    .setFrameCreator(cd11ConnManProcessingConfiguration.getFrameCreator())
                    .setFrameDestination(cd11ConnManProcessingConfiguration.getFrameDestination())
                    .setResponderName(cd11ConnManProcessingConfiguration.getResponderName())
                    .setResponderType(cd11ConnManProcessingConfiguration.getResponderType())
                    .setServiceType(cd11ConnManProcessingConfiguration.getServiceType())
                    .build();
            Cd11Connection cd11Connection = new Cd11Connection(
                    cd11ConnectionConfig,
                    socket,
                    this::lookupCd11Station,
                    this::addConnectionLog,
                    ignoredStationsMap);
            cd11Connection.start();
        } catch (Exception e) {
            logger.error("Cd11Connection thread could not be started.", e);
        }
    }

    /**
     * Closes the server socket, since it blocks on the accept() method.
     */
    public void onStop() {

        keepRunning = false;
        // Close the server socket, if it is listening for connections.
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                // Close the ServerSocket, which will break the a blocking call to serverSocket.accept().
                serverSocket.close();
                logger.info("Server socket for Cd11ConnManService closed.");
            } catch (Exception e) {
                // Do nothing.
                logger.error("Error closing the server socket for Cd11ConnManService: ", e);
            }
        }
    }

    //-------------------- CD 1.1 Station Registration Methods --------------------

    private void initializeCd11StationsLookup() {

        //need to only add stations that are being acquired, ie isAcquired set to true

        cd11ConnManProcessingConfiguration.getCd11StationParameters()
                .forEach(cd11Param -> {
                    if (cd11Param.isAcquired()) {
                        this.addCd11Station(
                                cd11Param.getStationName(),
                                // TODO: This provider IP address will be used in the future for validation, currently unused logically
                                inetAddressMap.get(DATA_PROVIDER_KEY),
                                inetAddressMap.get(DATA_MANAGER_ADDRESS_KEY),
                                cd11Param.getPort());
                    } else {
                        //Check if we have added this station to the ignored map,
                        // meaning we don't need to log for the nth time when stations attempt to connect
                        if (!ignoredStationsMap.containsKey(cd11Param.getStationName())) {
                            logger.info(
                                    "Station {} is configured to not be acquired, ignoring connection requests",
                                    cd11Param.getStationName());
                            ignoredStationsMap.put(cd11Param.getStationName(), true);
                        }
                    }
                });
    }

    private void initializeInetAddresses() {
        // Replace key value pairs of ip addresses using system config
        Cd11ConnManUtil.replaceInetAdresses(inetAddressMap, cd11ConnManSystemConfiguration);
    }

    private void validateRequiredServicesRegisteredFailsafe() {
        // check to see if we can resolve data consumer information
        final RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withBackoff(1, 60, ChronoUnit.SECONDS)
                .withMaxAttempts(15)
                .handle(List.of(IllegalStateException.class))
                .onFailedAttempt(e -> logger.warn(
                        "Invalid state, necessary hosts may be unavailable: {}, checking again",
                        e));
        Failsafe.with(retryPolicy).run(this::initializeInetAddresses);
    }

    /**
     * Register a new CD 1.1 station.
     *
     * @param stationName Name of the station.
     * @param expectedDataProviderIpAddress Expected IP Address of the Data Provider connecting to
     * this Connection Manager.
     * @param dataConsumerIpAddress IP Address of the Data Consumer to redirect this request to.
     * @param dataConsumerPort Port number of the Data Consumer to redirect this request to.
     */
    public void addCd11Station(
            String stationName,
            InetAddress expectedDataProviderIpAddress,
            InetAddress dataConsumerIpAddress,
            int dataConsumerPort) {

        // Add a new station to the list, or replace an existing station.
        cd11StationsLookup.put(stationName, new Cd11Station(
                expectedDataProviderIpAddress,
                dataConsumerIpAddress,
                dataConsumerPort));
    }

    /**
     * Remove a registered CD 1.1 Station.
     *
     * @param stationName Remove a CD 1.1 station.
     */
    public void removeCd11Station(String stationName) {
        // Remove the station, if it exists.
        cd11StationsLookup.remove(stationName);
    }

    /**
     * Looks up the CD 1.1 Station, and returns connection information (or null if it does not
     * exist).
     *
     * @param stationName Name of the station.
     * @return Cd11Station info.
     */
    public Cd11Station lookupCd11Station(String stationName) {
        return cd11StationsLookup.getOrDefault(stationName, null);
    }

    /**
     * Returns the total number of registered CD 1.1 stations.
     *
     * @return Total stations.
     */
    public int getTotalCd11Stations() {
        return cd11StationsLookup.size();
    }

    //-------------------- Connection Log Methods --------------------

    private synchronized void addConnectionLog(ConnectionLog connectionLog) {
        this.connectionLogs.add(connectionLog);
    }

    /**
     * Returns the current snapshot of the connection logs.
     *
     * @return Connection logs.
     */
    public synchronized ConnectionLog[] getConnectionLogs() {
        return connectionLogs.toArray(new ConnectionLog[0]);
    }

    /**
     * The total number of connection logs stored up to the current moment.
     *
     * @return Total connections.
     */
    public synchronized int getTotalConnectionLogs() {
        return connectionLogs.size();
    }

    /**
     * The total number of valid connections that currently exist in the connection logs.
     *
     * @return Total connections.
     */
    public synchronized long getTotalValidConnectionLogs() {
        return connectionLogs.stream()
                .filter(x -> x.isValidConnectionRequest)
                .count();
    }

    /**
     * The total number of invalid connections that currently exist in the connection logs.
     *
     * @return Total connections.
     */
    public synchronized long getTotalInvalidConnectionLogs() {
        return connectionLogs.stream()
                .filter(x -> !x.isValidConnectionRequest)
                .count();
    }
}
