package com.app.zpower.data.dao

import androidx.room.*
import com.app.zpower.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ThermalAreaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thermalArea: ThermalArea): Long

    @Update
    suspend fun update(thermalArea: ThermalArea)

    @Delete
    suspend fun delete(thermalArea: ThermalArea)

    @Query("SELECT * FROM thermal_areas")
    fun getAllThermalAreas(): Flow<List<ThermalArea>>

    @Query("SELECT DISTINCT name FROM thermal_areas")
    fun getAllThermalAreaNames(): Flow<List<String>>

    @Query("SELECT * FROM thermal_areas WHERE id = :id")
    suspend fun getThermalAreaById(id: Long): ThermalArea?

    @Query("DELETE FROM thermal_areas")
    suspend fun deleteAll()
}

@Dao
interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(room: RoomEntity): Long

    @Update
    suspend fun update(room: RoomEntity)

    @Delete
    suspend fun delete(room: RoomEntity)

    @Query("SELECT * FROM rooms WHERE thermalAreaId = :thermalAreaId")
    fun getRoomsForThermalArea(thermalAreaId: Long): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE thermalAreaId = :thermalAreaId")
    suspend fun getRoomsForThermalAreaSync(thermalAreaId: Long): List<RoomEntity>

    @Query("SELECT * FROM rooms")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT DISTINCT name FROM rooms")
    fun getAllRoomNames(): Flow<List<String>>

    @Query("DELETE FROM rooms")
    suspend fun deleteAll()
}

@Dao
interface PanelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(panel: PanelEntity): Long

    @Update
    suspend fun update(panel: PanelEntity)

    @Delete
    suspend fun delete(panel: PanelEntity)

    @Query("SELECT * FROM panels WHERE roomId = :roomId")
    fun getPanelsForRoom(roomId: Long): Flow<List<PanelEntity>>

    @Query("SELECT * FROM panels WHERE roomId = :roomId")
    suspend fun getPanelsForRoomSync(roomId: Long): List<PanelEntity>

    @Query("SELECT * FROM panels")
    fun getAllPanels(): Flow<List<PanelEntity>>

    @Query("SELECT DISTINCT name FROM panels")
    fun getAllPanelNames(): Flow<List<String>>

    @Query("DELETE FROM panels")
    suspend fun deleteAll()
}

@Dao
interface RelayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relay: RelayEntity): Long

    @Update
    suspend fun update(relay: RelayEntity)

    @Delete
    suspend fun delete(relay: RelayEntity)

    @Query("SELECT * FROM relays WHERE panelId = :panelId")
    fun getRelaysForPanel(panelId: Long): Flow<List<RelayEntity>>

    @Query("SELECT * FROM relays WHERE panelId = :panelId")
    suspend fun getRelaysForPanelSync(panelId: Long): List<RelayEntity>

    @Query("SELECT * FROM relays")
    fun getAllRelays(): Flow<List<RelayEntity>>

    @Query("SELECT DISTINCT name FROM relays")
    fun getAllRelayNames(): Flow<List<String>>

    @Query("DELETE FROM relays")
    suspend fun deleteAll()
}

@Dao
interface ChildProcessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(childProcess: ChildProcess): Long

    @Update
    suspend fun update(childProcess: ChildProcess)

    @Delete
    suspend fun delete(childProcess: ChildProcess)

    @Query("SELECT * FROM child_processes WHERE relayId = :relayId")
    fun getChildProcessesForRelay(relayId: Long): Flow<List<ChildProcess>>

    @Query("SELECT * FROM child_processes WHERE relayId = :relayId")
    suspend fun getChildProcessesForRelaySync(relayId: Long): List<ChildProcess>

    @Query("SELECT * FROM child_processes")
    fun getAllChildProcesses(): Flow<List<ChildProcess>>

    @Query("SELECT DISTINCT name FROM child_processes")
    fun getAllChildProcessNames(): Flow<List<String>>

    @Query("SELECT * FROM child_processes WHERE name = :name LIMIT 1")
    suspend fun getChildProcessByName(name: String): ChildProcess?

    @Query("DELETE FROM child_processes")
    suspend fun deleteAll()
}

