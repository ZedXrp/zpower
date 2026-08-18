package com.app.zpower.ui.theme

enum class BackgroundStyle(val id: String, val label: String) {
    LIQUID_GRADIENT("liquid_gradient", "Liquid Gradient"),
    BRUSHED_METAL("brushed_metal", "Brushed Metal"),
    DIGITAL_GRID("digital_grid", "Digital Grid"),
    LIQUID_GLASS("liquid_glass", "Liquid Glass");

    companion object {
        fun fromId(id: String): BackgroundStyle = entries.find { it.id == id } ?: LIQUID_GRADIENT
    }
}
