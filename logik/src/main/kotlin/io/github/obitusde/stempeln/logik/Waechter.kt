// Stempeln-Logik: der Waechter. Entscheidet bei jedem Aufwachen (Wecker, Ortszone, Knopf, neuer Stand),
// ob eine Erinnerung faellig ist und wann er das naechste Mal aufwachen muss. Kein Dauerlauf, kein Standort-Abfragen:
// der Standort kommt nur ueber Android-Ortszonen und nur morgens bis zum ersten Einstempeln.
package io.github.obitusde.stempeln.logik

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Regeln {
    const val SOLL = 505            // 8 h 25 ohne Pause
    const val START_DI_FR = 435     // 07:15 Erinnerung an Home-Office-Tagen
    const val START_SONST = 465     // 07:45 Mo/Mi/Do
    const val ORTBIS = 660          // 11:00 danach keine Ortszonen mehr
    const val MITTAG = 705          // 11:45 Mittagspause-Erinnerung
    const val MITTAG_ENDE = 870     // 14:30
    const val ABEND = 1170          // 19:30 danach keine Erinnerungen mehr
    const val PAUSE_SS = 45         // Pause vorbei nach 45 min bei SUR SITE
    const val PAUSE_KURZ = 30       // sonst 30 min
    const val WDH = 15              // gleiche Erinnerung alle 15 min
    val ZONE: ZoneId = ZoneId.of("Europe/Zurich")
}

/** Waadtlaender Feiertage. */
object Feiertage {
    fun ostern(j: Int): LocalDate {
        val a = j % 19; val b = j / 100; val c = j % 100; val d = b / 4; val e = b % 4
        val f = (b + 8) / 25; val g = (b - f + 1) / 3; val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
        val monat = (h + l - 7 * m + 114) / 31; val tag = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(j, monat, tag)
    }
    fun liste(j: Int): Set<LocalDate> {
        val o = ostern(j)
        var s = LocalDate.of(j, 9, 1); while (s.dayOfWeek != DayOfWeek.SUNDAY) s = s.plusDays(1)
        val bettagMontag = s.plusDays(15)   // Montag nach dem 3. Sonntag im September
        return setOf(LocalDate.of(j, 1, 1), LocalDate.of(j, 1, 2), o.minusDays(2), o.plusDays(1), o.plusDays(39), o.plusDays(50),
            LocalDate.of(j, 8, 1), bettagMontag, LocalDate.of(j, 12, 25))
    }
    fun ist(d: LocalDate) = d in liste(d.year)
}

data class Urlaub(val von: LocalDate, val bis: LocalDate) { fun umfasst(d: LocalDate) = !d.isBefore(von) && !d.isAfter(bis) }

/** Zuletzt gezeigte Erinnerung (gegen Wiederholung). */
data class Letzte(val ziel: String, val label: String, val grund: String, val zeit: Long, val datum: LocalDate)

data class Lage(
    val jetzt: LocalDateTime,
    val epoch: Long,
    val zustand: Zustand,
    val urlaub: Urlaub? = null,
    val spaeterBis: Long? = null,
    val heuteNicht: LocalDate? = null,
    val zone: String? = null,          // Typ der Ortszone, in der du gerade bist (HO/SS/HUB) oder null
    val letzte: Letzte? = null,
    val nachfrage: LocalDate? = null,  // an diesem Tag schon nachgefragt
)

data class Erinnerung(val ziel: String, val grund: String, val text: String, val label: String, val heim: Boolean, val wdh: Int = Regeln.WDH) {
    val anzeige get() = "$text → $label"
}

data class Entscheidung(
    val erinnerung: Erinnerung?,
    val unterdrueckt: Boolean,          // gleiche Erinnerung laeuft schon (nicht neu zeigen, aber auch nicht wegnehmen)
    val wecker: LocalDateTime?,         // naechstes Aufwachen
    val ortszonen: Boolean,             // Ortszonen jetzt anmelden?
    val grund: String,                  // fuer die Anzeige: warum / was als naechstes
)

object Waechter {
    fun istArbeitstag(d: LocalDate, urlaub: Urlaub?): Boolean =
        d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY && !Feiertage.ist(d) && urlaub?.umfasst(d) != true

    fun startMinute(d: LocalDate) = if (d.dayOfWeek == DayOfWeek.TUESDAY || d.dayOfWeek == DayOfWeek.FRIDAY) Regeln.START_DI_FR else Regeln.START_SONST

    /** Beginn des naechsten Arbeitstags ab (einschliesslich) d. */
    fun naechsterStart(ab: LocalDate, urlaub: Urlaub?): LocalDateTime {
        var d = ab
        repeat(400) { if (istArbeitstag(d, urlaub)) return zeit(d, startMinute(d)); d = d.plusDays(1) }
        return zeit(d, startMinute(d))
    }

    fun zeit(d: LocalDate, minute: Int): LocalDateTime = d.atStartOfDay().plusMinutes(minute.toLong())

    /** Live-Arbeitszeit heute in Minuten. */
    fun praesenz(l: Lage): Int {
        val z = heute(l)
        return if (z.status == "IN" && z.lese > 0) z.pres + ((l.epoch - z.lese) / 60).toInt().coerceAtLeast(0) else z.pres
    }

    fun heute(l: Lage): Zustand = if (l.zustand.datum == l.jetzt.toLocalDate()) l.zustand else Zustand.LEER

