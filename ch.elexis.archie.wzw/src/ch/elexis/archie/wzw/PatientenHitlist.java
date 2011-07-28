package ch.elexis.archie.wzw;

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import ch.elexis.data.Konsultation;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.TimeTool;

public class PatientenHitlist extends BaseStats {
	static final String NAME="Patienten-Hitliste";
	static final String DESC="Listet Patienten nach Kosten";
	static final String[] HEADINGS={"PatientNr","Tarmed","Medicals","Medikamente","Physio","Andere"};

	public PatientenHitlist() {
		super(NAME,DESC,HEADINGS);
	}
	@Override
	protected IStatus createContent(IProgressMonitor monitor) {
		try{
			HashMap<String, PatientStat> pstat=new HashMap<String, PatientenHitlist.PatientStat>();
			for(Konsultation k:getConses()){
				
			}
			return Status.OK_STATUS;
		}catch(Throwable t){
			ExHandler.handle(t);
			return new Status(Status.ERROR, "ch.elexis.archie.wzw", t.getMessage());
		}
	
	}

	static class PatientStat{
		String PatientID;
		TimeTool birthDate;
		Integer numCons;
		Double costTarmedAL;
		Double costTarmedTL;
		Double costMedical;
		Double costMedikamente;
		Double costPhysio;
		Double costOther;
		Double costTotal;
	}
}
