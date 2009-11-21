package ch.elexis.archie.patientstatistik;

import org.eclipse.swt.widgets.Composite;

import ch.elexis.data.Patient;
import ch.unibe.iam.scg.archie.model.RegexValidation;
import ch.unibe.iam.scg.archie.ui.widgets.AbstractWidget;

public class PatientSelector extends AbstractWidget {
	public Patient patSelected;
	
	public PatientSelector(Composite parent, int style, String labelText, RegexValidation regex){
		super(parent,style,labelText,regex);
	}
	@Override
	public Object getValue(){
		return patSelected;
	}
	
	@Override
	public boolean isValid(){
		return true;
	}
	
	@Override
	public void setDescription(String description){
		
	}
	
	@Override
	public void setValue(Object value){
		patSelected=(Patient)value;
	}
	
}
