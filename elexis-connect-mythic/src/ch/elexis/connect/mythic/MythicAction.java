/*******************************************************************************
 * Copyright (c) 2007-2010, G. Weirich
 * All rights reserved.
 * $Id: MythicAction.java 410 2007-12-20 21:10:15Z Gerry $
 *******************************************************************************/
package ch.elexis.connect.mythic;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.elexis.data.Labor;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.dialogs.KontaktSelektor;
import ch.elexis.rs232.Connection;
import ch.elexis.rs232.Connection.ComPortListener;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class MythicAction extends Action implements ComPortListener {
	
	private static final int MODE_AWAIT_START = 0;
	private static final int MODE_AWAIT_LINES = 1;
	private final int mode = 0;
	
	Connection ctrl =
		new Connection("Elexis-Mythic", Hub.localCfg.get(Preferences.PORT, "COM1"), Hub.localCfg
			.get(Preferences.PARAMS, "9600,8,n,1"), this);
	Labor myLab;
	Patient actPatient;
	
	public MythicAction(){
		super("Mythic", AS_CHECK_BOX);
		setToolTipText("Daten von Mythic einlesen");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
			"ch.elexis.connect.mythic", "icons/mythic.ico")); //$NON-NLS-1$
		Query<Labor> qbe = new Query<Labor>(Labor.class);
		qbe.add("Name", "LIKE", "%Mythic%");
		List<Labor> list = qbe.execute();
		if (list.size() != 1) {
			myLab = new Labor("Mythic", "Eigenlabor: Mythic");
		} else {
			myLab = list.get(0);
		}
	}
	
	@Override
	public void run(){
		if (isChecked()) {
			KontaktSelektor ksl =
				new KontaktSelektor(Hub.getActiveShell(), Patient.class, "Patient auswählen",
				"Wem soll der Mythic-Befund zugeordnet werden?",Patient.DEFAULT_SORT);
			ksl.create();
			ksl.getShell().setText("Mythic Patientenauswahl");
			if (ksl.open() == org.eclipse.jface.dialogs.Dialog.OK) {
				actPatient = (Patient) ksl.getSelection();
				if (ctrl.connect()) {
					ctrl.readLine((byte) 13, 600);
					return;
				} else {
					SWTHelper.showError("Fehler mit Port", "Konnte seriellen Port nicht öffnen");
				}
			}
		} else {
			if (ctrl.isOpen()) {
				actPatient = null;
				ctrl.sendBreak();
				ctrl.close();
			}
		}
		setChecked(false);
	}
	
	public void gotBreak(final Connection connection){
		actPatient = null;
		connection.close();
		SWTHelper.showError("Mythic", "Datenübertragung wurde unterbrochen");
		
	}
	
	public void gotChunk(final Connection connection, final String data){
		
		// System.out.println(data+"\n");
		if (actPatient != null) {
			if (data.startsWith("END_RESULT")) {
				actPatient = null;
				ctrl.close(); // That's it!
				setChecked(false); // Pop out Mythic-Button
				ElexisEventDispatcher.reload(LabItem.class); // and tell everybody, we're finished
			} else {
				fetchResult(data);
			}
			
		}
		
	}
	
	private LabResult fetchResult(final String data){
		String[] line = data.split(";");
		int idx = StringTool.getIndex(results, line[0]);
		if (idx != -1) {
			if (line.length > 7) {
				LabItem li;
				String liid = new Query<LabItem>(LabItem.class).findSingle("kuerzel", "=", line[0]);
				if (liid == null) {
					String ref = line[5] + "-" + line[6];
					li =
						new LabItem(line[0], line[0], myLab, ref, ref, units[idx],
							LabItem.typ.NUMERIC, "MTH Mythic", "50");
				} else {
					li = LabItem.load(liid);
				}
				String comment = "";
				if ((line[2].length() > 0) || (line[3].length() > 0)) {
					comment = line[2] + ";" + line[3];
				}
				LabResult lr = new LabResult(actPatient, new TimeTool(), li, line[1], comment);
				lr.set("Quelle", "Mythic");
				return lr;
			}
		}
		return null;
	}
	
	public void timeout(){
		ctrl.close();
		SWTHelper.showError("Mythic", "Das Gerät antwortet nicht");
		setChecked(false);
	}
	
	String[] results =
	{
		"WBC", "RBC", "HGB", "HCT", "MCV", "MCH", "MCHC", "RDW", "PLT", "MPV", "THT", "PDW",
		"LYM%", "MON%", "GRA%", "LYM", "MON", "GRA"
	};
	String[] units =
	{
		"G/l", "G/l", "g/dl", "%", "fl", "pg", "g/dl", "%", "G/l", "fl", "%", "%", "%", "%",
		"%", "G/l", "G/l", "G/l"
	};
}
