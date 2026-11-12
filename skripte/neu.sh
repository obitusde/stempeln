# Stempelseite nicht erkannt (Accueil, noch am Laden oder Bildschirm aus/gesperrt): nochmal oeffnen, 7 s warten
# Huelle bricht nach 20 s ab -> hoechstens 8 s aufs Entsperren warten (danach gibt die Huelle noch bis 24 s fuer die Seite)
lauf
input keyevent KEYCODE_WAKEUP </dev/null >/dev/null 2>&1
bis=$(( $(date +%s) + 8 )); n=0
while [ "$(gesperrt)" != 0 ] && [ $(date +%s) -lt $bis ]; do
  [ $n = 0 ] && note "Bitte entsperren - ich pruefe noch die Stempelseite" "Stempeln" stempeln hinweis
  n=$(( n + 1 )); sleep 1
done
t "neu: Bildschirm geweckt, $n s aufs Entsperren gewartet"
am start -a android.intent.action.VIEW -d "https://aptime.cloud-horoquartz.fr/webquartz/ux/clock" -p com.android.chrome --es com.android.browser.application_id macrodroid.stempeln </dev/null >/dev/null 2>&1
sleep 7
