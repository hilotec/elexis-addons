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

import ch.elexis.util.SWTHelper;

public class PhoneBookContentParser_de extends PhoneBookContentParser {
	protected static String country = "de";
	private static final int htmlReadTimeout = 7000;
	
	final char NON_BREAKING_SPACE = 0x00A0; 
	
	String[] extractionSpecs = {
			PhoneBookEntry.FLD_NAME       + ":N:0",
			PhoneBookEntry.FLD_FIRSTNAME  + ":FN:0",
			PhoneBookEntry.FLD_STREET     + ":ADR:2",            //home/work
			PhoneBookEntry.FLD_ZIP        + ":ADR:5",            //home/work
			PhoneBookEntry.FLD_PLACE      + ":ADR:3",            //home/work
			PhoneBookEntry.FLD_PHONE1     + ":TEL;HOME:0",       //home/work
			PhoneBookEntry.FLD_PHONE2     + ":TEL;WORK:0",       //home/work
			PhoneBookEntry.FLD_FAX        + ":TEL;HOME;FAX:0",   //home/work
			PhoneBookEntry.FLD_MOBILE     + ":TEL;HOME;CELL:0",  //home/work
			PhoneBookEntry.FLD_EMAIL      + ":EMAIL;INTERNET:0",
			PhoneBookEntry.FLD_WEBSITE    + ":URL;HOME:0",       //home/work
			PhoneBookEntry.FLD_PROFESSION + ":TITLE:0",
			PhoneBookEntry.FLD_NOTE       + ":NOTE:0"
		};
	
	public PhoneBookContentParser_de(String name, String geo, int pageNum) {
		super(name, geo, pageNum);
	}
	
	@Override
	public HashMap<String, String> extractKontaktFromDetail() {
		// in telefonbuch.de there is no distinct detail page when searching
		// -> just call the list-version
		return extractKontaktFromList();
	}
	
	@Override
	public HashMap<String, String> extractKontaktFromList() {
		// im deutschen Telefonbuch gibt es auch eine vCard - da steht aber nicht mehr drinnen
		// und Name/firstname ist auch nicht getrennt -> ergo einfach direkt alles aus der Homepage ziehen
		// das geht schneller...
		// doch doch: in der vCard sind alle telnr aufgeführt
		// und Geschäftlich ist unterscheidbar aufgrund Eintrag Telnr in Gesch.
		
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		initHashMap(result);
		
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
		// *** extract link to detail info, actual name and firstname
		tmpPos = namePart.indexOf("<div class=\"long hide\">");
		tmpPos = tmpPos + "<div class=\"long hide\">".length();
		
		tmpPos = namePart.indexOf("href=\"", tmpPos);
		tmpPos = tmpPos + "href=\"".length();
		int endPos = namePart.indexOf("\"", tmpPos);
		String detailInfoLink = namePart.substring(tmpPos, endPos);
		
		System.out.println(detailInfoLink);
		
		tmpPos = namePart.indexOf("rel=\"nofollow\"", tmpPos) + "rel=\"nofollow\"".length();
		tmpPos = namePart.indexOf(">", tmpPos) + 1;
		namePart = namePart.substring(tmpPos);
		String[] fullName_ = namePart.split("</a>");
		String fullName = fullName_[0];
		fullName = fullName.replaceAll("[\\r\\n\\t]", "").trim();
		String[] nachnameVorname = getFirstnameLastname(fullName);
		String lastname = nachnameVorname[1];
		String firstname  = nachnameVorname[0];
		
		// *** extract actual street
		String street = streetPart.replaceAll("[\\r\\n\\t]", "").trim().replace("&nbsp;", " ");
		
		// *** extract actual zip and city
		String zipCity = zipCityPart.replaceAll("[\\r\\n\\t]", "").trim();
		String[] zipCity_ = zipCity.split("[\\s\\xA0]");
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
		
		// populate the hashMap
		result.put(PhoneBookEntry.FLD_DISPLAYNAME,    fullName);
		result.put(PhoneBookEntry.FLD_NAME,           lastname);
		result.put(PhoneBookEntry.FLD_FIRSTNAME,      firstname);
		result.put(PhoneBookEntry.FLD_STREET,         street);
		result.put(PhoneBookEntry.FLD_ZIP,            zip);
		result.put(PhoneBookEntry.FLD_PLACE,          city);
		result.put(PhoneBookEntry.FLD_PHONE1,         phone);
		result.put(PhoneBookEntry.FLD_FAX,            fax);
		result.put(PhoneBookEntry.FLD_WEBSITE,        website);
		result.put(PhoneBookEntry.FLD_EMAIL,          email);
		result.put(PhoneBookEntry.FLD_VCARDLINK,      vCard);
		result.put(PhoneBookEntry.FLD_DETAILINFOLINK, detailInfoLink);
		result.put(PhoneBookEntry.FLD_LAND,           "DE");
		
		// return hashMap
		return result;
	}
	
