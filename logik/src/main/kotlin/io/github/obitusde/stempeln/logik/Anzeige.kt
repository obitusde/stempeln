// Stempeln-Logik: was die App ueber den Tag anzeigt (Soll, bereits, fehlt, Schluss, Pause, naechstes Stempeln,
// Arbeitstage bis zum Arbeitsende, Ueberstunden) - v0.3.0
package io.github.obitusde.stempeln.logik

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/** Tagesbild fuer die Anzeige. Minuten; schluss = Minute des Tages (null = Soll schon erreicht). */
data class Tag(
    val soll: Int,
    val bereits: Int,
    val fehlt: Int,              // 0 wenn erreicht
    val mehr: Int,               // Minuten ueber dem Soll (0 wenn noch nicht erreicht)
    val schluss: Int?,
    val hinweis: String,         // Erklaerung zur Schluss-Zeit
)

/** Was der grosse Knopf beim naechsten Mal tut. */
data class Naechstes(val richtung: String, val knopf: String) {
    val text get() = (if (richtung == "AUS") "Ausstempeln" else "Einstempeln") + " · " + knopf
}

object Anzeige {
    /** Mittagspause noch offen? (heute noch keine gemacht und noch vor 14:30) */
    private fun pauseOffen(z: Zustand, now: Int) = now < Regeln.MITTAG_ENDE && (z.pause == 0 || (z.pause < 0 && z.buch <= 1))

    fun tag(l: Lage): Tag {
        val z = Waechter.heute(l)
        val p = Waechter.praesenz(l)
        val d = l.jetzt.toLocalDate()
        val soll = l.e.sollAm(d)
        val now = l.jetzt.hour * 60 + l.jetzt.minute
        val rest = soll - p
        if (rest <= 0) return Tag(soll, p, 0, -rest, null,
            if (z.status == "IN") "Soll erreicht seit ${hhmm(now + rest)} - du kannst ausstempeln" else "Soll erreicht - Feierabend")
        val lz = minutenOderNull(z.letzte)
        return when {
            z.buch == 0 -> {
                val pz = l.e.pause(l.zone ?: if (l.e.istHoTag(d)) "HO" else "SS")
                Tag(soll, p, rest, 0, now + rest + pz, "wenn du jetzt einstempelst, mit $pz min Mittagspause")
            }
            z.status == "IN" -> {
                val pz = l.e.pause(z.ort)
                if (pauseOffen(z, now)) Tag(soll, p, rest, 0, now + rest + pz, "mit $pz min Mittagspause")
                else Tag(soll, p, rest, 0, now + rest, "wenn du durcharbeitest")
            }
            lz != null && lz in Regeln.ORTBIS..Regeln.MITTAG_ENDE -> {
                val wieder = maxOf(now, lz + l.e.pause(z.ort))
                Tag(soll, p, rest, 0, wieder + rest, "wenn du um ${hhmm(wieder)} wieder einstempelst")
            }
            else -> Tag(soll, p, rest, 0, now + rest, "wenn du jetzt wieder einstempelst")
        }
    }

    /** Pause in Minuten: laeuft gerade / schon gemacht / geplant (null = nichts zu zeigen). */
    fun pause(l: Lage): String? {
        val z = Waechter.heute(l)
        val now = l.jetzt.hour * 60 + l.jetzt.minute
        val d = l.jetzt.toLocalDate()
        val lz = minutenOderNull(z.letzte)
        val geplant = l.e.pause(if (z.buch == 0) (l.zone ?: if (l.e.istHoTag(d)) "HO" else "SS") else z.ort)
        val fertig = Waechter.praesenz(l) >= l.e.sollAm(d)
        return when {
            z.status != "IN" && z.buch > 0 && lz != null && lz in Regeln.ORTBIS..Regeln.MITTAG_ENDE && !fertig -> {
                val seit = now - lz
                if (seit < geplant) "Pause läuft seit $seit min · geplant $geplant min, vorbei um ${hhmm(lz + geplant)}"
                else "Pause läuft seit $seit min · geplant waren $geplant min"
            }
            z.pause > 0 -> "Pause gemacht: ${z.pause} min"
            z.status != "IN" && z.buch > 0 -> null
            pauseOffen(z, now) -> "Pause geplant: $geplant min (Erinnerung um ${hhmm(l.e.mittag)})"
            else -> null
        }
    }

    /** Was der grosse Knopf tut. ortName = Name der Ortszone, in der du gerade bist (morgens). */
    fun naechstes(l: Lage, ortName: String? = null): Naechstes {
        val z = Waechter.heute(l)
        fun mitName(typ: String, name: String?) = knopf(typ) + if (typ == "SS" && !name.isNullOrEmpty()) " ($name)" else ""
        return when {
            z.status == "IN" -> Naechstes("AUS", if (z.ort.isNotEmpty()) knopf(z.ort) else "Ort per Standort")
            z.buch > 0 && z.ort.isNotEmpty() -> Naechstes("EIN", mitName(z.ort, z.name))
            l.zone != null -> Naechstes("EIN", mitName(l.zone, ortName))
            else -> Naechstes("EIN", "Ort per Standort")
        }
    }

    /** Arbeitstage von heute (zaehlt mit) bis zum letzten Arbeitstag: Mo-Fr ohne Waadtlaender Feiertage, Urlaub zaehlt mit. */
    fun arbeitstage(heute: LocalDate, ende: LocalDate): Int {
        var n = 0; var d = heute
        while (!d.isAfter(ende)) {
            if (d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY && !Feiertage.ist(d)) n++
            d = d.plusDays(1)
        }
        return n
    }

    /** "4 h 38 min", "38 min", "8 h", "-1 h 30 min" */
    fun lang(min: Int): String {
        val n = if (min < 0) "-" else ""; val a = Math.abs(min); val h = a / 60; val m = a % 60
        return n + when { h == 0 -> "$m min"; m == 0 -> "$h h"; else -> "$h h $m min" }
    }

    /** Minuten in Arbeitstagen zu soll Minuten, eine Nachkommastelle mit Komma ("1,6"). */
    fun tage(min: Int, soll: Int = Regeln.SOLL): String = String.format(Locale.GERMAN, "%.1f", min / soll.toDouble())
}
