# Stempeln — Projektstand für Claude

**Stand 12.11.2026** · App (`app/`, `logik/`) v0.2.1 · Skripte (`skripte/`, `SKV` in `kopf.sh`) v4.6 · Makros (`makros/`) Stempeln v4.3 + Stand v4.3 · Workflow `.github/workflows/app.yml` v1.0.0

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
 Anzeige, Knoepfe, Planen, Urlaub                    Stempeln v4.3 / Stand v4.3      start, pruef, haupt, tipp, ende ...
 Waechter: Wecker + Ortszonen ──Befehl──────────────► NotificationTrigger            ◄─ Bildschirm lesen, Shizuku input tap
   (stille Meldung BEFEHL-STEMPELN / BEFEHL-STAND)    "BEFEHL-..." (Paket der App)    ende.sh: zustand.txt, konto.txt
 StandEmpfaenger ◄──────────── am broadcast -n io.github.obitusde.stempeln/.StandEmpfaenger (app_melden in kopf.sh)
```
- **Dateien** in `Download/stempeln/`: `zustand.txt` (Z_*), `konto.txt` (K_SOLDE/K_JOUR/K_RATT/K_HS), `orte.txt` (`TYP;lat;lon;Name;Radius|…`), `vorschlag.txt` (App schreibt bei Erinnerung, `haupt.sh` prüft), `spaeter.txt`, `heute-nicht.txt`, `skripte/`, `lauf/` (letzter Lauf: Seiten vor/nach, aktion.txt).
- **App-Waechter** (`logik/.../Waechter.kt`, getestet in `WaechterTest.kt`): Mo–Fr ohne VD-Feiertage und Urlaub; Ortszonen (Geofences) nur morgens bis zum ersten Einstempeln (Di/Fr ab 07:15, Mo/Mi/Do ab 07:45, bis 11:00), sonst nur Wecker auf feste Zeiten: Mittag 11:45, Pause vorbei +30 min (HO/HUB) / +45 min (SS), Nachfrage nach 60 min einmal, 8 h 25 voll; Wiederholung 15 min; nach 19:30 / Feierabend / Urlaub nur ein Wecker auf den nächsten Arbeitstag. Planen = exakter Wecker → Befehl Stempeln.
- **Updates:** Skripte = `SKV` hochzählen, `python3 werkzeug/paket-bauen.py`, pushen → in der App „Skripte aktualisieren“. App = `versionName`/`versionCode` in `app/build.gradle.kts` hochzählen, pushen → Workflow baut + Release `app-v<version>` → in der App „App aktualisieren“.
- **Signieren:** Secrets `STEMPELN_KEYSTORE` (Base64) + `STEMPELN_PASSWORT`; ohne sie Debug-Schlüssel (Update nur per Neuinstallation). Schlüssel nie ins Repo/Chat.
- Bauen in der Cloud: kein Android-SDK lokal → nur `logik` lokal testen (eigenes settings mit nur `:logik`), APK baut GitHub Actions (`curl https://api.github.com/repos/obitusde/stempeln/actions/runs?per_page=1`).

## 3. Sammelliste – erst bauen, wenn Christof es sagt
1. (12.11.) Pause in Minuten anzeigen: geplant (30/45 min nach Ort), läuft gerade (seit … min), schon gemacht (… min).
2. (12.11.) Zähler Arbeitstage bis zum Arbeitsende: Mo–Fr, **VD-Feiertage abziehen**, eingetragenen Urlaub **nicht** abziehen, **letzter Arbeitstag 31.5.2028** (1.6. frei), **heute zählt mit**.
3. (12.11.) Menü „Einstellungen“ – Christof: 1a 2a 3a = Datei `Download/stempeln/einstellungen.txt` (App + Skripte lesen sie); Arbeitszeit mit „gültig ab“-Datum; alle Werte: Soll/Tag, letzter Arbeitstag (31.5.2028), Pause HO/HUB 30 / SS 45, Home-Office-Tage (Di, Fr), Erinnerung ab (7:15 / 7:45), Mittag (11:45), Wiederholung (15 min), Abendgrenze (19:30).
4. (12.11.) Ergebnis nach jedem Stempeln **sehen und hören**: Meldung von der App selbst (ende.sh schreibt Ergebnis in eine Datei + Broadcast), eigener Ton für „geklappt“ und anderer für „Problem“; Shell-Meldung (`cmd notification`) dann nur noch als Rückfall. Christof 1a: „geklappt“ = kurzer Ton + Vibration, „Problem“ = anderer, auffälliger Ton + Vibration.
5. (12.11.) In der App anzeigen, was der nächste Stempel-Knopf tut: ein/aus und mit welchem Knopf/Ort (z. B. „Nächstes Stempeln: AUSstempeln · HUB“; morgens: Ort aus Ortszone oder „per Standort beim Stempeln“). Christof 2a: der große Knopf zeigt es selbst („Ausstempeln · HUB“).
   Plan: nur App + Skripte, **kein Makro-Import** nötig (Reihenfolge: erst App, dann Skripte).

