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
 * $Id: CountryEintrag.java
 *******************************************************************************/

package ch.marlovits.plz;

import ch.elexis.data.PersistentObject;

public class CountryEintrag extends PersistentObject {
	// Tabellenname in der Datenbank
	private static final String TABLENAME = "CH_MARLOVITS_COUNTRY";
	// die Version der Tabelle
	public static final String VERSION = "2.0";
	// CreateScript für die Tabelle
	private static final String createDB =
		"CREATE TABLE " + TABLENAME + " (" +
		"id					character varying(25) NOT NULL,	" +
		"iso2				character(2),					" + // innerhalb Elexis gebraucht
		"iso3				character(3),					" +
		"isonum				integer,						" +
		"fips				character(2),					" + // US-Variante
		"name				character varying,				" +
		"tld				character(3),					" + // top level domain
		"currencycode		character(3),					" + // evtl. für Umrechnung von Rechnungsbeträgen
		"currencyname		character varying(25),			" +	//
		"phone				character varying(20),			" +	// Vorwahl Land, evtl. für Kontrolle Tel Nr
		"postalcodeformat	character varying(200),			" + // PLZ-Repräsentation: #=Ziffer, @=Buchstabe
		"postalcoderegex	character varying(200),			" +	// PLZ-Kontrolle
		"postalcodemessage	character varying(100),			" +	// Nachricht, die angezeigt wird, wenn die
																// Bedingung postalcoderegex nicht erfüllt ist
																// wenn keine separate Bedingung eingegeben wird,
																// dann wird postalcodeformat mit Kommentar angezeigt
		"languages			character varying(70),			" +	// Sprachen des Landes
		"geonameid			bigint,							" + // für Zugriff auf weitere Daten von GeoNames
		"neigbours			character varying(60),			" +	// Nachbarländer
		"entrylanguage		character(2),					" +	// Sprache dieses Eintrages
		"landsorting		integer DEFAULT 9999,			" +	// Sortierung vor Namenssortierung
		"strasseerlaubt		integer,						" +
		"kantonauswaehlen	integer,						" +
		"deleted			CHAR(1) default '0',			" +
		"lastupdate			bigint,							" +
		"CONSTRAINT	" + TABLENAME + "_pkey PRIMARY KEY (id)	" + // Primary key erstellen
		")													" +
		"WITH (OIDS=FALSE);									" +
		"ALTER TABLE " + TABLENAME + " OWNER TO elexisuser;	" + // den owner für die Tabelle setzen
		// die Indizes erstellen
		"CREATE INDEX ch_marlovits_country0 ON CH_MARLOVITS_COUNTRY USING btree (name);		" +
		"CREATE INDEX ch_marlovits_country1 ON CH_MARLOVITS_COUNTRY USING btree (iso2);		" +
		"CREATE INDEX ch_marlovits_country2 ON CH_MARLOVITS_COUNTRY USING btree (iso3);		" +
		"CREATE INDEX ch_marlovits_country3 ON CH_MARLOVITS_COUNTRY USING btree (isonum);	";
	
	static	{
		// ziemlich einfaches Mapping - einheitlich einfach der erste Buchstaben der Feldbezeichnung in der Datenbank als Capital
		addMapping(	TABLENAME,
					"Iso2=iso2",
					"Iso3=iso3",
					"Isonum=S:N:isonum",
					"Fips=fips",
					"Name=name",
					"Tld=tld",
					"Currencycode=currencycode",
					"Currencyname=currencyname",
					"Phone=phone",
					"Postalcodeformat=postalcodeformat",
					"Postalcoderegex=postalcoderegex",
					"Languages=languages",
					"Geonameid=S:N:geonameid",
					"Neigbours=neigbours",
					"Entrylanguage=entrylanguage",
					"Postalcodemessage=postalcodemessage",
					"Landsorting=S:N:landsorting",
					"Strasseerlaubt=S:N:strasseerlaubt",
					"Kantonauswaehlen=S:N:kantonauswaehlen",
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
	 * Aktualisierung eines Eintrages in der Datenbank
	 * @param iso2, iso3, isoNum, fips, name, tld, currencyCode, currencyName, phone, 
	 * @param postalCodeFormat, postalCodeRegex, languages, geonameId, neigbours, entryLanguage
	 */
	public void updateEntry(String iso2,
							String iso3,
							int    isoNum,
							String fips,
							String name,
							String tld,
							String currencyCode,
							String currencyName,
							String phone,
							String postalCodeFormat,
							String postalCodeRegex,
							String languages,
							int    geonameId,
							String neigbours,
							String entryLanguage)	{
		set(new String[]{"Iso2", "Iso3",   "Isonum", "Fips", "Name", "Tld", "Currencycode", "Currencyname", "Phone", "Postalcodeformat", "Postalcoderegex", "Languages",   "Geonameid", "Neigbours", "Entrylanguage"},
			new String[]{ iso2,   iso3,  ""+isoNum,   fips,   name,   tld,   currencyCode,   currencyName,   phone,   postalCodeFormat,   postalCodeRegex,   languages,  ""+geonameId,   neigbours,   entryLanguage});
	}
	
	/**
	 * Constructor: erstellt einen neuen Eintrag in der Datenbank, lediglich mit der ID. 
	 * Die ID besteht aus dem Iso2-Ländercode und der Sprache, getrennt durch einen Underscore. 
	 * Diese Kombination darf jeweils nur einmal vorkommen. 
	 * Dies ist ein etwas "falscher"/seltsamer Constructor: falls die ID schon in der Datenbank vorhanden 
	 * ist, dann wird dieser CountryEintrag verwendet, falls noch nicht vorhanden, dann wird ein neuer Eintrag in der 
	 * Datenbank erstellt.
	 * @param id: Iso2_Language, zBsp: CH_de
	 */
	public CountryEintrag(final String id){
		super(id);
		if (!exists())	{
			create(id);
		}
	}
	
	public String toString() {
		return super.toString();
	}

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
	
	static String getTableName1()	{
		return TABLENAME;
	}
	/**
	 * Name des Landes zurückgeben
	 */
	public String getCountryName(){
		return get("Name");
	}
}
