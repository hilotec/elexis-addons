/*******************************************************************************
 * Copyright (c) 2007, medshare and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    M. Imhof - initial implementation
 *    
 * $Id: DirectoriesContentParser.java 5277 2009-05-05 19:00:19Z tschaller $
 *******************************************************************************/

package ch.medshare.elexis.directories_marlovits;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
;

/*
http://www1.dastelefonbuch.de/?la=de&kw=Marlovits&ci=&ciid=&cmd=search&cifav=0&mdest=sec1.www1&vert_ok=1&recfrom=1

reccount=10

http://www2.dastelefonbuch.de/Erweiterte-Suche.html?la=de&cmd=&s=a30000&sp=1&aktion=26&kw=Marlovits&ort=testort&firstname=Haribo
http://www2.dastelefonbuch.de/Erweiterte-Suche.html?la=de&cmd=&s=a30000&sp=1&aktion=26&kw=Marlovits&ort=testort&firstname=Haribo
ist mit method post - get geht auch
 */

public class DirectoriesContentParser extends HtmlParser {
	
	// some constants
	private static String MARKER_VCARD_BEGIN_   = "BEGIN:VCARD";
	private static String MARKER_VCARD_END_     = "END:VCARD";
	private static String LOCAL_COPYRIGHT      = "\\[Copyright \\(c\\) local\\.ch ag\\]";
	
	//+++++ flag for testing my new version
	private static final Boolean useNewVersion = true;
	
	private static final String ADR_LIST_TAG = "class=\"vcard searchResult resrowclr"; //$NON-NLS-1$
	private static final String ADR_DETAIL_TAG = "<div class=\"resrowclr";; //$NON-NLS-1$
	//+++++ START
	private static final String   ADR_LEDIGNAMENSTRENNER = "\\(-";
	private static final String   ADR_LEDIGNAMENSSCHLUSS = "\\)";
	private static final String[] ADR_VORNAMENSTRENNER = {" und ",  " u\\. ",  " e ",  " et "};
	
	//
	private int maxEntriesToRead = 20;
	private String name = "";
	private String geo  = "";
	private String country = "ch";
	
	// all titles without period
	private static String[] ADR_TITLES = {	"", // 
											"Prof. Dr. med. dent. ",
											"Prof. Dr. méd. dent. ",
											"Prof. Dr. med. vet. ",
											"Prof. Dr. méd. vét. ",
											"Prof. Dr. med. ",
											"Prof. Dr. méd. ",
											"Prof. Dr. ",
											"Prof. Dr. med. ",
											"Prof. Dr. méd. ",
											"Prof. ",
											"Dr. med. dent. ",
											"Dr. méd. dent. ",
											"Dr. med. vet. ",
											"Dr. méd. vét. ",
											"Dr. med. ",
											"Dr. méd. ",
											"PD. Dr. med. dent. ",
											"PD. Dr. méd. dent. ",
											"PD. Dr. med. ",
											"PD. Dr. méd. "
											};
	//+++++ END
/*
	public DirectoriesContentParser(String htmlText){
		super(htmlText);
	}
*/	
	public DirectoriesContentParser(String htmlText, String name, String geo, String country){
		super(htmlText);
		this.name = name;
		this.geo  = geo;
		this.country = country;
		
/*		if (ADR_TITLES[0].equalsIgnoreCase(""))	{
			for (int i = 1; i < ADR_TITLES.length; i++)	{
				ADR_TITLES[i] = ADR_TITLES[i].replaceAll(" ", "[\\\\.]? ");
			}
			ADR_TITLES[0] = "I_N_I_T_E_D";
		}
		System.out.println("");
*/	}
	
	/**
	 * Retourniert String in umgekehrter Reihenfolge
	 */
	private String reverseString(String text){
		if (text == null) {
			return "";
		}
		String reversed = "";
		for (char c : text.toCharArray()) {
			reversed = c + reversed;
		}
		return reversed;
	}
	
	private static String[] getVornameNachname(String text){
		String vorname = ""; //$NON-NLS-1$
		String nachname = text;
		int nameEndIndex = text.trim().indexOf(" "); //$NON-NLS-1$
		if (nameEndIndex > 0) {
			vorname = text.trim().substring(nameEndIndex).trim();
			nachname = text.trim().substring(0, nameEndIndex).trim();
		}
		return new String[] {
			vorname, nachname
		};
	}
	
	private static String removeDirt(String text){
		return text.replace("<span class=\"highlight\">", "").replace("</span>", "");
	}
	
	/**
	 * Informationen zur Suche werden extrahiert. Bsp: <div class="summary"> <strong>23</strong>
	 * Treffer für <strong class="what">müller hans</strong> in <strong class="where">bern</strong>
	 * <div id="printlink" .... <span class="spacer">&nbsp;</span> <a
	 * href="http://tel.local.ch/de/">Neue Suche</a> </div>
	 */
	public String getSearchInfo(){
		reset();
		String searchInfoText = "";
		if (country.equalsIgnoreCase("ch"))	{
			searchInfoText = extract("<div class=\"summary\">", "<div id=\"printlink\"");
			if (searchInfoText == null) {
				return "";//$NON-NLS-1$
			}
			// noPrint-Anteil aus String entfernen
			searchInfoText = searchInfoText.replaceAll("<span class=\"totalResults noPrint\">[^<]+</span>", "");
			// alle HTML <xxx> entfernen
			searchInfoText = searchInfoText.replaceAll("<[^>]+>", "");
			// obere Grenze Resultate ändern, extract upper bounds
	/*		String upperBoundsStr = searchInfoText.replaceAll(	"([^-]*[^0-9]+[0-9]+[^0-9]+)", "");
			upperBoundsStr = upperBoundsStr.replaceAll("([0-9]+)([^0-9]+)", "$1");
			int upperBounds = Integer.parseInt(upperBoundsStr);
			// jetzt korrigierte upper bounds einsetzen
			upperBounds = (upperBounds > maxEntriesToRead) ? maxEntriesToRead : upperBounds;
			//searchInfoText = searchInfoText.replaceAll("([^-]*[^0-9]+)([0-9]+)([^0-9]+)", "$1$2" + upperBounds + "$3");
			searchInfoText = searchInfoText.replaceAll("([^-]*[^0-9]+)([0-9]+)([^0-9]+)", "$1" + upperBounds + "$3");
			 */
		} else	{
			if (moveTo("<div class=\"functionbar\">"))	{
				searchInfoText = extract("<li class=\"blank\">", "</li>");
				// alle HTML <xxx> entfernen
				searchInfoText = searchInfoText.replaceAll("<[^>]+>", "");
				searchInfoText = searchInfoText.replaceAll("[\\r\\n\\t]", "");
			}
		}
		// return result
		return searchInfoText;
	}
	
