package com.example.eva.service

import com.example.eva.data.AppServiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaPipDecisionEngineTest {

    @Test
    fun youtube_triggersPiPApproval() {
        val ytService = AppServiceCatalog.getById("youtube")
        val decision = EvaPipDecisionEngine.evaluate(
            activeService = ytService,
            hasActiveVideo = false
        )
        assertTrue(decision.allowPip)
        assertEquals(16, decision.numerator)
        assertEquals(9, decision.denominator)
    }

    @Test
    fun socialFeeds_suppressPiPToPreventObstruction() {
        val insta = AppServiceCatalog.getById("instagram")
        val fb = AppServiceCatalog.getById("facebook")
        val linkedin = AppServiceCatalog.getById("linkedin")
        val reddit = AppServiceCatalog.getById("reddit")

        assertFalse(EvaPipDecisionEngine.evaluate(insta).allowPip)
        assertFalse(EvaPipDecisionEngine.evaluate(fb).allowPip)
        assertFalse(EvaPipDecisionEngine.evaluate(linkedin).allowPip)
        assertFalse(EvaPipDecisionEngine.evaluate(reddit).allowPip)
    }

    @Test
    fun audioStreaming_routesToNotificationInsteadOfPiP() {
        val spotify = AppServiceCatalog.getById("spotify")
        val ytMusic = AppServiceCatalog.getById("ytmusic")

        assertFalse(EvaPipDecisionEngine.evaluate(spotify).allowPip)
        assertFalse(EvaPipDecisionEngine.evaluate(ytMusic).allowPip)
    }

    @Test
    fun activeHtml5VideoInDom_grantsPiP() {
        val insta = AppServiceCatalog.getById("instagram")
        // If a video element is actively playing in DOM, PiP should be granted
        val decision = EvaPipDecisionEngine.evaluate(
            activeService = insta,
            hasActiveVideo = true
        )
        assertTrue(decision.allowPip)
    }

    @Test
    fun urlClassifier_identifiesStreamingDomains() {
        assertTrue(EvaPipDecisionEngine.classifyServiceForPip("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(EvaPipDecisionEngine.classifyServiceForPip("https://twitch.tv/streamer"))
        assertFalse(EvaPipDecisionEngine.classifyServiceForPip("https://www.instagram.com/p/abcdef/"))
    }
}
