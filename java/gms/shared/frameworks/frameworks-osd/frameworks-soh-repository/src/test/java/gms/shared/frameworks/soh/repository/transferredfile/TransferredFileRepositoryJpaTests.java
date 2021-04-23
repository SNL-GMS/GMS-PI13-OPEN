package gms.shared.frameworks.soh.repository.transferredfile;

import gms.shared.frameworks.osd.api.util.TimeRangeRequest;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFile;
import gms.shared.frameworks.osd.coi.channel.dataacquisitionstatus.TransferredFileStatus;
import gms.shared.frameworks.soh.repository.util.DbTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.testcontainers.junit.jupiter.Testcontainers;

import static gms.shared.frameworks.soh.repository.transferredfile.TransferredFileTestFixtures.receptionTime2;
import static gms.shared.frameworks.soh.repository.transferredfile.TransferredFileTestFixtures.transferTime2;
import static gms.shared.frameworks.soh.repository.transferredfile.TransferredFileTestFixtures.transferredInvoice;
import static gms.shared.frameworks.soh.repository.transferredfile.TransferredFileTestFixtures.transferredInvoice2;
import static gms.shared.frameworks.soh.repository.transferredfile.TransferredFileTestFixtures.transferredRawStationDataFrame;
import static gms.shared.frameworks.soh.repository.transferredfile.TransferredFileTestFixtures.transferredRawStationDataFrame2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO: Fix Unit Tests. Set Disabled for testcontainers
@Testcontainers
@Disabled
class TransferredFileRepositoryJpaTests extends DbTest {

  private TransferredFileRepositoryJpa transferredFileRepositoryJpa;

  private static List<TransferredFile<?>> storedFiles = List
      .of(transferredInvoice,
          transferredRawStationDataFrame);

  @BeforeEach
  void testCaseSetup() {
    transferredFileRepositoryJpa = new TransferredFileRepositoryJpa(entityManagerFactory);

    // Load some initial TransferredFiles objects before each test is run -
    // 1 transferredInvoice and 1 transferredRawStationDataFrame
    transferredFileRepositoryJpa.storeTransferredFiles(storedFiles);
  }

  @Test
  void testRetrieveAll() {

    // Check for the 2 TransferredFiles objects that were loaded during the @Before stage
    List<TransferredFile> retrievedFiles = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    assertResultsEqual(storedFiles, retrievedFiles);
  }

  @Test
  void testRetrieveByTransferTime() {

    // retrieve all that has been stored up until now, which is 2 TransferredFiles objects
    List<TransferredFile> retrievedFiles = transferredFileRepositoryJpa
        .retrieveByTransferTime(TimeRangeRequest.create(Instant.EPOCH, Instant.now()));
    // assert stored contents match retrieved contents
    assertResultsEqual(storedFiles, retrievedFiles);

    // nothing had been stored at the EPOCH point in time - returned list should be empty
    List<TransferredFile> epochFiles = transferredFileRepositoryJpa
        .retrieveByTransferTime(TimeRangeRequest.create(Instant.EPOCH, Instant.EPOCH));
    assertTrue(epochFiles.isEmpty());

    // using a more specific time frame, test retrieving only 1 of the 3 entries persisted by
    // storing and retrieving a new single object that has a payload start/end time earlier than
    // any other object persisted so far
    transferredFileRepositoryJpa.storeTransferredFiles(List.of(transferredRawStationDataFrame2));
    List<TransferredFile> rsdf2Files = transferredFileRepositoryJpa
        .retrieveByTransferTime(
            TimeRangeRequest.create(transferTime2, receptionTime2));
    assertResultsEqual(List.of(transferredRawStationDataFrame2), rsdf2Files);
  }

  @Test
  void testStoreNewFile() {

    // store a new object
    List<String> filenames = transferredFileRepositoryJpa.storeTransferredFiles(List.of(transferredInvoice2));
    assertEquals(List.of(transferredInvoice2.getFileName()), filenames);
    List<TransferredFile> retrievedFiles = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    // assert that new object was stored and there are now a total of 3 objects persisted
    // (2 were stored during @Before stage)
    assertNotNull(retrievedFiles);
    assertEquals(3, retrievedFiles.size());
    assertTrue(retrievedFiles.contains(transferredInvoice2));
  }

