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

import org.apache.commons.lang.StringEscapeUtils;

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
		super(name, geo, pageNum, "ISO-8859-1");
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
		 * NO - CHANGE: ALWAYS read vCard from detailPage since we need to read some additional
		 *              info from the detail page anyway
		 */
		// read contents of detail page
		String detailPageLink = kontaktHashMap.get(PhoneBookEntry.FLD_DETAILINFOLINK);
		// find kw, extract and delete from link string
		int kwPos = detailPageLink.indexOf("&kw=");
		if (kwPos >= 0)	{
			int kwEndPos = detailPageLink.indexOf("&", kwPos + 1);
			String kwString = detailPageLink.substring(kwPos + 4, kwEndPos);
			// append converted string to end of link string
			try {
				detailPageLink = detailPageLink.replace("&kw=" + kwString, "");
				kwString = URLEncoder.encode(kwString, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			detailPageLink = detailPageLink + "&kw=" + kwString;
		}
		// find ci, extract and delete from link string
		int ciPos = detailPageLink.indexOf("&ci=");
		if (ciPos >= 0)	{
			int ciEndPos = detailPageLink.indexOf("&", ciPos + 1);
			String ciString = detailPageLink.substring(ciPos + 4, ciEndPos);
			// append converted string to end of link string
			try {
				detailPageLink = detailPageLink.replace("&ci=" + ciString, "");
				ciString = URLEncoder.encode(ciString, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			detailPageLink = detailPageLink + "&ci=" + ciString;
		}
		
		String detailPageContent = readContent(detailPageLink, "ISO-8859-1", htmlReadTimeout * 5);
		if (detailPageContent.equalsIgnoreCase(""))	{
			System.out.println("PhoneBookContentParser_de: no Detailpage info read");
			return kontaktHashMap;
		}
		// find start of vCardLink on detailPage
		int vCard2Start = detailPageContent.indexOf("href=\"VCard");
		String vCardLink2 = "";
		String vCardContents = "";
		if (vCard2Start >= 0)	{
			// extract vCardLink from detailPage
			vCard2Start = vCard2Start + "href=\"VCard".length();
			int vCard2End   = detailPageContent.indexOf("\"", vCard2Start);
			vCardLink2 = detailPageContent.substring(vCard2Start, vCard2End);
			// prepare vCardLink
			vCardLink2 = vCardLink2.replaceAll(";jsessionid=[^?]*", "");
			vCardLink2 = vCardLink2.replaceAll("%", "%25");
			vCardLink2 = vCardLink2.replaceAll("\\+", "%2B");
			// find the correct base address from parentLink
			String wwwPart = detailPageLink.substring(0, detailPageLink.indexOf("?"));
			// now read the card
			String link = wwwPart + "/VCard" + vCardLink2;
			vCardContents = readContent(link, "ISO-8859-1", 500);
			if (vCardContents.isEmpty())	{
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
		
		// title may be appended to Entry FLD_NAME
		// or appended to Entry FLD_DISPLAYNAME
		String vCardName = result.get(PhoneBookEntry.FLD_NAME);
		String vCardTitle = "";
		// try to extract from FLD_NAME first
		boolean foundTitle = false;
		for (int i = 1; i < ADR_TITLES.length; i++)	{
			String currTitle = ADR_TITLES[i];
			if (vCardName.indexOf(currTitle) >= 0)	{
				vCardTitle = currTitle;
				// strip title off vCardProfession
				vCardName = vCardName.replace(currTitle, "");
				foundTitle = true;
				break;
			}
			currTitle = ADR_TITLES[i].replaceAll(" ", "");
			if (vCardName.indexOf(currTitle) >= 0)	{
				vCardTitle = currTitle;
				// strip title off vCardProfession
				vCardName = vCardName.replace(currTitle, "");
				foundTitle = true;
				break;
			}
		}
		// now try to extract from FLD_DISPLAY
		String vCardName2 = kontaktHashMap.get(PhoneBookEntry.FLD_DISPLAYNAME);
		if (!foundTitle)	{
			for (int i = 1; i < ADR_TITLES.length; i++)	{
				String currTitle = ADR_TITLES[i];
				if (vCardName2.indexOf(currTitle) >= 0)	{
					vCardTitle = currTitle;
					// strip title off vCardProfession
					//vCardName = vCardName2.replace(currTitle, "");
					break;
				}
				currTitle = ADR_TITLES[i].replaceAll(" ", "");
				if (vCardName2.indexOf(currTitle) >= 0)	{
					vCardTitle = currTitle;
					// strip title off vCardProfession
					//vCardName = vCardName2.replace(currTitle, "");
					break;
				}
			}
		}
		result.put(PhoneBookEntry.FLD_TITLE, vCardTitle);
		result.put(PhoneBookEntry.FLD_NAME,  vCardName);
		
		// ****** extract missing parts from html: Kategorie
		String category = "";
		HtmlParser subParser = new HtmlParser(detailPageContent);
		// move to <div class="category">
		if (subParser.moveTo("<div class=\"category\">")) {
			category = subParser.extract("<div class=\"content\">", "</div>");
			category = category.replaceAll("<[^>]+>", ""); //$NON-NLS-1$
			category = category.replaceAll("[\t\r\n]", "").trim();
		}		
		result.put(PhoneBookEntry.FLD_PROFESSION, category);
		
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
			//name = new String (name.getBytes(),"UTF-8");
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

	@Override
	public String[][] getCitiesList() {
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
							String cell = cells[2].replaceAll("\\xA0", " ").trim();
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

	@Override
	public String getCitiesListMessage() {
		//if (!hasCitiesList()) return "";
		reset();
		if (!moveTo("<!-- Meldung -->")) return "";
		String message = extract("<div id=\"msg-caution\">", "</div>");
		message = message.replaceAll("[\\r\\n\\t]", "").trim();
		message = message.replaceAll("<br[ ]*[/]*[ ]*>", "\n");
		message = StringEscapeUtils.unescapeHtml(message);
		return message;
	}
	
	@Override
	public boolean hasCitiesList() {
		reset();
		if (getNextPos("ortsliste") == -1)	{
			return false;
		}
		return true;
		/*
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
		*/
	}
	
	@Override
	public boolean noCityFound() {
		if (getNextPos("FEHLER_KEIN_ORT_GEFUNDEN") == -1)	{
			return false;
		}
		return true;
	}
	
	public String[][] getCitySuggestions(final String part)	{
		String result = readContent("http://www.telefonbuch.de/Suggestor?source=sm&max_res=20&where=" + part, "UTF-8", this.htmlReadTimeout);
		result = result.replaceAll("zeige\\(0, new Array\\(\"ort\"\\), new Array\\(new Array\\(\"", "");
		result = result.replaceAll("\"\\), new Array\\(\"", ";");
		result = result.replaceAll("\"\\)\\)\\);", " ");
		System.out.println(result);
		String[] splitted = result.split(";");
		String results[][] = new String[splitted.length][2];
		for (int i = 0; i < splitted.length; i ++)	{
			results[i][0] = splitted[i];
			results[i][1] = "1";
		}
		return results;
	}
	
	/*
	 * http://www.telefonbuch.de/Suggestor?source=sm&max_res=20&where=Singen
	 * zeige(0, new Array("ort"), new Array(new Array("Singen Hohentwiel"), new Array("Singhofen"), new Array("Singwitz Gem. Obergurig"), new Array("Singlis Stadt Borken (Hessen)"), new Array("Singelbert Gem. Reichshof"), new Array("Singen Gem. Remchingen"), new Array("Singer Gem. Sankt Wolfgang"), new Array("Singenbach"), new Array("Singern"), new Array("Singlding Stadt Erding")));
	 */
}
