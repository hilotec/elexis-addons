/*******************************************************************************
 * Copyright (c) 2007, medshare and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    M. Imhof - initial implementation
 *    G. Weirich - added toHashmap
 *    
 * $Id$
 *******************************************************************************/

package ch.marlovits.addressSearch.directories;

import java.util.HashMap;

import ch.elexis.data.Patient;

public class KontaktEntry {
	private final String vorname;
	private final String name;
	private final String zusatz;
	private final String adresse;
	private final String plz;
	private final String ort;
	private final String tel;
	private final String fax;
	private final String email;
	private final boolean isDetail; // List Kontakt oder Detail Kontakt
	// +++++ START
	private boolean isVCardDetail;
	private String detailLink; // link to vCard in tel.search.ch
	private String website;
	private String tel2;
	private String mobile;
	private String ledigname;
	private String profession;
	private String category;
	private boolean isOrganisation;
	private String title;
	private String country; // iso3
	
	// +++++ END
	
	public KontaktEntry(final String vorname, final String name, final String zusatz,
		final String adresse, final String plz, final String ort, final String tel, String fax,
		String email, boolean isDetail){
		super();
		this.vorname = vorname;
		this.name = name;
		this.zusatz = zusatz;
		this.adresse = adresse;
		this.plz = plz;
		this.ort = ort;
		this.tel = tel;
		this.fax = fax;
		this.email = email;
		this.isDetail = isDetail;
		
		// +++++
		isVCardDetail = false;
	}
	
	// +++++ START new constructor with additional fields
	public KontaktEntry(final String vorname, final String name, final String zusatz,
		final String adresse, final String plz, final String ort, final String tel, String fax,
		String email, boolean isDetail, final boolean isVCardDetail, final String detailLink,
		final String website, final String tel2, final String mobile, final String ledigname,
		final String profession, final String category, boolean isOrganisation, final String title,
		final String country){
		super();
		this.vorname = vorname;
		this.name = name;
		this.zusatz = zusatz;
		this.adresse = adresse;
		this.plz = plz;
		this.ort = ort;
		this.tel = tel;
		this.fax = fax;
		this.email = email;
		this.isDetail = isDetail;
		
		this.isVCardDetail = isVCardDetail;
		this.detailLink = detailLink;
		this.website = website;
		this.tel2 = tel2;
		this.mobile = mobile;
		this.ledigname = ledigname;
		this.profession = profession;
		this.category = category;
		this.isOrganisation = isOrganisation;
		this.title = title;
		this.country = country;
	}
	
	// +++++ END new constructor with additional fields
	
	/**
	 * Fill all fields into a hashmap
	 * 
	 * @return a hashmap with all non-empty fields with standard names
	 * @author gerry
	 */
	public HashMap<String, String> toHashmap(){
		HashMap<String, String> ret = new HashMap<String, String>();
		if (countValue(name) > 0) {
			ret.put(Patient.FLD_NAME, name);
		}
		if (countValue(vorname) > 0) {
			ret.put(Patient.FLD_FIRSTNAME, vorname);
		}
		if (countValue(adresse) > 0) {
			ret.put(Patient.FLD_STREET, adresse);
		}
		if (countValue(plz) > 0) {
			ret.put(Patient.FLD_ZIP, plz);
		}
		if (countValue(ort) > 0) {
			ret.put(Patient.FLD_PLACE, ort);
		}
		if (countValue(tel) > 0) {
			ret.put(Patient.FLD_PHONE1, tel);
		}
		if (countValue(fax) > 0) {
			ret.put(Patient.FLD_FAX, fax);
		}
		// ++++ START
		// if(countValue(detailLink)>0){
		// ret.put(Patient.FLD_WEBSITE, detailLink);
		// }
		if (countValue(website) > 0) {
			ret.put(Patient.FLD_WEBSITE, website);
		}
		if (countValue(tel2) > 0) {
			ret.put(Patient.FLD_PHONE2, tel2);
		}
		if (countValue(mobile) > 0) {
			ret.put(Patient.FLD_MOBILEPHONE, mobile);
		}
		// if(countValue(ledigname)>0){
		// ret.put(Patient.FLD_EXTINFO, ledigname);
		// }
		// if(countValue(profession)>0){
		// ret.put(Patient.FLD_EXTINFO, profession);
		// }
		// if(countValue(category)>0){
		// ret.put(Patient.FLD_EXTINFO, category);
		// }
		if (countValue(country) > 0) {
			ret.put(Patient.FLD_COUNTRY, country);
		}
		// ++++ END
		return ret;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getVorname(){
		return this.vorname;
	}
	
	public String getZusatz(){
		return this.zusatz;
	}
	
	public String getAdresse(){
		return this.adresse;
	}
	
	public String getPlz(){
		return this.plz;
	}
	
	public String getOrt(){
		return this.ort;
	}
	
	public String getTelefon(){
		return this.tel;
	}
	
	public String getFax(){
		return fax;
	}
	
	public String getEmail(){
		return email;
	}
	
	public boolean isDetail(){
		return this.isDetail;
	}
	
	// ++++ START
	public boolean getIsVCardDetail(){
		return this.isVCardDetail;
	}
	
	public String getDetailLink(){
		return this.detailLink;
	}
	
	public String getWebsite(){
		return this.website;
	}
	
	public String getTelefon2(){
		return this.tel2;
	}
	
	public String getMobile(){
		return this.mobile;
	}
	
	public String getLedigname(){
		return this.ledigname;
	}
	
	public String getProfession(){
		return this.profession;
	}
	
	public String getCategory(){
		return this.category;
	}
	
	public boolean getIsOrganisation(){
		return this.isOrganisation;
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public String getCountry(){
		return this.country;
	}
	
	// ++++ END
	
	private int countValue(String value){
		if (value != null && value.length() > 0) {
			return 1;
		}
		return 0;
	}
	
	public int countNotEmptyFields(){
		return countValue(getVorname()) + countValue(getName()) + countValue(getZusatz())
			+ countValue(getAdresse()) + countValue(getPlz()) + countValue(getOrt())
			+ countValue(getTelefon()) + countValue(getFax()) + countValue(getEmail())

			+ countValue(getWebsite()) + countValue(getTelefon2()) + countValue(getMobile())
			+ countValue(getLedigname()) + countValue(getProfession()) + countValue(getCategory())
			+ countValue(getTitle()) + countValue(getCountry());
	}
	
	public String toString(){
		return getName() + ", " + getZusatz() + ", " + getAdresse() + ", " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			+ getPlz() + " " + getOrt() + " " + getTelefon() //$NON-NLS-1$ //$NON-NLS-2$
			// +++++ new:
			+ " " + getWebsite() + " " //$NON-NLS-1$
			+ getTelefon2() + " " //$NON-NLS-1$
			+ getMobile() + " " //$NON-NLS-1$
			+ getLedigname() + " " //$NON-NLS-1$
			+ getProfession() + " " //$NON-NLS-1$
			+ getCategory() + " " //$NON-NLS-1$
			+ getTitle() + " " //$NON-NLS-1$
			+ getCountry() + " " //$NON-NLS-1$
		;
	}
}
