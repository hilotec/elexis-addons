package ch.marlovits.plz;

import java.util.ArrayList;
import java.util.List;

import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.rgw.tools.StringTool;

public class Plz extends PersistentObject {
	
	static {
		addMapping("Plz", "Land", "LandISO3", "Plz", "Ort", "Strasse", "Kanton", "Kantonkuerzel");
	}
	
	public Plz(String Land, String LandIso3, String plz, String Ort, String Strasse, String Kanton,
		String Kantonkuerzel){
		create(null);
		set(new String[] {
			"Land", "LandISO3", "Plz", "Ort", "Strasse", "Kanton", "Kantonkuerzel"
		}, new String[] {
			Land, LandIso3, plz, Ort, Strasse, Kanton, Kantonkuerzel
		});
	}
	
	public static Plz load(String id){
		if (StringTool.isNothing(id)) {
			return null;
		}
		return new Plz(id);
	}
	
	public Plz(String id){
		super(id);
	}
	
	public String getLabel(){
		String[] f = new String[7];
		get(new String[] {
			"Land", "LandIso3", "Plz", "Ort", "Strasse", "Kanton", "Kantonkuerzel"
		}, f);
		StringBuilder ret = new StringBuilder();
		ret.append(f[0]).append(" ").append(f[1]).append(" ").append(f[2]);
		return ret.toString();
	}
	
	protected Plz(){ /* empty */}
	
	@Override
	protected String getTableName(){
		return "Plz";
	}
	
	@Override
	public int getCacheTime(){
		return Integer.MAX_VALUE;
	}
	
	public String getFieldData(String fieldName){
		// return "some data";
		return get(fieldName);
	}
	
	/**
	 * Gibt eine Liste aller angezeigten Postleitzahlen zurück.
	 * 
	 * @param -
	 * @return eine Liste der aktuell angezeigten Postleitzahlen
	 */
	public static List<Plz> getShownPostleitzahlen(){
		List<Plz> plzs = new ArrayList<Plz>();
		
		Query<Plz> query = new Query<Plz>(Plz.class);
		query.insertTrue();
		query.orderBy(false, "Land", "Plz", "Ort");
		List<Plz> plzList = query.execute();
		if (plzList != null) {
			plzs.addAll(plzList);
		}
		return plzs;
	}
	
}
