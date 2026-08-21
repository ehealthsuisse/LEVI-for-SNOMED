package ch.ehealth.levi.core.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DbVariant}.
 */
public class DbVariantTest {

    @Test
    public void testDefaultCountryCode() {
        assertEquals("CH", DbVariant.PRODUCTION.getDefaultCountryCode());
        assertEquals("CH", DbVariant.BETA.getDefaultCountryCode());
        assertEquals("CH", DbVariant.PRE_PRODUCTION.getDefaultCountryCode());
        assertEquals("AT", DbVariant.AT.getDefaultCountryCode());
    }

    @Test
    public void testSuggestDbName() {
        assertEquals("SCT:CH_Jun26",
                DbVariant.PRODUCTION.suggestDbName("CH", "20260601"));
        assertEquals("SCT:CH_Beta_Aug26",
                DbVariant.BETA.suggestDbName("CH", "20260801"));
        assertEquals("SCT:CH_PreProdJun26",
                DbVariant.PRE_PRODUCTION.suggestDbName("CH", "20260601"));
        assertEquals("SCT:AT_Mar25",
                DbVariant.AT.suggestDbName("AT", "20250315"));
    }

    @Test
    public void testSuggestDbNameFallsBackToDefaultCountry() {
        assertEquals("SCT:AT_Mar25", DbVariant.AT.suggestDbName(null, "20250315"));
        assertEquals("SCT:CH_Aug26", DbVariant.PRODUCTION.suggestDbName("", "20260801"));
    }

    @Test
    public void testDetectVariant() {
        assertEquals(DbVariant.PRODUCTION, DbVariant.detectVariant(
                "/opt/Downloads/SnomedCT_ManagedServiceCH_PRODUCTION_CH1000195_20260607T120000Z"));
        assertEquals(DbVariant.BETA, DbVariant.detectVariant(
                "/opt/Downloads/SnomedCT_ManagedServiceCH_DAILYBUILD_BETA_CH1000195_20261207T120000Z"));
        assertEquals(DbVariant.BETA, DbVariant.detectVariant("CH_BETA_TEST"));
        assertEquals(DbVariant.PRE_PRODUCTION, DbVariant.detectVariant(
                "/opt/Downloads/SnomedCT_ManagedServiceCH_PREPRODUCTION_CH1000195_20260607T120000Z"));
        assertEquals(DbVariant.PRE_PRODUCTION, DbVariant.detectVariant(
                "/opt/Downloads/SnomedCT_ManagedServiceCH_PRE_PRODUCTION_CH1000195_20260607T120000Z"));
        assertEquals(DbVariant.AT, DbVariant.detectVariant("AT1000234"));
        assertNull(DbVariant.detectVariant("CH1000195"));
        assertNull(DbVariant.detectVariant(null));
        assertNull(DbVariant.detectVariant(""));
    }
}