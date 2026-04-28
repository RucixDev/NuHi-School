package com.nuhi.nu_hi_school.ui.translation

import android.content.Intent
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import com.nuhi.nu_hi_school.ai.*
import com.nuhi.nu_hi_school.databinding.*
import kotlinx.coroutines.*

class TranslationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTranslationBinding
    private lateinit var aiManager: AIManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val languages = listOf(
        "English" to "en",
        "Polish" to "pl",
        "German" to "de",
        "French" to "fr",
        "Spanish" to "es",
        "Italian" to "it",
        "Portuguese" to "pt",
        "Russian" to "ru",
        "Chinese" to "zh",
        "Japanese" to "ja",
        "Korean" to "ko",
        "Arabic" to "ar"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranslationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        aiManager = AIManager(this)

        setupToolbar()
        setupLanguageSelectors()
        setupTranslation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Translation"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupLanguageSelectors() {
        val sourceNames = languages.map { it.first }
        val targetNames = languages.map { it.first }

        binding.spinnerSourceLang.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, sourceNames
        )
        binding.spinnerTargetLang.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, targetNames
        )

        binding.btnSwapLanguages.setOnClickListener {
            val sourcePos = binding.spinnerSourceLang.selectedItemPosition
            val targetPos = binding.spinnerTargetLang.selectedItemPosition
            binding.spinnerSourceLang.setSelection(targetPos)
            binding.spinnerTargetLang.setSelection(sourcePos)
        }
    }

    private fun setupTranslation() {
        binding.btnTranslate.setOnClickListener {
            translateText()
        }

        binding.btnDetectLanguage.setOnClickListener {
            detectLanguage()
        }

        binding.btnSpeakSource.setOnClickListener {
            speakText(binding.editSourceText.text.toString(), languages[binding.spinnerSourceLang.selectedItemPosition].second)
        }

        binding.btnSpeakTarget.setOnClickListener {
            speakText(binding.editTargetText.text.toString(), languages[binding.spinnerTargetLang.selectedItemPosition].second)
        }
    }

    private fun detectLanguage() {
        val text = binding.editSourceText.text.toString()
        if (text.isBlank()) {
            Toast.makeText(this, "Please enter text first", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressTranslate.visibility = View.VISIBLE
        binding.btnDetectLanguage.isEnabled = false

        scope.launch {
            val detectedLang = aiManager.detectLanguage(text)
            val langIndex = languages.indexOfFirst { it.second == detectedLang }
            if (langIndex >= 0) {
                binding.spinnerSourceLang.setSelection(langIndex)
                binding.detectedLangText.text = "Detected: ${languages[langIndex].first}"
            } else {
                binding.detectedLangText.text = "Detected: $detectedLang"
            }
            binding.progressTranslate.visibility = View.GONE
            binding.btnDetectLanguage.isEnabled = true
        }
    }

    private fun translateText() {
        val sourceText = binding.editSourceText.text.toString()
        if (sourceText.isBlank()) {
            Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show()
            return
        }

        val sourceLang = languages[binding.spinnerSourceLang.selectedItemPosition].second
        val targetLang = languages[binding.spinnerTargetLang.selectedItemPosition].second

        binding.progressTranslate.visibility = View.VISIBLE
        binding.btnTranslate.isEnabled = false

        scope.launch {
            val translated = aiManager.translateText(sourceText, sourceLang, targetLang)
            binding.editTargetText.setText(translated)
            binding.progressTranslate.visibility = View.GONE
            binding.btnTranslate.isEnabled = true
        }
    }

    private fun speakText(text: String, langCode: String) {
        if (text.isBlank()) return
        aiManager.speakText(text, langCode)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
