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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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

    /** One UI toggle plus the FFlags it controls. Add a new tweak by adding one entry in onCreate. */
    private class Tweak(
        val prefKey: String,
        val toggle: CompoundButton,
        val flags: Map<String, Any>
    )

    private val flagsPath = "/data/local/tmp/ClientAppSettings.json"
    private val backupPath = "/data/local/tmp/faststrap_refresh_backup"
    private val fpsProp = "debug.graphics.game_default_frame_rate.disabled"
    private val robloxPkg = "com.roblox.client"
    private val shellTimeoutSeconds = 20L

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
    private lateinit var tweaks: List<Tweak>
    private val segments = mutableListOf<MaterialCardView>()

    // Commands run one at a time off the main thread; the watchdog kills hung processes.
    private val executor = Executors.newSingleThreadExecutor()
    private val watchdog = Executors.newSingleThreadScheduledExecutor()
    private var busy = false

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

        tweaks = listOf(
            Tweak(
                "fps", cbFps, linkedMapOf<String, Any>(
                    "DFIntTaskSchedulerTargetFps" to 9999,
                    "FFlagEnableAndroidVsync" to false,
                    "FFlagTaskSchedulerLimitTargetFpsTo2402" to false
                )
            ),
            Tweak("fpscounter", cbFpsCounter, linkedMapOf<String, Any>("FFlagDebugDisplayFPS" to true)),
            Tweak("sky", cbSky, linkedMapOf<String, Any>("FFlagDebugSkyGray" to true)),
            Tweak(
                "grass", cbGrass, linkedMapOf<String, Any>(
                    "FIntFRMMinGrassDistance" to 0,
                    "FIntFRMMaxGrassDistance" to 0
                )
            ),
            Tweak("shadows", cbShadows, linkedMapOf<String, Any>("DFFlagDebugPauseVoxelizer" to true)),
            Tweak(
                "textures", cbTextures, linkedMapOf<String, Any>(
                    "DFFlagTextureQualityOverrideEnabled" to true,
                    "DFIntTextureQualityOverride" to 0
                )
            )
        )
        switches = tweaks.map { it.toggle }

        buildSegments()

        tweaks.forEach { it.toggle.isChecked = prefs.getBoolean(it.prefKey, false) }
        switches.forEach { it.setOnCheckedChangeListener { _, _ -> updateCount() } }

        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)

        requestButton.setOnClickListener { Shizuku.requestPermission(100) }
        applyButton.setOnClickListener {
            confirmRestart("Apply the selected tweaks. Roblox needs a restart for them to take effect.") {
                applyFlags(it)
            }
        }
        viewButton.setOnClickListener { viewFlags() }
        removeButton.setOnClickListener {
            confirmRestart("Remove all FastStrap flags. Other flags in the file are kept.") {
                removeFlags(it)
            }
        }
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
            s.contentDescription = "Theme ${i + 1}" + if (i == themeIndex) ", selected" else ""
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

    private fun robloxInstalled(): Boolean =
        try {
            packageManager.getPackageInfo(robloxPkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

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
            !robloxInstalled() -> "Roblox ($robloxPkg) isn't installed."
            else -> ""
        }
        hintText.visibility = if (hintText.text.isEmpty()) View.GONE else View.VISIBLE
        requestButton.visibility = if (alive && !ready) View.VISIBLE else View.GONE
        val enabled = ready && !busy
        listOf(applyButton, viewButton, removeButton).forEach {
            it.isEnabled = enabled
            it.alpha = if (enabled) 1f else 0.4f
        }
    }

    /**
     * Runs a shell command through Shizuku. stderr is merged into stdout inside the shell so a
     * full stderr buffer can't deadlock us, and a watchdog kills the process after a timeout.
     */
    private fun runShell(cmd: String): String {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(
            null, arrayOf("sh", "-c", "{ $cmd\n} 2>&1"), null, null
        ) as Process
        val timedOut = AtomicBoolean(false)
        val killer = watchdog.schedule({
            timedOut.set(true)
            process.destroy()
        }, shellTimeoutSeconds, TimeUnit.SECONDS)
        try {
            val out = process.inputStream.bufferedReader().readText()
            process.waitFor()
            return if (timedOut.get()) out + "\n(timed out after ${shellTimeoutSeconds}s)" else out
        } finally {
            killer.cancel(false)
            process.destroy()
        }
    }

    /** Runs [task] off the main thread, disables the buttons meanwhile, then shows its result. */
    private fun runTask(task: () -> String) {
        if (busy) return
        busy = true
        outputText.text = "Running..."
        updateStatus()
        executor.execute {
            val result = try {
                task()
            } catch (e: Exception) {
                "Error: $e"
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                outputText.text = result
                updateStatus()
            }
        }
    }

    private fun confirmRestart(message: String, action: (restart: Boolean) -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Restart Roblox?")
            .setMessage(message)
            .setPositiveButton("Continue & restart") { _, _ -> action(true) }
            .setNeutralButton("Continue, no restart") { _, _ -> action(false) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fpsOnScript(): String = "setprop $fpsProp true\n"

    // The restore block cleans up refresh-rate backups that may have been written by older builds.
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

    private fun managedKeys(): Set<String> = tweaks.flatMap { it.flags.keys }.toSet()

    /**
     * Merges our flags into whatever is already in the flags file: our keys are replaced,
     * everything else is preserved. Returns null when nothing is left (file should be deleted).
     */
    private fun mergeJson(existing: String, selected: List<Tweak>): String? {
        val obj = try {
            if (existing.isBlank()) JSONObject() else JSONObject(existing)
        } catch (e: JSONException) {
            JSONObject()
        }
        managedKeys().forEach { obj.remove(it) }
        selected.forEach { t -> t.flags.forEach { (k, v) -> obj.put(k, v) } }
        return if (obj.length() == 0) null else obj.toString(2)
    }

    private fun writeFlagsScript(json: String?): String =
        if (json == null) {
            "rm -f $flagsPath\n"
        } else {
            "cat > $flagsPath.new <<'FASTSTRAP_END'\n$json\nFASTSTRAP_END\n" +
                "mv -f $flagsPath.new $flagsPath && chmod 0644 $flagsPath\n"
        }

    private fun applyFlags(restart: Boolean) {
        val editor = prefs.edit()
        tweaks.forEach { editor.putBoolean(it.prefKey, it.toggle.isChecked) }
        editor.apply()

        // Snapshot UI state here; the task runs on a worker thread.
        val selected = tweaks.filter { it.toggle.isChecked }
        val fpsOn = cbFps.isChecked

        runTask {
            val json = mergeJson(runShell("cat $flagsPath 2>/dev/null"), selected)
            val sb = StringBuilder()
            sb.append(writeFlagsScript(json))
            sb.append(if (fpsOn) fpsOnScript() else fpsOffScript())
            if (restart) sb.append("am force-stop $robloxPkg\n")
            sb.append("echo 'Applied ${selected.size} tweak(s). " +
                if (restart) "Roblox was restarted.'\n" else "Restart Roblox to use them.'\n")
            sb.append("echo; ls -la $flagsPath 2>&1\n")
            sb.append("echo; cat $flagsPath 2>&1\n")
            sb.append("echo; echo fps_prop=\$(getprop $fpsProp)\n")
            runShell(sb.toString())
        }
    }

    private fun viewFlags() {
        runTask {
            runShell(
                "echo '== flags file =='; " +
                    "if [ -f $flagsPath ]; then cat $flagsPath; else echo '(no flags file)'; fi; " +
                    "echo; echo '== fps =='; echo fps_prop=\$(getprop $fpsProp); " +
                    "echo peak=\$(settings get system peak_refresh_rate) " +
                    "min=\$(settings get system min_refresh_rate)"
            )
        }
    }

    private fun removeFlags(restart: Boolean) {
        switches.forEach { it.isChecked = false }
        val keepTheme = themeIndex
        prefs.edit().clear().putInt("theme", keepTheme).apply()
        runTask {
            // Only our keys are removed; flags the user added by hand stay.
            val json = mergeJson(runShell("cat $flagsPath 2>/dev/null"), emptyList())
            val sb = StringBuilder()
            sb.append(writeFlagsScript(json))
            sb.append(fpsOffScript())
            if (restart) sb.append("am force-stop $robloxPkg\n")
            sb.append("echo 'FastStrap flags removed. " +
                if (restart) "Roblox was restarted.'" else "Restart Roblox to finish.'")
            runShell(sb.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        executor.shutdownNow()
        watchdog.shutdownNow()
    }
}
