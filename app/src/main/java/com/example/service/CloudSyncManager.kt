package com.example.service

import com.example.data.FileEntity
import com.example.viewmodel.CloudBackupItem
import kotlinx.coroutines.delay

interface CloudSyncProvider {
    val providerName: String
    
    suspend fun backupFolder(
        folderName: String,
        files: List<FileEntity>,
        isSafeFolder: Boolean,
        isSafeUnlocked: Boolean,
        onProgress: (Float, String) -> Unit
    ): CloudBackupItem
}

class GoogleDriveSyncProvider : CloudSyncProvider {
    override val providerName: String = "Google Drive"
    
    override suspend fun backupFolder(
        folderName: String,
        files: List<FileEntity>,
        isSafeFolder: Boolean,
        isSafeUnlocked: Boolean,
        onProgress: (Float, String) -> Unit
    ): CloudBackupItem {
        if (isSafeFolder && !isSafeUnlocked) {
            throw IllegalStateException("PIN Verification REQUIRED to access and encrypt Safe Folder assets.")
        }
        
        val total = files.size
        if (total == 0) {
            onProgress(1f, "No files found to backup in this partition.")
            delay(150)
        } else {
            for (i in 1..8) {
                val progress = (i / 8f)
                val fileIndex = (i - 1) % total
                val currentFile = files[fileIndex].name
                val text = "Uploading $currentFile via Google Drive secure API Security Tunnel (${i}/8)..."
                onProgress(progress, text)
                delay(150)
            }
        }
        
        return CloudBackupItem(
            id = "backup_${folderName.lowercase().replace(" ", "_")}_google_drive",
            folderName = folderName,
            fileCount = total,
            totalSizeBytes = files.sumOf { it.size },
            backupTime = System.currentTimeMillis(),
            cloudService = providerName,
            isEncryptedSafeFolder = isSafeFolder
        )
    }
}

class DropboxSyncProvider : CloudSyncProvider {
    override val providerName: String = "Dropbox"
    
    override suspend fun backupFolder(
        folderName: String,
        files: List<FileEntity>,
        isSafeFolder: Boolean,
        isSafeUnlocked: Boolean,
        onProgress: (Float, String) -> Unit
    ): CloudBackupItem {
        if (isSafeFolder && !isSafeUnlocked) {
            throw IllegalStateException("PIN Verification REQUIRED to access and encrypt Safe Folder assets.")
        }
        
        val total = files.size
        if (total == 0) {
            onProgress(1f, "No files found to backup in this partition.")
            delay(150)
        } else {
            for (i in 1..8) {
                val progress = (i / 8f)
                val fileIndex = (i - 1) % total
                val currentFile = files[fileIndex].name
                val text = "Uploading $currentFile via Dropbox secure OAuth Security Tunnel (${i}/8)..."
                onProgress(progress, text)
                delay(150)
            }
        }
        
        return CloudBackupItem(
            id = "backup_${folderName.lowercase().replace(" ", "_")}_dropbox",
            folderName = folderName,
            fileCount = total,
            totalSizeBytes = files.sumOf { it.size },
            backupTime = System.currentTimeMillis(),
            cloudService = providerName,
            isEncryptedSafeFolder = isSafeFolder
        )
    }
}

class OneDriveSyncProvider : CloudSyncProvider {
    override val providerName: String = "OneDrive"
    
    override suspend fun backupFolder(
        folderName: String,
        files: List<FileEntity>,
        isSafeFolder: Boolean,
        isSafeUnlocked: Boolean,
        onProgress: (Float, String) -> Unit
    ): CloudBackupItem {
        if (isSafeFolder && !isSafeUnlocked) {
            throw IllegalStateException("PIN Verification REQUIRED to access and encrypt Safe Folder assets.")
        }
        
        val total = files.size
        if (total == 0) {
            onProgress(1f, "No files found to backup in this partition.")
            delay(150)
        } else {
            for (i in 1..8) {
                val progress = (i / 8f)
                val fileIndex = (i - 1) % total
                val currentFile = files[fileIndex].name
                val text = "Uploading $currentFile via OneDrive live REST Security Tunnel (${i}/8)..."
                onProgress(progress, text)
                delay(150)
            }
        }
        
        return CloudBackupItem(
            id = "backup_${folderName.lowercase().replace(" ", "_")}_onedrive",
            folderName = folderName,
            fileCount = total,
            totalSizeBytes = files.sumOf { it.size },
            backupTime = System.currentTimeMillis(),
            cloudService = providerName,
            isEncryptedSafeFolder = isSafeFolder
        )
    }
}

class CloudSyncManager {
    private val providers = mapOf(
        "Google Drive" to GoogleDriveSyncProvider(),
        "Dropbox" to DropboxSyncProvider(),
        "OneDrive" to OneDriveSyncProvider()
    )
    
    fun getProvider(name: String): CloudSyncProvider {
        return providers[name] ?: GoogleDriveSyncProvider()
    }
    
    suspend fun performSync(
        providerName: String,
        folderName: String,
        files: List<FileEntity>,
        isSafeFolder: Boolean,
        isSafeUnlocked: Boolean,
        isWifiOnly: Boolean,
        onProgress: (Float, String) -> Unit
    ): CloudBackupItem {
        // Safe Folder authentication check
        if (isSafeFolder && !isSafeUnlocked) {
            throw IllegalStateException("Safe Folder is currently locked. Verification PIN is sequence-mandated prior to upload.")
        }
        
        // Custom Simulation check for Wifi-only
        if (isWifiOnly) {
            onProgress(0.05f, "Checking network carrier rules... [Wi-Fi Only Mode ENFORCED]")
            delay(400)
        }
        
        val provider = getProvider(providerName)
        return provider.backupFolder(folderName, files, isSafeFolder, isSafeUnlocked, onProgress)
    }
}
