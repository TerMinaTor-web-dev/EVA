package com.example.eva.security

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaPermissionManagerTest {

    @Test
    fun requiredPermissions_includesCameraAndAudio() {
        val permissions = EvaPermissionManager.getRequiredPermissions()
        assertNotNull(permissions)
        assertTrue(permissions.isNotEmpty())
        assertTrue(permissions.contains(android.Manifest.permission.CAMERA))
        assertTrue(permissions.contains(android.Manifest.permission.RECORD_AUDIO))
    }
}
