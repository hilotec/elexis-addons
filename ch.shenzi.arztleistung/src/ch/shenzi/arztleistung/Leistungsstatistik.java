package ch.shenzi.arztleistung;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Mandant;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.data.TarmedLeistung;
import ch.elexis.data.Verrechnet;
import ch.rgw.tools.Money;
import ch.rgw.tools.TimeTool;
import ch.unibe.iam.scg.archie.annotations.GetProperty;
import ch.unibe.iam.scg.archie.annotations.SetProperty;
import ch.unibe.iam.scg.archie.model.AbstractTimeSeries;
import ch.unibe.iam.scg.archie.ui.widgets.WidgetTypes;

public class Leistungsstatistik extends AbstractTimeSeries {
	private static String NAME="Tarmed-Leistungsstatistik";
	private boolean bOnlyActiveMandator;
	
	public Leistungsstatistik() {
		super(NAME);
	}

	@GetProperty(name= "Nur aktueller Mandant", widgetType= WidgetTypes.BUTTON_CHECKBOX, index=1)
	public boolean getOnlyActiveMandator(){
		return bOnlyActiveMandator;
	}
	
	@SetProperty(name = "Nur aktueller Mandant", index=1)
	public void setOnlyActiveMandator(boolean val){
		bOnlyActiveMandator=val;
	}
	@Override
	public String getDescription() {
		return NAME;
	}

	@Override
	protected List<String> createHeadings() {
		String[] headings=new String[]{"Mandant", "AL (offen)", "TL (offen)", "AL (bezahlt)", "TL ( bezahlt)"};
		return Arrays.asList(headings);
	}

	/**
	 * Die TarmedLeistungen des eingestellten Zeitraums einlesen und separat nach den Kriterien TL/AL, bezahlt/offen 
	 * aufsummieren.
	 */
	@Override
	protected IStatus createContent(IProgressMonitor monitor) {
		Query<Konsultation> qbe=new Query<Konsultation>(Konsultation.class);
		TimeTool tt=new TimeTool(getStartDate().getTimeInMillis());
		qbe.add(Konsultation.FLD_DATE, Query.GREATER_OR_EQUAL, tt.toString(TimeTool.DATE_COMPACT));
		tt=new TimeTool(getEndDate().getTimeInMillis());
		qbe.add(Konsultation.FLD_DATE, Query.LESS_OR_EQUAL, tt.toString(TimeTool.DATE_COMPACT));
		// Hashes zum aufsummieren
		HashMap<Mandant, Money> alHash=new HashMap<Mandant, Money>();
		HashMap<Mandant, Money> tlHash=new HashMap<Mandant, Money>();
		HashMap<Mandant, Money> alHashOffen=new HashMap<Mandant, Money>();
		HashMap<Mandant, Money> tlHashOffen=new HashMap<Mandant, Money>();
		
		for(Konsultation k:qbe.execute()){
			Rechnung r=k.getRechnung();
			Fall actFall=k.getFall();
			Mandant mandant=k.getMandant();
			if(r!=null && r.getStatus()==RnStatus.BEZAHLT){
				// wenn eine Rechnung existiert, UND diese den Status "Bezahlt" hat
				zaehleLeistungen(tt, alHash, tlHash, k, actFall, mandant);
			}else{
				// es existiert keine Rechnung, ODER sie hat einen anderen Status als "Bezahlt". 
				zaehleLeistungen(tt, alHashOffen, tlHashOffen, k, actFall, mandant);
			}
		}
		
		// Resultat-Array für Archie aufbauen
		final ArrayList<Comparable<?>[]> result = new ArrayList<Comparable<?>[]>();
		
		for(Mandant m:alHashOffen.keySet()){
			Comparable<?>[] row = new Comparable<?>[this.dataSet.getHeadings().size()];
			row[0]=m.getLabel();
			row[1]=getMoneyAsString(alHashOffen, m);
			
			row[2]=getMoneyAsString(tlHashOffen, m);
			row[3]=getMoneyAsString(alHash, m);
			row[4]=getMoneyAsString(tlHash, m);
			result.add(row);
		}
		// Und an Archie übermitteln
		this.dataSet.setContent(result);
		
		// Job finished successfully
		monitor.done();
		

		
		return Status.OK_STATUS;
	}

	private String getMoneyAsString(Map<Mandant,Money> h, Mandant m){
		Money money=h.get(m);
		if(money==null){
			return "0.00";
		}else{
			return money.getAmountAsString();
		}
	}
	// Alle leistungen einer Konsultation aufsummieren.
	private void zaehleLeistungen(TimeTool tt, HashMap<Mandant, Money> alHash,
			HashMap<Mandant, Money> tlHash, Konsultation k, Fall actFall,
			Mandant mandant) {
		List<Verrechnet> liste=k.getLeistungen();
		for(Verrechnet v:liste){
			int zahl=v.getZahl();
			if(v.getVerrechenbar() instanceof TarmedLeistung){
				TarmedLeistung tl = (TarmedLeistung) v.getVerrechenbar();
				String arzl = v.getDetail("AL");
				String tecl = v.getDetail("TL");
				double primaryScale = v.getPrimaryScaleFactor();
				double secondaryScale = v.getSecondaryScaleFactor();

				double tlTl, tlAL, mult;
				if (arzl != null) {
					tlTl = Double.parseDouble(tecl);
					mult = PersistentObject.checkZeroDouble(v
							.get("VK_Scale")); // Taxpunkt
					tlAL = Double.parseDouble(arzl);

				} else {
					tlTl = tl.getTL();
					tlAL = tl.getAL();
					mult = tl.getVKMultiplikator(tt, actFall);
				}
				Money mAL = new Money((int) Math.round(tlAL * mult * zahl
						* primaryScale * secondaryScale));
				Money mTL = new Money((int) Math.round(tlTl * mult * zahl
						* primaryScale * secondaryScale));
				Money oldTL=tlHash.get(mandant);
				if(oldTL==null){
					oldTL=new Money();
				}
				oldTL.addMoney(mTL);
				tlHash.put(mandant, oldTL);
				Money oldAL=alHash.get(mandant);
				if(oldAL==null){
					oldAL=new Money();
				}
				oldAL.addMoney(mAL);
				alHash.put(mandant, oldAL);
			}
		}
	}
	

}
