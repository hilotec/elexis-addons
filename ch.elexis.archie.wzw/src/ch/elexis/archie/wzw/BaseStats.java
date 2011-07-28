package ch.elexis.archie.wzw;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import ch.elexis.data.Konsultation;
import ch.elexis.data.Query;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.rgw.tools.IFilter;
import ch.rgw.tools.TimeTool;
import ch.unibe.iam.scg.archie.annotations.GetProperty;
import ch.unibe.iam.scg.archie.annotations.SetProperty;
import ch.unibe.iam.scg.archie.model.AbstractTimeSeries;
import ch.unibe.iam.scg.archie.ui.widgets.WidgetTypes;

public abstract class BaseStats extends AbstractTimeSeries {
	protected String desc;
	protected String[] headings;
	private String dateMethod = "Rechnungsdatum";
	private boolean bOnlyActiveMandator;

	public BaseStats(String name, String desc, String[] headings) {
		super(name);
		this.headings = headings;
		this.desc = desc;
	}

	@GetProperty(name = "Nur aktueller Mandant", widgetType = WidgetTypes.BUTTON_CHECKBOX, index = 1)
	public boolean getOnlyActiveMandator() {
		return bOnlyActiveMandator;
	}

	@SetProperty(name = "Nur aktueller Mandant", index = 1)
	public void setOnlyActiveMandator(boolean val) {
		bOnlyActiveMandator = val;
	}

	@GetProperty(widgetType = WidgetTypes.COMBO, description = "Interpretation des Datums", index = 1, items = {
			"Rechnungsdatum", "Zahlungsdatum", "Konsultationsdatum" }, name = "Datum-Typ")
	public String getDateType() {
		return dateMethod;
	}

	@SetProperty(index = 1, name = "Datum-Typ")
	public void setDateType(String dT) {
		dateMethod = dT;
	}

	@Override
	public String getDescription() {
		return desc;
	}

	@Override
	protected List<String> createHeadings() {
		return Arrays.asList(headings);
	}

	protected List<Konsultation> getConses() {
		Query<Konsultation> qbe = new Query<Konsultation>(Konsultation.class);
		final String dateFrom = new TimeTool(getStartDate().getTime().getTime())
				.toString(TimeTool.DATE_COMPACT);
		final String dateUntil = new TimeTool(getEndDate().getTimeInMillis())
				.toString(TimeTool.DATE_COMPACT);
		if (getDateType().equals("Konsultationsdatum")) {
			qbe.add(Konsultation.FLD_DATE, ">=", dateFrom);
			qbe.add(Konsultation.FLD_DATE, "<=", dateUntil);
		} else if (getDateType().equals("Rechnungsdatum")) {
			qbe.add(Konsultation.FLD_DATE, Query.LESS_OR_EQUAL, dateFrom);
			qbe.add(Konsultation.FLD_BILL_ID, Query.NOT_EQUAL, null);
			qbe.addPostQueryFilter(new IFilter() {
				@Override
				public boolean select(Object element) {
					Konsultation k = (Konsultation) element;
					Rechnung rn = k.getRechnung();
					String rndate = new TimeTool(rn.getDatumRn()).toString(TimeTool.DATE_COMPACT);
					return rndate.compareTo(dateFrom)>=0 && rndate.compareTo(dateUntil) <=0;
				}
			});
		} else if (getDateType().equals("Zahlungsdatum")) {
			qbe.add(Konsultation.FLD_DATE, Query.LESS_OR_EQUAL, dateFrom);
			qbe.add(Konsultation.FLD_BILL_ID, Query.NOT_EQUAL, null);
			qbe.addPostQueryFilter(new IFilter() {
				@Override
				public boolean select(Object element) {
					Konsultation k = (Konsultation) element;
					Rechnung rn = k.getRechnung();
					if(rn.getStatus()==RnStatus.BEZAHLT || rn.getStatus()==RnStatus.ZUVIEL_BEZAHLT){
						// dunno
						return false;
					}
					String rndate = new TimeTool(rn.getDatumRn()).toString(TimeTool.DATE_COMPACT);
					return rndate.compareTo(dateFrom)>=0 && rndate.compareTo(dateUntil) <=0;
				}
			});

		}
		return qbe.execute();
	}

}