  @Test
  void testUpdateStoredFile() {

    // remove all of the objects (2) that were stored during the @Before stage
    Duration duration = Duration.ofSeconds(0);
    transferredFileRepositoryJpa.removeSentAndReceived(duration);
    List<TransferredFile> retrievedTransferredFiles = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    assertTrue(retrievedTransferredFiles.isEmpty());

    // store a new object to test updating one of it's field
    transferredFileRepositoryJpa.storeTransferredFiles(List.of(transferredInvoice2));
    List<TransferredFile> retrievedFile = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    assertResultsEqual(List.of(transferredInvoice2), retrievedFile);

    // verify stored object's status - it should be SENT
    TransferredFile<?> retrievedStoredFile = retrievedFile.get(0);
    assertEquals(TransferredFileStatus.SENT, retrievedStoredFile.getStatus());

    // use the autovalue builder to set a field to a new value, keeping the other values the same
    TransferredFile<?> someUpdatedFile = retrievedStoredFile.toBuilder()
        .setStatus(TransferredFileStatus.SENT_AND_RECEIVED)
        .build();

    // store updated object and check to make sure there are still the same number of objects persisted
    transferredFileRepositoryJpa.storeTransferredFiles(List.of(someUpdatedFile));
    List<TransferredFile> updatedFiles = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    assertNotNull(updatedFiles);
    // assert that the modified file was stored as an "update" and not as "new" - there should only be 1 object persisted
    assertEquals(1, updatedFiles.size());

    // individual assertions of each field being the same, except for the updated one (status)
    final TransferredFile<?> onlyFile = updatedFiles.get(0);
    assertEquals(transferredInvoice2.getFileName(), onlyFile.getFileName());
    assertEquals(transferredInvoice2.getPriority(), onlyFile.getPriority());
    assertEquals(transferredInvoice2.getTransferTime(), onlyFile.getTransferTime());
    assertEquals(transferredInvoice2.getReceptionTime(), onlyFile.getReceptionTime());
    // check the modified field for the updated status value
    assertEquals(TransferredFileStatus.SENT_AND_RECEIVED, onlyFile.getStatus());
    assertEquals(transferredInvoice2.getMetadataType(), onlyFile.getMetadataType());
    assertEquals(transferredInvoice2.getMetadata(), onlyFile.getMetadata());
  }

  @Test
  void testRemoveSentAndReceived() {

    // remove all of the objects (2 SENT_AND_RECEIVED) that were stored during the @Before stage
    Duration duration = Duration.ofSeconds(0);   // now minus - 0 seconds
    transferredFileRepositoryJpa.removeSentAndReceived(duration);
    List<TransferredFile> sentAndReceivedFiles = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    assertTrue(sentAndReceivedFiles.isEmpty());

    // store a single object that has a status of SENT, not SENT_AND_RECEIVED (so it won't be removed)
    transferredFileRepositoryJpa.storeTransferredFiles(List.of(transferredInvoice2));
    List<TransferredFile> sentFile = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    assertResultsEqual(List.of(transferredInvoice2), sentFile);

    // check if newly stored file gets removed - it shouldn't since status is not SENT_AND_RECEIVED
    // Note - a call to removeSentAndReceived with a duration of 0 seconds should remove all
    // SENT_AND_RECEIVED objects with a transfer time older than now
    transferredFileRepositoryJpa.removeSentAndReceived(duration);
    List<TransferredFile> retrievedSentFile = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    // assert it wasn't removed and is the only object stored
    assertResultsEqual(List.of(transferredInvoice2), retrievedSentFile);
  }

  @Test
  void testRemoveSentAndReceivedByAgeOfTransferTime() {

    // 2 objects with status of SENT_AND_RECEIVED were stored during the @Before stage, with
    // a transfer time of now
    // Store a new SENT_AND_RECEIVED object, then remove it due to it's transfer time age,
    // leaving only the original 2 objects that were stored during the @Before stage

    transferredFileRepositoryJpa.storeTransferredFiles(List.of(transferredRawStationDataFrame2));
    List<TransferredFile> rsdf2Files = transferredFileRepositoryJpa
        .retrieveByTransferTime(
            TimeRangeRequest.create(transferTime2, receptionTime2));
    assertResultsEqual(List.of(transferredRawStationDataFrame2), rsdf2Files);

    List<TransferredFile> allRetrievedFiles = transferredFileRepositoryJpa
        .retrieveAllTransferredFiles(List.of());
    assertEquals(3, allRetrievedFiles.size());
    assertTrue(allRetrievedFiles.contains(transferredInvoice));
    assertTrue(allRetrievedFiles.contains(transferredRawStationDataFrame));
    assertTrue(allRetrievedFiles.contains(transferredRawStationDataFrame2));

    // storing and then remove a new object with a transfer time that is much older
    // than the original 2 objects that were stored during the @Before stage
    Duration duration = Duration.ofSeconds(60 * (long)30);   // 30 mins ago
    transferredFileRepositoryJpa.removeSentAndReceived(duration);
    List<TransferredFile> sentAndReceivedFiles = transferredFileRepositoryJpa.retrieveAllTransferredFiles(List.of());
    assertEquals(2, sentAndReceivedFiles.size());
    assertResultsEqual(storedFiles, sentAndReceivedFiles);
  }

  // Helper function
  private void assertResultsEqual(List<TransferredFile<?>> expected, List<TransferredFile> actual) {
    assertNotNull(actual);
    assertEquals(expected.size(), actual.size());
    assertEquals(new HashSet<>(expected), new HashSet<>(actual));
  }

}
