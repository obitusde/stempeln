# Knopf [Spaeter]: 15 min keine Erinnerung
jetzt
V_GRUND=""; [ -f $B/vorschlag-letzter.txt ] && . $B/vorschlag-letzter.txt
echo $(( EPOCH + 900 )) > $B/spaeter.txt
rm -f $B/vorschlag.txt
note "Spaeter${V_GRUND:+: $V_GRUND} - Erinnerung wieder ab $(hm $(( (NOW + 15) % 1440 )))" "Stempeln" spaeter
