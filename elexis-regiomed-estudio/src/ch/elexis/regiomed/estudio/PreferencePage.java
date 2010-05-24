package ch.elexis.regiomed.estudio;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;

public class PreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public static final String INI_PATH ="regiomed/ini_path";
	public static final String PROP_PATH="regiomed/prop_path";

	public PreferencePage(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
	}
	@Override
	protected void createFieldEditors() {
		addField(new FileFieldEditor(INI_PATH, "Ort der patient.ini", getFieldEditorParent()));
		addField(new FileFieldEditor(PROP_PATH,"Ort der Rose-Properties", getFieldEditorParent()));
	}

	public void init(final IWorkbench workbench) {
//		TODO Auto-generated method stub
	}}
