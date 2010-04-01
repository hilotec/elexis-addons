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
 * $Id: PlzEintrag.java
 *******************************************************************************/

package ch.marlovits.plz;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.geonames.PostalCode;
import org.geonames.Style;
import org.geonames.WebService;

import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.JdbcLink.Stm;

/**
 * Constructor: es muss ENTWEDER die Plz ODER der Ort angegeben werden. Werden beide angegeben,
 * so wird die Plz benutzt und der Ort gesucht.
 * @param landStr iso2 des Landes,	zu welchem die Plz oder der Ort	gesucht werden soll
 * @param plzStr die Postleitzahl,	zu welcher der Ort				gesucht werden soll
 * @param ortStr der Ort,			zu welchem die Plz				gesucht werden soll
 * @author Harry
 */
class PlzSearch	{
	public PlzSearch(String	landStr,
					 String	plzStr,
					 String	ortStr) {
		super();
	}
	
	public static List<PlzEintrag> searchOrtFromPlz(final String	landStr,
													final String	plzStr,
													final boolean   exact,
													final String... orderBy)	{
		return search(landStr, plzStr, null, exact, orderBy);
	}

	public static List<PlzEintrag> searchPlzFromOrt(final String	landStr,
			  										final String	ortStr,
													final boolean   exact,
													final String... orderBy)	{
		return search(landStr, null, ortStr, exact, orderBy);
	}
	
	public static boolean isCountryInDatabase(final String    landStr)	{
		Stm stm = PersistentObject.getConnection().getStatement();
		ResultSet rs = stm.query("select count(*) as cnt from " + PlzEintrag.getTableName2() + " where lower(land) = lower(" + JdbcLink.wrap(landStr) + ")");
		try {
			rs.next();
			long numOfEntries = Integer.decode(rs.getString("cnt"));
			rs.close();
			if (numOfEntries <= 0)	{
				return false;
			} 
		} catch (SQLException e) {
			return false;
		} finally	{
		}
		return true;
	}
	
	public static List<PlzEintrag> search(final String    landStr,
										  final String    plzStr,
										  final String    ortStr,
										  final boolean   exact,
										  final String... orderBy)	{
		// Wenn das Land nicht in der Datenbank vorhanden ist, dann Abfrage auf GeoNames
		Stm stm = PersistentObject.getConnection().getStatement();
		ResultSet rs = stm.query("select count(*) as cnt from " + PlzEintrag.getTableName2() + " where lower(land) = lower(" + JdbcLink.wrap(landStr) + ")");
		try {
			rs.next();
			long numOfEntries = Integer.decode(rs.getString("cnt"));
			rs.close();
			if (numOfEntries <= 0)	{
				// goto geonames
				//SWTHelper.alert("To be done", "Land nicht in der Datenbank");
				List<PlzEintrag> plzList = searchGeoNames(landStr,
														  plzStr,
														  ortStr,
														  exact,
														  orderBy);
				return plzList;
			} 
		} catch (SQLException e) {
			// wie "Land nicht in der Datenbank" -> GeoNames
			// goto GeoNames
			SWTHelper.alert("To be done", "Land nicht in der Datenbank");
			return null;
		}
		
		boolean plzUsed = false;
		// Erstellen einer Query auf Plz und alle Datensätze einlesen, sortieren nach parameter orderBy
		Query<PlzEintrag> query = new Query<PlzEintrag>(PlzEintrag.class);
		//query.insertTrue();
		query.add("Land", "=", landStr, true);
		if (!StringTool.isNothing(plzStr))	{
			query.add("Plz", "=", plzStr, true);
			plzUsed = true;
		}
		if (plzUsed == false){
			if (!StringTool.isNothing(ortStr))	{
				if (exact)	{
					// der Ort kann wildcard */% respektive ?/_ enthalten. dann wird auch "like" automatisch verwendet
					String operator;
					if (ortStr.matches("[%*?_]"))	{
						operator = "like";
					} else	{
						operator = "=";
					}
					query.add("Ort27", operator, ortStr, true);
				} else	{
					query.add("Ort27", "like", ortStr + "%", true);
				}
			}
		}
		// 80 ist nicht adressierbar (interne Postadressen)
		query.add("Plztyp", "!=", "80", true);
		query.orderBy(false, orderBy);
		List<PlzEintrag> plzList = query.execute();
		
		return plzList;
	}

	public static List<PlzEintrag> searchGeoNames(final String    landStr,
												  final String    plzStr,
												  final String    ortStr,
												  final boolean   exact,
												  final String... orderBy)	{
		List<PostalCode> postalCodes;
		//List<PlzEintrag> plzs = new ArrayList<PlzEintrag>();
		
		try {
			WebService.setDefaultStyle(Style.FULL);
			postalCodes = WebService.postalCodeSearch(plzStr, "", landStr);
			//PostalCode[] postalCodeArray = (PostalCode[]) postalCodes.toArray();
			
			List<PlzEintrag> plzs = new ArrayList<PlzEintrag>();
			String resultText = "";
			for (int i = 0; i < postalCodes.size(); i++)	{
				PlzEintrag plz = new PlzEintrag();
				plz.set("Plz",		postalCodes.get(i).getPostalCode());
				plz.set("Ort27",	postalCodes.get(i).getPlaceName());
				plz.set("Land",		postalCodes.get(i).getCountryCode());
				plz.set("Kanton",	postalCodes.get(i).getAdminCode1());
				plz.set("Strasse",	"");
				
				plzs.add(plz);
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}
	
}