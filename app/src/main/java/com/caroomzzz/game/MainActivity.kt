package com.caroomzzz.game

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.PI

class MainActivity : Activity() {
    private lateinit var physics: CarromPhysics
    private lateinit var board: CarromBoardView
    private lateinit var lan: LanManager
    private lateinit var status: TextView
    private lateinit var score: TextView
    private lateinit var power: SeekBar
    private lateinit var position: SeekBar
    private lateinit var aim: SeekBar
    private var mode = "solo"
    private var aiPending = false
    private var syncFrame = 0
    private var lastSync = ""
    private var lastTurn = -1
    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            if (!::physics.isInitialized) return
            if (mode != "client") {
                physics.step()
                if (mode == "host") {
                    syncFrame++
                    val snapshot = physics.encode()
                    if (snapshot != lastSync && syncFrame % 4 == 0) {
                        lan.send("STATE|$snapshot")
                        lastSync = snapshot
                    }
                }
                if (mode == "solo" && !physics.moving && physics.activePlayer == 1 && !aiPending) {
                    aiPending = true
                    handler.postDelayed({
                        if (mode == "solo" && !physics.moving && physics.activePlayer == 1) {
                            physics.shoot((PI / 2).toFloat(), .48f)
                        }
                        aiPending = false
                    }, 650)
                }
            }
            updateTurnControls()
            if (::board.isInitialized) {
                board.aimAngle = ((aim.progress - 90) * PI / 90.0).toFloat()
                board.powerFraction = power.progress / 100f
                board.invalidate()
            }
            if (::score.isInitialized) {
                score.text = if (mode == "client")
                    "PLAYER 1  ${physics.scores[0]}       YOU  ${physics.scores[1]}"
                else
                    "YOU  ${physics.scores[0]}       ${if (mode == "solo") "AI" else "PLAYER 2"}  ${physics.scores[1]}"
            }
            handler.postDelayed(this, 16)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(38, 0, 11)
        window.navigationBarColor = Color.rgb(38, 0, 11)
        showLoading()
    }

    private fun showLoading() {
        val box = LinearLayout(this).apply {
            orientation = 1; gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(83, 5, 29))
        }
        box.addView(text("CAROOMZZZ", 36f, 0xffffd15a.toInt(), true))
        box.addView(text("REAL CARROM • LOCAL MULTIPLAYER", 12f))
        setContentView(box)
        handler.postDelayed({ showHome() }, 650)
    }

    private fun text(value: String, size: Float = 18f, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(7, 6, 7, 6)
        }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = 1
        setPadding(14, 10, 14, 10)
        setBackgroundColor(Color.rgb(75, 5, 27))
    }

    private fun button(label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label; textSize = 16f; gravity = Gravity.CENTER
            setTextColor(0xff4b1720.toInt()); typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xffffe17a.toInt(), 0xffffb52f.toInt())
            ).apply {
                cornerRadius = 18f
                setStroke(2, 0xffffe8a4.toInt())
            }
            elevation = 5f
            setPadding(12, 12, 12, 12)
            setOnClickListener { action() }
        }

    private fun showHome() {
        val b = panel().apply { gravity = Gravity.CENTER }
        b.addView(text("CAROOMZZZ", 36f, 0xffffd15a.toInt(), true))
        b.addView(text("CLASSIC CARROM • PLAY TOGETHER", 12f, 0xffffe6c0.toInt()))
        b.addView(Space(this), LinearLayout.LayoutParams(1, 34))
        b.addView(button("SINGLE PLAYER  •  VS AI") { startGame("solo") },
            LinearLayout.LayoutParams(-1, 62).apply { bottomMargin = 16 })
        b.addView(button("LAN MULTIPLAYER") { showLanMenu() },
            LinearLayout.LayoutParams(-1, 62).apply { bottomMargin = 16 })
        b.addView(text("Wi-Fi hotspot • Same local network • No internet", 12f))
        b.addView(text("Caroomzzz", 11f, 0xffffd98a.toInt()))
        setContentView(b)
    }

    private fun showLanMenu() {
        AlertDialog.Builder(this).setTitle("LAN MULTIPLAYER")
            .setItems(arrayOf("HOST GAME  •  Create room", "JOIN GAME  •  Enter host IP", "Back")) { _, which ->
                when (which) {
                    0 -> { startGame("host"); lan.host() }
                    1 -> joinDialog()
                    else -> showHome()
                }
            }.show()
    }

    private fun joinDialog() {
        val input = EditText(this).apply {
            hint = "Host IP (example: 192.168.43.1)"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        val layout = LinearLayout(this).apply {
            orientation = 1; setPadding(22, 8, 22, 0); addView(input)
        }
        AlertDialog.Builder(this).setTitle("JOIN LOCAL GAME")
            .setMessage("Connect this phone to the host's Wi-Fi hotspot first. Enter the host IP shown on the host phone.")
            .setView(layout)
            .setPositiveButton("JOIN") { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isNotEmpty()) {
                    startGame("client")
                    lan.join(ip)
                } else Toast.makeText(this, "Enter the host IP address", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("FIND HOST") { _, _ ->
                startGame("client")
                lan.discover { found -> runOnUiThread { lan.join(found) } }
            }
            .setNegativeButton("CANCEL", null).show()
    }

    private fun startGame(which: String) {
        if (::lan.isInitialized) lan.close()
        mode = which
        physics = CarromPhysics()
        lastSync = ""; syncFrame = 0; lastTurn = -1; aiPending = false
        lan = LanManager(
            { message -> runOnUiThread {
                if (::status.isInitialized) status.text = message
                if (message == "Player connected" && mode == "host") {
                    lan.send("STATE|${physics.encode()}")
                    lastSync = physics.encode()
                }
            } },
            { message -> runOnUiThread { receive(message) } }
        )

        val b = panel().apply { setPadding(8, 6, 8, 8) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(button("‹") { lan.close(); handler.removeCallbacks(tick); showHome() },
            LinearLayout.LayoutParams(52, 46))
        top.addView(text("CAROOMZZZ", 20f, 0xffffd15a.toInt(), true),
            LinearLayout.LayoutParams(0, 46, 1f))
        top.addView(button("↻") {
            if (mode == "host" || mode == "solo") {
                physics.reset()
                lastTurn = -1
                if (mode == "host") lan.send("STATE|${physics.encode()}")
            }
        }, LinearLayout.LayoutParams(52, 46))
        b.addView(top)

        score = text("YOU  0       PLAYER 2  0", 14f, 0xfffff0d8.toInt(), true)
        b.addView(score)
        status = text(
            when (which) {
                "solo" -> "YOUR TURN"
                "host" -> "HOSTING • Waiting for player"
                else -> "CONNECTING TO HOST…"
            }, 12f, 0xffffdf9a.toInt()
        )
        b.addView(status)

        board = CarromBoardView(this, physics)
        b.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))

        b.addView(text("STRIKER POSITION  •  MOVE LEFT / RIGHT", 11f, 0xffffdf9a.toInt()))
        position = SeekBar(this).apply {
            max = 100; progress = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar?, value: Int, fromUser: Boolean) {
                    if (!fromUser || physics.moving || !isMyTurn()) return
                    physics.placeStriker(.16f + value / 100f * .68f)
                    board.invalidate()
                }
                override fun onStartTrackingTouch(seek: SeekBar?) {}
                override fun onStopTrackingTouch(seek: SeekBar?) {
                    if (mode == "client" && isMyTurn() && !physics.moving)
                        lan.send("POS|${physics.strikerX}")
                }
            })
        }
        b.addView(position)

        b.addView(text("SHOT POWER", 11f, 0xffffdf9a.toInt()))
        power = SeekBar(this).apply { max = 100; progress = 58 }
        b.addView(power)

        b.addView(text("AIM DIRECTION  •  DRAG TO AIM", 11f, 0xffffdf9a.toInt()))
        aim = SeekBar(this).apply { max = 180; progress = 90 }
        b.addView(aim)

        b.addView(button("STRIKE  ●") { takeShot() },
            LinearLayout.LayoutParams(-1, 54).apply { topMargin = 4 })
        setContentView(b)
        updateTurnControls()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun isMyTurn(): Boolean =
        !physics.moving && when (mode) {
            "client" -> physics.activePlayer == 1
            "host" -> physics.activePlayer == 0
            else -> physics.activePlayer == 0
        }

    private fun updateTurnControls() {
        if (!::position.isInitialized || !::power.isInitialized || !::aim.isInitialized) return
        val allowed = isMyTurn()
        position.isEnabled = allowed
        power.isEnabled = allowed
        aim.isEnabled = allowed
        if (::status.isInitialized && mode != "solo") {
            if (mode == "client" && !allowed && !physics.moving) status.text = "OPPONENT'S TURN"
            else if (mode == "client" && allowed) status.text = "YOUR TURN • Move striker and shoot"
            else if (mode == "host" && allowed) status.text =
                if (status.text.toString().startsWith("Player connected")) "YOUR TURN" else status.text
            else if (mode == "host" && physics.activePlayer == 1 && !physics.moving) status.text = "GUEST'S TURN"
        } else if (mode == "solo" && allowed) status.text = "YOUR TURN"
    }

    private fun takeShot() {
        if (!isMyTurn()) {
            status.text = if (physics.moving) "WAIT • Disks are still moving" else "WAIT FOR YOUR TURN"
            return
        }
        val angle = ((aim.progress - 90) * PI / 90.0).toFloat()
        val strength = power.progress / 100f
        if (mode == "client") {
            lan.send("SHOT|$angle|$strength|${position.progress}")
            status.text = "SHOT SENT • Waiting for host"
        } else {
            physics.placeStriker(.16f + position.progress / 100f * .68f)
            physics.shoot(angle, strength)
            if (mode == "host") lan.send("STATE|${physics.encode()}")
        }
        board.invalidate()
    }

    private fun receive(line: String) {
        when {
            line.startsWith("STATE|") && mode == "client" -> {
                physics.decode(line.substringAfter("|"))
                position.progress = ((physics.strikerX - .16f) / .68f * 100).toInt().coerceIn(0, 100)
                board.invalidate()
                status.text = when {
                    physics.moving -> "OPPONENT SHOT • Disks moving"
                    physics.activePlayer == 1 -> "YOUR TURN • Move striker and shoot"
                    else -> "OPPONENT'S TURN"
                }
            }
            line.startsWith("POS|") && mode == "host" -> {
                if (physics.activePlayer == 1 && !physics.moving) {
                    val x = line.substringAfter("|").toFloatOrNull()
                    if (x != null) {
                        physics.placeStriker(x)
                        lan.send("STATE|${physics.encode()}")
                        lastSync = physics.encode()
                    }
                }
            }
            line.startsWith("SHOT|") && mode == "host" -> {
                if (physics.activePlayer == 1 && !physics.moving) {
                    val parts = line.split("|")
                    val angle = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                    val strength = parts.getOrNull(2)?.toFloatOrNull() ?: .5f
                    val x = parts.getOrNull(3)?.toIntOrNull()?.coerceIn(0, 100) ?: 50
                    physics.placeStriker(.16f + x / 100f * .68f)
                    physics.shoot(angle, strength)
                    lan.send("STATE|${physics.encode()}")
                    lastSync = physics.encode()
                    status.text = "GUEST SHOT • Disks moving"
                } else {
                    lan.send("STATE|${physics.encode()}")
                }
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        if (::lan.isInitialized) lan.close()
        super.onDestroy()
    }
}
