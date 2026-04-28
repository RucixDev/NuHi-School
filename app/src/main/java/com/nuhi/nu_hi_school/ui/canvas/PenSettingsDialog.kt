package com.nuhi.nu_hi_school.ui.canvas

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import com.nuhi.nu_hi_school.R

class PenSettingsDialog(
    context: Context,
    private val onPenSettingsChanged: (color: Int, width: Float) -> Unit
) : Dialog(context) {

    private var selectedColor = Color.BLACK
    private var penWidth = 5f

    private val colors = listOf(
        Color.BLACK, Color.WHITE, Color.RED, Color.parseColor("#FF9800"),
        Color.YELLOW, Color.GREEN, Color.BLUE, Color.parseColor("#9C27B0"),
        Color.parseColor("#795548"), Color.parseColor("#607D8B")
    )

    private lateinit var colorContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_pen_settings)

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.white)

        colorContainer = findViewById(R.id.color_container)
        setupColorPicker()
        setupWidthSlider()
        setupApplyButton()
    }

    private fun setupColorPicker() {
        for (color in colors) {
            val colorView = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(color)
                setOnClickListener {
                    selectedColor = color
                    highlightSelectedColor(this)
                }
            }
            colorContainer.addView(colorView)
        }
    }

    private fun highlightSelectedColor(selected: View) {
        for (i in 0 until colorContainer.childCount) {
            colorContainer.getChildAt(i).setBackgroundResource(android.R.color.transparent)
        }
        selected.setBackgroundResource(R.drawable.color_selected_bg)
    }

    private fun setupWidthSlider() {
        val seekBar = findViewById<SeekBar>(R.id.width_seekbar)
        seekBar.progress = penWidth.toInt()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                penWidth = progress.toFloat().coerceAtLeast(1f)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun setupApplyButton() {
        findViewById<Button>(R.id.btn_apply).setOnClickListener {
            onPenSettingsChanged(selectedColor, penWidth)
            dismiss()
        }
    }
}
