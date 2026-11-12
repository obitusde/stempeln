# Stempeln

Halbautomatisches Stempeln auf der Horoquartz-Stempelseite (Pixel 8):

- **App** (`app/`, Logik in `logik/`): zeigt Stand, Arbeitszeit und Konto, Knöpfe Stempeln / Stand lesen / Planen / Urlaub,
  erinnert ans Stempeln (Wecker + Android-Ortszonen, kein Dauerlauf). Download: [Releases](https://github.com/obitusde/stempeln/releases/latest).
- **MacroDroid-Hüllen** (`makros/`): öffnen die Stempelseite, lesen sie und tippen per Shizuku.
- **Shell-Skripte** (`skripte/` → `paket.txt`, gebaut mit `werkzeug/paket-bauen.py`): die eigentliche Logik auf dem Handy
  (`Download/stempeln/skripte`), aktualisiert über die App.

Verlauf: `TAGEBUCH.md` (aus `obitusde/cupra-ziel`, `macrodroid/test/README.md`).
