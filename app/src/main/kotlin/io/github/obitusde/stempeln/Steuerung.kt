// Stempeln-App 5.0: Steuerung - wird bei jedem Aufwachen einmal kurz durchlaufen (Wecker, Ortszone, Knopf, neuer Stand, Start)
package io.github.obitusde.stempeln

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.Notification
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import io.github.obitusde.stempeln.logik.*
import java.time.LocalDate
import java.time.LocalDateTime

object Steuerung {
    const val KANAL_ERINNERUNG = "erinnerung"
    const val KANAL_BEFEHL = "befehl"
    const val KANAL_INFO = "info"
    const val ID_ERINNERUNG = 4711
    const val ID_BEFEHL = 4720
    const val ID_INFO = 4730
    const val ID_LAUF = 4740          // Fortschritt und Ergebnis eines Laufs (eine Meldung, wird ersetzt)
    const val KANAL_LAUF = "lauf"
    const val KANAL_OK = "geklappt"
    const val KANAL_PROBLEM = "problem"
    const val AKTUALISIERT = "io.github.obitusde.stempeln.AKTUALISIERT"

    fun jetzt(): LocalDateTime = LocalDateTime.now(Regeln.ZONE)
    fun epoch(): Long = System.currentTimeMillis() / 1000

    fun kanaele(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(KANAL_ERINNERUNG, "Erinnerungen", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(KANAL_INFO, "Hinweise", NotificationManager.IMPORTANCE_DEFAULT))
        // Befehle an MacroDroid: still, MacroDroid liest sie trotzdem
        nm.createNotificationChannel(NotificationChannel(KANAL_BEFEHL, "Befehle an MacroDroid (still)", NotificationManager.IMPORTANCE_MIN))
        nm.createNotificationChannel(NotificationChannel(KANAL_LAUF, "Stempeln läuft (still)", NotificationManager.IMPORTANCE_LOW))
        fun mitTon(id: String, name: String, wichtig: Int, ton: Int, muster: LongArray) = nm.createNotificationChannel(NotificationChannel(id, name, wichtig).apply {
            setSound(android.net.Uri.parse("android.resource://${c.packageName}/$ton"),
                android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true); vibrationPattern = muster
        })
        mitTon(KANAL_OK, "Geklappt", NotificationManager.IMPORTANCE_DEFAULT, R.raw.geklappt, longArrayOf(0, 120, 80, 120))
        mitTon(KANAL_PROBLEM, "Problem", NotificationManager.IMPORTANCE_HIGH, R.raw.problem, longArrayOf(0, 400, 150, 400, 150, 600))
    }

    fun lage(c: Context, zone: String? = aktuelleZone(c)): Lage = Lage(
        jetzt(), epoch(), Speicher.zustand(), Speicher.urlaub(c), Speicher.spaeterBis(), Speicher.heuteNicht(),
        zone, Speicher.letzte(c), Speicher.nachfrage(c), Speicher.einstellungen(),
    )

    fun aktuelleZonenName(c: Context): String? = Speicher.zonenName(Speicher.drin(c), aktuelleZone(c))

    /** Meldung der Skripte (Fortschritt oder Ergebnis eines Laufs): art = fortschritt | hinweis | ok | problem. */
    fun laufMeldung(c: Context, art: String, text: String) {
        kanaele(c)
        val t = jetzt()
        val (kanal, titel) = when (art) {
            "ok" -> KANAL_OK to "✅ Geklappt"
            "problem" -> KANAL_PROBLEM to "⚠ Bitte prüfen"
            "hinweis" -> KANAL_INFO to "Stempeln – bitte schauen"
            else -> KANAL_LAUF to "Stempeln läuft …"
        }
        val inhalt = "%02d:%02d · %s".format(t.hour, t.minute, text)
        val b = Notification.Builder(c, kanal).setSmallIcon(R.drawable.ic_stempel).setContentTitle(titel).setContentText(inhalt)
            .setStyle(Notification.BigTextStyle().bigText(inhalt))
            .setContentIntent(PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true).setOnlyAlertOnce(false)
        if (art == "fortschritt" || art == "hinweis") b.setTimeoutAfter(3 * 60_000L)
        c.getSystemService(NotificationManager::class.java).notify(ID_LAUF, b.build())
    }

