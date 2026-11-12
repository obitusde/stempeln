# Waechter: Mo-Fr alle 5 min (07:00-19:30) - Arbeitstag? Stand + Uhrzeit -> Erinnerung oder nichts
# Standort nur morgens bis zum Einstempeln (ab START/START2, alle 15 min): ORTLAUF=0 -> Signal "Stempel-Ort",
# der Waechter holt dann den Standort und startet das Skript mit ORTLAUF=1 nochmal. Nach dem Einstempeln: Ort vom Einstempeln.
jetzt
W=$B/waechter.log
w() { [ $DEBUG = 1 ] && echo "$(date '+%d.%m. %H:%M') $*" >> $W; }
exec 2>>$B/fehler.log
# laeuft gerade Stempeln?
[ -f $B/lock.txt ] && [ $(( EPOCH - $(cat $B/lock.txt) )) -lt 240 ] && { w "Stempeln laeuft - nichts"; exit 0; }
# Arbeitstag? Mo-Fr, kein Waadtlaender Feiertag, nicht frei
tagnr() { y=$1; m=$2; d=$3; [ $m -le 2 ] && y=$(( y - 1 )); era=$(( y / 400 )); yoe=$(( y - era*400 ))
  if [ $m -gt 2 ]; then mp=$(( m - 3 )); else mp=$(( m + 9 )); fi
  doy=$(( (153*mp + 2)/5 + d - 1 )); doe=$(( yoe*365 + yoe/4 - yoe/100 + doy )); echo $(( era*146097 + doe - 719468 )); }
feiertag() { y=$1; n=$2
  a=$(( y % 19 )); b=$(( y / 100 )); c=$(( y % 100 )); d=$(( b / 4 )); e=$(( b % 4 )); f=$(( (b + 8) / 25 )); g=$(( (b - f + 1) / 3 ))
  h=$(( (19*a + b - d - g + 15) % 30 )); i=$(( c / 4 )); k=$(( c % 4 )); l=$(( (32 + 2*e + 2*i - h - k) % 7 )); m=$(( (a + 11*h + 22*l) / 451 ))
  om=$(( (h + l - 7*m + 114) / 31 )); od=$(( (h + l - 7*m + 114) % 31 + 1 )); os=$(tagnr $y $om $od)
  s1=$(tagnr $y 9 1); wd=$(( (s1 + 4) % 7 )); bettag=$(( s1 + (7 - wd) % 7 + 15 ))
  for f in $(tagnr $y 1 1) $(tagnr $y 1 2) $(( os - 2 )) $(( os + 1 )) $(( os + 39 )) $(( os + 50 )) $(tagnr $y 8 1) $bettag $(tagnr $y 12 25); do
    [ $f = $n ] && return 0; done; return 1; }
