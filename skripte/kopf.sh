# Stempel-Skripte - gemeinsamer Kopf (Version siehe SKV = gemeinsame Version von App, Skripten und Makros)
# Wird von den Makro-Huellen geladen: vorher gesetzt LAT LON FREI TEXT (Magic Text aus MacroDroid)
SKV=5.0
B=/sdcard/Download/stempeln
mkdir -p $B
DEBUG=0
SOLL=505
# Einstempel-Erinnerung ab START (Di/Fr, 435 = 07:15) bzw. START2 (Mo/Mi/Do, 465 = 07:45), Standort nur bis ORTBIS (660 = 11:00)
# Der Waechter wacht Mo-Fr um 07:00 auf (Shizuku-Pruefung) und holt den Standort nur morgens bis zum Einstempeln, alle 15 min
START=435
START2=465
ORTBIS=660
# Pause vorbei nach PAUSE_SS min (SUR SITE), sonst PAUSE_KURZ min (HOME OFFICE, HUB)
PAUSE_SS=45
PAUSE_KURZ=30
# Einstellungen aus der App (Download/stempeln/einstellungen.txt): Arbeitszeit (mit "gueltig ab"), Pausen usw. - ueberschreiben die Werte oben
E_SOLL=""; [ -f $B/einstellungen.txt ] && . $B/einstellungen.txt
PAUSE_SS=${E_PAUSE_SS:-$PAUSE_SS}; PAUSE_KURZ=${E_PAUSE_KURZ:-$PAUSE_KURZ}
# Orte: eigene Datei auf dem Handy (schreibt das private Makro "Stempeln Update"), nie im Repo
ORTE=$(cat $B/orte.txt 2>/dev/null)
# Welche Seite zeigt Chrome? $1 = Bildschirminhalt -> SEITE=clock|fremd, U=Adresse aus der Adressleiste
# (Die Startseite .../ux/home zeigt auch "HOME OFFICE" - deshalb zaehlt die Adresse; ohne Adresse: Abschnitt "TIMBRER AVEC MOTIF")
seite_art() { U=$(grep -o 'webquartz/ux/[a-z]*' "$1" 2>/dev/null | head -1)
  case "$U" in
    */clock) SEITE=clock ;;
    "") if grep -q 'TIMBRER AVEC MOTIF' "$1" 2>/dev/null; then SEITE=clock; else SEITE=fremd; fi ;;
    *) SEITE=fremd ;;
  esac; }
# App "Stempeln" benachrichtigen (neuer Stand / Konto) - sie stellt danach ihre Wecker neu
app_melden() { am broadcast -n io.github.obitusde.stempeln/.StandEmpfaenger -a io.github.obitusde.stempeln.STAND </dev/null >/dev/null 2>&1; }
# Zum Schluss die App "Stempeln" zeigen (neuer Stand) statt Chrome - wie ein Tipp aufs App-Symbol
zurueck() { am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n io.github.obitusde.stempeln/.MainActivity </dev/null >/dev/null 2>&1; }
# Sperrbildschirm im Bildschirminhalt $1?
sperrbild() { grep -qE 'systemui:id/(keyguard|device_entry)' "$1" 2>/dev/null; }
# Sperrbildschirm sichtbar? (0 = nein)
gesperrt() { dumpsys window </dev/null 2>&1 | grep -cE 'isKeyguardShowing=true|mShowingLockscreen=true|mDreamingLockscreen=true'; }
# Meldung: note TEXT TITEL TAG [ART] - zeigt die App (ART fortschritt = still, hinweis, ok = Ton "geklappt", problem = Ton "Problem");
# antwortet die App nicht (result=7 fehlt), kommt die Meldung wie frueher von der Shell
note() { r=$(am broadcast -n io.github.obitusde.stempeln/.StandEmpfaenger -a io.github.obitusde.stempeln.MELDUNG --es art "${4:-fortschritt}" --es text "$1" </dev/null 2>&1)
  case "$r" in *result=7*) ;; *) cmd notification post -S bigtext -t "$2" "$3" "$(date +%H:%M) · $1" </dev/null >/dev/null 2>&1 ;; esac; }
