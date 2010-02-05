package ch.elexis.stickynotes.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.stickynotes.ui.messages"; //$NON-NLS-1$
	public static String Preferences_BackgroundColor;
	public static String Preferences_ForegroundColor;
	public static String StickyNotesView_NoPatientSelected;
	public static String StickyNotesView_StickyNotesName;
	public static String StickyNotesView_StickyNotesNameDash;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
