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

    private fun hhmmZuMin(s: String) = minutenOderNull(s)!!
}
