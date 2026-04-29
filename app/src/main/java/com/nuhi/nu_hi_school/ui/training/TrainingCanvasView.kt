package com.nuhi.nu_hi_school.ui.training

import android.content.Context
import android.graphics.*
import android.util.*
import android.view.MotionEvent
import android.view.View
import com.google.gson.Gson
import com.nuhi.nu_hi_school.data.model.Stroke
import com.nuhi.nu_hi_school.data.model.StrokePoint
import kotlin.math.sqrt

class TrainingCanvasView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val strokes = mutableListOf<Stroke>()
    private var currentStroke: Stroke? = null
    private val strokePoints = mutableListOf<StrokePoint>()
    private val eraserRadius = 30f
    private var isEraser = false

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.BLACK
        strokeWidth = 5f
    }

    private var lastX = 0f
    private var lastY = 0f
    private val gson = Gson()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

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
                if (isStylusEraser) {
                    isEraser = true
                    eraseAt(x, y)
                } else {
                    isEraser = false
                    currentStroke = Stroke(
                        points = mutableListOf(StrokePoint(x, y, pressure)),
                        color = Color.BLACK,
                        width = 5f * pressure.coerceIn(0.5f, 2f)
                    )
                }
                lastX = x
                lastY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isEraser) {
                    eraseAt(x, y)
                } else {
                    currentStroke?.points?.add(StrokePoint(x, y, pressure))
                }
                lastX = x
                lastY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isEraser) {
                    currentStroke?.let {
                        strokes.add(it)
                        strokePoints.addAll(it.points)
                    }
                }
                currentStroke = null
                isEraser = false
                invalidate()
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
                val dist = kotlin.math.sqrt((point.x - x) * (point.x - x) + (point.y - y) * (point.y - y))
                if (dist < eraserRadius) {
                    iterator.remove()
                    break
                }
            }
        }
    }

    fun clear() {
        strokes.clear()
        currentStroke = null
        invalidate()
    }

    fun getStrokesData(): String {
        return gson.toJson(strokes)
    }
}
