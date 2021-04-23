package gms.shared.frameworks.osd.api.transferredfile;

import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * TransferredFileRepository provides permanent, persistent storage of TransferredFiles with status
 * of either SENT or RECEIVED (i.e. those TransferredFiles appearing in a TransferredFileInvoice
 * where the corresponding data file has not been received; those TransferredFiles referencing
 * received data files that have not appeared in a TransferredFileInvoice).  TransferredFiles with
 * status of SENT are used to populate the "Gap List" display.  TransferredFiles with status of
 * SENT_AND_RECEIVED are regularly purged from the TransferredFileRepository.
 * TransferredFileRepository needs to be accessible from multiple applications (i.e. one or more
 * TransferAuditorUtility instances operating in data acquisition sequences and
 * TransferredFileRepositoryService).
 *
 * TransferredFileRepository has both load and loadByTransferTime operations to support the "Gap
 * List" displaying either recently missed transfers or missed transfers from arbitrary time
 * intervals.
 */

/**
 * Define an interface for storing, removing, and retrieving {@link TransferredFile}
 */
public interface TransferredFileRepositoryInterface {

  /**
   * Stores the provided TransferredFiles if they are not already in the TransferredFileRepository
   * @param transferredFiles - Collection of transferred files to store.
   * @return The {@link List} of file names that were stored
   */
  @Path("/data-acquisition/transferred-files/store")
  @POST
  @Consumes(ContentType.MSGPACK_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Stores the provided TransferredFiles if they are not already in the TransferredFileRepository")
  List<String> storeTransferredFiles(
      @RequestBody(description = "collection of transferred files to store")
      Collection<TransferredFile<?>> transferredFiles);

  /**
   * Removes TransferredFiles older than the Duration provided.
   *
   * @param olderThan the {@link Duration} for which older {@link TransferredFile}s should be
   * deleted
   */
  @Path("/data-acquisition/transferred-files/remove/older-than")
  @POST
  @Consumes(ContentType.MSGPACK_NAME)
  @Produces(ContentType.JSON_NAME)
  @Operation(summary = "Removes TransferredFiles older than the Duration provided.")
  void removeSentAndReceived(
      @RequestBody(description = "datetime (as Duration object) specifying date and time "
          + "of transferred files we'd like to keep track of.")
      Duration olderThan);

  /**
   * Loads and returns all TransferredFiles in the repository the match the list of filenames.
   * @return an empty collection if no matching TransferredFiles are found in the repository. If no
   * filenames
   *
   * @param filenames the list of file names to retrieve
   */
  @Path("/data-acquisition/transferred-files")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.MSGPACK_NAME)
  @Operation(summary = "loads and returns all transferred files in the repoistory that match "
      + "the list of filenames. returns all files if empty list provided.")
  List<TransferredFile> retrieveAllTransferredFiles(
      @RequestBody(description = "list of files to retrieve. If empty, all files are retrieved.")
      Collection<String> filenames);

  /**
   * Loads and returns all TransferredFiles in the repository which have a transferTime attribute
   * between transferStartTime and transferEndTime.  Both times are inclusive. Returns an empty
   * collection if no matching TransferredFiles are found in the repository.
   *
   * @param request the {@link TimeRangeRequest} representing the start end time for the
   * {@link TransferredFile}s to retrieve.
   * @return the list of {@link TransferredFile}s falling in the desired time range
   */
  @Path("/data-acquisition/transferred-files/by-transfer-time")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.MSGPACK_NAME)
  @Operation(summary = "retrieves all files current stored in the database within the given timerange"
      + "provided.")
  List<TransferredFile> retrieveByTransferTime(
      @RequestBody(description = "time range to retrieve the given transferred files from")
      TimeRangeRequest request);

  /**
   * Finds a transferred file in the repository if it exists.
   * @param file the file to search for
   * @param <T> the type of the transferred file
   * @return the file if it exists, or empty
   */
  @Path("/data-acquisition/status/transferred-file/name/search")
  @POST
  @Consumes(ContentType.JSON_NAME)
  @Produces(ContentType.MSGPACK_NAME)
  @Operation(summary = "Find a transferred file in the repository if it exists")
  <T extends TransferredFile> Optional<TransferredFile> find(
      @RequestBody(description = "the file to search for")
      T file);
}
