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
 * $Id: DirectoriesHelper.java 4628 2008-10-23 07:57:50Z michael_imhof $
 *******************************************************************************/

package ch.medshare.elexis.directories_marlovits;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Locale;

public class DirectoriesHelper {
	
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
		boolean useNewVersion = false;
		
		if (useNewVersion){
			text = text.replaceAll("&#x([0-9A-Fa-f]{2,2});", "%$1");
			try {
				text = URLDecoder.decode(text, "ISO-8859-1");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return text;
		} else	{
			// this version is NOT prepared for any characters
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
			text = text.replace("&#xE2;", "â");//$NON-NLS-1$ //$NON-NLS-2$
			
			text = text.replace("&#xA0;", " ");//$NON-NLS-1$ //$NON-NLS-2$
			
			text = text.replace("&nbsp;", " ");//$NON-NLS-1$ //$NON-NLS-2$
			
			return text;
		}
    }
	/**
	 * create and return the url for an online address query
	 * @param  name    search for this name
	 * @param  geo     search in this city/location
	 * @param  country search in this country - must be iso2 name of the country
	 * @return the url which returns the results, null if any error occurs
	 */
	private static URL getURL_(String name, String geo, String country)	{
		name = name.replace(' ', '+');
		geo = geo.replace(' ', '+');
		country = country.toLowerCase();
		
		int recCount = 10;
		String urlPattern = "";
		// *** create the url string for different countries
		if (country.equalsIgnoreCase("ch"))	{
			// *** switzerland
			urlPattern = "http://tel.local.ch/{0}/q/?what={1}&where={2}"; //$NON-NLS-1$
		} else if (country.equalsIgnoreCase("de"))	{
			// *** germany
			try {
				name = URLEncoder.encode(name, "ISO-8859-1");
				geo  = URLEncoder.encode(geo,  "ISO-8859-1");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			recCount = 20;
			urlPattern = "http://www.dastelefonbuch.de/?la={0}&kw={1}&ci={2}&ciid=&cmd=search&cifav=0&mdest=sec1.www1&vert_ok=1&recfrom=1&reccount=10"; //$NON-NLS-1$
		}
		
		// *** actually create the URL
		try {
			return new URL(MessageFormat.format(urlPattern, new Object[] {
				Locale.getDefault().getLanguage(), name, geo, recCount
			}));
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	/**
	 * 
	 * @param  name    search for this name
	 * @param  geo     search in this city/location
	 * @param  country search in this country - must be iso2 name of the country
	 * @param  pageNum
	 * @return the url which returns the results, null if any error occurs
	 */
	private static URL getURL(String name, String geo, String country, int pageNum)	{
		name = name.replace(' ', '+');
		geo = geo.replace(' ', '+');
		country = country.toLowerCase();
		
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
			urlPattern = "http://www.dastelefonbuch.de/?la={0}&kw={1}&ci={2}&ciid=&cmd=search&cifav=0&mdest=sec1.www1&vert_ok=1&recfrom={3}&reccount={4}"; //$NON-NLS-1$
		}
		
		// *** actually create the URL
		try {
			return new URL(MessageFormat.format(urlPattern, new Object[] {
				Locale.getDefault().getLanguage(), name, geo, pageNum, recCount
			}));
		} catch (MalformedURLException e) {
			return null;
		}
	}
	
	/**
	 * Schreibt binäre Datei
	 */
	public static void writeFile(String filenamePath, final String text)
		throws IOException{
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(filenamePath);
			output.write(text.getBytes());
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}
	
	/**
	 * Liest Inhalt einer Web-Abfrage auf www.directories.ch/weisseseiten
	 */
	public static String readContent_(final String name, final String geo, final String country)
		throws IOException, MalformedURLException{
		URL content = getURL_(name, geo, country);
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
		return cleanupUmlaute(cleanupText(sb.toString()));
	}
	
	/**
	 * Liest Inhalt einer Web-Abfrage auf www.directories.ch/weisseseiten
	 * mit Angabe der Seite
	 */
	public static String readContent(final String name, final String geo, final String country, int pageNum)
		throws IOException, MalformedURLException{
		
		URL content = getURL(name, geo, country, pageNum);
		InputStream input = content.openStream();
		
		StringBuffer sb = new StringBuffer();
		
		int count = 0;
		char[] c = new char[10000];
		InputStreamReader isr = new InputStreamReader(input, "ISO-8859-1");
		try {
			while ((count = isr.read(c)) > 0) {
				sb.append(c, 0, count);
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return cleanupUmlaute(cleanupText(sb.toString()));
	}
}