	/**
	 * extracts the total number of found entries
	 * @return number of found entries
	 */
	public int getNumOfEntries()	{
		reset();
		String resultStr = "0";
		if (country.equalsIgnoreCase("ch"))	{
			moveTo("<div class=\"printResultSummary printOnly\">");
			resultStr = extract("</div><strong>", "</strong>");
			// if not found at all -> return 0
			if (resultStr.equalsIgnoreCase("")) return 0;
			// if string contains other than numbers -> return 0
			if (!resultStr.replaceAll("[0-9]", "").equalsIgnoreCase("")) return 0;
			// now it should be ok
		} else if (country.equalsIgnoreCase("de"))	{
			if (moveTo("<div class=\"hits\">"))	{
				resultStr = extract("<span>", "</span>");
				resultStr = resultStr.replaceAll("[^0-9]", "");
			}
		}
		return Integer.parseInt(resultStr);
	}
	
	public boolean hasCitiesList()	{
		reset();
		if (moveTo("<div id=\"content\" class=\"hitlist place-sel\">"))	{
			boolean hasNextCategory = moveTo("<div class=\"functionbar\">");
			while(hasNextCategory)	{
				String category = extractTo("</div>").trim();
				// if last char is ")" then it IS a category
				if (category.endsWith(")"))	{
					return true;
				}
				hasNextCategory = moveTo("<div class=\"functionbar\">");
			}
		}
		return false;
	}
	
	public String getCitiesHitListMessage()	{
		if (!hasCitiesList()) return "";
		reset();
		if (!moveTo("<!-- Meldung -->")) return "";
		String message = extract("<div id=\"msg-caution\">", "</div>");
		message = message.replaceAll("[\\r\\n\\t]", "").trim();
		message = message.replaceAll("<br[ ]*[/]*[ ]*>", "\n");
		message = StringEscapeUtils.unescapeHtml(message);
		return message;
	}
	
	/**
	 * returns a list of possibly matching city names if the entered city could not be found
	 * @return String[] the list of city-pairs, null if none found. 
	 *                  each entry consist of following parts: city - selectable.
	 *                  if the entry is not selectable, then it is a category for the following entries
	 */
	//<div id="content" class="hitlist place-sel">
	public String[][] getCitiesHitList()	{
		reset();
		if (moveTo("<div id=\"content\" class=\"hitlist place-sel\">"))	{
			// get all categories
			boolean hasNextCategory = moveTo("<div class=\"functionbar\">");
			int rowCount = 0;
			String tempResult = "";
			String delim = "";
			while(hasNextCategory)	{
				String category = extractTo("</div>").trim();
				// if last char is ")" then it IS a category
				if (category.endsWith(")"))	{
					System.out.println("category: " + category);
					tempResult = tempResult + delim + category;
					delim = ";";
					tempResult = tempResult + delim + 0;
					rowCount++;
					// extract part up to next category
					hasNextCategory = (getNextPos("<div class=\"functionbar\">") >= 0);
					String part = extractTo("<div class=\"functionbar\">");
					// replace splitters 
					part = part.replaceAll("</table>", "___ROWSPLITTER___");
					part = part.replaceAll("</a>",     "___CELLSPLITTER___");
					// strip html tags and blanks/returns
					part = part.replaceAll("<[^>]+>", "");
					part = part.replaceAll("[\\r\\n\\t]", "");
					// split on </table> -> rows
					String[] rows = part.split("___ROWSPLITTER___");
					// start with second row because first row is the header
					for (int rowNum = 1; rowNum < rows.length; rowNum++)	{
						String row = rows[rowNum];
						// split on </a> -> cells
						String[] cells = row.split("___CELLSPLITTER___");
						if (cells.length > 2)	{
							rowCount++;
							String cell = cells[2].trim();
							System.out.println("cell: " + cell);
							tempResult = tempResult + delim + cell + delim + 1;
						}
					}
				} else {
					hasNextCategory = false;
				}
			}
			System.out.println(tempResult);
			String [][] cities  = new String[rowCount][2];
			String[] splittedTemp = tempResult.split(";");
			for (int i = 0; i < splittedTemp.length; i++)	{
				String name = splittedTemp[i];
				i++;
				String selectable = splittedTemp[i];
				cities[i/2][0] = name;
				cities[i/2][1] = selectable;
			}
			return cities;
		}
		return null;
	}

	/**
	 * Extrahiert Informationen aus dem retournierten Html. Anhand der <div class="xxx"> kann
	 * entschieden werden, ob es sich um eine Liste oder einen Detaileintrag (mit Telefon handelt).
	 * 
	 * Detaileinträge: "adrNameDetLev0", "adrNameDetLev1", "adrNameDetLev3" Nur Detaileintrag
	 * "adrNameDetLev2" darf nicht extrahiert werden
	 * 
	 * Listeinträge: "adrListLev0", "adrListLev1", "adrListLev3" Nur Listeintrag "adrListLev0Cat"
	 * darf nicht extrahiert werden
	 */
	public List<KontaktEntry> extractKontakte() throws IOException{
		reset();
		List<KontaktEntry> kontakte = new Vector<KontaktEntry>();
		
		if (country.equalsIgnoreCase("ch"))	{
			int listIndex = getNextPos(ADR_LIST_TAG);
			int detailIndex = getNextPos(ADR_DETAIL_TAG);
			while (listIndex > 0 || detailIndex > 0) {
				KontaktEntry entry = null;
				if (detailIndex < 0 || (listIndex >= 0 && listIndex < detailIndex)) {
					// Parsing Liste
					entry = extractListKontakt();
				} else if (listIndex < 0 || (detailIndex >= 0 && detailIndex < listIndex)) {
					// Parsing Einzeladresse
					entry = extractKontakt();
				}
				if (entry != null) {
					kontakte.add(entry);
				}
				listIndex = getNextPos(ADR_LIST_TAG);
				detailIndex = getNextPos(ADR_DETAIL_TAG);
			}
		} else if (country.equalsIgnoreCase("de"))	{
			// if there is a hits entry, then we found some data, else there are no entries
			boolean foundit = moveTo("<div class=\"hits\">");
			if (foundit)	{
				// skip first <td class=\"col1\">
				moveTo("folgendes MUSS in einer Zeile stehen, sonst macht IE Abstaende ");
				int listIndex = getNextPos("<td class=\"col1\">", 0);
				while (listIndex > 0) {
					KontaktEntry entry = null;
					entry = extractListKontakt();
					if (entry != null) {
						kontakte.add(entry);
					}
					listIndex = getNextPos("<td class=\"col1\">");
				}
			}
		}
		
		return kontakte;
	}

