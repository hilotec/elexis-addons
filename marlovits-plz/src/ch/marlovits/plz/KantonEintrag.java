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
 * $Id: KantonEintrag.java
 *******************************************************************************/

package ch.marlovits.plz;

import ch.elexis.data.PersistentObject;

public class KantonEintrag extends PersistentObject {
	// Tabellenname in der Datenbank
	private static final String TABLENAME = "CH_MARLOVITS_KANTON";
	// die Version der Tabelle
	public static final String VERSION = "2.0";
	// CreateScript für die Tabelle
	private static final String createDB =
		"CREATE TABLE " + TABLENAME + " (" +
			"id				character varying(25) NOT NULL,			" +
			// ISO-Land-Merkmale, link zu Info in Wikipedia
			"kantonname			character varying,					" +
			"kantonfullcode		character varying(10),				" +
			"kantonsubcode		character varying(6),				" +
			"kantonland			character varying(2),				" +
			"kantonindex		CHAR(1),							" +
			"kantonkind			character varying(40),				" +
			"kantonwikilink		character varying,					" +
			// dieser Eintrag gilt für diese Sprache
			"kantonlanguage		character varying(2),				" +
			// anderes Standardzeugs
			"deleted			CHAR(1) default '0',				" +
			"lastupdate			bigint,								" +
			// Primary key erstellen
			"CONSTRAINT	ch_marlovits_kanton_pkey PRIMARY KEY (id)	" +
			")														" +
		"WITH (OIDS=FALSE);											" +
		// den owner für die Tabelle setzen
		"ALTER TABLE " + TABLENAME + " OWNER TO elexisuser;			" +
		// die Indices erstellen
		"CREATE INDEX ch_marlovits_kanton0 ON CH_MARLOVITS_KANTON USING btree (kantonland);		" +
		"CREATE INDEX ch_marlovits_kanton1 ON CH_MARLOVITS_KANTON USING btree (kantonindex);	" +
		"CREATE INDEX ch_marlovits_kanton2 ON CH_MARLOVITS_KANTON USING btree (kantonlanguage);	";
	
	static	{
		// ziemlich einfaches Mapping - einheitlich einfach der erste Buchstaben der Feldbezeichnung in der Datenbank als Capital
		addMapping(	TABLENAME,
					"Kantonname=kantonname",
					"Kantonfullcode=kantonfullcode",
					"Kantonsubcode=kantonsubcode",
					"Kantonland=kantonland",
					"Kantonindex=kantonindex",
					"Kantonkind=kantonkind",
					"Kantonwikilink=kantonwikilink",
					"Kantonlanguage=kantonlanguage",
					"Deleted=deleted",
					"lastupdate=lastupdate");
		
		// Erstellen der Tabelle in der Datenbank
		createOrModifyTable(createDB);
		/*
		KantonEintrag version = KantonEintrag.load("1");
		if (!version.exists()) {
			createOrModifyTable(createDB);
		} else {
			VersionInfo vi = new VersionInfo(version.getKantonName());
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
	public KantonEintrag(String kantonname,
						 String kantonfullcode,
						 String kantonsubcode,
						 String kantonland,
						 String	kantonindex,
						 String kantonkind,
						 String kantonwikilink,
						 String kantonlanguage) {
		create(null);
		set(new String[]{"Kantonname", "Kantonfullcode", "Kantonsubcode", "Kantonland", "Kantonindex", "Kantonkind", "Kantonwikilink", "Kantonlanguage"},
			new String[]{kantonname,   kantonfullcode,   kantonsubcode,   kantonland,   kantonindex,   kantonkind,   kantonwikilink, kantonlanguage});
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
	
	public static KantonEintrag load(String id){
		return new KantonEintrag( "dummylandname", "", "", "", "", "", "", "");
	}
	
	/**
	 * Name des Landes zurückgeben
	 */
	public String getKantonName(){
		return get("Kantonname");
	}
}
