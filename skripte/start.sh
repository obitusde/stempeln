# Start von Stempeln / Stand: Lauf-Ordner, Sperre, aufs Entsperren warten, Leiste schliessen
jetzt
echo $EPOCH > $B/lock.txt
D=$B/lauf; rm -rf "$D"; mkdir -p "$D"; echo "$D" > $B/aktuell.txt
L=$D/log.txt; exec 2>>"$L"
t "$TEXT (Skripte $SKV, Makro ${MV:-alt}) - Start"
# Version der Makro-Huelle fuer die App merken (Huellen ab 5.0 setzen MV)
[ -n "$MV" ] && echo "$MV" > $B/makro-version.txt
# gesperrt (z. B. geplantes Stempeln aus der App): Bildschirm wecken
[ "$(gesperrt)" != 0 ] && input keyevent KEYCODE_WAKEUP </dev/null >/dev/null 2>&1
n=0
while [ "$(gesperrt)" != 0 ] && [ $n -lt 60 ]; do
  [ $n = 0 ] && note "Bitte entsperren - dann geht es weiter" "Stempeln" stempeln hinweis
  sleep 1; n=$(( n + 1 ))
done
cmd statusbar collapse </dev/null >/dev/null 2>&1
t "entsperrt nach $n s, Leiste zu"
note "$TEXT ..." "Stempeln" stempeln
