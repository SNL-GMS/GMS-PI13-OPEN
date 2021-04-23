package gms.shared.frameworks.osd.coi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PhaseTypeTests {

  @Test
  public void testPorSCheckForSampleOfPhases() {
    assertEquals(PhaseType.pPdiff.getFinalPhase(), PhaseType.P );
    assertEquals(PhaseType.pPKiKP.getFinalPhase(), PhaseType.P );
    assertEquals(PhaseType.pPKP  .getFinalPhase(), PhaseType.P );
    assertEquals(PhaseType.SKKSdf.getFinalPhase(), PhaseType.S );
    assertEquals(PhaseType.SKP   .getFinalPhase(), PhaseType.P );
    assertEquals(PhaseType.SKPab .getFinalPhase(), PhaseType.P );
    assertEquals(PhaseType.SKPbc .getFinalPhase(), PhaseType.P );
    assertEquals(PhaseType.PPP   .getFinalPhase(), PhaseType.P );
    assertEquals(PhaseType.PPP_B .getFinalPhase(), PhaseType.P );
    assertEquals(PhaseType.PPS   .getFinalPhase(), PhaseType.S );
    assertEquals(PhaseType.PPS_B .getFinalPhase(), PhaseType.S );
    assertEquals(PhaseType.PS    .getFinalPhase(), PhaseType.S );
    assertEquals(PhaseType.PS_1  .getFinalPhase(), PhaseType.S );
    assertEquals(PhaseType.pSdiff.getFinalPhase(), PhaseType.S );

  }

}
