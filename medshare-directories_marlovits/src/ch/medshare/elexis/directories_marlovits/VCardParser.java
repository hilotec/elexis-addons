/*******************************************************************************
 * Copyright (c) 2010, Harald Marlovits
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Read contents from a vcard
 *
 * Contributors:
 *    Harald Marlovits
 *    
 * $Id: VCardParser
 *******************************************************************************/

package ch.medshare.elexis.directories_marlovits;

import java.util.HashMap;

public class VCardParser {
	// constants
	private static String MARKER_VCARD_BEGIN_ = "BEGIN:VCARD";
	private static String MARKER_VCARD_END_   = "END:VCARD";
	
	// members
	private String vCardContents = "";					// the full text of the vCard
	private HashMap<String, String> vCardValues = null;	// hashtable containing values of this vCard
	
	/**
	 * the constructor: save full contents and parse contents into the internal hashtable
	 * @param vCardContents
	 */
	public VCardParser(String vCardContents){
		// first test if this really is a vCard: Start with "BEGIN:VCARD", end with "END:VCARD"
		String MARKER_VCARD_NEWLINE = getDocReturnCharacter(vCardContents);
		String MARKER_VCARD_BEGIN = MARKER_VCARD_BEGIN_ + MARKER_VCARD_NEWLINE;
		String MARKER_VCARD_END   = MARKER_VCARD_BEGIN_ + MARKER_VCARD_NEWLINE;
		if (!vCardContents.substring(0, MARKER_VCARD_BEGIN.length()).equalsIgnoreCase(MARKER_VCARD_BEGIN))	{
			return;
		}
		int endMarkerPos = vCardContents.indexOf(MARKER_VCARD_END);
		if (endMarkerPos == -1)	{
			return;
		}
		
		// save contents internally
		this.vCardContents = vCardContents;
		
		// create and populate the hashMap
		vCardValues = new HashMap<String, String>();
		int lineStartIx     = 0;
		int nextLinestartIx = 0;
		while ((nextLinestartIx = vCardContents.indexOf(MARKER_VCARD_NEWLINE, lineStartIx))>=0)	{
			String vCardLine = vCardContents.substring(lineStartIx, nextLinestartIx);
			// find pos of first colon: on the left = key, on the right = data
			String key   = "";
			String value = "";
			int colonPos = vCardLine.indexOf(":");
			if (colonPos >= 0)	{
				key   = vCardLine.substring(1, colonPos);
				value = vCardLine.substring(colonPos + 1);
			}
			vCardValues.put(key, key);
			lineStartIx = nextLinestartIx + MARKER_VCARD_NEWLINE.length();
		}
		System.out.println("at end of constructor");
		}
	
	/**
	 * find the value for a key in the vCard HashMap
	 * @param key : the key for which to find the matching value
	 * @return
	 */
	public String getVCardValue(final String key)	{
		if (vCardValues == null) return "";
		return vCardValues.get(key);
	}
	
	/**
	 * 
	 * @param vCardContents
	 * @param key
	 * @param isCompany
	 * @param lang
	 * @return
	 */
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
	
	/**
	 * try to find out which kind of return character this document uses
	 * @param contents : the text to be analyzed
	 * @return : the return character used: "\r\n" or "\r" or "\n"
	 */
	public static String getDocReturnCharacter(final String contents)	{
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

}
