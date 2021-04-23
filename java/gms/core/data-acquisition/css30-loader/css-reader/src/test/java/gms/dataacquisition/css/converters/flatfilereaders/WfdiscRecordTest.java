package gms.dataacquisition.css.converters.flatfilereaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gms.dataacquisition.cssreader.data.WfdiscRecord;
import gms.dataacquisition.cssreader.data.WfdiscRecord32;
import gms.dataacquisition.cssreader.flatfilereaders.FlatFileWfdiscReader;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests WfdiscRecord, which is a class that both represents and parses CSS 3.0 WfdiscRecord files.
 */
class WfdiscRecordTest {

  private static WfdiscRecord32 wfdisc;
  private double comparisonDelta = 0.000001;

  @BeforeEach
  void resetWfdisc() {
    wfdisc = new WfdiscRecord32();
    wfdisc.setSta("DAVOX");
    wfdisc.setChan("HHN");
    wfdisc.setTime("1274313600");
    wfdisc.setWfid("64583333");
    wfdisc.setChan("4247");
    wfdisc.setJdate("2010140");
    wfdisc.setEndtime("1274313732.99167");
    wfdisc.setNsamp("15960");
    wfdisc.setSamprate("120");
    wfdisc.setCalib("0.253");
    wfdisc.setCalper("1");
    wfdisc.setInstype("STS-2");
    wfdisc.setSegtype("o");
    wfdisc.setDatatype("s4");
    wfdisc.setClip("-");
    wfdisc.setDir("src/test/resources/css/WFS4/");
    wfdisc.setDfile("DAVOX0.w");
    wfdisc.setFoff("3897184");
    wfdisc.setCommid("-1");
    wfdisc.setLddate("20150408 12:00:00");
  }

