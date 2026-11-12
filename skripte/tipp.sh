# Knopf waehlen und tippen (oder bewusst nicht) - von haupt.sh und (zweiter Versuch) ende.sh geladen
# braucht: STATUS + Knopf-Positionen (stand), Standort (ort), Z_ORT/Z_NAME, WUNSCH/V_LABEL (Erinnerung), ZWEIT=1 im zweiten Versuch
# -> AKTION (tipp|manuell|keine) KORR ERGT KTYP KNAME LABEL
if [ "$STATUS" = IN ]; then wahl AUS; else wahl EIN; fi
t "Wunsch '${WUNSCH:-keiner}' ($V_LABEL), jetzt: $LABEL"
AKTION=keine; KORR=""
if [ "$WUNSCH" = EIN ] && [ "$STATUS" = IN ]; then
  ERGT="Du warst schon eingestempelt - nichts getippt"; KORR=1
elif [ "$WUNSCH" = AUS ] && [ "$STATUS" = OUT ]; then
  ERGT="Du warst schon ausgestempelt - nichts getippt"; KORR=1
elif [ -n "$V_LABEL" ] && [ "$V_LABEL" != "$LABEL" ]; then
  ERGT="Ort passt nicht mehr zur Erinnerung ($V_LABEL, jetzt $LABEL) - nichts getippt. Stimmt $LABEL, starte Stempeln nochmal"; KORR=1
elif [ -z "$KTYP" ]; then
  if [ "$ZWEIT" = 1 ]; then ERGT="Unbekannter Ort - bitte selbst auf der Stempelseite tippen"
  else
    AKTION=manuell
    ERGT="Unbekannter Ort"
    note "Unbekannter Ort - tippe selbst auf der Seite den passenden Knopf. Ich lese in 60 s nach." "Stempeln" stempeln hinweis
    sleep 60
  fi
else
  case "$KTYP" in
    HO)  X=$HOX; Y=$HOY ;;
    SS)  X=$SSX; Y=$SSY ;;
    HUB) X=$HUX; Y=$HUY ;;
  esac
  input tap $X $Y </dev/null >/dev/null 2>&1
  t "Tipp $LABEL bei $X,$Y rc=$?"
  AKTION=tipp; ERGT=$LABEL
fi
