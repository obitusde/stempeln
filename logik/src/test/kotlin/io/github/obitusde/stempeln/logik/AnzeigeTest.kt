package io.github.obitusde.stempeln.logik

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.*

class AnzeigeTest {
    private fun t(d: String, hm: String) = LocalDateTime.parse("${d}T$hm")
    private fun ep(x: LocalDateTime) = x.atZone(Regeln.ZONE).toEpochSecond()
    private fun lage(d: String, hm: String, z: Zustand = Zustand.LEER, zone: String? = null): Lage {
        val j = t(d, hm); return Lage(j, ep(j), z, zone = zone)
    }

    @Test fun formate() {
        assertEquals("4 h 38 min", Anzeige.lang(278))
        assertEquals("38 min", Anzeige.lang(38))
        assertEquals("8 h", Anzeige.lang(480))
        assertEquals("-1 h 30 min", Anzeige.lang(-90))
        assertEquals("1,6", Anzeige.tage(805))       // 13:25 Ueberstunden
        assertEquals("-0,2", Anzeige.tage(-90))
    }

    @Test fun ausgestempelt_mittag_hub() {
        // Screenshot 12.11.: seit 11:35 aus (HUB), heute 3:47 -> Pause bis 12:05, Schluss 12:05 + 4:38 = 16:43
        val z = Zustand(LocalDate.parse("2026-11-12"), "OUT", 227, 1, "11:35", 2, "HUB", "")
        val g = Anzeige.tag(lage("2026-11-12", "11:39", z))
        assertEquals(227, g.bereits); assertEquals(278, g.fehlt)
        assertEquals(hhmmZuMin("16:43"), g.schluss)
        assertTrue(g.hinweis.contains("12:05"))
    }

    @Test fun eingestempelt_ohne_pause_sur_site() {
        val z = Zustand(LocalDate.parse("2026-11-11"), "IN", 60, ep(t("2026-11-11", "09:00")), "08:00", 1, "SS", "")
        val g = Anzeige.tag(lage("2026-11-11", "09:00", z))
        assertEquals(445, g.fehlt)
        assertEquals(540 + 445 + 45, g.schluss)        // 17:10
    }

    @Test fun eingestempelt_nach_pause() {
        val z = Zustand(LocalDate.parse("2026-11-11"), "IN", 300, ep(t("2026-11-11", "14:00")), "12:30", 3, "HO", "")
        val g = Anzeige.tag(lage("2026-11-11", "14:00", z))
        assertEquals(840 + 205, g.schluss)              // 17:25
    }

    @Test fun soll_erreicht() {
        val z = Zustand(LocalDate.parse("2026-11-11"), "IN", 500, ep(t("2026-11-11", "17:00")), "12:30", 3, "HO", "")
        val g = Anzeige.tag(lage("2026-11-11", "17:10", z))   // 510 min
        assertNull(g.schluss); assertEquals(5, g.mehr); assertEquals(0, g.fehlt)
        assertTrue(g.hinweis.contains("17:05"))
    }

    @Test fun noch_nicht_gestempelt() {
        val g = Anzeige.tag(lage("2026-11-13", "07:30"))         // Freitag -> Home Office, 30 min Pause
        assertEquals(450 + 505 + 30, g.schluss)
    }

    @Test fun arbeitstage_bis_ende() {
        assertEquals(395, Anzeige.arbeitstage(LocalDate.parse("2026-11-12"), LocalDate.parse("2028-05-31")))
        assertEquals(1, Anzeige.arbeitstage(LocalDate.parse("2028-05-31"), LocalDate.parse("2028-05-31")))
        assertEquals(0, Anzeige.arbeitstage(LocalDate.parse("2028-06-01"), LocalDate.parse("2028-05-31")))
    }

    @Test fun pause_anzeige() {
        val aus = Zustand(LocalDate.parse("2026-11-12"), "OUT", 227, 1, "11:35", 2, "HUB", "", 0)
        assertEquals("Pause läuft seit 4 min · geplant 30 min, vorbei um 12:05", Anzeige.pause(lage("2026-11-12", "11:39", aus)))
        val drin = Zustand(LocalDate.parse("2026-11-12"), "IN", 300, ep(t("2026-11-12", "14:00")), "12:10", 3, "HUB", "", 35)
        assertEquals("Pause gemacht: 35 min", Anzeige.pause(lage("2026-11-12", "14:00", drin)))
        val morgen = Zustand(LocalDate.parse("2026-11-12"), "IN", 60, ep(t("2026-11-12", "09:00")), "08:00", 1, "SS", "Le Brassus", 0)
        assertEquals("Pause geplant: 45 min (Erinnerung um 11:45)", Anzeige.pause(lage("2026-11-12", "09:00", morgen)))
    }

