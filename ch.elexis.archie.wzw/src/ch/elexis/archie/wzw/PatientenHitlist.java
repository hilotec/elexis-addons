package ch.elexis.archie.wzw;

import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import ch.elexis.artikel_ch.data.Medical;
import ch.elexis.artikel_ch.data.Medikament;
import ch.elexis.artikel_ch.data.MiGelArtikel;
import ch.elexis.data.Fall;
import ch.elexis.data.IVerrechenbar;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Mandant;
import ch.elexis.data.Patient;
import ch.elexis.data.PhysioLeistung;
import ch.elexis.data.TarmedLeistung;
import ch.elexis.data.Verrechnet;
import ch.elexis.labortarif2009.data.Labor2009Tarif;
import ch.elexis.medikamente.bag.data.BAGMedi;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.TimeTool;

public class PatientenHitlist extends BaseStats {
	static final String NAME = "Patienten-Hitliste";
	static final String DESC = "Listet Patienten nach Kosten";
	static final String[] HEADINGS = { "PatientNr", "Tarmed", "Medicals",
			"Medikamente", "Physio", "Andere" };

	public PatientenHitlist() {
		super(NAME, DESC, HEADINGS);
	}

	@Override
	protected IStatus createContent(IProgressMonitor monitor) {
		try {
			HashMap<String, PatientStat> pstat = new HashMap<String, PatientenHitlist.PatientStat>();
			for (Konsultation k : getConses()) {
				TimeTool kdate = new TimeTool(k.getDatum());
				Fall fall = k.getFall();
				if (fall != null) {

					Patient pat = fall.getPatient();
					if (pat != null) {
						PatientStat ps = pstat.get(pat.getId());
						if (ps == null) {
							ps = new PatientStat(pat);
							pstat.put(pat.getId(), ps);
						}
						ps.numCons++;
						List<Verrechnet> vr = k.getLeistungen();
						Mandant m = k.getMandant();
						if (m != null) {
							for (Verrechnet v : vr) {
								IVerrechenbar vv = v.getVerrechenbar();
								if (vv != null) {
									if (vv instanceof TarmedLeistung) {
										TarmedLeistung tl = (TarmedLeistung) vv;
										ps.costTarmedAL += tl.getAL()
												* tl.getFactor(kdate, fall);
										ps.costTarmedTL += tl.getTL()
												* tl.getFactor(kdate, fall);
									} else if (vv instanceof PhysioLeistung) {
										ps.costPhysio+=v.getNettoPreis().doubleValue();
									} else if (vv instanceof Medical
											|| vv instanceof MiGelArtikel) {
										ps.costMedical+=v.getNettoPreis().doubleValue();
									} else if (vv instanceof Medikament
											|| vv instanceof BAGMedi) {
										ps.costMedikamente+=v.getNettoPreis().doubleValue();
									} else if (vv instanceof Labor2009Tarif) {
										ps.costLabor+=v.getNettoPreis().doubleValue();
									}else{
										ps.costOther+=v.getNettoPreis().doubleValue();
									}
								}
							}
						}
					}
				}
			}
			return Status.OK_STATUS;
		} catch (Throwable t) {
			ExHandler.handle(t);
			return new Status(Status.ERROR, "ch.elexis.archie.wzw",
					t.getMessage());
		}

	}

	static class PatientStat {
		PatientStat(Patient pat) {
			PatientID = pat.getId();
			birthDate = new TimeTool(pat.get(Patient.FLD_DOB));
			sex = pat.getGeschlecht();
			numCons = 0;
			costTarmedAL = 0.0;
			costTarmedTL = 0.0;
			costMedical = 0.0;
			costMedikamente = 0.0;
			costPhysio = 0.0;
			costLabor=0.0;
			costOther = 0.0;
			costTotal = 0.0;
		}

		String PatientID;
		TimeTool birthDate;
		String sex;
		Integer numCons;
		Double costTarmedAL;
		Double costTarmedTL;
		Double costMedical;
		Double costMedikamente;
		Double costPhysio;
		Double costLabor;
		Double costOther;
		Double costTotal;
	}
}
