/*******************************************************************************
 * Copyright (c) 2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *    $Id: Vaccination.java 88 2010-05-13 15:27:47Z gerry.weirich $
 *******************************************************************************/

package ch.elexis.impfplan.model;

import ch.elexis.ElexisException;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.scala.runtime.Tuple;
import ch.rgw.tools.TimeTool;

public class Vaccination extends PersistentObject {
	
	public static final String OBSERVATIONS = "observations";
	public static final String DATE = "date";
	public static final String VACCINATION_TYPE = "vaccinationType";
	public static final String PATIENT_ID = "patientID";
	private static final String TABLENAME="CH_ELEXIS_IMPFPLAN_VACCINATIONS";
	private static final String VERSION="0.1.0";
	private static final String createDB=
		"CREATE TABLE "+TABLENAME+" ("+
		"ID VARCHAR(25) primary key, deleted CHAR(1) default '0', lastupdate bigint," +
		"patientID VARCHAR(25), vaccinationType VARCHAR(25), date CHAR(8), observations TEXT);"+
		"CREATE INDEX "+TABLENAME+"_IDX1 on "+TABLENAME+" (patientID);"+
		"INSERT INTO "+TABLENAME+"(ID,observations) VALUES('VERSION','"+VERSION+"');";
	
	static{
		addMapping(TABLENAME, PATIENT_ID,VACCINATION_TYPE,DATE,OBSERVATIONS);
		// getConnection().exec("DROP TABLE "+TABLENAME);
		Vaccination ver=load("VERSION");
		if (!ver.exists()) {
			createOrModifyTable(createDB);
		}
	}

	public Vaccination(VaccinationType vt, Patient pat, TimeTool date, boolean bUnexact){
		create(null);
		String dat=date.toString(TimeTool.DATE_COMPACT);
		if(bUnexact){
			dat=dat.substring(0,4)+"0000";
		}
		set(new String[]{VACCINATION_TYPE,PATIENT_ID,DATE},vt.getId(),pat.getId(),dat);	
	}
	public Vaccination(VaccinationType vt, Patient pat){
		this(vt,pat, new TimeTool(),false);
	}
	
	@Override
	public String getLabel() {
		Patient pat=Patient.load(get(PATIENT_ID));
		VaccinationType type=VaccinationType.load(get(VACCINATION_TYPE));
		return new StringBuilder().append(pat.getLabel()).append(" : ").append(type.getLabel()).toString();
	}

	@Override
	protected String getTableName() {
		return TABLENAME;
	}
	
	public VaccinationType getVaccinationType(){
		VaccinationType vt=VaccinationType.load(get(VACCINATION_TYPE));
		if(vt.exists()){
			return vt;
		}
		return null;
	}
	
	public String getDateAsString(){
		String dat = get(Vaccination.DATE);
		if (dat.endsWith("0000")) {
			return "( ~" + dat.substring(0, 4) + ")";
		} else {
			return getDate().toString(TimeTool.DATE_GER);
		}
	}
	
	public TimeTool getDate(){
		String dRaw=get(DATE);
		if(dRaw.endsWith("0000")){
			dRaw=dRaw.substring(0, 4)+"0101";
		}
		return new TimeTool(dRaw);
	}

	public void setDate(TimeTool date, boolean bIsUnexact){
		String val=date.toString(TimeTool.DATE_COMPACT);
		if(bIsUnexact){
			val=val.substring(0,4)+"0000";
		}
		set(DATE,val);
	}
	public static Vaccination load(String id){
		return new Vaccination(id);
	}
	
	protected Vaccination(String id){
		super(id);
	}
	
	protected Vaccination(){}
	
}
