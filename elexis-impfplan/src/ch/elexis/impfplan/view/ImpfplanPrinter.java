package ch.elexis.impfplan.view;

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import scala.collection.JavaConversions;
import ch.elexis.ElexisException;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.Brief;
import ch.elexis.data.Patient;
import ch.elexis.impfplan.controller.ImpfplanController;
import ch.elexis.impfplan.model.Vaccination;
import ch.elexis.impfplan.model.VaccinationType;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;

public class ImpfplanPrinter extends TitleAreaDialog implements ICallback {
	private TextContainer text = null;

	public ImpfplanPrinter(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite ret = new Composite(parent, SWT.NONE);
		Patient actPatient = ElexisEventDispatcher.getSelectedPatient();
		if (actPatient != null) {
			ret.setLayout(new FillLayout());
			ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));

			String template = Messages.ImpfplanPrinter_templateName;

			text = new TextContainer(getShell());
			text.getPlugin().createContainer(ret, this);
			text.getPlugin().showMenu(true);
			text.getPlugin().showToolbar(true);
			text.createFromTemplateName(null, template, Brief.UNKNOWN,
					Hub.actUser, Messages.ImpfplanPrinter_templateType);
			Collection<Vaccination> r = JavaConversions
					.asCollection(ImpfplanController
							.getVaccinations(actPatient));
			String[][] tbl=new String[r.size()][3];
			int line=0;
			for (Vaccination vacc : r) {
				VaccinationType vt=vacc.getVaccinationType();
				tbl[line][0]=vt.get(VaccinationType.NAME);
				tbl[line][1]=vt.get(VaccinationType.PRODUCT);
				tbl[line++][2]=vacc.getDateAsString();
			}
			text.getPlugin().insertTable(Messages.ImpfplanPrinter_templatePlaceHolder, 0, tbl, null);
			List<VaccinationType> vts;
			try {
				vts = VaccinationType.findDueFor(actPatient);
				StringBuilder sb=new StringBuilder();
				for(VaccinationType vt:vts){
					sb.append(vt.get(VaccinationType.NAME)).append("\n"); //$NON-NLS-1$
				}
				text.replace(Messages.ImpfplanPrinter_recommendPlaceholder, sb.toString());
			} catch (ElexisException e) {
				ExHandler.handle(e);
			}
		}
		return ret;
	}

	@Override
	public void create() {
		super.create();
		setMessage(Messages.ImpfplanPrinter_printPlanMessage);
		setTitle(Messages.ImpfplanPrinter_printPlanTitle);
		getShell().setText(Messages.ImpfplanPrinter_printListHeading);
		getShell().setSize(800, 700);

	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}

	@Override
	public void save() {
	}

	@Override
	public boolean saveAs() {
		return false;
	}

}
