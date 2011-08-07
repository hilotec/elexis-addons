package ch.elexis.archie.wzw;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public class AlleLeistungen extends BaseStats {
	static final String NAME="Alle Leistungen";
	static final String DESC="Listet sämtliche Leistungen im gegebenen Zeitraum";
	static final String[] HEADINGS={"Datum","Codesystem","Code","Preis", "PatientNr"};
	
	public AlleLeistungen() {
		super(NAME,DESC,HEADINGS);
	}

	
	@Override
	protected IStatus createContent(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

}
