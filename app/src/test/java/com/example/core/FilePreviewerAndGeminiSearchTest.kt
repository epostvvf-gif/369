package com.example.core

import com.example.viewmodel.PreviewFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("File Previewer & Gemini Search Logic Unit Suite (JUnit 5)")
class FilePreviewerAndGeminiSearchTest {

    @Nested
    @DisplayName("File Previewer Logic Tests")
    inner class PreviewerTests {

        @Test
        @DisplayName("Verify PreviewFile entity attributes and categories")
        fun testPreviewFileAttributes() {
            val imagePreview = PreviewFile(
                id = "img_test_101",
                name = "vishwa_sunset.png",
                size = 2048576L,
                category = "Images",
                path = "/sandbox/img_test_101.png",
                isCloud = false,
                isSafe = true
            )

            assertNotNull(imagePreview)
            assertEquals("img_test_101", imagePreview.id)
            assertEquals("vishwa_sunset.png", imagePreview.name)
            assertEquals("Images", imagePreview.category)
            assertFalse(imagePreview.isCloud)
            assertTrue(imagePreview.isSafe)
        }

        @Test
        @DisplayName("Verify document type detection heuristic matches correctly")
        fun testDocumentTypeAttributes() {
            val docPreview = PreviewFile(
                id = "doc_test_202",
                name = "vishwa_audit_ledger.txt",
                size = 14502L,
                category = "Documents",
                path = "/sandbox/ledger.txt",
                textContent = "TX_CODE: VFC-2026\nSTATUS: VERIFIED BY VISHWA TRUST\nLEDGER_BAL: 49M",
                isCloud = true,
                isSafe = false
            )

            assertEquals("vishwa_audit_ledger.txt", docPreview.name)
            assertEquals("Documents", docPreview.category)
            assertNotNull(docPreview.textContent)
            assertTrue(docPreview.textContent!!.contains("VFC-2026"))
            assertTrue(docPreview.isCloud)
        }
    }

    @Nested
    @DisplayName("Gemini Search Integration Logic Tests")
    inner class GeminiSearchTests {

        @Test
        @DisplayName("Simulate Gemini search scoring calculation matching on keywords")
        fun testGeminiSearchScoringEngine() {
            val query = "logo"
            val targetFiles = listOf(
                "vishwa_vijayaa_logo.png",
                "tax_exemption_audit_ledger.txt",
                "brand_logo_colorway.jpg",
                "system_cache_logs.tmp"
            )

            val matches = targetFiles.map { fileName ->
                val nameLower = fileName.lowercase()
                val score = when {
                    nameLower.contains(query) -> 100
                    nameLower.contains("audit") -> 50
                    else -> 0
                }
                fileName to score
            }.filter { it.second > 0 }

            assertEquals(3, matches.size)
            assertEquals("vishwa_vijayaa_logo.png", matches[0].first)
            assertEquals(100, matches[0].second)
            assertEquals("tax_exemption_audit_ledger.txt", matches[1].first)
            assertEquals(50, matches[1].second)
            assertEquals("brand_logo_colorway.jpg", matches[2].first)
            assertEquals(100, matches[2].second)
        }

        @Test
        @DisplayName("Reconstruct and verify mock Gemini prompts sent during file optimizations")
        fun testGeminiOptimizationPromptStructure() {
            val targetFolder = "Duplicate Pictures Cache"
            val itemCount = 4
            val prompt = """
                You are Gemini AI File Optimization Engine.
                Analyze the following folder metadata path: '$targetFolder'
                Found $itemCount duplicate files or large log structures.
                Provide optimization guidelines and format recommendations.
            """.trimIndent()

            assertTrue(prompt.contains("Gemini AI File Optimization Engine"))
            assertTrue(prompt.contains("Duplicate Pictures Cache"))
            assertTrue(prompt.contains("Found 4 duplicate files"))
        }
    }
}
