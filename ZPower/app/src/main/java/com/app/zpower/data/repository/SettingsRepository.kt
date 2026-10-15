package com.app.zpower.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import java.io.File

// Default internal dataStore for fallback or initial use if needed
val Context.internalDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val _permissionUpdateTrigger = MutableStateFlow(0)

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

    @OptIn(ExperimentalCoroutinesApi::class)
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

    val backgroundStyle: Flow<String> = getSettingFlow(PreferencesKeys.BACKGROUND_STYLE, "glass_gradient")

    val blurEnabled: Flow<Boolean> = getSettingFlow(PreferencesKeys.BLUR_ENABLED, true)

    val searchPathColor: Flow<Int> = getSettingFlow(PreferencesKeys.SEARCH_PATH_COLOR, 0xFF9E9E9E.toInt())

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
}
