package com.app.zpower.ui.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

@Serializable
sealed interface ZPowerRoute : NavKey {
    @Serializable
    data object ThermalAreaList : ZPowerRoute

    @Serializable
    data class RoomList(
        val thermalAreaId: Long,
        val thermalAreaName: String
    ) : ZPowerRoute

    @Serializable
    data class PanelList(
        val roomId: Long,
        val roomName: String,
        val thermalAreaName: String
    ) : ZPowerRoute

    @Serializable
    data class RelayList(
        val panelId: Long,
        val panelName: String,
        val roomName: String,
        val thermalAreaName: String
    ) : ZPowerRoute

    @Serializable
    data class ChildProcessList(
        val relayId: Long,
        val relayName: String,
        val panelName: String,
        val roomName: String,
        val thermalAreaName: String
    ) : ZPowerRoute

    @Serializable
    data class ChildProcessDetail(
        val childProcessId: Long,
        val childProcessName: String,
        val relayName: String,
        val panelName: String,
        val roomName: String,
        val thermalAreaName: String
    ) : ZPowerRoute

    @Serializable
    data class SubProcessList(
        val parentId: Long,
        val parentType: String,
        val parentName: String,
        val relayName: String,
        val panelName: String,
        val roomName: String,
        val thermalAreaName: String
    ) : ZPowerRoute

    @Serializable
    data class SubProcessDetail(
        val subProcessId: Long,
        val subProcessName: String,
        val parentName: String,
        val relayName: String,
        val panelName: String,
        val roomName: String,
        val thermalAreaName: String
    ) : ZPowerRoute

    @Serializable
    data object Settings : ZPowerRoute
}
