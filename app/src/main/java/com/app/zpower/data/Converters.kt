package com.app.zpower.data

import androidx.room.TypeConverter
import com.app.zpower.data.entity.CustomSection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json

    @TypeConverter
    fun fromCustomSectionList(value: List<CustomSection>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toCustomSectionList(value: String): List<CustomSection> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
