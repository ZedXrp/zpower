package com.app.zpower.ui.navigation

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.zpower.data.ZPowerDatabase
import com.app.zpower.data.dao.SearchResult
import com.app.zpower.data.entity.*
import com.app.zpower.data.repository.DatabaseRepository
import com.app.zpower.data.repository.BackupType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.app.zpower.data.repository.SettingsRepository
import com.app.zpower.data.repository.TelegramBackupItem
import com.app.zpower.ui.theme.BackgroundStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

class NavigationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ZPowerDatabase.getDatabase(application)
    private val settingsRepository = SettingsRepository(application)
    val repository = DatabaseRepository(database, application, settingsRepository)

    init {
        // DatabaseRepository handles initial sync in its own init block
    }

    private val _backStack = mutableStateListOf<ZPowerRoute>(ZPowerRoute.ThermalAreaList)
    val backStack: List<ZPowerRoute> get() = _backStack

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: StateFlow<Int> = _refreshTrigger.asStateFlow()

    fun triggerRefresh() {
        _refreshTrigger.value += 1
    }

    // Undo/Redo Mechanism
    val snackbarHostState = SnackbarHostState()
    private val undoStack = mutableStateListOf<Command>()
    private val redoStack = mutableStateListOf<Command>()

    data class Command(
        val name: String,
        val undo: suspend () -> Unit,
        val redo: suspend () -> Unit
    )

    private fun addCommand(name: String, undo: suspend () -> Unit, redo: suspend () -> Unit) {
        undoStack.add(Command(name, undo, redo))
        if (undoStack.size > 20) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        viewModelScope.launch {
            if (undoStack.isNotEmpty()) {
                val command = undoStack.removeAt(undoStack.size - 1)
                command.undo()
                redoStack.add(command)
                snackbarHostState.showSnackbar("Undone: ${command.name}")
            }
        }
    }

    fun redo() {
        viewModelScope.launch {
            if (redoStack.isNotEmpty()) {
                val command = redoStack.removeAt(redoStack.size - 1)
                command.redo()
                undoStack.add(command)
                snackbarHostState.showSnackbar("Redone: ${command.name}")
            }
        }
    }

    // CRUD with Undo Support
    fun deleteThermalArea(area: ThermalArea) {
        viewModelScope.launch {
            repository.getThermalAreaDao().delete(area)
            addCommand(
                name = "Delete ${area.name}",
                undo = { 
                    repository.getThermalAreaDao().insert(area)
                },
                redo = { 
                    repository.getThermalAreaDao().delete(area)
                }
            )
        }
    }

    fun updateThermalArea(area: ThermalArea, oldArea: ThermalArea) {
        viewModelScope.launch {
            repository.getThermalAreaDao().update(area)
            addCommand(
                name = "Update ${area.name}",
                undo = { 
                    repository.getThermalAreaDao().update(oldArea)
                },
                redo = { 
                    repository.getThermalAreaDao().update(area)
                }
            )
        }
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch {
            repository.getRoomDao().delete(room)
            addCommand(
                name = "Delete ${room.name}",
                undo = { 
                    repository.getRoomDao().insert(room)
                },
                redo = { 
                    repository.getRoomDao().delete(room)
                }
            )
        }
    }

    fun updateRoom(room: RoomEntity, oldRoom: RoomEntity) {
        viewModelScope.launch {
            repository.getRoomDao().update(room)
            addCommand(
                name = "Update ${room.name}",
                undo = { 
                    repository.getRoomDao().update(oldRoom)
                },
                redo = { 
                    repository.getRoomDao().update(room)
                }
            )
        }
    }

    fun deletePanel(panel: PanelEntity) {
        viewModelScope.launch {
            repository.getPanelDao().delete(panel)
            addCommand(
                name = "Delete ${panel.name}",
                undo = { 
                    repository.getPanelDao().insert(panel)
                },
                redo = { 
                    repository.getPanelDao().delete(panel)
                }
            )
        }
    }

    fun updatePanel(panel: PanelEntity, oldPanel: PanelEntity) {
        viewModelScope.launch {
            repository.getPanelDao().update(panel)
            addCommand(
                name = "Update ${panel.name}",
                undo = { 
                    repository.getPanelDao().update(oldPanel)
                },
                redo = { 
                    repository.getPanelDao().update(panel)
                }
            )
        }
    }

    fun deleteRelay(relay: RelayEntity) {
        viewModelScope.launch {
            repository.getRelayDao().delete(relay)
            addCommand(
                name = "Delete ${relay.name}",
                undo = { 
                    repository.getRelayDao().insert(relay)
                },
                redo = { 
                    repository.getRelayDao().delete(relay)
                }
            )
        }
    }

    fun updateRelay(relay: RelayEntity, oldRelay: RelayEntity) {
        viewModelScope.launch {
            repository.getRelayDao().update(relay)
            addCommand(
                name = "Update ${relay.name}",
                undo = { 
                    repository.getRelayDao().update(oldRelay)
                },
                redo = { 
                    repository.getRelayDao().update(relay)
                }
            )
        }
    }

    fun deleteChildProcess(childProcess: ChildProcess) {
        viewModelScope.launch {
            repository.getChildProcessDao().delete(childProcess)
            addCommand(
                name = "Delete ${childProcess.name}",
                undo = { 
                    repository.getChildProcessDao().insert(childProcess)
                },
                redo = { 
                    repository.getChildProcessDao().delete(childProcess)
                }
            )
            pop() // Go back after delete
        }
    }

    fun updateChildProcess(childProcess: ChildProcess, oldChildProcess: ChildProcess) {
        viewModelScope.launch {
            repository.getChildProcessDao().update(childProcess)
            addCommand(
                name = "Update ${childProcess.name}",
                undo = { 
                    repository.getChildProcessDao().update(oldChildProcess)
                },
                redo = { 
                    repository.getChildProcessDao().update(childProcess)
                }
            )
        }
    }

    fun deleteSubProcess(subProcess: SubProcess) {
        viewModelScope.launch {
            repository.getSubProcessDao().delete(subProcess)
            addCommand(
                name = "Delete ${subProcess.name}",
                undo = { 
                    repository.getSubProcessDao().insert(subProcess)
                },
                redo = { 
                    repository.getSubProcessDao().delete(subProcess)
                }
            )
            pop() // Go back after delete
        }
    }

    fun updateSubProcess(subProcess: SubProcess, oldSubProcess: SubProcess) {
        viewModelScope.launch {
            repository.getSubProcessDao().update(subProcess)
            addCommand(
                name = "Update ${subProcess.name}",
                undo = { 
                    repository.getSubProcessDao().update(oldSubProcess)
                },
                redo = { 
                    repository.getSubProcessDao().update(subProcess)
                }
            )
        }
    }


    // Settings States
    val blurIntensity = settingsRepository.blurIntensity.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15f)
    val accentColor = settingsRepository.accentColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF00BCD4.toInt())
    val cardSize = settingsRepository.cardSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Medium")
    val rootTitle = settingsRepository.rootTitle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Thermal Area")
    val backgroundStyle = settingsRepository.backgroundStyle.map { BackgroundStyle.fromId(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundStyle.LIQUID_GRADIENT)
    val blurEnabled = settingsRepository.blurEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val searchPathColor = settingsRepository.searchPathColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF9E9E9E.toInt())

    val isTelegramEnabled = settingsRepository.isTelegramEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val customBotToken = settingsRepository.customBotToken.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val telegramChatId = settingsRepository.telegramChatId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val telegramGroupId = settingsRepository.telegramGroupId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "@zpowerdata")
    val adminPassword = settingsRepository.adminPassword.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "@#Abdullah542543")
    val shareBotApiInBackup = settingsRepository.shareBotApiInBackup.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isCustomConfigActive = settingsRepository.isCustomConfigActive.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hasSeenJoinPrompt = settingsRepository.hasSeenJoinPrompt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val recipientUserIds = settingsRepository.recipientUserIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val glassTextColor = settingsRepository.glassTextColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF5D4037.toInt())
    val bgGradientColor1 = settingsRepository.bgGradientColor1.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF0F2027.toInt())
    val bgGradientColor2 = settingsRepository.bgGradientColor2.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF203A43.toInt())
    val bgGradientColor3 = settingsRepository.bgGradientColor3.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF2C5364.toInt())
    val bgImageUri = settingsRepository.bgImageUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val wallpaperDim = settingsRepository.wallpaperDim.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.4f)

    var isEditMode by mutableStateOf(false)
        private set

    var showAbout by mutableStateOf(false)

    var fullScreenImageUri by mutableStateOf<String?>(null)
        private set

    fun updateBlurIntensity(intensity: Float) {
        viewModelScope.launch { settingsRepository.updateBlurIntensity(intensity) }
    }

    fun updateAccentColor(color: Color) {
        viewModelScope.launch { settingsRepository.updateAccentColor(color.toArgb()) }
    }

    fun updateCardSize(size: String) {
        viewModelScope.launch { settingsRepository.updateCardSize(size) }
    }

    fun updateRootTitle(title: String) {
        viewModelScope.launch { settingsRepository.updateRootTitle(title) }
    }

    fun updateBackgroundStyle(style: BackgroundStyle) {
        viewModelScope.launch { settingsRepository.updateBackgroundStyle(style.id) }
    }

    fun updateBlurEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateBlurEnabled(enabled) }
    }

    fun updateSearchPathColor(color: Color) {
        viewModelScope.launch { settingsRepository.updateSearchPathColor(color.toArgb()) }
    }

    fun updateTelegramEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTelegramEnabled(enabled) }
    }

    fun updateCustomBotToken(token: String) {
        viewModelScope.launch { settingsRepository.updateCustomBotToken(token) }
    }

    fun updateTelegramChatId(chatId: String) {
        viewModelScope.launch { settingsRepository.updateTelegramChatId(chatId) }
    }

    fun updateTelegramGroupId(groupId: String) {
        viewModelScope.launch { settingsRepository.updateTelegramGroupId(groupId) }
    }

    fun updateAdminPassword(password: String) {
        viewModelScope.launch { settingsRepository.updateAdminPassword(password) }
    }

    fun updateShareBotApiInBackup(share: Boolean) {
        viewModelScope.launch { settingsRepository.updateShareBotApiInBackup(share) }
    }

    fun updateCustomConfigActive(active: Boolean) {
        viewModelScope.launch { settingsRepository.updateCustomConfigActive(active) }
    }

    fun updateHasSeenJoinPrompt(seen: Boolean) {
        viewModelScope.launch { settingsRepository.updateHasSeenJoinPrompt(seen) }
    }

    fun updateRecipientUserIds(ids: String) {
        viewModelScope.launch { settingsRepository.updateRecipientUserIds(ids) }
    }

    fun updateGlassTextColor(color: Color) {
        viewModelScope.launch { settingsRepository.updateGlassTextColor(color.toArgb()) }
    }

    fun updateBgGradientColor1(color: Color) {
        viewModelScope.launch { settingsRepository.updateBgGradientColor1(color.toArgb()) }
    }

    fun updateBgGradientColor2(color: Color) {
        viewModelScope.launch { settingsRepository.updateBgGradientColor2(color.toArgb()) }
    }

    fun updateBgGradientColor3(color: Color) {
        viewModelScope.launch { settingsRepository.updateBgGradientColor3(color.toArgb()) }
    }

    fun updateBgImageUri(uri: String) {
        viewModelScope.launch { settingsRepository.updateBgImageUri(uri) }
    }

    fun updateWallpaperDim(dim: Float) {
        viewModelScope.launch { settingsRepository.updateWallpaperDim(dim) }
    }

    fun revertToDefaultBot() {
        viewModelScope.launch {
            settingsRepository.updateCustomBotToken("")
            settingsRepository.updateCustomConfigActive(false)
            snackbarHostState.showSnackbar("Reverted to Reliable System Bot")
        }
    }

    fun notifyPermissionChanged() {
        settingsRepository.notifyPermissionChanged()
    }

    // Long-Press Preview
    data class PreviewData(
        val name: String,
        val description: String,
        val imagePath: String?
    )

    var previewData by mutableStateOf<PreviewData?>(null)
        private set

    fun showPreview(name: String, description: String, imagePath: String?) {
        previewData = PreviewData(name, description, imagePath)
    }

    fun dismissPreview() {
        previewData = null
    }

    fun showFullScreenImage(uri: String) {
        fullScreenImageUri = uri
    }

    fun dismissFullScreenImage() {
        fullScreenImageUri = null
    }

    var searchQuery by mutableStateOf("")
        private set

    var isSearching by mutableStateOf(false)
        private set

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    // Suggestions Flows
    val thermalAreaSuggestions = repository.getThermalAreaDao().getAllThermalAreas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val roomSuggestions = repository.getRoomDao().getAllRooms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val panelSuggestions = repository.getPanelDao().getAllPanels().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val relaySuggestions = repository.getRelayDao().getAllRelays().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val childProcessSuggestions = repository.getChildProcessDao().getAllChildProcesses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subProcessSuggestions = repository.getSubProcessDao().getAllSubProcesses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateEditMode(enabled: Boolean) {
        isEditMode = enabled
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        if (query.length >= 2) {
            viewModelScope.launch {
                _searchResults.value = repository.globalSearch(query)
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun updateIsSearching(searching: Boolean) {
        isSearching = searching
        if (!searching) {
            searchQuery = ""
            _searchResults.value = emptyList()
        }
    }

    var isZipProcessing by mutableStateOf(false)
        private set

    private val _isTelegramUploading = MutableStateFlow(false)
    val isTelegramUploading: StateFlow<Boolean> = _isTelegramUploading.asStateFlow()

    private val _isTelegramFetching = MutableStateFlow(false)
    val isTelegramFetching: StateFlow<Boolean> = _isTelegramFetching.asStateFlow()

    private val _showTelegramBackupDialog = MutableStateFlow(false)
    val showTelegramBackupDialog: StateFlow<Boolean> = _showTelegramBackupDialog.asStateFlow()

    fun updateShowTelegramBackupDialog(show: Boolean) {
        _showTelegramBackupDialog.value = show
    }

    var showConnectionError by mutableStateOf(false)
        private set
    var connectionErrorMessage by mutableStateOf("")
        private set

    fun dismissConnectionError() {
        showConnectionError = false
    }

    private val _telegramBackups = MutableStateFlow<List<TelegramBackupItem>>(emptyList())
    val telegramBackups: StateFlow<List<TelegramBackupItem>> = _telegramBackups.asStateFlow()

    var showImportApiKeyDialog by mutableStateOf(false)
        private set
    var pendingIncomingToken by mutableStateOf<String?>(null)
        private set
    private var importChoiceDeferred: CompletableDeferred<Boolean>? = null

    fun resolveImportApiKeyChoice(keepCurrent: Boolean) {
        importChoiceDeferred?.complete(keepCurrent)
        showImportApiKeyDialog = false
    }

    private suspend fun handleSmartImport(incomingToken: String?, applySettings: suspend (keepCurrentToken: Boolean) -> Unit) {
        val currentToken = customBotToken.value
        if (!incomingToken.isNullOrBlank() && currentToken.isNotBlank()) {
            pendingIncomingToken = incomingToken
            importChoiceDeferred = CompletableDeferred()
            showImportApiKeyDialog = true
            val keepCurrent = importChoiceDeferred!!.await()
            applySettings(keepCurrent)
        } else {
            // If incoming has no token, keep current automatically (keepCurrent = true)
            // If incoming has a token but current doesn't, replace (keepCurrent = false)
            // Logic: keepCurrent = incomingToken.isNullOrBlank()
            applySettings(incomingToken.isNullOrBlank())
        }
    }

    fun loadTelegramBackups() {
        viewModelScope.launch {
            val effectiveToken = if (isCustomConfigActive.value) {
                customBotToken.value.takeIf { it.isNotBlank() } ?: SettingsRepository.getAdminBotToken()
            } else {
                SettingsRepository.getAdminBotToken()
            }
            val effectiveChatId = telegramChatId.value
            
            if (effectiveChatId.isBlank()) {
                snackbarHostState.showSnackbar("Telegram Personal ID missing")
                return@launch
            }

            _isTelegramFetching.value = true
            try {
                val result = repository.fetchBackupListFromTelegram(effectiveToken, effectiveChatId)
                if (result.isSuccess) {
                    _telegramBackups.value = result.getOrNull() ?: emptyList()
                    if (_telegramBackups.value.isEmpty()) {
                        snackbarHostState.showSnackbar("No backups found on Telegram")
                    } else {
                        _showTelegramBackupDialog.value = true
                    }
                } else {
                    snackbarHostState.showSnackbar("Failed to fetch backups: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                _isTelegramFetching.value = false
            }
        }
    }

    fun importSpecificTelegramBackup(fileId: String, includeImages: Boolean) {
        viewModelScope.launch {
            val effectiveToken = if (isCustomConfigActive.value) {
                customBotToken.value.takeIf { it.isNotBlank() } ?: SettingsRepository.getAdminBotToken()
            } else {
                SettingsRepository.getAdminBotToken()
            }
            
            _isTelegramFetching.value = true
            snackbarHostState.showSnackbar("Importing backup...")
            
            try {
                val result = repository.importSpecificTelegramBackup(effectiveToken, fileId, includeImages) { token, apply ->
                    handleSmartImport(token, apply)
                }
                if (result.isSuccess) {
                    triggerRefresh()
                    snackbarHostState.showSnackbar("Import Successful")
                } else {
                    snackbarHostState.showSnackbar("Import Failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Import Failed: ${e.message}")
            } finally {
                _isTelegramFetching.value = false
            }
        }
    }

    fun fetchLatestFromTelegram() {
        viewModelScope.launch {
            val effectiveToken = if (isCustomConfigActive.value) {
                customBotToken.value.takeIf { it.isNotBlank() } ?: SettingsRepository.getAdminBotToken()
            } else {
                SettingsRepository.getAdminBotToken()
            }
            val effectiveChatId = telegramChatId.value
            
            if (effectiveChatId.isBlank()) {
                snackbarHostState.showSnackbar("Telegram Personal ID missing")
                return@launch
            }

            _isTelegramFetching.value = true
            snackbarHostState.showSnackbar("Importing from Telegram...")
            
            try {
                val result = repository.fetchLatestFromTelegram(effectiveToken, effectiveChatId) { token, apply ->
                    handleSmartImport(token, apply)
                }
                if (result.isSuccess) {
                    triggerRefresh()
                    snackbarHostState.showSnackbar("Import Successful")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    snackbarHostState.showSnackbar(if (errorMsg.contains("No backup found")) errorMsg else "Import Failed: $errorMsg")
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationViewModel", "Telegram fetch failed", e)
                snackbarHostState.showSnackbar("Import Failed: ${e.message}")
            } finally {
                _isTelegramFetching.value = false
            }
        }
    }

    fun sendBackupToTelegram(type: BackupType = BackupType.DATA_ONLY) {
        viewModelScope.launch {
            val effectiveToken = if (isCustomConfigActive.value) {
                customBotToken.value.takeIf { it.isNotBlank() } ?: SettingsRepository.getAdminBotToken()
            } else {
                SettingsRepository.getAdminBotToken()
            }
            val effectiveChatId = telegramChatId.value
            
            if (effectiveChatId.isBlank()) {
                snackbarHostState.showSnackbar("Telegram Personal ID missing")
                return@launch
            }

            _isTelegramUploading.value = true
            try {
                val context = getApplication<Application>()
                val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val tempDir = File(publicDocs, "Gold Knowledge/temp")
                if (!tempDir.exists()) tempDir.mkdirs()
                
                val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                val safeHeader = rootTitle.value.replace(" ", "")
                val fileName = "ZPower_${safeHeader}_$date.zip"
                val zipFile = File(tempDir, fileName)
                
                FileOutputStream(zipFile).use { fos ->
                    repository.exportDatabaseToZip(context, fos, type)
                }
                
                val result = repository.uploadBackupToTelegram(
                    zipFile = zipFile,
                    botToken = effectiveToken,
                    personalChatId = effectiveChatId,
                    groupChatId = telegramGroupId.value,
                    rootTitle = rootTitle.value,
                    additionalRecipients = recipientUserIds.value
                )
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Upload Successful")
                } else {
                    snackbarHostState.showSnackbar("Upload Failed: ${result.exceptionOrNull()?.message}")
                }
                
                // Cleanup
                zipFile.delete()
                
            } catch (e: Exception) {
                android.util.Log.e("NavigationViewModel", "Telegram upload failed", e)
                snackbarHostState.showSnackbar("Upload Failed: ${e.message}")
            } finally {
                _isTelegramUploading.value = false
            }
        }
    }

    fun testTelegramConnection() {
        viewModelScope.launch {
            val isCustom = isCustomConfigActive.value
            val customToken = customBotToken.value
            
            val effectiveToken = if (isCustom && customToken.isNotBlank()) customToken else SettingsRepository.getAdminBotToken()

            _isTelegramFetching.value = true
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://api.telegram.org/bot$effectiveToken/getMe")
                    .build()
                
                val success = withContext(Dispatchers.IO) {
                    try {
                        client.newCall(request).execute().use { response ->
                            response.isSuccessful
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (success) {
                    snackbarHostState.showSnackbar("Connection Successful!")
                } else {
                    if (isCustom) {
                        connectionErrorMessage = "Custom Bot API test failed. Check your token or network."
                        showConnectionError = true
                    } else {
                        snackbarHostState.showSnackbar("System Bot Connection Failed")
                    }
                }
            } catch (e: Exception) {
                if (isCustomConfigActive.value) {
                    connectionErrorMessage = "Connection Error: ${e.message}"
                    showConnectionError = true
                } else {
                    snackbarHostState.showSnackbar("Connection Error: ${e.message}")
                }
            } finally {
                _isTelegramFetching.value = false
            }
        }
    }

    fun fetchBackupByLink(messageLink: String) {
        viewModelScope.launch {
            val effectiveToken = if (isCustomConfigActive.value) {
                customBotToken.value.takeIf { it.isNotBlank() } ?: SettingsRepository.getAdminBotToken()
            } else {
                SettingsRepository.getAdminBotToken()
            }
            val effectiveChatId = telegramChatId.value
            if (messageLink.isBlank()) {
                snackbarHostState.showSnackbar("Message link is empty")
                return@launch
            }
            _isTelegramFetching.value = true
            try {
                val result = repository.fetchBackupByLink(effectiveToken, effectiveChatId, messageLink)
                if (result.isSuccess) {
                    val item = result.getOrNull()
                    if (item != null) {
                        val currentList = _telegramBackups.value.toMutableList()
                        if (currentList.none { it.fileId == item.fileId }) {
                            currentList.add(0, item)
                            _telegramBackups.value = currentList
                        }
                        _showTelegramBackupDialog.value = true
                        snackbarHostState.showSnackbar("Backup metadata fetched")
                    }
                } else {
                    snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Fetch Failed")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                _isTelegramFetching.value = false
            }
        }
    }

    fun exportDataToZip(uri: Uri, type: BackupType = BackupType.DATA_ONLY, onComplete: () -> Unit) {
        viewModelScope.launch {
            isZipProcessing = true
            try {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    repository.exportDatabaseToZip(getApplication(), outputStream, type)
                    onComplete()
                    snackbarHostState.showSnackbar("Export successful")
                } ?: throw java.io.IOException("Could not open output stream")
            } catch (e: Exception) {
                android.util.Log.e("NavigationViewModel", "ZIP export failed with full stack trace", e)
                Toast.makeText(getApplication(), "Zip failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isZipProcessing = false
            }
        }
    }

    fun importDataFromZip(uri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch {
            isZipProcessing = true
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                    repository.importDatabaseFromZip(getApplication(), inputStream, true) { token, apply ->
                        handleSmartImport(token, apply)
                    }
                    onComplete()
                    triggerRefresh()
                    snackbarHostState.showSnackbar("Import successful")
                } ?: throw java.io.IOException("Could not open input stream")
            } catch (e: Exception) {
                android.util.Log.e("NavigationViewModel", "ZIP import failed with full stack trace", e)
                Toast.makeText(getApplication(), "Zip failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isZipProcessing = false
            }
        }
    }

    var isProcessingImage by mutableStateOf(false)
        private set

    fun saveImage(uri: Uri, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            repository.saveImage(getApplication(), uri).collect { result ->
                when (result.state) {
                    com.app.zpower.data.repository.ImageProcessingState.PROCESSING -> {
                        isProcessingImage = true
                    }
                    com.app.zpower.data.repository.ImageProcessingState.SUCCESS -> {
                        isProcessingImage = false
                        if (result.fileName.isNotEmpty()) {
                            onComplete(result.fileName)
                        }
                    }
                    com.app.zpower.data.repository.ImageProcessingState.ERROR -> {
                        isProcessingImage = false
                        snackbarHostState.showSnackbar("Error saving image: ${result.error}")
                    }
                    else -> {
                        isProcessingImage = false
                    }
                }
            }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.resetToDefault()
            snackbarHostState.showSnackbar("Settings reset to default")
            triggerRefresh()
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            snackbarHostState.showSnackbar("Wiping all data...")
            repository.wipeAllData(getApplication())
            triggerRefresh()
            navigateTo(ZPowerRoute.ThermalAreaList)
            snackbarHostState.showSnackbar("All data wiped successfully")
        }
    }

    fun forceRefreshData() {
        viewModelScope.launch {
            try {
                if (repository.forceRefreshData(getApplication())) {
                    triggerRefresh()
                    snackbarHostState.showSnackbar("Data refreshed from data.json")
                } else {
                    snackbarHostState.showSnackbar("Refresh failed: Check logs")
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationViewModel", "Manual refresh failed", e)
                snackbarHostState.showSnackbar("Refresh failed: ${e.message}")
            }
        }
    }

    fun syncExternalChanges() {
        viewModelScope.launch {
            try {
                repository.syncDataWithJson(getApplication())
            } catch (e: Exception) {
                android.util.Log.e("NavigationViewModel", "Background sync failed", e)
            }
        }
    }

    fun push(route: ZPowerRoute) {
        _backStack.add(route)
    }

    fun pop() {
        if (_backStack.size > 1) {
            _backStack.removeAt(_backStack.size - 1)
        }
    }

    fun navigateTo(route: ZPowerRoute) {
        val index = _backStack.indexOf(route)
        if (index != -1) {
            while (_backStack.size > index + 1) {
                _backStack.removeAt(_backStack.size - 1)
            }
        }
    }

    fun navigateToLevel(index: Int) {
        if (index >= 0 && index < _backStack.size) {
            while (_backStack.size > index + 1) {
                _backStack.removeAt(_backStack.size - 1)
            }
        }
    }

    fun navigateToSearchResult(result: SearchResult) {
        viewModelScope.launch {
            updateIsSearching(false)
            _backStack.clear()
            _backStack.add(ZPowerRoute.ThermalAreaList)

            when (result.type) {
                "thermal_area" -> {
                    // Navigate to ThermalAreaList (Root) - it's already there
                    // But we should highlight it? For now just staying at root is correct for "parent's list" 
                    // since Thermal Areas are at the root.
                }
                "room" -> {
                    val room = repository.getRoomDao().getAllRooms().first().find { it.id == result.id } ?: return@launch
                    val area = repository.getThermalAreaDao().getThermalAreaById(room.thermalAreaId) ?: return@launch
                    // Navigate to RoomList for this area
                    push(ZPowerRoute.RoomList(area.id, area.name))
                }
                "panel" -> {
                    val panel = repository.getPanelDao().getAllPanels().first().find { it.id == result.id } ?: return@launch
                    val room = repository.getRoomDao().getAllRooms().first().find { it.id == panel.roomId } ?: return@launch
                    val area = repository.getThermalAreaDao().getThermalAreaById(room.thermalAreaId) ?: return@launch
                    push(ZPowerRoute.RoomList(area.id, area.name))
                    // Navigate to PanelList for this room
                    push(ZPowerRoute.PanelList(room.id, room.name, area.name))
                }
                "relay" -> {
                    val relay = repository.getRelayDao().getAllRelays().first().find { it.id == result.id } ?: return@launch
                    val panel = repository.getPanelDao().getAllPanels().first().find { it.id == relay.panelId } ?: return@launch
                    val room = repository.getRoomDao().getAllRooms().first().find { it.id == panel.roomId } ?: return@launch
                    val area = repository.getThermalAreaDao().getThermalAreaById(room.thermalAreaId) ?: return@launch
                    push(ZPowerRoute.RoomList(area.id, area.name))
                    push(ZPowerRoute.PanelList(room.id, room.name, area.name))
                    // Navigate to RelayList for this panel
                    push(ZPowerRoute.RelayList(panel.id, panel.name, room.name, area.name))
                }
                "child_process" -> {
                    val process = repository.getChildProcessDao().getAllChildProcesses().first().find { it.id == result.id } ?: return@launch
                    val relay = repository.getRelayDao().getAllRelays().first().find { it.id == process.relayId } ?: return@launch
                    val panel = repository.getPanelDao().getAllPanels().first().find { it.id == relay.panelId } ?: return@launch
                    val room = repository.getRoomDao().getAllRooms().first().find { it.id == panel.roomId } ?: return@launch
                    val area = repository.getThermalAreaDao().getThermalAreaById(room.thermalAreaId) ?: return@launch
                    push(ZPowerRoute.RoomList(area.id, area.name))
                    push(ZPowerRoute.PanelList(room.id, room.name, area.name))
                    push(ZPowerRoute.RelayList(panel.id, panel.name, room.name, area.name))
                    // Navigate to ChildProcessList for this relay
                    push(ZPowerRoute.ChildProcessList(relay.id, relay.name, panel.name, room.name, area.name))
                }
                "sub_process" -> {
                    val subProcess = repository.getSubProcessDao().getAllSubProcesses().first().find { it.id == result.id } ?: return@launch
                    
                    val pathRoutes = mutableListOf<ZPowerRoute>()
                    var currentParentId = subProcess.parentId
                    var currentParentType = subProcess.parentType

                    if (currentParentType == "SUB") {
                        // Traverse up to find all parent sub-processes
                        while (currentParentType == "SUB") {
                            val parent = repository.getSubProcessDao().getAllSubProcesses().first().find { it.id == currentParentId }
                            if (parent != null) {
                                pathRoutes.add(0, ZPowerRoute.SubProcessList(
                                    parent.id, parent.parentType, parent.name, "...", "...", "...", "..."
                                ))
                                currentParentId = parent.parentId
                                currentParentType = parent.parentType
                            } else break
                        }
                    }
                    
                    // Now currentParentType should be "CHILD"
                    if (currentParentType == "CHILD") {
                        val process = repository.getChildProcessDao().getAllChildProcesses().first().find { it.id == currentParentId }
                        if (process != null) {
                            val relay = repository.getRelayDao().getAllRelays().first().find { it.id == process.relayId }
                            val panel = repository.getPanelDao().getAllPanels().first().find { it.id == relay?.panelId }
                            val room = repository.getRoomDao().getAllRooms().first().find { it.id == panel?.roomId }
                            val area = repository.getThermalAreaDao().getThermalAreaById(room?.thermalAreaId ?: -1)
                            
                            if (area != null && room != null && panel != null && relay != null) {
                                push(ZPowerRoute.RoomList(area.id, area.name))
                                push(ZPowerRoute.PanelList(room.id, room.name, area.name))
                                push(ZPowerRoute.RelayList(panel.id, panel.name, room.name, area.name))
                                push(ZPowerRoute.ChildProcessList(relay.id, relay.name, panel.name, room.name, area.name))
                                // This is the list where the sub-process exists (if it's a child of a ChildProcess)
                                push(ZPowerRoute.SubProcessList(process.id, "CHILD", process.name, relay.name, panel.name, room.name, area.name))
                            }
                        }
                    }
                    
                    pathRoutes.forEach { push(it) }
                    // The last push in pathRoutes (if any) is the list where the result exists.
                }
            }
        }
    }
}
