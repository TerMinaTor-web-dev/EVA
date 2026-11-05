package com.example.eva.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppServiceCatalogTest {
    @Test
    fun catalog_containsEssentialServices() {
        val services = AppServiceCatalog.services
        assertTrue(services.isNotEmpty())
        assertNotNull(AppServiceCatalog.getById("instagram"))
        assertNotNull(AppServiceCatalog.getById("whatsapp"))
        assertNotNull(AppServiceCatalog.getById("youtube"))
        assertNotNull(AppServiceCatalog.getById("spotify"))
    }

    @Test
    fun whatsapp_usesDesktopUserAgent() {
        val whatsapp = AppServiceCatalog.getById("whatsapp")
        assertTrue(whatsapp.isDesktopUserAgent)
    }
}
