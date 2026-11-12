# Start von Stempeln / Stand: Lauf-Ordner, Sperre, aufs Entsperren warten, Leiste schliessen
jetzt
echo $EPOCH > $B/lock.txt
D=$B/lauf; rm -rf "$D"; mkdir -p "$D"; echo "$D" > $B/aktuell.txt
L=$D/log.txt; exec 2>>"$L"
t "$TEXT (Skripte v$SKV) - Start"
# gesperrt (z. B. geplantes Stempeln aus der App): Bildschirm wecken
[ "$(gesperrt)" != 0 ] && input keyevent KEYCODE_WAKEUP </dev/null >/dev/null 2>&1
n=0
while [ "$(gesperrt)" != 0 ] && [ $n -lt 60 ]; do
  [ $n = 0 ] && note "Bitte entsperren - dann geht es weiter" "Stempeln" stempeln
  sleep 1; n=$(( n + 1 ))
done
cmd statusbar collapse </dev/null >/dev/null 2>&1
t "entsperrt nach $n s, Leiste zu"
note "$TEXT ..." "Stempeln" stempeln