    fun aktuelleZone(c: Context): String? {
        val drin = Speicher.drin(c).map { it.substringBefore('#') }
        return listOf("HUB", "SS", "HO").firstOrNull { it in drin }
    }

    /** Ein Durchlauf: entscheiden, Erinnerung zeigen/wegnehmen, Wecker und Ortszonen stellen. */
    @Synchronized
    fun aktualisieren(c: Context, quelle: String) {
        kanaele(c)
        if (!Speicher.zugriff()) return
        val l = lage(c)
        val e = Waechter.entscheide(l)
        val nm = c.getSystemService(NotificationManager::class.java)
        val er = e.erinnerung
        if (er != null) {
            erinnerungZeigen(c, er)
            Speicher.schreibVorschlag(er, l.epoch, l.jetzt.toLocalDate())
            Speicher.setzeLetzte(c, Letzte(er.ziel, er.label, er.grund, l.epoch, l.jetzt.toLocalDate()))
            if (er.grund == "Nachfrage") Speicher.setzeNachfrage(c, l.jetzt.toLocalDate())
        } else if (!e.unterdrueckt) nm.cancel(ID_ERINNERUNG)
        ortszonen(c, e.ortszonen)
        weckerStellen(c, e.wecker)
        planStellen(c)
        val w = e.wecker
        Speicher.setzeNaechster(c, (if (w != null) "Nächstes Aufwachen: " + (if (w.toLocalDate() == l.jetzt.toLocalDate()) "" else w.toLocalDate().let { "${it.dayOfMonth}.${it.monthValue}. " }) +
            "%02d:%02d".format(w.hour, w.minute) else "") + (if (e.grund.isNotEmpty()) " (${e.grund})" else ""))
        // Morgens beim ersten Aufwachen: laeuft Shizuku? (ohne Shizuku kann MacroDroid nicht tippen)
        if (e.ortszonen && Speicher.shizukuWarnung(c) != l.jetzt.toLocalDate()) {
            Speicher.setzeShizukuWarnung(c, l.jetzt.toLocalDate())
            if (!shizukuLaeuft()) hinweis(c, "Shizuku läuft nicht", "Bitte Shizuku starten (nach jedem Neustart nötig) – sonst kann MacroDroid nicht stempeln.")
        }
        c.sendBroadcast(Intent(AKTUALISIERT).setPackage(c.packageName))
    }

    fun shizukuLaeuft(): Boolean = try { rikka.shizuku.Shizuku.pingBinder() } catch (t: Throwable) { false }

    private fun pi(c: Context, code: Int, i: Intent) =
        PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun erinnerungZeigen(c: Context, e: Erinnerung) {
        val t = jetzt()
        val b = Notification.Builder(c, KANAL_ERINNERUNG)
            .setSmallIcon(R.drawable.ic_stempel)
            .setContentTitle("Stempeln?")
            .setContentText("%02d:%02d · %s".format(t.hour, t.minute, e.anzeige))
            .setStyle(Notification.BigTextStyle().bigText("%02d:%02d · %s".format(t.hour, t.minute, e.anzeige)))
            .setContentIntent(PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
        fun knopf(text: String, aktion: String, code: Int) =
            b.addAction(Notification.Action.Builder(null, text, pi(c, code, Intent(c, KnopfEmpfaenger::class.java).setAction(aktion))).build())
        knopf("Stempeln", KnopfEmpfaenger.STEMPELN, 11)
        knopf("Schon gestempelt", KnopfEmpfaenger.STAND, 12)
        if (e.heim) knopf("Heute nicht", KnopfEmpfaenger.HEUTE_NICHT, 13) else knopf("Später", KnopfEmpfaenger.SPAETER, 14)
        c.getSystemService(NotificationManager::class.java).notify(ID_ERINNERUNG, b.build())
    }

    fun hinweis(c: Context, titel: String, text: String) {
        kanaele(c)
        val n = Notification.Builder(c, KANAL_INFO).setSmallIcon(R.drawable.ic_stempel).setContentTitle(titel).setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true).build()
        c.getSystemService(NotificationManager::class.java).notify(ID_INFO, n)
    }

