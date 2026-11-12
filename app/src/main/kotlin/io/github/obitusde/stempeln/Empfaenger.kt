// Stempeln-App: Empfaenger - jeder wacht kurz auf, erledigt seine Sache und ruft einmal die Steuerung auf
package io.github.obitusde.stempeln

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** Wecker: Waechter-Pruefung oder geplantes Stempeln. */
class WeckerEmpfaenger : BroadcastReceiver() {
    companion object { const val WAECHTER = "waechter"; const val PLAN = "plan" }
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == PLAN) {
            Speicher.setzePlan(c, null)
            Steuerung.makro(c, "stempeln")
            Steuerung.hinweis(c, "Geplantes Stempeln", "Stempeln läuft – ist das Handy gesperrt, bitte entsperren.")
        }
        Steuerung.aktualisieren(c, i.action ?: "wecker")
    }
}

/** Ortszonen: betreten / verlassen (nur morgens angemeldet). */
class OrtEmpfaenger : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val ev = GeofencingEvent.fromIntent(i) ?: return
        if (ev.hasError()) return
        val ids = ev.triggeringGeofences?.map { it.requestId } ?: return
        val drin = Speicher.drin(c).toMutableSet()
        when (ev.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> drin += ids
            Geofence.GEOFENCE_TRANSITION_EXIT -> drin -= ids.toSet()
        }
        Speicher.setzeDrin(c, drin)
        Steuerung.aktualisieren(c, "ort")
    }
}

/** Knoepfe in der Erinnerung. */
class KnopfEmpfaenger : BroadcastReceiver() {
    companion object { const val STEMPELN = "stempeln"; const val STAND = "stand"; const val SPAETER = "spaeter"; const val HEUTE_NICHT = "heutenicht" }
    override fun onReceive(c: Context, i: Intent) {
        when (i.action) {
            STEMPELN -> Steuerung.makro(c, "stempeln")
            STAND -> Steuerung.makro(c, "stand")
            SPAETER -> Speicher.setzeSpaeter(Steuerung.epoch() + 15 * 60)
            HEUTE_NICHT -> Speicher.setzeHeuteNicht(Steuerung.jetzt().toLocalDate())
        }
        c.getSystemService(android.app.NotificationManager::class.java).cancel(Steuerung.ID_ERINNERUNG)
        if (i.action == SPAETER || i.action == HEUTE_NICHT) Steuerung.aktualisieren(c, i.action!!)
    }
}

/** Die Skripte haben einen neuen Stand gelesen (nach Stempeln / Stand). */
class StandEmpfaenger : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) = Steuerung.aktualisieren(c, "stand")
}

/** Nach Neustart / App-Update: Wecker und Ortszonen neu stellen (Android vergisst sie). */
class StartEmpfaenger : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Speicher.setzeZonenAktiv(c, false)
        Steuerung.aktualisieren(c, "start")
    }
}