    @Test fun naechstes_stempeln() {
        val drin = Zustand(LocalDate.parse("2026-11-12"), "IN", 60, ep(t("2026-11-12", "09:00")), "08:00", 1, "HUB", "", 0)
        assertEquals("Ausstempeln · HUB", Anzeige.naechstes(lage("2026-11-12", "09:00", drin)).text)
        val pause = Zustand(LocalDate.parse("2026-11-12"), "OUT", 227, 1, "11:35", 2, "SS", "Le Brassus", 0)
        assertEquals("Einstempeln · SUR SITE (Le Brassus)", Anzeige.naechstes(lage("2026-11-12", "12:00", pause)).text)
        assertEquals("Einstempeln · Ort per Standort", Anzeige.naechstes(lage("2026-11-13", "07:30")).text)
        assertEquals("Einstempeln · HOME OFFICE", Anzeige.naechstes(lage("2026-11-13", "07:30", zone = "HO")).text)
    }

    @Test fun einstellungen_datei() {
        val e = Einstellungen(soll = listOf(LocalDate.parse("2000-01-01") to 505, LocalDate.parse("2027-01-01") to 480))
        val f = Einstellungen.aus(e.text())
        assertEquals(e, f)
        assertEquals(505, f.sollAm(LocalDate.parse("2026-12-31")))
        assertEquals(480, f.sollAm(LocalDate.parse("2027-01-01")))
        assertTrue(e.text().contains("E_SOLL='2000-01-01=08:25,2027-01-01=08:00'"))
        assertEquals(Einstellungen(), Einstellungen.aus(null))
        // andere Arbeitszeit wirkt im Waechter: 8 h voll
        val z = Zustand(LocalDate.parse("2027-01-04"), "IN", 480, ep(t("2027-01-04", "17:00")), "12:30", 3, "HO", "", 30)
        val j = t("2027-01-04", "17:00")
        val w = Waechter.entscheide(Lage(j, ep(j), z, e = f))
        assertEquals("Soll voll", w.erinnerung!!.grund)
    }

    @Test fun anfang_pause_buchungen() {
        val z = Zustand.aus("Z_DATUM=2026-11-16\nZ_STATUS=IN\nZ_PRES=300\nZ_LESE=${ep(t("2026-11-16", "16:00"))}\nZ_LETZTE=12:39\nZ_BUCH=3\nZ_ORT=SS\nZ_PAUSE=36\nZ_ZEITEN='7:59 12:03 12:39'\n")
        val l = lage("2026-11-16", "16:00", z)
        assertEquals(479, Anzeige.angefangen(l))
        assertEquals("Pause gemacht" to "36 min (12:03–12:39)", Anzeige.pauseZeile(l))
        assertEquals("07:59 – 12:03 · 12:39 – jetzt", Anzeige.buchungen(l))
        val aus = Zustand(LocalDate.parse("2026-11-16"), "OUT", 240, 1, "12:03", 2, "SS", "", 0, listOf(479, 723))
        assertEquals("Pause läuft" to "seit 10 min, bis 12:48", Anzeige.pauseZeile(lage("2026-11-16", "12:13", aus)))
        assertEquals("07:59 – 12:03", Anzeige.buchungen(lage("2026-11-16", "12:13", aus)))
        val morgen = Zustand(LocalDate.parse("2026-11-16"), "IN", 60, ep(t("2026-11-16", "09:00")), "08:00", 1, "HO", "", 0, listOf(480))
        assertEquals("Pause geplant" to "30 min", Anzeige.pauseZeile(lage("2026-11-16", "09:00", morgen)))
        // alte Skripte ohne Z_ZEITEN: Anfang aus der einzigen Buchung
        val alt = Zustand(LocalDate.parse("2026-11-16"), "IN", 60, ep(t("2026-11-16", "09:00")), "08:00", 1, "HO", "")
        assertEquals(480, Anzeige.angefangen(lage("2026-11-16", "09:00", alt)))
    }

    @Test fun ort_in_der_app() {
        val orte = leseOrte("HO;46.5000;6.5000;Zuhause;300|SS;46.6000;6.2000;Buero;1200")
        val a = ortFuer(46.5010, 6.5000, 30, orte)!!          // ~111 m vom Zuhause
        assertEquals("HO", a.typ); assertEquals("Zuhause", a.name); assertTrue(a.abstand in 100..120); assertTrue(a.sicher)
        val b = ortFuer(46.4500, 6.5000, 30, orte)!!          // ~5,6 km weg
        assertNull(b.typ); assertEquals("Zuhause", b.name); assertFalse(b.sicher)
        assertFalse(ortFuer(46.5010, 6.5000, 900, orte)!!.sicher)   // zu ungenau
        assertNull(ortFuer(46.5, 6.5, 10, emptyList()))
    }

    private fun hhmmZuMin(s: String) = minutenOderNull(s)!!
}
