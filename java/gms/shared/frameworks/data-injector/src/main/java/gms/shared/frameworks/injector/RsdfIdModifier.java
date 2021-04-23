package gms.shared.frameworks.injector;

import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RsdfIdModifier implements Modifier<Iterable<RawStationDataFrame>> {

    private static final Logger logger = LoggerFactory.getLogger(RsdfIdModifier.class);

    private Instant startTime = null;
    private Instant endTime = null;
    private Instant receiptTime = null;

    private final Duration interval;

    public RsdfIdModifier() {
        this(Duration.ofSeconds(20));
    }

    public RsdfIdModifier(Duration interval) {
        this.interval = interval;
    }

    @Override
    public List<RawStationDataFrame> apply(Iterable<RawStationDataFrame> rsdfList) {
        logger.debug("RsdfIdModifier apply():");

        List<RawStationDataFrame> newRsdfList = new ArrayList<>();

        for (RawStationDataFrame rsdf : rsdfList) {
            if (startTime == null) {
                startTime = rsdf.getMetadata().getPayloadStartTime();
            }
            if (endTime == null) {
                endTime = rsdf.getMetadata().getPayloadEndTime();
            }
            if (receiptTime == null) {
                receiptTime = rsdf.getMetadata().getReceptionTime();
            }

            final var metadata = rsdf.getMetadata().toBuilder()
                .setReceptionTime(receiptTime)
                .setPayloadStartTime(startTime)
                .setPayloadEndTime(endTime)
                .build();
            logger.debug("start {} : end {}", metadata.getPayloadStartTime(),
                metadata.getPayloadEndTime());

            newRsdfList.add(RawStationDataFrame.builder()
                .generatedId()
                .setMetadata(metadata)
                .setRawPayload(rsdf.getRawPayload())
                .build());

            startTime = startTime.plus(interval);
            endTime = endTime.plus(interval);
            receiptTime = receiptTime.plus(interval);
        }
        logger.debug("\n");
        return newRsdfList;
    }

}
