// Stempeln-App: Fortschrittsbalken fuer die Arbeitszeit (v0.2.0)
package io.github.obitusde.stempeln

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class Balken(c: Context) : View(c) {
    var anteil = 0f; set(v) { field = v; invalidate() }
    var farbe = 0xFF1E5BD8.toInt(); set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val r = h / 2
        p.color = 0xFFE3E7EE.toInt(); cv.drawRoundRect(0f, 0f, w, h, r, r, p)
        val f = w * anteil.coerceIn(0f, 1f)
        if (f > 0) { p.color = farbe; cv.drawRoundRect(0f, 0f, maxOf(f, h), h, r, r, p) }
    }
}
