// Stempeln-App: Oberflaeche - Stand, Konto, Plan, Urlaub, Knoepfe. Rechnet nur, solange sie offen ist.
package io.github.obitusde.stempeln

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import io.github.obitusde.stempeln.logik.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var konto: TextView
    private lateinit var infos: TextView
    private lateinit var warnungen: LinearLayout
    private lateinit var meldung: TextView
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable { override fun run() { zeigen(); h.postDelayed(this, 30_000) } }
    private val neu = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) = zeigen() }
    private val blau = Color.parseColor("#1E5BD8")

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun text(groesse: Float, fett: Boolean = false, farbe: Int = Color.parseColor("#202124")) = TextView(this).apply {
        textSize = groesse; setTextColor(farbe); if (fett) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(4), 0, dp(4))
    }
    private fun taste(t: String, f: () -> Unit) = Button(this).apply { text = t; isAllCaps = false; textSize = 16f; setOnClickListener { f() } }
    private fun karte(vararg v: View) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12))
        background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(Color.WHITE) }
        v.forEach { addView(it) }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Steuerung.kanaele(this)
        status = text(20f, true); konto = text(15f); infos = text(13f, farbe = Color.parseColor("#5F6368"))
        meldung = text(14f, farbe = blau)
        warnungen = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val zeile1 = LinearLayout(this).apply { addView(taste("Stempeln") { Steuerung.makro(this@MainActivity, "stempeln"); melde("Stempeln gestartet") }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(taste("Stand lesen") { Steuerung.makro(this@MainActivity, "stand"); melde("Stand lesen gestartet") }, LinearLayout.LayoutParams(0, -2, 1f)) }
        val zeile2 = LinearLayout(this).apply { addView(taste("Planen") { planen() }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(taste("Urlaub") { urlaub() }, LinearLayout.LayoutParams(0, -2, 1f)) }
        val zeile3 = LinearLayout(this).apply { addView(taste("Skripte aktualisieren") { skripte() }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(taste("App aktualisieren") { app() }, LinearLayout.LayoutParams(0, -2, 1f)) }
        val titel = text(24f, true, blau).apply { text = "Stempeln" }
        val inhalt = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(24), dp(16), dp(24))
            addView(titel); addView(warnungen); addView(karte(status, konto)); addView(zeile1); addView(zeile2)
            addView(meldung); addView(karte(infos)); addView(zeile3)
        }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#EEF1F6")); addView(inhalt) })
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(neu, IntentFilter(Steuerung.AKTUALISIERT), Context.RECEIVER_NOT_EXPORTED)
        Thread { Steuerung.aktualisieren(applicationContext, "app") }.start()
        h.post(tick)
    }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick); try { unregisterReceiver(neu) } catch (_: Exception) {} }

    private fun melde(s: String) { meldung.text = s }

    private fun zeigen() {
        pruefeRechte()
        if (!Speicher.zugriff()) { status.text = "Zugriff auf Dateien fehlt"; konto.text = ""; infos.text = ""; return }
        val l = Steuerung.lage(this)
        val z = Waechter.heute(l)
        val p = Waechter.praesenz(l)
        val now = l.jetzt.hour * 60 + l.jetzt.minute
        val ort = if (z.ort.isNotEmpty()) " · " + knopf(z.ort) else ""
        status.text = when {
            l.zustand.datum != l.jetzt.toLocalDate() -> "Heute noch nicht gestempelt" + (Steuerung.aktuelleZone(this)?.let { " · Ort: " + knopf(it) } ?: "")
            z.status == "IN" -> "Eingestempelt seit ${z.letzte}$ort"
            else -> "Ausgestempelt seit ${z.letzte}$ort"
        }
        val rest = Regeln.SOLL - p
        var k = "Heute ${hm(p)}" + when {
            z.status == "IN" && rest > 0 -> " · noch ${hm(rest)} · fertig um ${hhmm(now + rest)}"
            rest > 0 -> " · noch ${hm(rest)} offen"
            else -> " · 8 h 25 voll (+${hm(-rest)})"
        }
        val kt = Speicher.konto()
        if (kt != null) {
            val wann = LocalDateTime.ofInstant(Instant.ofEpochSecond(kt.lese), Regeln.ZONE)
            k += "\n\nSaldo (bis gestern): ${kt.solde}\nHeures du jour: ${kt.jour}\nRattrapage: ${kt.ratt}\nHS cumulée: ${kt.hs}" +
                "\n(Stand der Stempelseite: " + (if (wann.toLocalDate() == l.jetzt.toLocalDate()) "" else "${wann.dayOfMonth}.${wann.monthValue}. ") +
                "%02d:%02d)".format(wann.hour, wann.minute)
        } else k += "\n\nKonto: noch nicht gelesen (Stand lesen)"
        konto.text = k
        val plan = Speicher.plan(this); val u = Speicher.urlaub(this)
        infos.text = listOfNotNull(
            plan?.let { "Geplant: Stempeln um %02d:%02d".format(it.hour, it.minute) },
            u?.let { "Urlaub: ${it.von.dayOfMonth}.${it.von.monthValue}. bis ${it.bis.dayOfMonth}.${it.bis.monthValue}.${it.bis.year}" },
            Speicher.naechster(this).ifEmpty { null },
            "Skripte: " + (Speicher.skriptVersion()?.let { "v$it" } ?: "fehlen - Skripte aktualisieren"),
            "Shizuku: " + if (Steuerung.shizukuLaeuft()) "laeuft" else "laeuft nicht (MacroDroid kann dann nicht tippen)",
            "App v" + packageManager.getPackageInfo(packageName, 0).versionName,
        ).joinToString("\n")
    }

    private fun pruefeRechte() {
        warnungen.removeAllViews()
        fun warn(t: String, f: () -> Unit) = warnungen.addView(taste("⚠ $t") { f() })
        if (!Speicher.zugriff()) warn("Zugriff auf alle Dateien erlauben (fuer Download/stempeln)") {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))) }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            warn("Benachrichtigungen erlauben") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1) }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            warn("Standort erlauben (nur morgens fuer die Ankunft)") { requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2) }
        else if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            warn("Standort: \"Immer zulassen\" waehlen") { requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 3) }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        Thread { Steuerung.aktualisieren(applicationContext, "rechte") }.start(); zeigen()
    }

    private fun planSetzen(t: LocalDateTime) {
        if (!t.isAfter(Steuerung.jetzt())) { melde("Die Uhrzeit ist schon vorbei"); return }
        Speicher.setzePlan(this, t); Steuerung.planStellen(this)
        melde("Geplant: Stempeln um %02d:%02d (Handy dann bitte entsperrt lassen oder entsperren)".format(t.hour, t.minute)); zeigen()
    }

    private fun planen() {
        val j = Steuerung.jetzt(); val heute = j.toLocalDate()
        val wahl = arrayOf("+30 min", "+1 h", "17:00", "17:30", "Uhrzeit waehlen…", "Plan loeschen")
        AlertDialog.Builder(this).setTitle("Heute zeitversetzt stempeln").setItems(wahl) { _, i ->
            when (i) {
                0 -> planSetzen(j.withSecond(0).withNano(0).plusMinutes(30))
                1 -> planSetzen(j.withSecond(0).withNano(0).plusHours(1))
                2 -> planSetzen(heute.atTime(17, 0))
                3 -> planSetzen(heute.atTime(17, 30))
                4 -> TimePickerDialog(this, { _, hh, mm -> planSetzen(heute.atTime(LocalTime.of(hh, mm))) }, j.hour, j.minute, true).show()
                5 -> { Speicher.setzePlan(this, null); Steuerung.planStellen(this); melde("Plan geloescht"); zeigen() }
            }
        }.show()
    }

    private fun urlaub() {
        val wahl = arrayOf("Urlaub eintragen…", "Urlaub loeschen")
        AlertDialog.Builder(this).setTitle("Urlaub (alles ruht)").setItems(wahl) { _, i ->
            if (i == 1) { Speicher.setzeUrlaub(this, null); melde("Urlaub geloescht"); Thread { Steuerung.aktualisieren(applicationContext, "urlaub") }.start(); zeigen(); return@setItems }
            val h = LocalDate.now(Regeln.ZONE)
            DatePickerDialog(this, { _, y, m, d ->
                val von = LocalDate.of(y, m + 1, d)
                DatePickerDialog(this, { _, y2, m2, d2 ->
                    val bis = LocalDate.of(y2, m2 + 1, d2)
                    if (bis.isBefore(von)) { melde("Ende liegt vor dem Anfang"); return@DatePickerDialog }
                    Speicher.setzeUrlaub(this, Urlaub(von, bis))
                    melde("Urlaub ${von.dayOfMonth}.${von.monthValue}. bis ${bis.dayOfMonth}.${bis.monthValue}. - in der Zeit ruht alles")
                    Thread { Steuerung.aktualisieren(applicationContext, "urlaub") }.start(); zeigen()
                }, von.year, von.monthValue - 1, von.dayOfMonth).apply { setTitle("Letzter Urlaubstag") }.show()
            }, h.year, h.monthValue - 1, h.dayOfMonth).apply { setTitle("Erster Urlaubstag") }.show()
        }.show()
    }

    private fun skripte() {
        melde("Skripte werden geladen …")
        Thread {
            val r = try { Aktualisieren.skripte() } catch (e: Exception) { "Fehler: ${e.message}" }
            runOnUiThread { melde(r); zeigen() }
        }.start()
    }

    private fun app() {
        melde("Suche neue App-Version …")
        Thread {
            try {
                val n = Aktualisieren.neueApp()
                val alt = packageManager.getPackageInfo(packageName, 0).versionName ?: "0"
                if (n == null || !Aktualisieren.istNeuer(n.version, alt)) { runOnUiThread { melde("App v$alt ist aktuell") }; return@Thread }
                if (!packageManager.canRequestPackageInstalls()) {
                    runOnUiThread { melde("Bitte erlauben, dass diese App Apps installieren darf, dann nochmal tippen")
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
                    return@Thread
                }
                runOnUiThread { melde("Lade App v${n.version} …") }
                Aktualisieren.appInstallieren(this, n)
            } catch (e: Exception) { runOnUiThread { melde("Fehler: ${e.message}") } }
        }.start()
    }
}
