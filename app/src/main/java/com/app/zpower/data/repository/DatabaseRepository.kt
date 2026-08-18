package com.app.zpower.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import com.app.zpower.data.ZPowerDatabase
import com.app.zpower.data.entity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

@Serializable
data class ZPowerData(
    @SerialName("ThermalAreas") val thermalAreas: List<ThermalArea> = emptyList()
)

@Serializable
data class TelegramUpdateResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate>
)

@Serializable
data class TelegramUpdate(
    val update_id: Long,
    val message: TelegramMessage? = null,
    val edited_message: TelegramMessage? = null,
    val channel_post: TelegramMessage? = null,
    val edited_channel_post: TelegramMessage? = null
)

@Serializable
data class TelegramMessage(
    val message_id: Long,
    val from: TelegramUser? = null,
    val chat: TelegramChat,
    val date: Long,
    val document: TelegramDocument? = null,
    val forward_from: TelegramUser? = null,
    val forward_from_chat: TelegramChat? = null
)

@Serializable
data class TelegramUser(
    val id: Long,
    val is_bot: Boolean,
    val first_name: String,
    val last_name: String? = null,
    val username: String? = null
)

@Serializable
data class TelegramChat(
    val id: Long,
    val title: String? = null,
    val username: String? = null,
    val type: String? = null
)

@Serializable
data class TelegramDocument(
    val file_name: String? = null,
    val file_id: String,
    val file_size: Long? = null
)

@Serializable
data class TelegramBackupItem(
    val fileId: String,
    val name: String,
    val date: Long,
    val size: Long
)

@Serializable
data class TelegramFileResponse(
    val ok: Boolean,
    val result: TelegramFile? = null
)

@Serializable
data class TelegramFile(
    val file_id: String,
    val file_path: String? = null
)

@Serializable
data class TelegramMessageResponse(
    val ok: Boolean,
    val result: TelegramMessage? = null
)

enum class BackupType {
    COMPLETE, DATA_ONLY
}

enum class ImageProcessingState {
    IDLE, PROCESSING, SUCCESS, ERROR
}

