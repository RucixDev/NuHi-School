package com.nuhi.nu_hi_school.ui.canvas

import android.content.Context
import android.graphics.*
import android.util.*
import android.view.MotionEvent
import android.view.View
import com.nuhi.nu_hi_school.data.model.*
import kotlin.math.*

class CanvasView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val strokes = mutableListOf<Stroke>()
    private var currentStroke: Stroke? = null
    private var backgroundColor = Color.WHITE

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var penColor = Color.BLACK
    private var penWidth = 5f
    private var isEraser = false
    private val eraserRadius = 30f

    private var lastX = 0f
    private var lastY = 0f
    private var isDrawing = false

    private var currentSuggestion: String? = null

    private val undoStack = mutableListOf<List<Stroke>>()
    private val redoStack = mutableListOf<List<Stroke>>()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(backgroundColor)

        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            paint.color = stroke.color
            paint.strokeWidth = stroke.width

            val path = Path()
            path.moveTo(stroke.points[0].x, stroke.points[0].y)

            for (i in 1 until stroke.points.size) {
                val p0 = stroke.points[i - 1]
                val p1 = stroke.points[i]
                val midX = (p0.x + p1.x) / 2
                val midY = (p0.y + p1.y) / 2
                path.quadTo(p0.x, p0.y, midX, midY)
            }
            path.lineTo(stroke.points.last().x, stroke.points.last().y)
            canvas.drawPath(path, paint)
        }

        currentStroke?.let { stroke ->
            if (stroke.points.size >= 2) {
                paint.color = if (isEraser) backgroundColor else penColor
                paint.strokeWidth = if (isEraser) eraserRadius * 2 else stroke.width

                val path = Path()
                path.moveTo(stroke.points[0].x, stroke.points[0].y)

                for (i in 1 until stroke.points.size) {
                    val p0 = stroke.points[i - 1]
                    val p1 = stroke.points[i]
                    val midX = (p0.x + p1.x) / 2
                    val midY = (p0.y + p1.y) / 2
                    path.quadTo(p0.x, p0.y, midX, midY)
                }
                path.lineTo(stroke.points.last().x, stroke.points.last().y)
                canvas.drawPath(path, paint)
            }
        }

        currentSuggestion?.let {
            val textPaint = Paint().apply {
                color = Color.parseColor("#8000FF00")
                textSize = 48f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText("💡 $it", 50f, height - 100f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val pressure = if (event.pressure > 0) event.pressure else 1f

        val toolType = event.getToolType(0)
        val isStylusEraser = toolType == MotionEvent.TOOL_TYPE_ERASER

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDrawing = true
                lastX = x
                lastY = y

                if (isStylusEraser) {
                    eraseAt(x, y)
                } else {
                    currentStroke = Stroke(
                        points = mutableListOf(StrokePoint(x, y, pressure)),
                        color = penColor,
                        width = penWidth * pressure.coerceIn(0.5f, 2f)
                    )
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    if (isEraser || isStylusEraser) {
                        eraseAt(x, y)
                    } else {
                        currentStroke?.points?.add(StrokePoint(x, y, pressure))
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDrawing) {
                    if (!isEraser && !isStylusEraser) {
                        currentStroke?.let { strokes.add(it) }
                        currentStroke = null
                        saveToUndo()
                    }
                    isDrawing = false
                    isEraser = false
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun eraseAt(x: Float, y: Float) {
        val iterator = strokes.iterator()
        while (iterator.hasNext()) {
            val stroke = iterator.next()
            for (point in stroke.points) {
                val dist = sqrt((point.x - x) * (point.x - x) + (point.y - y) * (point.y - y))
                if (dist < eraserRadius) {
                    iterator.remove()
                    break
                }
            }
        }
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            redoStack.add(strokes.toList())
            strokes.removeLast()
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            strokes.addAll(redoStack.removeLast())
            invalidate()
        }
    }

    fun clear() {
        saveToUndo()
        strokes.clear()
        invalidate()
    }

    private fun saveToUndo() {
        undoStack.add(strokes.toList())
        redoStack.clear()
    }

    fun setPenStyle(color: Int, width: Float) {
        penColor = color
        penWidth = width
        isEraser = false
    }

    fun setEraserMode() {
        isEraser = true
    }

    fun setCanvasBackgroundColor(color: Int) {
        backgroundColor = color
        invalidate()
    }

    fun getCanvas(): DrawingCanvas {
        return DrawingCanvas(strokes.toMutableList(), null, backgroundColor)
    }

    fun loadCanvas(canvas: DrawingCanvas) {
        strokes.clear()
        strokes.addAll(canvas.strokes)
        backgroundColor = canvas.backgroundColor
        invalidate()
    }

    fun showSuggestion(suggestion: String) {
        currentSuggestion = suggestion
        invalidate()
    }

    fun clearSuggestion() {
        currentSuggestion = null
        invalidate()
    }

    fun recognizeText(): String {
        return ""
    }

    fun getStrokesForRecognition(): List<Stroke> {
        return strokes.toList()
    }
}
