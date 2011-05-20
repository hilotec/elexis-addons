package ch.marlovits.addressSearch.directories;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class PhoneBookContentParser_at extends PhoneBookContentParser {
	
	public PhoneBookContentParser_at(String name, String geo, int pageNum){
		super(name, geo, pageNum, "UTF-8");
	}
	
	@Override
	public HashMap<String, String> extractKontaktFromDetail(){
		// in herold.at there is no distinct detail page when searching
		// -> just call the list-version
		return extractKontaktFromList();
	}
	
	@Override
	public HashMap<String, String> extractKontaktFromList(){
		// create an empty HashMap
		HashMap<String, String> result = new HashMap<String, String>();
		initHashMap(result);
		
		// get detail info links
		moveTo("<h3><a href=\"http://www.herold.at/telefonbuch/");
		String detailInfoLink = "http://www.herold.at/telefonbuch/" + extractTo("\">");
		
		// get lastname, firstname, title if any (separated by comma)
		String lastnameFirstname = extractTo("</a>");
		String[] firstnameLastname = getFirstnameLastname(lastnameFirstname);
		String firstname = firstnameLastname[0].trim();
		String lastname = firstnameLastname[1].trim();
		String[] firstnameTitle = firstname.split(",");
		firstname = firstnameTitle[0].trim();
		String title = "";
		if (firstnameTitle.length > 1) {
			int commaPos = lastnameFirstname.indexOf(",");
			title = lastnameFirstname.substring(commaPos + 1).trim();
		}
		
		// extract group info if present
		int groupPos = getNextPos("<div class=\"group\">");
		int addrPos = getNextPos("class=\"addrF\"><p");
		String group = "";
		if ((groupPos >= 0) && (groupPos < addrPos)) {
			moveTo("<div class=\"group\">");
			group = extractTo("</div>").trim();
		}
		
		// get firstLine/second line/third line
		// last line contains the actual address (zip, city, street, street number)
		// BUT: the zip may be missing... and the street, and the number...
		// SO: use the LAST line as the address line and extract address
		// the lines before is additional data -> combine into one single line
		moveTo("class=\"addr");
		moveTo("\"><p");
		moveTo(">");
		String dataLines = extractTo("<br /></p>");
		// split lines
		String[] linesArr = dataLines.split("<br />");
		String lastLine = linesArr[linesArr.length - 1].trim();
		String otherLines = "";
		String delim = "";
		for (int i = 0; i < linesArr.length - 1; i++) {
			otherLines = otherLines + delim + linesArr[i].trim();
			delim = ", ";
		}
		String[] zipCityStreetNum = lastLine.split(",");
		String zipCity = zipCityStreetNum[0].trim();
		String zip = zipCity.split(" ")[0];
		String city = "";
		if ((zip.length() != 4) || (!zip.replaceAll("[0-9]", "").equalsIgnoreCase(""))) {
			// no zip specified
			zip = "";
			city = zipCity;
		} else {
			// data to the right of " " is the city
			city = zipCity.substring(5).trim();
		}
		
		String street = "";
		if (zipCityStreetNum.length > 1) {
			street = zipCityStreetNum[1].trim();
			if (zipCityStreetNum.length > 2) {
				street = street + ", " + zipCityStreetNum[2].trim();
			}
		}
		
		// populate the hashMap
		result.put(PhoneBookEntry.FLD_DETAILINFOLINK, detailInfoLink);
		result.put(PhoneBookEntry.FLD_DISPLAYNAME, lastnameFirstname);
		result.put(PhoneBookEntry.FLD_NAME, lastname);
		result.put(PhoneBookEntry.FLD_FIRSTNAME, firstname);
		result.put(PhoneBookEntry.FLD_TITLE, title);
		result.put(PhoneBookEntry.FLD_ZUSATZ, otherLines);
		
		result.put(PhoneBookEntry.FLD_ZIP, zip);
		result.put(PhoneBookEntry.FLD_PLACE, city);
		result.put(PhoneBookEntry.FLD_STREET, street);
		result.put(PhoneBookEntry.FLD_LAND, "AT");
		result.put(PhoneBookEntry.FLD_CATEGORY, group);
		
		// return hashMap
		return result;
	}
	
	@Override
	public List<HashMap<String, String>> extractKontakte(){
		reset();
		List<HashMap<String, String>> kontakte = new Vector<HashMap<String, String>>();
		// if class="noresults" can be found, then there are no results found...
		boolean foundit = (this.getNextPos("class=\"noresults\"") >= 0);
		if (foundit)
			return kontakte;
		foundit = moveTo("<!--begin: results-->");
		if (foundit) {
			int listIndex = getNextPos("<h3><a href=\"http://www.herold.at/telefonbuch/");
			while (listIndex >= 0) {
				HashMap<String, String> entry = null;
				entry = extractKontaktFromList();
				if (entry != null) {
					kontakte.add(entry);
				}
				listIndex = getNextPos("<h3><a href=\"http://www.herold.at/telefonbuch/");
				// as long as there still is an end marker, there is more data - else break
				int endMarkerPos = getNextPos("<!--end: results-->", listIndex);
				if (endMarkerPos < 0)
					break;
			}
		}
		return kontakte;
	}
	
	@Override
	public HashMap<String, String> extractMaxInfo(HashMap<String, String> kontaktHashMap){
		// TODO Auto-generated method stub
		// SWTHelper.alert("Error", "extractMaxInfo() not yet implemented!");
		return kontaktHashMap;
	}
	
	@Override
	public int getNumOfEntries(){
		reset();
		moveTo("<!--begin: pagegrid-->");
		moveTo("</strong>");
		moveTo("<strong>");
		String result = extractTo("</strong>");
		return Integer.parseInt(result.trim());
	}
	
	@Override
	public String getSearchInfo(){
		reset();
		moveTo("<!--begin: pagegrid-->");
		moveTo("<strong>");
		String result = extractTo("</h1>");
		// alle HTML <xxx> entfernen
		result = result.replaceAll("<[^>]+>", "");
		return result.trim();
	}
	
	@Override
	public URL getURL(String name, String geo, int pageNum){
		// http://www.herold.at/servlet/at.herold.sp.servlet.SPWPSearchServlet?searchregion=Ratten&searchterm=Marlovits
		name = name.replace(' ', '+');
		geo = geo.replace(' ', '+');
		country = country.toLowerCase();
		
		int lPageNum = pageNum;
		int recCount = 10;
		String urlPattern = "";
		// try {
		// name = URLEncoder.encode(name, "ISO-8859-1");
		// geo = URLEncoder.encode(geo, "ISO-8859-1");
		// } catch (UnsupportedEncodingException e) {
		// e.printStackTrace();
		// }
		recCount = 20;
		lPageNum = lPageNum * 20 + 1;
		// language not supported in herold.at ???
		// lang = {0}
		// name = {1}
		// geo = {2}
		// lPageNum = {3}
		// recCount = {4}
		urlPattern =
			"http://www.herold.at/servlet/at.herold.sp.servlet.SPWPSearchServlet?searchregion={2}&searchterm={1}&fmExact=1";
		
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
	
	@Override
	public String[][] getCitiesList(){
		return null;
	}
	
	@Override
	public String getCitiesListMessage(){
		return "";
	}
	
	@Override
	public boolean hasCitiesList(){
		return false;
	}
	
	@Override
	public boolean noCityFound(){
		return false;
	}
	
	@Override
	public String[][] getCitySuggestions(String part){
		// TODO Auto-generated method stub
		return null;
	}
}
