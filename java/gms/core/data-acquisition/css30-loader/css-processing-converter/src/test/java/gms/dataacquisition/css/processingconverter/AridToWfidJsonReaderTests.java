package gms.dataacquisition.css.processingconverter;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.Test;


class AridToWfidJsonReaderTests {

  @Test
  void testRead() throws Exception {
    final String path = "src/test/resources/processingfiles/Arid2Wfid.json";
    Map<Integer, Long> aridToWfid = AridToWfidJsonReader.read(path);
    assertNotNull(aridToWfid);
    assertEquals(168, aridToWfid.size());
    // sample a few keys - look up an arid, get an expected wfid.
    assertEquals(300000L, (long) aridToWfid.get(59210057));
    assertEquals(300011L, (long) aridToWfid.get(59210061));
    assertEquals(300022L, (long) aridToWfid.get(59210187));
  }

}
