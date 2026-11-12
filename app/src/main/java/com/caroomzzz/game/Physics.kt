package com.caroomzzz.game

import kotlin.math.*

data class Disk(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val color: Int,
    val queen: Boolean = false
)

class CarromPhysics {
    val disks = mutableListOf<Disk>()
    var strikerX = .5f
    var strikerY = .84f
    var activePlayer = 0
    var scores = intArrayOf(0, 0)
    var moving = false
    private val radius = .022f
    private val pocketRadius = .052f

    init { reset() }

    fun reset() {
        disks.clear()
        scores = intArrayOf(0, 0)
        activePlayer = 0
        moving = false
        strikerX = .5f
        strikerY = .84f
        val cx = .5f
        val cy = .5f
        disks.add(Disk(cx, cy, color = 0xffd51d2d.toInt(), queen = true))
        for (i in 0 until 6) {
            val a = i * PI / 3
            disks.add(Disk(cx + cos(a).toFloat() * .052f, cy + sin(a).toFloat() * .052f,
                color = if (i % 2 == 0) 0xfff5e4cb.toInt() else 0xff292d32.toInt()))
        }
        for (i in 0 until 12) {
            val a = i * PI / 6
            disks.add(Disk(cx + cos(a).toFloat() * .105f, cy + sin(a).toFloat() * .105f,
                color = if (i % 2 == 0) 0xfff5e4cb.toInt() else 0xff292d32.toInt()))
        }
    }

    fun placeStriker(x: Float) {
        strikerX = x.coerceIn(.17f, .83f)
        strikerY = if (activePlayer == 0) .84f else .16f
    }

    fun shoot(angle: Float, power: Float) {
        if (moving) return
        val speed = .004f + power.coerceIn(0f, 1f) * .045f
        disks.add(Disk(strikerX, strikerY, cos(angle) * speed, sin(angle) * speed,
            0xfff8f8f8.toInt()))
        moving = true
    }

    fun step() {
        if (!moving) return
        val damping = .986f
        for (d in disks) {
            d.x += d.vx
            d.y += d.vy
            d.vx *= damping
            d.vy *= damping
            if (abs(d.vx) + abs(d.vy) < .00018f) {
                d.vx = 0f; d.vy = 0f
            }

            // Side rails. Corners are left open for pocket entry.
            if (d.x < .075f || d.x > .925f) {
                d.vx = -d.vx * .78f
                d.x = d.x.coerceIn(.055f, .945f)
            }
            if (d.y < .075f || d.y > .925f) {
                d.vy = -d.vy * .78f
                d.y = d.y.coerceIn(.055f, .945f)
            }
        }

        // Equal-mass disk collisions with positional correction.
        for (i in disks.indices) {
            for (j in i + 1 until disks.size) {
                val a = disks[i]
                val b = disks[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val distance = hypot(dx, dy)
                val minDistance = radius * 2f
                if (distance > .000001f && distance < minDistance) {
                    val nx = dx / distance
                    val ny = dy / distance
                    val overlap = minDistance - distance
                    a.x -= nx * overlap * .5f
                    a.y -= ny * overlap * .5f
                    b.x += nx * overlap * .5f
                    b.y += ny * overlap * .5f
                    val relative = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                    if (relative < 0f) {
                        val impulse = -(1.82f * relative) / 2f
                        a.vx -= impulse * nx
                        a.vy -= impulse * ny
                        b.vx += impulse * nx
                        b.vy += impulse * ny
                    }
                }
            }
        }

        // Pocket capture: disks close to a corner disappear smoothly.
        val before = disks.size
        disks.removeAll { d ->
            val corners = arrayOf(.075f to .075f, .925f to .075f,
                .075f to .925f, .925f to .925f)
            corners.any { (px, py) -> hypot(d.x - px, d.y - py) < pocketRadius }
        }
        if (disks.size < before) {
            val removed = before - disks.size
            scores[activePlayer] += removed
        }

        val stillMoving = disks.any { abs(it.vx) + abs(it.vy) > .00018f }
        if (!stillMoving) {
            disks.removeAll { it.color == 0xfff8f8f8.toInt() }
            moving = false
            activePlayer = 1 - activePlayer
            placeStriker(strikerX)
        }
    }

    fun encode(): String =
        disks.joinToString(";") { "${it.x},${it.y},${it.vx},${it.vy},${it.color},${it.queen}" } +
            "#" + activePlayer + "#" + scores.joinToString(",") + "#" + strikerX + "," + strikerY

    fun decode(snapshot: String) {
        val parts = snapshot.split("#")
        disks.clear()
        if (parts.isNotEmpty() && parts[0].isNotBlank()) {
            parts[0].split(";").forEach { entry ->
                runCatching {
                    val a = entry.split(",")
                    if (a.size >= 6) disks.add(Disk(
                        a[0].toFloat(), a[1].toFloat(), a[2].toFloat(), a[3].toFloat(),
                        a[4].toLong().toInt(), a[5].toBoolean()
                    ))
                }
            }
        }
        if (parts.size > 1) activePlayer = (parts[1].toIntOrNull() ?: 0).coerceIn(0, 1)
        if (parts.size > 2) {
            val values = parts[2].split(",")
            for (i in 0..1) scores[i] = values.getOrNull(i)?.toIntOrNull() ?: 0
        }
        if (parts.size > 3) {
            val striker = parts[3].split(",")
            strikerX = striker.getOrNull(0)?.toFloatOrNull()?.coerceIn(.17f, .83f) ?: strikerX
            strikerY = striker.getOrNull(1)?.toFloatOrNull()?.coerceIn(.12f, .88f) ?: strikerY
        }
        moving = disks.any { abs(it.vx) + abs(it.vy) > .00018f }
    }
}
