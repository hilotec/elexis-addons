package ch.elexis.order.medicom;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public static final String CUSTOMER = "orders/medicom/customer";
	public static final String MAIL = "orders/medicom/mail";
	
	public Preferences(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.globalCfg));
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new StringFieldEditor(CUSTOMER, "Kundenidentifikation", getFieldEditorParent()));
		addField(new StringFieldEditor(MAIL, "Mailadresse", getFieldEditorParent()));
	}
	
	@Override
	public void init(IWorkbench workbench){
	// TODO Auto-generated method stub
	
	}
	
	@Override
	protected void performApply(){
		Hub.globalCfg.flush();
		super.performApply();
	}
	
}