	@Override
	public List<HashMap<String, String>> extractKontakte() {
		reset();
		List<HashMap<String, String>> kontakte = new Vector<HashMap<String, String>>();
		// if there is a hits entry, then we found some data, else there are no entries
		boolean foundit = moveTo("<div class=\"hits\">");
		if (foundit)	{
			// skip first <td class=\"col1\">
			moveTo("folgendes MUSS in einer Zeile stehen, sonst macht IE Abstaende ");
			int listIndex = getNextPos("<td class=\"col1\">", 0);
			while (listIndex > 0) {
				HashMap<String, String> entry = null;
				entry = extractKontaktFromList();
				if (entry != null) {
					kontakte.add(entry);
				}
				listIndex = getNextPos("<td class=\"col1\">");
			}
		}
		return kontakte;
	}
	
	@Override
	public HashMap<String, String> extractMaxInfo(HashMap<String, String> kontaktHashMap) {
		// TODO Auto-generated method stub
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		initHashMap(result);
		
		/* many many times, this vCard link doesn't work (not even on the homepage itself...)
		 * and returns an empty doc
		 * usually the vCard link on the detail page is working -> try to read this one
		 * read contents of detail page
		 * NO - CHANGE: ALWAYS read vCard from detailPage since we need to read some additional
		 *              info from the detail page anyway
		 */
		
		// read vCardLink from kontaktHashMap
		String vCardLink = kontaktHashMap.get(PhoneBookEntry.FLD_VCARDLINK);
		if (vCardLink.equalsIgnoreCase("")){
			System.out.println("PhoneBookContentParser_de: no vCardLink found");
			return kontaktHashMap;
		}
		
		// ****** read contents of vCard
		// strip session id
		vCardLink = vCardLink.replaceAll(";jsessionid=[^?]*", "");
		vCardLink = vCardLink.replaceAll("\\+", "%2B");
		String vCardContents = readContent(vCardLink, "UTF-8", htmlReadTimeout);
		if (vCardContents.isEmpty()) {
			// read contents of detail page
			String detailPageLink = kontaktHashMap.get(PhoneBookEntry.FLD_DETAILINFOLINK);
			String detailPageContent = readContent(detailPageLink, "ISO-8859-1", htmlReadTimeout * 5);
			if (detailPageContent.equalsIgnoreCase(""))	{
				System.out.println("PhoneBookContentParser_de: no Detailpage info read");
				return kontaktHashMap;
			}
			// find start of vCardLink on detailPage
			int vCard2Start = detailPageContent.indexOf("href=\"VCard");
			String vCardLink2 = "";
			if (vCard2Start >= 0)	{
				// extract vCardLink from detailPage
				vCard2Start = vCard2Start + "href=\"VCard".length();
				int vCard2End   = detailPageContent.indexOf("\"", vCard2Start);
				vCardLink2 = detailPageContent.substring(vCard2Start, vCard2End);
				// prepare vCardLink
				vCardLink2 = vCardLink2.replaceAll(";jsessionid=[^?]*", "");
				vCardLink2 = vCardLink2.replaceAll("%", "%25");
				vCardLink = vCardLink.replaceAll("\\+", "%2B");
				// find the correct base address from parentLink
				// TODO
				//detailPageLink  http://www3.dastelefonbuch.de
				String wwwPart = detailPageLink.substring(0, detailPageLink.indexOf("?"));
				// now read the card
				String link = wwwPart + "/VCard" + vCardLink2;
				vCardContents = readContent(link, "UTF-8", 500);
				if (vCardContents.isEmpty())	{
					System.out.println("PhoneBookContentParser_de: Leere vCard auf Detailpage!");
					return kontaktHashMap;
				}
			}
			if (vCardContents.isEmpty()) {
				System.out.println("PhoneBookContentParser_de: Leere vCard auf Detailpage!");
				return kontaktHashMap;
			}
		}
		
		// strip ENCODING, CHARSET and PREF from vCard
		vCardContents = vCardContents.replaceAll(";CHARSET=[a-zA-Z1-9-_]+", "");
		vCardContents = vCardContents.replaceAll(";ENCODING=[a-zA-Z1-9-_]+", "");
		vCardContents = vCardContents.replaceAll(",PREF", "");
		
		// initizalize vCardParser
		VCardParser vCardParser = new VCardParser(vCardContents);
		
		// test if this is a company entry: if "TEL;WORK can be found then assume work-address
		boolean isCompany = false;
		if (vCardContents.indexOf("TEL;WORK") >= 0)	{
			if (vCardContents.indexOf("TEL;HOME") >= 0)	{
				isCompany = false;
			} else	{
				isCompany = true;
			}
		}
		
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
		// TODO
		/*
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
		*/
		
		// strip copyright notice [Copyright (c) local.ch ag] from vCard-field NOTE
		// TODO
		/*
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
		*/
		
		// ****** extract missing parts from html
		// Category
		/*
		<!-- Kategorie -->
            <div class="category">
                <div class="hl">Kategorie:</div>
                <div class="content">
                    Fach&auml;rzte f&uuml;r Innere Medizin und Allgemeinmedizin</div>
            </div>
        <!-- Kategorie Ende --> 
		 */
		/*
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
				poBox = poBox;
			}
		}		
		
		result.put(PhoneBookEntry.FLD_MAIDENNAME, maidenname);
		result.put(PhoneBookEntry.FLD_POBOX,      poBox);
		*/
		result.put(PhoneBookEntry.FLD_LAND,       "DE");
		
		// return result
		return result;
	}
	
