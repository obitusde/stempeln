// Stempeln-Logik: Einstellungen (Datei Download/stempeln/einstellungen.txt, App und Skripte lesen sie) - v0.3.0
// Format wie zustand.txt, in der Shell mit "." ladbar. Zeiten als HH:MM.
package io.github.obitusde.stempeln.logik

import java.time.DayOfWeek
import java.time.LocalDate

data class Einstellungen(
    /** Arbeitszeit pro Tag (Minuten) mit "gueltig ab", aufsteigend sortiert. */
    val soll: List<Pair<LocalDate, Int>> = listOf(LocalDate.of(2000, 1, 1) to Regeln.SOLL),
    val ende: LocalDate = LocalDate.of(2028, 5, 31),          // letzter Arbeitstag
    val pauseKurz: Int = Regeln.PAUSE_KURZ,                   // Pause zu Hause / HUB
    val pauseSS: Int = Regeln.PAUSE_SS,                       // Pause SUR SITE
    val hoTage: Set<DayOfWeek> = setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
    val startHO: Int = Regeln.START_DI_FR,                    // Einstempel-Erinnerung ab (Home-Office-Tage)
    val startBuero: Int = Regeln.START_SONST,                 // ... (Buerotage)
    val mittag: Int = Regeln.MITTAG,                          // Mittagspause-Erinnerung
    val wdh: Int = Regeln.WDH,                                // Erinnerung wiederholen alle ... min
    val abend: Int = Regeln.ABEND,                            // danach keine Erinnerungen mehr
) {
    fun sollAm(d: LocalDate): Int = soll.filter { !it.first.isAfter(d) }.maxByOrNull { it.first }?.second ?: soll.first().second
    fun start(d: LocalDate) = if (d.dayOfWeek in hoTage) startHO else startBuero
    fun istHoTag(d: LocalDate) = d.dayOfWeek in hoTage
    fun pause(ort: String?) = if (ort == "SS") pauseSS else pauseKurz

    fun text(): String = listOf(
        "# Stempeln - Einstellungen (in der App unter Einstellungen aendern). Zeiten HH:MM, Daten JJJJ-MM-TT.",
        "E_SOLL='" + soll.sortedBy { it.first }.joinToString(",") { "${it.first}=${hhmm(it.second)}" } + "'",
        "E_ENDE=$ende",
        "E_PAUSE_KURZ=$pauseKurz",
        "E_PAUSE_SS=$pauseSS",
        "E_HO_TAGE=" + hoTage.map { it.value }.sorted().joinToString(""),
        "E_START_HO=${hhmm(startHO)}",
        "E_START_BUERO=${hhmm(startBuero)}",
        "E_MITTAG=${hhmm(mittag)}",
        "E_WDH=$wdh",
        "E_ABEND=${hhmm(abend)}",
    ).joinToString("\n") + "\n"

    companion object {
        fun aus(text: String?): Einstellungen {
            val m = leseKv(text); val d = Einstellungen()
            if (m.isEmpty()) return d
            fun zeit(k: String, alt: Int) = minutenOderNull(m[k])?.takeIf { it in 0..1439 } ?: alt
            fun zahl(k: String, alt: Int, max: Int) = m[k]?.trim()?.toIntOrNull()?.takeIf { it in 1..max } ?: alt
            val soll = m["E_SOLL"]?.split(',')?.mapNotNull { e ->
                val t = e.split('='); if (t.size != 2) return@mapNotNull null
                val ab = datumOderNull(t[0]) ?: return@mapNotNull null
                val min = minutenOderNull(t[1])?.takeIf { it in 1..1439 } ?: return@mapNotNull null
                ab to min
            }?.sortedBy { it.first }?.takeIf { it.isNotEmpty() } ?: d.soll
            val ho = m["E_HO_TAGE"]?.mapNotNull { it.digitToIntOrNull()?.takeIf { n -> n in 1..7 }?.let { n -> DayOfWeek.of(n) } }?.toSet()
            return Einstellungen(soll, datumOderNull(m["E_ENDE"]) ?: d.ende, zahl("E_PAUSE_KURZ", d.pauseKurz, 240), zahl("E_PAUSE_SS", d.pauseSS, 240),
                if (m.containsKey("E_HO_TAGE")) ho ?: d.hoTage else d.hoTage, zeit("E_START_HO", d.startHO), zeit("E_START_BUERO", d.startBuero),
                zeit("E_MITTAG", d.mittag), zahl("E_WDH", d.wdh, 240), zeit("E_ABEND", d.abend))
        }
    }
}
