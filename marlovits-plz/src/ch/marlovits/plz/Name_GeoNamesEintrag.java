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
 * $Id: NamesEintrag.java
 *******************************************************************************/

package ch.marlovits.plz;

import ch.elexis.data.PersistentObject;

public class Name_GeoNamesEintrag extends PersistentObject {
	// Tabellenname in der Datenbank
	private static final String TABLENAME = "CH_MARLOVITS_NAME_GEONAMES";
	// die Version der Tabelle
	public static final String VERSION = "2.0";
	// CreateScript für die Tabelle
	private static final String createDB =
		"CREATE TABLE " + TABLENAME + " (" +
		"id					character varying(25) NOT NULL,	" + // alternateNameId
		"nameid				character varying,				" + // geonameid
		"language			character(10),					" +	// iso 639 language
		"name				character varying,				" + // Name in der Sprache <language>
		"ispreferredname	CHAR(1),						" + // '1', if this alternate name is an official/preferred name
		"isshortname		CHAR(1),						" + // '1', if this is a short name like 'California' for 'State of California'
		"deleted			CHAR(1) default '0',			" +
		"lastupdate			bigint,							" +
		"CONSTRAINT	" + TABLENAME + "_pkey PRIMARY KEY (id)	" + // Primary key erstellen
		")													" +
		"WITH (OIDS=FALSE);									" +
		"ALTER TABLE " + TABLENAME + " OWNER TO elexisuser;	" + // den owner für die Tabelle setzen
		// die Indizes erstellen
		"CREATE INDEX " + TABLENAME + "0 ON " + TABLENAME + " USING btree (nameid);		" +
		"CREATE INDEX " + TABLENAME + "1 ON " + TABLENAME + " USING btree (name);		";
	
	static	{
		// ziemlich einfaches Mapping - einheitlich einfach der erste Buchstaben der Feldbezeichnung in der Datenbank als Capital
		addMapping(	TABLENAME,
					"Nameid=nameid",
					"Language=language",
					"Name=name",
					"Ispreferredname=ispreferredname",
					"Isshortname=isshortname",
					"Deleted=deleted",
					"Lastupdate=lastupdate");
		
		// Erstellen der Tabelle in der Datenbank
		createOrModifyTable(createDB);
		/*
		LandEintrag version = LandEintrag.load("1");
		if (!version.exists()) {
			createOrModifyTable(createDB);
		} else {
			VersionInfo vi = new VersionInfo(version.getLandName());
			if (vi.isOlder(VERSION)) {
				// Update-Script für ältere Versionen in der Datenbank
				if (vi.isOlder("1.0.0")) {
					getConnection().exec("ALTER TABLE " + TABLENAME + " ADD deleted CHAR(1) default '0';");
				}
				// Update-Script für ältere Versionen in der Datenbank
				if (vi.isOlder("1.1.0")) {
					createOrModifyTable("ALTER TABLE " + TABLENAME + " ADD Category VARCHAR(80);");
				}
				version.set("Text", VERSION);
			}
		}
		*/
	}
		
	/**
	 * Aktualisierung des aktuellen Eintrages in der Datenbank
	 * @param nameId: alternateNameId aus GeoNames
	 * @param language: iso 639 language
	 * @param name: Name in der Sprache <language>
	 * @param isPreferredName: '1', if this alternate name is an official/preferred name
	 * @param isShortName: '1', if this is a short name like 'California' for 'State of California'
	 */
	public void updateEntry(String nameId,
							String language,
							String name,
							String isPreferredName,
							String isShortName)	{
		set(new String[]{"Nameid", "Language", "Name", "ispreferredname", "isshortname"},
			new String[]{ nameId,   language,   name,   isPreferredName,   isShortName});
	}
	
	/**
	 * Erstellen eines neuen Eintrages in der Datenbank
	 * @param id: aus GeoNames
	 * @param nameId: alternateNameId aus GeoNames
	 * @param language: iso 639 language
	 * @param name: Name in der Sprache <language>
	 * @param isPreferredName: '1', if this alternate name is an official/preferred name
	 * @param isShortName: '1', if this is a short name like 'California' for 'State of California'
	 */
	public Name_GeoNamesEintrag(String id,
								String nameId,
								String language,
								String name,
								String isPreferredName,
								String isShortName)	{
		create(id);
		set(new String[]{"Nameid", "Language", "Name", "Ispreferredname", "Isshortname"},
			new String[]{ nameId,   language,   name,   isPreferredName,   isShortName});
	}
	
	// TODO
	public String toString() {
		return super.toString();
	}

	// TODO
	@Override
	public String getLabel() {
		String[] f = new String[9];
		get(new String[]{"Bezeichnung", "WikiLink", "Iso3166Alpha2", "Iso3166Alpha3", "Iso3166Numeric", "TopLevelDomain", "OlympicCountryCodeIOC", "Iso3166_2", "Language"},f);
		StringBuilder ret=new StringBuilder();
		ret.append(f[0]).append(" ").append(f[1]).append(" ").append(f[2]);
		return ret.toString();
	}

	@Override
	protected String getTableName() {
		return TABLENAME;
	}
}