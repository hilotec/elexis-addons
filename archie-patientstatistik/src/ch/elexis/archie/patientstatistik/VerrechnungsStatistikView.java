package ch.elexis.archie.patientstatistik;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
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
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Money;
import ch.rgw.tools.StringTool;

public class VerrechnungsStatistikView extends ViewPart implements ActivationListener,
SelectionListener, Counter.IJobFinishedListener {
	private Action recalcAction, exportCSVAction;
	Form form;
	Table table;
	String[] tableHeaders = {
		"Codesystem", "Code", "Text", "Anzahl", "Gesamtbetrag"
	};
	int[] columnWidths = new int[] {
		130, 60, 160, 40, 50
	};
	
	public VerrechnungsStatistikView(){
		
	}
	
	@Override
	public void createPartControl(Composite parent){
		form = Desk.getToolkit().createForm(parent);
		form.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		form.getBody().setLayout(new GridLayout());
		table = new Table(form.getBody(), SWT.NONE);
		table.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		for (int i = 0; i < tableHeaders.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE);
			tc.setText(tableHeaders[i]);
			tc.setWidth(columnWidths[i]);
		}
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		makeActions();
		ViewMenus menu = new ViewMenus(getViewSite());
		menu.createToolbar(exportCSVAction, recalcAction);
		menu.createMenu(exportCSVAction);
		GlobalEvents.getInstance().addActivationListener(this, this);
	}
	
	@Override
	public void dispose(){
		GlobalEvents.getInstance().removeActivationListener(this, this);
		super.dispose();
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	public void activation(boolean mode){
		// TODO Auto-generated method stub
		
	}
	
	public void visible(boolean mode){
		if (mode) {
			GlobalEvents.getInstance().addSelectionListener(this);
			selectionEvent(GlobalEvents.getSelectedPatient());
		} else {
			GlobalEvents.getInstance().removeSelectionListener(this);
		}
		
	}
	
	public void clearEvent(Class<? extends PersistentObject> template){
		// TODO Auto-generated method stub
		
	}
	
	public void selectionEvent(PersistentObject obj){
		Patient pat = GlobalEvents.getSelectedPatient();
		if (pat == null) {
			form.setText("Kein Patient ausgewählt");
		} else {
			form.setText(pat.getLabel());
			recalc();
		}
	}
	
	private void recalc(){
		
		Patient pat = GlobalEvents.getSelectedPatient();
		if (pat != null) {
			final Counter counter = new Counter(pat, null, null, this);
			counter.schedule();
		}
	}
	
	public void jobFinished(final Counter counter){
		HashMap<IVerrechenbar, List<Verrechnet>> cnt = counter.getValues();
		HashMap<String, Money> totals = new HashMap<String, Money>();
		table.removeAll();
		
		// TreeSet<IVerrechenbar> set=new
		// TreeSet<IVerrechenbar>(cnt.keySet());
		ArrayList<IVerrechenbar> set = new ArrayList<IVerrechenbar>(cnt.keySet());
		Collections.sort(set, new Comparator<IVerrechenbar>() {
			
			public int compare(IVerrechenbar o1, IVerrechenbar o2){
				if (o1 != null && o2 != null) {
					String csname1 = o1.getCodeSystemName();
					String csname2 = o2.getCodeSystemName();
					int res = csname1.compareTo(csname2);
					if (res == 0) {
						String cscode1 = o1.getCode();
						String cscode2 = o2.getCode();
						res = cscode1.compareTo(cscode2);
					}
					return res;
				}
				return 0;
				
			}
		});
		for (IVerrechenbar iv : set) {
			if (iv != null) {
				TableItem ti = new TableItem(table, SWT.NONE);
				String codename = iv.getCodeSystemName();
				Money tCode = totals.get(codename);
				if (tCode == null) {
					tCode = new Money();
					totals.put(codename, tCode);
				}
				ti.setText(0, StringTool.unNull(codename));
				ti.setText(1, StringTool.unNull(iv.getCode()));
				ti.setText(2, StringTool.unNull(iv.getText()));
				Money total = new Money();
				int count = 0;
				for (Verrechnet vv : cnt.get(iv)) {
					Money singlePrice=vv.getNettoPreis();
					int num=vv.getZahl();
					singlePrice.multiply(num);
					total.addMoney(singlePrice);
					count+=num;
				}
				tCode.addMoney(total);
				ti.setText(3, Integer.toString(count));
				ti.setText(4, total.getAmountAsString());
				
			}
		}
		Money sumAll = new Money();
		for (String n : totals.keySet()) {
			TableItem ti = new TableItem(table, SWT.NONE);
			ti.setText(0, "Summe " + n);
			Money sumClass = totals.get(n);
			ti.setText(4, sumClass.getAmountAsString());
			sumAll.addMoney(sumClass);
		}
		TableItem ti = new TableItem(table, SWT.BOLD);
		ti.setText(0, "Summe total ");
		ti.setText(4, sumAll.getAmountAsString());
	}
	
	private void makeActions(){
		recalcAction = new Action("Neu einlesen", Desk.getImageDescriptor(Desk.IMG_REFRESH)) {
			@Override
			public void run(){
				recalc();
			}
			
		};
		exportCSVAction =
			new Action("Nach CSV exportieren", Desk.getImageDescriptor(Desk.IMG_EXPORT)) {
			@Override
			public void run(){
				FileDialog fd = new FileDialog(getViewSite().getShell(), SWT.SAVE);
				fd.setFilterExtensions(new String[] {
					"*.csv", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[] {
					"CSV", "Alle Dateien"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFileName("elexis-verr.csv"); //$NON-NLS-1$
				String fname = fd.open();
				if (fd != null) {
					try {
						FileWriter fw = new FileWriter(fname);
						fw.write(StringTool.join(tableHeaders, ";")+"\r\n");
						for (TableItem it : table.getItems()) {
							StringBuilder sb = new StringBuilder();
							sb.append(it.getText(0)).append(";").append(it.getText(1)).append(
							";").append(it.getText(2)).append(";").append(it.getText(3))
							.append(";").append(it.getText(4)).append("\r\n");
							fw.write(sb.toString());
						}
						fw.close();
					} catch (IOException e) {
						ExHandler.handle(e);
						
					}
					
				}
				
			}
			
		};
	}
}
