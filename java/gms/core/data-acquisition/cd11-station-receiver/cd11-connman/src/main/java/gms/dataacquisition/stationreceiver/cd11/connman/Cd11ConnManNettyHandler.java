package gms.dataacquisition.stationreceiver.cd11.connman;


import com.google.common.net.InetAddresses;
import gms.dataacquisition.stationreceiver.cd11.common.Cd11FrameFactory;
import gms.dataacquisition.stationreceiver.cd11.common.FrameParsingDecoder;
import gms.dataacquisition.stationreceiver.cd11.common.FrameParsingUtility;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionRequestFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11ConnectionResponseFrame;
import gms.dataacquisition.stationreceiver.cd11.common.frames.Cd11Frame;
import gms.dataacquisition.stationreceiver.cd11.connman.configuration.Cd11ConnManConfig;
import java.io.IOException;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;


/**
 * Server listens to incoming messages in NIO fashion. Each message is checked to see if it conforms
 * to the expected payload and if it does, the server responds to the request to continue the
 * handshake
 */
public class Cd11ConnManNettyHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(Cd11ConnManNettyHandler.class);

  private static final int AUTHENTICATION_KEY_IDENTIFIER = 7;
  private static final short PROTOCOL_MAJOR_VERSION = 2;
  private static final short PROTOCOL_MINOR_VERSION = 0;

  //TODO Consider refactoring this into separate methods
  public BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> handleInboundOutbound(
      Cd11ConnManConfig cd11ConnManConfig,
      Function<String, Cd11Station> cd11StationLookup,
      Map<String, Boolean> ignoredStationsMap) {

    return (nettyInbound, nettyOutbound) ->
        nettyInbound.withConnection(x ->
        x.addHandlerFirst(new FrameParsingDecoder()))
        .receive()
        .asByteBuffer()
        .map(FrameParsingUtility::parseByteBuffer)
        .flatMap(partialFrame -> {
          LOGGER.info(
              "A data provider successfully connected to the Connection Manager, " +
                  "presumably for setting up a new data stream.");

          Cd11FrameFactory cd11FrameFactory = createCd11FrameFactory(cd11ConnManConfig);
          try {
            Cd11Frame cd11Frame = FrameParsingUtility.createCd11Frame(partialFrame);

            // Validate CRC for frame.
            if (!cd11Frame.isValidCRC()) {
              LOGGER.error("CRC check failed for frame!!!");
            }

            // check that we have a connection request frame type
            if (cd11Frame.frameType.equals(Cd11Frame.FrameType.CONNECTION_REQUEST)) {

              // Retrieve the connection request frame.
              Cd11ConnectionRequestFrame connRequestFrame = cd11Frame
                  .asFrameType(Cd11ConnectionRequestFrame.class);

              if (ignoredStationsMap.get(connRequestFrame.stationName) == null) {
                //As this station is NOT set to be ignored, begin processing the request
                processConnectionRequestFrame(connRequestFrame, nettyOutbound,
                    cd11StationLookup, cd11FrameFactory);
              } else {
                LOGGER.debug("Cd11 Request frame for station {} is being ignored.",
                    connRequestFrame.stationName);
              }
            } else {
              LOGGER.debug("Expected Cd11Frame of type {}, but received {}",
                  Cd11Frame.FrameType.CONNECTION_REQUEST,
                  cd11Frame.frameType);
            }
          } catch (IOException e) {
            LOGGER.error("Error reading or parsing frame: ", e);
          }

          return Mono.empty();
        }).then();
  }

  // Create the cd11 frame factory for reading and converting byte data
  private Cd11FrameFactory createCd11FrameFactory(Cd11ConnManConfig cd11ConnManConfig) {
    return Cd11FrameFactory.builderWithDefaults()
            .setAuthenticationKeyIdentifier(AUTHENTICATION_KEY_IDENTIFIER)
            .setFrameCreator(cd11ConnManConfig.getFrameCreator())
            .setFrameDestination(cd11ConnManConfig.getFrameDestination())
            .setProtocolMajorVersion(PROTOCOL_MAJOR_VERSION)
            .setProtocolMinorVersion(PROTOCOL_MINOR_VERSION)
            .setResponderName(cd11ConnManConfig.getResponderName())
            .setResponderType(cd11ConnManConfig.getResponderType())
            .setServiceType(cd11ConnManConfig.getServiceType()).build();
  }

  // Process the connection request frame from client and send response frame
  private void processConnectionRequestFrame(Cd11ConnectionRequestFrame connRequestFrame,
      NettyOutbound nettyOutbound,
      Function<String, Cd11Station> cd11StationLookup,
      Cd11FrameFactory cd11FrameFactory) throws IOException {

    LOGGER.info("Received connection request for station {} at {}:{}",
        connRequestFrame.stationName, connRequestFrame.ipAddress, connRequestFrame.port);

    // Find the station info.
    Cd11Station cd11Station = cd11StationLookup.apply(connRequestFrame.stationName);

    // Check that the station name is known.
    if (cd11Station == null) {
      LOGGER.warn(
          "Connection request received from station {} that has no active configuration; ignoring connection.",
          connRequestFrame.stationName);
    } else {

      // Check that the request originates from the expected IP Address.
      LOGGER.info("Connection Request received from station {}. Redirecting station to {}:{} ",
          connRequestFrame.stationName,
          cd11Station.dataConsumerIpAddress,
          cd11Station.dataConsumerPort);

      // Send out the Connection Response Frame.
      String consumerAddressIp = InetAddresses.toAddrString(cd11Station.dataConsumerIpAddress);
      LOGGER.info("Configured data consumer retrieved from cd11Station, resolved IP: {}",
          consumerAddressIp);

      // Create the Cd11ConnectionResponseFrame with the frame factory
      Cd11ConnectionResponseFrame cd11ConnectionResponseFrame = cd11FrameFactory.
          createCd11ConnectionResponseFrame(cd11Station.dataConsumerIpAddress,
              cd11Station.dataConsumerPort, null, null);

      // Send the response frame back to the client for connecting station
      nettyOutbound.sendByteArray(Mono.just(cd11ConnectionResponseFrame.toBytes()))
          .then()
          .subscribe();

      LOGGER.info("Connection Response Frame sent to station {}.",
          connRequestFrame.stationName);
    }

  }
}

