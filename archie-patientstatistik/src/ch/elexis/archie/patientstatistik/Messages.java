package ch.elexis.archie.patientstatistik;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.archie.patientstatistik.messages"; //$NON-NLS-1$
	public static String VerrechnungsStatistikView_AllFiles;
	public static String VerrechnungsStatistikView_AMOUNT;
	public static String VerrechnungsStatistikView_CODE;
	public static String VerrechnungsStatistikView_CODESYSTEM;
	public static String VerrechnungsStatistikView_ExportToCSV;
	public static String VerrechnungsStatistikView_NoPatientSelected;
	public static String VerrechnungsStatistikView_NUMBER;
	public static String VerrechnungsStatistikView_REFRESH;
	public static String VerrechnungsStatistikView_SUM;
	public static String VerrechnungsStatistikView_SUMTOTAL;
	public static String VerrechnungsStatistikView_TEXT;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
