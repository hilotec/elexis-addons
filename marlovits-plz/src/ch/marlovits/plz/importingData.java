package ch.marlovits.plz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jface.action.Action;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVReader;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class importingData 	{
	private static final String SRC_ENCODING				= "UTF-8";
	public  static final String ID							= "ch.marlovits.plz.PLZView";

	private static final String BASE_URL_DE					= "http://de.wikipedia.org";
	private static final String URL_DE						= "http://de.wikipedia.org/wiki/ISO-3166-1-Kodierliste";
	private static final String WIKI_LAND_STARTINFO_MARKER	= "<table class=" + "\"" + "wikitable sortable" + "\"";
	private static final String WIKI_LAND_STARTCELLS_MARKER	= "<td";
	private static final String WIKI_LAND_ENDINFO_MARKER	= "</table>";
	private static final String WIKI_LAND_SKIPDATA_MARKER	= "<td><span style";
	
	private static final String WIKI_LAND_SUBISO_STARTMARKER	= "<table class=\"prettytable sortable\"";
	private static final String WIKI_LAND_SUBISO_ENDMARKER		= "</table>";

	private static final String TABLEROW_STARTMARKER	= "<tr";
	private static final String TABLEROW_ENDMARKER		= "</tr>";
	private static final String TABLEDATA_STARTMARKER	= "<td>";
	private static final String TABLEDATA_ENDMARKER		= "</td>";

	/***************************************************************************/
	/***************************************************************************/
	/***************************************************************************/
	/**
	 * Liest Inhalt einer ganzen Seite einer übergebenen URL
	 * @param url: die einzulesende URL
	 * @return die ganze HTML-Seite als String
	 */
	public static String readHTMLPage(final String url) throws IOException, MalformedURLException{
		URL content = new URL(url);
		InputStream input = content.openStream();
		StringBuffer sb = new StringBuffer();
		int count = 0;
		char[] c = new char[10000];
		InputStreamReader isr = new InputStreamReader(input);
		try {
			while ((count = isr.read(c)) > 0) {
				sb.append(c, 0, count);
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}
		// TO DO: clean up ist nicht ok/nicht vollständig
		return cleanupUmlaute(cleanupText(sb.toString()));
	}
	
	private static String cleanupText(String text){
		text = text.replace("</nobr>", "").replace("<nobr>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		text = text.replace("&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("<b class=\"searchWords\">", ""); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("</b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace((char) 160, ' '); // Spezielles Blank Zeichen wird
												// ersetzt
		return text;
	}
	
	private static String cleanupUmlaute(String text) {
		text = text.replace("&#xE4;", "ä");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xC4;", "Ä");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xF6;", "ö");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xD6;", "Ö");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xFC;", "ü");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xDC;", "Ü");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xE8;", "è");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xE9;", "é");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xEA;", "ê");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xE0;", "à");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xA0;", " ");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("%C3%9F", "ss");//$NON-NLS-1$ //$NON-NLS-2$
		
		return text;
    }
		
	/**
	 * Extrahieren der Iso-Länderdaten aus Wikipedia
	 * @param language: die jeweilige Sprache, für welche die Daten extrahiert 
	 * werden sollen, die URL wird entsprechend gewählt
	 */
	private void extractLandData(final String language)	{
		// Einlesen der ganzen Seite in der gewünschten Sprache in die Variable wholeHTMLPage
		String wholeHTMLPage = null;
		try {
			// TO DO: abhängig von language
			wholeHTMLPage = readHTMLPage(URL_DE);
		} catch (MalformedURLException e) {
			SWTHelper.alert("Fehler", "Die URL '" + URL_DE + "' ist nicht korrekt formatiert.");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			SWTHelper.alert("Fehler", "Die URL '" + URL_DE + "' kann nicht eingelesen werden.");
			e.printStackTrace();
		}
		
		// Tabellen-Inhalt aus Text extrahieren
		int tableStartPos			= wholeHTMLPage.indexOf(WIKI_LAND_STARTINFO_MARKER,	0);
		int tableContentStartPos	= wholeHTMLPage.indexOf(TABLEROW_STARTMARKER,		tableStartPos);
		int tableDataStartPos		= wholeHTMLPage.indexOf(TABLEROW_STARTMARKER,		tableStartPos);
		int tableEndPos				= wholeHTMLPage.indexOf(WIKI_LAND_ENDINFO_MARKER,   tableDataStartPos);
		String landTableContent 	= wholeHTMLPage.substring(tableContentStartPos,		tableEndPos);
		
		// durch die Tabelle loopen und die einzelnen Datensätze einlesen
		int currRowPos	= 0;
		int nextRowPos	= 0;
		String rowData = "";
		int i = 0;
		while ((currRowPos != -1))	{
			nextRowPos = landTableContent.indexOf(TABLEROW_STARTMARKER, currRowPos + TABLEROW_STARTMARKER.length());
			rowData = landTableContent.substring(currRowPos, nextRowPos);
			currRowPos = nextRowPos;
			extractLandRowData(rowData, language);
			i++;
		}
	}
	
	/**
	 * Extrahieren der einzelnen Zeilen aus der Iso-Länder-HTML-Tabelle
	 * @param rowData: HTML-Inhalt einer Zeile, ohne enclosing <tr></tr>
	 * @param language: die Sprache dieses Eintrages
	 */
	private void extractLandRowData(final String rowData, final String language)	{
		String landName;
		String landWikiLink;
		String landIso2;
		String landIso3;
		String landIsoNum;
		String landTld;
		String landIoc;
		String landIso3166_2;
		
		// initialisieren der Zähler, etc
		int			currDataPos	= 0;
		int			nextDataPos	= 0;
		String		cellData;
		String		subContentHTML = null;
		String[]	linkAndText;
		
		// die erste Zelle enthält den Namen des Landes und dessen Wikipedia-Link
		currDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		if (currDataPos == -1)	{
			return;		// wenn kein <td vorhanden ist, dann ist es ein header - skip
		}
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = rowData.substring(currDataPos, nextDataPos);
		String theString = cellData.substring(0, WIKI_LAND_SKIPDATA_MARKER.length());
		if (theString.equals(WIKI_LAND_SKIPDATA_MARKER))	{
			return;		// wenn die erste Zelle den WIKI_LAND_SKIPDATA_MARKER enthält, dann überspringen
		}
		cellData = extractCellData(cellData);
		linkAndText = splitHyperlinkCell(cellData);
		landName     = linkAndText[1];
		landWikiLink = BASE_URL_DE + linkAndText[0];
		currDataPos = nextDataPos;
		
		// Alpha 2
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landIso2 = left(cellData, 2);
		
		// Alpha 3
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landIso3 = left(cellData, 3);
		
		// Numeric
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landIsoNum = left(cellData, 3);
		
		// TopLevelDomain
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landTld = left(cellData, 3);
		
		// IOC
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landIoc = left(cellData, 3);
		
		// ISO3166-2: enthält den Iso2 und dessen Wikipedia-Link zur Seite mit den Sub-Infos
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		linkAndText = splitHyperlinkCell(cellData);
		//System.out.println(BASE_URL_DE + linkAndText[0]);	// link
		currDataPos = nextDataPos;
		landIso3166_2 = left(linkAndText[1], 2);
		
		// Erstellen eines neuen Eintrages in der Tabelle CH_MARLOVITS_LAND
		new LandEintrag(landName,
						landIso2,
						landIso3,
						landIsoNum,
						landTld,
						landIoc,
						landIso3166_2,
						landWikiLink,
						language);
		
		// Einlesen der Informationen aus den Unter-Seiten
		subContentHTML = null;
		
		//if (landIso2.equals("éé"))	{
		
		try {
			String combinedURL = BASE_URL_DE + linkAndText[0];
			subContentHTML = readHTMLPage(combinedURL);
			extractSubIsos(subContentHTML, language);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//}
	}
	
	/**
	 * Entfernt die enclosing <td>  / </td>
	 * @param cellDataWithEnclosings
	 * @return input ohne enclosing <td>  / </td>
	 */
	private String extractCellData(final String cellDataWithEnclosings)	{
		String cellContents = null;
		
		// fast exit, falls leer
		if ((cellDataWithEnclosings == null) || (cellDataWithEnclosings == ""))	{
			return cellContents;
		}
		int contentStart = cellDataWithEnclosings.indexOf(TABLEDATA_STARTMARKER, 0);
		if (contentStart != -1)	{
			// has <td>[Content]</td>
			int contentEnd	= cellDataWithEnclosings.indexOf(TABLEDATA_ENDMARKER, TABLEDATA_STARTMARKER.length());
			cellContents = cellDataWithEnclosings.substring(contentStart + TABLEDATA_STARTMARKER.length(), contentEnd);
		} else	{
			// has <td [...]> [Content] </td>
			// TO DO
	        Pattern pattern = Pattern.compile("<td.*>");
	        Matcher matcher = pattern.matcher(cellDataWithEnclosings);
	        cellContents = matcher.replaceAll("");
	        pattern = Pattern.compile("</td>");
	        matcher = pattern.matcher(cellContents);
	        cellContents = matcher.replaceAll("");
		}
		return cellContents;
	}
	
	/**
	 * Extrahiert aus einem HTML-String mit einem href den ersten Link 
	 * und den reinen Text-Teil
	 * @param htmlString: html-String mit href-Teil
	 * @return String[]: {linkPart, textPart}
	 */
	private String[] splitHyperlinkCell(final String htmlString)	{
		// Initialisieren
		String linkPart = "";
		String textPart = "";
		final String hrefStartMarker	= "<a href=\"";
		final String hrefEndMarker		= "\">";
		
		// leerer String - fast exit
		if ((htmlString == null) || (htmlString.equals("")))	{
			linkPart = "";
			textPart = htmlString;
			return new String[] {linkPart, textPart};
		}
		
		// es wird nur der erste Link extrahiert		
		int hrefStart = htmlString.indexOf(hrefStartMarker, 0);
		
		// es gibt keinen Link
		if (hrefStart == -1){
			linkPart = "";
			textPart = htmlString;
			return new String[] {linkPart, textPart};
		}
		
		// Teil links des href gehört zum Text-Teil
		textPart = textPart + htmlString.substring(0, hrefStart);
		
		// href extrahieren
		int hrefEnd = htmlString.indexOf(hrefEndMarker, hrefStart) + hrefEndMarker.length();
		String hrefPart = htmlString.substring(hrefStart, hrefEnd);
		
		// Link aus href extrahieren
		linkPart = extractHrefLink(hrefPart);
		
		// Text rechts des hrefs gehört zum Text-Teil, es müssen alle </a> entfernt werden
		textPart = textPart + htmlString.substring(hrefEnd, htmlString.length()).replace("</a>", "");
		
		// jetzt werden alle restlichen HTML-Tags <XXX> </XXX> entfernt
		textPart = stripHTMLTags(textPart);
        
        // returns, etc, entfernen
        textPart = replaceReturns(textPart, " ");
        
		// Rückgabe
		return new String[] {linkPart, textPart};
	}
	
	/**
	 * 
	 * @param href: voller href, aus welchem der Link extrahiert werden soll
	 * @return der Link, der in href vorhanden ist
	 */
	private String extractHrefLink(final String href)	{
		// Initialisieren
		final String hrefLinkStartMarker = "<a href=\"";
		final String hrefLinkEndMarker   = "\"";
		
		// Start des href suchen
		int hrefLinkStart = href.indexOf(hrefLinkStartMarker,	0);
		if (hrefLinkStart == -1)	{
			return "";
		}
		// Ende des href suchen
		hrefLinkStart = hrefLinkStart + hrefLinkStartMarker.length();
		int hrefLinkEnd = href.indexOf(hrefLinkEndMarker, hrefLinkStart);
		
		// extrahieren, Rückgabe
		return href.substring(hrefLinkStart, hrefLinkEnd);
	}
	
	/**
	 * Extrahieren der Daten aus den Info-Seiten für die Sub-Isos "ISO 3166-2".
	 * Es können mehrere Tabellen mit den Infos vorhanden sein!
	 * @param pageHTML
	 * @author Harald Marlovits
	 */
	private void extractSubIsos(final String pageHTML, final String language)	{
		int startOfTable	= pageHTML.indexOf(WIKI_LAND_SUBISO_STARTMARKER, 0);
		int endOfTable		= 0;
		String subTableContent = null;
		// alle vorhandenen Tabellen durchlaufen
		int subTableIndex = 0;
		while (startOfTable != -1)	{
			endOfTable = pageHTML.indexOf(WIKI_LAND_SUBISO_ENDMARKER, startOfTable);
			startOfTable = pageHTML.indexOf(TABLEROW_STARTMARKER, startOfTable);
			subTableContent = pageHTML.substring(startOfTable, endOfTable);
			startOfTable = pageHTML.indexOf(WIKI_LAND_SUBISO_STARTMARKER, startOfTable);
			System.out.println("**************************************");
			//System.out.print(subTableContent);
			extractSubCellTableData(subTableContent, subTableIndex, language);
			subTableIndex++;
		}
	}
		
	private void extractSubCellTableData(final String tableData, final int subTableIndex, final String language)	{
		// durch die Tabelle loopen und die einzelnen Datensätze einlesen
		int currRowPos	= 0;
		int nextRowPos	= 0;
		String rowData = "";
		
		int i = 0;
		
		String headerMarker = "<th";
		
		// calc the number of columns for this table
		// always need the first (iso name) and the last column (iso-code)
		// if > 2 columns, then concat second column in parenthesis to first column
		
		String currName = "";
		
		int columnCount = 0;
		while (currRowPos != -1)	{
			nextRowPos = tableData.indexOf(TABLEROW_STARTMARKER, currRowPos + TABLEROW_STARTMARKER.length());
			rowData = tableData.substring(currRowPos, (nextRowPos == -1) ? tableData.length() : nextRowPos);
			currRowPos = nextRowPos;
			System.out.println("********************");
			// Anzahl Spalten ermitteln
			if (columnCount == 0){
				String regex = "<t[dh]>";
				Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(rowData);
				while (m.find()) {
					columnCount++;
				}
			}
			// Inhalt extrahieren,  <tr> / </tr> entfernen
	        Pattern pattern = Pattern.compile("<tr.*>");
	        Matcher matcher = pattern.matcher(rowData);
	        rowData = matcher.replaceAll("");
	        pattern = Pattern.compile("</tr>");
	        matcher = pattern.matcher(rowData);
	        rowData = matcher.replaceAll("");
	        int startOfContent = rowData.indexOf("<t");
	        rowData = rowData.substring(startOfContent);
	        
			// wenn <th> dann enthält die Zelle den Namen von Kanton/District/Region, etc
			int headerMarkerStart = rowData.indexOf(headerMarker, 0);
			if (headerMarkerStart != -1){
				headerMarkerStart = rowData.indexOf(">", headerMarkerStart) + 1;
				int endMarker = rowData.indexOf("</t", headerMarkerStart);
				currName = rowData.substring(headerMarkerStart, endMarker);
				System.out.println(currName);
			} else	{
				exctractSubCellRowData(rowData, currName, subTableIndex, language, columnCount);
			}
			i++;
		}	
	}
	
	/** 
	 * Entfernt aus einem String alle tabs/returns/newlines/formsfeeds
	 * @param inputString: zu bearbeitender String
	 * @param replacement: damit werden die gefundenen Vorkommen ersetzt
	 * @return inputString, von welchem alle whiteSpaces weggestrippt sind
	 */
	private String replaceReturns(final String inputString, final String replacement){
		String tmp = inputString;
		tmp = tmp.replaceAll("\\t", replacement);
		tmp = tmp.replaceAll("\\n", replacement);
		tmp = tmp.replaceAll("\\f", replacement);
		tmp = tmp.replaceAll("\\r", replacement);
		return tmp;
	}
	
	/**
	 * Entfernt aus einem String alle HTML-Tags <XXX> und </XXX>
	 * @param inputString
	 * @return inputString, von welchem alle whiteSpaces weggestrippt sind
	 */
	private String stripHTMLTags(final String inputString)	{
		String tmp = inputString;
		tmp = tmp.replaceAll("</.*>", "");
		tmp = tmp.replaceAll("<.*>", "");
		return tmp; 
	}
	
	/**
	 * entfernen von: leading spaces, trailing spaces und mehrfach-Spaces im String
	 * @param source: zu bearbeitender String
	 * @return gestrippter String
	 */
    public static String fullTrim(final String source) {
    	String tmp = source;
    	// leading Spaces strippen
    	tmp = tmp.replaceAll("^\\s+", "");
    	// trailing Spaces strippen
    	tmp = tmp.replaceAll("\\s+$", "");
    	// mehrfach-Spaces durch einfachen Space ersetzen
    	tmp = tmp.replaceAll("\\s+", " ");
    	return tmp;
	}
    
	private void exctractSubCellRowData(final String rowData, final String kantonName, final int subTableIndex, final String language, final int columnCount)	{
		// Die erste  Spalte enthält die zu benutzende Bezeichnung
		// die letzte Spalte enthält den ISO-Code
		// falls zweite Spalte vorhanden, dann in Klammern an Spalte 1 anfügen
		
		String localData = rowData;
		// die Endmarker werden immer allesamt entfernt
		localData = localData.replace("</td>", "");
		// erstelle String Array, split on <td>
		String[] cellDataStringArray = localData.split("<td>");
		
		String nameRow  = replaceReturns(cellDataStringArray[1], " ");
		String codeRow  = replaceReturns(cellDataStringArray[columnCount], " ");
		
		// nameRow: enthält den Link zu Sub-Info und den Iso-Namen, splitten
		String[] landLink_landName = splitHyperlinkCell(nameRow);
		
		// codeRow: nur Text links von Space ist gültig, Rest wegstrippen
		codeRow  = codeRow.split(" ")[0];
		// codeRow: splitten auf "-", links ist Iso2 des Landes, rechts Iso des "Kantons"
		String[] landIso_SubIso  = codeRow.split("-");
		
		// falls mehr als 2 Spalten, dann 2. Spalte in Klammern an den Namen anhängen
		String kantonname		= landLink_landName[1];
		kantonname = replaceReturns(kantonname, "");
		if (columnCount > 2)	{
			kantonname = kantonname + " (" + stripHTMLTags(replaceReturns(cellDataStringArray[2], "")) + ")";
			kantonname = fullTrim(kantonname);
		}
		
		// die Werte zusammentragen
		String kantonfullcode	= codeRow;
		String kantonsubcode	= landIso_SubIso[1];
		String kantonland		= landIso_SubIso[0];
		String kantonindex		= "" + subTableIndex;
		String kantonkind		= kantonName;
		String kantonwikilink	= BASE_URL_DE + landLink_landName[0];
		String kantonlanguage	= language;
		
		// debug
		System.out.println("kantonname:     " + kantonname);
		System.out.println("kantonfullcode: " + kantonfullcode);
		System.out.println("kantonsubcode:  " + kantonsubcode);
		System.out.println("kantonland:     " + kantonland);
		System.out.println("kantonindex:    " + kantonindex);
		System.out.println("kantonkind:     " + kantonkind);
		System.out.println("kantonwikilink: " + kantonwikilink);
		System.out.println("kantonlanguage: " + kantonlanguage);
		
		// den Eintrag erstellen
	 	new KantonEintrag(kantonname,
				 		  kantonfullcode,
				 		  kantonsubcode,
				 		  kantonland,
				 		  kantonindex,
				 		  kantonkind,
				 		  kantonwikilink,
				 		  kantonlanguage);
}
	
	/**
	 * Rückgabe des linken Anteils des Eingabe-Strings
	 * @param input: String, dessen linker Teil zurückgegeben werden soll
	 * @param count: Anzahl Zeichen, die zurückgegeben werden sollen
	 * @return gestrippter String oder "", wenn input null oder leer
	 */
	private String left(final String input, final int count)	{
		if ((input == null) || (input.equals("")))	{
			return "";
		} else	{
			return input.substring(0, count);
		}
	}
	
	
	/**
	 * 
	 * @param countryIso2: Land, in welchem nach der Postleitzahl gesucht werden soll
	 * @param postalCode: Postleitzahl, für welche der Ort gefunden werden soll
	 * @param language: Resultat in dieser Sprache ausgeben
	 * @return String[]: String-Array aller passenden Einträge, null, wenn nichts gefunden
	 */
	public static String[] geoNames_PlaceNameFromPostalCode(final String countryIso2,
															final String postalCode,
															final String language)	{
		// Initialisieren Result
		String[] result = null;
		
		// *** minimales Fehlerhandling, damit unnötige Abfragen schon mal vermieden werden
		// countryIso2 muss 2 Zeichen lang sein
		if (countryIso2.length() != 2)	{
			return null;
		}
		// darf nicht null oder leer sein
		if ((postalCode.equals("")) || (postalCode == null))	{
			return null;
		}
		// language muss 2 Zeichen lang sein
		if (language.length() != 2)	{
			return null;
		}
		
		// *** Erstellen der URL für die Abfrage
		String url = "http://ws.geonames.org/postalCodeSearch?postalcode=" + postalCode + "&country=" + countryIso2 + "lang=" + language + "&style=full";
		
		try {
			Node		n;
			Node		currN;
			NodeList	nList;
			NodeList	nListSub;
			
			File	file;
			
			// den Parser anzapfen
			DOMParser p = new DOMParser();
			// den Inhalt einlesen
			p.parse(url);
			Document doc = p.getDocument();
			
			// next
			n = doc.getDocumentElement().getFirstChild();
			n = n.getNextSibling();
			nList = n.getParentNode().getChildNodes();
			for (int i = 0; i < nList.getLength(); i++)	{
				currN = nList.item(i);
				String currNodeName = currN.getNodeName();
				if (currNodeName.equals("#text") == false)	{
					if (currNodeName.equals("code") == false)	{
						System.out.println("Child " + i + " Name:      " + currN.getNodeName());
						System.out.println("Child " + i + " Type:      " + currN.getNodeType());
						System.out.println("Child " + i + " String:    " + currN.getNodeValue());
						System.out.println("Child " + i + " Text:      " + currN.getTextContent());
						System.out.println("Child " + i + " LocalName: " + currN.getLocalName());
					}
					nListSub = currN.getChildNodes();
					for (int j = 0; j < nListSub.getLength(); j++)	{
						currN = nListSub.item(j);
						if (currN.getNodeName().equals("#text") == false)	{
							System.out.println("Child " + i + " Name:      " + currN.getNodeName());
							System.out.println("Child " + i + " Type:      " + currN.getNodeType());
							System.out.println("Child " + i + " String:    " + currN.getNodeValue());
							System.out.println("Child " + i + " Text:      " + currN.getTextContent());
							System.out.println("Child " + i + " LocalName: " + currN.getLocalName());
						}
					}
				}
				
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	/*	try {
			Node n;
			Node currN;
			NodeList nList;
			NodeList nListSub;
			DOMParser p = new DOMParser();
			p.parse(xmlDoc);
			Document doc = p.getDocument();
			
			// Anzahl gefundener Einträge <totalResultsCount>
			n = doc.getDocumentElement().getFirstChild();
			n = n.getNextSibling();
			//System.out.println("Anzahl gefundener Einträge: " + n.getNodeName());
			
			// next
			nList = n.getParentNode().getChildNodes();
			System.out.println("Und nun: " + nList.getLength());
			for (int i = 0; i < nList.getLength(); i++)	{
				currN = nList.item(i);
				String currNodeName = currN.getNodeName();
				if (currNodeName.equals("#text") == false)	{
					if (currNodeName.equals("code") == false)	{
						System.out.println("Child " + i + " Name:      " + currN.getNodeName());
						System.out.println("Child " + i + " Type:      " + currN.getNodeType());
						System.out.println("Child " + i + " String:    " + currN.getNodeValue());
						System.out.println("Child " + i + " Text:      " + currN.getTextContent());
						System.out.println("Child " + i + " LocalName: " + currN.getLocalName());
					}
					nListSub = currN.getChildNodes();
					for (int j = 0; j < nListSub.getLength(); j++)	{
						currN = nListSub.item(j);
						if (currN.getNodeName().equals("#text") == false)	{
							System.out.println("Child " + i + " Name:      " + currN.getNodeName());
							System.out.println("Child " + i + " Type:      " + currN.getNodeType());
							System.out.println("Child " + i + " String:    " + currN.getNodeValue());
							System.out.println("Child " + i + " Text:      " + currN.getTextContent());
							System.out.println("Child " + i + " LocalName: " + currN.getLocalName());
						}
					}
				}
				
			}
		}
		*/
		return result;
	}
	
	
	public static void xmlParser(final String xmlDoc) {
		try {
			Node n;
			Node currN;
			NodeList nList;
			NodeList nListSub;
			DOMParser p = new DOMParser();
			p.parse(xmlDoc);
			Document doc = p.getDocument();
					
			/*
			 * <>
			 * 
			 * 
			 */
			
			// Anzahl gefundener Einträge <totalResultsCount>
			//System.out.println("root: " + doc.getFirstChild().getNodeName());
			
			// Anzahl gefundener Einträge <totalResultsCount>
			n = doc.getDocumentElement().getFirstChild();
			n = n.getNextSibling();
			//System.out.println("Anzahl gefundener Einträge: " + n.getNodeName());
			
			// next
			nList = n.getParentNode().getChildNodes();
			System.out.println("Und nun: " + nList.getLength());
			for (int i = 0; i < nList.getLength(); i++)	{
				currN = nList.item(i);
				String currNodeName = currN.getNodeName();
				if (currNodeName.equals("#text") == false)	{
					if (currNodeName.equals("code") == false)	{
						System.out.println("Child " + i + " Name:      " + currN.getNodeName());
						System.out.println("Child " + i + " Type:      " + currN.getNodeType());
						System.out.println("Child " + i + " String:    " + currN.getNodeValue());
						System.out.println("Child " + i + " Text:      " + currN.getTextContent());
						System.out.println("Child " + i + " LocalName: " + currN.getLocalName());
					}
					nListSub = currN.getChildNodes();
					for (int j = 0; j < nListSub.getLength(); j++)	{
						currN = nListSub.item(j);
						if (currN.getNodeName().equals("#text") == false)	{
							System.out.println("Child " + i + " Name:      " + currN.getNodeName());
							System.out.println("Child " + i + " Type:      " + currN.getNodeType());
							System.out.println("Child " + i + " String:    " + currN.getNodeValue());
							System.out.println("Child " + i + " Text:      " + currN.getTextContent());
							System.out.println("Child " + i + " LocalName: " + currN.getLocalName());
						}
					}
				}
				
			}
			
			
			/*Node n = doc.getDocumentElement().getFirstChild();
			while (n!=null && !n.getNodeName().equals("totalResultsCount")) 
				n = n.getNextSibling();
			PrintStream out = System.out;
			out.println("<?xml version=\"1.0\"?>");
			out.println("<totalResultsCount>");
			if (n!=null)
				print(n, out);
			*/
			System.out.println("</code>");
			
		} catch (Exception e)	{
			e.printStackTrace();
		}
	}

	
	static void print(Node node, PrintStream out) {
		    int type = node.getNodeType();
		    switch (type) {
		      case Node.ELEMENT_NODE:
		        out.print("<" + node.getNodeName());
		        NamedNodeMap attrs = node.getAttributes();
		        int len = attrs.getLength();
		        for (int i=0; i<len; i++) {
		            Attr attr = (Attr)attrs.item(i);
		            out.print(" " + attr.getNodeName() + "=\"" +
		                      escapeXML(attr.getNodeValue()) + "\"");
		        }
		        out.print('>');
		        NodeList children = node.getChildNodes();
		        len = children.getLength();
		        for (int i=0; i<len; i++)
		          print(children.item(i), out);
		        out.print("</" + node.getNodeName() + ">");
		        break;
		      case Node.ENTITY_REFERENCE_NODE:
		        out.print("&" + node.getNodeName() + ";");
		        break;
		      case Node.CDATA_SECTION_NODE:
		        out.print("<![CDATA[" + node.getNodeValue() + "]]>");
		        break;
		      case Node.TEXT_NODE:
		        out.print(escapeXML(node.getNodeValue()));
		        break;
		      case Node.PROCESSING_INSTRUCTION_NODE:
		        out.print("<?" + node.getNodeName());
		        String data = node.getNodeValue();
		        if (data!=null && data.length()>0)
		           out.print(" " + data);
		        out.println("?>");
		        break;
		    }
		  }

		  static String escapeXML(String s) {
		    StringBuffer str = new StringBuffer();
		    int len = (s != null) ? s.length() : 0;
		    for (int i=0; i<len; i++) {
		       char ch = s.charAt(i);
		       switch (ch) {
		       case '<': str.append("&lt;"); break;
		       case '>': str.append("&gt;"); break;
		       case '&': str.append("&amp;"); break;
		       case '"': str.append("&quot;"); break;
		       case '\'': str.append("&apos;"); break;
		       default: str.append(ch);
		     }
		    }
		    return str.toString();
		  }
	public void tmp(final String countryCode, final String tempDir)	{
		String downloadLoc;
		String theFileName;
		
		// die Datei in das Temp downloaden
		theFileName = countryCode + ".zip";
		downloadLoc = "http://download.geonames.org/export/dump/";
		FileDataDownload.FileDownload(downloadLoc + theFileName, theFileName, tempDir);
		// den Inhalt des zip-Files in Temp dekomprimieren
		zipReader(tempDir + theFileName, countryCode + ".txt", tempDir);
		
		// die Datei in das Temp downloaden
		String srcFileName = countryCode + ".zip";
		String dstFileName = countryCode + "zip.zip";
		downloadLoc = "http://download.geonames.org/export/zip/";
		FileDataDownload.FileDownload(downloadLoc + srcFileName, dstFileName, tempDir);
		// den Inhalt des zip-Files in Temp dekomprimieren
		zipReader(tempDir + dstFileName, countryCode + "zip.txt", tempDir);
		importTabDelimited(tempDir + countryCode + "zip.txt");
	}
	
	private void importTabDelimited(final String file){
		try {
			char delimiter = (char)9;
			CSVReader cr = new CSVReader(new FileReader(file), delimiter);
			String[] line;
			while ((line = cr.readNext()) != null) {
				//importLine(line);
		    	// TODO hier könnte man noch ggf ; durch , ersetzen (Excel -> csv !)
		    	//line = StringTool.convertEncoding(line, SRC_ENCODING);
				new Plz(line[0],	// Land (+)
						line[0],	// LandIso2 +
						line[1],	// Plz +
						line[2],	// Ort +
						"",			// Strasse (+)
						line[3],	// Kanton +
						line[4]);	// KantonKuerzel +
/*
00 country code      : iso country code, 2 characters
01 postal code       : varchar(10)
02 place name        : varchar(180)
03 admin name1       : 1. order subdivision (state) varchar(100)
04 admin code1       : 1. order subdivision (state) varchar(20)
05 admin name2       : 2. order subdivision (county/province) varchar(100)
06 admin code2       : 2. order subdivision (county/province) varchar(20)
07 admin name3       : 3. order subdivision (community) varchar(100)
08 atitude          : estimated latitude (wgs84)
09 longitude         : estimated longitude (wgs84)
10 accuracy          : accuracy of lat/lng from 1=estimated to 6=centroid
*/
			}
			return;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return;
		}
		
	}

	public void readIntoDB(final String file)	{
		try {
			FileReader fr = new FileReader(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	public void zipReader(final String zipFile, final String dataFileName, final String tempDir)	{
		ZipInputStream inStream;
		try {
			inStream = new ZipInputStream(new FileInputStream(zipFile));
			byte[] buffer = new byte[1024];
			int read;
			ZipEntry entry;
			while ((entry = inStream.getNextEntry()) != null) {
				OutputStream outStream = new FileOutputStream(tempDir + dataFileName);
				while ((read = inStream.read(buffer)) > 0) {
					outStream.write(buffer, 0, read);
				}
				outStream.close();
			}
			inStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}