# Vorschlag an den Waechter: Text steht im Titel (MacroDroid zeigt not_title), Kennung im Text:
# "Stempel-Vorschlag" = Knoepfe [Stempeln] [Schon gestempelt] [Spaeter], "Stempel-Heimfrage" = zu Hause, [Heute nicht] statt [Spaeter]
signal() { cmd notification post -S bigtext -t "$(date +%H:%M) · $1" vorschlag "${2:-Stempel-Vorschlag}" </dev/null >/dev/null 2>&1; }
jetzt() { if [ -n "$T_JETZT" ]; then set -- $T_JETZT; else set -- $(date '+%Y %m %d %H %M %u %s'); fi
  JJ=$1; MM=${2#0}; TT=${3#0}; HH=${4#0}; MI=${5#0}; WT=$6; EPOCH=$7; HEUTE=$1-$2-$3; NOW=$(( ${HH:-0}*60 + ${MI:-0} )); }
hm() { echo "$(( $1 / 60 )):$(( $1 % 60 / 10 ))$(( $1 % 10 ))"; }
minuten() { h=${1%%:*}; m=${1#*:}; h=${h#0}; m=${m#0}; echo $(( ${h:-0}*60 + ${m:-0} )); }
mikro() { g=${1%%.*}; f=${1#*.}; [ "$f" = "$1" ] && f=0; f=${f}000000; f=${f%"${f#??????}"}; echo $(( g*1000000 + 1$f - 1000000 )); }
wurzel() { n=$1; x=$n; [ $x -le 1 ] && { echo $x; return; }; y=$(( (x+1)/2 )); while [ $y -lt $x ]; do x=$y; y=$(( (x + n/x)/2 )); done; echo $x; }
abst() { a=$(( $1 - $3 )); b=$(( $2 - $4 )); [ $a -lt 0 ] && a=$(( -a )); [ $b -lt 0 ] && b=$(( -b ))
  if [ $a -gt 200000 ] || [ $b -gt 200000 ]; then echo 99999; return; fi
  a=$(( a*1112/10000 )); b=$(( b*766/10000 )); wurzel $(( a*a + b*b )); }
# naechster Ort zu $1 $2 -> TYP DIST RAD NAME (TYP leer = kein gueltiger Standort)
ort() { TYP=""; DIST=""; RAD=""; NAME=""
  case "$1$2" in *[0-9].[0-9]*[0-9].[0-9]*) ;; *) return ;; esac
  la=$(mikro "$1"); lo=$(mikro "$2")
  set -f; alt=$IFS; IFS='|'
  for o in $ORTE; do
    IFS=';'; set -- $o; IFS='|'
    [ $# -ge 4 ] || continue
    e=$(abst $la $lo $(mikro "$2") $(mikro "$3")); r=${5:-800}
    if [ -z "$DIST" ] || [ "$e" -lt "$DIST" ]; then TYP=$1; DIST=$e; RAD=$r; NAME=$4; fi
  done
  IFS=$alt; set +f; }
# Knopf-Text fuer Typ $1 (nur der Knopf, ohne Ortsname)
label() { case "$1" in HO) echo "HOME OFFICE" ;; HUB) echo "HUB" ;; SS) echo "SUR SITE" ;; *) echo "unbekannter Ort, du tippst selbst" ;; esac; }
# Welcher Knopf fuer Richtung $1 (EIN/AUS)? Heute schon eingestempelt -> Knopf vom Einstempeln (Z_ORT, du bleibst dort),
# sonst Standort (vorher ort aufrufen) -> KTYP KNAME LABEL (KTYP leer = unbekannter Ort)
wahl() { if [ -n "$Z_ORT" ]; then KTYP=$Z_ORT; KNAME=$Z_NAME
  elif [ -n "$TYP" ] && [ "$DIST" -le "$RAD" ]; then KTYP=$TYP; KNAME=$NAME
  else KTYP=""; KNAME=""; fi
  LABEL=$(label "$KTYP"); }
# Woerterbuch aus "Bildschirminhalt lesen" -> Zeilen "x|y|Text"
elemente() { x=""; y=""
  while IFS= read -r z; do
    case "$z" in
      "-- [centerX]: "*) x=${z#*: } ;;
      "-- [centerY]: "*) y=${z#*: } ;;
      "-- [text]: "*) s=${z#*\]: }; [ -n "$s" ] && echo "$x|$y|$s" ;;
    esac
  done < "$1"; }
# Stand aus Elementliste $1: STATUS (IN/OUT/?), PRES (Minuten, leer = nicht gelesen), LETZTE, BUCH, PAUSE, Knopf-Positionen
# AGENDA=1 nur wenn die Agenda fertig geladen ist (Stundenzahl 23:00 in der Agenda-Spalte) - sonst ist der Knopf-Text unzuverlaessig
stand() { STATUS="?"; HOT=""; PRES=""; AGENDA=0; K_SOLDE=""; K_JOUR=""; K_RATT=""; K_HS=""; HOX=113; HOY=436; SSX=70; SSY=567; HUX=172; HUY=567; ZEITEN=""
  while IFS='|' read -r x y s; do
    case "$x$y" in *[!0-9]*|"") continue ;; esac
    if [ "$x" -lt 600 ]; then
      case "$s" in
        *" HOME OFFICE") HOT=$s; HOX=$x; HOY=$y
          case "$s" in SORTIE*) STATUS=IN ;; ENTR*) STATUS=OUT ;; esac ;;
        "SUR SITE") SSX=$x; SSY=$y ;;
        HUB) HUX=$x; HUY=$y ;;
        *"Solde heure"*) K_SOLDE=${s%%[!0-9:-]*} ;;
        *"Heures du jour"*) K_JOUR=${s%%[!0-9:-]*} ;;
        *"Rattrapage"*) K_RATT=${s%%[!0-9:-]*} ;;
        *"HS cumul"*) K_HS=${s%%[!0-9:-]*} ;;
        *"Temps de pr"*) p=${s%%[!0-9:]*}; case "$p" in *[0-9]:[0-9][0-9]) PRES=$(minuten "$p") ;; esac ;;
      esac
    elif [ "$x" -le 720 ]; then
      [ "$s" = "23:00" ] && AGENDA=1
    else
      case "$s" in [0-2][0-9]:[0-5][0-9]) ZEITEN="$ZEITEN $s" ;; esac
    fi
  done < "$1"
  # halb geladen: Knopf/Zeiten nicht verwenden
  HALB=0; [ $AGENDA = 1 ] || { [ -n "$HOT" ] && HALB=1; STATUS="?"; PRES=""; ZEITEN=""; K_SOLDE=""; K_JOUR=""; K_RATT=""; K_HS=""; }
  LETZTE=""; BUCH=0; PAUSE=0; aus=""
  # Zeiten abwechselnd rein/raus; Luecken nach einem Ausstempeln 11:00-14:30 = Mittagspause
  for s in $(for s in $ZEITEN; do echo $s; done | sort -u); do
    LETZTE=$s; BUCH=$(( BUCH + 1 )); m=$(minuten $s)
    if [ $(( BUCH % 2 )) = 0 ]; then aus=$m
    elif [ -n "$aus" ]; then [ $aus -ge 660 ] && [ $aus -le 870 ] && PAUSE=$(( PAUSE + m - aus )); aus=""; fi
  done; }
# Lauf-Ordner (von start.sh angelegt)
lauf() { D=$(cat $B/aktuell.txt 2>/dev/null); [ -d "$D" ] || { D=$B/lauf; mkdir -p "$D"; }
  L=$D/log.txt; exec 2>>"$L"; }
t() { [ $DEBUG = 1 ] && echo "$(date +%T) $*" >> "$L"; }
# Arbeitszeit fuer Tag $1 (JJJJ-MM-TT) aus E_SOLL ("2000-01-01=08:25,2027-01-01=08:00"), sonst SOLL
soll_am() { s=$SOLL; set -f; alt=$IFS; IFS=','; for e in $E_SOLL; do [[ "${e%%=*}" > "$1" ]] || s=$(minuten "${e#*=}"); done; IFS=$alt; set +f; echo $s; }
# Pause fuer Knopf $1 (SS = SUR SITE, sonst HO/HUB)
pause_fuer() { if [ "$1" = SS ]; then echo $PAUSE_SS; else echo $PAUSE_KURZ; fi; }
jetzt
SOLL=$(soll_am $HEUTE)
