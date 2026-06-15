package com.example.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FileManagerViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: FileManagerViewModel
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(app)
        viewModel = FileManagerViewModel(app)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSeedingAndLoadingFiles() = runBlocking {
        // Since database seeds automatically if empty, wait and check all files load
        val files = viewModel.allLocalFiles.first()
        assertNotNull(files)
        assertTrue("Expected seed files to exist", files.isNotEmpty())
    }

    @Test
    fun testSearchFlowWithDatabaseLikeQuery() = runBlocking {
        // Set search query and observe calculateSearchMatchesFlow results
        viewModel.searchQuery.value = "logo"
        
        val matches = viewModel.calculateSearchMatchesFlow().first()
        
        assertNotNull(matches)
        assertTrue("Expected matching search entries for 'logo'", matches.isNotEmpty())
        
        matches.forEach { (file, score) ->
            assertTrue("Filename should contain 'logo'", file.name.lowercase().contains("logo"))
            assertTrue("Score should be high for substring matches", score >= 80.0)
        }
    }

    @Test
    fun testDuplicateMatchingGroupings() = runBlocking {
        // Trigger getDuplicateFileGroupings and inspect results
        val duplicateGroups = viewModel.getDuplicateFileGroupings()
        
        assertNotNull(duplicateGroups)
        assertTrue("Expected duplicate groupings to contain entries with same size", duplicateGroups.isNotEmpty())
        
        duplicateGroups.forEach { (size, files) ->
            assertTrue("Each duplicate group must contain at least 2 files", files.size >= 2)
            files.forEach { file ->
                assertEquals("File sizes in a group must match key size", size, file.size)
            }
        }
    }

    @Test
    fun testSafePINOperations() = runBlocking {
        // Reset pin first
        viewModel.resetSafePinConfig()
        
        // Assert initial status
        val registerStatus = viewModel.passcodeMode.first()
        assertEquals(PinMode.Register, registerStatus)

        // Try setting PIN (4-digits)
        viewModel.submitPinDigit("1")
        viewModel.submitPinDigit("2")
        viewModel.submitPinDigit("3")
        viewModel.submitPinDigit("4")
        
        // After 4-digits submitted, state switches to Confirm
        assertEquals(PinMode.Confirm, viewModel.passcodeMode.value)
        
        // Confirm PIN with matching 4-digits
        viewModel.submitPinDigit("1")
        viewModel.submitPinDigit("2")
        viewModel.submitPinDigit("3")
        viewModel.submitPinDigit("4")
        
        // After successful confirmation, pin is registered
        assertTrue(viewModel.isPinRegistered.value)
    }
}
