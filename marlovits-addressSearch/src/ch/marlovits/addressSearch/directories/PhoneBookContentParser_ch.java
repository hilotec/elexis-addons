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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class PhoneBookContentParser_ch extends PhoneBookContentParser {
	protected static String country = "ch";
	
	// markers for identifying html parts
	private static final String ADR_LIST_TAG = "class=\"vcard searchResult resrowclr"; //$NON-NLS-1$
	private static final String ADR_DETAIL_TAG = "<div class=\"resrowclr";; //$NON-NLS-1$
	
	//
	private static final String   ADR_MAIDENNAMESEPARATOR = "\\(-";
	private static final String   ADR_MAIDENNAMEEND       = "\\)";
	
	private static String LOCAL_COPYRIGHT      = "\\[Copyright \\(c\\) local\\.ch ag\\]";
	
	private static final int htmlReadTimeout = 7000;
	
	private static String[] extractionSpecs = {
			PhoneBookEntry.FLD_NAME       + ":N:0",
			PhoneBookEntry.FLD_FIRSTNAME  + ":N:1",
			PhoneBookEntry.FLD_STREET     + ":Item1.ADR;TYPE=HOME:2",  //home/work
			PhoneBookEntry.FLD_ZIP        + ":Item1.ADR;TYPE=HOME:5",  //home/work
			PhoneBookEntry.FLD_PLACE      + ":Item1.ADR;TYPE=HOME:3",  //home/work
			PhoneBookEntry.FLD_PHONE1     + ":TEL;TYPE=VOICE,HOME:0",  //home/work
			PhoneBookEntry.FLD_PHONE2     + ":TEL;TYPE=VOICE,WORK:0",  //home/work
			PhoneBookEntry.FLD_FAX        + ":TEL;TYPE=FAX,HOME:0",    // home/work
			PhoneBookEntry.FLD_MOBILE     + ":TEL;TYPE=CELL,HOME:0",   //home/work
			PhoneBookEntry.FLD_EMAIL      + ":EMAIL;TYPE=INTERNET:0",
			PhoneBookEntry.FLD_WEBSITE    + ":URL;TYPE=HOME:0",        //home/work
			PhoneBookEntry.FLD_PROFESSION + ":TITLE:0",
			PhoneBookEntry.FLD_NOTE       + ":NOTE:0",
			PhoneBookEntry.FLD_ISORG      + ":X-ABShowAs:0"
		};
	
	/**
	 * this is the constructor: save html, name, geo and country in members
	 * @param name
	 * @param geo
	 * @param pageNum
	 */
	public PhoneBookContentParser_ch(final String name, final String geo, final int pageNum)	{
		super(name, geo, pageNum, "UTF-8");
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
	public HashMap<String, String> extractKontaktFromList()	{
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		initHashMap(result);
		
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
		String street = removeDirt(new HtmlParser(adressTxt).extract("<span class=\"street-address\">",	", <span class="));
		String zip = removeDirt(new HtmlParser(adressTxt).extract("<span class=\"postal-code\">", "</span>"));
		HtmlParser cityParser = new HtmlParser(adressTxt);
		cityParser.moveTo("<span class=\"locality\">");
		String city = removeDirt(cityParser.getTail());
		
		// read DetailPage-Link
		String vCard = extract("<a class=\"detaillink\" href=\"", "\">");
		// replace "/de/d/", "/fr/d/", "/it/d/", "/en/d/" by "/vard/" to get the vcard-url
		vCard = vCard.replaceAll("tel\\.local\\.ch/[a-zA-Z][a-zA-Z]/d/", "tel\\.local\\.ch/vcard/");
		// and strip "?what=<searchTerm>" from the end to get the vcard-url
		vCard = vCard.split("\\?what=")[0];
		
		// populate the hashMap
		result.put(PhoneBookEntry.FLD_DISPLAYNAME, lastNameFirstNameText);
		result.put(PhoneBookEntry.FLD_FIRSTNAME,   firstname);
		result.put(PhoneBookEntry.FLD_NAME,        lastname);
		result.put(PhoneBookEntry.FLD_ZUSATZ,      zusatz);
		result.put(PhoneBookEntry.FLD_PHONE1,      phone);
		result.put(PhoneBookEntry.FLD_STREET,      street);
		result.put(PhoneBookEntry.FLD_ZIP,         zip);
		result.put(PhoneBookEntry.FLD_PLACE,       city);
		result.put(PhoneBookEntry.FLD_VCARDLINK,   vCard);
		result.put(PhoneBookEntry.FLD_LAND,        "CH");
		
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
	public HashMap<String, String> extractKontaktFromDetail(){
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		initHashMap(result);
		
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
		result.put(PhoneBookEntry.FLD_DISPLAYNAME, lastnameFirstnameText);
		result.put(PhoneBookEntry.FLD_FIRSTNAME,   firstname);
		result.put(PhoneBookEntry.FLD_NAME,        lastname);
		result.put(PhoneBookEntry.FLD_ZUSATZ,      zusatz);
		result.put(PhoneBookEntry.FLD_PHONE1,      phone);
		result.put(PhoneBookEntry.FLD_STREET,      street);
		result.put(PhoneBookEntry.FLD_ZIP,         zip);
		result.put(PhoneBookEntry.FLD_PLACE,       city);
		result.put(PhoneBookEntry.FLD_VCARDLINK,   vCard);
		result.put(PhoneBookEntry.FLD_MAIDENNAME,  maidenname);
		result.put(PhoneBookEntry.FLD_POBOX,       poBox);
		result.put(PhoneBookEntry.FLD_FAX,         fax);
		result.put(PhoneBookEntry.FLD_EMAIL,       email);
		result.put(PhoneBookEntry.FLD_LAND,        "CH");
		
		// return hashMap
		return result;
	}
	
	/**
	 * extracts a Kontakt with ALL available info from a vCard and /or html combined
	 * @param kontaktHashMap Kontakt for which to extract the info
	 * @return the Kontakt in a HashMap, the possible keys of the HashMap are described above
	 */
	public HashMap<String, String> extractMaxInfo(HashMap<String, String> kontaktHashMap)	{
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		initHashMap(result);
		
		// read vCardLink from kontaktHashMap
		String vCardLink = kontaktHashMap.get(PhoneBookEntry.FLD_VCARDLINK);
		if (vCardLink.equalsIgnoreCase("")){
			return result;
		}
		
		// ****** read contents of vCard
		String vCardContents = readContent(vCardLink, "UTF-8", htmlReadTimeout);
		// strip ENCODING, CHARSET and PREF from vCard
		vCardContents = vCardContents.replaceAll(";CHARSET=[a-zA-Z1-9-_]+", "");
		vCardContents = vCardContents.replaceAll(";ENCODING=[a-zA-Z1-9-_]+", "");
		vCardContents = vCardContents.replaceAll(",PREF", "");
		
		// initizalize vCardParser
		VCardParser vCardParser = new VCardParser(vCardContents);
		
		// test if this is a company entry
		boolean isCompany = (vCardParser.getVCardValue("X-ABShowAs", 0).equalsIgnoreCase("COMPANY"));
		
		// ****** read from vCard into hashMap
		for (int i = 0; i < extractionSpecs.length; i++)	{
			String extractionSpec = extractionSpecs[i];
			// switch "HOME" and "WORK" for company entries
			if (isCompany){
				if (extractionSpec.indexOf("HOME") >= 0){
					extractionSpec = extractionSpec.replace("HOME", "WORK");
				} else	{
					extractionSpec = extractionSpec.replace("WORK", "HOME");
				}
			}
			// split extraction spec on ":"
			String[] specParts = extractionSpec.split(":");
			String kontaktKey = specParts[0];	// write  to  this key in PhoneBookEntry
			String vCardKey   = specParts[1];	// search for this key in vCard-Info
			String infoIndex  = specParts[2];	// index in info of vCard (comma-separated)
			String vCardValue = formatString(vCardParser.getVCardValue(vCardKey, Integer.parseInt(infoIndex)));
			result.put(kontaktKey, vCardValue);
		}
		
		// ****** postprocess values read from vCard
		// extract distinct firstnames from field firstname
		String firstName = result.get(PhoneBookEntry.FLD_FIRSTNAME);
		firstName = extractFirstnames(formatString(firstName), "|");
		result.put(PhoneBookEntry.FLD_FIRSTNAME, firstName);
		
		// ****** set company boolean
		if (isCompany)	{
			result.put(PhoneBookEntry.FLD_ISORG, "1");
		} else	{
			result.put(PhoneBookEntry.FLD_ISORG, "0");
		}
		
		// format phone numbers
		result.put(PhoneBookEntry.FLD_PHONE1, formatPhoneNumber(result.get(PhoneBookEntry.FLD_PHONE1)));
		result.put(PhoneBookEntry.FLD_FAX,    formatPhoneNumber(result.get(PhoneBookEntry.FLD_FAX)));
		result.put(PhoneBookEntry.FLD_PHONE2, formatPhoneNumber(result.get(PhoneBookEntry.FLD_PHONE2)));
		result.put(PhoneBookEntry.FLD_MOBILE, formatPhoneNumber(result.get(PhoneBookEntry.FLD_MOBILE)));
		
		// title may be contained in vCardProfession -> extract to vCardTitle
		String vCardProfession = result.get(PhoneBookEntry.FLD_PROFESSION);
		String vCardTitle = "";
		for (int i = 1; i < ADR_TITLES.length; i++)	{
			String currTitle = ADR_TITLES[i];
			if (vCardProfession.indexOf(currTitle) >= 0)	{
				vCardTitle = currTitle;
				// strip title off vCardProfession
				vCardProfession = vCardProfession.replace(currTitle, "");
				break;
			}
		}
		result.put(PhoneBookEntry.FLD_TITLE, vCardTitle);
		
		// strip copyright notice [Copyright (c) local.ch ag] from vCard-field NOTE
		String vCardZusatz = result.get(PhoneBookEntry.FLD_NOTE);
		String MARKER_VCARD_NEWLINE = VCardParser.getDocReturnCharacter(vCardContents);
		vCardZusatz = vCardZusatz.replaceAll(MARKER_VCARD_NEWLINE + LOCAL_COPYRIGHT, "");
		vCardZusatz = vCardZusatz.replaceAll(LOCAL_COPYRIGHT, "");
		// part between () belongs to "role"/profession
		String category = vCardZusatz;
		String additionalRole = "";
		int parPos = vCardZusatz.indexOf("(");
		if (parPos >= 0)	{
			category   = vCardZusatz.substring(0, parPos).trim();
			additionalRole = vCardZusatz.substring(parPos + 1).trim();
			additionalRole = additionalRole.substring(0, additionalRole.length() - 1);
		}
		result.put(PhoneBookEntry.FLD_ZUSATZ, vCardZusatz);
		result.put(PhoneBookEntry.FLD_CATEGORY, category);
		
		// profession calculated: add part from vCardZusatz if found
		if (!vCardProfession.isEmpty())	{
			if (!additionalRole.isEmpty()) additionalRole = " " + additionalRole;
			vCardProfession = vCardProfession + additionalRole;
		} else	{
			vCardProfession = additionalRole;
		}
		result.put(PhoneBookEntry.FLD_PROFESSION, vCardProfession);
		
		// ****** extract missing parts from html
		String maidenname = "";
		String poBox      = "";
		// calc the url of the detail entry
		String htmlUrl = vCardLink.replace("/vcard/", "/de/d/");
		String htmlContents = readContent(htmlUrl, "UTF-8", htmlReadTimeout);
		HtmlParser subParser = new HtmlParser(htmlContents);
		if (subParser.moveTo(ADR_DETAIL_TAG)) {
			// move to lsatname/firstname
			String lastnameFirstnameText = subParser.extract("<h2 class=\"fn\">", "</h2>");
			// no empty contents
			if (lastnameFirstnameText != null && lastnameFirstnameText.length() > 0) {
				// extract maidenname from lastnameFirstnameText
				String[] firstnameMaidenname = lastnameFirstnameText.split(ADR_MAIDENNAMESEPARATOR);
				maidenname = "";
				if (firstnameMaidenname.length > 1)	{
					maidenname = firstnameMaidenname[1].split(ADR_MAIDENNAMEEND)[0].trim();
				}
				// address -> pobox if present
				String adressTxt = subParser.extract("<div class=\"streetAddress\">", "</div>");
				HtmlParser parser = new HtmlParser(adressTxt);
				poBox = removeDirt(parser.extract("<span class=\"post-office-box\">", "</span>"));
				poBox = poBox.replaceAll("^\\<br /\\>", "");
				poBox = poBox.replaceAll("\\<br /\\>$", "");
				poBox = poBox.replaceAll("\\<br /\\>", ", ");
			}
		}		
		
		result.put(PhoneBookEntry.FLD_MAIDENNAME, maidenname);
		result.put(PhoneBookEntry.FLD_POBOX,      poBox);
		result.put(PhoneBookEntry.FLD_LAND,       "CH");
		
		// return result
		return result;
	}

	
	/**
	 * creates and returns a url for reading data from an online-address-query page
	 * @param  name    search for this name
	 * @param  geo     search in this city/location
	 * @param  country search in this country - must be iso2 name of the country
	 * @param  pageNum
	 * @return the url which returns the results, null if any error occurs
	 */
	public URL getURL(String name, String geo, int pageNum)	{
		name = name.replace(' ', '+');
		geo = geo.replace(' ', '+');
		country = country.toLowerCase();
		
		int lPageNum = pageNum;
		int recCount = 10;
		String urlPattern = "";
		// *** create the url string for different countries
		if (country.equalsIgnoreCase("ch"))	{
			// *** switzerland
			urlPattern = "http://tel.local.ch/{0}/q/?what={1}&where={2}&cid=directories&start={3}"; //$NON-NLS-1$
		} else if (country.equalsIgnoreCase("de"))	{
			// *** germany
			try {
				name = URLEncoder.encode(name, "ISO-8859-1");
				geo  = URLEncoder.encode(geo,  "ISO-8859-1");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			recCount = 20;
			lPageNum = lPageNum * 20 + 1;
			System.out.println("lPageNum: " + lPageNum);
			urlPattern = "http://www.dastelefonbuch.de/?la={0}&kw={1}&ci={2}&ciid=&cmd=search&cifav=0&mdest=sec1.www1&vert_ok=1&recfrom={3}&reccount={4}"; //$NON-NLS-1$
		}
		
		// *** actually create the URL
		try {
			return new URL(MessageFormat.format(urlPattern, new Object[] {
				Locale.getDefault().getLanguage(), name, geo, lPageNum, recCount
			}));
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	/**
	 * Format a phone number as swiss phone number, 0xx xxx xx xx
	 * @param phoneNumber the phoneNumber as returned from vCard from tel.local.ch, 
	 *                    eg. "+41523439772", the "+" is already replaced by " " -> " 41523439772"
	 * @return the reformatted phone number. if the input is not formatted correctly, then the
	 * function returns an empty string
	 */
	public static String formatPhoneNumber(final String phoneNumber)	{
		String result = phoneNumber;
		
		// do some testing
		if (phoneNumber.length() == 0) return "";
		String tmp = phoneNumber.replaceAll(" [0-9]{11}", "");
		if (tmp.length() != 0) return "";
		
		// now format the number
		result = "0" + 
						phoneNumber.substring( 3,  5) + " " +
						phoneNumber.substring( 5,  8) + " " +
						phoneNumber.substring( 8, 10) + " " +
						phoneNumber.substring(10, 12);
		return result;
	}

	@Override
	public String[][] getCitiesList() {
		return null;
	}

	@Override
	public String getCitiesListMessage() {
		return "";
	}

	@Override
	public boolean hasCitiesList() {
		return false;
	}

	@Override
	public boolean noCityFound() {
		return false;
	}

/*
<li class="WLSep"><ul><li>Ortschaften, Gemeinden, Quartiere</li></ul></li>
<li class="WLRow"><a href="javascript:void(0)" onClick="javascript:whereLiveSearch.update('Bülach')">Bülach</a></li>
<li class="WLRow"><a href="javascript:void(0)" onClick="javascript:whereLiveSearch.update('Eschenmosen (Bülach)')">Eschenmosen (Bülach)</a></li>
<li class="WLRow"><a href="javascript:void(0)" onClick="javascript:whereLiveSearch.update('Nussbaumen (Bülach)')">Nussbaumen (Bülach)</a></li>
<li class="WLSep"><ul><li>Kantone, Bezirke, Regionen</li></ul></li>
<li class="WLRow"><a href="javascript:void(0)" onClick="javascript:whereLiveSearch.update('Bülach (Bezirk/Amt)')">Bülach (Bezirk/Amt)</a></li>
*/
	public String[][] getCitySuggestions(String part) {
		String lPart;
		try {
			lPart = URLEncoder.encode(part, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			String results[][] = {{""}, {""}};
			return results;
		}
		String urlText = "http://tel.local.ch/{0}/geoservice/searchArea/" + lPart;
		urlText = MessageFormat.format(urlText, new Object[] { Locale.getDefault().getLanguage() });
		String result = readContent(urlText, "UTF-8", htmlReadTimeout);
		// mark as separator row for my cities list
		result = result.replaceAll("<li class=\"WLSep\">", "0; ");
		// mark as data row for my cities list
		result = result.replaceAll("<li class=\"WLRow\">", "1; ");
		// mark row omitted-line (comments on last line, not selectable)
		result = result.replaceAll("<li class=\"WLRowOmitted\">", "0; ");
		// mark end-of-row
		result = result.replaceAll("</li></ul></li>", ";");
		result = result.replaceAll("</li>", ";");
		// strip all html tags
		result = result.replaceAll("<[^>]+>", ""); //$NON-NLS-1$
	
		String[] splitted = result.split(";");
		// if num of entries is uneven, then the last line contains a messgae
		String results[][] = new String[splitted.length/2][2];
		for (int i = 0; i < splitted.length; i ++)	{
			results[i/2][(i%2==0) ? 1 : 0] = splitted[i].trim();
		}
		return results;
	}
}