HN=$(tagnr $JJ $MM $TT)
FN=0
case "$FREI" in *[0-9].*[0-9].*[0-9]*) fd=${FREI%%.*}; r=${FREI#*.}; fm=${r%%.*}; fy=${r#*.}; fd=${fd#0}; fm=${fm#0}; [ ${#fy} = 2 ] && fy=20$fy; FN=$(tagnr $fy $fm $fd) ;; esac
GRUND=""
[ $WT -gt 5 ] && GRUND="Wochenende"
feiertag $JJ $HN && GRUND="Feiertag"
[ $HN -lt $FN ] && GRUND="frei bis $FREI"
# Ruhe-Signal bei jedem Lauf (schaltet den Waechter bis morgen aus; ging es verloren, kommt es beim naechsten Lauf wieder)
ruhe() { echo "$HEUTE" > $B/ruhe.txt; w "Ruhe: $1"; note "Stempel-Ruhe · $1" "Stempel-Ruhe" ruhe; exit 0; }
# alter Waechter (v4.1) holt den Standort immer selbst
[ -z "$ORTLAUF" ] && { ORTLAUF=0; [ -n "$LAT" ] && ORTLAUF=1; }
[ -n "$GRUND" ] && ruhe "$GRUND"
# Stand vom letzten Stempeln
Z_DATUM=""; Z_STATUS=OUT; Z_PRES=0; Z_LESE=$EPOCH; Z_LETZTE=""; Z_BUCH=0; Z_ORT=""; Z_NAME=""
[ -f $B/zustand.txt ] && . $B/zustand.txt
[ "$Z_DATUM" != "$HEUTE" ] && { Z_STATUS=OUT; Z_PRES=0; Z_LESE=$EPOCH; Z_LETZTE=""; Z_BUCH=0; Z_ORT=""; Z_NAME=""; }
P=$Z_PRES; [ "$Z_STATUS" = IN ] && P=$(( Z_PRES + (EPOCH - Z_LESE) / 60 ))
[ "$Z_STATUS" = OUT ] && [ $P -ge $SOLL ] && ruhe "Feierabend ($(hm $P))"
# Einstempel-Erinnerung ab START (Di/Fr) bzw. START2 (Mo/Mi/Do)
AB=$START2; { [ $WT = 2 ] || [ $WT = 5 ]; } && AB=$START
[ $NOW -lt $START ] && exit 0
# spaeter?
[ -f $B/spaeter.txt ] && [ $EPOCH -lt $(cat $B/spaeter.txt) ] && { w "spaeter - nichts"; exit 0; }
ZIEL=""; MSG=""; GRUND=""; WDH=15; TYP=""; DIST=""
if [ "$Z_STATUS" = IN ]; then
  if [ $P -ge $SOLL ]; then ZIEL=AUS; GRUND="8 h 25 voll"; UE=$(( P - SOLL ))
    MSG="8 h 25 voll seit $(hm $(( (NOW - UE + 1440) % 1440 )))"; [ $UE -ge 5 ] && MSG="$MSG (+$UE min)"; MSG="$MSG - ausstempeln?"
  elif [ $NOW -ge 705 ] && [ $NOW -lt 870 ] && [ $(minuten "${Z_LETZTE:-0:00}") -lt 660 ]; then
    ZIEL=AUS; GRUND="Mittagspause"; MSG="Mittagspause (heute $(hm $P)) - ausstempeln?"
  fi
elif [ "$Z_BUCH" = 0 ]; then
  # noch nicht eingestempelt: Standort noetig, nur morgens und hoechstens alle 15 min
  [ $NOW -ge $AB ] && [ $NOW -lt $ORTBIS ] || exit 0
  if [ $ORTLAUF = 0 ]; then
    OZ=$(cat $B/ort-zeit.txt 2>/dev/null); [ -n "$OZ" ] && [ $(( EPOCH - OZ )) -lt 840 ] && { w "Ort erst wieder in 15 min"; exit 0; }
    echo $EPOCH > $B/ort-zeit.txt; w "Standort anfordern"
    note "Stempel-Ort · Standort holen" "Stempel-Ort" ort; exit 0
  fi
  echo $EPOCH > $B/ort-zeit.txt
  ort "$LAT" "$LON"
  DA=""; [ -n "$TYP" ] && [ "$DIST" -le "$RAD" ] && DA=$TYP
  # [Heute nicht] in der Zuhause-Erinnerung: heute keine Zuhause-Erinnerung mehr
  [ "$DA" = HO ] && [ "$(cat $B/heute-nicht.txt 2>/dev/null)" = "$HEUTE" ] && DA=""
  case "$DA" in
    SS|HUB) ZIEL=EIN; GRUND="Ankunft"; MSG="Angekommen - einstempeln?" ;;
    HO) ZIEL=EIN; GRUND="Home Office"
        if [ $WT = 2 ] || [ $WT = 5 ]; then MSG="Zu Hause - einstempeln?"; else MSG="Noch zu Hause - heute Home Office? Einstempeln?"; fi ;;
  esac
else
  # nach dem Ausstempeln in der Mittagszeit: Pause vorbei (SUR SITE 45 min, sonst 30 min), nach 60 min einmal nachfragen
  LZ=$(minuten "${Z_LETZTE:-0:00}"); PZ=$PAUSE_KURZ; [ "$Z_ORT" = SS ] && PZ=$PAUSE_SS
  if [ $LZ -ge 660 ] && [ $LZ -le 870 ] && [ $NOW -ge $(( LZ + PZ )) ] && [ $NOW -lt $(( LZ + 60 )) ]; then
    ZIEL=EIN; GRUND="Pause vorbei"; MSG="Pause seit $(( NOW - LZ )) min (heute $(hm $P)) - wieder einstempeln?"
  elif [ $LZ -ge 660 ] && [ $LZ -le 870 ] && [ $NOW -ge $(( LZ + 60 )) ]; then
    # vielleicht am Badge eingestempelt - einmal nachfragen
    ZIEL=EIN; GRUND="Nachfrage"; WDH=9999
    MSG="Seit $Z_LETZTE ausgestempelt ($(( NOW - LZ )) min) - schon wieder eingestempelt? Sonst jetzt einstempeln?"; fi
fi
w "Stand=$Z_STATUS heute=$(hm $P) Ort=${TYP:--} ${DIST:-} -> ${ZIEL:-nichts}"
[ -z "$ZIEL" ] && exit 0
wahl $ZIEL
MSG="$MSG → $LABEL"
V_ZIEL=""; V_ZEIT=0; V_DATUM=""; V_LABEL=""; V_GRUND=""
[ -f $B/vorschlag-letzter.txt ] && . $B/vorschlag-letzter.txt
# gleiche Frage (Richtung, Knopf und Grund) erst nach WDH Minuten wiederholen
if [ "$V_ZIEL" = "$ZIEL" ] && [ "$V_LABEL" = "$LABEL" ] && [ "$V_GRUND" = "$GRUND" ] && [ "$V_DATUM" = "$HEUTE" ] && [ $(( EPOCH - V_ZEIT )) -lt $(( WDH * 60 - 120 )) ]; then exit 0; fi
{ echo "V_ZIEL=$ZIEL"; echo "V_ZEIT=$EPOCH"; echo "V_DATUM=$HEUTE"; echo "V_LABEL='$LABEL'"; echo "V_GRUND='$GRUND'"; } > $B/vorschlag-letzter.txt
cp $B/vorschlag-letzter.txt $B/vorschlag.txt
w "Vorschlag: $MSG"
if [ "$GRUND" = "Home Office" ]; then signal "$MSG" Stempel-Heimfrage; else signal "$MSG"; fi
