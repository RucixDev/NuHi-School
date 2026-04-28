package com.nuhi.nu_hi_school.data.model

import android.graphics.Color

data class Note(
    val id: Long,
    var title: String,
    var contentPath: String,
    val createdAt: Long,
    var updatedAt: Long = System.currentTimeMillis(),
    var backgroundColor: String = "#FFFFFF"
)

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
)

data class Stroke(
    val points: MutableList<StrokePoint> = mutableListOf(),
    val color: Int = Color.BLACK,
    val width: Float = 5f
)

data class DrawingCanvas(
    val strokes: MutableList<Stroke> = mutableListOf(),
    var currentStroke: Stroke? = null,
    var backgroundColor: Int = Color.WHITE
)

data class TrainingSample(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val language: String,
    val category: TrainingCategory,
    val strokesData: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TrainingCategory {
    ENGLISH,
    POLISH,
    MATH,
    SYMBOLS,
    BIOLOGY,
    PHYSICS,
    CHEMISTRY,
    HISTORY,
    GEOGRAPHY,
    OTHER
}

data class AISuggestion(
    val type: SuggestionType,
    val text: String,
    val confidence: Float,
    val position: Int
)

enum class SuggestionType {
    MATH_SOLUTION,
    TRANSLATION,
    SPELL_CHECK,
    GRAMMAR,
    FORMULA
}
