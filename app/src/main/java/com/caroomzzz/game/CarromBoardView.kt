package com.caroomzzz.game

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

class CarromBoardView(context:Context, val game:CarromPhysics):View(context) {
    private val paint=Paint(3)
    private val wood=Paint(3)
    init { setLayerType(View.LAYER_TYPE_SOFTWARE,null) }
    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        val side=min(width,height).toFloat(); val left=(width-side)/2; val top=(height-side)/2
        c.save(); c.translate(left,top); c.scale(side,side)
        paint.color=0xff5c1028.toInt(); c.drawRoundRect(.015f,.015f,.985f,.985f,.04f,.04f,paint)
        paint.shader=LinearGradient(0f,0f,1f,1f, intArrayOf(0xffffd276.toInt(),0xff9b4d16.toInt(),0xffffd276.toInt()),null,Shader.TileMode.CLAMP)
        c.drawRoundRect(.025f,.025f,.975f,.975f,.035f,.035f,paint); paint.shader=null
        wood.shader=LinearGradient(.05f,.05f,.95f,.95f,0xffd8a879.toInt(),0xffa96d3e.toInt(),Shader.TileMode.CLAMP)
        c.drawRoundRect(.045f,.045f,.955f,.955f,.025f,.025f,wood); wood.shader=null
        paint.color=0xff6f351e.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=.004f
        c.drawRect(.15f,.15f,.85f,.85f,paint); c.drawCircle(.5f,.5f,.17f,paint); c.drawCircle(.5f,.5f,.145f,paint)
        c.drawRect(.25f,.14f,.75f,.22f,paint); c.drawRect(.25f,.78f,.75f,.86f,paint)
        c.drawRect(.14f,.25f,.22f,.75f,paint); c.drawRect(.78f,.25f,.86f,.75f,paint)
        paint.style=Paint.Style.FILL
        for((x,y) in listOf(.07f to .07f,.93f to .07f,.07f to .93f,.93f to .93f)) { paint.color=Color.BLACK; c.drawCircle(x,y,.038f,paint); paint.color=0xff30231d.toInt(); c.drawCircle(x,y,.043f,paint) }
        game.disks.forEach { d ->
            paint.color=0x55000000; c.drawCircle(d.x+.004f,d.y+.006f,.022f,paint)
            paint.color=d.color; c.drawCircle(d.x,d.y,.022f,paint)
            paint.style=Paint.Style.STROKE; paint.strokeWidth=.003f; paint.color=if(d.color==0xff292d32.toInt()) 0xff899096.toInt() else 0xfffff4df.toInt(); c.drawCircle(d.x,d.y,.017f,paint); paint.style=Paint.Style.FILL
            if(d.queen) { paint.color=0xffffe2a1.toInt(); c.drawCircle(d.x,d.y,.007f,paint) }
        }
        if(!game.moving) {
            paint.color=0xfff7f7f7.toInt(); c.drawCircle(game.strikerX,game.strikerY,.028f,paint)
            paint.color=0xffbf2836.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=.004f; c.drawCircle(game.strikerX,game.strikerY,.02f,paint); paint.style=Paint.Style.FILL
        }
        c.restore()
    }
}
