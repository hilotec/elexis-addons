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

package ch.marlovits.addressSearch.directories;

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

import ch.elexis.data.Patient;

import java.util.HashMap;

public class PhoneBookContentParser_ch extends PhoneBookContentParser {
	// markers for identifying html parts
	private static final String ADR_LIST_TAG = "class=\"vcard searchResult resrowclr"; //$NON-NLS-1$
	private static final String ADR_DETAIL_TAG = "<div class=\"resrowclr";; //$NON-NLS-1$
	
	//
	private static final String   ADR_MAIDENNAMESEPARATOR = "\\(-";
	private static final String   ADR_MAIDENNAMEEND       = "\\)";
	
	private static final int htmlReadTimeout = 7000;

	/**
	 * this is the constructor: save html, name, geo and country in members
	 * @param htmlText
	 * @param name
	 * @param geo
	 * @param country
	 */
	public PhoneBookContentParser_ch(String htmlText, String name, String geo, String country){
		super(htmlText, name, geo, country);
	}
	
	/**
	 * extract infos for the current search from HTML-source                        <br>
	 *  - für die Suche in ch - tel.search.ch         zBsp "Treffer 1 - 10 von 11"  <br>
	 *  - für die Suche in de - telefonbuch.de        zBsp "Seite  1 (von 6)"       <br>
	 *  - für die Suche in at - herold.at/telefonbuch zBsp "Treffer 1-15 von 61"    <br>
	 * Abstract function, must override
	 * @return the search string to be displayed as info
	 */
	public String getSearchInfo(){
		reset();
		String searchInfoText = extract("<div class=\"summary\">", "<div id=\"printlink\"");  //$NON-NLS-1$
		if (searchInfoText == null) {
			return ""; //$NON-NLS-1$
		}
		// noPrint-Anteil aus String entfernen
		searchInfoText = searchInfoText.replaceAll("<span class=\"totalResults noPrint\">[^<]+</span>", "");  //$NON-NLS-1$
		// alle HTML <xxx> entfernen
		searchInfoText = searchInfoText.replaceAll("<[^>]+>", ""); //$NON-NLS-1$
		return searchInfoText;
	}
	
	/**
	 * extracts the total number of found entries  <br>
	 * Abstract function, must override
	 * @return number of found entries
	 */
	public int getNumOfEntries()	{
		reset();
		moveTo("<div class=\"printResultSummary printOnly\">"); //$NON-NLS-1$
		String resultStr = extract("</div><strong>", "</strong>"); //$NON-NLS-1$
		// if not found at all -> return 0
		if (resultStr.equalsIgnoreCase("")) return 0; //$NON-NLS-1$
		// if string contains other than numbers -> return 0
		if (!resultStr.replaceAll("[0-9]", "").equalsIgnoreCase("")) return 0; //$NON-NLS-1$,  $NON-NLS-2$
		// now it should be ok
		return Integer.parseInt(resultStr);
	}
	
	/**
	 * extracts Kontakte from HTML                 <br>
	 * extract [entriesPerPage] number of entries  <br>
	 * by parsing &lt;div class="xxx"&gt; we can decide if this is a listEntry or a detailEntry<br>
	 * 
	 * detailEntries: 								<li>
	 * 		"adrNameDetLev0"						<li>
	 * 		"adrNameDetLev1"						<li>
	 * 		"adrNameDetLev3"						<li>
	 * 		"adrNameDetLev2" must NOT be extracted	<br>
	 * 
	 * listEntries:									<li>
	 * 		"adrListLev0"							<li>
	 * 		"adrListLev1"							<li>
	 * 		"adrListLev3"							<li>
	 * 		"adrListLev0Cat" must NOT be extracted
	 * 
	 * @return the List of KontaktEntry's<br><br>
	 *
	 */
	public List<HashMap<String, String>> extractKontakte()	{
		reset();
		List<HashMap<String, String>> kontakte = new Vector<HashMap<String, String>>();
		
		int listIndex = getNextPos(ADR_LIST_TAG);
		int detailIndex = getNextPos(ADR_DETAIL_TAG);
		// looping as long as there is a next listEntry or a next detailEntry
		while (listIndex > 0 || detailIndex > 0) {
			HashMap<String, String> entry = null;
			if (detailIndex < 0 || (listIndex >= 0 && listIndex < detailIndex)) {
				// parsing from listEntries
				entry = extractKontaktFromList();
			} else if (listIndex < 0 || (detailIndex >= 0 && detailIndex < listIndex)) {
				// parsing from detailEntry
				entry = extractKontaktFromDetail();
			}
			if (entry != null) {
				kontakte.add(entry);
			}
			listIndex = getNextPos(ADR_LIST_TAG);
			detailIndex = getNextPos(ADR_DETAIL_TAG);
		}
		return kontakte;
	}
	
