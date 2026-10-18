package com.faststrap.dev

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import rikka.shizuku.Shizuku

class Palette(
    val bg: Int, val c1: Int, val c2: Int, val c3: Int,
    val accent: Int, val seg: Int, val stroke: Int
)

private fun pal(bg: String, c1: String, c2: String, c3: String, accent: String, seg: String, stroke: String) =
    Palette(
        Color.parseColor(bg), Color.parseColor(c1), Color.parseColor(c2), Color.parseColor(c3),
        Color.parseColor(accent), Color.parseColor(seg), Color.parseColor(stroke)
    )

class MainActivity : AppCompatActivity() {

    private val flagsPath = "/data/local/tmp/ClientAppSettings.json"
    private val backupPath = "/data/local/tmp/faststrap_refresh_backup"
    private val fpsProp = "debug.graphics.game_default_frame_rate.disabled"
    private val robloxPkg = "com.roblox.client"

    private val palettes = listOf(
        pal("#FFFFFF", "#ECEFFE", "#E5F1F8", "#FBE9EE", "#111111", "#AAB6F5", "#B9C1F2"),
        pal("#F7FBF8", "#E3F4E8", "#E1F1F0", "#F3F1DD", "#1F3D2B", "#8CCB9B", "#A9D8B5"),
        pal("#FFFBF8", "#FFE9DC", "#FFF1D9", "#FBE3E8", "#4A2511", "#F4A97F", "#F2BFA0"),
        pal("#F7FAFD", "#DEEBFB", "#DDF3F6", "#E7E6FA", "#0F2A4A", "#7FB0EA", "#A5C6EE"),
        pal("#FFF9FB", "#FCE4EC", "#F5E3F6", "#FFE9E3", "#4A1230", "#F08FB3", "#F2B3CB"),
        pal("#F6F7F9", "#E8EAEE", "#EDEFF2", "#E3E7EC", "#1F2937", "#8E9AAF", "#B4BCC9")
    )
    private var themeIndex = 0

