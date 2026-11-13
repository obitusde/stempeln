# Stempeln — Projektstand für Claude

**Stand 13.11.2026** · **Gemeinsame Version 5.1**: App (`app/`, `logik/`) 5.1 · Skripte (`skripte/`, `SKV` in `kopf.sh`) 5.1 · Makros (`makros/`) „Stempeln“ + „Stempeln Stand“ 5.0 (unverändert, `MAKRO_NOETIG` 5.0) · Workflow `.github/workflows/app.yml` v1.0.0

## 1. Nutzer und Arbeitsweise (unbedingt beachten)
- **Christof** (GitHub `obitusde`), Pixel 8 (Android 17), MacroDroid Pro + Shizuku. Will so wenig wie möglich selbst machen, kein Terminal.
- **Erst kurz high-level besprechen, dann bauen** (nach „los“/„ja“). Fragen als Text mit a/b/c. **Deutsch** überall. **Versionsnummer bei jeder Änderung.** Unsicherheit ehrlich benennen. Direkt auf `main`.
- Möglichst wenig Hintergrundaktivität: nichts darf laufen, wenn nichts zu tun ist (nachts, Wochenende, Urlaub).
- **Privat, nie ins Repo:** Orte/Koordinaten (`orte.txt` nur auf dem Handy; private Makro-Fassung mit Orten nur in Christofs Drive), Seiten-Dumps (Name + Login-Ticket), Screenshots/Logs nur im Chat, Signierschlüssel (nur GitHub-Secrets).
- **Makro-Dateien** gehören in Christofs Drive-Ordner `1EE216VJXRqmXnno7-ks51n-ufNmaBQBP` (Drive-Connector, `create_file` mit `parentId`, JSON mit Zeilenumbruch nach `},{` hochladen, Größe gegen lokale Datei prüfen). Gut abschreibbare IDs (`7441101200nn`).
- `m_description` jedes Makros aus Nutzersicht (wie läuft es, was muss er tun, was nicht).
- **Eine gemeinsame Version (Christof 13.11., a):** App (`versionName`) und Skripte (`SKV`) bekommen bei jeder Änderung **dieselbe** Nummer (5.0, 5.1, …), auch wenn sich nur eins ändert. Makros heißen ohne Nummer („Stempeln“, „Stempeln Stand“), Version in `m_description` und `MV="…"` in jeder Hülle (start.sh schreibt `makro-version.txt`); Makros nur neu, wenn sich die Hülle ändert, dann `MAKRO_NOETIG` in `MainActivity` hochsetzen. Dateien `makros/Stempeln-<version>.macro`. App warnt, wenn Skripte ≠ App oder Makros < `MAKRO_NOETIG`.

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
(leer)

Umgesetzt 13.11. (Christof: „Neue version“) – **5.1** (App + Skripte, Skripte nur Nummer):
- Farbiges **Status-Band** ganz oben: EINGESTEMPELT (grün), PAUSE (orange, seit … min / vorbei um …), AUSGESTEMPELT (rot), FEIERABEND (dunkelgrün), NOCH NICHT EINGESTEMPELT (blaugrau), URLAUB (lila), FREI (grau).
- **Plan** als hellblaue Zeile oben mit rotem Knopf „Plan löschen“.
- Alte Makros (Wächter, Spaeter, Frei, Heute nicht, v4.x) können weg; „Stempeln Update“ nur ausschalten (enthält Orte).

Umgesetzt 13.11. (Christof: „a, bauen“) – **5.0** (App, Skripte, Makros):
- Gemeinsame Version (siehe 1.), Makros umbenannt „Stempeln“ / „Stempeln Stand“ (GUID 744110150000 / 744110160000, sonst = v4.3-Hülle), App zeigt „Version: App · Skripte · Makros“ + Warnknöpfe.
- Planen „in 35 min“; jedes Stempeln (App-Knopf, Erinnerung, Plan) löscht den Plan (`Steuerung.makro`); Pause läuft = orange Zeile oben; Plan-Hinweis auf derselben Meldung (ID_LAUF).
- Nicht gemacht: Standort von der App (Log 13.11.: GPS 1,5 s parallel zur Wartezeit, bringt nichts).
- Befund 13.11. (MacroDroid-Log): auf dem Handy liefen noch v4.2 (erste Prüfung erst nach ~22 s, am Ende „Startbildschirm“ → App kam nicht nach vorn).

