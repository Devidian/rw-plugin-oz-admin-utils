package de.omegazirkel.risingworld.adminutils.blueprint;

import static de.omegazirkel.risingworld.adminutils.blueprint.BlueprintService.Decision.ALLOWED;
import static de.omegazirkel.risingworld.adminutils.blueprint.BlueprintService.Decision.EXTERNAL;
import static de.omegazirkel.risingworld.adminutils.blueprint.BlueprintService.Decision.PLAYTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlueprintServiceTest {
    @Test
    public void provenanceThresholdIsStrictAndOffByDefault() {
        assertFalse(BlueprintService.needsLookup(-1, false, false, 10));
        assertTrue(BlueprintService.needsLookup(0, false, false, 0));
        assertFalse(BlueprintService.needsLookup(3, false, false, 3));
        assertTrue(BlueprintService.needsLookup(3, false, false, 4));
        assertEquals(ALLOWED, BlueprintService.decide(3, 0, false, false, 0, 3, false));
        assertEquals(EXTERNAL, BlueprintService.decide(3, 0, false, false, 0, 4, false));
        assertEquals(ALLOWED, BlueprintService.decide(3, 0, false, false, 0, 4, true));
    }

    @Test
    public void adminExemptionBypassesBothRulesOnlyWhenEnabled() {
        assertFalse(BlueprintService.needsLookup(0, true, true, 5));
        assertEquals(ALLOWED, BlueprintService.decide(0, 10, true, true, 0, 5, false));
        assertEquals(PLAYTIME, BlueprintService.decide(0, 10, false, true, 0, 5, false));
        assertEquals(EXTERNAL, BlueprintService.decide(0, 10, false, true, 36000, 5, false));
        assertEquals(PLAYTIME, BlueprintService.decide(-1, 10, true, false, 35999, 5, true));
    }
}
