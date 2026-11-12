# Test-Makros (nicht Cupra)

Hier liegen Makros, die nichts mit Cupra-Ziel zu tun haben und nur zum Ausprobieren im öffentlichen Repo stehen.
Wenn sie laufen, zieht Christof sie in ein eigenes Repo um. **Nichts Persönliches in die Dateien** (Adressen,
Namen, Zugangsdaten): Solche Werte stehen nur in MacroDroid-Variablen auf dem Handy.

## Stempeln v15 (`Stempeln-v15.macro`, Pixel) und Stempeln Mi v1 (`Stempeln-Mi-v1.macro`, Mi 9T), 26.10.2026

Stempelt in der Zeiterfassung „SUR SITE" (schaltet je nach Stand **ein oder aus**, bucht sofort, ohne Bestätigung –
Christof 26.10.2026). Von Hand starten. **Keine Ja/Nein-Abfrage mehr** (Christof 26.10.2026: „ganz raus") –
jeder Start stempelt.

**Adresse fest im Makro** (Christof 26.10.2026: „mach Makro jetzt mit der URL, später lösche ich das aus dem Repo"):
direkt die Stempelseite `…/webquartz/ux/clock`.

**Warum Chrome:** Die Seite zeigt hochkant nur „bitte drehen". Mit „Desktop-Website" klappt es hochkant.
Firefox merkt sich das nur pro Tab, Chrome pro Seite (bei Christof eingerichtet). Firefox bleibt Standard-Browser.

| # | Aktion | Wert |
|---|---|---|
| 1–7 | Log, Intent (Activity), Warten 7 s, Log, **gleicher Intent noch einmal**, Warten 6 s | VIEW `…/ux/clock`, Paket `com.android.chrome`, Extra `com.android.browser.application_id` = `macrodroid.stempeln` (immer derselbe Tab) |
| 5–10 | Log, wenn `debug`: Screenshot **A** | vor dem Tipp |
| 11–12 | Log, **Shell-Skript über Shizuku** `input tap X Y </dev/null >/dev/null 2>&1` (Pixel 70 567, Mi 68 522), Rückgabewert nach `Download/stempeln.txt` | `ShellScriptAction`: `m_script`, `useShizuku` true, `m_nonRoot` false, `blockNextAction` true, `timeoutSeconds` 10 (Felder aus Beispiel in BespredeL/MacroDroid; Shizuku-Modus geraten) |
| 13–17 | Log, Warten 5 s, Log, gleicher Intent, Warten 7 s | Seite neu laden |
| 18–22 | wenn `debug`: Screenshot **C** | Agenda nach Neuladen |
| 23–28 | Log, Startbildschirm, Warten 1 s, Chrome beenden, Popup, Log | |

### Shizuku einrichten (einmalig, ohne PC)

1. Play Store: **Shizuku** installieren.
2. Einstellungen → System → Entwickleroptionen → **Kabelloses Debugging** an (Handy im WLAN).
3. Shizuku öffnen → „Über kabelloses Debugging starten" → **Kopplung**. Dann in „Kabelloses Debugging" →
   „Gerät über Kopplungscode koppeln" → den Code in die Shizuku-Benachrichtigung eingeben.
4. In Shizuku **Starten** → oben „Shizuku läuft".
5. MacroDroid fragt beim ersten Shell-Skript nach Shizuku-Zugriff → erlauben (oder in Shizuku unter
   „Autorisierte Anwendungen" MacroDroid einschalten).
6. **Nach jedem Neustart des Handys:** Shizuku öffnen → Starten (WLAN + Kabelloses Debugging nötig).

Stempelseite hochkant/Desktop in Chrome (Pixel 8, 1080 × 2400, Screenshot × 1,2): „SORTIE HOME OFFICE" y 419–454
(x 17–209), **„SUR SITE" ≈ 37–101 / 558–576 → Tipp 70, 567**, „HUB" ab x 158.

Gelernt (v3, 26.10.2026): Text-Klick mit Regex `(?i)^\s*tout voir\s*$` in Chrome lief laut Log, bewirkte aber nichts
(Screenshots A und B gleich). Ob Regex oder Chrome schuld ist, ist offen → Koordinaten. Die Screenshot-Aktion
(`TakeScreenshotAction`, `m_option` 0) speichert ohne Rückfrage, Magic Text `{fg_app_name}` geht („Chrome").

Gelernt (v4, 26.10.2026): Tipp auf 70, 567 war sichtbar, stempelte aber nicht. Vermutung: kam direkt nach dem
Schließen der Ja/Nein-Abfrage (MacroDroid-Fenster noch im Vordergrund) → v5 wartet 2 s und macht Screenshot B davor.

Gelernt (v5 + Klick-Test v1, 26.10.2026): Ein Tipp auf eine feste Stelle wirkt in Chrome (Klick-Test: „Accueil" →
Startseite), Klick über Text nicht. In v4/v5 kam der Tipp **nach** der Ja/Nein-Abfrage und wirkte nicht, obwohl der
Screenshot davor kein Fenster zeigte und der Tipp genau auf „SUR SITE" saß (Schrift x 38–99, y 562–572 im
Original-Screenshot). Vermutung: die Abfrage (MacroDroid-Activity) schluckt Tipps noch eine Weile unsichtbar →
v6 fragte zuerst und öffnete Chrome erst danach; v7 fragt gar nicht mehr.

Gelernt (v7, 26.10.2026): auch ohne Abfrage bucht der kurze Klick nicht (Screenshots vor/nach gleich). „SUR SITE" ist
ein normales `<button type="button">` (React, Klick-Listener am Knopf und an `div#main`). **Von Hand, hochkant, in
Chrome mit Desktop-Ansicht bucht es sofort** (Christof) → nicht die Seite, sondern der künstliche Kurz-Klick ist das
Problem → v8: Geste mit 150 ms Druckdauer.

Gelernt (v8, 26.10.2026): **Die Seite zeigt eine neue Buchung erst nach dem Neuladen** (auch nach einem Finger-Tipp
bleibt die Agenda gleich; der rote Strich in der Agenda ist nur die aktuelle Uhrzeit, kein Status). Sicher ohne
Buchung: v5 (13:22) und v7 (13:57). v8 (Geste 14:14:51) unklar: es gibt genau eine Buchung ← 14:15, Christof tippte
kurz danach auch von Hand. → v9: Geste 200 ms, danach Seite neu laden und Screenshot C.

Gelernt (v9, 26.10.2026): Geste 200 ms bucht nicht (nach Neuladen keine neue Buchung). Ein Screenshot (14:17) zeigt
um „SUR SITE" den **Fokus-Rahmen** (x 18–120, y 551–585 → Tipp 70, 567 sitzt mittig): der künstliche Tipp wählt den
Knopf an, löst ihn aber nicht aus. → v10: danach „Klick auf aktuellen Fokus" (Bedienungshilfe-Klick).

Gelernt (v10, 26.10.2026): bucht auch nicht. **Neu durchdacht:** Gemeinsam ist v5–v10, dass Chrome 3–12 s nach dem
Tipp neu geladen (v9/v10, nach 3–4 s) oder beendet wurde (v5/v7/v8, nach ~10 s). v4 (12:09, Chrome blieb offen) und
v8 (Geste 14:14:51, Chrome erst 14:15:03 beendet) fallen zeitlich genau mit den Buchungen ← 12:09 und ← 14:15 zusammen,
die Christof seinem Finger zuschrieb (die Seite zeigt Buchungen erst nach Neuladen, er hat jeweils kurz danach von
Hand nachgetippt). Vermutung: „SUR SITE" holt erst den **Standort** (Horoquartz wirbt mit geolokalisiertem Stempeln)
und bucht erst danach; Neuladen/Beenden bricht das ab. Der Finger lässt die Seite offen. → v11: einfacher Tipp,
**60 s Chrome offen lassen**, erst dann neu laden.

Gelernt (v11, 26.10.2026): bucht auch nicht. Am PC mit der Maus erscheint die Buchung sofort → keine Wartezeit,
Standort-Vermutung falsch. Fazit: Tipps über die Bedienungshilfe (Klick, Geste, Fokus-Klick) lösen diesen Knopf
nicht aus, einen Link schon; Ursache von außen nicht feststellbar. → v12: Tipp über die System-Eingabe
(`input tap` per Shizuku), derselbe Weg wie ein Finger.

Gelernt (v12, 26.10.2026): Screenshot A zeigte **Accueil** statt der Stempelseite (erstes Öffnen landete auf der
Startseite), der Tipp ging ins Leere; nach dem Neuladen war die Stempelseite da. Shizuku-Tipp also noch ungetestet.
Zweites Öffnen landete bisher immer auf der Stempelseite (Klick-Test 2a, v9–v12 Screenshot C) → v13 öffnet zweimal.
(Zeilennummern der Tabelle ab Aktion 8 um 3 verschoben.)

Gelernt (v13, 26.10.2026, 15:12): Stempelseite war offen (Screenshot A), Shizuku-`input tap` bucht trotzdem nicht.
Gemeinsam bei allen künstlichen Tipps: sie setzen höchstens den Fokus (blauer Rahmen), lösen aber nicht aus; Finger
und PC-Maus schon. → v14: nach dem Tipp **Enter-Taste** (`input keyevent 66`) – ein fokussierter `<button>` wird vom
Browser per Tastatur ausgelöst, unabhängig davon, wie die Seite Berührungen auswertet.

### Einrichten

1. Chrome einmalig: Seite öffnen, anmelden, ⋮ → „Desktopwebsite" (erledigt 26.10.2026).
2. `https://obitusde.github.io/cupra-ziel/macrodroid/test/Stempeln-v15.macro` (Pixel) bzw. `…/Stempeln-Mi-v1.macro` (Mi) laden → MacroDroid → ⋮ → **Import**,
   ältere „Stempeln"-Fassungen löschen.
3. MacroDroid → Variablen: **`debug`** = wahr (Screenshots an).
4. Ausführen = **echt stempeln** (ein oder aus).
5. Debuggen: MacroDroid → ☰ → **Makro-Log** und die Screenshots A/B/C in den Chat. **Nie ins Repo** (Name, Zeiten).
6. Läuft alles: `debug` = falsch.

### Unsicher (v15 / Mi v1)

- v12–v14 buchten nicht, Christof sah keinen Tipp-Kreis. Ob `input tap` überhaupt lief, ist offen (MacroDroid führt
  Shell-Skripte stumm aus; Log 15:06 „Shizuku permission not granted" bei v12, später keine Meldung) → Shizuku-Test v1.
- Keine Horoquartz-App verfügbar (Christof). Fallback: halbautomatisch – Makro öffnet die Seite, Christof tippt.
- Später: Start per GPS (Ankunft am Arbeitsort / Verlassen).

## Klick-Test v1 (`Klick-Test-v1.macro`, 26.10.2026)

**Stempelt nicht.** Klärt, wie MacroDroid in Chrome überhaupt etwas anklicken kann. Ziel ist nur der harmlose
Link „Accueil" (Brotkrume oben links auf der Stempelseite, wechselt zur Startseite).

1. Stempelseite öffnen (gleicher Intent wie Stempeln v5), 7 s, Screenshot 1a → **Tipp auf die Stelle 94, 157**
   (Text „Accueil" ≈ x 71–118, y 150–166), 4 s, Screenshot 1b.
2. Stempelseite erneut öffnen, 7 s, Screenshot 2a → **Klick über den Text `Accueil`** (ohne Regex, Einstellungen wie
   „Suche" im Cupra-Makro), 4 s, Screenshot 2b.
3. Startbildschirm, Chrome beenden. Popups zeigen, welcher Test gerade läuft.

Auswertung: Zeigt 1b bzw. 2b die Startseite (Kästen „Timbrer", „Mes indicateurs" …), funktioniert dieser Weg →
Stempeln v6 klickt „SUR SITE" genauso.

## Shizuku-Test v3 (`Shizuku-Test-v3.macro`, 26.10.2026)

**Stempelt nicht.**

Ergebnis v1 (15:24): Shizuku läuft (`uid=2000(shell)`, Gruppe `1004(input)`), aber `input tap` →
`cmd: Failure calling service input: Failed transaction (2147483646)` – **v12–v14 haben nie getippt.**

Ergebnis v2 (15:38): Pixel 8, **Android 17 (SDK 37)**. Aus dem Shizuku-Prozess scheitert **jeder** `cmd`-Dienst
(`input`, `settings`, `window`: Failed transaction), `service check input` findet den Dienst aber. Alle drei
Schreibweisen des Tipps scheitern gleich. **`/dev/input/event3` = `goodix_ts0`** (Touchscreen, `crw-rw---- root input`),
Protokoll B: `ABS_MT_SLOT` 0–9, `ABS_MT_TRACKING_ID`, `ABS_MT_POSITION_X` 0–1079, `ABS_MT_POSITION_Y` 0–2399
(= Bildschirmpixel), `ABS_MT_TOUCH_MAJOR`/`MINOR`, `ABS_MT_PRESSURE` 0–255, `BTN_TOUCH`, `BTN_TOOL_FINGER`.

v3: Tipp auf „Accueil" (94, 157) **direkt am Touchscreen-Treiber** per `sendevent /dev/input/event3`
(Slot 0, Tracking-ID, Position, Größe 40, Druck 80, `BTN_TOUCH`/`BTN_TOOL_FINGER` an, SYN – 0,1 s – Tracking-ID −1,
Tasten aus, SYN), Fehler je Befehl in `Download/shizuku-test3.txt`. Screenshot 2 zeigt die Startseite = es geht →
derselbe Tipp auf „SUR SITE" (70, 567). Sonst Schluss → halbautomatisch.

**Ergebnis v3 (15:45): `sendevent: /dev/input/event3: Permission denied`** bei jedem Befehl – trotz Gruppe `input`
blockiert Android (SELinux) das Schreiben auf den Touchscreen aus Shizuku. Seite blieb auf der Stempelseite.

## Fazit (26.10.2026)

Vollautomatisches Stempeln auf diesem Weg geht auf dem Pixel 8 mit Android 17 **nicht**:
- Tipps über die Bedienungshilfe (Klick, Geste, Fokus-Klick, Text-Klick) wählen „SUR SITE" höchstens an, lösen ihn
  aber nicht aus (Links wie „Accueil" schon). Bekanntes Muster bei Android-Chrome (Buttons vs. Links).
- Shizuku läuft (uid 2000), aber unter Android 17 scheitert daraus jeder `cmd`-Dienst (`input tap`, `input keyevent`,
  `settings`, `wm`: „Failed transaction"), und `sendevent` auf den Touchscreen wird verweigert.
- Nicht verfolgt: echtes `adb shell input tap` vom PC (bräuchte ständig einen verbundenen Rechner); direkter Aufruf
  der Stempel-Schnittstelle der Seite (Anmeldedaten/Token, fragil, evtl. gegen Firmenregeln).
- Vielleicht später: ein Shizuku-Update, das `cmd` unter Android 17 wieder erlaubt → dann `input tap` erneut testen
  (Shizuku-Test v3 zeigt es sofort).

Nächster Schritt: **halbautomatisch** – Makro öffnet die Stempelseite (später per GPS mit Hinweis), Christof tippt
„SUR SITE" selbst.

## Mi-Test v1 (`Mi-Test-v1.macro`, 26.10.2026) – zweites Handy

**Stempelt nicht.** Gerät: **Xiaomi Mi 9T (davinci), PixelOS 16.2 (Android 16), 1080 × 2340**, vermutlich gerootet.
Stempelseite dort (Chrome, Desktopwebsite, hochkant; Screenshot in Originalgröße vermessen): „Accueil" x 71–116,
y 107–119 → **94, 113**; „ENTRÉE/SORTIE HOME OFFICE" y 374–408; **„SUR SITE" x 38–99, y 517–527 → 68, 522**;
„HUB" ab x 158.

Ablauf (Shell-Skripte im **Root-Modus**: `m_nonRoot` false, `useShizuku` false):
1. Diagnose nach `Download/mi-test.txt`: `id` (**`uid=0` = Root**), Modell, Android, `wm size`, `/dev/input`,
   `getevent -lp`. Keine Datei = kein Root (oder Root-Abfrage abgelehnt).
2. Stempelseite zweimal öffnen, Screenshot 1 → `input tap 94 113` (Root) → Screenshot 2 (Startseite?).
3. Stempelseite wieder öffnen, Screenshot 3 → Tipp **direkt am Touchscreen** per `sendevent` (Gerät und Wertebereich
   werden aus `getevent -lp` gesucht und umgerechnet) → Screenshot 4 (Startseite?).
4. Startbildschirm, Chrome beenden.

Danach: Wirkt ein Weg auf „Accueil" → derselbe Weg auf „SUR SITE" (68, 522) als echter Test.

**Ergebnis Mi-Test v1 (22:57):** keine Root-Abfrage, keine Datei `mi-test.txt`, alle Screenshots zeigen die
Stempelseite → **kein Root** (PixelOS ohne Magisk/KernelSU), Root-Skripte scheiterten still.

## Mi-Test v2 (`Mi-Test-v2.macro`, 26.10.2026)

**Stempelt nicht.** Wie Mi-Test v1, aber alle Shell-Skripte über **Shizuku** (`useShizuku` true). Datei
`Download/mi-test2.txt`. Voraussetzung: Shizuku auf dem Mi eingerichtet und gestartet (Anleitung oben),
MacroDroid-Zugriff erlaubt. Erwartung: unter Android 16 läuft `input tap` aus Shizuku (am Pixel scheiterte es an
Android 17); `sendevent` vermutlich wieder „Permission denied".

**Ergebnis Mi-Test v2 (23:08):** Shizuku läuft (`uid=2000(shell)`), Android 16 – aber **derselbe Fehler wie am
Pixel**: `wm size` und `input tap` → „Failed transaction", `sendevent` auf `/dev/input/event2` (`goodix_ts`,
0–1079 × 0–2339) → „Permission denied". **Die Android-17-Erklärung war falsch.**

## Mi-Test v3 (`Mi-Test-v3.macro`, 26.10.2026)

**Stempelt nicht.** Neue Vermutung: Alle `cmd`-Befehle (`input`, `wm`, `settings`) reichen ihre Ausgabe-Kanäle an
den Systemdienst weiter. In allen Tests war die Ausgabe **direkt in eine Datei unter `/sdcard/Download`** umgeleitet –
diese Datei darf `system_server` nicht benutzen → Binder-Aufruf scheitert mit „Failed transaction". Befehle ohne
Systemdienst (`id`, `getevent`) liefen.

v3 schreibt nach `Download/mi-test3.txt`: `wm size` (A) mit Ausgabe in die Datei, (B) über eine Shell-Variable,
(C) nach `/dev/null`; dann `input tap 94 113` (Accueil) mit Ausgabe nach `/dev/null`, Rückgabewert in die Datei,
Screenshot 2. A scheitert, B/C klappen und Screenshot 2 zeigt die Startseite = Vermutung bestätigt → echter Test auf
„SUR SITE" (Mi: 68, 522), Skripte ohne Datei-Umleitung.

**Ergebnis Mi-Test v3 (23:12): Vermutung bestätigt.** `wm size` mit Ausgabe in die Datei → „Failed transaction",
über Variable → `rc=0 Physical size: 1080x2340`, nach `/dev/null` → `rc=0`. **`input tap 94 113` mit Ausgabe nach
`/dev/null` → `rc=0`, Screenshot 2 zeigt die Startseite.** Der Shizuku-Tipp funktioniert; alle früheren
Shizuku-Fehlschläge (Pixel v12–v14, Shizuku-Test v1/v2, Mi-Test v2) lagen an der Ausgabe-Umleitung in eine Datei
unter `/sdcard` (bzw. bei v12–v14 vermutlich an MacroDroids Ausgabe-Kanal – nie geprüft).
**Regel: `cmd`-Befehle (`input`, `wm`, `settings`, …) aus Shizuku immer mit `</dev/null >/dev/null 2>&1` oder über
`$(…)` aufrufen.** → Stempeln Mi v1 / Stempeln v15 (Pixel): echter Test auf „SUR SITE".

## Pixel-Test v1 (`Pixel-Test-v1.macro`, 26.10.2026)

**Stempelt nicht.** Wie Mi-Test v3, für das Pixel 8: `wm size` auf drei Arten, dann Shizuku-`input tap 94 157`
(„Accueil" auf dem Pixel) mit Ausgabe nach `/dev/null`, Screenshot 2. Datei `Download/pixel-test.txt`. Startseite auf
Screenshot 2 → am Pixel geht der Tipp auch → Stempeln v15 beim nächsten echten Stempeln.

**Ergebnis Pixel-Test v1 (23:22): klappt auch am Pixel.** Gleiches Muster wie am Mi (A „Failed transaction",
B `rc=0 Physical size: 1080x2400`, C `rc=0`), `input tap 94 157` → `rc=0`, Screenshot zeigt die Startseite.
(Shizuku fragte nicht erneut nach Zugriff – MacroDroid war noch autorisiert.) → Stempeln v15 am Pixel ist der echte Test.

**✅ 27.10.2026: Stempeln v15 am Pixel hat gebucht** (Christof: „hat geklappt"). Getippt wird per Shizuku
`input tap 70 567` = Mitte von „SUR SITE" (Schrift x 38–99 / y 562–572, Knopf x 18–120 / y 551–585).
Nächste Schritte: `debug` = falsch, Start per GPS (Ankunft/Verlassen) mit Schutzregeln, Erinnerung „8 h voll".

## Stempeln Ort v2 (`Stempeln-Ort-v2.macro`, 27.10.2026) – ein Knopf, Ort per GPS

**Stempelt.** Christof (27.10.2026): „b" – ein Symbol, das selbst erkennt, wo er ist. Zu Hause → großer Knopf
„ENTRÉE/SORTIE HOME OFFICE" (**113, 436**, Knopf x 18–208 / y 419–453), Le Brassus (2 Orte) → „SUR SITE" (70, 567),
Crissier/Millenium → „HUB" (**172, 567**, Schrift x 159–186 / y 562–572). Alle schalten ein/aus.

Ablauf: Popup → **`ForceLocationUpdateAction`** (JSON nur aus `ruby-macrodroid`, keine Felder), 10 s → Stempelseite
2× öffnen → Screenshot A → **Shell (Shizuku)**: liest `{last_loc_lat}`/`{last_loc_long}` (Magic Text) und die Liste
**`ORTE`** im Skript (`Typ;Lat;Lon;Name;Radius|…`, Typ `HO`/`SS`/`HUB`, Radius leer = 800 m), sucht per `awk`
(Haversine) den nächsten Ort; innerhalb des Radius → `input tap` auf den passenden Knopf (Ausgabe `/dev/null`), sonst
nichts. Ergebnis als **Benachrichtigung** (`cmd notification post … </dev/null >/dev/null 2>&1`) und Zeile in
`Download/stempeln.txt` → 5 s, neu laden, Screenshot C, Chrome beenden.

v1 → v2: Orte stehen direkt im Shell-Skript statt in der globalen Variable `stempel_orte` (ein Import überschreibt
eine schon vorhandene Variable nicht zuverlässig); Radius je Ort (Zuhause kleiner, damit es nicht schon in der
Nachbarschaft „Home Office" stempelt).

**Orte nie ins Repo:** Im Repo ist `ORTE` leer. Die Fassung mit Christofs Orten liegt **privat in seinem
Google Drive** (`Stempeln-Ort-v2.macro`, angelegt von Claude über den Drive-Connector), Import in MacroDroid von dort.
Stand 27.10.2026: Zuhause, Le Brassus (2 Orte), Crissier. **v2.1 (nur Drive):** genaue Koordinaten für den zweiten
Ort in Le Brassus (v2 hatte einen geschätzten Wert); Logik unverändert, die Repo-Fassung bleibt v2.

### v2.2 (27.10.2026) – Diagnose für einen einzigen Testlauf

Christof kann nicht oft testen → ein Lauf muss alles zeigen. Jeder Lauf legt `Download/stempeln/<Datum_Zeit>/` an
(Zeiger in `Download/stempeln/aktuell.txt`, damit die drei Shell-Schritte denselben Ordner finden):
- `log.txt`: Schritt 0 (eigenes Shizuku-Skript gleich am Anfang: `id`, Gerät, Bildschirm an/aus, Sperre, App vorne,
  `awk` vorhanden?, Benachrichtigung „Shizuku ok"), Schritt 4 (MacroDroid-Standort, Android-Standort mit Genauigkeit aus
  `dumpsys location`, App vorne, Drehung, Rohergebnis der Berechnung, Tipp und rc), Schritt 6 (App vorne nach Neuladen).
- `1-vor-tipp.png`, `2-nach-tipp.png` (2 s nach dem Tipp), `3-nach-neuladen.png` per `screencap` aus der Shell
  (ersetzt die MacroDroid-Screenshots und die Variable `debug`).
- `ergebnis.txt`; Endbenachrichtigung = Ergebnis + Ordnername. Keine Benachrichtigung = Shizuku lief nicht.

`dumpsys`/`wm` nur über `$(…)` bzw. mit `</dev/null`, nie direkt in eine Datei (Regel oben). Bewusst **kein**
`uiautomator dump` für den Seitentext: kann Bedienungshilfen-Dienste (MacroDroid, Cupra-Makro) abschalten.
Ausschalten, wenn alles läuft: `DEBUG=0` in den Shell-Skripten (nächste Version), dann nur noch die Zeile in
`Download/stempeln.txt`. Lokal mit nachgebauten Android-Befehlen getestet (Tipp zu Hause, zu weit weg, kein Standort).

Logik lokal getestet (zu Hause 13 m → HO, Le Brassus 94 m → SS, Crissier 43 m → HUB, 718 m von zu Hause → nichts,
kein Standort → nichts). Unsicher: Import von `ForceLocationUpdateAction`, ob MacroDroid Magic Text in Shell-Skripten
ersetzt (Beispiel in BespredeL nutzt `{lv=…}` in Shell-Skripten → wahrscheinlich ja), `cmd notification post` aus Shizuku.

**Testlauf v2.2 (27.10.2026, 12:14 + 12:15, zu Hause):** nichts gebucht. Bestätigt: `ForceLocationUpdateAction`
importiert und liefert den Standort (±60–83 m, 47 m von „Zuhause"), Magic Text `{last_loc_lat}`/`{last_loc_long}` wird im
Shell-Skript ersetzt, `screencap` und `cmd notification post` aus Shizuku klappen. Benachrichtigung: „Kein Standort" –
tatsächlich lieferte die `awk`-Rechnung am Pixel nichts (kein `2-nach-tipp.png` → Tipp-Zweig nie erreicht). Die
Textdateien (`log.txt`, `ergebnis.txt`) existieren, die Dateien-App zeigt sie aber nicht an (nur die PNGs).

### v2.3 (27.10.2026) – Rechnung ohne awk, Ergebnis in der Benachrichtigung

- Entfernung per Shell-Ganzzahl (Android `mksh`, 32 Bit): Grad → Mikrograd, 1 µ° Breite = 0,1112 m, Länge bei 46,5° =
  0,0766 m, Wurzel per Newton; mehr als 0,2° Abstand gilt als „über 20 km" (Überlaufschutz). Lokal in `mksh` getestet:
  zu Hause 46 m → HO, Le Brassus 0 m / Le Chenit 13 m → SS, Crissier 6 m → HUB, 857 m → nichts, Zürich → nichts,
  Magic Text nicht ersetzt / leer → „Kein Standort".
- Meldungen getrennt: „Kein Standort" (Koordinaten ungültig) / „Ort nicht berechnet" / „Kein bekannter Ort".
- Endbenachrichtigung: Ergebnis, Entfernung zu jedem Ort, die letzten Shell-Fehlermeldungen (`exec 2>>log.txt`; alle
  `cmd`-artigen Befehle leiten selbst nach `/dev/null` bzw. in `$(…)` um) und der Log-Ordner → Screenshot genügt.

**Test v2.3 (27.10.2026, 12:56):** ✅ „HOME OFFICE gedrueckt (Zuhause, 19 m, rc=0)", Kalender zeigt „12:57 →" (Entrée Home
Office). Logging bleibt bis zum ersten Lauf vor Ort (Sur Site / Hub), dann v2.4 mit `DEBUG=0`.

## Lese-Test v1 (27.10.2026) – kann MacroDroid „Heures du jour" von der Seite lesen?

Für Stempel v3 (Vorschlag per Benachrichtigung, Endzeit 8 h 25): Arbeitszeit am liebsten von der Seite lesen (Christof 1b),
sonst zählt das Makro selbst (1a). `Lese-Test v1` öffnet die Seite und prüft per Bedienungshilfe (`CheckTextOnScreenAction`,
JSON aus BespredeL) „Heures du jour", „HOME OFFICE" und einen Zeitwert (Regex) → `lt_heures`/`lt_knopf`/`lt_zeit`. Tippt nichts.
Die Aktion „Bildschirminhalt lesen" (Wörterbuch `seite`, View-ID → Text) ist in keinem Beispiel als JSON zu finden →
Christof hängt sie von Hand ans Ende an; `Lese-Test Anzeige v1` zeigt danach alles im Log, als Benachrichtigung und in
`Download/lesetest.txt`. Unsicher: Webseiten-Elemente haben oft keine View-ID → evtl. fehlen sie im Wörterbuch.

**Ergebnis Lese-Test v1 (27.10.2026, 13:51):** ✅ MacroDroid liest die Webseite. `CheckTextOnScreenAction` → alle drei True.
„Bildschirminhalt lesen" (mit Positionen, Force Refresh) liefert ein Wörterbuch `index:NN` → `text`, `bounds`, `centerX/Y`, u. a.:
`SORTIE HOME OFFICE` (Mitte 113,435; „ENTRÉE …" = ausgestempelt, „SORTIE …" = eingestempelt), `SUR SITE` (69,568), `HUB` (172,568),
`4:44Heures du jour` (nur abgeschlossene Blöcke), `5:38Temps de présence estimé` (laufende Arbeitszeit inkl. aktuellem Block),
Agenda-Zeiten (07:41, 07:42, 12:21, 12:57 …). Die `index:NN` sind nicht stabil → im Skript nach Text suchen, nicht nach Index.
Die Ausgabe enthält die URL mit Login-Ticket → nie ins Repo.

## Stempeln v3 (27.10.2026) – Vorschläge per Benachrichtigung, Endzeit 8 h 25

Vier Makros (private Fassungen mit Orten nur in Christofs Google Drive; Skript-Quellen in `stempeln-v3-skripte/`,
zusammengesetzt: `kopf.sh` [+ `seite.sh`] + Teil):

| Makro | Auslöser | Was |
|---|---|---|
| `Stempeln v3` | leer / Knopf „Stempeln" | wartet bis 60 s aufs Entsperren, Standort, Seite 2× öffnen, **Bildschirminhalt lesen** → Stand (ENTRÉE/SORTIE, „Temps de présence estimé", Agenda-Zeiten), Knopf-Position von der Seite; kein Doppel-Stempeln (Vorschlag EIN/AUS ≠ Stand → nichts tippen); unbekannter Ort → 60 s selbst tippen; neu laden, nochmal lesen → „gebucht?" + Endzeit; schreibt `Download/stempeln/zustand.txt` |
| `Stempel-Waechter v3` | alle 5 min (nur wenn `stempel_aktiv`), 06:30 an, 19:30 aus, 2× Benachrichtigung von „Shell" | Standort + Skript: Mo–Fr, VD-Feiertage (Ostern-Formel, Bettagsmontag), `frei_bis`; Regeln: Di/Fr ab 7:30 zu Hause, Mo/Mi/Do ab 8:15 noch zu Hause (bis 11:00), Ankunft Büro (sofort, ab 8:00 alle 15 min), Pause ≥ 35 min (Ausstempeln 11:00–14:30, bis 3 h), 8 h 25 voll, Büro verlassen (> Radius + 200 m). Skript meldet per Shell-Benachrichtigung „Stempel-Vorschlag · …" bzw. „Stempel-Ruhe · …" → Benachrichtigungs-Auslöser im selben Makro zeigt MacroDroid-Benachrichtigung mit [Stempeln] [Spaeter] bzw. setzt `stempel_aktiv` = aus |
| `Stempeln Spaeter v3` | Knopf „Spaeter" | 15 min Ruhe (`spaeter.txt`) |
| `Stempeln Frei v3` | leer | fragt `frei_bis` (TT.MM.JJJJ), bis dahin Ruhe |

Warum der Umweg über eine Shell-Benachrichtigung: Für „Shell-Ausgabe in Variable" und für Geofence-Auslöser gibt es kein
bekanntes JSON; Benachrichtigungs-Auslöser, Benachrichtigung mit Knöpfen (startet Makro per GUID), Intervall/Uhrzeit,
TriggerThatInvoked und Löschen von Benachrichtigungen stammen aus Beispielen (BespredeL), „Bildschirminhalt lesen" aus
Christofs Export. Lokal in `mksh` getestet (Seite parsen mit echter Lese-Test-Ausgabe; 5 Stempel- und 20 Wächter-Fälle).
Unsicher: Import der neuen Bausteine, ob `{notification}` den Text liefert, Akku (Standort alle 5 min 06:30–19:30).
Ab 1. Lauf im Büro: `DEBUG=0` (dann keine Screenshots/Seiten-Dumps mehr).

**Erster Lauf v3 (27.10.2026, 15:15–15:25):** Wächter läuft (Intervall, Standort ±60–100 m, Skript), schlägt aber
korrekt nichts vor: Home-Office-Vorschlag nur bis 11:00, und das Stempeln von 12:57 (mit v2.3) kennt er nicht
(kein `zustand.txt`) → kein „8 h 25 voll". Darum:

### Stempeln Stand v3 (v3.1-Skripte, 27.10.2026)

Liest die Seite, **tippt nichts**: Benachrichtigung „Eingestempelt 12:57 · heute 5:38 · fertig um 16:38" und
`zustand.txt` für den Wächter (nach Stempeln am PC, per Badge oder direkt auf der Seite; Christof stempelt im Büro
teils so). Ist man eingestempelt und der Ort unbekannt, nimmt `ende.sh` (v3.1) den Ort aus dem Standort (für „Büro
verlassen"). `stand-start.sh` = `start.sh` mit anderem Text. `Stempeln v3` in Drive hat noch `ende.sh` v3.0
(gleiches Verhalten beim Stempeln). Im Büro tippt das Makro beim Ein- und Ausstempeln „SUR SITE"/„HUB" (Christof: richtig).

**Bürotag 28.10.2026 (Le Brassus):** ✅ Ankunfts-Vorschlag, Einstempeln per Knopf, Meldung „Eingestempelt 07:55 · heute 1:36 · fertig um 16:21".

### v3.1 (28.10.2026)

- **Endzeit mit Mittagspause:** solange heute noch keine Pause (Lücke nach einem Ausstempeln 11:00–14:30, Agenda-Zeiten
  abwechselnd rein/raus) und vor 14:30 → +45 min („fertig um 17:06 (mit 45 min Pause)"); danach zählt die echte Pause.
  „8 h 25 voll" im Wächter bleibt an der echten Arbeitszeit.
- **Schneller:** Standort parallel (keine 8 s Pause), Seite einmal öffnen; `pruef.sh` erkennt Accueil (keine
  Präsenz/kein Knopf) und öffnet dann per `am start` (Shizuku, nach /dev/null) nochmal; nach dem Neuladen dieselbe Prüfung.
  **Chrome wird nicht mehr beendet** (Vermutung: Beenden kostet die Anmeldung → Horoquartz landet nach dem Login auf Accueil).
  Erwartet ~20–25 s statt ~40 s.
- **Logging aus** (`DEBUG=0`): kein Ordner/Screenshot je Lauf, letzter Lauf in `Download/stempeln/lauf/` (log.txt).
- Neu: `Stempeln v3.1`, `Stempel-Waechter v3.1` (Knopf → Stempeln v3.1, sonst unverändert), `Stempeln Stand v3.1`;
  `Stempeln Spaeter v3` und `Stempeln Frei v3` bleiben.

### Wächter v3.2 (28.10.2026)

Christof: Erinnerung zur Mittagspause **ab 11:45**, alle 15 min bis ausgestempelt. Regel: eingestempelt, letzte Buchung vor
11:00 (also noch keine Pause), 11:45–14:30 → „Mittagspause (heute …) – ausstempeln?" mit [Stempeln] [Spaeter]. Geht vor
„Büro verlassen", nach „8 h 25 voll". Knopf startet weiterhin `Stempeln v3.1`.

### v3.2 / Wächter v3.3 (28.10.2026)

- **Benachrichtigungsleiste schließen:** `start.sh`/`stand-start.sh` rufen nach dem Entsperren `cmd statusbar collapse`
  (Shizuku, nach /dev/null) – Knopf in der Benachrichtigung ließ die Leiste offen. Unsicher, ob das unter Android 17 aus
  Shizuku wirkt (Log `Leiste schliessen rc=…`).
- **Falsche Erinnerung → richtige Frage:** passt der Vorschlag nicht zum Stand (z. B. „ausstempeln?", aber schon per Badge
  ausgestempelt), tippt `haupt.sh` nichts (`A_KORR`), `ende.sh` zeigt „Die Erinnerung passte nicht zu deinem Stand" und
  sofort einen neuen Vorschlag „Du bist ausgestempelt seit 12:21 – einstempeln?" mit Knopf.
- **Ausstempeln mit dem Knopf vom Einstempeln** (`Z_ORT` aus `zustand.txt`, Standort egal). Christof: Ortswechsel =
  dort ausstempeln, woanders einstempeln.
- **Le Brassus Radius 1200 m** (Kantine ~750 m entfernt; nur in den privaten Fassungen).
- **Shizuku-Prüfung 06:30** (Wächter v3.3): `shizuku_ok` = aus, Shell-Skript `ok.sh` meldet „Stempel-OK · Shizuku laeuft",
  der Benachrichtigungs-Auslöser setzt `shizuku_ok` = an; nach 20 s noch aus → Warnung „Shizuku laeuft nicht".
- Neu: `Stempeln v3.2`, `Stempeln Stand v3.2`, `Stempel-Waechter v3.3` (Knopf → Stempeln v3.2). `Stempeln Spaeter v3`
  und `Stempeln Frei v3` bleiben. Globale Variable neu: `shizuku_ok`.

### v3.3 / Wächter v3.4 / Spaeter + Frei v3.1 (29.10.2026)

- **Knopf und Ort in jeder Erinnerung und Meldung:** „→ SUR SITE (Le Brassus)“ bzw. „(Le Chenit)“, „→ HUB“, „→ HOME OFFICE“,
  „→ unbekannter Ort, du tippst selbst“. `zustand.txt` merkt zusätzlich `Z_NAME` (beide Büros sind SUR SITE), Funktionen
  `label`/`wahl` in `kopf.sh`.
- **Ort geändert zwischen Erinnerung und Tippen:** Vorschlag speichert `V_LABEL`; passt der Knopf jetzt nicht mehr, tippt
  `haupt.sh` nichts, `ende.sh` stellt sofort die richtige Frage.
- **Uhrzeit vorne** in jeder Benachrichtigung (`note`). Vorschlag: Text im **Titel** der Shell-Benachrichtigung, Kennung
  „Stempel-Vorschlag“ im Text → MacroDroid zeigt `{not_title}` (kein Vorspann mehr).
- **Mittagspause:** nach dem Ausstempeln „Pause 30 min bis …, 45 min bis …“. **8 h 25:** „8 h 25 voll seit 16:37 (+15 min)“.
- **Spaeter v3.1:** Bestätigung „Spaeter: Mittagspause – Erinnerung wieder ab 12:36“, verschwindet nach 6 s (Löschen per Text).
- **Schneller:** keine Log-/Toast-Aktionen, kein Lauf-Log (`DEBUG=0` → `t`/`w` schreiben nichts), Prüfung „Stempelseite da?“
  per `CheckTextOnScreenAction` („HOME OFFICE“ → Variable `seite_ok`); nur wenn nicht da: `neu.sh` (nochmal öffnen) und
  zweites Lesen. Wartezeiten nach dem Öffnen bleiben 6 s (sonst könnte die alte Seite gelesen werden).
- Beschreibungen der Makros aus Nutzersicht (was läuft, was Christof tun muss und was nicht).
- Lokal getestet (mksh, 15 Fälle): Ankunft, Home Office, Mittagspause, 8 h 25, Ortswechsel-Korrektur, Kantine weit weg,
  Stand nach Badge (Le Chenit), alter `zustand.txt` ohne `Z_NAME`, Spaeter + Wiederholung, Wochenende.
- Unsicher: `{not_title}` (aus Beispielen, nicht am Pixel getestet), `CheckTextOnScreenAction` mit globaler Variable.

## Stempeln v4 (2.11.2026) – dünne Makros, Skripte per „Stempeln Update“

Anlass 2.11.2026: (1) Christof stempelte mittags am Badge wieder ein → der Wächter hielt ihn für „in der Pause“, „8 h 25
voll“ kam erst nach einem Stand-Lauf (17:15). (2) 17:22 [Stempeln] auf „ausstempeln?“: die Seite zeigte „ENTRÉE“, obwohl
eingestempelt – die **Agenda war leer** (Stunden-Felder ohne Text), die Seite also nur halb geladen. Die Sicherung tippte
nichts, stellte aber die falsche Gegenfrage „einstempeln?“. (3) Das Importieren von fünf Makros bei jeder Änderung nervt.

- **Aufbau:** Die Makros sind nur noch Hüllen (`. $S/kopf.sh; . $S/<name>.sh`) und reichen Magic Text als Variablen
  durch (`LAT`, `LON`, `FREI`, `TEXT`; Seite → `Download/stempeln/seite-roh.txt`, ohne `ticket=`). Die Logik liegt in
  `Download/stempeln/skripte/` auf dem Handy. Quellen: `stempeln/skripte/*.sh`, Paket `stempeln/paket.txt`
  (`#STEMPELPAKET v`, `#DATEI name`, …, `#ENDE v`), gebaut mit `stempeln/paket-bauen.py`.
- **`Stempeln Update v4`** (nur wenn Christof es startet): HTTP-Anfrage (`HttpRequestAction`, Vorlage aus
  BespredeL/MacroDroid, Antwort → Variable `stempel_paket`) auf
  `https://raw.githubusercontent.com/obitusde/cupra-ziel/main/macrodroid/test/stempeln/paket.txt`, Shell prüft
  Anfang/Ende/Version, packt nach `skripte-neu` aus und tauscht (alte Fassung → `skripte-alt`). GitHub nicht erreichbar:
  beim ersten Mal die eingebaute Fassung, sonst bleibt alles. Die **private** Fassung (Drive) schreibt außerdem
  `Download/stempeln/orte.txt` (Ortsliste, nie im Repo; `kopf.sh` liest sie).
- **Seite fertig geladen?** Nach dem Öffnen `CheckTextOnScreenAction` „23:00“ (Stundenzahl der Agenda) → `agenda_ok`,
  bis 8× 3 s warten. `stand()` setzt `AGENDA`/`HALB`; ohne Agenda gilt der Knopf-Text nicht → nichts tippen, Meldung
  „Stempelseite nicht fertig geladen“, Zustand bleibt.
- **Keine Gegenfrage mehr:** War Christof schon gestempelt oder passt der Ort nicht mehr, tippt `haupt.sh` nichts und
  `ende.sh` meldet „… – Stand gemerkt“ (Zustand aktualisiert, Vorschlag gelöscht).
- **Wächter v4:** ab 07:30 (Shizuku-Prüfung 07:30, ohne Skriptdatei), Knöpfe **[Stempeln] [Schon gestempelt] [Spaeter]**
  ([Schon gestempelt] = `Stempeln Stand v4`). „Pause vorbei“ nur bis 60 min nach dem Ausstempeln, danach einmal
  „Seit 12:02 ausgestempelt (63 min) – schon wieder eingestempelt? Sonst jetzt einstempeln?“ (nur an bekanntem Ort).
  Wiederholung vergleicht jetzt auch den Grund.
- **Knopf ohne Ortsname:** „→ SUR SITE“ (Christof 2.11.).
- Lokal getestet (mksh): Installation (GitHub ok / leer / halb), Hülle ohne Skripte, Ankunft, Home Office, Mittagspause,
  8 h 25, Ortswechsel, Kantine, schon ausgestempelt per Badge, halb geladene echte Seite vom 2.11. (Stempeln + Stand),
  Pause vorbei + Nachfrage, Spaeter, Wochenende, frei.
- Unsicher: HTTP-Antwort (~17 KB) in einer Variable und dann per Magic Text ins Shell-Skript; drei Knöpfe in der
  Benachrichtigung (Platz); „23:00“ als Ladekennzeichen (in allen echten Seiten-Dumps vorhanden, in der halb geladenen nicht).

### v4.1 (3.11.2026) – Zuhause ab 07:15, Knopf [Heute nicht]

- **Wächter v4.1** wacht um **07:00** auf (Shizuku-Prüfung 07:00), Erinnerungen erst ab `START` (in `kopf.sh`, 435 = 07:15)
  → spätere Zeitänderungen ab 07:00 gehen ohne Import, nur über „Stempeln Update“. Di/Fr zu Hause ab 07:15.
- **Zuhause-Erinnerung** (Di/Fr ab 07:15, Mo/Mi/Do ab 08:15) mit eigener Kennung „Stempel-Heimfrage“ → eigene Benachrichtigung
  **[Stempeln] [Schon gestempelt] [Heute nicht]**. Neues Makro `Stempeln Heute nicht v4.1` (`heutenicht.sh`) schreibt
  `heute-nicht.txt` → heute keine Zuhause-Erinnerung mehr; Büro-Erinnerungen bleiben. Zuhause-Erinnerungen kommen ohnehin
  nur, solange der Standort zu Hause ist.
- Skripte v4.1 brauchen Wächter v4.1 (sonst keine Zuhause-Erinnerung, der alte Wächter kennt „Stempel-Heimfrage“ nicht).
- Lokal getestet: 07:10 nichts, 07:15 Heimfrage, Heute nicht → nichts mehr zu Hause, 08:10 im Büro Ankunft; Mo 08:15
  Heimfrage; Wochenende-Ruhe um 07:00.

### Skripte v4.2 (5.11.2026) – Handy nach dem Tipp gesperrt

- Anlass 5.11. 17:06: Ausstempeln HUB per Knopf hat gebucht, aber beim Nachlesen war der Sperrbildschirm zu sehen
  (`seite-nach` = Sperrbildschirm) → „HUB getippt, aber danach: Stempelseite nicht gelesen“, Stand nicht gemerkt →
  Wächter 17:20 „8 h 25 voll – ausstempeln?“.
- `neu.sh` weckt den Bildschirm (`input keyevent KEYCODE_WAKEUP`), wartet bis 8 s aufs Entsperren („Bitte entsperren –
  ich pruefe noch die Stempelseite“), öffnet dann die Seite neu (Hülle: 20 s Zeitlimit, danach noch bis 24 s Ladeprüfung).
  `gesperrt()` jetzt in `kopf.sh`.
- `ende.sh`: nach einem Tipp nicht nachlesbar (gesperrt / halb geladen / nicht gelesen) → Stand wird angenommen
  (Gegenteil von vorher, Präsenz von vorher, letzte Buchung = jetzt, Pause nach Mittag), Meldung
  „HUB getippt – Handy war gesperrt, Buchung nicht nachgeprueft. Gemerkt: …“ → Wächter erinnert nicht falsch.
  `haupt.sh` schreibt dafür `A_PRES A_LETZTE A_BUCH A_PAUSE` in `aktion.txt`.
- Nur Skripte → „Stempeln Update“, kein Import. Lokal getestet mit dem echten Sperrbildschirm-Dump (Aus- und Einstempeln).

### Wächter v4.2 + Skripte v4.3 (7.11.2026) – Standort nur morgens, Wochenende ganz still

- Anlass: Samstagabend griff MacroDroid ständig auf den Standort zu (Wächter holte 07:00–19:30 alle 5 min den Ort, jeden
  Tag; das Ruhe-Signal kam nur einmal pro Tag).
- **Wächter v4.2:** 07:00- und 19:30-Auslöser nur **Mo–Fr** (`m_daysOfWeek` Mo…So). Der 5-min-Lauf holt **keinen**
  Standort mehr; braucht das Skript ihn, schickt es das Signal **„Stempel-Ort“** → neuer Zweig: Standort holen, 8 s,
  Skript mit `ORTLAUF=1`.
- **Skripte v4.3** (Christof 7.11.):
  - Standort nur bis zum ersten Einstempeln: Di/Fr ab 07:15 (`START`), Mo/Mi/Do ab 07:45 (`START2`), alle 15 min
    (`ort-zeit.txt`), bis 11:00 (`ORTBIS`). Zu Hause → Heimfrage, Büro → „Angekommen“, unterwegs → nichts.
  - Danach kein Standort mehr: es gilt der Ort vom Einstempeln (`wahl` nimmt `Z_ORT` für beide Richtungen, auch im
    Stempeln-Makro). „Büro verlassen“ entfällt.
  - Pause vorbei nach 30 min (HOME OFFICE, HUB) bzw. 45 min (SUR SITE, `PAUSE_SS`/`PAUSE_KURZ`); Nachfrage nach 60 min ohne Ort.
  - Ruhe-Signal bei jedem Lauf (verlorenes Signal wird nachgeholt). Alte Hülle v4.1 (immer mit Standort) läuft weiter.
- Unsicher: Reihenfolge von `m_daysOfWeek` (Mo = erster Eintrag, laut ruby-macrodroid).

### Stempeln v4.1 / Stand v4.1 / Wächter v4.3 + Skripte v4.4 (11.11.2026) – falsche Seite (`/ux/home`)

- Anlass 11.11. 07:01: Chrome landete statt auf `…/ux/clock` auf der Startseite `…/ux/home` (vermutlich nach neuer
  Anmeldung). Die Prüfung „HOME OFFICE sichtbar“ war trotzdem erfüllt (die Startseite zeigt den Text auch) → nicht neu
  geöffnet, 30 s auf die Agenda gewartet, nichts getippt; Christof stempelte von Hand. Am 10.11. stand dort kein „HOME
  OFFICE“ → neu geöffnet → ok. MacroDroid-Protokoll kürzt Skript-Einträge nach ~2900 Zeichen.
- **Hüllen Stempeln v4.1 / Stand v4.1:** keine Text-Prüfung „HOME OFFICE“ mehr. Warten auf die Agenda („23:00“, 6 s + 3×4 s);
  fehlt sie → Bildschirm lesen → **`pruef.sh`** (neu): Adresse aus der Adressleiste (`seite_art`) ≠ `ux/clock` oder
  gesperrt → Bildschirm wecken, bis 12 s aufs Entsperren warten, Stempelseite neu öffnen; Stempelseite lädt nur langsam →
  nichts tun; dann bis 5×5 s weiter warten. Seiteninhalt jetzt mit `sed 's/ticket=…/ticket=-/'` statt `grep -v ticket=`
  (die Adresse bleibt lesbar, das Login-Ticket nicht).
- **Skripte v4.4:** `haupt.sh` tippt nur auf der richtigen, fertig geladenen Seite (Adresse `ux/clock`, ohne Adresse:
  Abschnitt „TIMBRER AVEC MOTIF“); sonst `A_AKTION=nochmal` (+ `A_WUNSCH`, `A_VLABEL`). **Zweiter Versuch** in `ende.sh`
  auf der neu geöffneten Seite: Entscheidung + Tipp aus **`tipp.sh`** (gemeinsam mit `haupt.sh`), danach Stand angenommen
  („Stempelseite kam erst beim zweiten Oeffnen, Buchung nicht nachgeprueft“), 3 s Pause vor dem Startbildschirm.
  Meldungen nennen die falsche Seite („Chrome zeigte webquartz/ux/home“).
- **Wächter v4.3** = v4.2 mit Knöpfen auf Stempeln v4.1 / Stand v4.1.
- Lokal getestet mit der echten Stempelseite vom 11.11. und einer nachgebauten `/ux/home`: Startseite → zweiter Versuch
  tippt; zweimal Startseite → nichts; pruef (Startseite/gesperrt → neu öffnen, ladende Stempelseite → nicht); Stand auf
  Startseite; zweiter Versuch, schon ausgestempelt. Alte Tests unverändert (bis auf die neuen Meldungen).
