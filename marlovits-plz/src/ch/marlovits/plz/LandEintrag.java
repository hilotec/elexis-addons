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
 * $Id: LandEintrag.java
 *******************************************************************************/

package ch.marlovits.plz;

import ch.elexis.data.PersistentObject;

public class LandEintrag extends PersistentObject {
	// Tabellenname in der Datenbank
	private static final String TABLENAME = "CH_MARLOVITS_LAND";
	// die Version der Tabelle
	public static final String VERSION = "2.0";
	// CreateScript für die Tabelle
	private static final String createDB =
		"CREATE TABLE " + TABLENAME + " (" +
			"id				character varying(25) NOT NULL,		" +
			// ISO-Land-Merkmale, link zu Info in Wikipedia
			"landname			character varying,				" +
			"landiso2			character varying(2),			" +
			"landiso3			character varying(3),			" +
			"landisonum			character varying(3),			" +
			"landtld			character varying(3),			" +
			"landioc			character varying(3),			" +
			"landiso3166_2		character varying(2),			" +
			"landwikilink		character varying,				" +
			// dieser Eintrag gilt für diese Sprache
			"landlanguage		character varying(2),			" +
			// Sortierung vor Namenssortierung
			"landsorting		integer default 9999,			" +
			// Gültigkeitsprüfungen für dieses Land
			"plzregex			character varying(100),			" +
			"plzregexmessage	character varying,				" +
			"strasseerlaubt		integer,						" +
			"kantonauswaehlen	integer,						" +
			// anderes Standardzeugs
			"deleted			CHAR(1) default '0',			" +
			"lastupdate			bigint,							" +
			// Primary key erstellen
			"CONSTRAINT	ch_marlovits_land_pkey PRIMARY KEY (id)	" +
			")													" +
		"WITH (OIDS=FALSE);										" +
		// den owner für die Tabelle setzen
		"ALTER TABLE " + TABLENAME + " OWNER TO elexisuser;		" +
		// die Indices erstellen
		"CREATE INDEX ch_marlovits_land0 ON CH_MARLOVITS_LAND USING btree (landname);	" +
		"CREATE INDEX ch_marlovits_land1 ON CH_MARLOVITS_LAND USING btree (landiso2);	" +
		"CREATE INDEX ch_marlovits_land2 ON CH_MARLOVITS_LAND USING btree (landiso3);	" +
		"CREATE INDEX ch_marlovits_land3 ON CH_MARLOVITS_LAND USING btree (landisonum);	";
	
	static	{
		// ziemlich einfaches Mapping - einheitlich einfach der erste Buchstaben der Feldbezeichnung in der Datenbank als Capital
		addMapping(	TABLENAME,
					"Landname=landname",
					"Landiso2=landiso2",
					"Landiso3=landiso3",
					"Landisonum=landisonum",
					"Landtld=landtld",
					"Landioc=landioc",
					"Landiso3166_2=landiso3166_2",
					"Landwikilink=landwikilink",
					"Landlanguage=landlanguage",
					"Landsorting=landsorting",
					"Plzregex=plzregex",
					"Plzregexmessage=plzregexmessage",
					"Strasseerlaubt=strasseerlaubt",
					"Kantonauswaehlen=kantonauswaehlen",
					"Deleted=deleted",
					"lastupdate=lastupdate");
		
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
	
	// Erstellen eines Eintrages in der Datenbank
	public LandEintrag( String landname,
						String landiso2,
						String landiso3,
						String landisonum,
						String landtld,
						String landioc,
						String landiso3166_2,
						String landwikilink,
						String landlanguage) {
		create(null);		
		set(new String[]{"Landname", "Landiso2", "Landiso3", "Landisonum", "Landtld", "Landioc", "Landiso3166_2", "Landwikilink", "Landlanguage"},
			new String[]{landname,   landiso2,   landiso3,   landisonum,   landtld,   landioc,   landiso3166_2,   landwikilink,   landlanguage});
	}

	public String toString() {
/*		return getName() + ", " + getZusatz() + ", " + getAdresse() + ", " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ getPlz() + " " + getOrt() + " " + getTelefon(); //$NON-NLS-1$ //$NON-NLS-2$
	*/
		System.out.print("toString() in LandIsoEntry not implemented");
		return "toString not implemented";
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
	
	public static LandEintrag load(String id){
		//return new LandEintrag(id, "", "", "", "", "", "", "", "");
		return new LandEintrag( "dummylandname", "", "", "", "", "", "", "", "");
	}
	
	/**
	 * Name des Landes zurückgeben
	 */
	public String getLandName(){
		return get("Landname");
	}
}
