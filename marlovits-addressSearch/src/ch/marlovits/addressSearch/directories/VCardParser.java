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

package ch.marlovits.addressSearch.directories;

import java.util.HashMap;

public class VCardParser {
	// constants
	private static String MARKER_VCARD_BEGIN_ = "BEGIN:VCARD";
	private static String MARKER_VCARD_END_   = "END:VCARD";
	
	// members
	private String vCardContents = "";					// the full text of the vCard
	private HashMap<String, String> vCardValues = null;	// hashtable containing values of this vCard
	
	/**
	 * the constructor: save full contents and parse contents into the internal HashMap
	 * @param vCardContents
	 */
	public VCardParser(String vCardContents){
		// first test if this really is a vCard: Start with "BEGIN:VCARD", end with "END:VCARD"
		String MARKER_VCARD_NEWLINE = getDocReturnCharacter(vCardContents);
		String MARKER_VCARD_BEGIN = MARKER_VCARD_BEGIN_ + MARKER_VCARD_NEWLINE;
		if (!vCardContents.substring(0, MARKER_VCARD_BEGIN.length()).equalsIgnoreCase(MARKER_VCARD_BEGIN))	{
			return;
		}
		int endMarkerPos = vCardContents.indexOf(MARKER_VCARD_END_);
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
			int colonPos = vCardLine.indexOf(":");
			if (colonPos >= 0)	{
				String key   = vCardLine.substring(0, colonPos).toUpperCase();
				String value = vCardLine.substring(colonPos + 1);
				if (!key.isEmpty()){
					vCardValues.put(key, value);
				}
			}
			lineStartIx = nextLinestartIx + MARKER_VCARD_NEWLINE.length();
		}
		}
	
	/**
	 * find the value for a key in the vCard HashMap
	 * @param key   the key for which to find the matching value
	 * @param index the index of the actual info in the data, zero-based, semicolon-delimited
	 * @return
	 */
	public String getVCardValue(final String key, int index)	{
		if (vCardValues == null) return "";
		String data = vCardValues.get(key.toUpperCase());
		if (data == null) return "";
		String[] dataParts = data.split(";");
		data = "";
		if (dataParts.length > index)	{
			data = dataParts[index];
		}
		return data;
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
