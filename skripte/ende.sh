# Stempeln / Stand: neuen Stand lesen, Buchung pruefen, Zustand fuer den Waechter speichern, Meldung, zum Schluss die App zeigen
# Konnte Stempeln beim ersten Lesen nicht tippen (Seite nicht bereit), wird hier auf der neu geoeffneten Seite getippt (zweiter Versuch)
lauf
jetzt
cp $B/seite-roh.txt "$D/seite-nach.txt" 2>/dev/null
elemente "$D/seite-nach.txt" > "$D/elemente-nach.txt"
stand "$D/elemente-nach.txt"
seite_art "$D/seite-nach.txt"
[ "$SEITE" = clock ] || { STATUS="?"; PRES=""; }
t "Seite nachher: $SEITE ($U) Status=$STATUS Praesenz=$PRES Letzte=$LETZTE Buchungen=$BUCH Pause=$PAUSE Agenda=$AGENDA"
A_AKTION=keine; A_STATUS="?"; A_TYP=""; A_NAME=""; A_TEXT=""; A_KORR=""; A_PRES=""; A_LETZTE=""; A_BUCH=0; A_PAUSE=0; A_WUNSCH=""; A_VLABEL=""
[ -f "$D/aktion.txt" ] && . "$D/aktion.txt"
Z_DATUM=""; Z_ORT=""; Z_NAME=""; [ -f $B/zustand.txt ] && . $B/zustand.txt
[ "$Z_DATUM" = "$HEUTE" ] || { Z_ORT=""; Z_NAME=""; }
ort "$LAT" "$LON"
GUT=0; [ -n "$PRES" ] && [ "$STATUS" != "?" ] && GUT=1
# Konto-Werte der Seite fuer die App (nur bei echt gelesener Seite)
[ $GUT = 1 ] && { echo "K_DATUM=$HEUTE"; echo "K_LESE=$EPOCH"; echo "K_SOLDE=$K_SOLDE"; echo "K_JOUR=$K_JOUR"; echo "K_RATT=$K_RATT"; echo "K_HS=$K_HS"; echo "K_PRES=$PRES"; } > $B/konto.txt
VERM=""
# zweiter Versuch: beim ersten Lesen war die Seite nicht bereit
if [ "$A_AKTION" = nochmal ]; then
  if [ $GUT = 1 ]; then
    WUNSCH=$A_WUNSCH; V_LABEL=$A_VLABEL; ZWEIT=1
    . $S/tipp.sh
    A_AKTION=$AKTION; A_TEXT=$ERGT; A_KORR=$KORR; A_TYP=$KTYP; A_NAME=$KNAME
    if [ "$AKTION" = tipp ]; then
      A_STATUS=$STATUS; A_PRES=$PRES; A_LETZTE=$LETZTE; A_BUCH=$BUCH; A_PAUSE=$PAUSE
      VERM="Stempelseite kam erst beim zweiten Oeffnen"; GUT=0
      sleep 3
    fi
  else
    A_AKTION=keine; A_TEXT="Stempelseite auch beim zweiten Oeffnen nicht bereit - nichts getippt, bitte selbst stempeln"
  fi
fi
# Nach einem Tipp nicht nachlesbar (z. B. Handy gesperrt): Stand annehmen, damit der Waechter nicht falsch erinnert
if [ $GUT = 0 ] && [ "$A_AKTION" = tipp ] && [ -n "$A_PRES" ]; then
  if [ -z "$VERM" ]; then
    if sperrbild "$D/seite-nach.txt" || [ "$(gesperrt)" != 0 ]; then VERM="Handy war gesperrt"
    elif [ "$SEITE" != clock ]; then VERM="Chrome zeigte danach eine andere Seite"
    elif [ "$HALB" = 1 ]; then VERM="Seite nicht fertig geladen"
    else VERM="Seite nicht gelesen"; fi
  fi
  PRES=$A_PRES; LETZTE=$(hm $NOW); BUCH=$(( A_BUCH + 1 )); PAUSE=$A_PAUSE
  if [ "$A_STATUS" = IN ]; then STATUS=OUT
  else STATUS=IN; la=$(minuten "${A_LETZTE:-0:00}")
    [ $la -ge 660 ] && [ $la -le 870 ] && [ $NOW -gt $la ] && PAUSE=$(( PAUSE + NOW - la )); fi
  t "angenommen ($VERM): Status=$STATUS Praesenz=$PRES Letzte=$LETZTE"