@Dao
interface SubProcessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subProcess: SubProcess): Long

    @Update
    suspend fun update(subProcess: SubProcess)

    @Delete
    suspend fun delete(subProcess: SubProcess)

    @Query("SELECT * FROM sub_processes WHERE parentId = :parentId AND parentType = :parentType")
    fun getSubProcessesByParent(parentId: Long, parentType: String): Flow<List<SubProcess>>

    @Query("SELECT * FROM sub_processes WHERE parentId = :parentId AND parentType = :parentType")
    suspend fun getSubProcessesByParentSync(parentId: Long, parentType: String): List<SubProcess>

    @Query("SELECT * FROM sub_processes")
    fun getAllSubProcesses(): Flow<List<SubProcess>>

    @Query("SELECT DISTINCT name FROM sub_processes")
    fun getAllSubProcessNames(): Flow<List<String>>

    @Query("SELECT * FROM sub_processes WHERE name = :name LIMIT 1")
    suspend fun getSubProcessByName(name: String): SubProcess?

    @Query("DELETE FROM sub_processes")
    suspend fun deleteAll()
}

@Dao
interface SearchDao {
    @Query("""
        SELECT 'thermal_area' as type, id, name, description, name as path, imagePath as imagePath 
        FROM thermal_areas 
        WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'
        
        UNION ALL
        
        SELECT 'room' as type, r.id, r.name, r.description, t.name || ' > ' || r.name as path, r.imagePath as imagePath
        FROM rooms r
        JOIN thermal_areas t ON r.thermalAreaId = t.id
        WHERE r.name LIKE '%' || :query || '%' OR r.description LIKE '%' || :query || '%'
        
        UNION ALL
        
        SELECT 'panel' as type, p.id, p.name, p.description, t.name || ' > ' || r.name || ' > ' || p.name as path, p.imagePath as imagePath
        FROM panels p
        JOIN rooms r ON p.roomId = r.id
        JOIN thermal_areas t ON r.thermalAreaId = t.id
        WHERE p.name LIKE '%' || :query || '%' OR p.description LIKE '%' || :query || '%'
        
        UNION ALL
        
        SELECT 'relay' as type, re.id, re.name, re.description, t.name || ' > ' || r.name || ' > ' || p.name || ' > ' || re.name as path, re.imagePath as imagePath
        FROM relays re
        JOIN panels p ON re.panelId = p.id
        JOIN rooms r ON p.roomId = r.id
        JOIN thermal_areas t ON r.thermalAreaId = t.id
        WHERE re.name LIKE '%' || :query || '%' OR re.description LIKE '%' || :query || '%'
        
        UNION ALL
        
        SELECT 'child_process' as type, cp.id, cp.name, cp.description, t.name || ' > ' || r.name || ' > ' || p.name || ' > ' || re.name || ' > ' || cp.name as path, cp.imagePath as imagePath
        FROM child_processes cp
        JOIN relays re ON cp.relayId = re.id
        JOIN panels p ON re.panelId = p.id
        JOIN rooms r ON p.roomId = r.id
        JOIN thermal_areas t ON r.thermalAreaId = t.id
        WHERE cp.name LIKE '%' || :query || '%' OR cp.description LIKE '%' || :query || '%'
        
        UNION ALL
        
        SELECT 'sub_process' as type, sp.id, sp.name, sp.description, t.name || ' > ' || r.name || ' > ' || p.name || ' > ' || re.name || ' > ' || cp.name || ' > ' || sp.name as path, sp.imagePath as imagePath
        FROM sub_processes sp
        LEFT JOIN sub_processes psp ON sp.parentId = psp.id AND sp.parentType = 'SUB'
        LEFT JOIN child_processes cp ON (sp.parentId = cp.id AND sp.parentType = 'CHILD') OR (psp.parentId = cp.id AND psp.parentType = 'CHILD')
        JOIN relays re ON cp.relayId = re.id
        JOIN panels p ON re.panelId = p.id
        JOIN rooms r ON p.roomId = r.id
        JOIN thermal_areas t ON r.thermalAreaId = t.id
        WHERE sp.name LIKE '%' || :query || '%' OR sp.description LIKE '%' || :query || '%'
    """)
    suspend fun globalSearch(query: String): List<SearchResult>
}

data class SearchResult(
    val type: String,
    val id: Long,
    val name: String,
    val description: String,
    val path: String = "",
    val imagePath: String? = null
)
