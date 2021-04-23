package gms.dataacquisition.stationreceiver.cd11.connman.util;

import com.google.common.net.InetAddresses;
import gms.shared.frameworks.systemconfig.SystemConfig;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;

/**
 * Cd11 ConnMan Utility for shared ConnMan methods
 */
public class Cd11ConnManUtil {
    private static final Logger logger = LoggerFactory.getLogger(Cd11ConnManUtil.class);

    private Cd11ConnManUtil() {}

    public static void replaceInetAdresses(Map<String, InetAddress> inetAddressHashMap,
                                           SystemConfig cd11ConnManSystemConfiguration) {
        // Replace key value pairs of ip addresses using system config
        inetAddressHashMap.replaceAll((k, v) -> {
            if (v == null) {
                var ipAddress = Objects
                        .requireNonNull(cd11ConnManSystemConfiguration.getValue(k));
                logger.info("Configured {} with address {} ", k, ipAddress);

                try {
                    // create InetAddress from IP address literal
                    logger.info("Attempting to get InetAddress using IP address literal {}", ipAddress);
                    v = InetAddresses.forString(ipAddress);
                } catch (IllegalArgumentException e) {
                    logger.info("Unable to get InetAddress using {}, cause: {}", ipAddress, e.getMessage());
                    try {
                        // create InetAddress from hostname
                        logger.info("Attempting to get InetAddress by name using {}", ipAddress);
                        v = InetAddress.getByName(ipAddress);
                        logger.info("Successfully obtained InetAddress using {}", ipAddress);
                    } catch (UnknownHostException ex) {
                        throw new IllegalStateException("Problem obtaining InetAddress: ", ex);
                    }
                }
            }
            return v;
        });

    }
}
