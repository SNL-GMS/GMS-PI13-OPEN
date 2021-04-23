package gms.dataacquisition.css.converters.flatfilereaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gms.dataacquisition.cssreader.data.WfdiscRecord;
import gms.dataacquisition.cssreader.flatfilereaders.FlatFileWfdiscReader;
import gms.dataacquisition.cssreader.flatfilereaders.WfdiscReaderInterface;
import java.util.Collection;
import org.junit.jupiter.api.Test;

/**
 * Tests the FlatFileWfdiscReader.
 */
class FlatFileWfdiscReaderTest {

  private WfdiscReaderInterface reader = new FlatFileWfdiscReader();

  @Test
  void testReadOnSampleFile() throws Exception {
    String TEST_DATA_DIR = "src/test/resources/css/WFS4/";
    String WF_DISC_TEST_FILE = TEST_DATA_DIR + "wfdisc_gms_s4.txt";
    Collection<WfdiscRecord> wfdiscRecords = reader.read(WF_DISC_TEST_FILE);
    assertNotNull(wfdiscRecords);
    int WF_DISC_TEST_FILE_ROWS = 76;
    assertEquals(wfdiscRecords.size(), WF_DISC_TEST_FILE_ROWS);
    assertFalse(wfdiscRecords.contains(null));
  }

  @Test
  void testReadOnNullPath() {
    assertThrows(NullPointerException.class, () -> {
      reader.read(null);
    });
  }

  @Test
  void testReadOnEmptyPath() {
    assertThrows(IllegalArgumentException.class, () -> {
      reader.read("");
    });
  }

  @Test
  void testReadOnBadPath() {
    assertThrows(IllegalArgumentException.class, () -> {
      reader.read("nonExistentPath");
    });
  }
}
