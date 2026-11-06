package com.example.eva.service

import com.example.eva.data.AppServiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaPipDecisionEngineTest {

    @Test
    fun youtube_triggersPiPApprovalOnlyWhenVideoActive() {
        val ytService = AppServiceCatalog.getById("youtube")
        // Video active in fullscreen -> PiP approved
        val activeDecision = EvaPipDecisionEngine.evaluate(
            activeService = ytService,
            hasActiveVideo = true
        )
        assertTrue(activeDecision.allowPip)
        assertEquals(16, activeDecision.numerator)
        assertEquals(9, activeDecision.denominator)

        // Video not active -> PiP suppressed to prevent blank white box
        val inactiveDecision = EvaPipDecisionEngine.evaluate(
            activeService = ytService,
            hasActiveVideo = false
        )
        assertFalse(inactiveDecision.allowPip)
    }

    @Test
    fun socialFeeds_suppressPiPToPreventObstruction() {
        val insta = AppServiceCatalog.getById("instagram")
        val fb = AppServiceCatalog.getById("facebook")
        val linkedin = AppServiceCatalog.getById("linkedin")
        val reddit = AppServiceCatalog.getById("reddit")

        assertFalse(EvaPipDecisionEngine.evaluate(insta, hasActiveVideo = false).allowPip)
        assertFalse(EvaPipDecisionEngine.evaluate(fb, hasActiveVideo = false).allowPip)
        assertFalse(EvaPipDecisionEngine.evaluate(linkedin, hasActiveVideo = false).allowPip)
        assertFalse(EvaPipDecisionEngine.evaluate(reddit, hasActiveVideo = false).allowPip)
    }

    @Test
    fun audioStreaming_routesToNotificationInsteadOfPiP() {
        val spotify = AppServiceCatalog.getById("spotify")
        val ytMusic = AppServiceCatalog.getById("ytmusic")

        assertFalse(EvaPipDecisionEngine.evaluate(spotify, hasActiveVideo = false).allowPip)
        assertFalse(EvaPipDecisionEngine.evaluate(ytMusic, hasActiveVideo = false).allowPip)
        // Even if audio is playing in ytmusic, PiP is strictly disallowed
        assertFalse(EvaPipDecisionEngine.evaluate(ytMusic, hasActiveVideo = true).allowPip)
    }

    @Test
    fun activeHtml5VideoInDom_grantsPiP() {
        val customVideo = AppServiceCatalog.getById("youtube")
        val decision = EvaPipDecisionEngine.evaluate(
            activeService = customVideo,
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
