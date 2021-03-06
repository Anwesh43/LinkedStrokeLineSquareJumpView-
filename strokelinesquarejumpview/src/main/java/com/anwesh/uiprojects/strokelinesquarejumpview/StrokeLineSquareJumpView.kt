package com.anwesh.uiprojects.strokelinesquarejumpview

/**
 * Created by anweshmishra on 05/07/20.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.content.Context
import android.app.Activity

val colors : Array<String> = arrayOf("#3F51B5", "#F44336", "#009688", "#2196F3", "#4CAF50")
val lines : Int = 4
val scGap : Float = 0.02f / (lines + 1)
val sizeFactor : Float = 5.9f
val strokeFactor : Float = 90f
val backColor : Int = Color.parseColor("#BDBDBD")
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse()).toFloat()
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawStrokeLineSquareJump(scale : Float, w : Float, h : Float, paint : Paint) {
    val size : Float = Math.min(w, h) / sizeFactor
    val sf : Float = scale.sinify()
    val sf5 : Float = sf.divideScale(4, lines + 1)
    save()
    translate(w / 2, (h - size) * (1 - sf5))
    for (j in 0..(lines - 1)) {
        val sfi : Float = sf.divideScale(j, lines + 1)
        save()
        translate(0f, size / 2)
        rotate(90f * j)
        translate(-size / 2, -size / 2)
        drawLine(0f, 0f, size * sfi, 0f, paint)
        restore()
    }
    restore()
}

fun Canvas.drawSLSJNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = Color.parseColor(colors[i])
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    drawStrokeLineSquareJump(scale, w, h, paint)
}

class StrokeLineSquareJumpView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class SLSJNode(var i : Int, val state : State = State()) {

        private var next : SLSJNode? = null
        private var prev : SLSJNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = SLSJNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSLSJNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : SLSJNode {
            var curr : SLSJNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class StrokeLineSquareJump(var i : Int, val state : State = State()) {

        private var curr : SLSJNode = SLSJNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : StrokeLineSquareJumpView) {

        private val animator : Animator = Animator(view)
        private val slsj : StrokeLineSquareJump = StrokeLineSquareJump(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            slsj.draw(canvas, paint)
            animator.animate {
                slsj.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            slsj.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : StrokeLineSquareJumpView {
            val view : StrokeLineSquareJumpView = StrokeLineSquareJumpView(activity)
            activity.setContentView(view)
            return view
        }
    }
}