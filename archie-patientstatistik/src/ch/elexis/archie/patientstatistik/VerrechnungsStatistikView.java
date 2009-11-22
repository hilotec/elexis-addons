package ch.elexis.archie.patientstatistik;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.actions.GlobalEvents.SelectionListener;
import ch.elexis.data.IVerrechenbar;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Verrechnet;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.rgw.tools.Money;

public class VerrechnungsStatistikView extends ViewPart implements ActivationListener, SelectionListener {
	private Action recalcAction;
	Form form;
	Table table;
	String[] tableHeaders={"Codesystem","Code","Text","Anzahl","Gesamtbetrag"};
	int[] columnWidths=new int[]{120,50,150,30,40};
	
	public VerrechnungsStatistikView() {
		
	}

	@Override
	public void createPartControl(Composite parent) {
		form=Desk.getToolkit().createForm(parent);
		form.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		form.getBody().setLayout(new GridLayout());
		table=new Table(form.getBody(),SWT.NONE);
		table.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		for(int i=0;i<tableHeaders.length;i++){
			TableColumn tc=new TableColumn(table, SWT.NONE);
			tc.setText(tableHeaders[i]);
			tc.setWidth(columnWidths[i]);
		}
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		makeActions();
		ViewMenus menu=new ViewMenus(getViewSite());
		menu.createToolbar(recalcAction);
		GlobalEvents.getInstance().addActivationListener(this, this);
	}

	@Override
	public void dispose() {
		GlobalEvents.getInstance().removeActivationListener(this, this);
		super.dispose();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public void activation(boolean mode) {
		// TODO Auto-generated method stub
		
	}

	public void visible(boolean mode) {
		if(mode){
		GlobalEvents.getInstance().addSelectionListener(this);
		}else{
			GlobalEvents.getInstance().removeSelectionListener(this);
		}
		
	}

	public void clearEvent(Class<? extends PersistentObject> template) {
		// TODO Auto-generated method stub
		
	}

	public void selectionEvent(PersistentObject obj) {
		Patient pat=GlobalEvents.getSelectedPatient();
		if(pat==null){
			form.setText("Kein Patient ausgewählt");
		}else{
			form.setText(pat.getLabel());
			recalc();
		}
	}
	
	private void recalc(){
		Patient pat=GlobalEvents.getSelectedPatient();
		if(pat!=null){
			HashMap<IVerrechenbar, List<Verrechnet>> cnt=new Counter().calculate(pat, null, null);
			HashMap<String,Money> totals=new HashMap<String,Money>();
			table.removeAll();
			
			//TreeSet<IVerrechenbar> set=new TreeSet<IVerrechenbar>(cnt.keySet());
			for(IVerrechenbar iv:cnt.keySet()){
				TableItem ti=new TableItem(table, SWT.NONE);
				String codename=iv.getCodeSystemName();
				Money tCode=totals.get(codename);
				if(tCode==null){
					tCode=new Money();
					totals.put(codename, tCode);
				}
				ti.setText(0,codename);
				ti.setText(1, iv.getCode());
				ti.setText(2,iv.getText());
				Money total=new Money();
				int count=0;
				for(Verrechnet vv:cnt.get(iv)){
					total.addMoney(vv.getNettoPreis());
					count++;
				}
				tCode.addMoney(total);
				ti.setText(3, Integer.toString(count));
				ti.setText(4, total.getAmountAsString());
			}
			Money sumAll=new Money();
			for(String n:totals.keySet()){
				TableItem ti=new TableItem(table, SWT.NONE);
				ti.setText(0, "Summe "+n);
				Money sumClass=totals.get(n);
				ti.setText(4,sumClass.getAmountAsString());
				sumAll.addMoney(sumClass);
			}
			TableItem ti=new TableItem(table,SWT.BOLD);
			ti.setText(0,"Summe total ");
			ti.setText(4,sumAll.getAmountAsString());
		}
	}

	private void makeActions(){
		recalcAction=new Action("Neu einlesen",Desk.getImageDescriptor(Desk.IMG_REFRESH)){
			@Override
			public void run() {
				recalc();
			}
			
		};
	}
}
