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
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import ch.elexis.data.Patient;

import java.util.HashMap;

public abstract class PhoneBookContentParser extends HtmlParser {	
	private static final String[] ADR_FIRSTNAMESEPARATORS = {" und ",  " u\\. ",  " e ",  " et "};
	protected static String country = "ch";
	protected static int readTimeOut = 7000;
	
	// members
	protected int    entriesPerPage = 20;
	protected String name           = "";
	protected String geo            = "";
	protected int    pageNum        = 0;
	
	/**
	 * this is the constructor: save name, geo and pageNum in members, calc htmlText and save in
	 * members too
	 * @param  name    search for this name
	 * @param  geo     search in this city/location
	 * @param  pageNum which page to get, zero-based
	 */
	public PhoneBookContentParser(final String name, final String geo, final int pageNum)	{
		super();
		String htmlText = readContent(name, geo, pageNum, readTimeOut);
		this.setHtmlText(htmlText);
		this.name     = name;
		this.geo      = geo;
		this.pageNum  = pageNum;
	}
	
	/**
	 * extract infos for the current search from HTML-source                        <br>
	 *  - für die Suche in ch - tel.search.ch         zBsp "Treffer 1 - 10 von 11"  <br>
	 *  - für die Suche in de - telefonbuch.de        zBsp "Seite  1 (von 6)"       <br>
	 *  - für die Suche in at - herold.at/telefonbuch zBsp "Treffer 1-15 von 61"    <br>
	 * Abstract function, must override
	 * @return the search string to be displayed as info
	 */
	public abstract String getSearchInfo();
	
	/**
	 * extracts the total number of found entries  <br>
	 * Abstract function, must override
	 * @return number of found entries
	 */
	public abstract int getNumOfEntries();
	
	/**
	 * extracts Kontakte from HTML                 <br>
	 * extract [entriesPerPage] number of entries  <br>
	 * Abstract function, must override
	 * @return the List of KontaktEntry's
	 */
	//public List<KontaktEntry> extractKontakte()	{
	public abstract List<HashMap<String, String>> extractKontakte();
	
	/**
	 * extracts a Kontakt from a listEntry (<b>multiple</b> results displayed on a page)  <br>
	 * this just extracts the parts needed for the display in the results list            <br>
	 * if the actual detail info is needed, then the vCards are extracted                 <br>
	 * Abstract function, must override
	 * @return the Kontakt in a HashMap, the possible keys of the HashMap are described above
	 */
	public abstract HashMap<String, String> extractKontaktFromList();
	
	/**
	 * extracts a Kontakt from a DetailEntry (<b>single</b> result displayed on a page)  <br>
	 * this just extracts the parts needed for the display in the results list           <br>
	 * if the actual detail info is needed, then the vCards are extracted                <br>
	 * this procedure can be the same as extracting from a list                          <br>
	 * - for ch this is different from extractListKontakt                                <br>
	 * - for de this is the same as extractListKontakt                                   <br>
	 * - for at this is the same as extractListKontakt
	 * Abstract function, must override
	 * @return the Kontakt in a HashMap, the possible keys of the HashMap are described above
	 */
	public abstract HashMap<String, String> extractKontaktFromDetail();
	
	/**
	 * extracts a Kontakt with ALL available info from a vCard and /or html combined   <br>
	 * usually if there is a vCard then it is better to use this to get the data
	 * @param kontaktHashMap Kontakt for which to extract the info
	 * @return the Kontakt in a HashMap, the possible keys of the HashMap are described above
	 */
	public abstract HashMap<String, String> extractMaxInfo(HashMap<String, String> kontaktHashMap);
	
	/*********************************************************************/
	/*** Some Helping Functions                                        ***/
	/*********************************************************************/
		
