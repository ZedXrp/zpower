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
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class ZPowerData(
    @SerialName("ThermalAreas") val thermalAreas: List<ThermalArea> = emptyList()
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

    suspend fun exportDatabaseToZip(context: Context, outputStream: OutputStream, type: BackupType = BackupType.DATA_ONLY) = withContext(Dispatchers.IO) {
        if (!hasStoragePermission()) {
            throw IOException("MANAGE_EXTERNAL_STORAGE permission missing")
        }

        try {
            android.util.Log.d("DatabaseRepository", "Starting Standard ZIP export (Deflate)")
            ZipOutputStream(outputStream).use { zos ->
                zos.setMethod(ZipOutputStream.DEFLATED)
                
                // 1. Sync and Add data/data.json
                val jsonString = exportDatabaseToJson()
                val dataEntry = ZipEntry("data/data.json")
                zos.putNextEntry(dataEntry)
                zos.write(jsonString.toByteArray())
                zos.closeEntry()

                // 2. Add images/ folder
                val imagesDir = getImagesDir(context)
                if (imagesDir.exists() && imagesDir.isDirectory) {
                    imagesDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            val imageEntry = ZipEntry("images/${file.name}")
                            zos.putNextEntry(imageEntry)
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }

                // 3. Add Settings if COMPLETE
                if (type == BackupType.COMPLETE) {
                    val settingsFile = File(context.filesDir.parentFile, "datastore/settings.preferences_pb")
                    if (settingsFile.exists()) {
                        val settingsEntry = ZipEntry("datastore/settings.preferences_pb")
                        zos.putNextEntry(settingsEntry)
                        settingsFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            android.util.Log.d("DatabaseRepository", "ZIP export successful")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "ZIP export failed with full stack trace", e)
            throw e
        }
    }

    suspend fun importDatabaseFromZip(context: Context, inputStream: InputStream) = withContext(Dispatchers.IO) {
        if (!hasStoragePermission()) {
            throw IOException("MANAGE_EXTERNAL_STORAGE permission missing")
        }
        
        isSyncing = true
        try {
            android.util.Log.d("DatabaseRepository", "Starting Standard ZIP import (Deflate)")
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.getNextEntry()
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "data/data.json" -> {
                            val baos = ByteArrayOutputStream()
                            zis.copyTo(baos)
                            val jsonString = baos.toString("UTF-8")
                            
                            importDatabaseFromJson(jsonString)
                            
                            // Also sync to Gold Knowledge/data/data.json
                            val dataFile = getDataFile(context)
                            dataFile.parentFile?.mkdirs()
                            dataFile.writeText(jsonString)
                        }
                        name.startsWith("images/") && !entry.isDirectory -> {
                            val fileName = name.substringAfter("images/")
                            val imagesDir = getImagesDir(context)
                            imagesDir.mkdirs()
                            val destFile = File(imagesDir, fileName)
                            destFile.outputStream().use { zis.copyTo(it) }
                        }
                        name == "datastore/settings.preferences_pb" -> {
                            val destSettings = File(context.filesDir.parentFile, "datastore/settings.preferences_pb")
                            destSettings.parentFile?.mkdirs()
                            destSettings.outputStream().use { zis.copyTo(it) }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.getNextEntry()
                }
            }
            android.util.Log.d("DatabaseRepository", "ZIP import successful")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "ZIP import failed with full stack trace", e)
            throw e
        } finally {
            isSyncing = false
        }
    }
}
