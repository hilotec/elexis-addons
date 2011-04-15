/*******************************************************************************
 * Copyright (c) 2009, Harald Marlovits
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Harald Marlovits
 *    
 * $Id: WikiTableExtractor.java
 *******************************************************************************/

package ch.marlovits.plz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.elexis.util.SWTHelper;

class WikiTableExtractor {
	// prettytable sortable
	private static final String WIKI_LAND_STARTINFO_MARKER =
		"<table class=\"prettytable sortable*\">";
	private static final String TABLE_ENDMARKER = "</table*>";
	private static final String TABLEROW_STARTMARKER = "<tr";
	private static final String TABLEROW_ENDMARKER = "</tr>";
	private static final String TABLEDATA_STARTMARKER = "<td>";
	private static final String TABLEDATA_ENDMARKER = "</td>";
	
	public static String[][] getWikiTable(final String url, final String tableName,
		final boolean skipFirstLine){
		String wholeHTMLPage = null;
		try {
			// TO DO: abhängig von language
			wholeHTMLPage = readHTMLPage(url);
		} catch (MalformedURLException e) {
			SWTHelper.alert("Fehler", "Die URL '" + url + "' ist nicht korrekt formatiert.");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			SWTHelper.alert("Fehler", "Die URL '" + url + "' kann nicht eingelesen werden.");
			e.printStackTrace();
			return null;
		}
		
		// *** Tabellen-Inhalt aus HTML extrahieren
		// den Start des Tabelleninhaltes suchen
		Pattern p = Pattern.compile(tableName);
		Matcher myMatcher = p.matcher(wholeHTMLPage);
		boolean foundIt = myMatcher.find();
		if (!foundIt)
			return null;
		int markerEndPos = myMatcher.end();
		
		// das Ende des Tabelleninhaltes suchen
		p = Pattern.compile(TABLE_ENDMARKER);
		myMatcher = p.matcher(wholeHTMLPage);
		foundIt = myMatcher.find(markerEndPos);
		int tableEndPos = myMatcher.start();
		if (!foundIt)
			return null;
		
		// den Tabelleninhalt einlesen
		String tableText = wholeHTMLPage.subSequence(markerEndPos, tableEndPos).toString();
		
		// System.out.println(tableText);
		
		boolean removeHtmlTags = true;
		String[] resultLine = new String[100];
		
		String[][] arrayOfStrings = null;
		
		// durch die Tabelle loopen und die einzelnen Datensätze einlesen
		tableText = tableText.replaceAll("</tr*>", "");
		String[] rows = tableText.split("<tr*>");
		int startRow = 1; // die erste Zeile ist immer leer
		if (skipFirstLine)
			startRow = 2;
		boolean arrayInitialized = false;
		for (int i = startRow; i < rows.length; i++) {
			String row = rows[i];
			System.out.print(rows[i]);
			String[] cells = row.split("<t[dh]*>");
			// beim ersten Durchgang den Array in der richtigen Grösse initialisieren
			if (arrayInitialized == false) {
				arrayOfStrings = new String[rows.length][cells.length];
				arrayInitialized = true;
			}
			resultLine[0] = Boolean.toString(cells[1].indexOf("</th") != -1);
			arrayOfStrings[i - 1][0] = resultLine[0];
			for (int c = 1; c < cells.length; c++) {
				String cell = cells[c].split("</t[dh]*>")[0];
				resultLine[c] = cell;
				if (removeHtmlTags)
					cell = stripHTMLTags(cell);
				arrayOfStrings[i - 1][c] = cell;
				System.out.println(cell);
			}
		}
		return arrayOfStrings;
	}
	
	public static String readHTMLPage(final String url) throws IOException, MalformedURLException{
		URL content = new URL(url);
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
		return sb.toString();
	}
	
	/**
	 * Entfernt aus einem String alle HTML-Tags <XXX> und </XXX>
	 * 
	 * @param inputString
	 * @return inputString, von welchem alle whiteSpaces weggestrippt sind
	 */
	private static String stripHTMLTags(final String inputString){
		String tmp = inputString;
		tmp = tmp.replaceAll("</.*>", "");
		tmp = tmp.replaceAll("<.*>", "");
		return tmp;
	}
}