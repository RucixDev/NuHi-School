package com.nuhi.nu_hi_school.ui.canvas

import android.app.Activity
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nuhi.nu_hi_school.ai.*
import com.nuhi.nu_hi_school.data.model.*
import com.nuhi.nu_hi_school.data.repository.*
import com.nuhi.nu_hi_school.databinding.*
import kotlinx.coroutines.*
import java.io.*

class CanvasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCanvasBinding
    private lateinit var canvasView: CanvasView
    private lateinit var noteRepository: NoteRepository
    private lateinit var aiManager: AIManager
    private var noteId: Long = -1
    private var note: Note? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isDarkMode = false

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCanvasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteId = intent.getLongExtra("note_id", -1)
        noteRepository = NoteRepository(this)
        aiManager = AIManager(this)

        setupToolbar()
        setupCanvas()
        setupToolbarActions()
        setupAISuggestions()
        loadNote()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notepad"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.setOnClickListener {
            showTitleEditDialog()
        }
    }

    private fun showTitleEditDialog() {
        val editText = EditText(this).apply {
            setText(note?.title ?: "Untitled")
            setSelection(text.length)
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Note Title")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = editText.text.toString().trim().ifBlank { "Untitled" }
                note?.title = newTitle
                supportActionBar?.title = newTitle
                scope.launch {
                    note?.let { noteRepository.saveNote(it) }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupCanvas() {
        canvasView = binding.canvasView
    }

    private fun setupToolbarActions() {
        binding.btnUndo.setOnClickListener { canvasView.undo() }
        binding.btnRedo.setOnClickListener { canvasView.redo() }
        binding.btnClear.setOnClickListener { canvasView.clear() }

        binding.btnPen.setOnClickListener {
            val dialog = PenSettingsDialog(this) { color, width ->
                canvasView.setPenStyle(color, width)
            }
            dialog.show()
        }

        binding.btnEraser.setOnClickListener {
            canvasView.setEraserMode()
        }

        binding.btnBackground.setOnClickListener {
            toggleBackground()
        }

        binding.btnAiSuggest.setOnClickListener {
            analyzeCanvas()
        }

        binding.btnSave.setOnClickListener {
            saveNote()
        }
    }

    private fun toggleBackground() {
        isDarkMode = !isDarkMode
        canvasView.setCanvasBackgroundColor(if (isDarkMode) Color.parseColor("#1A1A1A") else Color.WHITE)
        binding.btnBackground.text = if (isDarkMode) "☀️" else "🌙"
    }

    private fun setupAISuggestions() {
        binding.suggestionCard.visibility = View.GONE
    }

    private fun loadNote() {
        scope.launch {
            val notes = noteRepository.getAllNotes()
            note = notes.find { it.id == noteId }
            note?.let {
                binding.toolbar.title = it.title
                isDarkMode = it.backgroundColor == "#1A1A1A"
                canvasView.setCanvasBackgroundColor(
                    if (isDarkMode) Color.parseColor("#1A1A1A") else Color.WHITE
                )
                binding.btnBackground.text = if (isDarkMode) "☀️" else "🌙"

                val canvas = noteRepository.loadCanvas(noteId)
                canvas?.let { c ->
                    canvasView.loadCanvas(c)
                }
            }
        }
    }

    private fun saveNote() {
        scope.launch {
            val canvas = canvasView.getCanvas()
            val path = noteRepository.saveCanvas(noteId, canvas)
            note?.let {
                it.contentPath = path
                it.backgroundColor = if (isDarkMode) "#1A1A1A" else "#FFFFFF"
                it.updatedAt = System.currentTimeMillis()
                noteRepository.saveNote(it)
            }
            Toast.makeText(this@CanvasActivity, "Note saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeCanvas() {
        val strokes = canvasView.getCanvas()
        if (strokes.strokes.isEmpty()) {
            Toast.makeText(this, "Draw something first!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.suggestionCard.visibility = View.VISIBLE
        binding.suggestionText.text = "Analyzing..."
        binding.suggestionProgress.visibility = View.VISIBLE

        scope.launch {
            val recognizedText = recognizeDrawing()
            if (recognizedText.isNotBlank()) {
                val suggestion = aiManager.analyzeText(recognizedText)
                binding.suggestionProgress.visibility = View.GONE
                binding.suggestionText.text = suggestion
                canvasView.showSuggestion(suggestion)
            } else {
                binding.suggestionProgress.visibility = View.GONE
                binding.suggestionText.text = "Could not recognize text. Try writing more clearly."
            }
        }
    }

    private suspend fun recognizeDrawing(): String = withContext(Dispatchers.IO) {
        try {
            val bitmap = Bitmap.createBitmap(
                canvasView.width.coerceAtLeast(1),
                canvasView.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvasView.draw(canvas)

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val task = textRecognizer.process(inputImage)

            var result = ""
            task.addOnSuccessListener { visionText ->
                result = visionText.text
            }
            task.addOnFailureListener {
                result = ""
            }

            delay(3000)
            result
        } catch (e: Exception) {
            ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        textRecognizer.close()
    }

    override fun onBackPressed() {
        saveNote()
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }
}
