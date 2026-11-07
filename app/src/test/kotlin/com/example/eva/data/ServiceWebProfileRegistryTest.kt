package com.example.eva.data

import com.example.eva.model.UserAgentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceWebProfileRegistryTest {

    @Test
    fun testKnownProfilesExist() {
        val ytProfile = ServiceWebProfileRegistry.getProfile("youtube", "https://m.youtube.com")
        assertEquals("youtube", ytProfile.serviceId)
        assertTrue(ytProfile.allowJavaScript)
        assertTrue(ytProfile.allowDomStorage)
        assertFalse(ytProfile.mediaPlaybackRequiresUserGesture)
        assertTrue(ytProfile.isOriginAllowed("https://m.youtube.com"))
        assertTrue(ytProfile.isOriginAllowed("https://accounts.google.com"))

        val instaProfile = ServiceWebProfileRegistry.getProfile("instagram", "https://www.instagram.com")
        assertEquals("instagram", instaProfile.serviceId)
        assertTrue(instaProfile.allowCamera)
        assertTrue(instaProfile.allowFileUploads)
        assertTrue(instaProfile.isOriginAllowed("https://www.instagram.com"))
        assertFalse(instaProfile.isOriginAllowed("https://malicious-site.com"))

        val whatsappProfile = ServiceWebProfileRegistry.getProfile("whatsapp", "https://web.whatsapp.com")
        assertEquals(UserAgentType.DESKTOP_CHROME, whatsappProfile.userAgentType)
        assertTrue(whatsappProfile.allowMicrophone)
        assertTrue(whatsappProfile.allowCamera)

        val flipkartProfile = ServiceWebProfileRegistry.getProfile("flipkart", "https://www.flipkart.com")
        assertEquals("flipkart", flipkartProfile.serviceId)
        assertTrue(flipkartProfile.allowGeolocation)
        assertTrue(flipkartProfile.allowPopups)
        assertTrue(flipkartProfile.isOriginAllowed("https://www.flipkart.com"))
        assertTrue(flipkartProfile.isOriginAllowed("https://phonepe.com"))

        val instamartProfile = ServiceWebProfileRegistry.getProfile("instamart", "https://www.swiggy.com/instamart")
        assertEquals("instamart", instamartProfile.serviceId)
        assertTrue(instamartProfile.allowGeolocation)
        assertTrue(instamartProfile.isOriginAllowed("https://www.swiggy.com"))

        val blinkitProfile = ServiceWebProfileRegistry.getProfile("blinkit", "https://blinkit.com")
        assertEquals("blinkit", blinkitProfile.serviceId)
        assertTrue(blinkitProfile.allowGeolocation)
        assertTrue(blinkitProfile.isOriginAllowed("https://blinkit.com"))
        assertTrue(blinkitProfile.isOriginAllowed("https://grofers.com"))
    }

    @Test
    fun testUserAgentResolution() {
        val mobileProfile = ServiceWebProfileRegistry.getProfile("youtube", "https://m.youtube.com")
        val mobileUa = mobileProfile.resolveUserAgent("DefaultMobile", "DefaultDesktop")
        assertEquals("DefaultMobile", mobileUa)

        val desktopProfile = ServiceWebProfileRegistry.getProfile("whatsapp", "https://web.whatsapp.com")
        val desktopUa = desktopProfile.resolveUserAgent("DefaultMobile", "DefaultDesktop")
        assertEquals("DefaultDesktop", desktopUa)
    }

    @Test
    fun testDynamicProfileCreationForCustomUrls() {
        val customProfile = ServiceWebProfileRegistry.getProfile("custom_github", "https://github.com/login")
        assertNotNull(customProfile)
        assertEquals("custom_github", customProfile.serviceId)
        assertTrue(customProfile.allowJavaScript)
        assertTrue(customProfile.isOriginAllowed("https://github.com/dashboard"))
        assertFalse(customProfile.isOriginAllowed("https://evil.com"))
    }
}
