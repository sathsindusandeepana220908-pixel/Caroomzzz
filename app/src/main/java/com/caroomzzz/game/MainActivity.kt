package com.caroomzzz.game

import android.app.*
import android.os.*
import android.graphics.Color
import android.view.*
import android.widget.*
import org.json.JSONObject
import kotlin.math.PI

class MainActivity:Activity() {
    private lateinit var root:LinearLayout
    private lateinit var physics:CarromPhysics
    private lateinit var board:CarromBoardView
    private lateinit var lan:LanManager
    private lateinit var status:TextView
    private lateinit var score:TextView
    private lateinit var power:SeekBar
    private lateinit var position:SeekBar
    private lateinit var aim:SeekBar
    private var mode="solo"
    private var isHost=true
    private var aiPending=false
    private val handler=Handler(Looper.getMainLooper())
    private val tick=object:Runnable {
        override fun run() {
            if(::physics.isInitialized && mode!="client") {
                physics.step()
                if(mode=="host") lan.send("STATE|"+physics.encode())
                if(mode=="solo" && !physics.moving && physics.activePlayer==1 && !aiPending) {
                    aiPending=true
                    handler.postDelayed({ if(mode=="solo" && !physics.moving && physics.activePlayer==1) physics.shoot((PI/2).toFloat(),.48f); aiPending=false },650)
                }
                if(::board.isInitialized) board.invalidate()
                if(::score.isInitialized) score.text="YOU  ${physics.scores[0]}       ${if(mode=="solo") "AI" else "PLAYER 2"}  ${physics.scores[1]}"
            }
            handler.postDelayed(this,16)
        }
    }
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor=Color.rgb(38,0,11); window.navigationBarColor=Color.rgb(38,0,11); showLoading() }
    private fun showLoading() {
        val box=LinearLayout(this).apply { orientation=1; gravity=Gravity.CENTER; setBackgroundColor(Color.rgb(83,5,29)) }
        val title=TextView(this).apply { text="CAROOMZZZ"; textSize=34f; setTextColor(0xffffd15a.toInt()); gravity=Gravity.CENTER; typeface=android.graphics.Typeface.DEFAULT_BOLD }
        val sub=TextView(this).apply { text="REAL CARROM • LOCAL MULTIPLAYER"; textSize=12f; setTextColor(Color.WHITE); gravity=Gravity.CENTER }
        box.addView(title); box.addView(sub); setContentView(box)
        handler.postDelayed({ showHome() },850)
    }
    private fun base():LinearLayout = LinearLayout(this).apply { orientation=1; setPadding(18,16,18,16); setBackgroundColor(Color.rgb(91,7,31)) }
    private fun text(value:String,size:Float=18f,color:Int=Color.WHITE)=TextView(this).apply { text=value; textSize=size; setTextColor(color); gravity=Gravity.CENTER; setPadding(8,8,8,8) }
    private fun button(value:String, action:()->Unit)=Button(this).apply { text=value; textSize=17f; setTextColor(Color.rgb(77,17,28)); setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffffc64b.toInt())); setOnClickListener { action() } }
    private fun showHome() {
        val b=base(); b.gravity=Gravity.CENTER
        b.addView(text("CAROOMZZZ",34f,0xffffd15a.toInt()))
        b.addView(text("CLASSIC CARROM • PLAY TOGETHER",12f))
        val spacer=Space(this); b.addView(spacer,LinearLayout.LayoutParams(1,34))
        b.addView(button("SINGLE PLAYER  •  VS AI") { startGame("solo") },LinearLayout.LayoutParams(-1,58).apply { bottomMargin=14 })
        b.addView(button("LAN MULTIPLAYER") { showLanMenu() },LinearLayout.LayoutParams(-1,58).apply { bottomMargin=14 })
        b.addView(text("Wi-Fi hotspot • Same local network • No internet needed",12f))
        b.addView(text("Caroomzzz 1.0",11f,0xffffd98a.toInt()))
        setContentView(b)
    }
    private fun showLanMenu() {
        AlertDialog.Builder(this).setTitle("LAN MULTIPLAYER")
            .setItems(arrayOf("HOST GAME (this phone hotspot)","JOIN GAME (connect to host Wi-Fi)","Back")) { _,which ->
                when(which) { 0 -> { isHost=true; startGame("host"); lan.host(); status.text="Hosting • Share hotspot and IP: connect the other phone" }
                    1 -> joinDialog(); else -> showHome() }
            }.show()
    }
    private fun joinDialog() {
        val input=EditText(this).apply { hint="Host IP (example: 192.168.43.1)"; setSingleLine(true) }
        val layout=LinearLayout(this).apply { orientation=1; setPadding(22,8,22,0); addView(input) }
        AlertDialog.Builder(this).setTitle("JOIN LOCAL GAME").setMessage("Connect this phone to the host's Wi-Fi hotspot first. Tap Find host or enter the host IP.")
            .setView(layout).setPositiveButton("JOIN") { _,_ -> startGame("client"); val ip=input.text.toString().trim(); if(ip.isNotEmpty()) lan.join(ip) else lan.discover { found -> runOnUiThread { lan.join(found) } } }
            .setNeutralButton("FIND HOST") { _,_ -> startGame("client"); lan.discover { found -> runOnUiThread { lan.join(found) } } }
            .setNegativeButton("CANCEL",null).show()
    }
    private fun startGame(which:String) {
        mode=which; physics=CarromPhysics(); lan=LanManager({ s -> runOnUiThread { if(::status.isInitialized) status.text=s; if(s=="Player connected" && mode=="host") lan.send("STATE|"+physics.encode()) } },{ msg -> runOnUiThread { receive(msg) } })
        val b=base(); b.setPadding(10,8,10,8)
        val top=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        top.addView(button("‹") { lan.close(); showHome() },LinearLayout.LayoutParams(54,48))
        top.addView(text("CAROOMZZZ",20f,0xffffd15a.toInt()),LinearLayout.LayoutParams(0,48,1f))
        top.addView(button("↻") { if(mode=="host"||mode=="solo") { physics.reset(); if(mode=="host") lan.send("STATE|"+physics.encode()) } },LinearLayout.LayoutParams(54,48))
        b.addView(top)
        score=text("YOU  0       PLAYER 2  0",14f); b.addView(score)
        status=text(if(which=="solo") "YOUR TURN" else if(which=="host") "Starting LAN host…" else "Connecting…",12f,0xffffdf9a.toInt()); b.addView(status)
        board=CarromBoardView(this,physics); b.addView(board,LinearLayout.LayoutParams(-1,0,1f))
        b.addView(text("STRIKER POSITION",11f,0xffffdf9a.toInt()))
        position=SeekBar(this).apply { max=100; progress=50; setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean) { if(user && !physics.moving && (mode!="client" || physics.activePlayer==1)) { physics.placeStriker(.16f+v/100f*.68f); board.invalidate() } }
            override fun onStartTrackingTouch(s:SeekBar?){}; override fun onStopTrackingTouch(s:SeekBar?){}
        }) }; b.addView(position)
        b.addView(text("SHOT POWER",11f,0xffffdf9a.toInt()))
        power=SeekBar(this).apply { max=100; progress=55 }; b.addView(power)
        b.addView(text("AIM DIRECTION",11f,0xffffdf9a.toInt()))
        aim=SeekBar(this).apply { max=180; progress=if(physics.activePlayer==0) 45 else 135 }; b.addView(aim)
        b.addView(button("STRIKE  ●") { takeShot() },LinearLayout.LayoutParams(-1,54).apply { topMargin=5 })
        setContentView(b)
        handler.removeCallbacks(tick); handler.post(tick)
    }
    private fun takeShot() {
        if(physics.moving) return
        if(mode=="client") {
            if(physics.activePlayer!=1) { status.text="Wait for your turn"; return }
            lan.send("SHOT|${(aim.progress-90)*PI/90.0}|${power.progress/100f}|${position.progress}")
        } else {
            if(mode=="host" && physics.activePlayer!=0) { status.text="Waiting for guest"; return }
            physics.placeStriker(.16f+position.progress/100f*.68f)
            physics.shoot(((aim.progress-90)*PI/90.0).toFloat(),power.progress/100f)
        }
        board.invalidate()
    }
    private fun receive(line:String) {
        when {
            line.startsWith("STATE|") && mode=="client" -> { physics.decode(line.substringAfter("|")); position.progress=((physics.strikerX-.16f)/.68f*100).toInt().coerceIn(0,100); board.invalidate(); score.text="YOU  ${physics.scores[1]}       PLAYER 1  ${physics.scores[0]}"; status.text=if(physics.activePlayer==1) "YOUR TURN" else "OPPONENT'S TURN" }
            line.startsWith("SHOT|") && mode=="host" -> {
                if(physics.activePlayer==1 && !physics.moving) { val a=line.split("|"); physics.placeStriker(.16f+(a.getOrNull(3)?.toIntOrNull()?:50)/100f*.68f); physics.shoot((a.getOrNull(1)?.toFloatOrNull()?:1f),a.getOrNull(2)?.toFloatOrNull()?:.5f) }
            }
        }
    }
    override fun onDestroy() { handler.removeCallbacks(tick); if(::lan.isInitialized) lan.close(); super.onDestroy() }
}