class DatabaseRepository(
    private val database: ZPowerDatabase,
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncChannel = Channel<Unit>(Channel.CONFLATED)
    @Volatile
    private var isSyncing = false

    init {
        // Background initialization for heavy I/O
        syncScope.launch {
            ensureStorageDirectories(context)
            syncDataWithJson(context)
        }

        // Automatic Export on every change
        syncScope.launch {
            for (item in syncChannel) {
                if (isSyncing) {
                    android.util.Log.d("DatabaseRepository", "Sync in progress, skipping automatic export.")
                    continue
                }
                delay(500) // Debounce reduced for more "immediate" feel
                syncDatabaseToJson(context)
            }
        }

        val tables = arrayOf("thermal_areas", "rooms", "panels", "relays", "child_processes", "sub_processes")
        database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(tables) {
            override fun onInvalidated(tables: Set<String>) {
                // Only track update and trigger sync if not currently importing
                if (!isSyncing) {
                    syncScope.launch {
                        settingsRepository.updateLastDbUpdateTime(System.currentTimeMillis())
                        syncChannel.trySend(Unit)
                    }
                }
            }
        })
    }

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private suspend fun getEffectiveBotToken(): String {
        val isCustom = settingsRepository.isCustomConfigActive.first()
        val customToken = settingsRepository.customBotToken.first()
        return if (isCustom && customToken.isNotBlank()) {
            customToken
        } else {
            SettingsRepository.getAdminBotToken()
        }
    }

    fun getThermalAreaDao() = database.thermalAreaDao()
    fun getRoomDao() = database.roomDao()
    fun getPanelDao() = database.panelDao()
    fun getRelayDao() = database.relayDao()
    fun getChildProcessDao() = database.childProcessDao()
    fun getSubProcessDao() = database.subProcessDao()
    fun getSearchDao() = database.searchDao()

    suspend fun globalSearch(query: String) = database.searchDao().globalSearch(query)

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // For older versions, assume permission is handled via traditional manifest/runtime requests
            // (In this specific project context, we are focusing on Android 11+ / Scoped Storage fixes)
            true
        }
    }

    fun ensureStorageDirectories(context: Context) {
        if (!hasStoragePermission()) {
            android.util.Log.w("DatabaseRepository", "Cannot ensure directories: MANAGE_EXTERNAL_STORAGE permission missing.")
            return
        }
        val dataDir = getDataDir(context)
        if (!dataDir.exists()) dataDir.mkdirs()
        val imagesDir = getImagesDir(context)
        if (!imagesDir.exists()) imagesDir.mkdirs()
        
        // Manage temp workspace: Clear on startup to ensure strict temporary management
        val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val tempDir = File(publicDocs, "Gold Knowledge/temp")
        if (tempDir.exists() && tempDir.isDirectory) {
            tempDir.listFiles()?.forEach { it.delete() }
        } else {
            tempDir.mkdirs()
        }
    }

    private fun getBaseDir(context: Context): File {
        // Use public Documents folder as the single source of truth - NO FALLBACK
        val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(publicDocs, "Gold Knowledge")
        if (hasStoragePermission() && !dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getDataDir(context: Context): File {
        val dir = File(getBaseDir(context), "data")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getImagesDir(context: Context): File {
        val dir = File(getBaseDir(context), "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getDataFile(context: Context): File {
        return File(getDataDir(context), "data.json")
    }

    data class ImageSaveResult(
        val state: ImageProcessingState,
        val fileName: String = "",
        val error: String? = null
    )

    suspend fun saveImage(context: Context, uri: Uri): Flow<ImageSaveResult> = flow {
        emit(ImageSaveResult(ImageProcessingState.PROCESSING))
        
        if (!hasStoragePermission()) {
            android.util.Log.w("DatabaseRepository", "Cannot save image: MANAGE_EXTERNAL_STORAGE permission missing.")
            emit(ImageSaveResult(ImageProcessingState.ERROR, error = "Permission missing"))
            return@flow
        }
        
        val imagesDir = getImagesDir(context)
        val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp"
        val destFile = File(imagesDir, fileName)
        
        var success = false
        try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        destFile.outputStream().use { output ->
                            success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, output)
                            } else {
                                @Suppress("DEPRECATION")
                                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, output)
                            }
                        }
                        bitmap.recycle()
                    } else {
                        android.util.Log.e("DatabaseRepository", "Failed to decode bitmap from URI: $uri")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Error saving image to storage", e)
        }
        
        if (success) {
            android.util.Log.d("DatabaseRepository", "Image saved successfully: $fileName at ${destFile.absolutePath}")
            emit(ImageSaveResult(ImageProcessingState.SUCCESS, fileName))
        } else {
            emit(ImageSaveResult(ImageProcessingState.ERROR, error = "Failed to save image"))
        }
    }

    fun getLocalImageFile(context: Context, fileName: String): File? {
        if (fileName.isEmpty()) return null
        val file = File(getImagesDir(context), fileName)
        return if (file.exists()) file else null
    }

    suspend fun exportDatabaseToJson(): String {
        val allAreas = database.thermalAreaDao().getAllThermalAreas().first()
        val allRooms = database.roomDao().getAllRooms().first()
        val allPanels = database.panelDao().getAllPanels().first()
        val allRelays = database.relayDao().getAllRelays().first()
        val allProcesses = database.childProcessDao().getAllChildProcesses().first()
        val allSubProcesses = database.subProcessDao().getAllSubProcesses().first()

        val nestedAreas = allAreas.map { area ->
            val areaRooms = allRooms.filter { it.thermalAreaId == area.id }.map { room ->
                val roomPanels = allPanels.filter { it.roomId == room.id }.map { panel ->
                    val panelRelays = allRelays.filter { it.panelId == panel.id }.map { relay ->
                        val relayProcesses = allProcesses.filter { it.relayId == relay.id }.map { process ->
                            process.apply { 
                                subProcesses = fetchRecursiveSubProcesses(process.id, "CHILD", allSubProcesses)
                            }
                        }
                        relay.apply { childProcesses = relayProcesses }
                    }
                    panel.apply { relays = panelRelays }
                }
                room.apply { panels = roomPanels }
            }
            area.apply { rooms = areaRooms }
        }

        val export = ZPowerData(thermalAreas = nestedAreas)
        return json.encodeToString(export)
    }

    private fun fetchRecursiveSubProcesses(parentId: Long, parentType: String, allSubProcesses: List<SubProcess>): List<SubProcess> {
        return allSubProcesses.filter { it.parentId == parentId && it.parentType == parentType }
            .map { subProcess ->
                subProcess.apply {
                    subProcesses = fetchRecursiveSubProcesses(subProcess.id, "SUB", allSubProcesses)
                }
            }
    }

    /**
     * Performs a clean import: clears all tables and inserts data from JSON.
     * Uses a mapping strategy to maintain relationships while avoiding ID conflicts.
     * Returns true if successful.
     */
    suspend fun importDatabaseFromJson(jsonString: String): Boolean {
        if (jsonString.isBlank()) {
            android.util.Log.w("DatabaseRepository", "Import aborted: JSON string is empty.")
            return false
        }

        isSyncing = true
        return try {
            android.util.Log.d("DatabaseRepository", "Starting clean import...")
            val data = try {
                json.decodeFromString<ZPowerData>(jsonString)
            } catch (e: Exception) {
                android.util.Log.e("DatabaseRepository", "Malformed JSON during import", e)
                return false
            }
            
            database.withTransaction {
                // Critical: Clear all existing data to ensure "Clean Import"
                database.subProcessDao().deleteAll()
                database.childProcessDao().deleteAll()
                database.relayDao().deleteAll()
                database.panelDao().deleteAll()
                database.roomDao().deleteAll()
                database.thermalAreaDao().deleteAll()
                
                data.thermalAreas.forEach { area ->
                    val newAreaId = database.thermalAreaDao().insert(area.copy(id = 0))
                    
                    area.rooms.forEach { room ->
                        val newRoomId = database.roomDao().insert(room.copy(id = 0, thermalAreaId = newAreaId))
                        
                        room.panels.forEach { panel ->
                            val newPanelId = database.panelDao().insert(panel.copy(id = 0, roomId = newRoomId))
                            
                            panel.relays.forEach { relay ->
                                val newRelayId = database.relayDao().insert(relay.copy(id = 0, panelId = newPanelId))
                                
                                relay.childProcesses.forEach { process ->
                                    val newProcessId = database.childProcessDao().insert(process.copy(id = 0, relayId = newRelayId))
                                    saveRecursiveSubProcesses(process.subProcesses, newProcessId, "CHILD")
                                }
                            }
                        }
                    }
                }
            }
            android.util.Log.d("DatabaseRepository", "Clean import successful.")
            true
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Clean import failed with exception", e)
            false
        } finally {
            // Delay resetting isSyncing slightly to allow Room invalidation to settle
            delay(800)
            isSyncing = false
        }
    }

    private suspend fun saveRecursiveSubProcesses(subProcesses: List<SubProcess>, parentId: Long, parentType: String) {
        subProcesses.forEach { subProcess ->
            val newSubId = database.subProcessDao().insert(subProcess.copy(id = 0, parentId = parentId, parentType = parentType))
            saveRecursiveSubProcesses(subProcess.subProcesses, newSubId, "SUB")
        }
    }

    /**
     * Robust synchronization logic:
     * 1. If data.json exists:
     *    - If DB is empty OR data.json is newer than last sync, import it.
     * 2. If data.json is missing:
     *    - If we have a record of a previous sync, it means the user deleted the file.
     *      In this case, clear the internal database to match the user's intent.
     */
    suspend fun syncDataWithJson(context: Context) = withContext(Dispatchers.IO) {
        if (isSyncing || !hasStoragePermission()) {
            if (!hasStoragePermission()) android.util.Log.w("DatabaseRepository", "Sync skipped: MANAGE_EXTERNAL_STORAGE permission missing.")
            return@withContext
        }
        try {
            ensureStorageDirectories(context)
            val dataFile = getDataFile(context)
            val lastSyncTime = settingsRepository.lastJsonSyncTime.first()
            val dbIsEmpty = database.thermalAreaDao().getAllThermalAreas().first().isEmpty()

            if (dataFile.exists()) {
                val currentFileTime = dataFile.lastModified()
                // Use a small buffer to avoid millisecond precision issues
                if (dbIsEmpty || currentFileTime > lastSyncTime + 1000) {
                    android.util.Log.d("DatabaseRepository", "Detected file change (File: $currentFileTime, Last Sync: $lastSyncTime). Triggering clean import.")
                    val jsonString = dataFile.readText()
                    if (importDatabaseFromJson(jsonString)) {
                        settingsRepository.updateLastJsonSyncTime(currentFileTime)
                    }
                } else {
                    android.util.Log.d("DatabaseRepository", "JSON file is not newer than last sync. Skipping import.")
                }
            } else if (lastSyncTime > 0) {
                // File was deleted by the user
                android.util.Log.d("DatabaseRepository", "data.json missing but previously synced. Clearing database to match.")
                isSyncing = true
                try {
                    database.withTransaction {
                    database.subProcessDao().deleteAll()
                    database.childProcessDao().deleteAll()
                    database.relayDao().deleteAll()
                        database.panelDao().deleteAll()
                        database.roomDao().deleteAll()
                        database.thermalAreaDao().deleteAll()
                    }
                    settingsRepository.updateLastJsonSyncTime(0)
                    settingsRepository.updateLastDbUpdateTime(System.currentTimeMillis())
                } finally {
                    delay(800)
                    isSyncing = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Failed to sync with JSON", e)
        }
    }

    suspend fun forceRefreshData(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!hasStoragePermission()) {
            android.util.Log.w("DatabaseRepository", "Force refresh skipped: MANAGE_EXTERNAL_STORAGE permission missing.")
            return@withContext false
        }
        val dataFile = getDataFile(context)
        val absolutePath = dataFile.absolutePath
        android.util.Log.d("DatabaseRepository", "Checking for data.json at: $absolutePath")
        
        if (dataFile.exists()) {
            android.util.Log.d("DatabaseRepository", "Force refreshing data from JSON...")
            val jsonString = dataFile.readText()
            if (importDatabaseFromJson(jsonString)) {
                settingsRepository.updateLastJsonSyncTime(dataFile.lastModified())
                settingsRepository.updateLastDbUpdateTime(System.currentTimeMillis())
                return@withContext true
            }
        } else {
            android.util.Log.w("DatabaseRepository", "Force refresh failed: data.json not found at $absolutePath")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: data.json not found at $absolutePath", Toast.LENGTH_LONG).show()
            }
        }
        false
    }

    suspend fun syncDatabaseToJson(context: Context) = withContext(Dispatchers.IO) {
        if (isSyncing || !hasStoragePermission()) {
            if (!hasStoragePermission()) android.util.Log.w("DatabaseRepository", "Export skipped: MANAGE_EXTERNAL_STORAGE permission missing.")
            return@withContext
        }
        try {
            ensureStorageDirectories(context)
            val dataFile = getDataFile(context)
            val lastDbUpdate = settingsRepository.lastDbUpdateTime.first()
            val fileLastModified = if (dataFile.exists()) dataFile.lastModified() else 0L

            // Only export if DB is newer than the file
            if (!dataFile.exists() || lastDbUpdate > fileLastModified) {
                val jsonString = exportDatabaseToJson()
                dataFile.writeText(jsonString)
                val newModified = dataFile.lastModified()
                settingsRepository.updateLastJsonSyncTime(newModified)
                android.util.Log.d("DatabaseRepository", "Database synced to JSON at: ${dataFile.absolutePath} (DB Update: $lastDbUpdate, File: $newModified)")
            } else {
                android.util.Log.d("DatabaseRepository", "Skipping export: DB ($lastDbUpdate) is NOT newer than file ($fileLastModified)")
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Failed to sync database to JSON", e)
        }
    }

    /**
     * Forces mirroring of both Room DB (as JSON) and Settings (as preferences_pb)
     * to the public Gold Knowledge folder.
     */
    private suspend fun mirrorDataForExport(context: Context) {
        android.util.Log.d("DatabaseRepository", "Mirroring: Starting force sync for export...")
        
        // 1. Force write data.json
        try {
            val jsonString = exportDatabaseToJson()
            val dataFile = getDataFile(context)
            dataFile.parentFile?.mkdirs()
            dataFile.writeText(jsonString)
            android.util.Log.d("DatabaseRepository", "Mirroring: data.json written to ${dataFile.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Mirroring: Failed to write data.json", e)
        }

        // 2. Force mirror settings
        try {
            settingsRepository.syncInternalToExternal()
            android.util.Log.d("DatabaseRepository", "Mirroring: Settings sync attempted")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Mirroring: Failed to sync settings", e)
        }
    }

    suspend fun exportDatabaseToZip(context: Context, outputStream: OutputStream, type: BackupType = BackupType.DATA_ONLY) = withContext(Dispatchers.IO) {
        if (!hasStoragePermission()) {
            android.util.Log.e("DatabaseRepository", "Export failed: MANAGE_EXTERNAL_STORAGE permission missing")
            throw IOException("MANAGE_EXTERNAL_STORAGE permission missing")
        }

        android.util.Log.d("DatabaseRepository", "Starting ZIP export (Type: $type)")
        
        // Ensure settings and data are mirrored to Gold Knowledge public folder first
        mirrorDataForExport(context)

        val tempZipFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.zip")
        try {
            val zipFile = ZipFile(tempZipFile)
            val zipParameters = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
            }

            // 1. Finding & Adding data/data.json
            android.util.Log.d("DatabaseRepository", "[Step 1] Finding Data Files...")
            val publicDataFile = getDataFile(context)
            if (publicDataFile.exists()) {
                android.util.Log.d("DatabaseRepository", "[Step 1] Adding data.json to ZIP (Source: ${publicDataFile.absolutePath})")
                val dataZipParams = ZipParameters(zipParameters).apply {
                    fileNameInZip = "data/data.json"
                }
                zipFile.addFile(publicDataFile, dataZipParams)
            }
            
            val bgImageFile = File(getDataDir(context), "background.webp")
            if (bgImageFile.exists()) {
                android.util.Log.d("DatabaseRepository", "[Step 1] Adding background.webp to ZIP")
                val bgZipParams = ZipParameters(zipParameters).apply {
                    fileNameInZip = "data/background.webp"
                }
                zipFile.addFile(bgImageFile, bgZipParams)
            }

            // 2. Finding & Adding images/ folder
            android.util.Log.d("DatabaseRepository", "[Step 2] Finding Images folder...")
            val imagesDir = getImagesDir(context)
            if (imagesDir.exists() && imagesDir.isDirectory && imagesDir.listFiles()?.isNotEmpty() == true) {
                android.util.Log.d("DatabaseRepository", "[Step 2] Adding images folder to ZIP...")
                zipFile.addFolder(imagesDir, zipParameters)
                android.util.Log.d("DatabaseRepository", "[Step 2] Images folder added")
            } else {
                android.util.Log.d("DatabaseRepository", "[Step 2] Images folder empty or missing, skipping")
            }

            // 3. Finding & Adding Settings if COMPLETE
            if (type == BackupType.COMPLETE) {
                android.util.Log.d("DatabaseRepository", "[Step 3] Finding Settings File...")
                val settingsFile = settingsRepository.getCurrentSettingsFile()
                android.util.Log.d("DatabaseRepository", "[Step 3] Source settings file: ${settingsFile.absolutePath} (Exists: ${settingsFile.exists()})")

                if (settingsFile.exists()) {
                    val currentSettings = settingsRepository.settingsData.first()
                    val shareBotApi = currentSettings.shareBotApiInBackup
                    val isTelegramEnabled = currentSettings.isTelegramEnabled
                    
                    android.util.Log.d("DatabaseRepository", "[Step 3] Include API Key toggle: $shareBotApi, Telegram enabled: $isTelegramEnabled")
                    
                    val tempSettingsFolder = File(context.cacheDir, "temp_settings_${System.currentTimeMillis()}")
                    tempSettingsFolder.mkdirs()
                    val targetSettingsFile = File(tempSettingsFolder, "settings.preferences_pb")

                    try {
                        android.util.Log.d("DatabaseRepository", "[Step 3] Scrubbing Settings...")
                        settingsFile.copyTo(targetSettingsFile, overwrite = true)
                        
                        // Scrub logic: ensure legacy and optionally custom tokens are removed
                        val scrubDataStore = PreferenceDataStoreFactory.create(produceFile = { targetSettingsFile })
                        try {
                            scrubDataStore.edit { prefs ->
                                // Requirement 1: Always clear legacy token
                                prefs.remove(stringPreferencesKey("telegram_bot_token"))
                                
                                // Requirement 1: If shareBotApiInBackup is FALSE -> telegramBotToken MUST be empty
                                if (!shareBotApi) {
                                    prefs.remove(stringPreferencesKey("custom_bot_token"))
                                }
                                
                                // Requirement 2: If isTelegramEnabled is FALSE -> clear token, chatId, and recipientUserIds
                                if (!isTelegramEnabled) {
                                    android.util.Log.d("DatabaseRepository", "Scrubbing Telegram data from export: Telegram is disabled")
                                    prefs.remove(stringPreferencesKey("custom_bot_token"))
                                    prefs.remove(stringPreferencesKey("telegram_chat_id"))
                                    prefs.remove(stringPreferencesKey("telegram_group_id"))
                                    prefs.remove(stringPreferencesKey("recipient_user_ids"))
                                }
                            }
                            android.util.Log.d("DatabaseRepository", "Scrubbing API Key from export: Success")
                        } catch (e: Exception) {
                            android.util.Log.e("DatabaseRepository", "Scrubbing API Key from export: Failure", e)
                        }
                        
                        // Small delay to ensure DataStore has finished writing to disk
                        delay(500)

                        if (targetSettingsFile.exists() && targetSettingsFile.length() > 0) {
                            android.util.Log.d("DatabaseRepository", "[Step 3] Adding Settings to ZIP (Size: ${targetSettingsFile.length()} bytes)")
                            val settingsZipParams = ZipParameters(zipParameters).apply {
                                fileNameInZip = "data/settings.preferences_pb"
                            }
                            zipFile.addFile(targetSettingsFile, settingsZipParams)
                            android.util.Log.d("DatabaseRepository", "[Step 3] Settings added successfully")
                        } else {
                            android.util.Log.w("DatabaseRepository", "[Step 3] Settings file is empty after scrubbing!")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DatabaseRepository", "[Step 3] Error scrubbing/adding settings", e)
                    } finally {
                        targetSettingsFile.delete()
                        tempSettingsFolder.deleteRecursively()
                    }
                } else {
                    android.util.Log.w("DatabaseRepository", "[Step 3] Settings file not found at ${settingsFile.absolutePath}")
                }
            }

            // Copy temp zip to outputStream
            android.util.Log.d("DatabaseRepository", "Writing final ZIP to output stream...")
            tempZipFile.inputStream().use { it.copyTo(outputStream) }
            android.util.Log.d("DatabaseRepository", "ZIP export successful")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "ZIP export failed", e)
            throw e
        } finally {
            if (tempZipFile.exists()) tempZipFile.delete()
        }
    }

    suspend fun importDatabaseFromZip(
        context: Context, 
        inputStream: InputStream, 
        includeImages: Boolean = true,
        onSettingsFound: (suspend (incomingToken: String?, applySettings: suspend (keepCurrentToken: Boolean) -> Unit) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        if (!hasStoragePermission()) {
            throw IOException("MANAGE_EXTERNAL_STORAGE permission missing")
        }
        
        isSyncing = true
        val tempZipFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.zip")
        try {
            android.util.Log.d("DatabaseRepository", "Starting Zip4j import. Include Images: $includeImages")
            
            // Save InputStream to temp file
            tempZipFile.outputStream().use { inputStream.copyTo(it) }

            val zipFile = ZipFile(tempZipFile)
            if (!zipFile.isValidZipFile) {
                throw IOException("Invalid ZIP file")
            }

            // 1. Extract data/data.json
            val dataEntry = zipFile.getFileHeader("data/data.json") ?: zipFile.getFileHeader("data.json")
            if (dataEntry != null) {
                val extractTempDir = File(context.cacheDir, "extract_temp_${System.currentTimeMillis()}")
                extractTempDir.mkdirs()
                zipFile.extractFile(dataEntry, extractTempDir.absolutePath)
                
                val extractedFile = File(extractTempDir, dataEntry.fileName)
                
                if (extractedFile.exists()) {
                    val jsonString = extractedFile.readText()
                    importDatabaseFromJson(jsonString)

                    // Sync to Gold Knowledge
                    val dataFile = getDataFile(context)
                    dataFile.parentFile?.mkdirs()
                    dataFile.writeText(jsonString)
                }
                extractTempDir.deleteRecursively()
            }

            // 1.5 Extract data/background.webp
            val bgEntry = zipFile.getFileHeader("data/background.webp")
            if (bgEntry != null) {
                val extractTempDir = File(context.cacheDir, "extract_bg_temp_${System.currentTimeMillis()}")
                extractTempDir.mkdirs()
                zipFile.extractFile(bgEntry, extractTempDir.absolutePath)
                val extractedFile = File(extractTempDir, bgEntry.fileName)
                if (extractedFile.exists()) {
                    val destFile = File(getDataDir(context), "background.webp")
                    extractedFile.copyTo(destFile, overwrite = true)
                }
                extractTempDir.deleteRecursively()
            }

            // 2. Extract images/
            if (includeImages) {
                val imagesDir = getImagesDir(context)
                imagesDir.mkdirs()
                
                zipFile.fileHeaders.forEach { header ->
                    if (header.fileName.startsWith("images/") && !header.isDirectory) {
                        // Extract to imagesDir's parent (Gold Knowledge) to maintain the "images/" prefix in the zip
                        zipFile.extractFile(header, imagesDir.parentFile!!.absolutePath)
                    }
                }
            }

            // 3. Extract settings
            val settingsEntry = zipFile.getFileHeader("data/settings.preferences_pb") 
                ?: zipFile.getFileHeader("datastore/settings.preferences_pb") // Fallback for old backups
            
            if (settingsEntry != null) {
                android.util.Log.d("DatabaseRepository", "Settings file found in ZIP: ${settingsEntry.fileName}")
                val tempSettings = File(context.cacheDir, "incoming_settings.preferences_pb")
                val extractSettingsDir = File(context.cacheDir, "extract_settings_${System.currentTimeMillis()}")
                extractSettingsDir.mkdirs()
                
                try {
                    zipFile.extractFile(settingsEntry, extractSettingsDir.absolutePath)
                    val extractedSettings = File(extractSettingsDir, settingsEntry.fileName)
                    
                    if (extractedSettings.exists()) {
                        android.util.Log.d("DatabaseRepository", "Settings extracted successfully, size: ${extractedSettings.length()}")
                        extractedSettings.copyTo(tempSettings, overwrite = true)
                        
                        if (onSettingsFound != null) {
                            android.util.Log.d("DatabaseRepository", "Triggering smart import for settings...")
                            val incomingToken = peekTokenFromSettingsFile(tempSettings)
                            onSettingsFound(incomingToken) { keepCurrent ->
                                applySettingsSafely(context, tempSettings, keepCurrent)
                                tempSettings.delete()
                            }
                        } else {
                            android.util.Log.d("DatabaseRepository", "Applying settings directly...")
                            val destSettings = settingsRepository.getCurrentSettingsFile()
                            destSettings.parentFile?.mkdirs()
                            tempSettings.copyTo(destSettings, overwrite = true)
                            tempSettings.delete()
                            ExternalDataStore.clearInstance()
                            settingsRepository.notifyPermissionChanged()
                        }
                    } else {
                        android.util.Log.e("DatabaseRepository", "Extracted settings file NOT found at expected path: ${extractedSettings.absolutePath}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseRepository", "Error extracting/applying settings", e)
                } finally {
                    extractSettingsDir.deleteRecursively()
                }
            } else {
                android.util.Log.d("DatabaseRepository", "No settings file found in ZIP")
            }
            
            android.util.Log.d("DatabaseRepository", "Zip4j import successful")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Zip4j import failed", e)
            throw e
        } finally {
            if (tempZipFile.exists()) tempZipFile.delete()
            isSyncing = false
        }
    }

    private suspend fun peekTokenFromSettingsFile(file: File): String? {
        val ds = PreferenceDataStoreFactory.create(produceFile = { file })
        val prefs = ds.data.first()
        return prefs[stringPreferencesKey("custom_bot_token")]
    }

    private suspend fun applySettingsSafely(context: Context, incomingFile: File, keepCurrentToken: Boolean) {
        val destFile = settingsRepository.getCurrentSettingsFile()
        
        if (keepCurrentToken) {
            // Get current token
            val currentToken = settingsRepository.customBotToken.first()
            
            // Edit the incoming file to restore current token before applying
            val incomingDs = PreferenceDataStoreFactory.create(produceFile = { incomingFile })
            incomingDs.edit { prefs ->
                val tokenKey = stringPreferencesKey("custom_bot_token")
                if (currentToken.isNotBlank()) {
                    prefs[tokenKey] = currentToken
                } else {
                    prefs.remove(tokenKey)
                }
            }
        }
        
        // Copy to final destination
        destFile.parentFile?.mkdirs()
        incomingFile.copyTo(destFile, overwrite = true)
        
        // Notify repository to refresh
        ExternalDataStore.clearInstance()
        settingsRepository.notifyPermissionChanged()
    }

    suspend fun uploadBackupToTelegram(
        zipFile: File,
        botToken: String, // Kept for compatibility
        personalChatId: String,
        groupChatId: String = "",
        rootTitle: String = "Backups",
        additionalRecipients: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val effectiveToken = getEffectiveBotToken()
        val client = OkHttpClient.Builder()
            .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val caption = "[ZPower] Personal Backup | Date: $date Time: $time Heading: $rootTitle Good Luck"

        val mediaType = "application/zip".toMediaType()
        
        // 1. Prioritize Personal Upload
        var personalUploadResult: TelegramMessageResponse? = null
        if (personalChatId.isNotBlank()) {
            val personalBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", personalChatId)
                .addFormDataPart("caption", caption)
                .addFormDataPart("document", zipFile.name, zipFile.asRequestBody(mediaType))
                .build()

            val personalRequest = Request.Builder()
                .url("https://api.telegram.org/bot$effectiveToken/sendDocument")
                .post(personalBody)
                .build()

            try {
                android.util.Log.d("DatabaseRepository", "Uploading to primary chatId: $personalChatId")
                client.newCall(personalRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        personalUploadResult = json.decodeFromString<TelegramMessageResponse>(response.body?.string() ?: "")
                        android.util.Log.d("DatabaseRepository", "Primary upload successful")
                    } else {
                        android.util.Log.e("DatabaseRepository", "Personal upload failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseRepository", "Personal upload exception", e)
            }
        }

        // 2. Optional Group Upload (Secondary)
        if (groupChatId.isNotBlank()) {
            val finalGroupChatId = if (!groupChatId.startsWith("@") && !groupChatId.startsWith("-")) {
                if (groupChatId.length >= 9) "-100$groupChatId" else "-$groupChatId"
            } else {
                groupChatId
            }

            val groupBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", finalGroupChatId)
                .addFormDataPart("caption", "$caption (Community Copy)")
            
            // Optimization: if personal upload succeeded, use file_id for group upload
            if (personalUploadResult?.ok == true && personalUploadResult?.result?.document != null) {
                groupBodyBuilder.addFormDataPart("document", personalUploadResult?.result?.document?.file_id ?: "")
            } else {
                groupBodyBuilder.addFormDataPart("document", zipFile.name, zipFile.asRequestBody(mediaType))
            }

            val groupRequest = Request.Builder()
                .url("https://api.telegram.org/bot$effectiveToken/sendDocument")
                .post(groupBodyBuilder.build())
                .build()

            try {
                android.util.Log.d("DatabaseRepository", "Uploading to group chatId: $finalGroupChatId")
                client.newCall(groupRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.e("DatabaseRepository", "Group upload failed (non-blocking): ${response.code}")
                    } else {
                        android.util.Log.d("DatabaseRepository", "Group upload successful")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseRepository", "Group upload exception (non-blocking)", e)
            }
        }

        // 3. Additional Recipients (Multi-Forwarding)
        if (additionalRecipients.isNotBlank()) {
            val recipients = additionalRecipients.split(",").map { it.trim() }.filter { it.isNotBlank() }
            recipients.forEach { recipientId ->
                val bodyBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", recipientId)
                    .addFormDataPart("caption", "$caption (Forwarded)")
                
                if (personalUploadResult?.ok == true && personalUploadResult?.result?.document != null) {
                    bodyBuilder.addFormDataPart("document", personalUploadResult?.result?.document?.file_id ?: "")
                } else {
                    bodyBuilder.addFormDataPart("document", zipFile.name, zipFile.asRequestBody(mediaType))
                }

                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$effectiveToken/sendDocument")
                    .post(bodyBuilder.build())
                    .build()

                try {
                    android.util.Log.d("DatabaseRepository", "Uploading to additional recipient: $recipientId")
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            android.util.Log.e("DatabaseRepository", "Upload to $recipientId failed: ${response.code}")
                        } else {
                            android.util.Log.d("DatabaseRepository", "Upload to $recipientId successful")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseRepository", "Upload to $recipientId exception", e)
                }
            }
        }

        // Return result based on personal upload (or if at least one worked, but prioritizing personal)
        if (personalUploadResult?.ok == true) {
            Result.success(Unit)
        } else if (personalChatId.isBlank() && (groupChatId.isNotBlank() || additionalRecipients.isNotBlank())) {
            // Fallback for when only group or additional is provided
            Result.success(Unit) 
        } else {
            Result.failure(IOException("Primary (Personal) Upload Failed. Make sure you have started the bot (@Zedbackupbot) in a private chat to receive backups."))
        }
    }

    suspend fun fetchBackupListFromTelegram(
        botToken: String, // Kept for compatibility
        chatId: String
    ): Result<List<TelegramBackupItem>> = withContext(Dispatchers.IO) {
        val effectiveToken = getEffectiveBotToken()
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        try {
            val targetChatId = chatId.toLongOrNull()
            
            val updatesRequest = Request.Builder()
                .url("https://api.telegram.org/bot$effectiveToken/getUpdates?limit=100")
                .build()

            val updatesResponse = client.newCall(updatesRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to get updates: ${response.code}")
                json.decodeFromString<TelegramUpdateResponse>(response.body?.string() ?: "")
            }

            if (!updatesResponse.ok) throw IOException("Telegram API error in getUpdates")

            val backups = updatesResponse.result
                .mapNotNull { it.message ?: it.edited_message ?: it.channel_post ?: it.edited_channel_post }
                .filter { msg ->
                    // Requirement: Only ZIP files sent or forwarded by the CURRENT USER'S ID
                    val senderId = msg.from?.id
                    val forwardFromId = msg.forward_from?.id
                    
                    val isFromCurrentUser = targetChatId != null && (senderId == targetChatId || forwardFromId == targetChatId)
                    
                    isFromCurrentUser && msg.document?.file_name?.endsWith(".zip", ignoreCase = true) == true
                }
                .map { msg ->
                    TelegramBackupItem(
                        fileId = msg.document!!.file_id,
                        name = msg.document.file_name ?: "Unknown",
                        date = msg.date * 1000L,
                        size = msg.document.file_size ?: 0L
                    )
                }
                .sortedByDescending { it.date }

            Result.success(backups)
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Failed to fetch backup list", e)
            Result.failure(e)
        }
    }

    suspend fun importSpecificTelegramBackup(
        botToken: String, // Kept for compatibility
        fileId: String, 
        includeImages: Boolean,
        onSettingsFound: (suspend (incomingToken: String?, applySettings: suspend (keepCurrentToken: Boolean) -> Unit) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val effectiveToken = getEffectiveBotToken()
        val client = OkHttpClient.Builder()
            .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        try {
            // 1. Get File Path
            val getFileRequest = Request.Builder()
                .url("https://api.telegram.org/bot$effectiveToken/getFile?file_id=$fileId")
                .build()

            val fileResponse = client.newCall(getFileRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to get file info: ${response.code}")
                json.decodeFromString<TelegramFileResponse>(response.body?.string() ?: "")
            }

            val filePath = fileResponse.result?.file_path ?: throw IOException("Could not retrieve file path")

            // 2. Download File
            val downloadUrl = "https://api.telegram.org/file/bot$effectiveToken/$filePath"
            val downloadRequest = Request.Builder().url(downloadUrl).build()

            val tempFile = File.createTempFile("telegram_backup", ".zip", context.cacheDir)
            client.newCall(downloadRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to download backup: ${response.code}")
                response.body?.byteStream()?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // 3. Import
            tempFile.inputStream().use {
                importDatabaseFromZip(context, it, includeImages, onSettingsFound)
            }

            // 4. Cleanup
            tempFile.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Telegram specific fetch failed", e)
            Result.failure(e)
        }
    }

    suspend fun duplicateHierarchyBranch(sourceId: Long, sourceType: String, newParentId: Long, newParentType: String = "") {
        database.withTransaction {
            when (sourceType) {
                "THERMAL_AREA" -> {
                    val rooms = database.roomDao().getRoomsForThermalAreaSync(sourceId)
                    rooms.forEach { room ->
                        val newRoomId = database.roomDao().insert(room.copy(id = 0, thermalAreaId = newParentId))
                        duplicateHierarchyBranch(room.id, "ROOM", newRoomId)
                    }
                }
                "ROOM" -> {
                    val panels = database.panelDao().getPanelsForRoomSync(sourceId)
                    panels.forEach { panel ->
                        val newPanelId = database.panelDao().insert(panel.copy(id = 0, roomId = newParentId))
                        duplicateHierarchyBranch(panel.id, "PANEL", newPanelId)
                    }
                }
                "PANEL" -> {
                    val relays = database.relayDao().getRelaysForPanelSync(sourceId)
                    relays.forEach { relay ->
                        val newRelayId = database.relayDao().insert(relay.copy(id = 0, panelId = newParentId))
                        duplicateHierarchyBranch(relay.id, "RELAY", newRelayId)
                    }
                }
                "RELAY" -> {
                    val childProcesses = database.childProcessDao().getChildProcessesForRelaySync(sourceId)
                    childProcesses.forEach { process ->
                        val newProcessId = database.childProcessDao().insert(process.copy(id = 0, relayId = newParentId))
                        duplicateHierarchyBranch(process.id, "CHILD_PROCESS", newProcessId)
                    }
                }
                "CHILD_PROCESS" -> {
                    val subProcesses = database.subProcessDao().getSubProcessesByParentSync(sourceId, "CHILD")
                    subProcesses.forEach { sub ->
                        val newSubId = database.subProcessDao().insert(sub.copy(id = 0, parentId = newParentId, parentType = "CHILD"))
                        duplicateHierarchyBranch(sub.id, "SUB_PROCESS", newSubId)
                    }
                }
                "SUB_PROCESS" -> {
                    val subProcesses = database.subProcessDao().getSubProcessesByParentSync(sourceId, "SUB")
                    subProcesses.forEach { sub ->
                        val newSubId = database.subProcessDao().insert(sub.copy(id = 0, parentId = newParentId, parentType = "SUB"))
                        duplicateHierarchyBranch(sub.id, "SUB_PROCESS", newSubId)
                    }
                }
            }
        }
    }

    suspend fun fetchLatestFromTelegram(
        botToken: String, // Kept for compatibility
        chatId: String,
        onSettingsFound: (suspend (incomingToken: String?, applySettings: suspend (keepCurrentToken: Boolean) -> Unit) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val listResult = fetchBackupListFromTelegram(botToken, chatId)
            val latest = listResult.getOrNull()?.firstOrNull() ?: return@withContext Result.failure(Exception("No backup found on Telegram"))
            
            importSpecificTelegramBackup(botToken, latest.fileId, includeImages = true, onSettingsFound)
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Telegram fetch failed", e)
            Result.failure(e)
        }
    }

    suspend fun wipeAllData(context: Context) = withContext(Dispatchers.IO) {
        isSyncing = true
        try {
            database.withTransaction {
                database.subProcessDao().deleteAll()
                database.childProcessDao().deleteAll()
                database.relayDao().deleteAll()
                database.panelDao().deleteAll()
                database.roomDao().deleteAll()
                database.thermalAreaDao().deleteAll()
            }
            
            // Delete data.json and background.webp
            val dataFile = getDataFile(context)
            if (dataFile.exists()) dataFile.delete()
            val bgFile = File(getDataDir(context), "background.webp")
            if (bgFile.exists()) bgFile.delete()
            
            // Delete settings preferences
            val settingsFile = settingsRepository.getCurrentSettingsFile()
            if (settingsFile.exists()) settingsFile.delete()
            
            // Delete entire images folder
            val imagesDir = getImagesDir(context)
            if (imagesDir.exists()) {
                imagesDir.deleteRecursively()
            }
            
            // Reset sync times
            settingsRepository.updateLastJsonSyncTime(0L)
            settingsRepository.updateLastDbUpdateTime(0L)
            
            // Re-ensure directories for future use
            ensureStorageDirectories(context)
            
            // Reset DataStore instance
            ExternalDataStore.clearInstance()
            settingsRepository.notifyPermissionChanged()
            
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Wipe all data failed", e)
        } finally {
            delay(1000)
            isSyncing = false
        }
    }

    suspend fun fetchBackupByLink(botToken: String, chatId: String, messageLink: String): Result<TelegramBackupItem> = withContext(Dispatchers.IO) {
        val effectiveToken = getEffectiveBotToken()
        
        // Robust link parsing: https://t.me/zpowerdata/123 or https://t.me/c/123456789/123
        val parts = messageLink.split("/")
        val messageId = parts.lastOrNull()?.toLongOrNull() ?: return@withContext Result.failure(Exception("Invalid message link format"))

        val finalChatId = if (!chatId.startsWith("@") && !chatId.startsWith("-")) {
            if (chatId.length >= 9) "-100$chatId" else "-$chatId"
        } else {
            chatId
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        try {
            // We search in updates. If not found, we inform the user to forward it to the bot.
            val updatesRequest = Request.Builder()
                .url("https://api.telegram.org/bot$effectiveToken/getUpdates?limit=100")
                .build()

            val updatesResponse = client.newCall(updatesRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to get updates: ${response.code}")
                json.decodeFromString<TelegramUpdateResponse>(response.body?.string() ?: "")
            }

            val targetMsg = updatesResponse.result
                .mapNotNull { it.message ?: it.edited_message ?: it.channel_post ?: it.edited_channel_post }
                .find { it.message_id == messageId }

            if (targetMsg?.document != null) {
                return@withContext Result.success(
                    TelegramBackupItem(
                        fileId = targetMsg.document.file_id,
                        name = targetMsg.document.file_name ?: "Backup",
                        date = targetMsg.date * 1000L,
                        size = targetMsg.document.file_size ?: 0L
                    )
                )
            }
            
            Result.failure(Exception("Backup not found in bot's recent memory. Tip: Forward the message from the group to @Zedbackupbot first."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
