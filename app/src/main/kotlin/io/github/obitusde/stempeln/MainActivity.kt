// Stempeln-App: Oberflaeche v0.2.0 - Status mit Balken, Arbeitszeit (Soll / bereits / fehlt / Schluss), Ueberstunden,
// Planen (mit Zurueck), Urlaub (erster + letzter Urlaubstag). Rechnet nur, solange sie offen ist.
package io.github.obitusde.stempeln

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import io.github.obitusde.stempeln.logik.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class MainActivity : Activity() {
    private lateinit var warnungen: LinearLayout
    private lateinit var statusKarte: LinearLayout
    private lateinit var kontoKarte: LinearLayout
    private lateinit var meldung: TextView
    private lateinit var infos: TextView
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable { override fun run() { zeigen(); h.postDelayed(this, 30_000) } }
    private val meldungWeg = Runnable { meldung.text = ""; meldung.visibility = View.GONE }
    private val neu = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) = zeigen() }

    private val blau = 0xFF1E5BD8.toInt()
    private val gruen = 0xFF1E8E3E.toInt()
    private val orange = 0xFFE37400.toInt()
    private val lila = 0xFF7B1FA2.toInt()
    private val grau = 0xFF5F6368.toInt()
    private val dunkel = 0xFF202124.toInt()

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun text(groesse: Float, fett: Boolean = false, farbe: Int = dunkel, t: String = "") = TextView(this).apply {
        textSize = groesse; setTextColor(farbe); if (fett) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(2), 0, dp(2)); text = t
    }
    private fun rund(farbe: Int, radius: Int = 14) = GradientDrawable().apply { cornerRadius = dp(radius).toFloat(); setColor(farbe) }
    private fun taste(t: String, primaer: Boolean = false, f: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; textSize = 17f; minHeight = dp(56); stateListAnimator = null
        setTextColor(if (primaer) Color.WHITE else blau); if (primaer) typeface = Typeface.DEFAULT_BOLD
        background = RippleDrawable(ColorStateList.valueOf(0x33000000), rund(if (primaer) blau else 0xFFDCE6FA.toInt()), null)
        setOnClickListener { f() }
    }
    private fun paar(a: View, b: View) = LinearLayout(this).apply {
        addView(a, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, dp(4), dp(6), dp(4)) })
        addView(b, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(6), dp(4), 0, dp(4)) })
    }
    private fun karte() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)); background = rund(Color.WHITE, 16)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(6), 0, dp(10)) }
    }
    /** Zeile "Bezeichnung ........ Wert" */
    private fun zeile(k: LinearLayout, links: String, rechts: String, farbe: Int = dunkel, fett: Boolean = false) =
        k.addView(LinearLayout(this).apply {
            setPadding(0, dp(3), 0, dp(3))
            addView(text(16f, t = links, farbe = grau), LinearLayout.LayoutParams(0, -2, 1f))
            addView(text(16f, fett, farbe, rechts).apply { gravity = Gravity.END })
        })
    private fun chip(k: LinearLayout, t: String, farbe: Int) = k.addView(text(13f, true, Color.WHITE, t).apply {
        background = rund(farbe, 12); setPadding(dp(10), dp(3), dp(10), dp(3))
    }, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(6) })

    private val wt = arrayOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    private fun datum(d: LocalDate) = "${wt[d.dayOfWeek.value - 1]} ${d.dayOfMonth}.${d.monthValue}."
    private fun wiederLos(u: Urlaub) = Waechter.naechsterStart(u.bis.plusDays(1), null).toLocalDate()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Steuerung.kanaele(this)
        warnungen = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        statusKarte = karte(); kontoKarte = karte()
        meldung = text(15f, farbe = blau).apply { visibility = View.GONE; setPadding(dp(4), dp(6), dp(4), dp(6)) }
        infos = text(13f, farbe = grau)
        val inhalt = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(20), dp(16), dp(28))
            addView(text(26f, true, blau, "Stempeln"))
            addView(warnungen); addView(statusKarte)
            addView(paar(taste("Stempeln", true) { befehl("stempeln") }, taste("Stand lesen") { befehl("stand") }))
            addView(paar(taste("Planen") { planen() }, taste("Urlaub") { urlaub() }))
            addView(meldung); addView(kontoKarte)
            addView(karte().apply { addView(infos) })
            addView(paar(taste("Skripte aktualisieren") { skripte() }, taste("App aktualisieren") { app() }))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFFEEF1F6.toInt()); addView(inhalt)
            setOnApplyWindowInsetsListener { v, ins ->
                val r = ins.getInsets(WindowInsets.Type.systemBars()); v.setPadding(r.left, r.top, r.right, r.bottom); ins }
        })
        // Android 15 zeichnet bis unter die Statusleiste: dunkle Symbole oben, Inhalt unter die Leiste schieben
        window.insetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(neu, IntentFilter(Steuerung.AKTUALISIERT), Context.RECEIVER_NOT_EXPORTED)
        Thread { Steuerung.aktualisieren(applicationContext, "app") }.start()
        h.post(tick)
    }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick); try { unregisterReceiver(neu) } catch (_: Exception) {} }

    /** Meldung unter den Knoepfen, verschwindet nach 60 s. */
    private fun melde(s: String) {
        h.removeCallbacks(meldungWeg); meldung.text = s; meldung.visibility = View.VISIBLE; h.postDelayed(meldungWeg, 60_000)
    }

    private fun befehl(b: String) {
        Steuerung.makro(this, b)
        melde(if (b == "stand") "Stand lesen läuft – danach kommst du hierher zurück" else "Stempeln läuft – danach kommst du hierher zurück")
    }

    private fun zeigen() {
        pruefeRechte()
        statusKarte.removeAllViews(); kontoKarte.removeAllViews()
        if (!Speicher.zugriff()) { statusKarte.addView(text(18f, true, t = "Zugriff auf Dateien fehlt (oben erlauben)")); infos.text = ""; return }
        val l = Steuerung.lage(this)
        val z = Waechter.heute(l)
        val heute = l.jetzt.toLocalDate()
        val u = Speicher.urlaub(this)
        val k = statusKarte
        val ortText = { t: String? -> if (t.isNullOrEmpty()) "" else " · " + knopf(t) }

        if (z.buch == 0 && u != null && u.umfasst(heute)) {
            chip(k, "Urlaub", lila)
            k.addView(text(24f, true, t = "Urlaub bis ${datum(u.bis)}"))
            k.addView(text(15f, farbe = grau, t = "${datum(u.bis)} ist dein letzter Urlaubstag. Am ${datum(wiederLos(u))} geht es wieder los – bis dahin ruht alles."))
        } else if (z.buch == 0 && !Waechter.istArbeitstag(heute, u)) {
            chip(k, "Frei", grau)
            k.addView(text(22f, true, t = "Heute kein Arbeitstag"))
            k.addView(text(15f, farbe = grau, t = "Nächster Arbeitstag: ${datum(Waechter.naechsterStart(heute, u).toLocalDate())}"))
        } else {
            when {
                z.buch == 0 -> { chip(k, "Noch nicht eingestempelt", grau); Steuerung.aktuelleZone(this)?.let { k.addView(text(15f, farbe = grau, t = "Ort: " + knopf(it))) } }
                z.status == "IN" -> { chip(k, "Eingestempelt", gruen); k.addView(text(15f, farbe = grau, t = "seit ${z.letzte}" + ortText(z.ort))) }
                else -> { chip(k, "Ausgestempelt", orange); k.addView(text(15f, farbe = grau, t = "seit ${z.letzte}" + ortText(z.ort))) }
            }
            val g = Anzeige.tag(l)
            val voll = g.fehlt == 0
            k.addView(text(30f, true, if (voll) gruen else blau,
                if (voll) "Soll erreicht" + (if (g.mehr > 0) " (+${Anzeige.lang(g.mehr)})" else "") else "noch ${Anzeige.lang(g.fehlt)}"))
            k.addView(Balken(this).apply { anteil = g.bereits.toFloat() / g.soll; farbe = if (voll) gruen else if (z.status == "IN") blau else 0xFF8AA6E0.toInt() },
                LinearLayout.LayoutParams(-1, dp(18)).apply { setMargins(0, dp(8), 0, dp(4)) })
            k.addView(text(13f, farbe = grau, t = "${Anzeige.lang(g.bereits)} von ${Anzeige.lang(g.soll)} · ${g.bereits * 100 / g.soll} %"))
            k.addView(View(this), LinearLayout.LayoutParams(-1, dp(8)))
            zeile(k, "Arbeitszeit Soll", Anzeige.lang(g.soll))
            zeile(k, "Bereits gearbeitet", Anzeige.lang(g.bereits))
            zeile(k, "Fehlt noch", if (voll) "nichts" else Anzeige.lang(g.fehlt), if (voll) gruen else dunkel, true)
            zeile(k, "Voraussichtlich Schluss", g.schluss?.let { hhmm(it) } ?: "jetzt", if (voll) gruen else blau, true)
            k.addView(text(13f, farbe = grau, t = g.hinweis))
            if (u != null) k.addView(text(14f, farbe = lila, t = "Urlaub eingetragen: ${datum(u.von)} bis ${datum(u.bis)} (letzter Urlaubstag)"))
        }
        Speicher.plan(this)?.let { p -> k.addView(text(15f, true, blau, "Geplantes Stempeln um %02d:%02d".format(p.hour, p.minute))) }

        // Ueberstunden (Saldo der Stempelseite, bis gestern)
        val kk = kontoKarte
        val kt = Speicher.konto()
        val s = kt?.let { minutenOderNull(it.solde) }
        if (kt != null && s != null) {
            kk.addView(LinearLayout(this).apply {
                addView(text(17f, true, t = "Überstunden"), LinearLayout.LayoutParams(0, -2, 1f))
                addView(text(22f, true, if (s >= 0) gruen else orange, Anzeige.lang(s)))
            })
            kk.addView(text(15f, farbe = grau, t = "= ${Anzeige.tage(s)} Arbeitstage zu 8 h 25 (Stand bis gestern)"))
            val wann = LocalDateTime.ofInstant(Instant.ofEpochSecond(kt.lese), ZoneId.of("Europe/Zurich"))
            kk.addView(text(13f, farbe = grau, t = "Stempelseite gelesen: " + (if (wann.toLocalDate() == heute) "heute " else datum(wann.toLocalDate()) + " ") +
                "%02d:%02d".format(wann.hour, wann.minute)))
        } else kk.addView(text(15f, farbe = grau, t = "Überstunden: noch nicht gelesen – „Stand lesen“ tippen"))

        infos.text = listOfNotNull(
            Speicher.naechster(this).ifEmpty { null },
            "Skripte: " + (Speicher.skriptVersion()?.let { "v$it" } ?: "fehlen – „Skripte aktualisieren“ tippen"),
            "Shizuku: " + if (Steuerung.shizukuLaeuft()) "läuft" else "läuft nicht (MacroDroid kann dann nicht tippen)",
            "App v" + packageManager.getPackageInfo(packageName, 0).versionName,
        ).joinToString("\n")
    }

    private fun pruefeRechte() {
        warnungen.removeAllViews()
        fun warn(t: String, f: () -> Unit) = warnungen.addView(taste("⚠ $t") { f() }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) })
        if (!Speicher.zugriff()) warn("Zugriff auf alle Dateien erlauben (für Download/stempeln)") {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))) }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            warn("Benachrichtigungen erlauben") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1) }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            warn("Standort erlauben (nur morgens für die Ankunft)") { requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2) }
        else if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            warn("Standort: „Immer zulassen“ wählen") { requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 3) }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        Thread { Steuerung.aktualisieren(applicationContext, "rechte") }.start(); zeigen()
    }

    // ---------- Planen ----------
    private fun planSetzen(t: LocalDateTime) {
        if (!t.isAfter(Steuerung.jetzt())) { melde("Die Uhrzeit ist schon vorbei"); return }
        Speicher.setzePlan(this, t); Steuerung.planStellen(this)
        melde("Geplant: Stempeln um %02d:%02d – Handy dann entsperrt lassen oder entsperren".format(t.hour, t.minute)); zeigen()
    }

    private fun planen() {
        val j = Steuerung.jetzt().withSecond(0).withNano(0); val heute = j.toLocalDate()
        val plan = Speicher.plan(this)
        val wahl = mutableListOf<Pair<String, () -> Unit>>("in 15 min" to { planSetzen(j.plusMinutes(15)) }, "in 30 min" to { planSetzen(j.plusMinutes(30)) },
            "in 45 min" to { planSetzen(j.plusMinutes(45)) }, "in 1 h" to { planSetzen(j.plusHours(1)) },
            "um 17:00" to { planSetzen(heute.atTime(17, 0)) }, "um 17:30" to { planSetzen(heute.atTime(17, 30)) },
            "Uhrzeit wählen …" to { TimePickerDialog(this, { _, hh, mm -> planSetzen(heute.atTime(LocalTime.of(hh, mm))) }, j.hour, j.minute, true).show() })
        if (plan != null) wahl += "Plan löschen (%02d:%02d)".format(plan.hour, plan.minute) to {
            Speicher.setzePlan(this, null); Steuerung.planStellen(this); melde("Plan gelöscht"); zeigen() }
        AlertDialog.Builder(this).setTitle("Heute später stempeln")
            .setItems(wahl.map { it.first }.toTypedArray()) { _, i -> wahl[i].second() }
            .setNegativeButton("Zurück", null).show()
    }

    // ---------- Urlaub ----------
    private fun tagWaehlen(titel: String, hinweis: String, start: LocalDate, ab: LocalDate, weiter: String, fertig: (LocalDate) -> Unit) {
        val w = DatePicker(this).apply {
            minDate = ab.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            updateDate(start.year, start.monthValue - 1, start.dayOfMonth)
        }
        AlertDialog.Builder(this).setTitle(titel).setMessage(hinweis).setView(w)
            .setPositiveButton(weiter) { _, _ -> fertig(LocalDate.of(w.year, w.month + 1, w.dayOfMonth)) }
            .setNegativeButton("Zurück", null).show()
    }

    private fun urlaubEintragen() {
        val h = LocalDate.now(Regeln.ZONE)
        tagWaehlen("Erster Urlaubstag", "An welchem Tag beginnt dein Urlaub?", h, h, "Weiter") { von ->
            tagWaehlen("Letzter Urlaubstag", "Den LETZTEN Tag eintragen, an dem du frei hast – nicht den Tag, an dem du wieder arbeitest.",
                von, von, "Speichern") { bis ->
                val u = Urlaub(von, bis)
                Speicher.setzeUrlaub(this, u)
                melde("Urlaub ${datum(von)} bis ${datum(bis)} eingetragen – am ${datum(wiederLos(u))} geht es wieder los")
                Thread { Steuerung.aktualisieren(applicationContext, "urlaub") }.start(); zeigen()
            }
        }
    }

    private fun urlaub() {
        val u = Speicher.urlaub(this) ?: return urlaubEintragen()
        AlertDialog.Builder(this).setTitle("Urlaub")
            .setMessage("Eingetragen: ${datum(u.von)} bis ${datum(u.bis)} (letzter Urlaubstag).\nAm ${datum(wiederLos(u))} geht es wieder los. In der Zeit ruht alles.")
            .setPositiveButton("Ändern") { _, _ -> urlaubEintragen() }
            .setNeutralButton("Löschen") { _, _ ->
                Speicher.setzeUrlaub(this, null); melde("Urlaub gelöscht")
                Thread { Steuerung.aktualisieren(applicationContext, "urlaub") }.start(); zeigen() }
            .setNegativeButton("Zurück", null).show()
    }

    // ---------- Aktualisieren ----------
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
