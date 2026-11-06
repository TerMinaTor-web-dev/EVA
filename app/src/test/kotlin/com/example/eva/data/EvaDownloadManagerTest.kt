package com.example.eva.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EvaDownloadManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSanitizeMimeType() {
        val jpgMime = EvaDownloadManager.sanitizeMimeType("https://example.com/photo.jpg", null)
        assertEquals("image/jpeg", jpgMime)

        val mp3Mime = EvaDownloadManager.sanitizeMimeType("https://example.com/audio.mp3", null)
        assertEquals("audio/mpeg", mp3Mime)

        val pdfMime = EvaDownloadManager.sanitizeMimeType("https://example.com/document.pdf", "application/pdf")
        assertEquals("application/pdf", pdfMime)

        val genericMime = EvaDownloadManager.sanitizeMimeType("https://example.com/binaryfile", null)
        assertEquals("application/octet-stream", genericMime)
    }

    @Test
    fun testResolveUniqueFileNameWithoutCollision() {
        val dir = tempFolder.newFolder("downloads")
        val uniqueName = EvaDownloadManager.resolveUniqueFileName("sample.txt", dir)
        assertEquals("sample.txt", uniqueName)
    }

    @Test
    fun testResolveUniqueFileNameWithCollision() {
        val dir = tempFolder.newFolder("downloads_collision")
        File(dir, "sample.txt").writeText("Existing content")

        val uniqueName = EvaDownloadManager.resolveUniqueFileName("sample.txt", dir)
        assertEquals("sample (1).txt", uniqueName)

        File(dir, "sample (1).txt").writeText("Second file")
        val nextUnique = EvaDownloadManager.resolveUniqueFileName("sample.txt", dir)
        assertEquals("sample (2).txt", nextUnique)
    }
}