	@Override
	public int getNumOfEntries() {
		reset();
		String resultStr = "0";
		if (moveTo("<div class=\"hits\">"))	{
			resultStr = extract("<span>", "</span>");
			resultStr = resultStr.replaceAll("[^0-9]", "");
			System.out.println("resultStr: " + resultStr);
		}
		return Integer.parseInt(resultStr);
	}
	
	@Override
	public String getSearchInfo() {
		reset();
		String searchInfoText = "";
		if (moveTo("<div class=\"functionbar\">"))	{
			searchInfoText = extract("<li class=\"blank\">", "</li>");
			// alle HTML <xxx> entfernen
			searchInfoText = searchInfoText.replaceAll("<[^>]+>", "");
			searchInfoText = searchInfoText.replaceAll("[\\r\\n\\t]", "");
		}
		// return result
		return searchInfoText;
	}
	
	@Override
	public URL getURL(String name, String geo, int pageNum) {
		name = name.replace(' ', '+');
		geo = geo.replace(' ', '+');
		country = country.toLowerCase();
		
		int lPageNum = pageNum;
		int recCount = 10;
		String urlPattern = "";
		try {
			name = URLEncoder.encode(name, "ISO-8859-1");
			geo  = URLEncoder.encode(geo,  "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		recCount = 20;
		lPageNum = lPageNum * 20 + 1;
		System.out.println("lPageNum: " + lPageNum);
		urlPattern = "http://www2.dastelefonbuch.de/?la={0}&kw={1}&ci={2}&ciid=&cmd=search&cifav=0&mdest=sec1.www1&vert_ok=1&recfrom={3}&reccount={4}"; //$NON-NLS-1$
		
		// *** actually create the URL
		try {
			return new URL(MessageFormat.format(urlPattern, new Object[] {
				Locale.getDefault().getLanguage(), name, geo, lPageNum, recCount
			}));
		} catch (MalformedURLException e) {
			return null;
		}
	}

}
