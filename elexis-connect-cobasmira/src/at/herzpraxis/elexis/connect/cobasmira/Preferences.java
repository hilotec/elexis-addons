package at.herzpraxis.elexis.connect.cobasmira;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.rs232.Connection;
import ch.elexis.util.SWTHelper;

/**
 * @author Marco Descher / Herzpraxis Dr. Wolber, Götzis, Austria
 * 
 * Original from tschaller - ch.elexis.connect.reflotron.Preferences
 * 
 * This page is shown within Elexis / Preferences / Devices / Cobas Mira.
 * It reads the current values, and stores the new values after modification.
 * 
 * It is connected to the main Elexis program via plugin.xml / Extension Point org.eclipse.ui.preferencePages
 * 
 */
public class Preferences extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String COBASMIRA_BASE = "connectors/cobasmira/"; //$NON-NLS-1$
	public static final String PORT = COBASMIRA_BASE + "port"; //$NON-NLS-1$
	public static final String TIMEOUT = COBASMIRA_BASE + "timeout"; //$NON-NLS-1$
	public static final String PARAMS = COBASMIRA_BASE + "params"; //$NON-NLS-1$
	public static final String LOG = COBASMIRA_BASE + "log"; //$NON-NLS-1$
	public static final String BACKGROUND = COBASMIRA_BASE + "background"; //$NON-NLS-1$
	public static final String IGNOREUSER = COBASMIRA_BASE + "ignoreuser"; //$NON-NLS-1$	//IDENTITY to be ignored (for CobasMira Control Purposes)
	public static final String LABIDENTIFICATION = COBASMIRA_BASE + "labidentification"; //$NON-NLS-1$ //XID of own Lab

	Combo ports, databits, stopbits, parity;
	Text speed, timeout, logFile, ignoreUser, labIdentification;
	Button log, background;

	public Preferences() {
		super(Messages.getString("CobasMiraAction.ButtonName")); //$NON-NLS-1$
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
	}

	@Override
	protected Control createContents(final Composite parent) {
		String[] param = Hub.localCfg.get(PARAMS, "1200,7,None,2").split(","); //$NON-NLS-1$ //$NON-NLS-2$

		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout(2, false));
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		
		Label lblLabIdentification = new Label(ret, SWT.NONE);
		lblLabIdentification.setText(Messages.getString("Preferences.LabIdentification")); //$NON-NLS-1$
		lblLabIdentification.setLayoutData(new GridData(SWT.NONE));
		String labIdentificationStr = Hub.localCfg.get(LABIDENTIFICATION, Messages.getString("CobasMiraAction.OwnLabIdentification"));
		labIdentification = new Text(ret, SWT.BORDER);
		labIdentification.setText(labIdentificationStr);	
		
		Label lblIgnoreUser = new Label(ret, SWT.NONE);
		lblIgnoreUser.setText(Messages.getString("Preferences.IgnoreUserOnInput")); //$NON-NLS-1$
		lblIgnoreUser.setLayoutData(new GridData(SWT.NONE));
		String ignoreUserStr = Hub.localCfg.get(IGNOREUSER, Messages
				.getString("CobasMiraAction.DefaultIgnoreUser")); //$NON-NLS-1$
		ignoreUser = new Text(ret, SWT.BORDER);
		ignoreUser.setText(ignoreUserStr);

		Label lblPorts = new Label(ret, SWT.NONE);
		lblPorts.setText(Messages.getString("Preferences.Port")); //$NON-NLS-1$
		lblPorts.setLayoutData(new GridData(SWT.NONE));
		ports = new Combo(ret, SWT.SINGLE);
		ports.setItems(Connection.getComPorts());
		ports.setText(Hub.localCfg.get(PORT, Messages
				.getString("CobasMiraAction.DefaultPort"))); //$NON-NLS-1$	

		Label lblSpeed = new Label(ret, SWT.NONE);
		lblSpeed.setText(Messages.getString("Preferences.Baud")); //$NON-NLS-1$
		lblSpeed.setLayoutData(new GridData(SWT.NONE));
		speed = new Text(ret, SWT.BORDER);
		speed.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		speed.setText(param[0]);

		Label lblDatabits = new Label(ret, SWT.NONE);
		lblDatabits.setText(Messages.getString("Preferences.Databits")); //$NON-NLS-1$
		lblDatabits.setLayoutData(new GridData(SWT.NONE));
		databits = new Combo(ret, SWT.SINGLE);
		databits.add("5");
		databits.add("6");
		databits.add("7");
		databits.add("8");
		databits.setText(param[1]);

		Label lblParity = new Label(ret, SWT.NONE);
		lblParity.setText(Messages.getString("Preferences.Parity")); //$NON-NLS-1$
		lblParity.setLayoutData(new GridData(SWT.NONE));
		parity = new Combo(ret, SWT.SINGLE);
		parity.add("None");
		parity.add("Even");
		parity.add("Odd");
		parity.setText(param[2]);

		Label lblStopbits = new Label(ret, SWT.NONE);
		lblStopbits.setText(Messages.getString("Preferences.Stopbits")); //$NON-NLS-1$
		lblStopbits.setLayoutData(new GridData(SWT.NONE));
		stopbits = new Combo(ret, SWT.SINGLE);
		stopbits.add("1");
		stopbits.add("2");
		stopbits.setText(param[3]);

		Label lblTimeout = new Label(ret, SWT.NONE);
		lblTimeout.setText(Messages.getString("Preferences.Timeout")); //$NON-NLS-1$
		lblTimeout.setLayoutData(new GridData(SWT.NONE));
		String timeoutStr = Hub.localCfg.get(TIMEOUT, Messages
				.getString("CobasMiraAction.DefaultTimeout")); //$NON-NLS-1$
		timeout = new Text(ret, SWT.BORDER);
		timeout.setText(timeoutStr);

		new Label(ret, SWT.NONE).setText(Messages
				.getString("Preferences.Backgroundprocess")); //$NON-NLS-1$
		background = new Button(ret, SWT.CHECK);
		background.setSelection(Hub.localCfg
				.get(BACKGROUND, "n").equalsIgnoreCase("y")); //$NON-NLS-1$ //$NON-NLS-2$

		new Label(ret, SWT.NONE).setText(Messages.getString("Preferences.Log")); //$NON-NLS-1$
		log = new Button(ret, SWT.CHECK);
		log.setSelection(Hub.localCfg.get(LOG, "n").equalsIgnoreCase("y")); //$NON-NLS-1$ //$NON-NLS-2$

		return ret;
	}

	public void init(IWorkbench arg0) {
	}

	@Override
	public boolean performOk() {
		StringBuilder sb = new StringBuilder();
		sb.append(speed.getText())
				.append(",").append(databits.getText()).append( //$NON-NLS-1$
						",").append(parity.getText()).append(",") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				.append(stopbits.getText());
		Hub.localCfg.set(IGNOREUSER, ignoreUser.getText().trim());
		Hub.localCfg.set(PARAMS, sb.toString());
		Hub.localCfg.set(PORT, ports.getText());
		Hub.localCfg.set(TIMEOUT, timeout.getText());
		Hub.localCfg.set(LOG, log.getSelection() ? "y" : "n"); //$NON-NLS-1$ //$NON-NLS-2$
		Hub.localCfg.set(BACKGROUND, background.getSelection() ? "y" : "n"); //$NON-NLS-1$ //$NON-NLS-2$
		Hub.localCfg.set(LABIDENTIFICATION, labIdentification.getText());
		Hub.localCfg.flush();
		return super.performOk();
	}

}
