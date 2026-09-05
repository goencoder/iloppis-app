package se.iloppis.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBuildInfoTest {
    @Test
    fun `staging build is visibly identified`() {
        val info = AppBuildInfo("staging", "0.1.0-staging", 7, "https://example.test/")

        assertTrue(info.isStaging)
        assertEquals("0.1.0-staging (7)", info.versionLabel)
    }

    @Test
    fun `production build is not identified as staging`() {
        val info = AppBuildInfo("production", "0.1.0", 8, "https://example.test/")

        assertFalse(info.isStaging)
        assertEquals("0.1.0 (8)", info.versionLabel)
    }
}
