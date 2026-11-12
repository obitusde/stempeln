# Stempelseite noch nicht fertig (Agenda fehlt) - die Huelle ruft das bis zu 8x auf (R=1..8), die Wartezeiten stehen HIER (aendern ohne Import)
# Handy gesperrt / Chrome sicher auf einer anderen Seite (Adresse z. B. .../ux/home) -> sofort Stempelseite neu oeffnen (hoechstens 3x)
# keine Adresse lesbar und keine Stempelseite: erst ab der 3. Pruefung neu oeffnen (sonst stoert es eine langsam ladende Seite)
# Stempelseite da, laedt nur langsam -> kurz warten
lauf
R=${R:-9}
cp $B/seite-roh.txt "$D/seite-pruef$R.txt" 2>/dev/null
seite_art "$D/seite-pruef$R.txt"
NEU=0; GRUND=""
if sperrbild "$D/seite-pruef$R.txt" || [ "$(gesperrt)" != 0 ]; then NEU=1; GRUND="gesperrt"
elif [ -n "$U" ] && [ "$SEITE" != clock ]; then NEU=1; GRUND="falsche Seite"
elif [ "$SEITE" != clock ] && [ $R -ge 3 ]; then NEU=1; GRUND="keine Stempelseite erkannt"; fi
if [ $NEU = 1 ]; then
  input keyevent KEYCODE_WAKEUP </dev/null >/dev/null 2>&1
  bis=$(( $(date +%s) + 8 )); n=0
  while [ "$(gesperrt)" != 0 ] && [ $(date +%s) -lt $bis ]; do
    [ $n = 0 ] && note "Bitte entsperren - ich warte noch auf die Stempelseite" "Stempeln" stempeln
    n=$(( n + 1 )); sleep 1
  done
  Z=$(cat "$D/neu-zahl.txt" 2>/dev/null); Z=${Z:-0}
  if [ $Z -lt 3 ]; then
    echo $(( Z + 1 )) > "$D/neu-zahl.txt"
    t "pruef $R: $GRUND (${U:-ohne Adresse}), $n s gesperrt -> Stempelseite neu geoeffnet ($(( Z + 1 )). Mal)"
    am start -a android.intent.action.VIEW -d "https://aptime.cloud-horoquartz.fr/webquartz/ux/clock" -p com.android.chrome --es com.android.browser.application_id macrodroid.stempeln </dev/null >/dev/null 2>&1
    sleep 5
  else
    t "pruef $R: $GRUND (${U:-ohne Adresse}) - schon 3x neu geoeffnet, warte nur"; sleep 3
  fi
else
  t "pruef $R: Seite=$SEITE (${U:-ohne Adresse}) laedt noch - warte"; sleep 2
fi
