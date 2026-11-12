# Knopf [Heute nicht] in der Zuhause-Erinnerung: heute keine Zuhause-Erinnerung mehr (Buero-Erinnerungen bleiben)
jetzt
echo "$HEUTE" > $B/heute-nicht.txt
rm -f $B/vorschlag.txt
note "Heute keine Zuhause-Erinnerung mehr - im Buero erinnere ich dich wie immer" "Stempeln" heutenicht