	/**
	 * Extrahiert einen Kontakt aus einem Listeintrag Bsp: <div id="te_ojUHu3vXsUWJbXidz2_sRQ"
	 * onmouseover="lcl.search.onEntryHover(this)" onclick="if (typeof(lcl.search) != 'undefined') { lcl.search.navigateTo(event, 'http://tel.local.ch/de/d/ILwo-yKRTlguXS4TFuVPuA?what=Meier&start=3'); }"
	 * class="vcard searchResult resrowclr_yellow mappable"> <div class="imgbox"> <a
	 * href="http://tel.local.ch/de/d/ILwo-yKRTlguXS4TFuVPuA?what=Meier&amp;start=3"> <img
	 * xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	 * src="http://s.staticlocal.ch/images/pois/na/blue1.png"
	 * alt="Dieser Eintrag kann auf der Karte angezeigt werden" height="26" width="27" /> </a>
	 * </div> <div class="entrybox"> <h4>
	 * <span class="category" title="Garage"> Garage </span> <br>
	 * <a class="fn" href="http://tel.local.ch/de/d/ILwo-yKRTlguXS4TFuVPuA?what=Meier&amp;start=3">
	 * Autocenter <span class="highlight">Meier</span> AG </a> </br></h4> <p
	 * class="bold phoneNumber"> <span class="label">Tel.</span> <span class="tel"> <a
	 * class="phonenr" href="callto://+41627234359"> 062 723 43 59 </a> </span> </p> <p class="adr">
	 * <span class="street-address"> Hauptstrasse 158 </span> , <span
	 * class="postal-code">5742</span> <span class="locality">Kölliken</span> </p> </div> <div
	 * style="clear: both;"></div> </div>
	 */
	private KontaktEntry extractListKontakt() throws IOException, MalformedURLException{
		
		if (country.equalsIgnoreCase("de"))	{
			// im deutschen Telefonbuch gibt es auch eine vCard - da steht aber nicht mehr drinnen
			// und Name/Vorname ist auch nicht getrennt -> ergo einfach direkt alles aus der Homepage ziehen
			// das geht schneller...
			// doch doch: in der vCard sind alle telnr aufgeführt
			// und Geschäftlich ist unterscheidbar aufgrund Eintrag Telnr in Gesch.
			
			// extract main parts
			moveTo("<td class=\"col1\">");
			String namePart = extractTo("</td>");
			
			moveTo("<td class=\"col2\">");
			String streetPart = extractTo("</td>");
			
			moveTo("<td class=\"col3\">");
			String zipCityPart = extractTo("</td>");
			
			moveTo("<td class=\"col4\">");
			String phonePart = extractTo("</td>");
			
			moveTo("<td class=\"col5\">");
			String editPart = extractTo("</td>");
			
			String optionalPart = extractTo("<tr class=\"dtl-preview hide\">");
			
			String hiddenPart = "";
			int nextPos = getNextPos("folgendes MUSS in einer Zeile stehen, sonst macht IE Abstaende");
			if (nextPos >= 0)	{
				hiddenPart = extractTo("folgendes MUSS in einer Zeile stehen, sonst macht IE Abstaende");
			} else {
				hiddenPart = getTail();
			}
			
			int tmpPos = 0;
			// *** extract actual name and firstname
			tmpPos = namePart.indexOf("<div class=\"long hide\">");
			tmpPos = tmpPos + "<div class=\"long hide\">".length();
			tmpPos = namePart.indexOf("rel=\"nofollow\"", tmpPos) + "rel=\"nofollow\"".length();
			tmpPos = namePart.indexOf(">", tmpPos) + 1;
			namePart = namePart.substring(tmpPos);
			String[] fullName_ = namePart.split("</a>");
			String fullName = fullName_[0];
			fullName = fullName.replaceAll("[\\r\\n\\t]", "").trim();
			String[] nachnameVorname = getVornameNachname(fullName);
			String nachname = nachnameVorname[1];
			String vorname  = nachnameVorname[0];
			
			// *** extract actual street
			String street = streetPart.replaceAll("[\\r\\n\\t]", "").trim().replace("&nbsp;", " ");
			
			// *** extract actual zip and city
			String zipCity = zipCityPart.replaceAll("[\\r\\n\\t]", "").trim();
			String[] zipCity_ = zipCity.split(" ");
			String zip  = zipCity_[0];
			String city = "";
			if (zipCity_.length > 1) city = zipCity_[1];
			
			// *** extract actual phone/fax
			String phone = "";
			String fax   = "";
			// phone AND fax: part contains src="img/a12000_fonfax.gif"
			if (phonePart.indexOf("src=\"img/a12000_fonfax.gif\"") >= 0)	{
				phonePart = phonePart.replaceAll("<[^>]+>", "");
				phonePart = phonePart.replaceAll("[\\r\\n\\t]", "").trim();
				phone = phonePart;
				fax   = phonePart;
			}
			// ONLY fax: part contains src="img/a12000_fax.gif"
			else if (phonePart.indexOf("src=\"img/a12000_fax.gif\"") >= 0)	{
				phonePart = phonePart.replaceAll("<[^>]+>", "");
				phonePart = phonePart.replaceAll("[\\r\\n\\t]", "").trim();
				fax   = phonePart;
			}
			else {
				phonePart = phonePart.replaceAll("<[^>]+>", "");
				phonePart = phonePart.replaceAll("[\\r\\n\\t]", "").trim();
				phone = phonePart;
			}
			
			// *** extract website
			String website = "";
			tmpPos = optionalPart.indexOf("alt=\"Web\"");
			if (tmpPos > -1)	{
				tmpPos = tmpPos + "alt=\"Web\"".length();
				tmpPos = optionalPart.indexOf("href=\"", tmpPos) + "href=\"".length();
				website = optionalPart.substring(tmpPos);
				String[] website_ = website.split("\"");
				website = website_[0];
				website = website.replaceAll("[\\r\\n\\t]", "").trim();
			}
			
			// *** extract email
			String email = "";
			tmpPos = optionalPart.indexOf("href=\"mailto:");
			if (tmpPos > -1)	{
				tmpPos = tmpPos + "href=\"mailto:".length();
				email = optionalPart.substring(tmpPos);
				String[] email_ = email.split("\"");
				email = email_[0];
				email = email.replaceAll("[\\r\\n\\t]", "").trim();
			}
			
			// *** extract vcard url
			String vCard = "";
			tmpPos = hiddenPart.indexOf("href=\"VCard");
			if (tmpPos > -1)	{
				tmpPos = tmpPos + "href=\"VCard".length();
				vCard = hiddenPart.substring(tmpPos);
				String[] vCard_ = vCard.split("\" target=\"_blank\"");
				vCard = vCard_[0];
				if (!vCard.equalsIgnoreCase(""))	{
					vCard = "http://www2.dastelefonbuch.de/VCard" + vCard;
				}
			}
			
			System.out.println("");
			
			String zusatz          = "";
			String tel2            = "";
			String mobile          = "";
			String ledigname       = "";
			String profession      = "";
			String category        = "";
			boolean isOrganisation = false;
			String title           = "";
			return new KontaktEntry(vorname, nachname, zusatz, //$NON-NLS-1$
					street, zip, city, phone, fax, email, false, //$NON-NLS-1$
					//+++++ new:
					false, vCard, website, tel2, mobile,
					ledigname, profession, category, isOrganisation, title, "DEU");
		}
		
		if (!moveTo(ADR_LIST_TAG)) { // Kein neuer Eintrag
			return null;
		}
		
		// Name, Vorname, Zusatz
		moveTo("<div class=\"entrybox\">");
		
		int catIndex = getNextPos("<span class=\"category\"");
		int nextEntryPoxIndex = getNextPos("<div class=\"entrybox\">");
		String zusatz = "";
		if (catIndex > 0 && catIndex < nextEntryPoxIndex) {
			moveTo("<span class=\"category\"");
			zusatz = extract("\">", "</span>");
		}
		
		// Name, Vorname
		moveTo("<a class=\"fn\"");
		
		String nameVornameText = extract("\">", "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		nameVornameText = removeDirt(nameVornameText);
		
		if (nameVornameText == null || nameVornameText.length() == 0) { // Keine leeren Inhalte
			return null;
		}
		String[] vornameNachname = getVornameNachname(nameVornameText);
		String vorname = vornameNachname[0];
		String nachname = vornameNachname[1];
		
		// Tel-Nr - Vorsicht - es gibt wirklich Einträge OHNE TelNr!
		moveTo("<span class=\"tel\"");
		String telNr = "";
		if (moveTo("<a class=\"phonenr\""))	{
			telNr = extract(">", "</a>").replace("&nbsp;", "").replace("*", "").trim();
		}
		
		// Adresse, Ort, Plz
		String adressTxt = extract("<p class=\"adr\">", "</p>");
		// 5.5.09 ts: verschachtelte spans -> alles bis zur nächsten span klasse holen
		String strasse =
			removeDirt(new HtmlParser(adressTxt).extract("<span class=\"street-address\">",
				", <span class="));
		String plz =
			removeDirt(new HtmlParser(adressTxt).extract("<span class=\"postal-code\">", "</span>"));
		String ort =
			removeDirt(new HtmlParser(adressTxt).extract("<span class=\"locality\">", "</span>"));
		
		// read DetailPage-Link
		String vCard = extract("<a class=\"detaillink\" href=\"", "\">");
		// replace "/de/d/", "/fr/d/", "/it/d/", "/en/d/" by "/vard/" to get the vcard-url
		vCard = vCard.replaceAll("tel\\.local\\.ch/[a-zA-Z][a-zA-Z]/d/", "tel\\.local\\.ch/vcard/");
		// and strip "?what=<searchTerm>" from the end to get the vcard-url
		vCard = vCard.split("\\?what=")[0];
		
		String website         = "";
		String tel2            = "";
		String mobile          = "";
		String ledigname       = "";
		String profession      = "";
		String category        = "";
		boolean isOrganisation = false;
		String title           = "";
		return new KontaktEntry(vorname, nachname, zusatz, //$NON-NLS-1$
				strasse, plz, ort, telNr, "", "", false, //$NON-NLS-1$
				//+++++ new:
				false, vCard, website, tel2, mobile,
				ledigname, profession, category, isOrganisation, title, "CHE");
	}
	
	public static String getDocReturnCharacter(final String contents)	{
		// get the used newline/return for this content
		String returnChar = "";
		int crlfPos = contents.indexOf("\r\n");
		if (crlfPos >= 0){
			returnChar = "\r\n";
		} else	{
			int crPos = contents.indexOf("\r");
			if (crPos >= 0){
				returnChar = "\r";
			} else	{
				returnChar = "\n";
			}
		}
		return returnChar;
	}
	
	//+++++ START Extract vCard Contents
	/*
	 * For specification of vCard see
	 *      - http://en.wikipedia.org/wiki/VCard
	 *      - http://www.ietf.org/rfc/rfc2426.txt
	 * 
	 * 
	 *  Matching:
	 *  N:          Name, strukturiert: <Nachname>;<Vorname>
	 *  FN:         Formatted Name: Fullname, Vorname und Nachname oder OrgName oder andere Bezeichnung
	 *  PHOTO:      ignorieren, falls tatsächlich vorhanden
	 *  BDAY:       einlesen, falls wirklich dereinst vorhanden +++++ format?
	 *  ADR:        als item1.ADR: strukturierte Adresse, Trenner ";" +++++
	 *              <Postfach_uä_Nach_PLZ_ORT>;<>;<Address1_StrasseUndNr>;<Ort>;<Kanton>;<PLZ>;<>
	 *              innerhalb eines Teils können Zeilenumbrüche mit Komma (!) eingefügt werden
	 *  TEL:        Telefon, Fax, at Work/at Home
	 *              TYPE=VOICE
	 *              TYPE=FAX
	 *  item1.ADR:  Adresse, Teile getrennt durch ";", at Work/at Home
	 *  URL:        genau das ist es
	 *  NOTE:       Verschiedenes
	 *  TITLE:      zBsp Beruf, anderes
	 *  ORG:        OrgName
	 *  X-ABShowAs: "COMPANY": ist eine Organisation -> Checkbox setzen
	 *   
	 */
	
	/**
	 * @param firstnames input
	 * @param delimiter used to separate the firstnames
	 * @return the separated firstnames, delimited by &lt;delimiter&gt;
	 */
	public static String extractVornamen(final String firstnames, final String delimiter)	{
		String result = "";
		String lFirstnames = firstnames;
		for (int fn_sepIx = 0; fn_sepIx < ADR_VORNAMENSTRENNER.length; fn_sepIx++)	{
			String fn_sep = ADR_VORNAMENSTRENNER[fn_sepIx];
			String[] parts = lFirstnames.split(fn_sep);
			String lDelimiter = "";
			result = "";
			for (int partsIx = 0; partsIx < parts.length; partsIx++)	{
				result = result + lDelimiter + parts[partsIx].trim();
				lDelimiter = delimiter;
			}
			lFirstnames = result;
		}
		return result;
	}
	/*
		vorname;
		name;
		zusatz;
		adresse;
		plz;
		ort;
		tel;
		fax;
		email;
		isDetail;
		isVCardDetail;
		detailLinkLink;
		website;
		tel2;
		mobile;
		ledigname;
		profession;
		category;
	*/
	public static KontaktEntry parseVCard(final String vCardURL, final String country)	{
		// read contents of vCard
		String vCardContents = "";
		try {
			vCardContents = readContent(vCardURL);
			//parseVCard(vCardContents);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		// get the used newline/return for this vcard
		String MARKER_VCARD_NEWLINE = getDocReturnCharacter(vCardContents);
		
		// 
		String MARKER_VCARD_BEGIN = MARKER_VCARD_BEGIN_ + MARKER_VCARD_NEWLINE;
		String MARKER_VCARD_END   = MARKER_VCARD_END_;
		
		// test if this really is a valid vCard: Start with BEGIN:VCARD, end with END:VCARD
		if (!vCardContents.substring(0, MARKER_VCARD_BEGIN.length()).equalsIgnoreCase(MARKER_VCARD_BEGIN))	{
			return null;
		}
		int endMarkerPos = vCardContents.indexOf(MARKER_VCARD_END);
		if (endMarkerPos == -1)	{
			return null;
		}
		
		Boolean isCompany = false;
		if (country.toLowerCase().equalsIgnoreCase("ch"))	{
			// test if this is company: X-ABShowAs:COMPANY entry
			isCompany = false;
			if (vCardContents.indexOf("X-ABShowAs:COMPANY") >= 0)	{
				isCompany = true;
			}
		}
		if (country.toLowerCase().equalsIgnoreCase("de"))	{
			// test if this is company: X-ABShowAs:COMPANY entry
			isCompany = false;
			if (vCardContents.indexOf("TEL;WORK") >= 0)	{
				if (vCardContents.indexOf("TEL;HOME") >= 0)	{
					isCompany = false;
				} else	{
					isCompany = true;
				}
			}
		}
		
		// now extract the values, one by one, in the order needed by Dialog
		// for telefonbuch.de this is always empty
		String vCardVorname = getVCardValue(vCardContents, "Vorname", isCompany, country);
		vCardVorname = extractVornamen(formatString(vCardVorname), ";");
		// for telefonbuch.de this contains firstname
		String vCardName = formatString(getVCardValue(vCardContents, "Name", isCompany, country));
		if (country.toLowerCase().equalsIgnoreCase("de"))	{
			if (!isCompany)	{
				String[] firstNameLastName = getVornameNachname(vCardName);
				vCardVorname = extractVornamen(firstNameLastName[0], ";");
				vCardName    = firstNameLastName[1];
			}
		}
		String vCardZusatz = getVCardValue(vCardContents, "Note", isCompany, country);
		vCardZusatz = formatString(vCardZusatz);
		// strip copyright notice [Copyright (c) local.ch ag]
		vCardZusatz = vCardZusatz.replaceAll(MARKER_VCARD_NEWLINE + LOCAL_COPYRIGHT, "");
		vCardZusatz = vCardZusatz.replaceAll(LOCAL_COPYRIGHT, "");
		String vCardStrasse = formatString(getVCardValue(vCardContents, "Strasse", isCompany, country));
		String vCardPlz = formatString(getVCardValue(vCardContents, "PLZ", isCompany, country));
		String vCardOrt = formatString(getVCardValue(vCardContents, "Ort", isCompany, country));
		String vCardTelefon1 = formatPhoneNumber(getVCardValue(vCardContents, "Telefon1", isCompany, country), country);
		String vCardFax = formatPhoneNumber(getVCardValue(vCardContents, "Fax", isCompany, country), country);
		String vCardEmail = formatString(getVCardValue(vCardContents, "Email", isCompany, country));
		String vCardIsDetail = "1";
		String vCardWebSite = formatString(getVCardValue(vCardContents, "Website", isCompany, country));
		String vCardTelefon2 = formatPhoneNumber(getVCardValue(vCardContents, "Telefon2", isCompany, country), country);
		String vCardMobile = formatPhoneNumber(getVCardValue(vCardContents, "Mobil", isCompany, country), country);
		String vCardBeruf = formatString(getVCardValue(vCardContents, "Beruf", isCompany, country));
		String vCardCategory = formatString(getVCardValue(vCardContents, "BEMERKUNG", isCompany, country));
		// part between () belongs to "role"/Beruf
		String DIEKATEGORIE = vCardZusatz;
		String ADDITIONALROLE = "";
		int parPos = vCardZusatz.indexOf("(");
		if (parPos >= 0)	{
			DIEKATEGORIE   = vCardZusatz.substring(0, parPos).trim();
			ADDITIONALROLE = vCardZusatz.substring(parPos + 1).trim();
			ADDITIONALROLE = ADDITIONALROLE.substring(0, ADDITIONALROLE.length() - 1);
		}
		// title may be contained in vCardBeruf or in vCardName -> extract to vCardTitle
		// search in vCardberuf
		String vCardTitle = "";
		for (int i = 1; i < ADR_TITLES.length; i++)	{
			String currTitle = ADR_TITLES[i];
			/*if (vCardBeruf.matches("^" + currTitle + "[.]*"))	{
				System.out.println("");
			}*/
			if (vCardBeruf.indexOf(currTitle) >= 0)	{
				vCardTitle = currTitle;
				// strip title off vCardBeruf
				vCardBeruf = vCardBeruf.replace(currTitle, "");
				break;
			}
		}
		
		// extract parts from html
		String group     = "";
		String ledigname = "";
		String poBox     = "";
		try {
			// calc the url of the detail entry
			String htmlUrl = vCardURL.replace("/vcard/", "/de/d/");
			String htmlContents = readContent(htmlUrl, "UTF-8");
			HtmlParser subParser = new HtmlParser(htmlContents);
			if (subParser.moveTo(ADR_DETAIL_TAG)) { // kein Eintrag
				// move to name/firstname
				String nameVornameText = subParser.extract("<h2 class=\"fn\">", "</h2>");
				// no empty contents
				if (nameVornameText != null && nameVornameText.length() > 0) {
					// extract ledigname from nameVornameText
					String[] vornameLedigname = nameVornameText.split(ADR_LEDIGNAMENSTRENNER);
					ledigname = "";
					if (vornameLedigname.length > 1)	{
						ledigname = cleanupUmlaute(vornameLedigname[1].split(ADR_LEDIGNAMENSSCHLUSS)[0].trim());
					}
					// address -> pobox if present
					String adressTxt = subParser.extract("<div class=\"streetAddress\">", "</div>");
					HtmlParser parser = new HtmlParser(adressTxt);
					poBox = removeDirt(parser.extract("<span class=\"post-office-box\">", "</span>"));
					poBox = poBox.replaceAll("^\\<br /\\>", "");
					poBox = poBox.replaceAll("\\<br /\\>$", "");
					poBox = poBox.replaceAll("\\<br /\\>", ", ");
					poBox = cleanupUmlaute(poBox);
				}
			}		
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// beruf calculated
		if (!vCardBeruf.isEmpty())	{
			if (!ADDITIONALROLE.isEmpty()) ADDITIONALROLE = " " + ADDITIONALROLE;
			vCardBeruf = formatString(vCardBeruf) + ADDITIONALROLE;
		} else	{
			vCardBeruf = ADDITIONALROLE;
		}
		
		// return result
		boolean isDetail = true;
		boolean isVCardDetail = true;
		String detailLink = "";
		return new KontaktEntry (
				vCardVorname,
				vCardName,
				poBox,
				vCardStrasse,
				vCardPlz,
				vCardOrt,
				vCardTelefon1,
				vCardFax,
				vCardEmail,
				isDetail,
				isVCardDetail,
				detailLink,
				vCardWebSite,
				vCardTelefon2,
				vCardMobile,
				ledigname,
				vCardBeruf,
				DIEKATEGORIE,
				isCompany,
				vCardTitle,
				"CHE"
				);
	}
	private static String getVCardValue(final String vCardContents, final String key, final boolean isCompany, final String lang)	{
		if (lang.equalsIgnoreCase("ch"))	{
			String[] extractionSpecs = {
					"NAME:N:0",
					"VORNAME:N:1",
					"STRASSE:Item1.ADR;TYPE=HOME:2",   //home/work
					"PLZ:Item1.ADR;TYPE=HOME:5",       //home/work
					"ORT:Item1.ADR;TYPE=HOME:3",       //home/work
					"Telefon1:TEL;TYPE=VOICE,HOME:0",  //home/work
					"Telefon2:TEL;TYPE=VOICE,WORK:0",  //home/work
					"FAX:TEL;TYPE=FAX,HOME:0",         //home/work
					"MOBIL:TEL;TYPE=CELL,HOME:0",      //home/work
					"EMAIL:EMAIL;TYPE=INTERNET:0",
					"WEBSITE:URL;TYPE=HOME:0",         //home/work
					"BERUF:TITLE:0",
					"NOTE:NOTE:0"
				};
			return getVCardValue(vCardContents, key, isCompany, extractionSpecs);
		}
		if (lang.equalsIgnoreCase("de"))	{
			String[] extractionSpecs = {
				"NAME:N:0",
				"VORNAME:N:1",
				"STRASSE:ADR:2",   //home/work
				"PLZ:ADR:5",       //home/work
				"ORT:ADR:3",       //home/work
				"Telefon1:TEL;HOME:0",  //home/work
				"Telefon2:TEL;WORK:0",  //home/work
				"FAX:TEL;HOME;FAX:0",         //home/work
				
				"MOBIL:TEL;HOME;CELL:0",    //home/work
				
				"EMAIL:EMAIL;INTERNET:0",
				"WEBSITE:URL;HOME:0",         //home/work
				"BERUF:TITLE:0",
				"NOTE:NOTE:0"
			};
			return getVCardValue(vCardContents, key, isCompany, extractionSpecs);
		}
		return "";
	}
	/**
	 * read the value for a given key from a vCard
	 * 
	 * @param vCardContents: the full contents of a vCard
	 * @param key: the key for which to search for
	 * @param isCompany: is this a company-entry - returns different values
	 * @return the found value or "" if not found
	 */
	private static String getVCardValue(final String vCardContents, final String key, final boolean isCompany, final String[] vCardExtractionSpecs)	{
		/* not present in vCard but on html-page:
		 *    - pobox
		 *    - ledigname
		 *    - group
		 */
		
		// find extraction spec for key
		String lKey = key.toLowerCase();
		String extractionSpec         = "";
		String extractionSpecSelector = "";
		for (int i = 0; i < vCardExtractionSpecs.length; i++){
			String currSpecKey = vCardExtractionSpecs[i].toLowerCase();
			currSpecKey = currSpecKey.substring(0, currSpecKey.indexOf(":"));
			if (currSpecKey.equalsIgnoreCase(lKey))	{
				extractionSpec = vCardExtractionSpecs[i];
				extractionSpec = extractionSpec.substring(extractionSpec.indexOf(":") + 1);
				extractionSpecSelector = extractionSpec.split(":")[1];
				extractionSpec = extractionSpec.split(":")[0];
				// switch HOME/WORK in specs if isCompany = true
				if (isCompany){
					if (extractionSpec.indexOf("HOME") >= 0){
						extractionSpec = extractionSpec.replace("HOME", "WORK");
					} else	{
						extractionSpec = extractionSpec.replace("WORK", "HOME");
					}
				}
				break;
			}
		}
		if (extractionSpec == "") return "";
		
		// stripping ENCODING, CHARSET and PREF from vCard
		String stripped = vCardContents.replaceAll(";CHARSET=[a-zA-Z1-9-_]+", "");
		stripped = stripped.replaceAll(";ENCODING=[a-zA-Z1-9-_]+", "");
		stripped = stripped.replaceAll(",PREF", "");
		
		String MARKER_VCARD_NEWLINE = getDocReturnCharacter(vCardContents);
		// loop through lines of vcard, try to match the extractionSpec
		int lineStartIx     = 0;
		int nextLinestartIx = 0;
		while ((nextLinestartIx = stripped.indexOf(MARKER_VCARD_NEWLINE, lineStartIx))>=0)	{
			String vCardLine = stripped.substring(lineStartIx, nextLinestartIx);
			if ((vCardLine.length() >= extractionSpec.length()) && (vCardLine.substring(0, extractionSpec.length()).equalsIgnoreCase(extractionSpec)))	{
				// data is on the right side of first colon
				String data = "";
				int colonPos = vCardLine.indexOf(":");
				if (colonPos >= 0)	{
					data = vCardLine.substring(colonPos + 1);
				}
				//String data = vCardLine.split(":")[1];
				String[] dataParts = data.split(";");
				if (dataParts.length > Long.parseLong(extractionSpecSelector))	{
					data = data.split(";")[(int) Long.parseLong(extractionSpecSelector)];
				} else {
					data = "";
				}
				return data;
			}
			lineStartIx = nextLinestartIx + MARKER_VCARD_NEWLINE.length();
		}
		
		
		return "";
	}
	
	private static String cleanupUmlaute(String text) {
		// this version is prepared for any characters
		String tmp = text;
		tmp = tmp.replaceAll("&#x([0-9A-Fa-f]{2,2});", "%$1");
		try {
			tmp = URLDecoder.decode(tmp, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
		}
		return tmp;
	}

		/**
	 * reformat string correctly which came from vCard from tel.local.ch
	 * @param sourceString
	 * @return the correctly formatted string
	 */
	public static String formatString(final String sourceString)	{
		// unescape, first replace "=" by "%"...
		String data = sourceString.replaceAll("=", "%");
		try {
			data = URLDecoder.decode(data, "ISO-8859-1");
			return data;
		} catch (UnsupportedEncodingException e) {
			return sourceString;
		}
	}
	
	/**
	 * Format a phone number as swiss phone number, 0xx xxx xx xx
	 * @param phoneNumber the phoneNumber as returned from vCard from tel.local.ch, 
	 *                    eg. +41523439772
	 * @return the reformatted phone number. if the input is not formatted correctly, then the
	 * function returns an empty string
	 */
	public static String formatPhoneNumber(final String phoneNumber, final String country)	{
		String result = phoneNumber;
		// do some testing
		if (country.toLowerCase().equalsIgnoreCase("ch"))	{
			if (phoneNumber.length() == 0) return "";
			String tmp = phoneNumber.replaceAll("\\+[0-9]{11}", "");
			if (tmp.length() != 0) return "";
			
			// now format the number
			result = "0" + 
							phoneNumber.substring( 3,  5) + " " +
							phoneNumber.substring( 5,  8) + " " +
							phoneNumber.substring( 8, 10) + " " +
							phoneNumber.substring(10, 12);
		}
		return result;
	}
	
	/**
	 * Format a phone number according to your needs. NOT YET IMPLEMENTED
	 * @param phoneNumber the phoneNumber as returned from vCard from tel.local.ch, 
	 *                    eg. +41523439772
	 * @param inFormat the format of the input
	 * @param outFormat how to format the output
	 * @return the reformatted phone number. if the input is not formatted correctly, then the
	 * function returns an empty string
	 */
	public static String formatPhoneNumber(final String phoneNumber, final String country, final String inFormat, final String outFormat)	{
		
		return "";
	}
	
	/**
	 * Converts a StringArray to a string, delimited by {delimiter}
	 * @param strArray
	 * @param delimiter
	 * @return
	 */
	protected static String stringArrayToString(String[] strArray, String delimiter)	{
	    StringBuffer result = new StringBuffer();
	    if (strArray.length > 0) {
	        result.append(strArray[0]);
	        for (int i=1; i<strArray.length; i++) {
	            result.append(delimiter);
	            result.append(strArray[i]);
	        }
	    }
	    return result.toString();
	}
	
	//+++++ END   Extract vCard Contents
	
	//+++++ START HELP READ PAGE
	private static String readContent(final String fullUrl)
		throws IOException, MalformedURLException{
		
		URL content = new URL(fullUrl);
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
		return sb.toString();
	}

	/**
	 *  read and return the contents of a html page
	 *  
	 * @param urlText = the url from where the page should be read
	 * @param charSet = the character set to be used for page-encoding
	 * 
	 * @return String, the contents of the page
	 */
	
	public static String readContent(final String urlText, final String charSet) throws IOException, MalformedURLException{
		URL url = new URL(urlText);
		InputStream input = url.openStream();
		
		StringBuffer sb = new StringBuffer();
		int count = 0;
		char[] c = new char[10000];
		InputStreamReader isr = new InputStreamReader(input, charSet);
		try {
			int lcount = isr.read(c);
			while ((count = isr.read(c)) > 0) {
				sb.append(c, 0, count);
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return sb.toString();
	}
//+++++ END   HELP READ PAGE

	
	
	/**
	 * Extrahiert einen Kontakt aus einem Detaileintrag Bsp: <div class="resrowclr_yellow"> </br>
	 * <img class="imgbox" src="http://s.staticlocal.ch/images/pois/na/blue.png" alt="poi"/> <p
	 * class="role">Garage</p> <h2 class="fn">Auto Meier AG</h2> <p class="role">Opel-Vertretung</p>
	 * <div class="addressBlockMain"> <div class="streetAddress"> <span
	 * class="street-address">Hauptstrasse 253</span> </br> <span class="post-office-box">Postfach<br>
	 * </span> <span class="postal-code">5314</span> <span class="locality">Kleindöttingen</span>
	 * </div> </br>
	 * <table>
	 * <tbody>
	 * <tr class="phoneNumber">
	 * <td>
	 * <span class="contact">Telefon:</span></td>
	 * <td class="tel">
	 * <a class="phonenr" href="callto://+41562451818"> 056 245 18 18 </a></td>
	 * <td id="freecall"></td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * </div> <br class="bighr"/> <div id="moreAddresses"> <h3>Zusatzeintrag</h3> <div
	 * class="additionalAddress" id="additionalAddress1"> <span class="role">Verkauf</span> </br>
	 * <table>
	 * <tbody>
	 * <tr class="phoneNumber">
	 * <td>
	 * <span class="contact">Telefon:</span></td>
	 * <td class="tel">
	 * <a class="phonenr" href="callto://+41448104211"> 044 810 42 11 </a></td>
	 * <td id="freecall"></td>
	 * </tr>
	 * <tr>
	 * <td>
	 * <span class="contact">Fax:</span></td>
	 * <td>
	 * &nbsp;044 810 54 40</td>
	 * </tr>
	 * <tr>
	 * <td>&nbsp;</td>
	 * <td>&nbsp;</td>
	 * <td></td>
	 * </tr>
	 * <tr class="">
	 * <td>
	 * <span class="contact">E-Mail:</span></td>
	 * <td>
	 * &nbsp;<a href="mailto:info@kvd.ch"> info@kvd.ch </a></td>
	 * <td></td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * </div> </div> </div>
	 */
	private KontaktEntry extractKontakt(){
		//++++ modification: read info from vCard - this is much safer...
		// TODO vCard
		
		if (!moveTo(ADR_DETAIL_TAG)) { // Kein neuer Eintrag
			return null;
		}
		
		// Name, Vorname
		String nameVornameText = extract("<h2 class=\"fn\">", "</h2>");
		if (nameVornameText == null || nameVornameText.length() == 0) { // Keine leeren Inhalte
			return null;
		}
		String[] vornameNachname = getVornameNachname(nameVornameText);
		String vorname = vornameNachname[0];
		String nachname = vornameNachname[1];
		
		//+++++ START extract vorname/ledigname from vorname
		String[] vornameLedigname = vorname.split(ADR_LEDIGNAMENSTRENNER);
		vorname = vornameLedigname[0].trim();
		String ledigname = "";
		if (vornameLedigname.length > 1)	{
			ledigname = vornameLedigname[1].split(ADR_LEDIGNAMENSSCHLUSS)[0].trim();
		}
		//+++++ END   extract vorname/ledigname from vorname
		
		// Zusatz
		String zusatz = "";
		if (moveTo("<p class=\"role\">")) {
			zusatz = extractTo("</p>");
		}
		
		// Adresse
		String adressTxt = extract("<div class=\"streetAddress\">", "</div>");
		HtmlParser parser = new HtmlParser(adressTxt);
		String streetAddress =
			removeDirt(parser.extract("<span class=\"street-address\">", "</span>"));
		String poBox = removeDirt(parser.extract("<span class=\"post-office-box\">", "</span>"));
		String plzCode = removeDirt(parser.extract("<span class=\"postal-code\">", "</span>"));
		
		// Ort
		// String ort = removeDirt(new HtmlParser(adressTxt).extract("<span class=\"locality\">",
		// "</span>"));
		parser.moveTo("<tr class=\"locality\">");
		parser.moveTo("<a href=");
		String ort = removeDirt(parser.extract(">", "</a>").replace("&nbsp;", "").trim());
		
		if (zusatz == null || zusatz.length() == 0) {
			zusatz = poBox;
		}
		
		// Tel/Fax & Email
		moveTo("<tr class=\"phoneNumber\">");
		String tel = "";
		if (moveTo("<span class=\"contact\">Telefon")) {
			moveTo("<td class=\"tel\"");
			moveTo("<a class=\"phonenr\"");
			tel = extract(">", "</a>").replace("&nbsp;", "").replace("*", "").trim();
		}
		String fax = "";
		if (moveTo("<span class=\"contact\">Fax")) {
			fax = extract("<td>", "</td>").replace("&nbsp;", "").replace("*", "").trim();
		}
		String email = "";
		if (moveTo("<span class=\"contact\">E-Mail")) {
			moveTo("<span class=\"obfuscml\"");
			email = extract("\">", "</span>");
			// Email Adresse wird verkehrt gesendet
			email = reverseString(email);
		}
		
		//+++++ new
		// read DetailPage-Link
		String vCard = extract("<li class=\"exportvcard\"><a href=\"", "\">");
		// prepend path 
		vCard = "http://tel.local.ch" + vCard;
		// and strip "?what=<searchTerm>" from the end to get the vcard-url
		vCard = vCard.split("\\?what=")[0];
		
//		return new KontaktEntry(vorname, nachname, zusatz, streetAddress, plzCode, ort, tel, fax,
//			email, true);
		String website         = "";
		String tel2            = "";
		String mobile          = "";
		String profession      = "";
		String category        = "";
		boolean isOrganisation = false;
		String title           = "";
		return new KontaktEntry(vorname, nachname, zusatz, //$NON-NLS-1$
				streetAddress, plzCode, ort, tel, fax, email, true, //$NON-NLS-1$
				//+++++ new:
				false, vCard, website, tel2, mobile,
				ledigname, profession, category, isOrganisation, title, "CHE");
	}
}
