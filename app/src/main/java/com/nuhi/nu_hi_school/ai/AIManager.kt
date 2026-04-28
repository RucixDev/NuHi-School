package com.nuhi.nu_hi_school.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.*
import java.util.*

data class MathResult(
    val result: String,
    val steps: String
)

class AIManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        initTTS()
    }

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.language = Locale.US
            }
        }
    }

    fun analyzeText(text: String): String {
        return when {
            isMathExpression(text) -> provideMathSuggestion(text)
            isLanguageRelated(text) -> "Translation available"
            else -> "Text recognized. AI can help with math, translation, and more!"
        }
    }

    private fun isMathExpression(text: String): Boolean {
        val mathPatterns = listOf(
            Regex("[+\\-×÷*/^√∫∑]"),
            Regex("\\d+\\s*[=<>>]\\s*\\d+"),
            Regex("[a-zA-Z]\\s*\\("),
            Regex("\\d+\\s+\\d+")
        )
        return mathPatterns.any { it.containsMatchIn(text) }
    }

    private fun isLanguageRelated(text: String): Boolean {
        return text.contains(Regex("[a-zA-Z]{3,}"))
    }

    private fun provideMathSuggestion(expression: String): String {
        return try {
            val result = evaluateMathExpression(expression)
            "💡 Result: $result"
        } catch (e: Exception) {
            "📐 Math detected. Use Math Helper for step-by-step solutions!"
        }
    }

    private fun evaluateMathExpression(expr: String): String {
        var expression = expr.replace("×", "*").replace("÷", "/").replace("^", "**")

        if (expression.contains(Regex("\\d+\\s*[+\\-*/]\\s*\\d+"))) {
            val result = evaluateSimpleMath(expression)
            return result
        }

        if (expression.contains("=")) {
            return solveSimpleEquation(expression)
        }

        return "Use Math Helper for detailed solution"
    }

    private fun evaluateSimpleMath(expr: String): String {
        return try {
            val clean = expr.replace(" ", "")
            if (clean.contains("+")) {
                val parts = clean.split("+")
                val sum = parts.sumOf { it.toDoubleOrNull() ?: 0.0 }
                return sum.toString()
            }
            if (clean.contains("-") && !clean.startsWith("-")) {
                val parts = clean.split("-")
                val diff = parts[0].toDouble() - parts.drop(1).sumOf { it.toDoubleOrNull() ?: 0.0 }
                return diff.toString()
            }
            if (clean.contains("*")) {
                val parts = clean.split("*")
                val product = parts.fold(1.0) { acc, s -> acc * (s.toDoubleOrNull() ?: 1.0) }
                return product.toString()
            }
            expr
        } catch (e: Exception) {
            "Error evaluating"
        }
    }

    private fun solveSimpleEquation(equation: String): String {
        try {
            if (equation.contains("x") && equation.contains("=")) {
                val parts = equation.split("=")
                val rhs = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
                val lhs = parts[0]

                if (lhs.contains("x") && !lhs.contains("x²")) {
                    val coef = lhs.replace("x", "").replace(" ", "").toDoubleOrNull() ?: 1.0
                    val solution = rhs / coef
                    return "x = $solution"
                }
            }
        } catch (e: Exception) {
            return "Complex equation - use Math Helper"
        }
        return "Use Math Helper for detailed solution"
    }

    suspend fun translateText(text: String, sourceLang: String, targetLang: String): String =
        withContext(Dispatchers.IO) {
            // Simple translation placeholder - in production would use ML Kit Translate
            "Translation of '$text' from $sourceLang to $targetLang"
        }

    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.IO) {
        when {
            text.any { it in 'а'..'я' || it in 'А'..'Я' } -> "ru"
            text.any { it in "ęóąśłżźćń" } -> "pl"
            text.any { it in "äöüß" } -> "de"
            text.any { it in "éèàù" } -> "fr"
            text.any { it in "ñáéíóú" } -> "es"
            else -> "en"
        }
    }

    suspend fun solveMath(input: String): MathResult = withContext(Dispatchers.Default) {
        try {
            val cleanInput = input.replace(" ", "")
                .replace("×", "*").replace("÷", "/")
                .replace("^", "**").replace("√", "sqrt")
                .replace("π", "PI")

            when {
                cleanInput.contains("=") && cleanInput.contains("x²") -> solveQuadratic(cleanInput)
                cleanInput.contains("=") && cleanInput.contains("x") -> solveLinear(cleanInput)
                cleanInput.contains("sin(") || cleanInput.contains("cos(") || cleanInput.contains("tan(") -> solveTrig(cleanInput)
                cleanInput.contains("∫") -> solveIntegral(cleanInput)
                cleanInput.contains("log(") -> solveLog(cleanInput)
                else -> evaluateBasic(cleanInput)
            }
        } catch (e: Exception) {
            MathResult("Error: ${e.message}", "Could not solve this problem")
        }
    }

    private fun solveLinear(eq: String): MathResult {
        try {
            val parts = eq.split("=")
            val rhs = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val lhs = parts[0]

            val xMatch = Regex("([\\d.]*)x").find(lhs)
            val coef = xMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 1.0
            val constant = lhs.replace(Regex("[^-\\d]"), "").toDoubleOrNull() ?: 0.0

            val solution = (rhs - constant) / coef
            return MathResult(
                "x = $solution",
                "Step 1: Isolate x term\nStep 2: Divide both sides by $coef\nStep 3: x = ($rhs - $constant) / $coef = $solution"
            )
        } catch (e: Exception) {
            return MathResult("Could not solve", "Check equation format")
        }
    }

    private fun solveQuadratic(eq: String): MathResult {
        return MathResult(
            "x = (-b ± √(b²-4ac)) / 2a",
            "Use quadratic formula:\n1. Identify a, b, c from ax² + bx + c = 0\n2. Calculate discriminant: b² - 4ac\n3. Apply formula: x = (-b ± √(b²-4ac)) / 2a"
        )
    }

    private fun solveTrig(expr: String): MathResult {
        return when {
            expr.contains("sin(30)") -> MathResult("sin(30°) = 0.5", "Unit circle reference")
            expr.contains("cos(0)") -> MathResult("cos(0°) = 1", "Unit circle reference")
            expr.contains("tan(45)") -> MathResult("tan(45°) = 1", "Unit circle reference")
            else -> MathResult("Trigonometric function", "Use calculator for specific values")
        }
    }

    private fun solveIntegral(expr: String): MathResult {
        return MathResult(
            "∫ f(x) dx",
            "Integration steps:\n1. Identify the integrand\n2. Apply integration rules\n3. Add constant of integration C"
        )
    }

    private fun solveLog(expr: String): MathResult {
        return when {
            expr.contains("log(10") -> MathResult("log₁₀(10) = 1", "log base 10")
            expr.contains("log(100") -> MathResult("log₁₀(100) = 2", "log base 10")
            else -> MathResult("Logarithm", "logₐ(b) = c means a^c = b")
        }
    }

    private fun evaluateBasic(expr: String): MathResult {
        return try {
            val result = evaluateSimpleMath(expr)
            MathResult("= $result", "Evaluated: $expr")
        } catch (e: Exception) {
            MathResult("Cannot evaluate", "Try a different format")
        }
    }

    fun speakText(text: String, langCode: String) {
        if (!ttsReady || text.isBlank()) return
        tts?.language = when (langCode) {
            "en" -> Locale.US
            "pl" -> Locale("pl", "PL")
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            "es" -> Locale("es", "ES")
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt", "PT")
            "ru" -> Locale("ru", "RU")
            "zh" -> Locale.CHINESE
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            else -> Locale.US
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.shutdown()
        scope.cancel()
    }
}