    fun entscheide(l: Lage): Entscheidung {
        val d = l.jetzt.toLocalDate()
        val now = l.jetzt.hour * 60 + l.jetzt.minute
        val morgen = naechsterStart(d.plusDays(1), l.urlaub)
        fun ruhe(g: String) = Entscheidung(null, false, morgen, false, g)
        if (!istArbeitstag(d, l.urlaub)) return ruhe(if (l.urlaub?.umfasst(d) == true) "Urlaub" else "kein Arbeitstag")
        val z = heute(l)
        val p = praesenz(l)
        if (z.status != "IN" && p >= Regeln.SOLL) return ruhe("Feierabend (${hm(p)})")
        if (now >= Regeln.ABEND) return ruhe("nach 19:30")
        val ab = startMinute(d)
        val ortszonenFenster = z.status != "IN" && z.buch == 0 && now >= ab && now < Regeln.ORTBIS
        if (z.status != "IN" && z.buch == 0 && now < ab) return Entscheidung(null, false, zeit(d, ab), false, "Erinnerungen ab ${hhmm(ab)}")
        if (l.spaeterBis != null && l.epoch < l.spaeterBis) {
            val bis = LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(l.spaeterBis), Regeln.ZONE)
            return Entscheidung(null, true, bis, ortszonenFenster, "Später bis ${hhmm(bis.hour * 60 + bis.minute)}")
        }
        val kand = mutableListOf<Int>()   // Minuten des Tages fuer das naechste Aufwachen
        var e: Erinnerung? = null
        var grund = ""
        val label = { ziel: String -> knopf(if (z.ort.isNotEmpty()) z.ort else if (ziel == "EIN") l.zone else null) }
        if (z.status == "IN") {
            val letzte = minutenOderNull(z.letzte) ?: 0
            if (p >= Regeln.SOLL) {
                val ue = p - Regeln.SOLL
                e = Erinnerung("AUS", "8 h 25 voll", "8 h 25 voll seit ${hhmm(now - ue)}" + (if (ue >= 5) " (+$ue min)" else "") + " - ausstempeln?", label("AUS"), false)
            } else {
                kand += now + (Regeln.SOLL - p)
                grund = "8 h 25 voll um ${hhmm(now + Regeln.SOLL - p)}"
                if (letzte < 660) {
                    if (now >= Regeln.MITTAG && now < Regeln.MITTAG_ENDE)
                        e = Erinnerung("AUS", "Mittagspause", "Mittagspause (heute ${hm(p)}) - ausstempeln?", label("AUS"), false)
                    else if (now < Regeln.MITTAG) { kand += Regeln.MITTAG; grund = "Mittagspause-Erinnerung um 11:45" }
                }
            }
        } else if (z.buch == 0) {
            if (!ortszonenFenster) return ruhe("nach 11:00 ohne Einstempeln - keine Erinnerung mehr")
            val da = l.zone?.takeUnless { it == "HO" && l.heuteNicht == d }
            e = when (da) {
                "SS", "HUB" -> Erinnerung("EIN", "Ankunft", "Angekommen - einstempeln?", label("EIN"), false)
                "HO" -> Erinnerung("EIN", "Home Office",
                    if (startMinute(d) == Regeln.START_DI_FR) "Zu Hause - einstempeln?" else "Noch zu Hause - heute Home Office? Einstempeln?", label("EIN"), true)
                else -> null
            }
            kand += Regeln.ORTBIS
            grund = if (da == null) "wartet auf Ankunft (Ortszonen bis 11:00)" else ""
        } else {
            val lz = minutenOderNull(z.letzte) ?: -1
            if (lz in 660..870) {
                val pz = if (z.ort == "SS") Regeln.PAUSE_SS else Regeln.PAUSE_KURZ
                when {
                    now < lz + pz -> { kand += lz + pz; grund = "Pause vorbei um ${hhmm(lz + pz)}" }
                    now < lz + 60 -> { e = Erinnerung("EIN", "Pause vorbei", "Pause seit ${now - lz} min (heute ${hm(p)}) - wieder einstempeln?", label("EIN"), false); kand += lz + 60 }
                    l.nachfrage != d -> e = Erinnerung("EIN", "Nachfrage",
                        "Seit ${hhmm(lz)} ausgestempelt (${now - lz} min) - schon wieder eingestempelt? Sonst jetzt einstempeln?", label("EIN"), false, 9999)
                    else -> return ruhe("ausgestempelt seit ${hhmm(lz)}")
                }
            } else return ruhe("ausgestempelt" + (if (lz >= 0) " seit ${hhmm(lz)}" else ""))
        }
        var unterdrueckt = false
        if (e != null) {
            val lt = l.letzte
            if (lt != null && lt.datum == d && lt.ziel == e.ziel && lt.label == e.label && lt.grund == e.grund &&
                l.epoch - lt.zeit < e.wdh.toLong() * 60 - 120) {
                unterdrueckt = true
                if (e.wdh < 9999) kand += now + ((lt.zeit + e.wdh * 60 - l.epoch + 59) / 60).toInt().coerceAtLeast(1)
                e = null
            } else if (e.wdh < 9999) kand += now + e.wdh
            grund = if (unterdrueckt) "Erinnerung läuft" else "Erinnerung"
        }
        val w = kand.filter { it > now }.minOrNull()?.let { if (it >= Regeln.ABEND) null else zeit(d, it) } ?: morgen
        return Entscheidung(e, unterdrueckt, w, ortszonenFenster, grund.ifEmpty { "nächste Prüfung ${hhmm(w.hour * 60 + w.minute)}" })
    }
}
