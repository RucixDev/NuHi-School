package com.nuhi.nu_hi_school.ui.training

import android.content.Intent
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import com.nuhi.nu_hi_school.ai.*
import com.nuhi.nu_hi_school.data.model.*
import com.nuhi.nu_hi_school.data.repository.*
import com.nuhi.nu_hi_school.databinding.*
import kotlinx.coroutines.*

class TrainingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrainingBinding
    private lateinit var trainingRepository: TrainingRepository
    private lateinit var canvasView: TrainingCanvasView
    private lateinit var aiManager: AIManager

    private var currentCategory = TrainingCategory.ENGLISH
    private var currentText = ""
    private var samplesWritten = 0
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val trainingTexts = mapOf(
        TrainingCategory.ENGLISH to listOf(
            "The quick brown fox jumps over the lazy dog.",
            "Hello, how are you today?",
            "Learning to write improves brain function.",
            "Practice makes perfect every single day.",
            "The sun rises in the east every morning."
        ),
        TrainingCategory.POLISH to listOf(
            "Ala ma kota, a kot ma ale.",
            "Dzien dobry, jak sie masz?",
            "Uczenie sie pisania poprawia pamiec.",
            "Cwiczenie czyni mistrza codziennie.",
            "Slonce wschodzi na wschodzie kazdego ranka."
        ),
        TrainingCategory.MATH to listOf(
            "2 + 2 = 4",
            "15 × 7 = 105",
            "√144 = 12",
            "3² + 4² = 5²",
            "(a + b)² = a² + 2ab + b²"
        ),
        TrainingCategory.SYMBOLS to listOf(
            "α β γ δ ε",
            "∑ ∏ ∫ √ ∞",
            "∈ ⊂ ⊃ ∪ ∩",
            "≤ ≥ ≠ ≈ ±",
            "∂ ∇ ÷ × ·"
        ),
        TrainingCategory.BIOLOGY to listOf(
            "Mitochondria is the powerhouse of the cell.",
            "Photosynthesis converts light energy to chemical energy.",
            "DNA stands for deoxyribonucleic acid.",
            "The human body has 206 bones.",
            "Blood cells are red and white corpuscles."
        ),
        TrainingCategory.PHYSICS to listOf(
            "E = mc²",
            "F = ma",
            "v = u + at",
            "PV = nRT",
            "λ = v/f"
        ),
        TrainingCategory.CHEMISTRY to listOf(
            "H₂O is water.",
            "NaCl is sodium chloride.",
            "CO₂ is carbon dioxide.",
            "The periodic table organizes elements.",
            "Chemical reactions involve bond breaking and forming."
        ),
        TrainingCategory.HISTORY to listOf(
            "World War II ended in 1945.",
            "The Roman Empire fell in 476 AD.",
            "The French Revolution began in 1789.",
            "The Renaissance started in Italy.",
            "Ancient Egypt built the pyramids."
        ),
        TrainingCategory.GEOGRAPHY to listOf(
            "Mount Everest is the highest mountain.",
            "The Nile is the longest river.",
            "Africa has 54 countries.",
            "The Pacific Ocean is the largest ocean.",
            "Europe has 44 countries."
        ),
        TrainingCategory.OTHER to listOf(
            "Practice writing any text here.",
            "Write what you see above.",
            "Use your own handwriting style.",
            "Write clearly and naturally.",
            "Take your time with each sample."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        trainingRepository = TrainingRepository(this)
        aiManager = AIManager(this)

        setupToolbar()
        setupCategorySelector()
        setupCanvas()
        setupTrainingControls()
        loadNextText()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "AI Training"
        binding.toolbar.setNavigationOnClickListener {
            saveCurrentSample()
            finish()
        }
    }

    private fun setupCategorySelector() {
        val categories = TrainingCategory.entries.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories.map { it.name })
        binding.categorySpinner.adapter = adapter

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = categories[position]
                loadNextText()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCanvas() {
        canvasView = binding.trainingCanvas
    }

    private fun setupTrainingControls() {
        binding.btnNextText.setOnClickListener {
            saveCurrentSample()
            loadNextText()
        }

        binding.btnClear.setOnClickListener {
            canvasView.clear()
        }

        binding.btnSaveSample.setOnClickListener {
            saveCurrentSample()
        }
    }

    private fun loadNextText() {
        val texts = trainingTexts[currentCategory] ?: trainingTexts[TrainingCategory.OTHER]!!
        currentText = texts.random()
        binding.trainingText.text = currentText
        canvasView.clear()
        updateProgress()
    }

    private fun saveCurrentSample() {
        val strokes = canvasView.getStrokesData()
        if (strokes.isEmpty()) return

        val sample = TrainingSample(
            text = currentText,
            language = currentCategory.name,
            category = currentCategory,
            strokesData = strokes
        )

        scope.launch {
            trainingRepository.addSample(sample)
            samplesWritten++
            updateProgress()
            Toast.makeText(this@TrainingActivity, "Sample saved! Keep going!", Toast.LENGTH_SHORT).show()
            canvasView.clear()
            loadNextText()
        }
    }

    private fun updateProgress() {
        binding.progressText.text = "Samples written: $samplesWritten"
    }

    override fun onBackPressed() {
        saveCurrentSample()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
