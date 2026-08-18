package com.app.zpower.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient

@Serializable
@Entity(tableName = "thermal_areas")
data class ThermalArea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @SerialName("Name") val name: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Image") val imagePath: String? = null
) {
    @Ignore @SerialName("Rooms") var rooms: List<RoomEntity> = emptyList()
    @Ignore @Transient var sourceId: Long? = null
    @Ignore @Transient var fullBranch: Boolean = false
}

@Serializable
@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = ThermalArea::class,
            parentColumns = ["id"],
            childColumns = ["thermalAreaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["thermalAreaId"])]
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val thermalAreaId: Long = 0,
    @SerialName("Name") val name: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Image") val imagePath: String? = null
) {
    @Ignore @SerialName("Panels") var panels: List<PanelEntity> = emptyList()
    @Ignore @Transient var sourceId: Long? = null
    @Ignore @Transient var fullBranch: Boolean = false
}

@Serializable
@Entity(
    tableName = "panels",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["roomId"])]
)
data class PanelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long = 0,
    @SerialName("Name") val name: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Image") val imagePath: String? = null
) {
    @Ignore @SerialName("Relays") var relays: List<RelayEntity> = emptyList()
    @Ignore @Transient var sourceId: Long? = null
    @Ignore @Transient var fullBranch: Boolean = false
}

@Serializable
@Entity(
    tableName = "relays",
    foreignKeys = [
        ForeignKey(
            entity = PanelEntity::class,
            parentColumns = ["id"],
            childColumns = ["panelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["panelId"])]
)
data class RelayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val panelId: Long = 0,
    @SerialName("Name") val name: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Image") val imagePath: String? = null
) {
    @Ignore @SerialName("ChildProcesses") var childProcesses: List<ChildProcess> = emptyList()
    @Ignore @Transient var sourceId: Long? = null
    @Ignore @Transient var fullBranch: Boolean = false
}

@Serializable
data class CustomSection(
    @SerialName("Title") val title: String = "",
    @SerialName("Content") val content: String = ""
)

@Serializable
@Entity(
    tableName = "child_processes",
    foreignKeys = [
        ForeignKey(
            entity = RelayEntity::class,
            parentColumns = ["id"],
            childColumns = ["relayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["relayId"])]
)
data class ChildProcess(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val relayId: Long = 0,
    @SerialName("Name") val name: String = "",
    @SerialName("Image") val imagePath: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Work") val work: String = "",
    @SerialName("Uses") val uses: String = "",
    @SerialName("Sections") val sections: List<CustomSection> = emptyList()
) {
    @Ignore @SerialName("SubProcesses") var subProcesses: List<SubProcess> = emptyList()
    @Ignore @Transient var sourceId: Long? = null
    @Ignore @Transient var fullBranch: Boolean = false
}

@Serializable
@Entity(
    tableName = "sub_processes",
    indices = [Index(value = ["parentId"])]
)
data class SubProcess(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long = 0,
    val parentType: String = "CHILD", // "CHILD" or "SUB"
    @SerialName("Name") val name: String = "",
    @SerialName("Image") val imagePath: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Work") val work: String = "",
    @SerialName("Uses") val uses: String = ""
) {
    @Ignore @SerialName("SubProcesses") var subProcesses: List<SubProcess> = emptyList()
    @Ignore @Transient var sourceId: Long? = null
    @Ignore @Transient var fullBranch: Boolean = false
}
