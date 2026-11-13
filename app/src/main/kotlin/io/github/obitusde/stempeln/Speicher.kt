// Stempeln-App: Dateien im Handy-Ordner Download/stempeln (gemeinsam mit den Shell-Skripten) und eigene Einstellungen
package io.github.obitusde.stempeln

import android.content.Context
import android.os.Environment
import io.github.obitusde.stempeln.logik.*
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

object Speicher {
    val ordner: File get() = File(Environment.getExternalStorageDirectory(), "Download/stempeln")
    fun datei(name: String) = File(ordner, name)
    fun lies(name: String): String? = try { datei(name).takeIf { it.isFile }?.readText() } catch (e: Exception) { null }
    fun schreib(name: String, text: String) = try { ordner.mkdirs(); datei(name).writeText(text); true } catch (e: Exception) { false }
    fun loesch(name: String) { try { datei(name).delete() } catch (_: Exception) {} }
    fun zugriff() = Environment.isExternalStorageManager()

    fun zustand() = Zustand.aus(lies("zustand.txt"))
    fun konto() = Konto.aus(lies("konto.txt"))
    fun orte() = leseOrte(lies("orte.txt"))
    fun skriptVersion() = lies("skripte/VERSION")?.trim()
    /** Version der MacroDroid-Huellen (schreibt start.sh beim Lauf, erst ab 5.0). */
    fun makroVersion() = lies("makro-version.txt")?.trim()?.ifEmpty { null }
    /** Einstellungen (gemeinsam mit den Skripten); fehlt die Datei, wird sie mit den heutigen Werten angelegt. */
    fun einstellungen(): Einstellungen {
        val t = lies("einstellungen.txt")
        if (t == null && zugriff()) schreib("einstellungen.txt", Einstellungen().text())
        return Einstellungen.aus(t)
    }
    fun setzeEinstellungen(e: Einstellungen) = schreib("einstellungen.txt", e.text())
    /** Name der Ortszone, in der du gerade bist (aus orte.txt), z. B. "Le Brassus". */
    fun zonenName(drin: Set<String>, typ: String?): String? {
        if (typ == null) return null
        val i = drin.firstOrNull { it.substringBefore('#') == typ }?.substringAfter('#')?.toIntOrNull() ?: return null
        return orte().getOrNull(i)?.name
    }

    // gemeinsame Dateien mit den Skripten (gleiche Formate wie bisher)
    fun spaeterBis(): Long? = lies("spaeter.txt")?.trim()?.toLongOrNull()
    fun setzeSpaeter(bis: Long) = schreib("spaeter.txt", "$bis\n")
    fun heuteNicht(): LocalDate? = datumOderNull(lies("heute-nicht.txt"))
    fun setzeHeuteNicht(d: LocalDate) = schreib("heute-nicht.txt", "$d\n")
    /** Erinnerung fuer haupt.sh (prueft damit, ob Richtung und Knopf noch passen). */
    fun schreibVorschlag(e: Erinnerung, epoch: Long, d: LocalDate) =
        schreib("vorschlag.txt", "V_ZIEL=${e.ziel}\nV_ZEIT=$epoch\nV_DATUM=$d\nV_LABEL='${e.label}'\nV_GRUND='${e.grund}'\n")

    // eigene Einstellungen der App
    private fun p(c: Context) = c.getSharedPreferences("stempeln", Context.MODE_PRIVATE)

    fun urlaub(c: Context): Urlaub? {
        val v = datumOderNull(p(c).getString("urlaub_von", null)); val b = datumOderNull(p(c).getString("urlaub_bis", null))
        return if (v != null && b != null && !b.isBefore(LocalDate.now(Regeln.ZONE))) Urlaub(v, b) else null
    }
    fun setzeUrlaub(c: Context, u: Urlaub?) = p(c).edit().putString("urlaub_von", u?.von?.toString()).putString("urlaub_bis", u?.bis?.toString()).apply()

    /** Geplantes Stempeln (nur fuer heute). */
    fun plan(c: Context): LocalDateTime? {
        val s = p(c).getString("plan", null) ?: return null
        val t = try { LocalDateTime.parse(s) } catch (e: Exception) { null } ?: return null
        return if (t.isAfter(LocalDateTime.now(Regeln.ZONE).minusMinutes(1))) t else null
    }
    fun setzePlan(c: Context, t: LocalDateTime?) = p(c).edit().putString("plan", t?.toString()).apply()

    fun letzte(c: Context): Letzte? {
        val s = p(c).getString("letzte", null) ?: return null
        val f = s.split('\u001f'); if (f.size < 5) return null
        return Letzte(f[0], f[1], f[2], f[3].toLongOrNull() ?: 0, datumOderNull(f[4]) ?: return null)
    }
    fun setzeLetzte(c: Context, l: Letzte) = p(c).edit().putString("letzte", listOf(l.ziel, l.label, l.grund, l.zeit.toString(), l.datum.toString()).joinToString("\u001f")).apply()

    fun nachfrage(c: Context) = datumOderNull(p(c).getString("nachfrage", null))
    fun setzeNachfrage(c: Context, d: LocalDate) = p(c).edit().putString("nachfrage", d.toString()).apply()

    /** Ortszonen, in denen du gerade bist (Kennungen "TYP#n"). */
    fun drin(c: Context): Set<String> = p(c).getStringSet("drin", emptySet())!!.toSet()
    fun setzeDrin(c: Context, s: Set<String>) = p(c).edit().putStringSet("drin", s).apply()
    fun zonenAktiv(c: Context) = p(c).getBoolean("zonen_aktiv", false)
    fun setzeZonenAktiv(c: Context, a: Boolean) = p(c).edit().putBoolean("zonen_aktiv", a).apply()

    fun naechster(c: Context): String = p(c).getString("naechster", "") ?: ""
    fun setzeNaechster(c: Context, s: String) = p(c).edit().putString("naechster", s).apply()
    fun shizukuWarnung(c: Context) = datumOderNull(p(c).getString("shizuku", null))
    fun setzeShizukuWarnung(c: Context, d: LocalDate) = p(c).edit().putString("shizuku", d.toString()).apply()
}
