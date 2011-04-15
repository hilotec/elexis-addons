package at.herzpraxis.elexis.connect.cobasmira;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import at.herzpraxis.elexis.connect.cobasmira.packages.Probe;
import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.LabItem;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.rs232.AbstractConnection;
import ch.elexis.rs232.AbstractConnection.ComPortListener;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

/**
 * @author Marco Descher / Herzpraxis Dr. Wolber, Goetzis, Austria
 * 
 */
public class CobasMiraAction extends Action implements ComPortListener {
	
	AbstractConnection _ctrl;
	Logger _rs232log;
	boolean background = false;
	Patient selectedPatient;
	static boolean running = false;
	
	/**
	 * 
	 */
	public CobasMiraAction(){
		super(Messages.getString("CobasMiraAction.ButtonName"), AS_CHECK_BOX); //$NON-NLS-1$
		setToolTipText(Messages.getString("CobasMiraAction.ToolTip")); //$NON-NLS-1$
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
			"at.herzpraxis.elexis.connect.cobasmira", "icons/cobasred.ico")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * This method is called when pressing the CobasMira Button
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run(){
		if (isChecked()) {
			if (running) {
				_ctrl.sendBreak();
				_ctrl.close();
				setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
					"at.herzpraxis.elexis.connect.cobasmira", "icons/cobasred.ico"));
				running = false;
				System.out.println("Cobas finished");
				return;
			}
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				"at.herzpraxis.elexis.connect.cobasmira", "icons/cobasgreen.ico"));
			running = true;
			System.out.println("Cobas listening");
			
			initConnection();
			_rs232log.logStart();
			String msg = _ctrl.connect();
			if (msg == null) {
				String timeoutStr =
					Hub.localCfg.get(Preferences.TIMEOUT, Messages
						.getString("CobasMiraAction.DefaultTimeout")); //$NON-NLS-1$
				int timeout = 20;
				try {
					timeout = Integer.parseInt(timeoutStr);
				} catch (NumberFormatException e) {
					// Do nothing. Use default value
				}
				_ctrl
					.awaitFrame(
						Desk.getTopShell(),
						Messages.getString("CobasMiraAction.WaitMsg"), 1, 4, 0, timeout, background, true); //$NON-NLS-1$
				// TODO(marco): Reset Icon to red.. but what exactly is it doing here?? HUH?
				return;
			} else {
				_rs232log.log("Error"); //$NON-NLS-1$
				SWTHelper.showError(Messages.getString("CobasMiraAction.RS232.Error.Title"), //$NON-NLS-1$
					msg);
			}
		} else {
			if (_ctrl.isOpen()) {
				_ctrl.sendBreak();
				_ctrl.close();
			}
		}
		setChecked(false);
		_rs232log.logEnd();
	}
	
	private void initConnection(){
		if (_ctrl != null && _ctrl.isOpen()) {
			_ctrl.close();
		}
		_ctrl =
			new CobasMiraConnection(Messages.getString("CobasMiraAction.ConnectionName"), //$NON-NLS-1$
				Hub.localCfg.get(Preferences.PORT, Messages
					.getString("CobasMiraAction.DefaultPort")), Hub.localCfg.get( //$NON-NLS-1$
					Preferences.PARAMS, Messages.getString("CobasMiraAction.DefaultParams")), //$NON-NLS-1$
				this);
		
		if (Hub.localCfg.get(Preferences.LOG, "n").equalsIgnoreCase("y")) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				_rs232log = new Logger(System.getProperty("user.home") + File.separator + "elexis" //$NON-NLS-1$ //$NON-NLS-2$
					+ File.separator + "cobasmira.log"); //$NON-NLS-1$
			} catch (FileNotFoundException e) {
				SWTHelper.showError(Messages.getString("CobasMiraAction.LogError.Title"), //$NON-NLS-1$
					Messages.getString("CobasMiraAction.LogError.Text")); //$NON-NLS-1$
				_rs232log = new Logger();
			}
		} else {
			_rs232log = new Logger(false);
		}
		
		background = Hub.localCfg.get(Preferences.BACKGROUND, "n").equalsIgnoreCase("y");
		
	}
	
	/**
	 * Fetch lines coming from the serial connection starting with 20. These are incoming result
	 * values. Generate a Probe object, where the initialization of the Probe object takes care
	 * about the proper parsing of the input. If the received probe adheres to certain criteria pass
	 * it on to further processing.
	 * 
	 * @param conn
	 *            , data
	 */
	public void gotData(AbstractConnection conn, final byte[] data){
		String content = new String(data);
		if (Hub.getSystemLogLevel() > 4) {
			_rs232log.logRX(content); // Log only if LogLevel >=DEBUGMSG (see ch.elexis.util.Log)
		}
		
		// String[] strArray = content.split("\r\n"); System dependent!!
		String[] strArray = content.split("\n"); //$NON-NLS-1$
		for (int i = 0; i < strArray.length; i++) {
			if (strArray[i].startsWith("20")) { // ^20 Denotes a result line
				Probe probe = new Probe(strArray[i]);
				if (Hub.getSystemLogLevel() > 4) {
					_rs232log
						.log(Messages.getString("CobasMiraAction.ProbeInput") + ": "
							+ probe.getIdent() + " " + probe.getTestKuerzel() + " "
							+ probe.getResult());
				}
				// IF IT IS A VALID PROBE, PROCESS IT, A PROBE IS VALID IFF:
				// NAME != ignoreUser AND THE RESULT IS NOT EQUAL 0!
				if (!probe.getIdent().equalsIgnoreCase(
					Hub.localCfg.get(Preferences.IGNOREUSER, "KONTROLLE"))
					&& probe.getResult() != 0) {
					processProbe(probe);
				} else {
					_rs232log
						.log(Messages.getString("CobasMiraAction.ProbeInputFault") + ":" + strArray[i]); //$NON-NLS-1$
				}
			}
			
		}
		
		_rs232log.log("Saved"); //$NON-NLS-1$
		ElexisEventDispatcher.reload(LabItem.class); // Sync the modifications on PersistentObjects
														// - especially Value, i.e. a LabItem in
														// this case
	}
	
	/**
	 * Process a valid probe. Query the database for a Patient according to the ident of the probe,
	 * present a MessageWindow with a Timeout and subsequently insert the code into the DB calling
	 * the write method of probe.
	 * 
	 * If the background parameter is set, the user is not noticed about incoming values, they are
	 * silently stored into the database.
	 * 
	 * @param probe
	 */
	private void processProbe(final Probe probe){
		Desk.getDisplay().asyncExec(new Runnable() {
			public void run(){
				String patientDeviceStr = null;
				String patientElexisStr = null;
				Query<Patient> patQuery = new Query<Patient>(Patient.class);
				
				// System.out.println("Searching for "+Patient.FLD_PATID+": "+probe.getIdent());
				patQuery.add(Patient.FLD_PATID, "like", probe.getIdent());
				List<Patient> patientList = patQuery.execute();
				if (patientList.size() == 1) {
					Patient probePat = patientList.get(0);
					patientDeviceStr = probe.getIdent();
					patientElexisStr = probePat.getName() + " " + probePat.getVorname();
					
					String text =
						MessageFormat.format(Messages.getString("CobasMiraAction.ValueInfoMsg"),
							patientDeviceStr, patientElexisStr, probe.getTestKuerzel(), probe
								.getDate().toString(TimeTool.DATE_GER), probe.getResult());
					if (background == false) {
						boolean ok =
							MessageDialog.openConfirm(Desk.getTopShell(), Messages
								.getString("CobasMiraAction.DeviceName"), text); //$NON-NLS-1$
						if (ok) {
							try {
								probe.write(probePat);
							} catch (Exception e) {
								showError("Exception: ", e.toString());
							}
							
						}
					} else {
						try {
							probe.write(probePat);
						} catch (Exception e) {
							showError("Exception: ", e.toString());
						}
						// TODO(marco): Logging if successful entry?
						//_rs232log.log(Messages.getString("CobasMiraAction.ProbeInput")+":"+probe.getIdent()+" "+probe.getTestKuerzel()+" "+probe.getResult()); //$NON-NLS-1$
					}
				} else {
					showError("ERROR", Messages.getString("CobasMiraAction.WrongPatientID") + ": "
						+ probe.getIdent());
				}
			}
		});
		
	}
	
	private static void showError(final String title, final String message){
		Desk.getDisplay().asyncExec(new Runnable() {
			
			public void run(){
				Shell shell = Desk.getTopShell();
				MessageDialog.openError(shell, title, message);
			}
		});
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.elexis.rs232.AbstractConnection.ComPortListener#gotBreak(ch.elexis
	 * .rs232.AbstractConnection)
	 */
	public void gotBreak(AbstractConnection conn){
		conn.close();
		setChecked(false);
		_rs232log.log("Break"); //$NON-NLS-1$
		_rs232log.logEnd();
		SWTHelper.showError(Messages.getString("CobasMiraAction.RS232.Break.Title"), Messages //$NON-NLS-1$
			.getString("CobasMiraAction.RS232.Break.Text")); //$NON-NLS-1$
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.elexis.rs232.AbstractConnection.ComPortListener#cancelled()
	 */
	public void cancelled(){
		_ctrl.close();
		_rs232log.log("Cancelled"); //$NON-NLS-1$
		setChecked(false);
		_rs232log.logEnd();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.elexis.rs232.AbstractConnection.ComPortListener#closed()
	 */
	public void closed(){
		_ctrl.close();
		_rs232log.log("Closed"); //$NON-NLS-1$
		setChecked(false);
		_rs232log.logEnd();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.elexis.rs232.AbstractConnection.ComPortListener#timeout()
	 */
	public void timeout(){
		_ctrl.close();
		_rs232log.log("Timeout"); //$NON-NLS-1$
		SWTHelper.showError(Messages.getString("CobasMiraAction.RS232.Timeout.Title"), //$NON-NLS-1$
			Messages.getString("CobasMiraAction.RS232.Timeout.Text")); //$NON-NLS-1$
		setChecked(false);
		_rs232log.logEnd();
	}
	
}