	/**
	 * extracts a Kontakt from a listEntry (<b>multiple</b> results displayed on a page)  <br>
	 * this just extracts the parts needed for the display in the results list            <br>
	 * if the actual detail info is needed, then the vCards are extracted                 <br>
	 * Abstract function, must override
	 * @return the Kontakt in a HashMap, the possible keys of the HashMap are described above<br><br>
	 *
	 * @example: <div id="te_ojUHu3vXsUWJbXidz2_sRQ"
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
	private HashMap<String, String> extractKontaktFromList()	{
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		
		// if there is no next entry just return
		if (!moveTo(ADR_LIST_TAG)) {
			return result;
		}
		
		// lastname, firstnname, zusatz
		moveTo("<div class=\"entrybox\">");  //$NON-NLS-1$
		
		int catIndex = getNextPos("<span class=\"category\"");  //$NON-NLS-1$
		int nextEntryPosIndex = getNextPos("<div class=\"entrybox\">");  //$NON-NLS-1$
		String zusatz = "";
		if (catIndex > 0 && catIndex < nextEntryPosIndex) {
			moveTo("<span class=\"category\"");  //$NON-NLS-1$
			zusatz = extract("\">", "</span>");  //$NON-NLS-1$
		}
		
		// lastname, firstnname
		moveTo("<a class=\"fn\"");  //$NON-NLS-1$
		
		String lastNameFirstNameText = extract("\">", "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lastNameFirstNameText = removeDirt(lastNameFirstNameText);
		
		// empty not allowed
		if (lastNameFirstNameText == null || lastNameFirstNameText.length() == 0) {
			return result;
		}
		String[] firstnameLastname = getFirstnameLastname(lastNameFirstNameText);
		String firstname = firstnameLastname[0];
		String lastname = firstnameLastname[1];
		
		// phone number - be careful - there really ARE entries without phone number!!!
		moveTo("<span class=\"tel\"");  //$NON-NLS-1$
		String phone = "";  //$NON-NLS-1$
		if (moveTo("<a class=\"phonenr\""))	{  //$NON-NLS-1$
			phone = extract(">", "</a>").replace("&nbsp;", "").replace("*", "").trim();  //$NON-NLS-1$, $NON-NLS-2$, $NON-NLS-3$, $NON-NLS-4$, $NON-NLS-5$, $NON-NLS-6$
		}
		
		// address, city, zip
		String adressTxt = extract("<p class=\"adr\">", "</p>");
		// 5.5.09 ts: verschachtelte spans -> alles bis zur nächsten span klasse holen
		String street =
			removeDirt(new HtmlParser(adressTxt).extract("<span class=\"street-address\">",	", <span class="));
		String zip =
			removeDirt(new HtmlParser(adressTxt).extract("<span class=\"postal-code\">", "</span>"));
		String city =
			removeDirt(new HtmlParser(adressTxt).extract("<span class=\"locality\">", "</span>"));
		
		// read DetailPage-Link
		String vCard = extract("<a class=\"detaillink\" href=\"", "\">");
		// replace "/de/d/", "/fr/d/", "/it/d/", "/en/d/" by "/vard/" to get the vcard-url
		vCard = vCard.replaceAll("tel\\.local\\.ch/[a-zA-Z][a-zA-Z]/d/", "tel\\.local\\.ch/vcard/");
		// and strip "?what=<searchTerm>" from the end to get the vcard-url
		vCard = vCard.split("\\?what=")[0];
		
		// populate the hashMap
		result.put(KontaktEntryHash.FLD_FIRSTNAME, firstname);
		result.put(KontaktEntryHash.FLD_LASTNAME,  lastname);
		result.put(KontaktEntryHash.FLD_ZUSATZ,    zusatz);
		result.put(KontaktEntryHash.FLD_PHONE,     phone);
		result.put(KontaktEntryHash.FLD_STREET,    street);
		result.put(KontaktEntryHash.FLD_ZIP,       zip);
		result.put(KontaktEntryHash.FLD_CITY,      city);
		result.put(KontaktEntryHash.FLD_VCARDLINK, vCard);
		
		// return hashMap
		return result;
	}
	
	/**
	 * extracts a Kontakt from a DetailEntry (<b>single</b> result displayed on a page)  <br>
	 * this just extracts the parts needed for the display in the results list           <br>
	 * if the actual detail info is needed, then the vCards are extracted                <br>
	 * this procedure can be the same as extracting from a list                          <br>
	 * - for ch this is different from extractListKontakt                                <br>
	 * - for de this is the same as extractListKontakt                                   <br>
	 * - for at this is the same as extractListKontakt
	 * Abstract function, must override
	 * @return the Kontakt in a HashMap, the possible keys of the HashMap are described above<br><br>
	 *
	 * @example: <div class="resrowclr_yellow"> </br>
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
	private HashMap<String, String> extractKontaktFromDetail(){
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		
		// if there is no next entry just return
		if (!moveTo(ADR_DETAIL_TAG)) {
			return result;
		}
		
		// lastname, firstname
		String lastnameFirstnameText = extract("<h2 class=\"fn\">", "</h2>");

		// empty not allowed
		if (lastnameFirstnameText == null || lastnameFirstnameText.length() == 0) {
			return result;
		}
		
		// extract firstname/lastname
		String[] firstnameLastname = getFirstnameLastname(lastnameFirstnameText);
		String firstname_ = firstnameLastname[0];
		String lastname = firstnameLastname[1];
		
		// extract firstname/maidenname from firstname_
		String[] firstnameMaidenname = firstname_.split(ADR_MAIDENNAMESEPARATOR);
		String firstname = firstnameMaidenname[0].trim();
		String maidenname = "";
		if (firstnameMaidenname.length > 1)	{
			maidenname = firstnameMaidenname[1].split(ADR_MAIDENNAMEEND)[0].trim();
		}
		
		// Zusatz
		String zusatz = "";
		if (moveTo("<p class=\"role\">")) {
			zusatz = extractTo("</p>");
		}
		
		// address
		String adressTxt = extract("<div class=\"streetAddress\">", "</div>");
		HtmlParser parser = new HtmlParser(adressTxt);
		String street = removeDirt(parser.extract("<span class=\"street-address\">", "</span>"));
		String poBox  = removeDirt(parser.extract("<span class=\"post-office-box\">", "</span>"));
		String zip    = removeDirt(parser.extract("<span class=\"postal-code\">", "</span>"));
		
		// city
		parser.moveTo("<tr class=\"locality\">");
		parser.moveTo("<a href=");
		String city = removeDirt(parser.extract(">", "</a>").replace("&nbsp;", "").trim());
		
		if (zusatz == null || zusatz.length() == 0) {
			zusatz = poBox;
		}
		
		// phone/fax & email
		moveTo("<tr class=\"phoneNumber\">");
		String phone = "";
		if (moveTo("<span class=\"contact\">Telefon")) {
			moveTo("<td class=\"tel\"");
			moveTo("<a class=\"phonenr\"");
			phone = extract(">", "</a>").replace("&nbsp;", "").replace("*", "").trim();
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
		
		// extract vCard-Link
		String vCard = extract("<li class=\"exportvcard\"><a href=\"", "\">");
		// prepend path
		vCard = "http://tel.local.ch" + vCard;
		// and strip "?what=<searchTerm>" from the end to get the vcard-url
		vCard = vCard.split("\\?what=")[0];
		
		
		// populate the hashMap
		result.put(KontaktEntryHash.FLD_FIRSTNAME,  firstname);
		result.put(KontaktEntryHash.FLD_LASTNAME,   lastname);
		result.put(KontaktEntryHash.FLD_ZUSATZ,     zusatz);
		result.put(KontaktEntryHash.FLD_PHONE,      phone);
		result.put(KontaktEntryHash.FLD_STREET,     street);
		result.put(KontaktEntryHash.FLD_ZIP,        zip);
		result.put(KontaktEntryHash.FLD_CITY,       city);
		result.put(KontaktEntryHash.FLD_VCARDLINK,  vCard);
		result.put(KontaktEntryHash.FLD_MAIDENNAME, maidenname);
		result.put(KontaktEntryHash.FLD_POBOX,      poBox);
		result.put(KontaktEntryHash.FLD_FAX,        fax);
		result.put(KontaktEntryHash.FLD_EMAIL,      email);
		
		// return hashMap
		return result;
	}
	
	/**
	 * extracts a Kontakt with ALL available info from a vCard and /or html combined
	 * @param kontaktHashMap Kontakt for which to extract the info
	 * @return the Kontakt in a HashMap, the possible keys of the HashMap are described above
	 */
	public HashMap<String, String> parseVCard(HashMap<String, String> kontaktHashMap)	{
		// TODO
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		
		// read vCardLink from kontaktHashMap
		String vCardLink = kontaktHashMap.get(KontaktEntryHash.FLD_VCARDLINK);
		if (vCardLink.equalsIgnoreCase("")){
			return result;
		}
		
		// read contents of vCard
		String vCardContents = readContent(vCardLink, htmlReadTimeout);
		// strip ENCODING, CHARSET and PREF from vCard
		vCardContents = vCardContents.replaceAll(";CHARSET=[a-zA-Z1-9-_]+", "");
		vCardContents = vCardContents.replaceAll(";ENCODING=[a-zA-Z1-9-_]+", "");
		vCardContents = vCardContents.replaceAll(",PREF", "");
		
		// initizalize vCardParser
		VCardParser vCardParser = new VCardParser(vCardContents);
		
		// test if this is a company entry
		boolean isCompany = (vCardParser.getVCardValue("X-ABShowAs").equalsIgnoreCase("COMPANY"));
		// switch "HOME" and "WORK" for company entries
		if (isCompany){
			if (extractionSpec.indexOf("HOME") >= 0){
				extractionSpec = extractionSpec.replace("HOME", "WORK");
			} else	{
				extractionSpec = extractionSpec.replace("WORK", "HOME");
			}
		}
		
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
	
}
