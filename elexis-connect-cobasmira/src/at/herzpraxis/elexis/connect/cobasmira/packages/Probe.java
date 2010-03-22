package at.herzpraxis.elexis.connect.cobasmira.packages;

import java.util.ResourceBundle;

import ch.elexis.data.Patient;
import ch.rgw.tools.TimeTool;

/**
 * @author Marco Descher / Herzpraxis Dr. Wolber, Goetzis, Austria
 * Structure of a single probe line, as delivered and pre-split, this is
 * called by CobasMiraAction:gotData
 * 20 w tt nnnn ti scno iiiiiiiiii resultttttt dp uu t f rrLF
 * (00) 20
 * (01) w: Worklist Type (one ascii char)
 * (02) tt: Test Number (A1..Z1..A6..Z6)
 * (03) nnnn: Test Name (String)
 * (04) ti: Test Result Index (Integer number 00..99)
 * (05) scno: Sample Cup Number (Integer number 0000..9999)
 * (06) ii..: Patient Identification (String)
 * (07) result: Concentration (Floating Point Number)
 * (08) dp: no of digits behind decimal point (Integer number 00..99)
 * (09) uu: Unit code (Integer number 00..99)
 * (10) t: Result type (one ascii char)
 * (11) f: Flag (one ascii char)
 * (12) rr: Remark (Integer number 00..99)
 */
public class Probe {
	public static final String BUNDLE_NAME = "at.herzpraxis.elexis.connect.cobasmira.packages.valuetexts";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);


	private TimeTool date;			// Measurement Date
	private String testKuerzel;		// Short name of the measurement (mapping of machine short name to elexis labitem short name in valuetexts.properties)
	private String ident;			// The identification of the patient, in DB kontakt.patientnr
	private float result; 			// The result for test testKuerzel as delievered by the CobasMira Device
	private int unit;				// The unit the result is measured in

	public Probe(final String strArray) {
		parse(strArray);
	}

	public String getTestKuerzel() {
		return testKuerzel;
	}

	private void parse(String strArray) {
		String[] measurementValue = strArray.split("[\\s]+");
		ident = measurementValue[6].trim();
		testKuerzel = measurementValue[3].trim();				// Referenced in laboritems.kuerzel?
		result = Float.valueOf(measurementValue[7].trim());
		date = new TimeTool();
		unit = Integer.valueOf(measurementValue[9]);

	}
	
	/**
	 * Schreibt Labordaten.
	 * 
	 * @param patient
	 */
	public void write(Patient patient) throws Exception {
		Value val = Value.getValue(testKuerzel, unit); // Fetch basic info about the test: shortName, longName, unit, refM, refF
		result = roundToDecimals(result, Integer.parseInt(RESOURCE_BUNDLE.getString(testKuerzel+".noDecPlaces")));
		val.fetchValue(patient, result, date); //$NON-NLS-1$
	}
	
	/**
	 * Round a given float value to a selected number of comma places
	 * 
	 * @param d the value to be rounded
	 * @param c the number of decimal places to round to
	 */
	public static float roundToDecimals(float d, int c) {
		//TODO(marco): If c==0 write an integer into the labresult, not a float because float.toString() results in x.y
		int temp=(int)((d*Math.pow(10,c)));
		return (float) ((temp)/Math.pow(10,c));
		}

	public TimeTool getDate() {
		return date;
	}

	public String getIdent() {
		return ident;
	}

	public float getResult() {
		return result;
	}

	public int getUnit() {
		return unit;
	}
}
