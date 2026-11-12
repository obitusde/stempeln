package io.github.obitusde.stempeln.logik

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.*

class WaechterTest {
    private fun t(d: String, hm: String) = LocalDateTime.parse("${d}T$hm")
    private fun ep(x: LocalDateTime) = x.atZone(Regeln.ZONE).toEpochSecond()
    private fun lage(d: String, hm: String, z: Zustand = Zustand.LEER, zone: String? = null, letzte: Letzte? = null, urlaub: Urlaub? = null,
                     heuteNicht: LocalDate? = null, nachfrage: LocalDate? = null, spaeter: Long? = null): Lage {
        val j = t(d, hm); return Lage(j, ep(j), z, urlaub, spaeter, heuteNicht, zone, letzte, nachfrage)
    }
    private fun zin(d: String, pres: Int, lese: String, letzte: String, ort: String, buch: Int = 1) =
        Zustand(LocalDate.parse(d), "IN", pres, ep(t(d, lese)), letzte, buch, ort, "")
    private fun zout(d: String, pres: Int, letzte: String, ort: String, buch: Int = 2) =
        Zustand(LocalDate.parse(d), "OUT", pres, 1, letzte, buch, ort, "")

    @Test fun feiertage() {
        assertTrue(Feiertage.ist(LocalDate.of(2026, 4, 3)))   // Karfreitag 2026
        assertTrue(Feiertage.ist(LocalDate.of(2026, 9, 21)))  // Bettagsmontag 2026
        assertTrue(Feiertage.ist(LocalDate.of(2026, 12, 25)))
        assertFalse(Feiertage.ist(LocalDate.of(2026, 11, 11)))
    }

    @Test fun wochenende_ruhe_bis_montag() {
        val e = Waechter.entscheide(lage("2026-11-14", "09:00"))   // Samstag
        assertNull(e.erinnerung); assertFalse(e.ortszonen)
        assertEquals(t("2026-11-16", "07:45"), e.wecker)            // Montag 07:45
    }

