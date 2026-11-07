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
        assertNotNull(AppServiceCatalog.getById("flipkart"))
        assertNotNull(AppServiceCatalog.getById("amazon"))
        assertNotNull(AppServiceCatalog.getById("myntra"))
        assertNotNull(AppServiceCatalog.getById("instamart"))
        assertNotNull(AppServiceCatalog.getById("blinkit"))
    }

    @Test
    fun catalog_allServicesHaveValidUrls() {
        val services = AppServiceCatalog.services
        for (service in services) {
            assertTrue(service.url.startsWith("https://"))
        }
    }
}
