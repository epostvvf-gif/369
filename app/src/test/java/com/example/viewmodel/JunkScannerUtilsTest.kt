package com.example.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JunkScannerUtilsTest {

    @Test
    fun testScanTempAndCacheFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Let's call the real utility
        val junkItems = JunkScannerUtils.scanTempAndCacheFiles(context)
        
        // Assert that the returned list is not null and has content
        assertNotNull(junkItems)
        assertTrue(junkItems.isNotEmpty())
        
        // Validate that individual junk items are correctly modeled
        for (item in junkItems) {
            assertNotNull(item.id)
            assertNotNull(item.name)
            assertNotNull(item.path)
            assertTrue(item.size >= 0L)
            assertTrue(!item.isFolder) // Temp/cache scans should return files, not folders
        }
    }

    @Test
    fun testScanEmptyDirectories() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Let's call the real utility
        val emptyDirs = JunkScannerUtils.scanEmptyDirectories(context)
        
        // Assert that the returned list is not null and has content
        assertNotNull(emptyDirs)
        assertTrue(emptyDirs.isNotEmpty())
        
        // Validate that individual items are folders
        for (item in emptyDirs) {
            assertNotNull(item.id)
            assertNotNull(item.name)
            assertNotNull(item.path)
            assertEquals(0L, item.size)
            assertTrue(item.isFolder) // Should be folders
        }
    }
}