    /** MacroDroid-Makro starten: stille Meldung "BEFEHL-STEMPELN" / "BEFEHL-STAND", auf die das Makro hoert. */
    fun makro(c: Context, befehl: String) {
        kanaele(c)
        // gestempelt wird jetzt (Knopf in der App, Erinnerung oder Plan) -> ein geplanter Termin ist erledigt
        if (befehl != "stand" && Speicher.plan(c) != null) { Speicher.setzePlan(c, null); planStellen(c) }
        c.getSystemService(NotificationManager::class.java).cancel(ID_ERINNERUNG)
        val text = if (befehl == "stand") "BEFEHL-STAND" else "BEFEHL-STEMPELN"
        val n = Notification.Builder(c, KANAL_BEFEHL).setSmallIcon(R.drawable.ic_stempel)
            .setContentTitle("Stempeln-App").setContentText(text).setTimeoutAfter(20_000).build()
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.cancel(ID_BEFEHL)
        nm.notify(ID_BEFEHL, n)
        laufMeldung(c, "fortschritt", if (befehl == "stand") "Stand lesen startet …" else "Stempeln startet …")
    }

    private fun alarmZeit(t: LocalDateTime) = t.atZone(Regeln.ZONE).toInstant().toEpochMilli()

    @SuppressLint("MissingPermission")
    private fun weckerStellen(c: Context, t: LocalDateTime?) {
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pi(c, 1, Intent(c, WeckerEmpfaenger::class.java).setAction(WeckerEmpfaenger.WAECHTER))
        if (t == null) { am.cancel(p); return }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmZeit(t), p)
    }

    @SuppressLint("MissingPermission")
    fun planStellen(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pi(c, 3, Intent(c, WeckerEmpfaenger::class.java).setAction(WeckerEmpfaenger.PLAN))
        val t = Speicher.plan(c)
        if (t == null) am.cancel(p) else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmZeit(t), p)
    }

    fun ortPi(c: Context): PendingIntent = PendingIntent.getBroadcast(c, 2, Intent(c, OrtEmpfaenger::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

    fun standortErlaubt(c: Context) =
        c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
        c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Ortszonen nur morgens bis zum Einstempeln anmelden, sonst abmelden (dann fragt Android nie nach dem Standort). */
    @SuppressLint("MissingPermission")
    private fun ortszonen(c: Context, an: Boolean) {
        val client = LocationServices.getGeofencingClient(c)
        if (!an) {
            if (Speicher.zonenAktiv(c)) { client.removeGeofences(ortPi(c)); Speicher.setzeZonenAktiv(c, false) }
            Speicher.setzeDrin(c, emptySet())
            return
        }
        if (Speicher.zonenAktiv(c) || !standortErlaubt(c)) return
        val orte = Speicher.orte()
        if (orte.isEmpty()) return
        val zonen = orte.mapIndexed { i, o ->
            Geofence.Builder().setRequestId("${o.typ}#$i").setCircularRegion(o.lat, o.lon, maxOf(o.radius, 150).toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT).build()
        }
        val req = GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofences(zonen).build()
        Speicher.setzeDrin(c, emptySet())
        client.addGeofences(req, ortPi(c)).addOnSuccessListener { Speicher.setzeZonenAktiv(c, true) }
            .addOnFailureListener { hinweis(c, "Ortszonen gehen nicht", "Android hat die Ortszonen abgelehnt (${it.message}). Standort an? In der App 'Standort: immer' erlauben.") }
    }

    fun naechstesDatum(t: LocalDateTime): LocalDate = t.toLocalDate()
}
