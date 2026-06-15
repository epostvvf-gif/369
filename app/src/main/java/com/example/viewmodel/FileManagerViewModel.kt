package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.Part
import com.example.data.AppDatabase
import com.example.data.FileEntity
import com.example.data.FileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class representation for mock cloud files
data class CloudFile(
    val id: String,
    val name: String,
    val size: Long,
    val dateUpdated: Long,
    val semanticScore: Int? = null // Contextual match percentage (AI calculation scan)
)

// Data class for Chat messages
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface PinMode {
    object Register : PinMode
    object Confirm : PinMode
    object EnterPin : PinMode
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FileRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FileRepository(database.fileDao())
        
        // Seed database if empty
        seedDatabaseIfEmpty()
    }

    // --- State Variables for Local File Manager ---
    val allLocalFiles = repository.allFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val normalFiles = repository.normalFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val safeFiles = repository.safeFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val junkFiles = repository.junkFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedLocalFileIds = MutableStateFlow<Set<Int>>(emptySet())
    val isMultiSelect = MutableStateFlow(false)
    
    // --- Junk Cleanup Animation states ---
    val isJunkCleaning = MutableStateFlow(false)
    val showCelebrationDialog = MutableStateFlow(false)
    val junkBytesCleaned = MutableStateFlow(0L)

    // --- Duplicate Scanner States ---
    val showDuplicateScanner = MutableStateFlow(false)

    // --- Secure Safe Folder states ---
    val isPinRegistered = MutableStateFlow(false)
    val isSafeUnlocked = MutableStateFlow(false)
    val passcodeMode = MutableStateFlow<PinMode>(PinMode.Register)
    val enteredPinBuffer = MutableStateFlow("")
    val pinErrorMessage = MutableStateFlow<String?>(null)
    private var tempRegisterPin = ""

    // --- Cloud Google Drive Switcher Simulator States ---
    val cloudAccounts = MutableStateFlow(listOf("epostvvf@gmail.com", "workspace_admin@corp.io"))
    val selectedCloudAccount = MutableStateFlow<String?>("epostvvf@gmail.com")
    val searchCloudQuery = MutableStateFlow("")
    val selectedCloudFileIds = MutableStateFlow<Set<String>>(emptySet())
    val isCloudScanning = MutableStateFlow(false)
    val cloudScanProgress = MutableStateFlow(0f)

    // Simulated cloud file listing
    private val baseCloudFiles = MutableStateFlow<List<CloudFile>>(emptyList())
    val cloudFiles = combine(baseCloudFiles, searchCloudQuery) { files, query ->
        if (query.isBlank()) {
            files
        } else {
            files.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI Assistant Tab States ---
    val chatbotMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("welcome", "Hello! I am your AI Smart Space Assistant. Ask me anything about files, space savings, or let me guide you mathematically with High Thinking operations! 🚀", false)
        )
    )
    val geminiApiKey = MutableStateFlow("")
    val useHighThinking = MutableStateFlow(true)
    val isSendingToGemini = MutableStateFlow(false)
    val liveSetupPanelExpanded = MutableStateFlow(false) // collapsible card default state

    init {
        // Prepare original mock cloud folder files
        initializeCloudFiles()
        // Determine starting state of Safe PIN
        checkSafePinState()
    }

    private fun checkSafePinState() {
        viewModelScope.launch {
            val dbPin = repository.getSafePin()
            if (dbPin != null) {
                isPinRegistered.value = true
                passcodeMode.value = PinMode.EnterPin
            } else {
                isPinRegistered.value = false
                passcodeMode.value = PinMode.Register
            }
        }
    }

    private fun seedDatabaseIfEmpty() {
        viewModelScope.launch {
            val currentList = repository.allFiles.first()
            if (currentList.isEmpty()) {
                val seedData = listOf(
                    // Normal Files
                    FileEntity(name = "Vishwa_Foundation_Proposal.docx", path = "/docs/Vishwa_Foundation_Proposal.docx", mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document", size = 1845000, category = "Documents"),
                    FileEntity(name = "Tax_Exemption_Certificate_2026.pdf", path = "/docs/Tax_Exemption_Certificate_2026.pdf", mimeType = "application/pdf", size = 2560000, category = "Documents"),
                    FileEntity(name = "Financial_Ledger_Q1.xlsx", path = "/docs/Financial_Ledger_Q1.xlsx", mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", size = 4120000, category = "Documents"),
                    
                    // Duplicate Documents (To trigger Duplicate Scanner visual pairings!)
                    FileEntity(name = "Tax_Exemption_Certificate_Backup_Copy.pdf", path = "/docs/Tax_Exemption_Certificate_Backup_Copy.pdf", mimeType = "application/pdf", size = 2560000, category = "Documents"),
                    
                    // Images
                    FileEntity(name = "vishwa_vijayaa_logo.png", path = "/images/vishwa_vijayaa_logo.png", mimeType = "image/png", size = 840000, category = "Images"),
                    FileEntity(name = "foundation_event_primary.jpg", path = "/images/foundation_event_primary.jpg", mimeType = "image/jpeg", size = 3200000, category = "Images"),
                    
                    // Duplicate Images
                    FileEntity(name = "vishwa_vijayaa_logo_v2.png", path = "/images/vishwa_vijayaa_logo_v2.png", mimeType = "image/png", size = 840000, category = "Images"),

                    // Audio
                    FileEntity(name = "morning_conch_chants.mp3", path = "/audio/morning_conch_chants.mp3", mimeType = "audio/mpeg", size = 9520000, category = "Audio"),
                    FileEntity(name = "meditation_ambient_wind.wav", path = "/audio/meditation_ambient_wind.wav", mimeType = "audio/wav", size = 24800000, category = "Audio"),

                    // Videos
                    FileEntity(name = "vishwa_vijayaa_foundation_anthem.mp4", path = "/videos/vishwa_vijayaa_foundation_anthem.mp4", mimeType = "video/mp4", size = 105800000, category = "Videos"),

                    // Junk stuff (For manual cleaner animations)
                    FileEntity(name = "cache_compiler_dump.tmp", path = "/junk/cache_compiler_dump.tmp", mimeType = "text/plain", size = 12500000, isJunk = true, category = "Others"),
                    FileEntity(name = "gradle_build_cache_unzip.log", path = "/junk/gradle_build_cache_unzip.log", mimeType = "text/plain", size = 18400000, isJunk = true, category = "Others"),
                    FileEntity(name = "temp_icon_shards.bin", path = "/junk/temp_icon_shards.bin", mimeType = "application/octet-stream", size = 8900000, isJunk = true, category = "Others")
                )
                repository.insertFiles(seedData)
            }
        }
    }

    private fun initializeCloudFiles() {
        baseCloudFiles.value = listOf(
            CloudFile("c1", "Cloud_Vishwa_Brochure_M3.pdf", 4890000, System.currentTimeMillis() - 86400000 * 2),
            CloudFile("c2", "Donor_List_2026_Seeded.xlsx", 1250000, System.currentTimeMillis() - 86400000 * 12),
            CloudFile("c3", "Trustee_Resolutions_Signed.pdf", 5600000, System.currentTimeMillis() - 86400000 * 1),
            CloudFile("c4", "Foundation_Theme_Chants_Chords.wav", 45200000, System.currentTimeMillis() - 86400000 * 5),
            CloudFile("c5", "unauthorized_unused_duplicate.pdf", 5600000, System.currentTimeMillis() - 86400000 * 8) // Identical size warning
        )
    }

    // --- Search with Custom Match Percentage scoring calculation ---
    // Calculates how heavily the characters in the search query match this file's name.
    // Returns a dynamic matched score list of Pairs
    fun calculateSearchMatchesFlow(): Flow<List<Pair<FileEntity, Double>>> {
        return combine(normalFiles, searchQuery) { files, query ->
            if (query.isBlank()) {
                files.map { it to 100.0 }
            } else {
                files.map { file ->
                    val percentage = getCustomSearchMatchRatio(file.name, query)
                    file to percentage
                }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second } // Best match calculation first!
            }
        }
    }

    private fun getCustomSearchMatchRatio(fileName: String, query: String): Double {
        val cleanName = fileName.lowercase()
        val cleanQuery = query.lowercase()

        if (cleanName.contains(cleanQuery)) {
            // Perfect substring match -> high weightage (80% - 100% depending on length match)
            return 80.0 + (cleanQuery.length.toDouble() / cleanName.length.toDouble() * 20.0)
        }

        // Fuzzy match: calculate how many characters of query are found in filename in sequence
        var queryIndex = 0
        var matches = 0
        for (i in cleanName.indices) {
            if (queryIndex < cleanQuery.length && cleanName[i] == cleanQuery[queryIndex]) {
                matches++
                queryIndex++
            }
        }

        if (matches == cleanQuery.length) {
            // Sequence characters matches index completely
            return 60.0 * (cleanQuery.length.toDouble() / cleanName.length.toDouble())
        }

        // Intermittent character overlap match
        val queryChars = cleanQuery.toSet()
        val matchCount = cleanName.count { it in queryChars }
        val score = (matchCount.toDouble() / cleanName.length.toDouble()) * 30.0
        return if (score > 5.0) score else 0.0
    }

    // --- Local Manager Multi-select Actions ---
    fun toggleLocalFileSelection(id: Int) {
        val currentSet = selectedLocalFileIds.value
        if (currentSet.contains(id)) {
            selectedLocalFileIds.value = currentSet - id
        } else {
            selectedLocalFileIds.value = currentSet + id
        }
    }

    fun selectAllNormalFiles() {
        viewModelScope.launch {
            val normalList = normalFiles.value
            val allIds = normalList.map { it.id }.toSet()
            selectedLocalFileIds.value = allIds
        }
    }

    fun clearLocalSelection() {
        selectedLocalFileIds.value = emptySet()
        isMultiSelect.value = false
    }

    fun deleteSelectedLocalFiles() {
        viewModelScope.launch {
            val idsToDelete = selectedLocalFileIds.value
            val targetFiles = allLocalFiles.value.filter { it.id in idsToDelete }
            repository.deleteFiles(targetFiles)
            clearLocalSelection()
        }
    }

    fun addManualLocalFile(name: String, mime: String, sizeRaw: Long, category: String) {
        viewModelScope.launch {
            val newFile = FileEntity(
                name = name,
                path = "/user/$name",
                mimeType = mime,
                size = sizeRaw,
                category = category
            )
            repository.insertFile(newFile)
        }
    }

    // --- Move Selected Files to Private Secure Folder ---
    fun moveSelectedToSafe() {
        viewModelScope.launch {
            val idsToMove = selectedLocalFileIds.value.toList()
            repository.updateSafeStatus(idsToMove, isSafe = true)
            clearLocalSelection()
        }
    }

    // --- Restore or Delete Safe Files inside folder ---
    fun restoreFromSafe(id: Int) {
        viewModelScope.launch {
            repository.updateSafeStatus(listOf(id), isSafe = false)
        }
    }

    fun deleteSafeFile(file: FileEntity) {
        viewModelScope.launch {
            repository.deleteFiles(listOf(file))
        }
    }

    // --- Junk Cleaner Routine with celebration animation dialog ---
    fun runManualJunkCleaner() {
        viewModelScope.launch {
            isJunkCleaning.value = true
            // Calculate size to clean
            val totalBytes = junkFiles.value.sumOf { it.size }
            junkBytesCleaned.value = totalBytes
            
            // Simulating delightful cleaning scanner animation delays
            delay(2200) 
            
            repository.clearAllJunk()
            isJunkCleaning.value = false
            showCelebrationDialog.value = true
        }
    }

    // --- Duplicate Scanner Pairings Visual Aggregation ---
    // Finds files with identical sizes
    fun getDuplicateFileGroupings(): Map<Long, List<FileEntity>> {
        val normalList = normalFiles.value
        return normalList.groupBy { it.size }.filter { it.value.size > 1 }
    }

    fun deleteDuplicateFile(file: FileEntity) {
        viewModelScope.launch {
            repository.deleteFiles(listOf(file))
        }
    }

    fun deleteLocalFileDirectly(id: Int) {
        viewModelScope.launch {
            repository.deleteFileById(id)
        }
    }

    // --- PIN Management State Machine (Register, Verify, Lock/Unlock states) ---
    fun submitPinDigit(digit: String) {
        val currentBuf = enteredPinBuffer.value
        if (currentBuf.length < 4) {
            val nextBuf = currentBuf + digit
            enteredPinBuffer.value = nextBuf
            pinErrorMessage.value = null

            // Once 4 digits are typed, execute states
            if (nextBuf.length == 4) {
                processPinSubmission(nextBuf)
            }
        }
    }

    fun backspacePinDigit() {
        val currentBuf = enteredPinBuffer.value
        if (currentBuf.isNotEmpty()) {
            enteredPinBuffer.value = currentBuf.substring(0, currentBuf.length - 1)
        }
    }

    fun clearPinBuffer() {
        enteredPinBuffer.value = ""
        pinErrorMessage.value = null
    }

    private fun processPinSubmission(pin: String) {
        viewModelScope.launch {
            val mode = passcodeMode.value
            when (mode) {
                PinMode.Register -> {
                    tempRegisterPin = pin
                    enteredPinBuffer.value = ""
                    passcodeMode.value = PinMode.Confirm
                }
                PinMode.Confirm -> {
                    if (pin == tempRegisterPin) {
                        repository.setSafePin(pin)
                        isPinRegistered.value = true
                        isSafeUnlocked.value = true
                        enteredPinBuffer.value = ""
                    } else {
                        pinErrorMessage.value = "PIN mismatch! Set PIN again."
                        enteredPinBuffer.value = ""
                        passcodeMode.value = PinMode.Register
                    }
                }
                PinMode.EnterPin -> {
                    val actualRecord = repository.getSafePin()
                    if (actualRecord?.pin == pin) {
                        isSafeUnlocked.value = true
                        enteredPinBuffer.value = ""
                        pinErrorMessage.value = null
                    } else {
                        pinErrorMessage.value = "Incorrect 4-digit PIN attempt!"
                        enteredPinBuffer.value = ""
                    }
                }
            }
        }
    }

    fun lockSafeFolder() {
        isSafeUnlocked.value = false
        enteredPinBuffer.value = ""
        pinErrorMessage.value = null
        // Switch mode to EnterPin if pin is set
        viewModelScope.launch {
            if (repository.getSafePin() != null) {
                passcodeMode.value = PinMode.EnterPin
            } else {
                passcodeMode.value = PinMode.Register
            }
        }
    }

    fun resetSafePinConfig() {
        viewModelScope.launch {
            repository.deleteSafePin()
            isPinRegistered.value = false
            isSafeUnlocked.value = false
            passcodeMode.value = PinMode.Register
            enteredPinBuffer.value = ""
            pinErrorMessage.value = null
        }
    }

    // --- Google Drive Cloud Account Controller ---
    fun addCloudAccount(email: String) {
        if (email.isNotBlank() && !cloudAccounts.value.contains(email)) {
            cloudAccounts.value = cloudAccounts.value + email
            selectedCloudAccount.value = email
        }
    }

    fun logoutFromCloudAccount() {
        selectedCloudAccount.value = null
    }

    fun selectCloudAccount(email: String) {
        selectedCloudAccount.value = email
    }

    // --- Cloud Multi-selection, search, deletion actions ---
    fun toggleCloudFileSelection(id: String) {
        val currentSet = selectedCloudFileIds.value
        if (currentSet.contains(id)) {
            selectedCloudFileIds.value = currentSet - id
        } else {
            selectedCloudFileIds.value = currentSet + id
        }
    }

    fun deleteSelectedCloudFiles() {
        val idsToDelete = selectedCloudFileIds.value
        baseCloudFiles.value = baseCloudFiles.value.filter { it.id !in idsToDelete }
        selectedCloudFileIds.value = emptySet()
    }

    fun addCloudMockFile(name: String, sizeRaw: Long) {
        val newId = "cloud_${System.currentTimeMillis()}"
        val newFile = CloudFile(newId, name, sizeRaw, System.currentTimeMillis())
        baseCloudFiles.value = baseCloudFiles.value + newFile
    }

    // --- Semantic Active Cloud Scan with Match Score computation ---
    fun startSemanticScan() {
        viewModelScope.launch {
            isCloudScanning.value = true
            cloudScanProgress.value = 0f
            
            // Simulating incremental loading progress
            for (i in 1..10) {
                delay(200)
                cloudScanProgress.value = i * 0.1f
            }

            // Once complete, calculate matching semantic score (random/heuristic correlation on trust categories)
            baseCloudFiles.value = baseCloudFiles.value.map { file ->
                val calculatedScore = computeSimulatedSemanticPercent(file.name)
                file.copy(semanticScore = calculatedScore)
            }

            isCloudScanning.value = false
        }
    }

    private fun computeSimulatedSemanticPercent(fileName: String): Int {
        // High scores given to Vishwa/Trustee files based on semantic theme
        val clean = fileName.lowercase()
        return when {
            clean.contains("vishwa") -> 95 + (fileName.length % 5)
            clean.contains("trustee") -> 88 + (fileName.length % 8)
            clean.contains("donor") -> 85 + (fileName.length % 7)
            clean.contains("chords") || clean.contains("chants") -> 74 + (fileName.length % 6)
            clean.contains("unauthorized") || clean.contains("copy") -> 12 + (fileName.length % 5)
            else -> 45 + (fileName.length % 20)
        }
    }

    // --- Chat Interface with Message State Persistence ---
    fun clearChatHistory() {
        chatbotMessages.value = listOf(
            ChatMessage("welcome", "Conversation reset. How can I help optimize your folder storage system or answer technical questions with high physical intelligence? 🤖", false)
        )
    }

    fun sendChatMessage(textInput: String) {
        val query = textInput.trim()
        if (query.isBlank()) return

        val userMsgId = "msg_user_${System.currentTimeMillis()}"
        val userMsg = ChatMessage(userMsgId, query, isUser = true)
        
        chatbotMessages.value = chatbotMessages.value + userMsg
        isSendingToGemini.value = true

        viewModelScope.launch {
            // Incorporate chat history to pass to Gemini
            val historyContents = chatbotMessages.value.filter { it.id != userMsgId && it.id != "welcome" }.map { 
                Content(parts = listOf(Part(text = it.text)))
            }

            // Call Repository Gemini endpoint
            val aiResponseText = repository.askGemini(
                prompt = query,
                customKey = geminiApiKey.value,
                useHighThinking = useHighThinking.value,
                history = historyContents
            )

            val aiMsg = ChatMessage(
                id = "msg_ai_${System.currentTimeMillis()}",
                text = aiResponseText,
                isUser = false
            )
            chatbotMessages.value = chatbotMessages.value + aiMsg
            isSendingToGemini.value = false
        }
    }

    // --- Dynamic formatters inside state layer ---
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val formatStrings = listOf("KB", "MB", "GB", "TB")
        val finalValue = bytes / Math.pow(1024.0, exp.toDouble())
        return String.format(Locale.US, "%.1f %s", finalValue, formatStrings[exp - 1])
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        return sdf.format(Date(timestamp))
    }
}
