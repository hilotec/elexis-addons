package ch.elexis.impfplan.view;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import scala.collection.JavaConversions;
import ch.elexis.impfplan.controller.ImpfplanController;
import ch.elexis.impfplan.model.VaccinationType;
import ch.elexis.util.DateInput;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

public class AddVaccinationDialog extends TitleAreaDialog {
	TableViewer tv;
	DateInput di;
	Button bCa;
	public VaccinationType result;
	public TimeTool date;
	public boolean bUnexact;
	
	public AddVaccinationDialog(Shell shell){
		super(shell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite ret=(Composite)super.createDialogArea(parent);
		ret.setLayout(new GridLayout());
		tv=new TableViewer(ret);
		tv.getControl().setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		Composite lower=new Composite(ret,SWT.NONE);
		lower.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		lower.setLayout(new FillLayout());
		di=new DateInput(lower);
		//di.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		di.setDate(new TimeTool());
		bCa=new Button(lower,SWT.CHECK);
		bCa.setText("Datum nur ungefähr");
		tv.setContentProvider(new ContentProviderAdapter(){
			@Override
			public Object[] getElements(Object arg0) {
				return JavaConversions.asCollection(ImpfplanController.allVaccs()).toArray();
			}});
		tv.setLabelProvider(new VaccinationLabelProvider());
		//dlg.setTitle("Impfung eintragen");
		tv.setInput(this);
		return ret;
	}

	@Override
	public void create() {
		super.create();
		getShell().setText("Impfung eintragen");
		setMessage("Bitte wählen Sie die applizierte Impfung und das Impfdatum aus");
	}

	@Override
	protected void okPressed() {
		IStructuredSelection sel=(IStructuredSelection)tv.getSelection();
		if(sel.isEmpty()){
			result=null;
		}
		result=(VaccinationType) sel.getFirstElement();
		date=new TimeTool(di.getDate());
		bUnexact=bCa.getSelection();
		super.okPressed();
	}
	
}
