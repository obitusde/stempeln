// Stempeln-Logik: was die App ueber den Tag anzeigt (Soll, bereits, fehlt, voraussichtlich Schluss, Ueberstunden) - v0.2.0
package io.github.obitusde.stempeln.logik

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

object Anzeige {
    fun pause(ort: String?) = if (ort == "SS") Regeln.PAUSE_SS else Regeln.PAUSE_KURZ

    fun tag(l: Lage): Tag {
        val z = Waechter.heute(l)
        val p = Waechter.praesenz(l)
        val now = l.jetzt.hour * 60 + l.jetzt.minute
        val rest = Regeln.SOLL - p
        if (rest <= 0) return Tag(Regeln.SOLL, p, 0, -rest, null,
            if (z.status == "IN") "Soll erreicht seit ${hhmm(now + rest)} - du kannst ausstempeln" else "Soll erreicht - Feierabend")
        val lz = minutenOderNull(z.letzte)
        return when {
            z.buch == 0 -> {
                val ort = l.zone ?: if (Waechter.startMinute(l.jetzt.toLocalDate()) == Regeln.START_DI_FR) "HO" else "SS"
                val pz = pause(ort)
                Tag(Regeln.SOLL, p, rest, 0, now + rest + pz, "wenn du jetzt einstempelst, mit $pz min Mittagspause")
            }
            z.status == "IN" -> {
                val pz = pause(z.ort)
                if (z.buch <= 1 && now < Regeln.MITTAG_ENDE) Tag(Regeln.SOLL, p, rest, 0, now + rest + pz, "mit $pz min Mittagspause")
                else Tag(Regeln.SOLL, p, rest, 0, now + rest, "wenn du durcharbeitest")
            }
            lz != null && lz in Regeln.ORTBIS..Regeln.MITTAG_ENDE -> {
                val wieder = maxOf(now, lz + pause(z.ort))
                Tag(Regeln.SOLL, p, rest, 0, wieder + rest, "wenn du um ${hhmm(wieder)} wieder einstempelst")
            }
            else -> Tag(Regeln.SOLL, p, rest, 0, now + rest, "wenn du jetzt wieder einstempelst")
        }
    }

    /** "4 h 38 min", "38 min", "8 h", "-1 h 30 min" */
    fun lang(min: Int): String {
        val n = if (min < 0) "-" else ""; val a = Math.abs(min); val h = a / 60; val m = a % 60
        return n + when { h == 0 -> "$m min"; m == 0 -> "$h h"; else -> "$h h $m min" }
    }

    /** Minuten in Arbeitstagen zu 8 h 25, eine Nachkommastelle mit Komma ("1,6"). */
    fun tage(min: Int): String = String.format(Locale.GERMAN, "%.1f", min / Regeln.SOLL.toDouble())
}
