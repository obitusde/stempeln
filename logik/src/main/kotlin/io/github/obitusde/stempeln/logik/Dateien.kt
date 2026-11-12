// Stempeln-Logik: Dateiformate im Handy-Ordner Download/stempeln (geschrieben von den Shell-Skripten)
package io.github.obitusde.stempeln.logik

import java.time.LocalDate

/** "SCHLUESSEL=wert" je Zeile, Werte evtl. in '...' (so schreiben es die Skripte). */
fun leseKv(text: String?): Map<String, String> {
    if (text == null) return emptyMap()
    val m = LinkedHashMap<String, String>()
    for (zeile in text.lines()) {
        val i = zeile.indexOf('=')
        if (i <= 0) continue
        var w = zeile.substring(i + 1).trim()
        if (w.length >= 2 && w.startsWith("'") && w.endsWith("'")) w = w.substring(1, w.length - 1)
        m[zeile.substring(0, i).trim()] = w
    }
    return m
}

fun datumOderNull(s: String?): LocalDate? = try { if (s.isNullOrBlank()) null else LocalDate.parse(s.trim()) } catch (e: Exception) { null }

/** "7:02" / "07:02" / "-1:30" -> Minuten (oder null). */
fun minutenOderNull(s: String?): Int? {
    if (s.isNullOrBlank()) return null
    val t = s.trim(); val neg = t.startsWith("-")
    val teile = t.removePrefix("-").split(":")
    if (teile.size != 2) return null
    val h = teile[0].toIntOrNull() ?: return null
    val m = teile[1].toIntOrNull() ?: return null
    return (h * 60 + m) * (if (neg) -1 else 1)
}

fun hm(min: Int): String { val n = if (min < 0) "-" else ""; val a = Math.abs(min); return n + (a / 60) + ":" + (a % 60).toString().padStart(2, '0') }
fun hhmm(min: Int): String { val a = ((min % 1440) + 1440) % 1440; return (a / 60).toString().padStart(2, '0') + ":" + (a % 60).toString().padStart(2, '0') }

/** zustand.txt: gemerkter Stand vom letzten Lesen der Stempelseite. */
data class Zustand(
    val datum: LocalDate?, val status: String, val pres: Int, val lese: Long,
    val letzte: String, val buch: Int, val ort: String, val name: String,
    val pause: Int = -1,   // Mittagspause heute schon gemacht (min), -1 = unbekannt (alte Skripte)
) {
    companion object {
        val LEER = Zustand(null, "OUT", 0, 0, "", 0, "", "", 0)
        fun aus(text: String?): Zustand {
            val m = leseKv(text)
            if (m.isEmpty()) return LEER
            return Zustand(
                datumOderNull(m["Z_DATUM"]), m["Z_STATUS"] ?: "OUT", m["Z_PRES"]?.toIntOrNull() ?: 0,
                m["Z_LESE"]?.toLongOrNull() ?: 0, m["Z_LETZTE"] ?: "", m["Z_BUCH"]?.toIntOrNull() ?: 0,
                m["Z_ORT"] ?: "", m["Z_NAME"] ?: "", m["Z_PAUSE"]?.toIntOrNull() ?: -1,
            )
        }
    }
}

/** konto.txt: Werte der Stempelseite (Saldo usw.), Text wie auf der Seite ("12:41"). */
data class Konto(val datum: LocalDate?, val lese: Long, val solde: String, val jour: String, val ratt: String, val hs: String, val pres: Int?) {
    companion object {
        fun aus(text: String?): Konto? {
            val m = leseKv(text)
            if (m.isEmpty()) return null
            return Konto(datumOderNull(m["K_DATUM"]), m["K_LESE"]?.toLongOrNull() ?: 0, m["K_SOLDE"] ?: "", m["K_JOUR"] ?: "",
                m["K_RATT"] ?: "", m["K_HS"] ?: "", m["K_PRES"]?.toIntOrNull())
        }
    }
}

/** orte.txt: "TYP;lat;lon;Name;Radius|..." (Radius fehlt -> 800 m). */
data class Ort(val typ: String, val lat: Double, val lon: Double, val name: String, val radius: Int)

fun leseOrte(text: String?): List<Ort> {
    if (text.isNullOrBlank()) return emptyList()
    return text.split('|', '\n').mapNotNull { e ->
        val f = e.trim().split(';')
        if (f.size < 4) return@mapNotNull null
        val lat = f[1].toDoubleOrNull() ?: return@mapNotNull null
        val lon = f[2].toDoubleOrNull() ?: return@mapNotNull null
        Ort(f[0].trim(), lat, lon, f[3].trim(), f.getOrNull(4)?.trim()?.toIntOrNull() ?: 800)
    }
}

fun knopf(typ: String?): String = when (typ) {
    "HO" -> "HOME OFFICE"; "SS" -> "SUR SITE"; "HUB" -> "HUB"; else -> "unbekannter Ort, du tippst selbst"
}

/** Inhalt eines Skript-Pakets (#STEMPELPAKET v / #DATEI name / ... / #ENDE v) -> Version + Dateien, null wenn unvollstaendig. */
data class Paket(val version: String, val dateien: Map<String, String>)

fun lesePaket(text: String): Paket? {
    var v: String? = null; var ok = false; var name: String? = null
    val d = LinkedHashMap<String, StringBuilder>()
    for (z in text.lines()) {
        when {
            z.startsWith("#STEMPELPAKET ") -> v = z.removePrefix("#STEMPELPAKET ").trim()
            z.startsWith("#DATEI ") -> { name = z.removePrefix("#DATEI ").trim(); d[name!!] = StringBuilder() }
            z.startsWith("#ENDE ") -> { if (v != null && z.removePrefix("#ENDE ").trim() == v) ok = true; name = null }
            name != null -> d[name!!]!!.append(z).append('\n')
        }
    }
    if (!ok || v == null) return null
    for (pflicht in listOf("kopf.sh", "haupt.sh", "ende.sh", "start.sh")) if (d[pflicht].isNullOrBlank()) return null
    return Paket(v!!, d.mapValues { it.value.toString() })
}
