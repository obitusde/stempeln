// Stempeln-App: Skripte (paket.txt) und App selbst von GitHub aktualisieren - nur auf Knopfdruck
package io.github.obitusde.stempeln

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.obitusde.stempeln.logik.lesePaket
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Aktualisieren {
    const val PAKET = "https://raw.githubusercontent.com/obitusde/stempeln/main/paket.txt"
    const val RELEASE = "https://api.github.com/repos/obitusde/stempeln/releases/latest"

    private fun hole(url: String): ByteArray {
        val v = URL(url).openConnection() as HttpURLConnection
        v.connectTimeout = 15000; v.readTimeout = 60000; v.instanceFollowRedirects = true
        v.setRequestProperty("User-Agent", "stempeln-app"); v.setRequestProperty("Cache-Control", "no-cache")
        try {
            if (v.responseCode != 200) throw Exception("HTTP ${v.responseCode}")
            return v.inputStream.use { it.readBytes() }
        } finally { v.disconnect() }
    }

    /** Skripte nach Download/stempeln/skripte (alte nach skripte-alt). Laeuft im Hintergrund-Thread. */
    fun skripte(): String {
        val alt = Speicher.skriptVersion()
        val p = lesePaket(String(hole(PAKET + "?t=" + System.currentTimeMillis()), Charsets.UTF_8))
            ?: return "Paket von GitHub unvollstaendig - nichts geaendert" + (alt?.let { " (es bleibt v$it)" } ?: "")
        if (p.version == alt) return "Skripte v${p.version} waren schon aktuell"
        val o = Speicher.ordner; o.mkdirs()
        val neu = File(o, "skripte-neu"); neu.deleteRecursively(); neu.mkdirs()
        for ((name, inhalt) in p.dateien) File(neu, name).writeText(inhalt)
        val akt = File(o, "skripte"); val sich = File(o, "skripte-alt")
        sich.deleteRecursively()
        if (akt.exists() && !akt.renameTo(sich)) return "Konnte alte Skripte nicht ersetzen"
        if (!neu.renameTo(akt)) { sich.renameTo(akt); return "Konnte neue Skripte nicht einsetzen" }
        return "Skripte v${p.version} installiert" + (alt?.let { " (vorher v$it)" } ?: "")
    }

    data class Neu(val version: String, val url: String)

    /** Neueste App-Version auf GitHub (Release mit .apk). */
    fun neueApp(): Neu? {
        val json = String(hole(RELEASE), Charsets.UTF_8)
        val tag = Regex("\"tag_name\"\\s*:\\s*\"app-v([^\"]+)\"").find(json)?.groupValues?.get(1) ?: return null
        val url = Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"").find(json)?.groupValues?.get(1) ?: return null
        return Neu(tag, url)
    }

    fun istNeuer(neu: String, alt: String): Boolean {
        val a = neu.split('.').map { it.toIntOrNull() ?: 0 }; val b = alt.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) { val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }; if (x != y) return x > y }
        return false
    }

    /** APK laden und Android-Installation starten (du bestaetigst). */
    fun appInstallieren(c: Context, n: Neu) {
        val d = File(c.cacheDir, "update"); d.mkdirs()
        val f = File(d, "stempeln.apk"); f.writeBytes(hole(n.url))
        val uri = FileProvider.getUriForFile(c, c.packageName + ".dateien", f)
        c.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
