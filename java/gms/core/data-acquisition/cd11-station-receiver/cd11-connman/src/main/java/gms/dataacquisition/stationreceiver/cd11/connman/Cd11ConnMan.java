package gms.dataacquisition.stationreceiver.cd11.connman;

/**
 * Connman class responsible for listening on a well known port for incoming station requests
 */
public interface Cd11ConnMan {
    /**
     * Run the necessary setup for Connman including experimental versions if applicable
     */
    void start();
}
