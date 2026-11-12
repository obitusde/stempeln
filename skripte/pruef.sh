# Stempelseite nach ~18 s noch nicht fertig (Agenda fehlt): Bildschirminhalt pruefen
# Handy gesperrt / Chrome auf einer anderen Seite (z. B. Startseite .../ux/home nach neuer Anmeldung) -> Stempelseite neu oeffnen
# Stempelseite da, laedt nur langsam -> nichts tun, die Huelle wartet weiter (bis 24 s)
lauf
cp $B/seite-roh.txt "$D/seite-pruef.txt" 2>/dev/null
seite_art "$D/seite-pruef.txt"
NEU=0; [ "$SEITE" = clock ] || NEU=1
input keyevent KEYCODE_WAKEUP </dev/null >/dev/null 2>&1
bis=$(( $(date +%s) + 12 )); n=0
while [ "$(gesperrt)" != 0 ] && [ $(date +%s) -lt $bis ]; do
  [ $n = 0 ] && note "Bitte entsperren - ich warte noch auf die Stempelseite" "Stempeln" stempeln
  n=$(( n + 1 )); NEU=1; sleep 1
done
t "pruef: Seite=$SEITE (${U:-ohne Adresse}), $n s gesperrt, neu oeffnen=$NEU"
if [ $NEU = 1 ]; then
  am start -a android.intent.action.VIEW -d "https://aptime.cloud-horoquartz.fr/webquartz/ux/clock" -p com.android.chrome --es com.android.browser.application_id macrodroid.stempeln </dev/null >/dev/null 2>&1
  sleep 7
fi