    @Test fun urlaub_ruht() {
        val u = Urlaub(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-20"))
        val e = Waechter.entscheide(lage("2026-11-13", "17:00", zout("2026-11-13", 510, "17:00", "HO"), urlaub = u))
        assertEquals(t("2026-11-23", "07:45"), e.wecker)            // Urlaub Mo-Fr -> naechster Montag 23.11.
    }

    @Test fun morgen_vor_start_und_ortszonen() {
        val a = Waechter.entscheide(lage("2026-11-11", "07:00"))    // Mittwoch
        assertEquals(t("2026-11-11", "07:45"), a.wecker); assertFalse(a.ortszonen)
        val b = Waechter.entscheide(lage("2026-11-11", "07:45"))    // unterwegs
        assertNull(b.erinnerung); assertTrue(b.ortszonen); assertEquals(t("2026-11-11", "11:00"), b.wecker)
        val c = Waechter.entscheide(lage("2026-11-11", "07:45", zone = "HO"))
        assertEquals("Noch zu Hause - heute Home Office? Einstempeln? → HOME OFFICE", c.erinnerung!!.anzeige); assertTrue(c.erinnerung!!.heim)
        assertEquals(t("2026-11-11", "08:00"), c.wecker)
        val d = Waechter.entscheide(lage("2026-11-10", "07:15", zone = "HO"))   // Dienstag
        assertEquals("Zu Hause - einstempeln? → HOME OFFICE", d.erinnerung!!.anzeige)
        val f = Waechter.entscheide(lage("2026-11-11", "08:35", zone = "SS"))
        assertEquals("Angekommen - einstempeln? → SUR SITE", f.erinnerung!!.anzeige)
        val g = Waechter.entscheide(lage("2026-11-11", "07:50", zone = "HO", heuteNicht = LocalDate.parse("2026-11-11")))
        assertNull(g.erinnerung)
        val h = Waechter.entscheide(lage("2026-11-11", "11:00"))
        assertFalse(h.ortszonen); assertEquals(t("2026-11-12", "07:45"), h.wecker)
    }

    @Test fun wiederholung_alle_15_min() {
        val le = Letzte("EIN", "HOME OFFICE", "Home Office", ep(t("2026-11-11", "07:45")), LocalDate.parse("2026-11-11"))
        val a = Waechter.entscheide(lage("2026-11-11", "07:50", zone = "HO", letzte = le))
        assertNull(a.erinnerung); assertTrue(a.unterdrueckt); assertEquals(t("2026-11-11", "08:00"), a.wecker)
        val b = Waechter.entscheide(lage("2026-11-11", "08:00", zone = "HO", letzte = le))
        assertNotNull(b.erinnerung)
    }

    @Test fun eingestempelt_mittag_und_voll() {
        val z = zin("2026-11-11", 0, "08:00", "08:00", "SS")
        val a = Waechter.entscheide(lage("2026-11-11", "10:00", z))
        assertNull(a.erinnerung); assertFalse(a.ortszonen); assertEquals(t("2026-11-11", "11:45"), a.wecker)
        val b = Waechter.entscheide(lage("2026-11-11", "11:45", z))
        assertEquals("Mittagspause (heute 3:45) - ausstempeln? → SUR SITE", b.erinnerung!!.anzeige)
        val z2 = zin("2026-11-11", 240, "13:00", "13:00", "HO", 3)
        val c = Waechter.entscheide(lage("2026-11-11", "13:00", z2))
        assertNull(c.erinnerung); assertEquals(t("2026-11-11", "17:25"), c.wecker)
        val d = Waechter.entscheide(lage("2026-11-11", "17:30", z2))
        assertEquals("Soll (8:25) voll seit 17:25 (+5 min) - ausstempeln? → HOME OFFICE", d.erinnerung!!.anzeige)
    }

    @Test fun pause_30_45_und_nachfrage() {
        val ss = zout("2026-11-11", 240, "12:00", "SS")
        assertEquals(t("2026-11-11", "12:45"), Waechter.entscheide(lage("2026-11-11", "12:01", ss)).wecker)
        assertEquals("Pause seit 45 min (heute 4:00) - wieder einstempeln? → SUR SITE",
            Waechter.entscheide(lage("2026-11-11", "12:45", ss)).erinnerung!!.anzeige)
        val hub = zout("2026-11-11", 240, "12:00", "HUB")
        assertEquals(t("2026-11-11", "12:30"), Waechter.entscheide(lage("2026-11-11", "12:01", hub)).wecker)
        val n = Waechter.entscheide(lage("2026-11-11", "13:00", ss))
        assertEquals("Nachfrage", n.erinnerung!!.grund); assertEquals(t("2026-11-12", "07:45"), n.wecker)
        val n2 = Waechter.entscheide(lage("2026-11-11", "13:05", ss, nachfrage = LocalDate.parse("2026-11-11")))
        assertNull(n2.erinnerung); assertEquals(t("2026-11-12", "07:45"), n2.wecker)
    }

    @Test fun feierabend_ruht() {
        val z = zout("2026-11-11", 510, "17:20", "SS", 4)
        val e = Waechter.entscheide(lage("2026-11-11", "17:21", z))
        assertNull(e.erinnerung); assertEquals(t("2026-11-12", "07:45"), e.wecker)
    }

    @Test fun dateien() {
        val z = Zustand.aus("Z_DATUM=2026-11-11\nZ_STATUS=IN\nZ_PRES=380\nZ_LESE=1794402167\nZ_LETZTE=12:58\nZ_BUCH=3\nZ_ORT=SS\nZ_NAME='Le Brassus (Rte)'\n")
        assertEquals("Le Brassus (Rte)", z.name); assertEquals(380, z.pres)
        assertEquals(-90, minutenOderNull("-1:30")); assertEquals("-1:30", hm(-90))
        val o = leseOrte("HO;46.1;6.2;Zuhause;300|SS;46.3;6.4;Le Chenit")
        assertEquals(2, o.size); assertEquals(800, o[1].radius)
        val p = lesePaket("#STEMPELPAKET 4.5\n#DATEI kopf.sh\na\n#DATEI haupt.sh\nb\n#DATEI ende.sh\nc\n#DATEI start.sh\nd\n#DATEI VERSION\n4.5\n#ENDE 4.5\n")
        assertEquals("4.5", p!!.version); assertEquals("4.5\n", p.dateien["VERSION"])
        assertNull(lesePaket("#STEMPELPAKET 4.5\n#DATEI kopf.sh\na\n"))
    }
}
