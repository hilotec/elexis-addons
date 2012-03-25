package ch.elexis.archie.wzw;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import ch.elexis.archie.wzw.AlleLeistungen.TarifStat;
import ch.elexis.data.Fall;
import ch.elexis.data.IVerrechenbar;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.Verrechnet;
import ch.rgw.tools.Money;

public class AlleLeistungenRoh extends BaseStats {
	static final String NAME = "Alle Leistungen roh";
	static final String DESC = "Listet sämtliche Leistungen im gegebenen Zeitraum";
	static final String[] HEADINGS = { "Patient-ID", "Patient-Name", "Patient Geschlecht", "Patient Alter", "Datum",
			"Codesystem", "Code", "Text", "Anzahl", "Umsatz" };

	public AlleLeistungenRoh() {
		super(NAME, DESC, HEADINGS);
	}

	List<Comparable<?>[]> lines = new ArrayList<Comparable<?>[]>(10000);

	@Override
	protected IStatus createContent(IProgressMonitor monitor) {
		List<Konsultation> conses = getConses(monitor);
		int clicksPerRound = HUGE_NUMBER / conses.size();
		for (Konsultation k : conses) {
			if (!k.isDeleted()) {
				Fall fall = k.getFall();
				if (fall != null) {
					Patient pat = fall.getPatient();
					if (pat != null) {
						for (Verrechnet v : k.getLeistungen()) {
							IVerrechenbar vv = v.getVerrechenbar();
							if (vv != null) {
								String[] line = new String[] {
										pat.getPatCode(), pat.getLabel(false),pat.getGeschlecht(), pat.getAlter(),
										k.getDatum(), vv.getCodeSystemName(),
										vv.getCode(), vv.getText(),
										Integer.toString(v.getZahl()), v.getNettoPreis().getAmountAsString() };
								lines.add(line);
							} else {
								System.out.println(v.getLabel());
							}
						}
					}
				}
			}
			monitor.worked(clicksPerRound);
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

		}
		// Und an Archie übermitteln
		this.dataSet.setContent(lines);
		return Status.OK_STATUS;

	}

}