  // tests on 'sta'
  @Test
  void testSetNullSta() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setSta(null);
      wfdisc.validate();
    });
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'chan'
  @Test
  void testSetNullChan() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setChan(null);
      wfdisc.validate();
    });
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'time'
  @Test
  void testSetNullTime() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setTime(null);
      wfdisc.validate();
    });
  }

  @Test
  void testSetTimeWithBadString() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setTime("not a number");
    });
  }

  @Test
  void testSetTime() {
    wfdisc.setTime("1274317219.75");  // epoch seconds with millis, just as in flatfilereaders files
    assertNotNull(wfdisc.getTime());
    Instant expected = Instant.ofEpochMilli((long) (1274317219.75 * 1000L));
    assertEquals(wfdisc.getTime(), expected);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'wfid'
  @Test
  void testSetWfIdNull() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setWfid(null);
    });
  }

  @Test
  void testSetWfIdBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setWfid("not a number");
    });
  }

  @Test
  void testSetWfId() {
    wfdisc.setWfid("12345");
    assertEquals(wfdisc.getWfid(), 12345);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'chanid'
  @Test
  void testSetChanIdNull() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setChanid(null);
    });
  }

  @Test
  void testSetChanIdBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setChanid("not a number");
    });
  }

  @Test
  void testSetChanId() {
    wfdisc.setChanid("12345");
    assertEquals(wfdisc.getChanid(), 12345);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'jdate'
  @Test
  void testSetJdateNull() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setJdate(null);
    });
  }

  @Test
  void testSetJdateBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setJdate("not a number");
    });
  }

  @Test
  void testSetJdateTooLow() {
    assertThrows(IllegalArgumentException.class, () -> {
      wfdisc
          .setJdate("1500001"); // ah, the first day of 1500 AD.  What a glorious time to be alive.
      wfdisc.validate();
    });
  }

  @Test
  void testSetJdateTooHigh() {
    assertThrows(IllegalArgumentException.class, () -> {
      wfdisc.setJdate("2900001"); // If this code is still running in 2900 AD, that's impressive.
      wfdisc.validate();
    });
  }

  @Test
  void testSetJdate() {
    wfdisc.setJdate("2012123");
    assertEquals(wfdisc.getJdate(), 2012123);
  }
  ////////////////////////////////////////////////////////////////////////
  // tests on 'endtime'

  @Test
  void testSetNullEndtime() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setEndtime(null);
    });
  }

  @Test
  void testSetEndtimeWithBadString() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setEndtime("not a number");
    });
  }

  @Test
  void testSetEndtime() {
    wfdisc.setEndtime("1274317219.75");  // epoch seconds with millis, just as in wfdiscreaders files
    assertNotNull(wfdisc.getEndtime());
    Instant expected = Instant.ofEpochMilli((long) (1274317219.75 * 1000L));
    assertEquals(wfdisc.getEndtime(), expected);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'nsamp'
  @Test
  void testSetNsampNull() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setNsamp(null);
    });
  }

  @Test
  void testSetNsampBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setNsamp("not a number");
    });
  }

  @Test
  void testSetNsampNegative() {
    assertThrows(IllegalArgumentException.class, () -> {
      wfdisc.setNsamp("-123");
      wfdisc.validate();
    });
  }

  @Test
  void testSetNsamp() {
    wfdisc.setNsamp("5000");
    assertEquals(wfdisc.getNsamp(), 5000);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'samprate'
  @Test
  void testSetSamprateNull() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setSamprate(null);
    });
  }

  @Test
  void testSetSamprateBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setSamprate("not a number");
    });
  }

  @Test
  void testSetSamprateNegative() {
    assertThrows(IllegalArgumentException.class, () -> {
      wfdisc.setSamprate("-123");
      wfdisc.validate();
    });
  }

  @Test
  void testSetSamprate() {
    wfdisc.setSamprate("40.0");
    assertEquals(wfdisc.getSamprate(), 40.0);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'calib'
  @Test
  void testSetCalibNull() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setCalib(null);
    });
  }

  @Test
  void testSetCalibBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setCalib("not a number");
    });
  }

  @Test
  void testSetCalibNegative() {
    assertThrows(IllegalArgumentException.class, () -> {
      wfdisc.setCalib("-123");
      wfdisc.validate();
    });
  }

  @Test
  void testSetCalib() {
    wfdisc.setCalib("1.0");
    assertEquals(wfdisc.getCalib(), 1.0);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'calper'
  @Test
  void testSetCalperNull() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setCalper(null);
    });
  }

  @Test
  void testSetCalperNegative() {
    assertThrows(IllegalArgumentException.class, () -> {
      wfdisc.setCalper("-123");
      wfdisc.validate();
    });
  }

  @Test
  void testSetCalperBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setCalper("not a number");
    });
  }

  @Test
  void testSetCalper() {
    wfdisc.setCalper("1.0");
    assertEquals(wfdisc.getCalper(), 1.0);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'instype'
  @Test
  void testSetNullInstype() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setInstype(null);
      wfdisc.validate();
    });
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'segtype'
  @Test
  void testSetSegtypeNull() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setSegtype(null);
      wfdisc.validate();
    });
  }

  @Test
  void testSetSegtype() {
    String seg = "o";
    wfdisc.setSegtype(seg);
    assertEquals(wfdisc.getSegtype(), seg);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'datatype'
  @Test
  void testSetDatatypeNull() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setDatatype(null);
      wfdisc.validate();
    });
  }

  @Test
  void testSetDatatype() {
    String dt = "s4";
    wfdisc.setDatatype(dt);  // format code from CSS 3.0
    assertEquals(wfdisc.getDatatype(), dt);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'clip'
  @Test
  void testSetClip() {
    wfdisc.setClip("-");
    assertFalse(wfdisc.getClip());

    wfdisc.setClip("some random string that isn't just 'c'");
    assertFalse(wfdisc.getClip());

    wfdisc.setClip("c");
    assertTrue(wfdisc.getClip());
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'dir'
  @Test
  void testSetNullDir() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setDir(null);
      wfdisc.validate();
    });
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'dfile'
  @Test
  void testSetNullDfile() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setDfile(null);
      wfdisc.validate();
    });
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'foff'
  @Test
  void testSetFoffNull() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setFoff(null);
    });
  }

  @Test
  void testSetFoffBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setFoff("not a number");
    });
  }

  @Test
  void testSetFoffNegative() {
    assertThrows(IllegalArgumentException.class, () -> {
      wfdisc.setFoff("-123");
      wfdisc.validate();
    });
  }

  @Test
  void testSetFoff() {
    wfdisc.setFoff("12345");
    assertEquals(wfdisc.getFoff(), 12345);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'commid'
  @Test
  void testSetCommidNull() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setCommid(null);
    });
  }

  @Test
  void testSetCommidBad() {
    assertThrows(NumberFormatException.class, () -> {
      wfdisc.setCommid("not a number");
    });
  }

  @Test
  void testSetCommid() {
    wfdisc.setCommid("12345");
    assertEquals(wfdisc.getCommid(), 12345);
  }

  ////////////////////////////////////////////////////////////////////////
  // tests on 'lddate'
  @Test
  void testSetNullLddate() {
    assertThrows(NullPointerException.class, () -> {
      wfdisc.setLddate(null);
      wfdisc.validate();
    });
  }

  // TODO: if lddate gets read into a Date or something in the future,
  // add a test verifying this works.
  ////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////
  // tests reading flatfilereaders against known values
  @Test
  void testWfdiscReadAll() throws Exception {
    FlatFileWfdiscReader reader = new FlatFileWfdiscReader();
    List<WfdiscRecord> wfdiscs = reader.read("src/test/resources/css/WFS4/wfdisc_gms_s4.txt");
    assertEquals(wfdiscs.size(), 76);

    WfdiscRecord wfdisc = wfdiscs.get(1);
    assertEquals(wfdisc.getSta(), "DAVOX");
    assertEquals(wfdisc.getChan(), "HHE");
    assertEquals(wfdisc.getTime(), Instant.ofEpochMilli(1274317199108l));
    assertEquals(wfdisc.getWfid(), 64583325);
    assertEquals(wfdisc.getChanid(), 4248);
    assertEquals(wfdisc.getJdate(), 2010140);
    assertEquals(wfdisc.getEndtime(), Instant.ofEpochMilli(1274317201991l));
    assertEquals(wfdisc.getNsamp(), 347);
    assertEquals(wfdisc.getSamprate(), 119.98617, this.comparisonDelta);
    assertEquals(wfdisc.getCalib(), 0.253, this.comparisonDelta);
    assertEquals(wfdisc.getCalper(), 1, this.comparisonDelta);
    assertEquals(wfdisc.getInstype(), "STS-2");
    assertEquals(wfdisc.getSegtype(), "o");
    assertEquals(wfdisc.getDatatype(), "s4");
    assertFalse(wfdisc.getClip());
    assertEquals(wfdisc.getDir(), "src/test/resources/css/WFS4/");
    assertEquals(wfdisc.getDfile(), "DAVOX0.w");
    assertEquals(wfdisc.getFoff(), 63840);
    assertEquals(wfdisc.getCommid(), -1);
    assertEquals(wfdisc.getLddate(), "20150408 12:00:00");
  }

  @Test
  void testWfdiscReadFilter() throws Exception {
    ArrayList<String> stations = new ArrayList<>(1);
    stations.add("MLR");

    ArrayList<String> channels = new ArrayList<>(1);
    channels.add("BHZ");

    FlatFileWfdiscReader reader = new FlatFileWfdiscReader(stations, channels, null, null);
    List<WfdiscRecord> wfdiscs = reader.read("src/test/resources/css/WFS4/wfdisc_gms_s4.txt");
    assertEquals(wfdiscs.size(), 3);

    WfdiscRecord wfdisc = wfdiscs.get(1);
    assertEquals(wfdisc.getSta(), "MLR");
    assertEquals(wfdisc.getChan(), "BHZ");
    assertEquals(wfdisc.getTime(), Instant.ofEpochMilli(1274317190019l));
    assertEquals(wfdisc.getWfid(), 64587196);
    assertEquals(wfdisc.getChanid(), 4162);
    assertEquals(wfdisc.getJdate(), 2010140);
    assertEquals(wfdisc.getEndtime(), Instant.ofEpochMilli(1274317209994l));
    assertEquals(wfdisc.getNsamp(), 800);
    assertEquals(wfdisc.getSamprate(), 40, this.comparisonDelta);
    assertEquals(wfdisc.getCalib(), 0.0633, this.comparisonDelta);
    assertEquals(wfdisc.getCalper(), 1, this.comparisonDelta);
    assertEquals(wfdisc.getInstype(), "STS-2");
    assertEquals(wfdisc.getSegtype(), "o");
    assertEquals(wfdisc.getDatatype(), "s4");
    assertFalse(wfdisc.getClip());
    assertEquals(wfdisc.getDir(), "src/test/resources/css/WFS4/");
    assertEquals(wfdisc.getDfile(), "MLR0.w");
    assertEquals(wfdisc.getFoff(), 2724800);
    assertEquals(wfdisc.getCommid(), -1);
    assertEquals(wfdisc.getLddate(), "20150408 12:00:00");
  }
}