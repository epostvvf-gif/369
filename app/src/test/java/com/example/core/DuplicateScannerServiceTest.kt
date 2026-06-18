package com.example.core

import com.example.data.FileEntity
import com.example.service.DuplicateScannerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DuplicateScannerService] to verify correct grouping
 * based on file sizes and cryptographic hash logic.
 */
class DuplicateScannerServiceTest {

    private val service = DuplicateScannerService()

    @Test
    fun testNoDuplicates() {
        val files = listOf(
            FileEntity(id = 1, name = "a.txt", path = "/path/a.txt", mimeType = "text/plain", size = 100, category = "Documents"),
            FileEntity(id = 2, name = "b.txt", path = "/path/b.txt", mimeType = "text/plain", size = 200, category = "Documents"),
            FileEntity(id = 3, name = "c.txt", path = "/path/c.txt", mimeType = "text/plain", size = 300, category = "Documents")
        )

        val results = service.scanForDuplicates(files)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testDuplicateGroupingBySizeAndHash() {
        val files = listOf(
            FileEntity(id = 1, name = "doc.pdf", path = "/path/doc.pdf", mimeType = "application/pdf", size = 500, category = "Documents"),
            FileEntity(id = 2, name = "doc.pdf", path = "/path/doc_copy.pdf", mimeType = "application/pdf", size = 500, category = "Documents"),
            FileEntity(id = 3, name = "diff.txt", path = "/path/diff.txt", mimeType = "text/plain", size = 100, category = "Documents")
        )

        // Running duplicate detection on SHA-256 (default fallback signature will match since id=1 & id=2 have same metadata attributes)
        val results = service.scanForDuplicates(files, "SHA-256")
        
        assertEquals(1, results.size)
        val duplicateList = results.values.first()
        assertEquals(2, duplicateList.size)
        assertTrue(duplicateList.any { it.id == 1 })
        assertTrue(duplicateList.any { it.id == 2 })
    }

    @Test
    fun testDifferentMetadataGeneratesDifferentHashes() {
        val files = listOf(
            FileEntity(id = 1, name = "track1.mp3", path = "/path/track1.mp3", mimeType = "audio/mpeg", size = 1000, category = "Audio"),
            FileEntity(id = 2, name = "track2.mp3", path = "/path/track2.mp3", mimeType = "audio/mpeg", size = 1000, category = "Audio")
        )

        val hash1 = service.calculateFileHash(files[0])
        val hash2 = service.calculateFileHash(files[1])

        // Fallbacks are deterministic of metadata. Different names mean different hashes.
        assertTrue(hash1 != hash2)

        val results = service.scanForDuplicates(files)
        // Since names are different, fallback hashes are different, so no duplicates should be clustered.
        assertTrue(results.isEmpty())
    }
}
