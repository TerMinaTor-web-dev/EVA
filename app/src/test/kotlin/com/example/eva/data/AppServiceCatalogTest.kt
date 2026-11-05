package com.example.eva.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppServiceCatalogTest {
    @Test
    fun catalog_containsEssentialServices() {
        val services = AppServiceCatalog.services
        assertTrue(services.isNotEmpty())
        assertNotNull(AppServiceCatalog.getById("instagram"))
        assertNotNull(AppServiceCatalog.getById("youtube"))
        assertNotNull(AppServiceCatalog.getById("spotify"))
        assertNotNull(AppServiceCatalog.getById("facebook"))
    }

    @Test
    fun catalog_allServicesHaveValidUrls() {
        val services = AppServiceCatalog.services
        for (service in services) {
            assertTrue(service.url.startsWith("https://"))
        }
    }
}
