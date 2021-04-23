package gms.dataacquisition.stationreceiver.cd11.injector;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrame;
import gms.shared.frameworks.osd.coi.waveforms.RawStationDataFrameMetadata;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.vertx.reactivex.core.buffer.Buffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class that assists in ordering the injection data files and providing them to users as a
 * single data stream
 */
class InjectionFileHandler {

  private CompositeDisposable subscriptions = new CompositeDisposable();

  /**
   * Creates a single data stream consisting of all injection files on path
   *
   * @param path root directory of raw station frame data
   * @return a Future resolving to a {@link Flowable<Buffer>} that is the stream
   */
  Flowable<RawStationDataFrame> createConsolidatedDataStream(String path) throws IOException {
    ObjectMapper mapper = CoiObjectMapperFactory.getJsonObjectMapper();
    JavaType rsdfArrayType = mapper.getTypeFactory().constructCollectionType(List.class, RawStationDataFrame.class);
    Path rootDir = Paths.get(path);
   return Flowable.fromIterable(
        Files.list(rootDir)
            .collect(Collectors.toList()))
        .map(fileName -> Files.readString(fileName))
        .map(contents -> {
          List<RawStationDataFrame> rsdf = mapper.readValue(contents, rsdfArrayType);
          return rsdf;
        })
        .filter(list -> !list.isEmpty())
        .sorted(Comparator.comparing(list -> list.get(0).getMetadata(), Comparator.comparing(RawStationDataFrameMetadata::getPayloadStartTime)))
        .flatMap(Flowable::fromIterable);
  }

  /**
   * Closes the InjectionFileHandler, cleaning up resources
   */
  void close() {
    subscriptions.clear();
  }
}