fi
if [ -n "$PRES" ] && [ "$STATUS" != "?" ]; then
  # Knopf merken, mit dem eingestempelt wurde (fuers Ausstempeln)
  case "$A_AKTION" in
    tipp) Z_ORT=$A_TYP; Z_NAME=$A_NAME ;;
    manuell) Z_ORT=""; Z_NAME="" ;;
    *) if [ -z "$Z_ORT" ] && [ "$STATUS" = IN ]; then wahl EIN; Z_ORT=$KTYP; Z_NAME=$KNAME; fi ;;
  esac
  { echo "Z_DATUM=$HEUTE"; echo "Z_STATUS=$STATUS"; echo "Z_PRES=$PRES"; echo "Z_LESE=$EPOCH"; echo "Z_LETZTE=$LETZTE"; echo "Z_BUCH=$BUCH"
    echo "Z_ORT=$Z_ORT"; echo "Z_NAME='$Z_NAME'"; } > $B/zustand.txt
  ZL=""; [ -n "$Z_ORT" ] && ZL=" - $(label "$Z_ORT")"
  REST=$(( SOLL - PRES ))
  if [ "$STATUS" = IN ]; then
    PZU=0; [ $PAUSE = 0 ] && [ $NOW -lt 870 ] && PZU=45
    if [ $REST -gt 0 ]; then INFO="heute $(hm $PRES) - noch $(hm $REST) -> fertig um $(hm $(( (NOW + REST + PZU) % 1440 )))"
      [ $PZU -gt 0 ] && INFO="$INFO (mit 45 min Pause)"
    else INFO="heute $(hm $PRES) - 8 h 25 schon voll"; fi
    KOPF="Eingestempelt${LETZTE:+ $LETZTE}$ZL"
  else
    if [ $REST -gt 0 ]; then INFO="heute $(hm $PRES) - noch $(hm $REST) offen"
      L2=$(minuten "${LETZTE:-0:00}")
      [ $L2 -ge 660 ] && [ $L2 -le 870 ] && INFO="$INFO
Pause 30 min bis $(hm $(( L2 + 30 ))), 45 min bis $(hm $(( L2 + 45 )))"
    else INFO="heute $(hm $PRES) - Feierabend"; fi
    KOPF="Ausgestempelt${LETZTE:+ $LETZTE}"
    [ "$A_AKTION" = tipp ] && KOPF="$KOPF - $(label "$A_TYP")"
  fi
  if [ -n "$VERM" ]; then MELD="$A_TEXT getippt - $VERM, Buchung nicht nachgeprueft. Gemerkt:
$KOPF
$INFO"
  elif [ "$A_AKTION" = tipp ] && [ "$STATUS" = "$A_STATUS" ]; then
    MELD="ACHTUNG: $A_TEXT getippt, aber der Stand hat sich nicht geaendert ($KOPF). Bitte pruefen!"
  elif [ "$A_AKTION" = manuell ] && [ "$STATUS" != "$A_STATUS" ]; then MELD="$KOPF (von Hand)
$INFO"
  elif [ -n "$A_KORR" ]; then MELD="$A_TEXT - Stand gemerkt:
$KOPF
$INFO"
  elif [ -n "$A_TEXT" ] && [ "$A_AKTION" != tipp ]; then MELD="$A_TEXT
$KOPF - $INFO"
  else MELD="$KOPF
$INFO"; fi
  # gemerkte Erinnerung passt nicht mehr zum Stand -> Waechter fragt neu
  rm -f $B/vorschlag.txt
else
  if sperrbild "$D/seite-nach.txt"; then MELD="Handy war gesperrt - Stand nicht gelesen, bitte selbst pruefen"
  elif [ "$SEITE" != clock ]; then MELD="Stempelseite nicht erreicht (Chrome zeigte ${U:-eine andere Seite}) - Stand nicht gelesen, bitte selbst pruefen"
  elif [ "$HALB" = 1 ]; then MELD="Stempelseite nicht fertig geladen - Stand nicht gelesen, bitte selbst pruefen"
  else MELD="Stempelseite nicht gelesen - bitte selbst pruefen"; fi
  [ "$A_AKTION" = tipp ] && MELD="$A_TEXT getippt, aber danach: $MELD"
  [ "$A_AKTION" = keine ] && [ -n "$A_TEXT" ] && MELD=$A_TEXT
fi
t "Meldung: $MELD"
note "$MELD" "Stempeln" stempeln
rm -f $B/lock.txt
app_melden
zurueck
