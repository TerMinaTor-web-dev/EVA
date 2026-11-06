package com.example.eva.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaHybridStorageEngineTest {

    @Test
    fun maxLocalCacheBytes_isConfiguredTo250Mb() {
        val expected = 250L * 1024L * 1024L
        assertEquals(expected, EvaHybridStorageEngine.MAX_LOCAL_CACHE_BYTES)
    }

    @Test
    fun calculateHardwareSpaceSavedMb_accountsForNativeAppsAndMediaOffload() {
        val savedMb = EvaHybridStorageEngine.calculateHardwareSpaceSavedMb(activeServicesCount = 8)
        // Expected ~2.03 GB native apps + 360 MB media offload = ~2.39 GB
        assertTrue(savedMb >= 2000)
    }

    @Test
    fun hybridStorageMetrics_calculatesGbAccurately() {
        val localBytes = 45L * 1024L * 1024L // 45 MB
        val cloudBytes = (2.4 * 1024.0 * 1024.0 * 1024.0).toLong() // 2.4 GB
        val spaceSavedMb = 2400

        val metrics = EvaHybridStorageEngine.HybridStorageMetrics(
            localUsedBytes = localBytes,
            cloudUsedBytes = cloudBytes,
            hardwareSavedMb = spaceSavedMb,
            filesSynced = 18,
            lastSyncTimestamp = System.currentTimeMillis()
        )

        assertEquals(45f, metrics.localUsedMb, 0.1f)
        assertEquals(2.4f, metrics.cloudUsedGb, 0.1f)
        assertEquals(2.34f, metrics.hardwareSavedGb, 0.1f)
    }
}
