package ch.elexis.archie.patientstatistik;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ch.elexis.data.Fall;
import ch.elexis.data.IVerrechenbar;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.data.Verrechnet;
import ch.rgw.tools.TimeTool;

public class Counter {

	public HashMap<IVerrechenbar, List<Verrechnet>> calculate(
			Patient p, TimeTool von, TimeTool bis) {
		HashMap<IVerrechenbar, List<Verrechnet>> ret = new HashMap<IVerrechenbar, List<Verrechnet>>();
		Fall[] faelle = p.getFaelle();
		if (faelle.length > 0) {
			Query<Konsultation> qbe = new Query<Konsultation>(
					Konsultation.class);
			qbe.startGroup();
			for (Fall fall : faelle) {
				qbe.add(Konsultation.CASE_ID, Query.EQUALS, fall.getId());
				qbe.or();
			}
			qbe.endGroup();
			qbe.and();
			if (von != null) {
				qbe.add(Konsultation.DATE, Query.GREATER_OR_EQUAL, von
						.toString(TimeTool.DATE_COMPACT));
			}
			if (bis != null) {
				qbe.add(Konsultation.DATE, Query.LESS_OR_EQUAL, bis
						.toString(TimeTool.DATE_COMPACT));
			}
			List<Konsultation> kk = qbe.execute();
			for(Konsultation k:kk){
				List<Verrechnet> lv=k.getLeistungen();
				for(Verrechnet v:lv){
					IVerrechenbar iv=v.getVerrechenbar();
					List<Verrechnet> liv=ret.get(iv);
					if(liv==null){
						liv=new LinkedList<Verrechnet>();
						ret.put(iv, liv);
					}
					liv.add(v);
				}
			}
		}
		return ret;
	}
}
