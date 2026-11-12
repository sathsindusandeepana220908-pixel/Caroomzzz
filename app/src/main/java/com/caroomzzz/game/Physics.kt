package com.caroomzzz.game

import kotlin.math.*

data class Disk(var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f, val color: Int, val queen: Boolean = false)
class CarromPhysics {
    val disks = mutableListOf<Disk>()
    var strikerX = 0.5f
    var strikerY = 0.84f
    var activePlayer = 0
    var scores = intArrayOf(0, 0)
    var moving = false
    private val radius = 0.022f
    init { reset() }
    fun reset() {
        disks.clear(); scores = intArrayOf(0,0); activePlayer = 0; moving = false
        val cx=.5f; val cy=.5f
        disks.add(Disk(cx,cy, color=0xffd51d2d.toInt(),queen=true))
        for (i in 0 until 6) {
            val a=i*PI/3
            disks.add(Disk(cx+cos(a).toFloat()*.052f,cy+sin(a).toFloat()*.052f,color=if(i%2==0) 0xfff5e4cb.toInt() else 0xff292d32.toInt()))
        }
        for (i in 0 until 12) {
            val a=i*PI/6
            disks.add(Disk(cx+cos(a).toFloat()*.105f,cy+sin(a).toFloat()*.105f,color=if(i%2==0) 0xfff5e4cb.toInt() else 0xff292d32.toInt()))
        }
    }
    fun placeStriker(x: Float) { strikerX=x.coerceIn(.16f,.84f); strikerY=if(activePlayer==0) .84f else .16f }
    fun shoot(angle: Float, power: Float) {
        if(moving) return
        val speed=(.008f + power.coerceIn(0f,1f)*.055f)
        val striker=Disk(strikerX,strikerY,cos(angle)*speed,sin(angle)*speed,0xfff8f8f8.toInt())
        disks.add(striker); moving=true
    }
    fun step() {
        if(!moving) return
        val all=disks.toList()
        for(d in all) {
            d.x+=d.vx; d.y+=d.vy
            d.vx*=.992f; d.vy*=.992f
            if(abs(d.vx)+abs(d.vy)<.00025f) { d.vx=0f; d.vy=0f }
            if(d.x<.075f || d.x>.925f) d.vx = -d.vx*.82f
            if(d.y<.075f || d.y>.925f) d.vy = -d.vy*.82f
            d.x=d.x.coerceIn(.075f,.925f); d.y=d.y.coerceIn(.075f,.925f)
        }
        for(i in disks.indices) for(j in i+1 until disks.size) {
            val a=disks[i]; val b=disks[j]; val dx=b.x-a.x; val dy=b.y-a.y
            val dist=hypot(dx,dy)
            if(dist>0f && dist<radius*2) {
                val nx=dx/dist; val ny=dy/dist
                val overlap=radius*2-dist
                a.x-=nx*overlap/2; a.y-=ny*overlap/2; b.x+=nx*overlap/2; b.y+=ny*overlap/2
                val rel=(b.vx-a.vx)*nx+(b.vy-a.vy)*ny
                if(rel<0) { val impulse=rel*.92f; a.vx+=impulse*nx; a.vy+=impulse*ny; b.vx-=impulse*nx; b.vy-=impulse*ny }
            }
        }
        val before=disks.size
        disks.removeAll { d -> (d.x<.075f && d.y<.075f)||(d.x>.925f && d.y<.075f)||(d.x<.075f && d.y>.925f)||(d.x>.925f && d.y>.925f) }
        if(disks.size<before) scores[activePlayer]++
        if(disks.none { abs(it.vx)+abs(it.vy)>.00025f }) {
            disks.removeAll { it.color==0xfff8f8f8.toInt() }
            moving=false; activePlayer=1-activePlayer; placeStriker(strikerX)
        }
    }
    fun encode(): String = disks.joinToString(";") { "${it.x},${it.y},${it.vx},${it.vy},${it.color},${it.queen}" } + "#" + activePlayer + "#" + scores.joinToString(",")
    fun decode(s: String) {
        val p=s.split("#"); disks.clear()
        if(p.isNotEmpty() && p[0].isNotBlank()) p[0].split(";").forEach { v -> runCatching { val a=v.split(","); disks.add(Disk(a[0].toFloat(),a[1].toFloat(),a[2].toFloat(),a[3].toFloat(),a[4].toLong().toInt(),a[5].toBoolean())) } }
        if(p.size>1) activePlayer=p[1].toIntOrNull()?:0
        if(p.size>2) p[2].split(",").take(2).forEachIndexed { i,v -> scores[i]=v.toIntOrNull()?:0 }
        moving=disks.any { abs(it.vx)+abs(it.vy)>.00025f }
    }
}
