package com.example.insulincalculator.data

import java.util.Date

data class InsulinEntry(
    val timestamp: Date,
    val currentBG: Double,
    val carbs: Double,
    val correctionDose: Double,
    val targetBG: Double,
    val icr: Int,
    val finalInsulinDose: Double
) 