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

public class PhoneBookContentParser_at extends PhoneBookContentParser {
	
	public PhoneBookContentParser_at(String name, String geo, int pageNum) {
		super(name, geo, pageNum);
	}
	
	@Override
	public HashMap<String, String> extractKontaktFromDetail() {
		// in herold.at there is no distinct detail page when searching
		// -> just call the list-version
		return extractKontaktFromList();
	}
	
	@Override
	public HashMap<String, String> extractKontaktFromList() {
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		initHashMap(result);
		
		// get detail info links
		moveTo("http://www.herold.at/telefonbuch/");
		String detailInfoLink = "http://www.herold.at/telefonbuch/" + extractTo("\">");
		
		// get lastname, firstname, title if any (separated by comma)
		String lastnameFistname = extractTo("</a>");
		String[] firstnameLastname = getFirstnameLastname(lastnameFistname);
		String firstname = firstnameLastname[0].trim();
		String lastname  = firstnameLastname[1].trim();
		String[] firstnameTitle = firstname.split(",");
		firstname = firstnameTitle[0].trim();
		String title = "";
		if (firstnameTitle.length > 1)	{
			title = firstnameTitle[1].trim();
		}
		
		// extract group info if present
		int groupPos = getNextPos("<div class=\"group\">");
		int addrPos = getNextPos("<div class=\"group\">");
		String group = "";
		if ((groupPos >= 0) && (groupPos < addrPos))	{
			moveTo("<div class=\"group\">");
			group = extractTo("</div>");
		}
		
		// get firstLine/secondline (zip, city, street, street number)
		moveTo("class=\"addrF\"><p");
		moveTo(">");
		String dataLines= extractTo("<br /></p>");
		// split lines
		String[] linesArr = dataLines.split("<br />");
		String zusatz = "";
		String zipCityStreetNum = "";
		if (linesArr.length > 1){
			zusatz = linesArr[0].trim();
			zipCityStreetNum = linesArr[1].trim();
		} else	{
			zipCityStreetNum = linesArr[0].trim();
		}
		String[] splitted = zipCityStreetNum.split(",");
		String zipCity = splitted[0].trim();
		String[] zipCityArray = getFirstnameLastname(zipCity);
		String zip  = zipCityArray[1].trim();
		String city = zipCityArray[0].trim();
		String street    = splitted[1].trim();
		if (splitted.length > 2)	{
			street = street + ", " + splitted[2].trim();
		}
		
		// populate the hashMap
		result.put(PhoneBookEntry.FLD_DETAILINFOLINK, detailInfoLink);
		result.put(PhoneBookEntry.FLD_NAME,           lastname);
		result.put(PhoneBookEntry.FLD_FIRSTNAME,      firstname);
		result.put(PhoneBookEntry.FLD_TITLE,          title);
		result.put(PhoneBookEntry.FLD_ZUSATZ,         zusatz);
		
		result.put(PhoneBookEntry.FLD_ZIP,            zip);
		result.put(PhoneBookEntry.FLD_PLACE,          city);
		result.put(PhoneBookEntry.FLD_STREET,         street);
		result.put(PhoneBookEntry.FLD_LAND,           "AT");
		result.put(PhoneBookEntry.FLD_CATEGORY,       group);
		
		// return hashMap
		return result;
	}
	
	@Override
	public List<HashMap<String, String>> extractKontakte() {
		reset();
		List<HashMap<String, String>> kontakte = new Vector<HashMap<String, String>>();
		// if class="noresults" can be found, then there are no results found...
		boolean foundit = (this.getNextPos("class=\"noresults\"") >= 0);
		if (foundit) return kontakte;
		foundit = moveTo("<!--begin: results-->");
		if (foundit)	{
			int listIndex = getNextPos("http://www.herold.at/telefonbuch/");
			while (listIndex >= 0) {
				HashMap<String, String> entry = null;
				entry = extractKontaktFromList();
				if (entry != null) {
					kontakte.add(entry);
				}
				listIndex = getNextPos("http://www.herold.at/telefonbuch/");
				// as long as there still is an end marker, there is more data - else break
				int endMarkerPos = getNextPos("<!--end: results-->", listIndex);
				if (endMarkerPos < 0) break;
			}
		}
		return kontakte;
	}
	
	@Override
	public HashMap<String, String> extractMaxInfo(
			HashMap<String, String> kontaktHashMap) {
		// TODO Auto-generated method stub
		//SWTHelper.alert("Error", "extractMaxInfo() not yet implemented!");
		return kontaktHashMap;
	}
	
	@Override
	public int getNumOfEntries() {
		reset();
		moveTo("<!--begin: pagegrid-->");
		moveTo("</strong>");
		moveTo("<strong>");
		String result = extractTo("</strong>");
		return Integer.parseInt(result.trim());
	}
	
	@Override
	public String getSearchInfo() {
		reset();
		moveTo("<!--begin: pagegrid-->");
		moveTo("<strong>");
		String result = extractTo("</h1>");
		// alle HTML <xxx> entfernen
		result = result.replaceAll("<[^>]+>", "");
		return result.trim();
	}
	
	@Override
	public URL getURL(String name, String geo, int pageNum) {
//		http://www.herold.at/servlet/at.herold.sp.servlet.SPWPSearchServlet?searchregion=Ratten&searchterm=Marlovits
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
		// language not supported in herold.at ???
		// lang     = {0}
		// name     = {1}
		// geo      = {2}
		// lPageNum = {3}
		// recCount = {4}
		urlPattern = "http://www.herold.at/servlet/at.herold.sp.servlet.SPWPSearchServlet?searchregion={2}&searchterm={1}&fmExact=1";
		
		// *** actually create the URL
		try {
			String tmp = MessageFormat.format(urlPattern, new Object[] {
				Locale.getDefault().getLanguage(), name, geo, lPageNum, recCount
			});
			
			return new URL(MessageFormat.format(urlPattern, new Object[] {
				Locale.getDefault().getLanguage(), name, geo, lPageNum, recCount
			}));
		} catch (MalformedURLException e) {
			return null;
		}
	}

}
