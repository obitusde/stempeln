# Stempeln: Stand lesen, Knopf waehlen, tippen (oder bewusst nicht)
# Seite nicht bereit (falsche Seite, halb geladen, nicht lesbar) -> nichts tippen, ende.sh versucht es auf der neu geoeffneten Seite nochmal
lauf
jetzt
cp $B/seite-roh.txt "$D/seite-vor.txt" 2>/dev/null
elemente "$D/seite-vor.txt" > "$D/elemente-vor.txt"
stand "$D/elemente-vor.txt"
seite_art "$D/seite-vor.txt"
t "Seite vorher: $SEITE ($U) Status=$STATUS Praesenz=$PRES Letzte=$LETZTE Buchungen=$BUCH Agenda=$AGENDA"
ort "$LAT" "$LON"
t "Standort -> $TYP $NAME $DIST m (Radius $RAD)"
# Vorschlag (Knopf in der Erinnerung), gilt 30 min
WUNSCH=""; V_ZIEL=""; V_ZEIT=0; V_LABEL=""
[ -f $B/vorschlag.txt ] && . $B/vorschlag.txt && [ $(( EPOCH - ${V_ZEIT:-0} )) -lt 1800 ] && WUNSCH=$V_ZIEL
[ -n "$WUNSCH" ] || V_LABEL=""
rm -f $B/vorschlag.txt
Z_DATUM=""; Z_ORT=""; Z_NAME=""; [ -f $B/zustand.txt ] && . $B/zustand.txt
[ "$Z_DATUM" = "$HEUTE" ] || { Z_ORT=""; Z_NAME=""; }
if [ "$SEITE" != clock ] || [ "$HALB" = 1 ] || [ "$STATUS" = "?" ] || [ -z "$PRES" ]; then
  AKTION=nochmal; KORR=""; KTYP=""; KNAME=""; ERGT="Stempelseite beim ersten Mal nicht bereit"
else
  ZWEIT=0; . $S/tipp.sh
fi
{ echo "A_AKTION=$AKTION"; echo "A_STATUS=$STATUS"; echo "A_TYP=$KTYP"; echo "A_NAME='$KNAME'"; echo "A_TEXT='$ERGT'"; echo "A_KORR=$KORR"
  echo "A_PRES=$PRES"; echo "A_LETZTE=$LETZTE"; echo "A_BUCH=$BUCH"; echo "A_PAUSE=$PAUSE"
  echo "A_WUNSCH=$WUNSCH"; echo "A_VLABEL='$V_LABEL'"; } > "$D/aktion.txt"
t "Aktion: $AKTION - $ERGT"
# nach einem Tipp kurz warten, bis die Buchung gespeichert ist (danach laedt die Huelle die Seite neu)
[ "$AKTION" = tipp ] && sleep 3
