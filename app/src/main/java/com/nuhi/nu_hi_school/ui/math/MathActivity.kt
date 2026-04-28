package com.nuhi.nu_hi_school.ui.math

import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import com.nuhi.nu_hi_school.ai.*
import com.nuhi.nu_hi_school.databinding.*
import kotlinx.coroutines.*

class MathActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMathBinding
    private lateinit var aiManager: AIManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val mathCategories = listOf(
        "Basic Arithmetic",
        "Algebra",
        "Geometry",
        "Trigonometry",
        "Calculus",
        "Statistics",
        "Linear Equations",
        "Quadratic Equations",
        "Probability",
        "Matrices"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMathBinding.inflate(layoutInflater)
        setContentView(binding.root)

        aiManager = AIManager(this)

        setupToolbar()
        setupMathCategories()
        setupMathInput()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Math Helper"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupMathCategories() {
        binding.spinnerCategory.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, mathCategories
        )

        binding.btnSolve.setOnClickListener {
            solveMath()
        }

        binding.btnClear.setOnClickListener {
            binding.editMathInput.text?.clear()
            binding.textResult.text = ""
            binding.textSteps.text = ""
        }
    }

    private fun setupMathInput() {
        binding.btnPlus.setOnClickListener { insertSymbol("+") }
        binding.btnMinus.setOnClickListener { insertSymbol("-") }
        binding.btnMultiply.setOnClickListener { insertSymbol("×") }
        binding.btnDivide.setOnClickListener { insertSymbol("÷") }
        binding.btnPower.setOnClickListener { insertSymbol("^") }
        binding.btnRoot.setOnClickListener { insertSymbol("√") }
        binding.btnPi.setOnClickListener { insertSymbol("π") }
        binding.btnSin.setOnClickListener { insertSymbol("sin(") }
        binding.btnCos.setOnClickListener { insertSymbol("cos(") }
        binding.btnTan.setOnClickListener { insertSymbol("tan(") }
        binding.btnLog.setOnClickListener { insertSymbol("log(") }
        binding.btnLn.setOnClickListener { insertSymbol("ln(") }
        binding.btnOpenParen.setOnClickListener { insertSymbol("(") }
        binding.btnCloseParen.setOnClickListener { insertSymbol(")") }
        binding.btnX.setOnClickListener { insertSymbol("x") }
        binding.btnY.setOnClickListener { insertSymbol("y") }
        binding.btnIntegral.setOnClickListener { insertSymbol("∫") }
        binding.btnSum.setOnClickListener { insertSymbol("∑") }
        binding.btnInfinity.setOnClickListener { insertSymbol("∞") }
        binding.btnDegree.setOnClickListener { insertSymbol("°") }
    }

    private fun insertSymbol(symbol: String) {
        val start = binding.editMathInput.selectionStart
        val end = binding.editMathInput.selectionEnd
        binding.editMathInput.text?.replace(
            start.coerceAtLeast(0),
            end.coerceAtLeast(0),
            symbol
        )
    }

    private fun solveMath() {
        val input = binding.editMathInput.text.toString()
        if (input.isBlank()) {
            Toast.makeText(this, "Please enter an equation", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressMath.visibility = View.VISIBLE
        binding.btnSolve.isEnabled = false

        scope.launch {
            val result = aiManager.solveMath(input)
            binding.progressMath.visibility = View.GONE
            binding.btnSolve.isEnabled = true

            binding.textResult.text = result.result
            binding.textSteps.text = result.steps
            binding.cardSteps.visibility = if (result.steps.isNotBlank()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
