# Stempeln — Projektstand für Claude

**Stand 12.11.2026** · App (`app/`, `logik/`) v0.1.0 · Skripte (`skripte/`, `SKV` in `kopf.sh`) v4.5 · Makros (`makros/`) Stempeln v4.2 + Stand v4.2 · Workflow `.github/workflows/app.yml` v1.0.0

## 1. Nutzer und Arbeitsweise (unbedingt beachten)
- **Christof** (GitHub `obitusde`), Pixel 8 (Android 17), MacroDroid Pro + Shizuku. Will so wenig wie möglich selbst machen, kein Terminal.
- **Erst kurz high-level besprechen, dann bauen** (nach „los“/„ja“). Fragen als Text mit a/b/c. **Deutsch** überall. **Versionsnummer bei jeder Änderung.** Unsicherheit ehrlich benennen. Direkt auf `main`.
- Möglichst wenig Hintergrundaktivität: nichts darf laufen, wenn nichts zu tun ist (nachts, Wochenende, Urlaub).
- **Privat, nie ins Repo:** Orte/Koordinaten (`orte.txt` nur auf dem Handy; private Makro-Fassung mit Orten nur in Christofs Drive), Seiten-Dumps (Name + Login-Ticket), Screenshots/Logs nur im Chat, Signierschlüssel (nur GitHub-Secrets).
- **Makro-Dateien** gehören in Christofs Drive-Ordner `1EE216VJXRqmXnno7-ks51n-ufNmaBQBP` (Drive-Connector, `create_file` mit `parentId`, JSON mit Zeilenumbruch nach `},{` hochladen, Größe gegen lokale Datei prüfen). Gut abschreibbare IDs (`7441101200nn`).
- `m_description` jedes Makros aus Nutzersicht (wie läuft es, was muss er tun, was nicht).

## 2. Aufbau
```
App "Stempeln" (io.github.obitusde.stempeln)        MacroDroid (Huellen)            Shell-Skripte (Download/stempeln/skripte)
 Anzeige, Knoepfe, Planen, Urlaub                    Stempeln v4.2 / Stand v4.2      start, pruef, haupt, tipp, ende ...
 Waechter: Wecker + Ortszonen ──Befehl──────────────► NotificationTrigger            ◄─ Bildschirm lesen, Shizuku input tap
   (stille Meldung BEFEHL-STEMPELN / BEFEHL-STAND)    "BEFEHL-..." (Paket der App)    ende.sh: zustand.txt, konto.txt
 StandEmpfaenger ◄──────────── am broadcast -n io.github.obitusde.stempeln/.StandEmpfaenger (app_melden in kopf.sh)
```
- **Dateien** in `Download/stempeln/`: `zustand.txt` (Z_*), `konto.txt` (K_SOLDE/K_JOUR/K_RATT/K_HS), `orte.txt` (`TYP;lat;lon;Name;Radius|…`), `vorschlag.txt` (App schreibt bei Erinnerung, `haupt.sh` prüft), `spaeter.txt`, `heute-nicht.txt`, `skripte/`, `lauf/` (letzter Lauf: Seiten vor/nach, aktion.txt).
- **App-Waechter** (`logik/.../Waechter.kt`, getestet in `WaechterTest.kt`): Mo–Fr ohne VD-Feiertage und Urlaub; Ortszonen (Geofences) nur morgens bis zum ersten Einstempeln (Di/Fr ab 07:15, Mo/Mi/Do ab 07:45, bis 11:00), sonst nur Wecker auf feste Zeiten: Mittag 11:45, Pause vorbei +30 min (HO/HUB) / +45 min (SS), Nachfrage nach 60 min einmal, 8 h 25 voll; Wiederholung 15 min; nach 19:30 / Feierabend / Urlaub nur ein Wecker auf den nächsten Arbeitstag. Planen = exakter Wecker → Befehl Stempeln.
- **Updates:** Skripte = `SKV` hochzählen, `python3 werkzeug/paket-bauen.py`, pushen → in der App „Skripte aktualisieren“. App = `versionName`/`versionCode` in `app/build.gradle.kts` hochzählen, pushen → Workflow baut + Release `app-v<version>` → in der App „App aktualisieren“.
- **Signieren:** Secrets `STEMPELN_KEYSTORE` (Base64) + `STEMPELN_PASSWORT`; ohne sie Debug-Schlüssel (Update nur per Neuinstallation). Schlüssel nie ins Repo/Chat.
- Bauen in der Cloud: kein Android-SDK lokal → nur `logik` lokal testen (eigenes settings mit nur `:logik`), APK baut GitHub Actions (`curl https://api.github.com/repos/obitusde/stempeln/actions/runs?per_page=1`).

## 3. Offen / zu testen (Pixel)
1. App v0.1.0 installieren, Rechte (Dateien, Benachrichtigungen, Standort „immer“), Skripte v4.5 laden, Makros v4.2 importieren, alte Makros (Wächter, Spaeter, Frei, Heute nicht, Update, Stempeln/Stand v4.1) löschen.
2. Knopf Stempeln/Stand lesen startet MacroDroid? Kommt der neue Stand in der App an (Broadcast aus ende.sh)? Konto-Werte richtig?
3. Morgens: Ortszonen-Erinnerung zu Hause / im Büro; Mittag, Pause, 8 h 25; Planen; Urlaub.
4. Geschichte bis v4.4: `TAGEBUCH.md` (aus cupra-ziel).