Umgesetzt 12.11. abends (Christof: „Bauen“) – App v0.3.0 + Skripte v4.7, **kein Makro-Import**:
- **Einstellungen** (Knopf „Einstellungen“): Datei `Download/stempeln/einstellungen.txt` (`logik/Einstellungen.kt`, `E_SOLL='2000-01-01=08:25,…'` mit „gültig ab“, `E_ENDE`, `E_PAUSE_KURZ/SS`, `E_HO_TAGE` (ISO-Ziffern, 25 = Di+Fr), `E_START_HO/BUERO`, `E_MITTAG`, `E_WDH`, `E_ABEND`); App legt sie mit den heutigen Werten an; Wächter/Anzeige nehmen `Lage.e`; Skripte laden sie in `kopf.sh` (`soll_am`, `pause_fuer`).
- **Pause** in der Statuskarte: „Pause läuft seit … min · geplant …, vorbei um …“ / „Pause gemacht: … min“ (`Z_PAUSE` in zustand.txt, neu von ende.sh) / „Pause geplant: … min“.
- **Zähler** „Noch Arbeitstage“ bis zum letzten Arbeitstag (Mo–Fr ohne VD-Feiertage, Urlaub zählt mit, heute zählt mit; 12.11.2026 → 395).
- **Großer Knopf** zeigt, was er tut: „Ausstempeln · HUB“, „Einstempeln · SUR SITE (Le Brassus)“, morgens ohne Ortszone „Einstempeln · Ort per Standort“ (`Anzeige.naechstes`).
- **Meldungen mit Ton:** Skripte rufen `note … ART` → `am broadcast … MELDUNG --es art --es text` → App zeigt eine Meldung (ID 4740, wird ersetzt): fortschritt (still), hinweis (Standardton), ok (Kanal „Geklappt“, `res/raw/geklappt.wav`, kurz hell), problem (Kanal „Problem“, `res/raw/problem.wav`, drei tiefe Töne, starke Vibration). App antwortet mit resultCode 7; fehlt `result=7` in der am-Ausgabe → Shell-Meldung wie früher (Rückfall). Problem = nicht nachgeprüft, schon gestempelt/nichts getippt, Seite nicht erreicht, unbekannter Ort.

Umgesetzt 12.11. (Christof: „Setze das alles jetzt um“):
- Makros v4.3: erste Prüfung nach ~5 s, bis zu 8 Runden „Bildschirm lesen → `pruef.sh` (R=1..8) → Agenda da?“; **Wartezeiten stehen in `pruef.sh`** (falsche Seite/gesperrt → sofort neu öffnen + 5 s, ohne Adresse erst ab Runde 3, höchstens 3× neu; lädt noch → 2 s), nach dem Tipp wartet `haupt.sh` 3 s. Kein „Startbildschirm“ mehr: `ende.sh` ruft `zurueck` → **App Stempeln kommt nach jedem Lauf nach vorn**.
- App v0.2.0: Status mit Fortschrittsbalken, Zeilen Arbeitszeit Soll / Bereits gearbeitet / Fehlt noch / Voraussichtlich Schluss (+ Erklärung, `logik/Anzeige.kt`, Pause 30/45 min eingerechnet), „Überstunden“ (= Saldo bis gestern) mit Arbeitstagen zu 8 h 25; Heures du jour, Rattrapage (= Urlaubs-Ansparminuten, egal), HS ausgeblendet; Planen +15/+30/+45 min/+1 h/17:00/17:30/Uhrzeit, Knopf „Zurück“; Urlaub: erster + **letzter Urlaubstag** mit Hinweis, Status zeigt Urlaub und „geht wieder los am …“; Meldungen verschwinden nach 60 s; Umlaute; Inhalt nicht mehr unter der Statusleiste.

## 4. Offen / zu testen (Pixel)
00000. 5.1 (13.11.): App aktualisieren + Skripte aktualisieren (kein Makro-Import). Status-Band und „Plan löschen“ prüfen.
0000. 5.0 (13.11.): App aktualisieren, Skripte aktualisieren, Makros „Stempeln-5.0“ + „Stempeln-Stand-5.0“ importieren, alle alten Stempel-Makros löschen. Prüfen: Dauer (~15–25 s?), App kommt zurück, eine Meldung, Plan verschwindet nach Stempeln, Pause orange, unten „Version: App 5.0 · Skripte 5.0 · Makros 5.0“.
000. v0.3.0 / v4.7 (12.11.): App aktualisieren (Knopf), dann Skripte aktualisieren, Einstellungen prüfen. Prüfen: Ton „geklappt“/„Problem“ kommt, keine doppelte Meldung (sonst liefert `am broadcast` kein result=7 → Rückfall), Knopf-Text, Pause, Zähler 395.
00. ✅ 12.11.2026 „Klappt alles“: v0.2.1 / v4.6 / v4.3: Secrets gesetzt, v0.2.1 = erste fest signierte Version → einmal alte App deinstallieren, v0.2.1 installieren, Rechte neu (danach Updates per Knopf), Skripte aktualisieren, Makros v4.3 importieren, v4.2 löschen. Prüfen: Startseite wird schnell erkannt? App kommt nach Stempeln/Stand zurück? Anzeige verständlich?
0. ✅ 12.11.2026: App v0.1.0 läuft, Import Stempeln/Stand v4.2 ok, „Stand lesen“ aus der App startet MacroDroid, Konto (Saldo 13:25, Rattrapage 71:40) kommt in der App an, Skripte v4.5, Shizuku erkannt.
1. App v0.1.0 installieren, Rechte (Dateien, Benachrichtigungen, Standort „immer“), Skripte v4.5 laden, Makros v4.2 importieren, alte Makros (Wächter, Spaeter, Frei, Heute nicht, Update, Stempeln/Stand v4.1) löschen.
2. Knopf Stempeln/Stand lesen startet MacroDroid? Kommt der neue Stand in der App an (Broadcast aus ende.sh)? Konto-Werte richtig?
3. Morgens: Ortszonen-Erinnerung zu Hause / im Büro; Mittag, Pause, 8 h 25; Planen; Urlaub.
4. Geschichte bis v4.4: `TAGEBUCH.md` (aus cupra-ziel).