Umgesetzt 12.11. (Christof: „Setze das alles jetzt um“):
- Makros v4.3: erste Prüfung nach ~5 s, bis zu 8 Runden „Bildschirm lesen → `pruef.sh` (R=1..8) → Agenda da?“; **Wartezeiten stehen in `pruef.sh`** (falsche Seite/gesperrt → sofort neu öffnen + 5 s, ohne Adresse erst ab Runde 3, höchstens 3× neu; lädt noch → 2 s), nach dem Tipp wartet `haupt.sh` 3 s. Kein „Startbildschirm“ mehr: `ende.sh` ruft `zurueck` → **App Stempeln kommt nach jedem Lauf nach vorn**.
- App v0.2.0: Status mit Fortschrittsbalken, Zeilen Arbeitszeit Soll / Bereits gearbeitet / Fehlt noch / Voraussichtlich Schluss (+ Erklärung, `logik/Anzeige.kt`, Pause 30/45 min eingerechnet), „Überstunden“ (= Saldo bis gestern) mit Arbeitstagen zu 8 h 25; Heures du jour, Rattrapage (= Urlaubs-Ansparminuten, egal), HS ausgeblendet; Planen +15/+30/+45 min/+1 h/17:00/17:30/Uhrzeit, Knopf „Zurück“; Urlaub: erster + **letzter Urlaubstag** mit Hinweis, Status zeigt Urlaub und „geht wieder los am …“; Meldungen verschwinden nach 60 s; Umlaute; Inhalt nicht mehr unter der Statusleiste.

## 4. Offen / zu testen (Pixel)
00. ✅ 12.11.2026 „Klappt alles“: v0.2.1 / v4.6 / v4.3: Secrets gesetzt, v0.2.1 = erste fest signierte Version → einmal alte App deinstallieren, v0.2.1 installieren, Rechte neu (danach Updates per Knopf), Skripte aktualisieren, Makros v4.3 importieren, v4.2 löschen. Prüfen: Startseite wird schnell erkannt? App kommt nach Stempeln/Stand zurück? Anzeige verständlich?
0. ✅ 12.11.2026: App v0.1.0 läuft, Import Stempeln/Stand v4.2 ok, „Stand lesen“ aus der App startet MacroDroid, Konto (Saldo 13:25, Rattrapage 71:40) kommt in der App an, Skripte v4.5, Shizuku erkannt.
1. App v0.1.0 installieren, Rechte (Dateien, Benachrichtigungen, Standort „immer“), Skripte v4.5 laden, Makros v4.2 importieren, alte Makros (Wächter, Spaeter, Frei, Heute nicht, Update, Stempeln/Stand v4.1) löschen.
2. Knopf Stempeln/Stand lesen startet MacroDroid? Kommt der neue Stand in der App an (Broadcast aus ende.sh)? Konto-Werte richtig?
3. Morgens: Ortszonen-Erinnerung zu Hause / im Büro; Mittag, Pause, 8 h 25; Planen; Urlaub.
4. Geschichte bis v4.4: `TAGEBUCH.md` (aus cupra-ziel).
