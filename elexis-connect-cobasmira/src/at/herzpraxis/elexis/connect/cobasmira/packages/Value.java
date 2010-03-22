package at.herzpraxis.elexis.connect.cobasmira.packages;

import java.util.List;
import java.util.ResourceBundle;

import at.herzpraxis.elexis.connect.cobasmira.Preferences;
import ch.elexis.Hub;
import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.elexis.data.Labor;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.rgw.tools.TimeTool;

// Creates a new LabItem Entry in the Database (Table laborwerte), to represent the Probe
public class Value {
	public static final String BUNDLE_NAME = "at.herzpraxis.elexis.connect.cobasmira.packages.valuetexts";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private static String getString(String paramName, String key) {
		return RESOURCE_BUNDLE.getString(paramName + "." + key);
	}

	public static Value getValue(final String paramName, final Integer unit) throws Exception {
		return new Value(paramName, unit);
	}

	String _shortName;
	String _longName;
	String _unit;
	LabItem _labItem;
	String _refMann;
	String _refFrau;

	public Value(String paramName, Integer unit) {
		_shortName = getString(paramName, "kuerzel");
		_longName = getString(paramName, "text");
		_unit = RESOURCE_BUNDLE.getString("CobasMira.UnitCode."+unit.toString());
		_refMann = getString(paramName, "refM");
		_refFrau = getString(paramName, "refF");
		_labItem = null;
	}

	public void fetchValue(Patient patient, Float result, TimeTool date) throws Exception {
		if (_labItem == null) {
			fetchLabitem();
		}
		
		//If an identical (date, patientid and labitem) already exists, do a silent overwrite!
		Query<LabResult> labResult = new Query<LabResult>(LabResult.class);
		labResult.add(LabResult.ITEM_ID, "=", _labItem.getId());
		labResult.and();
		labResult.add(LabResult.DATE, "=", date.toString(TimeTool.DATE_GER));
		labResult.and();
		labResult.add(LabResult.PATIENT_ID, "=", patient.getId());
		List<LabResult> labResults = labResult.execute();
		
		if(labResults.size() == 0) {
			LabResult lr = new LabResult(patient, date, _labItem, result.toString(), "");
			lr.set("Quelle", _labItem.getLabor().getKuerzel());
			lr.setFlag(0, true);
		} else {
			LabResult lr = labResults.get(0);
			lr.set(LabResult.RESULT, result.toString());
			lr.set("Quelle", _labItem.getLabor().getKuerzel());
			lr.setFlag(0, true);
		}			
	}
	


	/**
	 * Fetch the respective labitem entry representing this value within the DB.
	 * 
	 * First select our Laboratory using the XID set in Preferences and then the according labitem.
	 */
	private void fetchLabitem() throws Exception {
		String myLabXID = Hub.localCfg.get(Preferences.LABIDENTIFICATION, "").trim();
		Labor myLab = Labor.load(myLabXID);
		if(myLab == null) { throw new Exception("No proper Lab XID configured. Check Preferences."); }
		
		Query<LabItem> qli = new Query<LabItem>(LabItem.class);
		qli.add("kuerzel", "=", _shortName);
		qli.and();
		qli.add("LaborID", "=", myLab.get("ID"));

		List<LabItem> itemList = qli.execute();
		if (itemList.size() < 1) {
			_labItem = new LabItem(_shortName, _longName, myLab, _refMann,
					_refFrau, _unit, LabItem.typ.NUMERIC, "", "50");
		} else {
			_labItem = itemList.get(0);
		}	
	}
	
}
