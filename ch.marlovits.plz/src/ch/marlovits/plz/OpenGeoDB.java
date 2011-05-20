package ch.marlovits.plz;

import java.io.File;
import java.io.FileInputStream;

import org.eclipse.jface.dialogs.MessageDialog;

import ch.elexis.Hub;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.Log;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.JdbcLink.Stm;

public class OpenGeoDB {
	protected static Log log = Log.get("PersistentObject");
	
	public OpenGeoDB(){
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void createOpenGeoDBTables(){
		final Stm stm = PersistentObject.getConnection().getStatement();
		
		log.log("Creating Tables for OpenGeoDB", Log.SYNCMARK);
		java.io.InputStream is = null;
		// Stm stm = null;
		try {
			File base = new File(Hub.getBasePath());
			String myBasePath =
				base.getParent().toString() + File.separator + "marlovits-plz" + File.separator
					+ "src" + File.separator + "ch" + File.separator + "marlovits" + File.separator
					+ "plz";
			String createscript = myBasePath + File.separator + "openGeoDB.script";
			is = new FileInputStream(createscript);
			if (stm.execScript(is, true, true) == true) {
				PersistentObject.disconnect();
				MessageDialog.openInformation(null, "Titel ???",
					"Die Tabellen für OpenGeoDB wurden erstellt.");
			} else {
				log.log("Kein create script für die OpenGeoDB Tabellen gefunden.", Log.ERRORS);
				return;
			}
		} catch (Throwable ex) {
			ExHandler.handle(ex);
			return;
		} finally {
			PersistentObject.getConnection().releaseStatement(stm);
			try {
				is.close();
			} catch (Exception ex) {
				/* Janusode */
			}
		}
	}
	
	public void importOpenDBData(){

	}
}