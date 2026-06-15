package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String,
    val mimeType: String,
    val size: Long,
    val isSafe: Boolean = false,
    val isJunk: Boolean = false,
    val category: String, // "Documents", "Images", "Audio", "Videos", "Others"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "safe_pin")
data class SafePinEntity(
    @PrimaryKey val id: Int = 1,
    val pin: String,
    val isRegistered: Boolean = true
)

@Dao
interface FileDao {
    @Query("SELECT * FROM local_files WHERE isSafe = 0 AND isJunk = 0 ORDER BY timestamp DESC")
    fun getNormalFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM local_files WHERE isSafe = 0 AND isJunk = 0 AND name LIKE :query ORDER BY timestamp DESC")
    fun getNormalFilesByNameLike(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM local_files WHERE isSafe = 1 ORDER BY timestamp DESC")
    fun getSafeFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM local_files WHERE isJunk = 1 ORDER BY timestamp DESC")
    fun getJunkFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM local_files ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Delete
    suspend fun deleteFiles(files: List<FileEntity>)

    @Query("DELETE FROM local_files WHERE id = :id")
    suspend fun deleteFileById(id: Int)

    @Query("UPDATE local_files SET isSafe = :isSafe WHERE id IN (:ids)")
    suspend fun updateSafeStatus(ids: List<Int>, isSafe: Boolean)

    @Query("DELETE FROM local_files WHERE isJunk = 1")
    suspend fun clearAllJunk()

    @Query("SELECT * FROM safe_pin WHERE id = 1 LIMIT 1")
    suspend fun getSafePin(): SafePinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSafePin(pin: SafePinEntity)

    @Query("DELETE FROM safe_pin WHERE id = 1")
    suspend fun deleteSafePin()
}

@Database(entities = [FileEntity::class, SafePinEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_manager_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
