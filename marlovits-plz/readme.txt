mögliche Datenquellen:

********************************************
******  Für Landnamen: Iso2  ***************
********************************************

Aus Wikipedia
********************************************
- Frei verfügbar
- muss mittels Scripts extrahiert werden, fehleranfällig
- alle Sprachen unterschiedlich abgelegt

Aus swissdata
********************************************
- Frei verfügbar???



********************************************
******  Für Postleitzahlen  ****************
********************************************

OpenGeoDB, http://opengeodb.hoppe-media.com
********************************************
- Frei verfügbar
- ES FEHLEN EINZELNE DATEN!!!
- Habe keine Online-Abfrage gefunden.
- Die ganzen Daten (alles auf einmal) können heruntergeladen werden als sql-Script für die
  Erstellung der Tabellen in der Datenbank und Einfügen der Daten selbst. Riesiges Script.
  Es können keine Teildaten heruntergeladen werden - zBsp nur die von uns benötigten, zBsp
  CH/D/A, sondern nur alles.

Wikipedia
********************************************
- ausführliche Infos zu Ländern, Untereinheiten.
- Die Infos sind uneinheitlich formatiert, könnten aber grundsätzlich auch benutzt werden.
- Postleitzahlen sind zu unvollständig, reichlich verteilt.
- VERTEILT, UNVOLLSTÄNDIG!!!

GeoNames
********************************************
- Frei verfügbar
- ES FEHLEN EINZELNE DATEN!!!
- Möglichkeit, online Abfragen zu schicken,
  Rückgabe des Resultates via XML für verschiedenste Abfragen
- Möglichkeit, die Daten herunterzuladen, auch nur Teildaten für einzelne Länder
  sind als tab-delimited in zip-files zu finden, müssen also dann importiert werden

Daten der Schweizer Post
********************************************
- Frei verfügbar
- VOLLSTÄNDIG

Daten der Österreichischen Post
********************************************
- Frei verfügbar
- VOLLSTÄNDIG

Daten der Deutschen Post
********************************************
- VON DER DEUTSCHEN POST NICHT FREI VERFÜGBAR
- via http://www.swissdata.net/Download/Download.asp frei verfügbar???
- via OpenGeoDB Download: http://fa-technik.adfc.de/code/OpenGeoDB/
- Vorsicht: hier muss zum Teil auch die Strasse angegeben werden, da dann die PLZ anders sein kann

********************************************
********************************************
********************************************
Meine aktuelle Lösung:

- Alle Tabellen und Scripts sind für verschiedene Sprachen eingerichtet.
- Die vorinstallierten Daten sind nur in deutsch vorinstalliert.

- Die häufig gebrauchten Daten werden fest vorinstalliert: default: CH/DE/AT
- Datenquellen:
	- Schweiz:		Schweizer Post, Grundstamm
					Sprachdaten aus GeoNames importiert
	- Österreich:	Österreichische Post
					Sprachdaten aus GeoNames importiert
	- Deutschland:	OpenGeoDB, http://fa-technik.adfc.de/code/OpenGeoDB/DE.tab
- Die Installation erfolgt via sql-Script.

- Die anderen Daten werden primär nicht eingefügt.
- Die Abfrage für ein anderes Land erfolgt via GeoNames - hier wird xml zurückgegeben
- Wenn für die eingegebene Land/PLZ-Kombination die Daten noch nicht vorhanden sind, 
  dann kann der User wählen, ob er die gesamten Landes-Daten importieren will.
- dasselbe gilt, wenn für die aktuelle Sprache in Elexis die Daten fehlen
- die fehlenden Daten werden aus GeoNames importiert, da online via xml möglich


********************************************
Enthält Iso2-Länder-Codes UND Postal-Code-Format!!!
http://download.geonames.org/export/dump/countryInfo.txt
select '9779' ~ '^(\\d{4})$' as tmp true(false
********************************************

********************************************
Aktualisierungskonzept
********************************************
- für die fest installierten Daten wird in der Datenbank das Importdatum abgelegt
- der User kann von Hand nach neuen/aktualisierten Daten suchen lassen
  oder einen regelmässigen Scan installieren


********************************************
******  Prefs  *****************************
********************************************
Auswahl des Defaults für das Feld Land
Import von PLZ-Daten in die Datenbank
Update von PLZ-Daten in der Datenbank

  