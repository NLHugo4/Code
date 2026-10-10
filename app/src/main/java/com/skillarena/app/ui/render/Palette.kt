package com.skillarena.app.ui.render

import androidx.compose.ui.graphics.Color

object Palette {
    val Blue = Color(0xFF2F7BFF)
    val BlueDark = Color(0xFF1A47A3)
    val Red = Color(0xFFE8413C)
    val RedDark = Color(0xFF9C1F1B)

    val Outline = Color(0xFF1B1B1F)
    val Skin = Color(0xFFF2C29B)
    val Steel = Color(0xFFD3DAE2)
    val SteelDark = Color(0xFF7D8A96)
    val Wood = Color(0xFF9A6332)
    val WoodDark = Color(0xFF5E3B1A)
    val Gold = Color(0xFFFFC83D)
    val Stone = Color(0xFFB3B5B8)
    val StoneDark = Color(0xFF75777B)
    val Bone = Color(0xFFF1EFE6)
    val GoblinGreen = Color(0xFF7CC242)
    val DrakeGreen = Color(0xFF3FA34D)
    val Fire = Color(0xFFFF8F1F)
    val FireCore = Color(0xFFFFE066)
    val Elixir = Color(0xFFD63AF9)
    val ElixirDark = Color(0xFF4A1572)

    val Grass = Color(0xFF5DAE3E)
    val GrassAlt = Color(0xFF57A539)
    val GrassEdge = Color(0xFF3F7F2A)
    val Path = Color(0xFFD9BC83)
    val PathEdge = Color(0xFFBF9F63)
    val Water = Color(0xFF2E8BD6)
    val WaterLight = Color(0xFF7CC4F5)

    fun team(owner: Int) = if (owner == 0) Blue else Red
    fun teamDark(owner: Int) = if (owner == 0) BlueDark else RedDark
}
