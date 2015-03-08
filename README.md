Allgemein:
Das Programm startet in ST.java, dort wird eine Sprachsynthese erzeugt und
ausgeführt. Eine Sprachsynthese ist vom Typ "CarSynthesisAbstract"
abgeleitet und in ST.java ist das die Variable "run".
Es gibt aktuell 2 Synthesen: "SynthesisMinimum" und "SynthesisRunner" welche
beide eben halt von "CarSynthesisAbstract" abgeleitet sind. Was die
enstprechenden Synthesen machen steht in den jeweiligen Klassen als Klassen
Kommentar über der Klasse.

Einstellung:
Momentan muss man um die Synthese zu wechseln einfach in ST.java den
initialisierten Typ von der Variable "run" verändern, zB:
Statt
static CarSynthesisAbstract run = new SynthesisRunner("Nach links abbiegen in");
benutzen wir:
static CarSynthesisAbstract run = new SynthesisMinimum("Nach links abbiegen in");

Ausführung:
Bei Start des Programmes wird der Meterwert langsam oder schnell von 150
Meter runtergezählt, wobei die Geschwindigkeit (in m/s) durch einen
Schieberegler (welcher bei Programmstart erscheint) eingestellt werden kann.
Bei der Ausführung wird momentan der aktuelle Meterwert in der Konsole
ausgegeben, sowie eine aktuelle Information was gerade ausgesprochen wird
(sowas wie "Counter" steht dafür, dass gerade sowas gesagt wird wie
"hundert", "Anfang" steht dafür, dass die Turninformation ausgegeben wird
(sowas wie "Nach links abbiegen in") diese Kommentarausgaben werden in
den jeweiligen Synthesen in der Methode "protected void stretchMeters"
erzeugt).

Anmerkungen:

Die Konsolenausgabe was gerade gesagt wird ("Counter" etc) ist momentan nicht ganz zuverlässig

ST.java ist noch dreckig