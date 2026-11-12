# Stempeln Frei: Bestaetigung (FREI kommt aus der Huelle)
jetzt
case "$FREI" in
  "") rm -f $B/ruhe.txt; note "Keine freien Tage eingetragen - Erinnerungen sind an" "Stempeln" stempeln ;;
  *[0-9].*[0-9].*[0-9]*) rm -f $B/ruhe.txt; note "Frei bis $FREI - bis dahin keine Erinnerungen (ab $FREI wieder)" "Stempeln" stempeln ;;
  *) note "'$FREI' nicht verstanden - bitte als TT.MM.JJJJ eingeben (z. B. 03.11.2026)" "Stempeln" stempeln ;;
esac