    private lateinit var prefs: SharedPreferences
    private lateinit var root: View
    private lateinit var statusText: TextView
    private lateinit var hintText: TextView
    private lateinit var countText: TextView
    private lateinit var outputText: TextView
    private lateinit var requestButton: MaterialButton
    private lateinit var applyButton: MaterialButton
    private lateinit var viewButton: MaterialButton
    private lateinit var removeButton: MaterialButton
    private lateinit var refreshButton: MaterialCardView
    private lateinit var statusCard: MaterialCardView
    private lateinit var heroCard: MaterialCardView
    private lateinit var perfCard: MaterialCardView
    private lateinit var gfxCard: MaterialCardView
    private lateinit var swatchRow: LinearLayout
    private lateinit var segRow: LinearLayout
    private lateinit var cbFps: CompoundButton
    private lateinit var cbFpsCounter: CompoundButton
    private lateinit var cbSky: CompoundButton
    private lateinit var cbGrass: CompoundButton
    private lateinit var cbShadows: CompoundButton
    private lateinit var cbTextures: CompoundButton
    private lateinit var switches: List<CompoundButton>
    private val segments = mutableListOf<MaterialCardView>()

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> updateStatus() }
    private val binderReceived = Shizuku.OnBinderReceivedListener { updateStatus() }
    private val binderDead = Shizuku.OnBinderDeadListener { updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("faststrap", MODE_PRIVATE)
        themeIndex = prefs.getInt("theme", 0).coerceIn(0, palettes.size - 1)

        root = findViewById(R.id.root)
        statusText = findViewById(R.id.statusText)
        hintText = findViewById(R.id.hintText)
        countText = findViewById(R.id.countText)
        outputText = findViewById(R.id.outputText)
        requestButton = findViewById(R.id.requestButton)
        applyButton = findViewById(R.id.applyButton)
        viewButton = findViewById(R.id.viewButton)
        removeButton = findViewById(R.id.removeButton)
        refreshButton = findViewById(R.id.refreshButton)
        statusCard = findViewById(R.id.statusCard)
        heroCard = findViewById(R.id.heroCard)
        perfCard = findViewById(R.id.perfCard)
        gfxCard = findViewById(R.id.gfxCard)
        swatchRow = findViewById(R.id.swatchRow)
        segRow = findViewById(R.id.segRow)
        cbFps = findViewById(R.id.cbFps)
        cbFpsCounter = findViewById(R.id.cbFpsCounter)
        cbSky = findViewById(R.id.cbSky)
        cbGrass = findViewById(R.id.cbGrass)
        cbShadows = findViewById(R.id.cbShadows)
        cbTextures = findViewById(R.id.cbTextures)
        switches = listOf(cbFps, cbFpsCounter, cbSky, cbGrass, cbShadows, cbTextures)

        buildSegments()

        cbFps.isChecked = prefs.getBoolean("fps", false)
        cbFpsCounter.isChecked = prefs.getBoolean("fpscounter", false)
        cbSky.isChecked = prefs.getBoolean("sky", false)
        cbGrass.isChecked = prefs.getBoolean("grass", false)
        cbShadows.isChecked = prefs.getBoolean("shadows", false)
        cbTextures.isChecked = prefs.getBoolean("textures", false)
        switches.forEach { it.setOnCheckedChangeListener { _, _ -> updateCount() } }

        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)

        requestButton.setOnClickListener { Shizuku.requestPermission(100) }
        applyButton.setOnClickListener { applyFlags() }
        viewButton.setOnClickListener { viewFlags() }
        removeButton.setOnClickListener { removeFlags() }
        refreshButton.setOnClickListener { updateStatus() }

        applyTheme()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun buildSegments() {
        val d = resources.displayMetrics.density
        for (i in 0 until 6) {
            val s = MaterialCardView(this)
            val lp = LinearLayout.LayoutParams(0, (30 * d).toInt(), 1f)
            if (i < 5) lp.marginEnd = (6 * d).toInt()
            s.layoutParams = lp
            s.radius = 10 * d
            s.cardElevation = 0f
            s.strokeWidth = (1 * d).toInt().coerceAtLeast(1)
            segRow.addView(s)
            segments.add(s)
        }
    }

    private fun buildSwatches() {
        swatchRow.removeAllViews()
        val d = resources.displayMetrics.density
        val size = (40 * d).toInt()
        palettes.forEachIndexed { i, p ->
            val s = MaterialCardView(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginEnd = (10 * d).toInt()
            s.layoutParams = lp
            s.radius = size / 2f
            s.cardElevation = 0f
            s.setCardBackgroundColor(p.seg)
            s.strokeWidth = if (i == themeIndex) (3 * d).toInt() else 0
            s.setStrokeColor(palettes[themeIndex].accent)
            s.setOnClickListener {
                themeIndex = i
                prefs.edit().putInt("theme", i).apply()
                applyTheme()
            }
            swatchRow.addView(s)
        }
    }

    private fun applyTheme() {
        val p = palettes[themeIndex]
        root.setBackgroundColor(p.bg)
        window.statusBarColor = p.bg
        window.navigationBarColor = p.bg
        heroCard.setCardBackgroundColor(p.c1)
        perfCard.setCardBackgroundColor(p.c2)
        gfxCard.setCardBackgroundColor(p.c3)
        refreshButton.setCardBackgroundColor(p.accent)
        statusCard.setCardBackgroundColor(p.accent)
        val tint = ColorStateList.valueOf(p.accent)
        requestButton.backgroundTintList = tint
        applyButton.backgroundTintList = tint
        removeButton.setStrokeColor(tint)
        removeButton.setTextColor(p.accent)
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val track = ColorStateList(states, intArrayOf(p.accent, Color.parseColor("#D1D5DB")))
        switches.forEach { (it as? MaterialSwitch)?.trackTintList = track }
        segments.forEach { it.setStrokeColor(p.stroke) }
        buildSwatches()
        updateCount()
    }

    private fun updateCount() {
        val p = palettes[themeIndex]
        val n = switches.count { it.isChecked }
        countText.text = n.toString()
        segments.forEachIndexed { i, s ->
            s.setCardBackgroundColor(if (i < n) p.seg else Color.TRANSPARENT)
        }
    }

    private fun hasShizuku(): Boolean =
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    private fun updateStatus() = runOnUiThread {
        val alive = Shizuku.pingBinder()
        val ready = hasShizuku()
        statusText.text = when {
            !alive -> "Shizuku not running"
            ready -> "Connected"
            else -> "Permission needed"
        }
        hintText.text = when {
            !alive -> "Open the Shizuku app and tap Start."
            !ready -> "Allow FastStrap to use Shizuku."
            else -> ""
        }
        hintText.visibility = if (ready) View.GONE else View.VISIBLE
        requestButton.visibility = if (alive && !ready) View.VISIBLE else View.GONE
        listOf(applyButton, viewButton, removeButton).forEach {
            it.isEnabled = ready
            it.alpha = if (ready) 1f else 0.4f
        }
    }

    private fun runShell(cmd: String): String {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(
            null, arrayOf("sh", "-c", cmd), null, null
        ) as Process
        val out = process.inputStream.bufferedReader().readText()
        val err = process.errorStream.bufferedReader().readText()
        process.waitFor()
        return out + err
    }

    private fun runAndShow(cmd: String) {
        outputText.text = "Running..."
        Thread {
            val result = try {
                runShell(cmd)
            } catch (e: Exception) {
                "Error: " + e.toString()
            }
            runOnUiThread { outputText.text = result }
        }.start()
    }

    private fun fpsOnScript(): String = "setprop $fpsProp true\n"

    private fun fpsOffScript(): String =
        "if [ -e $backupPath ]; then\n" +
            "set -- \$(cat $backupPath)\n" +
            "if [ \"\$1\" = null ]; then settings delete system peak_refresh_rate; " +
            "else settings put system peak_refresh_rate \"\$1\"; fi\n" +
            "if [ \"\$2\" = null ]; then settings delete system min_refresh_rate; " +
            "else settings put system min_refresh_rate \"\$2\"; fi\n" +
            "rm -f $backupPath\n" +
            "fi\n" +
            "setprop $fpsProp false\n"

    private fun buildJson(): String {
        val flags = mutableListOf<String>()
        if (cbFps.isChecked) {
            flags.add("\"DFIntTaskSchedulerTargetFps\": 9999")
            flags.add("\"FFlagEnableAndroidVsync\": false")
            flags.add("\"FFlagTaskSchedulerLimitTargetFpsTo2402\": false")
        }
        if (cbFpsCounter.isChecked) {
            flags.add("\"FFlagDebugDisplayFPS\": true")
        }
        if (cbSky.isChecked) {
            flags.add("\"FFlagDebugSkyGray\": true")
        }
        if (cbGrass.isChecked) {
            flags.add("\"FIntFRMMinGrassDistance\": 0")
            flags.add("\"FIntFRMMaxGrassDistance\": 0")
        }
        if (cbShadows.isChecked) {
            flags.add("\"DFFlagDebugPauseVoxelizer\": true")
        }
        if (cbTextures.isChecked) {
            flags.add("\"DFFlagTextureQualityOverrideEnabled\": true")
            flags.add("\"DFIntTextureQualityOverride\": 0")
        }
        return if (flags.isEmpty()) "{}"
        else "{\n  " + flags.joinToString(",\n  ") + "\n}"
    }

    private fun applyFlags() {
        prefs.edit()
            .putBoolean("fps", cbFps.isChecked)
            .putBoolean("fpscounter", cbFpsCounter.isChecked)
            .putBoolean("sky", cbSky.isChecked)
            .putBoolean("grass", cbGrass.isChecked)
            .putBoolean("shadows", cbShadows.isChecked)
            .putBoolean("textures", cbTextures.isChecked)
            .apply()

        val json = buildJson()
        val sb = StringBuilder()
        if (json == "{}") {
            sb.append("rm -f $flagsPath\n")
        } else {
            sb.append("cat > $flagsPath.new <<'FASTSTRAP_END'\n")
            sb.append(json).append("\n")
            sb.append("FASTSTRAP_END\n")
            sb.append("mv -f $flagsPath.new $flagsPath && chmod 0644 $flagsPath\n")
        }
        sb.append(if (cbFps.isChecked) fpsOnScript() else fpsOffScript())
        sb.append("am force-stop $robloxPkg\n")
        sb.append("echo Applied. Roblox was restarted.\n")
        sb.append("echo; ls -la $flagsPath 2>&1\n")
        sb.append("echo; cat $flagsPath 2>&1\n")
        sb.append("echo; echo fps_prop=\$(getprop $fpsProp)\n")
        runAndShow(sb.toString())
    }

    private fun viewFlags() {
        runAndShow(
            "echo '== flags file =='; " +
                "if [ -f $flagsPath ]; then cat $flagsPath; else echo '(no flags file)'; fi; " +
                "echo; echo '== fps =='; echo fps_prop=\$(getprop $fpsProp); " +
                "echo peak=\$(settings get system peak_refresh_rate) " +
                "min=\$(settings get system min_refresh_rate)"
        )
    }

    private fun removeFlags() {
        switches.forEach { it.isChecked = false }
        val keepTheme = themeIndex
        prefs.edit().clear().putInt("theme", keepTheme).apply()
        runAndShow(
            "rm -f $flagsPath\n" +
                fpsOffScript() +
                "am force-stop $robloxPkg\n" +
                "echo 'All flags removed. Roblox was restarted.'"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
    }
}