package com.app.zpower.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.app.zpower.util.SecretObfuscator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class SettingsData(
    val accentColor: Int = 0xFF00BCD4.toInt(),
    val blurIntensity: Float = 15f,
    val blurEnabled: Boolean = true,
    val cardSize: String = "Medium",
    val rootTitle: String = "Thermal Area",
    val backgroundStyle: String = "liquid_gradient",
    val searchPathColor: Int = 0xFF9E9E9E.toInt(),
    val isTelegramEnabled: Boolean = false,
    val customBotToken: String = "",
    val telegramChatId: String = "",
    val telegramGroupId: String = "@zpowerdata",
    val adminPassword: String = "@#Abdullah542543",
    val shareBotApiInBackup: Boolean = false,
    val isCustomConfigActive: Boolean = false,
    val hasSeenJoinPrompt: Boolean = false,
    val recipientUserIds: String = "",
    val glassTextColor: Int = 0xFF5D4037.toInt(),
    val bgGradientColor1: Int = 0xFF0F2027.toInt(),
    val bgGradientColor2: Int = 0xFF203A43.toInt(),
    val bgGradientColor3: Int = 0xFF2C5364.toInt(),
    val bgImageUri: String = "",
    val wallpaperDim: Float = 0.4f
)

// Default internal dataStore for fallback or initial use if needed
val Context.internalDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepository(private val context: Context) {

    private val _permissionUpdateTrigger = MutableStateFlow(0)

    companion object {
        private val OBFUSCATED_TOKEN = byteArrayOf(
            66, 73, 90, 64, 85, 71, 106, 75, 92, 87, 72, 36, 53, 26, 52, 11,
            14, 53, 11, 99, 90, 121, 19, 69, 93, 13, 81, 5, 55, 21, 49, 9,
            3, 31, 24, 103, 41, 46, 29, 102, 100, 71, 83, 117, 63, 3
        )
        fun getAdminBotToken(): String = SecretObfuscator.decode(OBFUSCATED_TOKEN)
    }

    fun notifyPermissionChanged() {
        _permissionUpdateTrigger.value += 1
    }

    private object PreferencesKeys {
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val CARD_SIZE = stringPreferencesKey("card_size")
        val ROOT_TITLE = stringPreferencesKey("root_title")
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
        val SEARCH_PATH_COLOR = intPreferencesKey("search_path_color")
        val LAST_JSON_SYNC_TIME = longPreferencesKey("last_json_sync_time")
        val LAST_DB_UPDATE_TIME = longPreferencesKey("last_db_update_time")
        val IS_TELEGRAM_ENABLED = booleanPreferencesKey("is_telegram_enabled")
        val CUSTOM_BOT_TOKEN = stringPreferencesKey("custom_bot_token")
        val TELEGRAM_CHAT_ID = stringPreferencesKey("telegram_chat_id")
        val TELEGRAM_GROUP_ID = stringPreferencesKey("telegram_group_id")
        val ADMIN_PASSWORD = stringPreferencesKey("admin_password")
        val SHARE_BOT_API_IN_BACKUP = booleanPreferencesKey("share_bot_api_in_backup")
        val IS_CUSTOM_CONFIG_ACTIVE = booleanPreferencesKey("is_custom_config_active")
        val HAS_SEEN_JOIN_PROMPT = booleanPreferencesKey("has_seen_join_prompt")
        val RECIPIENT_USER_IDS = stringPreferencesKey("recipient_user_ids")
        val GLASS_TEXT_COLOR = intPreferencesKey("glass_text_color")
        val BG_GRADIENT_COLOR_1 = intPreferencesKey("bg_gradient_color_1")
        val BG_GRADIENT_COLOR_2 = intPreferencesKey("bg_gradient_color_2")
        val BG_GRADIENT_COLOR_3 = intPreferencesKey("bg_gradient_color_3")
        val BG_IMAGE_URI = stringPreferencesKey("bg_image_uri")
        val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
    }

    fun getCurrentSettingsFile(): File {
        val external = getExternalSettingsFile()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager() && external != null) {
            external
        } else {
            File(context.filesDir.parentFile, "datastore/settings.preferences_pb")
        }
    }

    fun syncInternalToExternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            val externalFile = getExternalSettingsFile() ?: return
            val internalFile = File(context.filesDir.parentFile, "datastore/settings.preferences_pb")
            
            if (internalFile.exists()) {
                // If external doesn't exist or internal is newer, copy it
                if (!externalFile.exists() || internalFile.lastModified() > externalFile.lastModified()) {
                    android.util.Log.d("SettingsRepository", "Mirroring internal settings to external Gold Knowledge storage")
                    internalFile.copyTo(externalFile, overwrite = true)
                }
            }
        }
    }

    private fun getExternalSettingsFile(): File? {
        return try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(publicDocs, "Gold Knowledge/data")
            if (!dir.exists() && !dir.mkdirs()) {
                android.util.Log.e("SettingsRepository", "Failed to create external settings directory")
                return null
            }
            File(dir, "settings.preferences_pb")
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error getting external settings file", e)
            null
        }
    }

    // Reactive DataStore selection
    private val activeDataStore: Flow<DataStore<Preferences>> = _permissionUpdateTrigger.map {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            val file = getExternalSettingsFile()
            if (file != null) {
                ExternalDataStore.getInstance(file)
            } else {
                context.internalDataStore
            }
        } else {
            context.internalDataStore
        }
    }.distinctUntilChanged()

    private fun <T> getSettingFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return activeDataStore.flatMapLatest { ds ->
            ds.data.map { preferences ->
                preferences[key] ?: defaultValue
            }
        }
    }

    val lastJsonSyncTime: Flow<Long> = getSettingFlow(PreferencesKeys.LAST_JSON_SYNC_TIME, 0L)

    suspend fun updateLastJsonSyncTime(time: Long) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.LAST_JSON_SYNC_TIME] = time
        }
    }

    val lastDbUpdateTime: Flow<Long> = getSettingFlow(PreferencesKeys.LAST_DB_UPDATE_TIME, 0L)

    suspend fun updateLastDbUpdateTime(time: Long) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.LAST_DB_UPDATE_TIME] = time
        }
    }

    val blurIntensity: Flow<Float> = getSettingFlow(PreferencesKeys.BLUR_INTENSITY, 15f)

    val accentColor: Flow<Int> = getSettingFlow(PreferencesKeys.ACCENT_COLOR, 0xFF00BCD4.toInt())

    val cardSize: Flow<String> = getSettingFlow(PreferencesKeys.CARD_SIZE, "Medium")

    val rootTitle: Flow<String> = getSettingFlow(PreferencesKeys.ROOT_TITLE, "Thermal Area")

    val backgroundStyle: Flow<String> = getSettingFlow(PreferencesKeys.BACKGROUND_STYLE, "liquid_gradient")

    val blurEnabled: Flow<Boolean> = getSettingFlow(PreferencesKeys.BLUR_ENABLED, true)

    val searchPathColor: Flow<Int> = getSettingFlow(PreferencesKeys.SEARCH_PATH_COLOR, 0xFF9E9E9E.toInt())

    val isTelegramEnabled: Flow<Boolean> = getSettingFlow(PreferencesKeys.IS_TELEGRAM_ENABLED, false)
    val customBotToken: Flow<String> = getSettingFlow(PreferencesKeys.CUSTOM_BOT_TOKEN, "")
    val telegramChatId: Flow<String> = getSettingFlow(PreferencesKeys.TELEGRAM_CHAT_ID, "")
    val telegramGroupId: Flow<String> = getSettingFlow(PreferencesKeys.TELEGRAM_GROUP_ID, "@zpowerdata")
    val adminPassword: Flow<String> = getSettingFlow(PreferencesKeys.ADMIN_PASSWORD, "@#Abdullah542543")
    val shareBotApiInBackup: Flow<Boolean> = getSettingFlow(PreferencesKeys.SHARE_BOT_API_IN_BACKUP, false)
    val isCustomConfigActive: Flow<Boolean> = getSettingFlow(PreferencesKeys.IS_CUSTOM_CONFIG_ACTIVE, false)
    val hasSeenJoinPrompt: Flow<Boolean> = getSettingFlow(PreferencesKeys.HAS_SEEN_JOIN_PROMPT, false)
    val recipientUserIds: Flow<String> = getSettingFlow(PreferencesKeys.RECIPIENT_USER_IDS, "")
    val glassTextColor: Flow<Int> = getSettingFlow(PreferencesKeys.GLASS_TEXT_COLOR, 0xFF5D4037.toInt())
    val bgGradientColor1: Flow<Int> = getSettingFlow(PreferencesKeys.BG_GRADIENT_COLOR_1, 0xFF0F2027.toInt())
    val bgGradientColor2: Flow<Int> = getSettingFlow(PreferencesKeys.BG_GRADIENT_COLOR_2, 0xFF203A43.toInt())
    val bgGradientColor3: Flow<Int> = getSettingFlow(PreferencesKeys.BG_GRADIENT_COLOR_3, 0xFF2C5364.toInt())
    val bgImageUri: Flow<String> = getSettingFlow(PreferencesKeys.BG_IMAGE_URI, "")
    val wallpaperDim: Flow<Float> = getSettingFlow(PreferencesKeys.WALLPAPER_DIM, 0.4f)

    val settingsData: Flow<SettingsData> = activeDataStore.flatMapLatest { ds ->
        ds.data.map { prefs ->
            mapSettings(prefs)
        }
    }

    fun mapSettings(prefs: Preferences): SettingsData {
        return SettingsData(
            accentColor = prefs[PreferencesKeys.ACCENT_COLOR] ?: 0xFF00BCD4.toInt(),
            blurIntensity = prefs[PreferencesKeys.BLUR_INTENSITY] ?: 15f,
            blurEnabled = prefs[PreferencesKeys.BLUR_ENABLED] ?: true,
            cardSize = prefs[PreferencesKeys.CARD_SIZE] ?: "Medium",
            rootTitle = prefs[PreferencesKeys.ROOT_TITLE] ?: "Thermal Area",
            backgroundStyle = prefs[PreferencesKeys.BACKGROUND_STYLE] ?: "liquid_gradient",
            searchPathColor = prefs[PreferencesKeys.SEARCH_PATH_COLOR] ?: 0xFF9E9E9E.toInt(),
            isTelegramEnabled = prefs[PreferencesKeys.IS_TELEGRAM_ENABLED] ?: false,
            customBotToken = prefs[PreferencesKeys.CUSTOM_BOT_TOKEN] ?: "",
            telegramChatId = prefs[PreferencesKeys.TELEGRAM_CHAT_ID] ?: "",
            telegramGroupId = prefs[PreferencesKeys.TELEGRAM_GROUP_ID] ?: "@zpowerdata",
            adminPassword = prefs[PreferencesKeys.ADMIN_PASSWORD] ?: "@#Abdullah542543",
            shareBotApiInBackup = prefs[PreferencesKeys.SHARE_BOT_API_IN_BACKUP] ?: false,
            isCustomConfigActive = prefs[PreferencesKeys.IS_CUSTOM_CONFIG_ACTIVE] ?: false,
            hasSeenJoinPrompt = prefs[PreferencesKeys.HAS_SEEN_JOIN_PROMPT] ?: false,
            recipientUserIds = prefs[PreferencesKeys.RECIPIENT_USER_IDS] ?: "",
            glassTextColor = prefs[PreferencesKeys.GLASS_TEXT_COLOR] ?: 0xFF5D4037.toInt(),
            bgGradientColor1 = prefs[PreferencesKeys.BG_GRADIENT_COLOR_1] ?: 0xFF0F2027.toInt(),
            bgGradientColor2 = prefs[PreferencesKeys.BG_GRADIENT_COLOR_2] ?: 0xFF203A43.toInt(),
            bgGradientColor3 = prefs[PreferencesKeys.BG_GRADIENT_COLOR_3] ?: 0xFF2C5364.toInt(),
            bgImageUri = prefs[PreferencesKeys.BG_IMAGE_URI] ?: "",
            wallpaperDim = prefs[PreferencesKeys.WALLPAPER_DIM] ?: 0.4f
        )
    }

    @Suppress("unused")
    suspend fun saveSettings(data: SettingsData) {
        activeDataStore.first().edit { prefs ->
            prefs[PreferencesKeys.ACCENT_COLOR] = data.accentColor
            prefs[PreferencesKeys.BLUR_INTENSITY] = data.blurIntensity
            prefs[PreferencesKeys.BLUR_ENABLED] = data.blurEnabled
            prefs[PreferencesKeys.CARD_SIZE] = data.cardSize
            prefs[PreferencesKeys.ROOT_TITLE] = data.rootTitle
            prefs[PreferencesKeys.BACKGROUND_STYLE] = data.backgroundStyle
            prefs[PreferencesKeys.SEARCH_PATH_COLOR] = data.searchPathColor
            prefs[PreferencesKeys.IS_TELEGRAM_ENABLED] = data.isTelegramEnabled
            prefs[PreferencesKeys.CUSTOM_BOT_TOKEN] = data.customBotToken
            prefs[PreferencesKeys.TELEGRAM_CHAT_ID] = data.telegramChatId
            prefs[PreferencesKeys.TELEGRAM_GROUP_ID] = data.telegramGroupId
            prefs[PreferencesKeys.ADMIN_PASSWORD] = data.adminPassword
            prefs[PreferencesKeys.SHARE_BOT_API_IN_BACKUP] = data.shareBotApiInBackup
            prefs[PreferencesKeys.IS_CUSTOM_CONFIG_ACTIVE] = data.isCustomConfigActive
            prefs[PreferencesKeys.HAS_SEEN_JOIN_PROMPT] = data.hasSeenJoinPrompt
            prefs[PreferencesKeys.RECIPIENT_USER_IDS] = data.recipientUserIds
            prefs[PreferencesKeys.GLASS_TEXT_COLOR] = data.glassTextColor
            prefs[PreferencesKeys.BG_GRADIENT_COLOR_1] = data.bgGradientColor1
            prefs[PreferencesKeys.BG_GRADIENT_COLOR_2] = data.bgGradientColor2
            prefs[PreferencesKeys.BG_GRADIENT_COLOR_3] = data.bgGradientColor3
            prefs[PreferencesKeys.BG_IMAGE_URI] = data.bgImageUri
            prefs[PreferencesKeys.WALLPAPER_DIM] = data.wallpaperDim
        }
    }

    suspend fun updateBlurIntensity(intensity: Float) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.BLUR_INTENSITY] = intensity
        }
    }

    suspend fun updateAccentColor(color: Int) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = color
        }
    }

    suspend fun updateCardSize(size: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.CARD_SIZE] = size
        }
    }

    suspend fun updateRootTitle(title: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.ROOT_TITLE] = title
        }
    }

    suspend fun updateBackgroundStyle(style: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_STYLE] = style
        }
    }

    suspend fun updateBlurEnabled(enabled: Boolean) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.BLUR_ENABLED] = enabled
        }
    }

    suspend fun updateSearchPathColor(color: Int) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.SEARCH_PATH_COLOR] = color
        }
    }

    suspend fun updateTelegramEnabled(enabled: Boolean) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.IS_TELEGRAM_ENABLED] = enabled
        }
    }

    suspend fun updateCustomBotToken(token: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_BOT_TOKEN] = token
        }
    }

    suspend fun updateTelegramChatId(chatId: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.TELEGRAM_CHAT_ID] = chatId
        }
    }

    suspend fun updateTelegramGroupId(groupId: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.TELEGRAM_GROUP_ID] = groupId
        }
    }

    suspend fun updateAdminPassword(password: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.ADMIN_PASSWORD] = password
        }
    }

    suspend fun updateShareBotApiInBackup(share: Boolean) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.SHARE_BOT_API_IN_BACKUP] = share
        }
    }

    suspend fun updateCustomConfigActive(active: Boolean) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.IS_CUSTOM_CONFIG_ACTIVE] = active
        }
    }

    suspend fun updateHasSeenJoinPrompt(seen: Boolean) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_JOIN_PROMPT] = seen
        }
    }

    suspend fun updateRecipientUserIds(ids: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.RECIPIENT_USER_IDS] = ids
        }
    }

    suspend fun updateGlassTextColor(color: Int) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.GLASS_TEXT_COLOR] = color
        }
    }

    suspend fun updateBgGradientColor1(color: Int) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.BG_GRADIENT_COLOR_1] = color
        }
    }

    suspend fun updateBgGradientColor2(color: Int) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.BG_GRADIENT_COLOR_2] = color
        }
    }

    suspend fun updateBgGradientColor3(color: Int) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.BG_GRADIENT_COLOR_3] = color
        }
    }

    suspend fun updateBgImageUri(uri: String) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.BG_IMAGE_URI] = uri
        }
    }

    suspend fun updateWallpaperDim(dim: Float) {
        activeDataStore.first().edit { preferences ->
            preferences[PreferencesKeys.WALLPAPER_DIM] = dim
        }
    }

    suspend fun resetToDefault() {
        activeDataStore.first().edit { it.clear() }
    }
}

object ExternalDataStore {
    private var instance: DataStore<Preferences>? = null

    fun getInstance(file: File): DataStore<Preferences> {
        return instance ?: synchronized(this) {
            instance ?: PreferenceDataStoreFactory.create(
                produceFile = { file }
            ).also { instance = it }
        }
    }

    fun clearInstance() {
        synchronized(this) {
            instance = null
        }
    }
}