	/**
	 * extract the firstnames from the input String
	 * @param firstnames input
	 * @param delimiter used to separate the firstnames for the result string
	 * @return the separated firstnames, delimited by &lt;delimiter&gt;
	 */
	public static String extractFirstnames(final String firstnames, final String delimiter)	{
		String result = "";
		String lFirstnames = firstnames;
		for (int fn_sepIx = 0; fn_sepIx < ADR_FIRSTNAMESEPARATORS.length; fn_sepIx++)	{
			String fn_sep = ADR_FIRSTNAMESEPARATORS[fn_sepIx];
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
	
	/**
	 * extract last name and first name from input string.  <br>
	 * format: &lt;LastName&gt; &lt;FirstName&gt;
	 * @param text input string 
	 * @return String[]: StringArray, index O: firstname, index 1: lastname
	 */
	protected
	static String[] getFirstnameLastname(String text){
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
	
	/**
	 * convert the encoded characters into regular characters.
	 * This is used for decoding in tel.local.ch
	 * @param text
	 * @return
	 */
	protected static String cleanupSpecialChars(String text) {
		// this version is prepared for any characters
		String tmp = text;
		// this is already included here for tel.local.ch for convenience...
		tmp = tmp.replaceAll("&#x([0-9A-Fa-f]{2,2});", "%$1");
		try {
			// avoid errors... 
			tmp = tmp.replaceAll("%([^0-9A-Fa-f])", "&ç&ç&ç&ç$1");
			tmp = URLDecoder.decode(tmp, "ISO-8859-1");
			tmp = StringEscapeUtils.unescapeHtml(tmp);
			tmp = tmp.replaceAll("&ç&ç&ç&ç([^0-9A-Fa-f])", "%$1");
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
		String data = sourceString.replaceAll("=([0-9A-Fa-f]{2,2})", "%$1");
		try {
			data = URLDecoder.decode(data, "ISO-8859-1");
			return data;
		} catch (UnsupportedEncodingException e) {
			return sourceString;
		}
	}
	
	//****************************************************
	/**
	 * Format a phone number according to the country of the subclass
	 * @param phoneNumber the phoneNumber as returned from html
	 * @return the reformatted phone number. if the input is not formatted correctly, then the
	 * function returns an empty string
	 * this implementation just returns the unchanged phone number
	 */
	public static String formatPhoneNumber(final String phoneNumber)	{
		return phoneNumber;
	}
	
	/**
	 * Format a phone number according to your needs. NOT YET IMPLEMENTED
	 * @param phoneNumber the phoneNumber as returned from html
	 * @param inFormat the format of the input
	 * @param outFormat how to format the output
	 * @return the reformatted phone number. if the input is not formatted correctly, then the
	 * function returns an empty string
	 */
	public static String formatPhoneNumber(final String phoneNumber, final String inFormat, final String outFormat)	{
		return phoneNumber;
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
	
	/**
	 * creates and returns a URL for reading data from an online-address-query page   <br>
	 * Abstract function, must override
	 * @param  name    search for this name
	 * @param  geo     search in this city/location
	 * @param  pageNum which page to get
	 * @return the url which returns the results, null if any error occurs
	 */
	public abstract URL getURL(String name, String geo, int pageNum);
	
	/**
	 *  read and return the contents of a html page, uses default character encoding
	 *  timeout needed because on telefonbuch.de vCards sometimes do not work correctly...
	 *  
	 * @param  name    search for this name
	 * @param  geo     search in this city/location
	 * @param  pageNum which page to get
	 * @param timeOut = how long to wait for the page to be returned in milliseconds, 0 = no timeout
	 * 
	 * @return String, the contents of the page
	 */
	public String readContent(final String name, final String geo, final int pageNum, final int timeout)	{
		StringBuffer sb = new StringBuffer();
		URL url;
		InputStream input = null;
		try {
			url = getURL(name, geo, pageNum);
			// set timeout
			URLConnection urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(timeout);
			urlConnection.setReadTimeout(timeout);
			// now open the stream
			input = urlConnection.getInputStream();
			// read from stream
			int count = 0;
			char[] c = new char[10000];
			InputStreamReader isr = new InputStreamReader(input);
			while ((count = isr.read(c)) > 0) {
				sb.append(c, 0, count);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
		return cleanupSpecialChars(sb.toString());
	}
	
	/**
	 *  read and return the contents of a html page
	 *  
	 * @param urlText = the url from where the page should be read
	 * @param charSet = the character set to be used for page-encoding
	 * @param timeOut = how long to wait for the page to be returned in milliseconds, 0 = no timeout
	 * 
	 * @return String, the contents of the page
	 */
	public static String readContent(final String urlText, final String charSet, final int timeout) {
		StringBuffer sb = new StringBuffer();
		URL url;
		InputStream input = null;
		try {
			url = new URL(urlText);
			// set timeout
			URLConnection urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(timeout);
			urlConnection.setReadTimeout(timeout);
			// now open the stream
			input = urlConnection.getInputStream();
			// read from stream
			int count = 0;
			char[] c = new char[10000];
			InputStreamReader isr = new InputStreamReader(input, charSet);
			while ((count = isr.read(c)) > 0) {
				sb.append(c, 0, count);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
		return cleanupSpecialChars(sb.toString());
	}
	
	/**
	 * Retourniert String in umgekehrter Reihenfolge
	 */
	protected String reverseString(String text){
		if (text == null) {
			return "";
		}
		String reversed = "";
		for (char c : text.toCharArray()) {
			reversed = c + reversed;
		}
		return reversed;
	}
	
	protected static String removeDirt(String text){
		return text.replace("<span class=\"highlight\">", "").replace("</span>", "");
	}

	public void initHashMap(HashMap<String, String> entry)	{
		entry.put(PhoneBookEntry.FLD_FIRSTNAME, "");
		entry.put(PhoneBookEntry.FLD_NAME, "");
		entry.put(PhoneBookEntry.FLD_ZUSATZ, "");
		entry.put(PhoneBookEntry.FLD_STREET, "");
		entry.put(PhoneBookEntry.FLD_ZIP, "");
		entry.put(PhoneBookEntry.FLD_PLACE, "");
		entry.put(PhoneBookEntry.FLD_PHONE1, "");
		entry.put(PhoneBookEntry.FLD_PHONE2, "");
		entry.put(PhoneBookEntry.FLD_FAX, "");
		entry.put(PhoneBookEntry.FLD_EMAIL, "");
		entry.put(PhoneBookEntry.FLD_MOBILE, "");
		entry.put(PhoneBookEntry.FLD_MAIDENNAME, "");
		entry.put(PhoneBookEntry.FLD_TITLE, "");
		entry.put(PhoneBookEntry.FLD_COUNTRY, "");
		entry.put(PhoneBookEntry.FLD_PROFESSION, "");
		entry.put(PhoneBookEntry.FLD_WEBSITE, "");
		entry.put(PhoneBookEntry.FLD_CATEGORY, "");
		entry.put(PhoneBookEntry.FLD_ISORG, "");
		entry.put(PhoneBookEntry.FLD_VCARDLINK, "");
		entry.put(PhoneBookEntry.FLD_POBOX, "");
		entry.put(PhoneBookEntry.FLD_NOTE, "");
		entry.put(PhoneBookEntry.FLD_LAND, "");
		entry.put(PhoneBookEntry.FLD_PATID, "");
		entry.put(PhoneBookEntry.FLD_SEX, "");
		entry.put(PhoneBookEntry.FLD_DOB, "");
	}
}
