package com.caroomzzz.game

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

class CarromBoardView(context: Context, private val game: CarromPhysics) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    var aimAngle = 0f
    var powerFraction = .58f

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = min(width, height).toFloat()
        if (side <= 0f) return
        val left = (width - side) / 2f
        val top = (height - side) / 2f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(side, side)

        // Deep red outer frame and layered gold rails.
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = 0xff3b0718.toInt()
        canvas.drawRoundRect(.005f, .005f, .995f, .995f, .055f, .055f, p)
        p.shader = LinearGradient(0f, 0f, 1f, 1f,
            intArrayOf(0xffffe18a.toInt(), 0xffa75b19.toInt(), 0xffffc84b.toInt(), 0xff71340e.toInt()),
            floatArrayOf(0f, .35f, .7f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(.018f, .018f, .982f, .982f, .045f, .045f, p)
        p.shader = null
        p.color = 0xff5d2a13.toInt()
        canvas.drawRoundRect(.035f, .035f, .965f, .965f, .032f, .032f, p)

        // Warm wooden playing surface with subtle grain.
        p.shader = LinearGradient(.05f, .05f, .92f, .95f,
            intArrayOf(0xffe5bd8c.toInt(), 0xffc9915d.toInt(), 0xffd9a875.toInt(), 0xffbd8250.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(.052f, .052f, .948f, .948f, .022f, .022f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = .0015f
        for (i in 0..16) {
            val y = .09f + i * .05f
            p.color = if (i % 2 == 0) 0x1b6c3d20 else 0x12fff0c0
            canvas.drawLine(.065f, y, .935f, y + .012f, p)
        }

        // Tournament-style baseline and center markings.
        p.color = 0x995d2b17.toInt()
        p.strokeWidth = .004f
        canvas.drawCircle(.5f, .5f, .175f, p)
        canvas.drawCircle(.5f, .5f, .145f, p)
        canvas.drawCircle(.5f, .5f, .115f, p)
        canvas.drawRect(.20f, .14f, .80f, .22f, p)
        canvas.drawRect(.20f, .78f, .80f, .86f, p)
        canvas.drawRect(.14f, .20f, .22f, .80f, p)
        canvas.drawRect(.78f, .20f, .86f, .80f, p)
        for ((x, y) in listOf(.25f to .18f, .75f to .18f, .25f to .82f, .75f to .82f)) {
            p.style = Paint.Style.FILL
            p.color = 0xffa85b34.toInt()
            canvas.drawCircle(x, y, .033f, p)
            p.color = 0xffe2b17b.toInt()
            canvas.drawCircle(x, y, .025f, p)
            p.style = Paint.Style.STROKE
            p.color = 0xff854323.toInt()
            p.strokeWidth = .004f
            canvas.drawCircle(x, y, .025f, p)
        }

        // Decorative center rosette.
        p.color = 0x886c351d.toInt()
        p.style = Paint.Style.STROKE
        p.strokeWidth = .003f
        for (i in 0 until 8) {
            val a = i * PI / 4
            val x = .5f + cos(a).toFloat() * .073f
            val y = .5f + sin(a).toFloat() * .073f
            canvas.drawCircle(x, y, .032f, p)
        }
        canvas.drawCircle(.5f, .5f, .018f, p)

        // Pockets with dark inset and warm metallic lip.
        p.style = Paint.Style.FILL
        for ((x, y) in listOf(.075f to .075f, .925f to .075f, .075f to .925f, .925f to .925f)) {
            p.color = 0xffe6a93d.toInt()
            canvas.drawCircle(x, y, .049f, p)
            p.shader = RadialGradient(x - .008f, y - .008f, .043f,
                intArrayOf(0xff252321.toInt(), 0xff050505.toInt(), 0xff000000.toInt()),
                null, Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, .043f, p)
            p.shader = null
        }

        // Disks: soft shadow, colored body, metallic rim and highlight.
        for (d in game.disks) drawDisk(canvas, d.x, d.y, d.color, d.queen)

        // Draw the striker preview on the correct baseline when the board is ready.
        if (!game.moving) {
            val sx = game.strikerX
            val sy = game.strikerY
            p.color = 0x55000000
            canvas.drawCircle(sx + .004f, sy + .006f, .031f, p)
            p.shader = RadialGradient(sx - .009f, sy - .012f, .04f,
                intArrayOf(0xffffffff.toInt(), 0xffd8f0f5.toInt(), 0xff78a9b8.toInt()),
                null, Shader.TileMode.CLAMP)
            canvas.drawCircle(sx, sy, .029f, p)
            p.shader = null
            p.style = Paint.Style.STROKE
            p.strokeWidth = .004f
            p.color = 0xffbd3541.toInt()
            canvas.drawCircle(sx, sy, .022f, p)
            p.color = 0xfffdf1d9.toInt()
            canvas.drawCircle(sx, sy, .014f, p)
            p.style = Paint.Style.FILL
            // Aim guide: points toward the board from the striker.
            val length = .12f + powerFraction.coerceIn(0f, 1f) * .18f
            val ex = sx + cos(aimAngle) * length
            val ey = sy + sin(aimAngle) * length
            p.color = 0x99fff0b0.toInt()
            p.strokeWidth = .004f
            canvas.drawLine(sx, sy, ex, ey, p)
            canvas.drawCircle(ex, ey, .008f, p)
        }
        canvas.restore()
    }

    private fun drawDisk(c: Canvas, x: Float, y: Float, color: Int, queen: Boolean) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = 0x55000000
        c.drawCircle(x + .004f, y + .006f, .024f, p)
        p.shader = RadialGradient(x - .008f, y - .009f, .034f,
            intArrayOf(Color.WHITE, color, darken(color)),
            floatArrayOf(0f, .58f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(x, y, .022f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = .003f
        p.color = if (color == 0xff292d32.toInt()) 0xffaeb6bb.toInt() else 0xfffff0d9.toInt()
        c.drawCircle(x, y, .018f, p)
        p.style = Paint.Style.FILL
        p.color = 0x66ffffff
        c.drawCircle(x - .006f, y - .007f, .004f, p)
        if (queen) {
            p.color = 0xffffe7a0.toInt()
            c.drawCircle(x, y, .008f, p)
            p.color = 0xffa93b27.toInt()
            c.drawCircle(x, y, .004f, p)
        }
    }

    private fun darken(color: Int): Int {
        val r = (Color.red(color) * .58f).toInt()
        val g = (Color.green(color) * .58f).toInt()
        val b = (Color.blue(color) * .58f).toInt()
        return Color.rgb(r, g, b)
    }
}
