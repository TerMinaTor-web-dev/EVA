package com.example.eva.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaCloudStorageManagerTest {

    private val fifteenGbBytes = 15L * 1024L * 1024L * 1024L
    private val nineGbBytes = 9L * 1024L * 1024L * 1024L

    @Test
    fun evaluateQuota_standardAccount_isEligibleWithNoDeficit() {
        val usedBytes = (2.4 * 1024.0 * 1024.0 * 1024.0).toLong()
        val quota = EvaCloudStorageManager.evaluateQuota(fifteenGbBytes, usedBytes)

        assertTrue(quota.isEligible)
        assertEquals(0L, quota.deficitBytes)
        assertEquals(15.0, quota.totalGb, 0.05)
        assertTrue(quota.freeGb >= 9.0)
        assertEquals("12.6", quota.formatFreeGb())
    }

    @Test
    fun evaluateQuota_insufficientSpaceAccount_isRejectedWithExactDeficit() {
        // 11.2 GB used out of 15 GB -> 3.8 GB free (5.2 GB short of 9 GB minimum)
        val usedBytes = (11.2 * 1024.0 * 1024.0 * 1024.0).toLong()
        val quota = EvaCloudStorageManager.evaluateQuota(fifteenGbBytes, usedBytes)

        assertFalse(quota.isEligible)
        assertTrue(quota.freeGb < 9.0)
        assertEquals("3.8", quota.formatFreeGb())
        assertEquals("5.2", quota.formatDeficitGb())
        assertTrue(quota.deficitBytes > 0)
    }

    @Test
    fun evaluateQuota_exactNineGbBoundary_isEligible() {
        // 15 GB total - 6 GB used = exactly 9.0 GB free
        val usedBytes = fifteenGbBytes - nineGbBytes
        val quota = EvaCloudStorageManager.evaluateQuota(fifteenGbBytes, usedBytes)

        assertTrue(quota.isEligible)
        assertEquals(0L, quota.deficitBytes)
        assertEquals(nineGbBytes, quota.freeBytes)
        assertEquals(9.0, quota.freeGb, 0.01)
    }

    @Test
    fun evaluateQuota_oneByteBelowNineGb_isRejected() {
        val usedBytes = (fifteenGbBytes - nineGbBytes) + 1L // 1 byte short of 9 GB
        val quota = EvaCloudStorageManager.evaluateQuota(fifteenGbBytes, usedBytes)

        assertFalse(quota.isEligible)
        assertEquals(1L, quota.deficitBytes)
    }

    @Test
    fun evaluateQuota_zeroTotalFallbackTo15Gb() {
        val quota = EvaCloudStorageManager.evaluateQuota(0L, 0L)
        assertEquals(fifteenGbBytes, quota.totalBytes)
        assertTrue(quota.isEligible)
        assertEquals(15.0, quota.freeGb, 0.01)
    }

    @Test
    fun generateVaultHierarchy_createsStandardStructure() {
        val vault = EvaCloudStorageManager.generateVaultHierarchy("user.test@gmail.com")

        assertEquals("/EVA_CloudSpace", vault.rootPath)
        assertEquals("/EVA_CloudSpace/Sessions", vault.sessionsPath)
        assertEquals("/EVA_CloudSpace/Media", vault.mediaPath)
        assertEquals("/EVA_CloudSpace/Archives", vault.archivesPath)
        assertTrue(vault.rootFolderId.contains("eva_vault_"))
    }
}
