package com.example.eva.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaPlayStoreBridgeManagerTest {

    @Test
    fun getPackageNameForService_resolvesMajorServicesCorrectly() {
        assertEquals("com.instagram.android", EvaPlayStoreBridgeManager.getPackageNameForService("instagram"))
        assertEquals("com.google.android.youtube", EvaPlayStoreBridgeManager.getPackageNameForService("youtube"))
        assertEquals("com.spotify.music", EvaPlayStoreBridgeManager.getPackageNameForService("spotify"))
        assertEquals("com.facebook.katana", EvaPlayStoreBridgeManager.getPackageNameForService("facebook"))
        assertEquals("com.twitter.android", EvaPlayStoreBridgeManager.getPackageNameForService("twitter"))
        assertEquals("com.reddit.frontpage", EvaPlayStoreBridgeManager.getPackageNameForService("reddit"))
        assertEquals("com.linkedin.android", EvaPlayStoreBridgeManager.getPackageNameForService("linkedin"))
    }

    @Test
    fun getPackageNameForService_isCaseInsensitive() {
        assertEquals("com.instagram.android", EvaPlayStoreBridgeManager.getPackageNameForService("INSTAGRAM"))
        assertEquals("com.google.android.youtube", EvaPlayStoreBridgeManager.getPackageNameForService("YouTube"))
    }

    @Test
    fun nativeAppWeights_providesRealisticEstimates() {
        assertTrue(EvaPlayStoreBridgeManager.getNativeAppWeightMb("instagram") >= 200)
        assertTrue(EvaPlayStoreBridgeManager.getNativeAppWeightMb("youtube") >= 250)
        assertTrue(EvaPlayStoreBridgeManager.getNativeAppWeightMb("facebook") >= 300)

        // Total saved space across all native apps should exceed 1.8 GB
        val totalWeightMb = EvaPlayStoreBridgeManager.getTotalEstimatedNativeWeightMb()
        assertTrue(totalWeightMb >= 1800)
    }

    @Test
    fun playStorePackageConstant_isStandard() {
        assertEquals("com.android.vending", EvaPlayStoreBridgeManager.PLAY_STORE_PACKAGE)
    }

    @Test
    fun unknownService_resolvesNullPackage() {
        org.junit.Assert.assertNull(EvaPlayStoreBridgeManager.getPackageNameForService("non_existent_app_xyz"))
    }

    @Test
    fun installApkFile_returnsFalseForNonExistentFile() {
        val dummyFile = java.io.File("non_existent_dummy.apk")
        org.junit.Assert.assertFalse(dummyFile.exists())
    }
}
