package ch.marlovits.plz;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;

/**
 * Einstellungen für PLZ (neu). Bis jetzt mal ein bisschen Testerei...
 * 
 * @author Harry
 * 
 */
public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String lHarryPLZ_String = "Mein String";
	
	@Override
	protected void createFieldEditors(){
		Combo cbBereich = new Combo(getFieldEditorParent(), SWT.READ_ONLY | SWT.SINGLE);
		
		String[][] entryNamesAndValues = {
			{
				"Name1", "Name2"
			}, {
				"Value1", "Value2"
			}
		};
		addField(new ComboFieldEditor("description1", "description2", entryNamesAndValues,
			getFieldEditorParent()));
		
		String prefsString = Messages.getString("marlovits-plz.prefs.labelLanguage");
		addField(new DirectoryFieldEditor(lHarryPLZ_String, prefsString, getFieldEditorParent()));
		
		// addField(invalidFieldEditor);
		
		addField(new BooleanFieldEditor("theNameBool", "theLabelBool", 1, getFieldEditorParent()));
		addField(new ColorFieldEditor("theNameColor", "theLabelColor", getFieldEditorParent()));
		FontFieldEditor ffe =
			new FontFieldEditor("theNameFont", "theLabelFont", "thePreviewText",
				getFieldEditorParent());
		ffe.setLabelText("Der Label-Text via setLabelText");
		ffe.setChangeButtonText("Changed Button Text");
		addField(ffe);
		ffe.setFocus();
		// MyListEditor lMyListEditor = new MyListEditor("theNameListEditor", "theLabelListEditor",
		// "333333", "999999999", getFieldEditorParent());
		// addField(lMyListEditor);
		IntegerFieldEditor lIntegerFieldEditor =
			new IntegerFieldEditor("theNameIntegerFieldEditor", "theLabelIntegerFieldEditor",
				getFieldEditorParent());
		addField(lIntegerFieldEditor);
		StringFieldEditor lStringFieldEditor =
			new StringFieldEditor("theNameStringFieldEditor", "theNameStringFieldEditor",
				getFieldEditorParent());
		addField(lStringFieldEditor);
		RadioGroupFieldEditor lRadioGroupFieldEditor =
			new RadioGroupFieldEditor("GeneralPage.DoubleClick", "resName", 1, new String[][] {
				{
					"Open Browser", "open"
				}, {
					"Expand Tree", "expand"
				}
			}, getFieldEditorParent());
		addField(lRadioGroupFieldEditor);
		FileFieldEditor lFileFieldEditor =
			new FileFieldEditor("theNameFileFieldEditor", "theLabelFileFieldEditor",
				getFieldEditorParent());
		addField(lFileFieldEditor);
		PathEditor lPathEditor =
			new PathEditor("theNamePathEditorEditor", "theLabelPathEditorEditor", "description",
				getFieldEditorParent());
		addField(lPathEditor);
	}
	
	public void init(IWorkbench workbench){
	// TODO Auto-generated method stub
	
	}
	
	@Override
	protected void performApply(){
		super.performApply();
		Hub.localCfg.flush();
	}
	
}
