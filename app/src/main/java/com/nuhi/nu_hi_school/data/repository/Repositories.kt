package com.nuhi.nu_hi_school.data.repository

import android.content.Context
import android.graphics.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.nuhi.nu_hi_school.data.model.*
import kotlinx.coroutines.*
import java.io.*

class NoteRepository(private val context: Context) {
    private val notesFile: File
        get() = File(context.filesDir, "notes.json")

    private val canvasDir: File
        get() = File(context.filesDir, "canvases").also { it.mkdirs() }

    private val gson = Gson()

    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        if (!notesFile.exists()) return@withContext emptyList()
        try {
            val json = notesFile.readText()
            val type = object : TypeToken<List<Note>>() {}.type
            gson.fromJson<List<Note>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        val notes = getAllNotes().toMutableList()
        val index = notes.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            notes[index] = note
        } else {
            notes.add(0, note)
        }
        saveAllNotes(notes)
    }

    suspend fun deleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        val notes = getAllNotes().toMutableList()
        notes.removeAll { it.id == noteId }
        saveAllNotes(notes)
        File(canvasDir, "$noteId.canvas").delete()
    }

    private fun saveAllNotes(notes: List<Note>) {
        val json = gson.toJson(notes)
        notesFile.writeText(json)
    }

    fun saveCanvas(noteId: Long, canvas: DrawingCanvas): String {
        val file = File(canvasDir, "$noteId.canvas")
        val json = gson.toJson(canvas)
        file.writeText(json)
        return file.absolutePath
    }

    fun loadCanvas(noteId: Long): DrawingCanvas? {
        val file = File(canvasDir, "$noteId.canvas")
        if (!file.exists()) return null
        return try {
            val json = file.readText()
            gson.fromJson(json, DrawingCanvas::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

class TrainingRepository(private val context: Context) {
    private val trainingFile: File
        get() = File(context.filesDir, "training_samples.json")

    private val gson = Gson()

    suspend fun getAllSamples(): List<TrainingSample> = withContext(Dispatchers.IO) {
        if (!trainingFile.exists()) return@withContext emptyList()
        try {
            val json = trainingFile.readText()
            val type = object : TypeToken<List<TrainingSample>>() {}.type
            gson.fromJson<List<TrainingSample>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSample(sample: TrainingSample) = withContext(Dispatchers.IO) {
        val samples = getAllSamples().toMutableList()
        samples.add(0, sample)
        val json = gson.toJson(samples)
        trainingFile.writeText(json)
    }

    suspend fun getSamplesByCategory(category: TrainingCategory): List<TrainingSample> =
        withContext(Dispatchers.IO) {
            getAllSamples().filter { it.category == category }
        }

    suspend fun clearSamples() = withContext(Dispatchers.IO) {
        trainingFile.delete()
    }
}
