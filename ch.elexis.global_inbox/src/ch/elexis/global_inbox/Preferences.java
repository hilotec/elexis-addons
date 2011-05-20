package ch.elexis.global_inbox;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String PREFERENCE_BRANCH = "plugins/global_inbox/"; //$NON-NLS-1$
	public static final String PREF_DIR = PREFERENCE_BRANCH + "dir"; //$NON-NLS-1$
	
	public Preferences(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new DirectoryFieldEditor(PREF_DIR, Messages.Preferences_directory,
			getFieldEditorParent()));
	}
	
	@Override
	public void init(IWorkbench workbench){

	}
	
	@Override
	protected void performApply(){
		super.performApply();
		Hub.localCfg.flush();
	}
	
}
