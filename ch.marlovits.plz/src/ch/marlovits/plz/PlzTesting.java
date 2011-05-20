package ch.marlovits.plz;

import java.awt.Font;
import java.awt.FontMetrics;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import javax.mail.Message;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.GlobalActions;
import ch.elexis.commands.Handler;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.PersistentObjectFactory;
import ch.elexis.mail.Mailer;
import ch.elexis.mail.PreferenceConstants;
import ch.elexis.util.Extensions;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.marlovits.plz.MCCombo.MCComboDataProvider;
import ch.rgw.io.FileTool;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.JdbcLink.Stm;

public class PlzTesting extends ViewPart implements ISaveablePart2 {
	public static final String ID = "ch.marlovits.plz.PLZView";
	
	// noch keine gute Methode gefunden, um die Breite des Menu-Innenteils festzustellen...
	public int scrollBarWidth;
	
	// die Felder auf der ViewPart
	private Composite top;
	private Composite rightComposite;
	private GridLayout tmpGrid;
	private Combo cbLandCombo;
	private Text landIso2Field;
	private ModifyListener landModifyListener;
	private MCCombo ortMCCombo;
	private MCCombo plzMCCombo;
	
	// Actions für ViewMenu, etc
	private Action testingAction;
	private Action importNamesAction;
	private Action importCountriesAction;
	private Action importTabDelimitedAction;
	MarlovitsCombo marloCombo;
	
	// constructor
	public PlzTesting(){
		super();
		// wenn jemand eine Methode weiss, wie man den Innenbereich des Menus erhält...
		if (System.getProperties().getProperty("os.name").equals("Windows XP")) {
			scrollBarWidth = 25;
		} else {
			scrollBarWidth = 5;
		}
		landModifyListener = new LandModifyListener();
	}
	
	// erstellen der ViewPart
	public void createPartControl(Composite parent){
		landModifyListener = new LandModifyListener();
		
		top = new Composite(parent, SWT.NONE);
		top.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		top.setLayout(new GridLayout(2, false));
		
		// Land *************************************
		new Label(top, SWT.NONE).setText("Land");
		
		// rightComposite für diese Zeile erstellen
		rightComposite = new Composite(top, SWT.NONE);
		rightComposite.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tmpGrid = new GridLayout(2, false);
		tmpGrid.marginWidth = 0;
		rightComposite.setLayout(tmpGrid);
		
		// LandCombo
		String[] laenderListeNamen = getLaenderListe("name", "landsorting, name", "en");
		String[] laenderListeIsos = getLaenderListe("iso2", "landsorting, name", "en");
		cbLandCombo = new Combo(rightComposite, SWT.DROP_DOWN + SWT.READ_ONLY);
		// cbLandCombo = new Combo(rightComposite, SWT.DROP_DOWN|SWT.READ_ONLY);
		cbLandCombo.setItems(laenderListeNamen);
		cbLandCombo.setData("LandIso2", laenderListeIsos);
		cbLandCombo.setVisibleItemCount(20);
		
		// landIso2Field
		// new Label(landComposite, SWT.NONE).setText("Land Iso 3");
		landIso2Field = new Text(rightComposite, SWT.BORDER);
		
		cbLandCombo.setText("Prefs Land");
		landIso2Field.setText("Prefs LandISO2");
		
		// Plz Label *************************************
		new Label(top, SWT.NONE).setText("Postleitzahl");
		
		// brauch ich dann...
		String landIso2 = landIso2Field.getText();
		
		// *** PLZ MCCombo
		// plzMCCombo = new MCCombo(top, SWT.BORDER);
		// plzMCCombo.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		// String[] plzShowField = null;
		// Object[] plzReturnFields = null;
		// if (StringTool.isNothing(landIso2)) {
		// plzShowField = new String[] {"plz", "ort27", "kanton", "land"};
		// plzReturnFields = new Object[] {plzMCCombo, ortMCCombo, null, landIso2Field};
		// } else {
		// plzShowField = new String[] {"plz", "ort27", "kanton"};
		// plzReturnFields = new Object[] {plzMCCombo, ortMCCombo, null};
		// }
		// plzMCCombo.setShowFields(plzShowField);
		// plzMCCombo.setSortFields(plzShowField);
		// plzMCCombo.setReturnFields(plzReturnFields);
		
		// Ort Label *************************************
		// new Label(top, SWT.NONE).setText("Ort");
		
		// *** Ort MCCombo
		// ortMCCombo = new MCCombo(top, SWT.BORDER);
		// ortMCCombo.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		// String[] ortShowField = {"ort27", "kanton", "plz", "land"};
		// ortMCCombo.setShowFields(ortShowField);
		// ortMCCombo.setSortFields(ortShowField);
		// Object[] ortReturnFields = {ortMCCombo, null, plzMCCombo, landIso2Field};
		// ortMCCombo.setReturnFields(ortReturnFields);
		
		// Erstellen der Actions für die Menus, etc
		makeActions();
		
		// Erstellen des ViewMenus
		ViewMenus menu = new ViewMenus(getViewSite());
		menu.createMenu(testingAction, importCountriesAction, null, importNamesAction,
			importTabDelimitedAction);
		
		// *** alle Listener, etc. intallieren
		// cbLandCombo.addModifyListener(landModifyListener);
		// plzMCCombo.installDataProvider(new PlzMCComboDataProvider());
		// ortMCCombo.installDataProvider(new OrtMCComboDataProvider());
		// cbLandCombo.addControlListener(new landComboControlListener());
		
		CCombo myCCombo = new CCombo(top, SWT.BORDER);
		String[] itemss =
			{
				"item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9",
				"item10"
			};
		myCCombo.setItems(itemss);
		
		// Another possibility
		// NO: THIS SEEMS TO BE THE SOLUTION !!! Nö - doch nicht...
		MCCombo2 mCCombo2 = new MCCombo2(top, SWT.BORDER | SWT.READ_ONLY);
		String[][] items2 = dataProviderForMCCombo2("c");
		mCCombo2.setItems(items2);
		// mCCombo2.setDividerLineColor(SWT.COLOR_DARK_MAGENTA);
		// mCCombo2.setDrawDividerLines(false);
		// mCCombo2.setColumnSpacing(20);
		// mCCombo2.setColumnLeftMargin(0);
		
		marloCombo = new MarlovitsCombo(top, SWT.BORDER | SWT.READ_ONLY);
		String[][] marlovitsItems = dataProviderForMarlovitsCombo("c");
		// marloCombo.setItems(marlovitsItems);
		
		Button button = new Button(top, SWT.NONE);
		button.setText("Testing");
		button.addSelectionListener(new ButtonListener());
	}
	
	class ButtonListener implements SelectionListener {
		public void widgetDefaultSelected(SelectionEvent e){
			System.out.println("widet default selected");
		}
		
		public void widgetSelected(SelectionEvent e){
			// marloCombo.setBackground(marloCombo.getDisplay().getSystemColor(SWT.COLOR_CYAN));
			// String[][] items = {{"1", "2", "3", "4", "5"}, {"_1", "_2", "_3", "_4", "_5"}};
			// marloCombo.setItems(items);
			int rowNum = marloCombo.getTable().getItemCount() + 1;
			// String[] rowCells = {rowNum + "1", rowNum + "2", rowNum + "3", rowNum + "4", rowNum +
			// "5"};
			// String[] rowCells = {rowNum + "1", rowNum + "2", rowNum + "3", rowNum + "4", rowNum +
			// "5", rowNum + "6"};
			for (rowNum = 1; rowNum < 10; rowNum++) {
				String[] rowCells =
					{
						"row " + rowNum + "/1", "row " + rowNum + "/2", "row " + rowNum + "/3",
						"row " + rowNum + "/4"
					};
				marloCombo.add(rowCells);
			}
			
			// System.out.println(marloCombo.indexOf(0, "row 9/1"));
			// String[] strs = marloCombo.getItemStringArray(3);
			// for (int i = 0; i < strs.length; i++) {
			// System.out.println(strs[i]);
			// }
			// marloCombo.setCell(0, 0, "replaced");
			// String[] strs = {"repla 1", "asdff 2", ":__sd 3", "aff 4", "55555 4"};
			// marloCombo.setItem(2, strs);
			
			String[][] marlovitsItems = dataProviderForMarlovitsCombo("c");
			marloCombo.setItems(marlovitsItems);
		}
	}
	
	/**
	 * Berechnet die Länge der Divider neu und setzt die neuen Dividers
	 * 
	 * @author Harry
	 * 
	 */
	class landComboControlListener implements ControlListener {
		public void controlMoved(ControlEvent e){}
		
		public void controlResized(ControlEvent e){
			String fillString = null;
			for (int i = 0; i < cbLandCombo.getItemCount(); i++) {
				System.out.println("item: " + cbLandCombo.getItem(i));
				if (cbLandCombo.getItem(i).equals("-")) {
					if (fillString == null) {
						fillString =
							getMaxLengthString(cbLandCombo, cbLandCombo.getBounds().width
								- scrollBarWidth, "-");
					}
					cbLandCombo.setItem(i, fillString);
				}
			}
		}
	}
	
	/**
	 * Gibt den String zurück der maxLength maximal ausfüllt mit chr
	 * 
	 * @param composite
	 *            für dieses Composite Font/Style/Size/Graphics
	 * @param maxLength
	 *            maximal erlaubte Länge in Pixeln
	 * @param chr
	 *            der String wird mit diesem Buchstaben gefüllt
	 * @return den ermittelten String
	 */
	public static String getMaxLengthString(Composite composite, final int maxLength,
		final String chr){
		double oneCharWidth = stringWidth(composite, chr);
		int numOfChars = (int) (maxLength / oneCharWidth);
		String fillString = StringTool.filler(chr, numOfChars);
		double currStringWidth = stringWidth(composite, fillString);
		if (currStringWidth == maxLength) {
			return fillString;
		}
		if (currStringWidth < maxLength) {
			while (currStringWidth < maxLength) {
				String newFillString = fillString + chr;
				double newStringWidth = stringWidth(composite, newFillString);
				if (newStringWidth > maxLength) {
					return fillString;
				} else {
					fillString = newFillString;
				}
				currStringWidth = newStringWidth;
			}
		} else {
			while (currStringWidth > maxLength) {
				String newFillString = fillString.substring(1, fillString.length() - chr.length());
				double newStringWidth = stringWidth(composite, newFillString);
				if (newStringWidth < maxLength) {
					return fillString;
				} else {
					fillString = newFillString;
				}
				currStringWidth = newStringWidth;
			}
		}
		return fillString;
	}
	
	/**
	 * Länge eines Strings in Pixeln ermitteln. Es wird der Zeichensatz benutzt, der im Feld
	 * composite verwendet wird. Etwas umständlich, da das Resultat von getStringBounds() "falsch"
	 * rauskommt
	 * 
	 * @param composite
	 *            : Berechnung für dieses Feld
	 * @param str
	 *            : die Länge dieses Strings wird berechnet
	 * @return
	 */
	@SuppressWarnings("serial")
	public static double stringWidth(Composite composite, final String str){
		FontData[] fontData = composite.getFont().getFontData();
		FontMetrics metrics =
			new FontMetrics(new Font(fontData[0].getName(), fontData[0].getStyle(), fontData[0]
				.getHeight())) {};
		double width = metrics.getStringBounds(str, null).getWidth();
		// muss offensichtlich um Auflösung korrgiert werden - hoffe, das funktioniert auf allen
		// OSes gleich
		int horDpi = composite.getDisplay().getDPI().x;
		width = width * horDpi / 72;
		return width;
	}
	
	/**
	 * Die Einträge ermitteln, die in der Plz-Liste angezeigt werden sollen. Diese Routine wird
	 * aufgerufen, wenn der Text im Textfeld geändert wurde.
	 * 
	 * @return die anzuzeigenden Einträge für die Liste als zweidimensionaler String array
	 */
	class PlzMCComboDataProvider implements MCComboDataProvider {
		// ausnahmsweise direkte Abfrage auf der Datenbank aufgrund der Geschwindigkeit,
		// die hier relevant ist.
		// via Persistent/Query unsäglich langsam (Buchstabe A hat mehrere hundert
		// Einträge nur schon für die Schweiz...)
		public String[][] mCComboDataProvider(MCCombo lThis){
			// das Resultat initialisieren
			String[][] plzStrings = null;
			
			// wenn Feld leer, dann keine Auswahl anzeigen
			String currText = plzMCCombo.getText();
			if (StringTool.isNothing(currText)) {
				return plzStrings;
			}
			
			// die anzuzeigenden/Sortierungs- Felder einlesen
			String[] showFields = lThis.getShowFields();
			String[] queryFields = lThis.getQueryFields();
			String[] sortFields = lThis.getSortFields();
			// umwandeln in comma-delimited Strings für sql
			String showFieldsString = stringArrayToString(showFields, ",");
			String queryFieldsString = showFieldsString;
			if (queryFields != null) {
				queryFieldsString = stringArrayToString(queryFields, ",");
			}
			String sortFieldsString = stringArrayToString(sortFields, ",");
			
			// Anzahl anzuzeigender=abzufragender Felder ist in showFields definiert
			int numOfFields = showFields.length;
			
			// alle passenden Einträge aus der Datenbank auslesen
			// Datenbank anzapfen - oozapft is...
			Stm stm = PersistentObject.getConnection().getStatement();
			
			// Anzahl Einträge ermitteln, die passen
			int numOfEntries = 0;
			// wenn kein Land ausgewählt ist, dann via sql ALLE Länder abfragen
			String landClause = " 1=1 ";
			if (!StringTool.isNothing(landIso2Field.getText())) {
				landClause =
					" lower(land) = lower(" + JdbcLink.wrap(landIso2Field.getText()) + ") ";
			}
			ResultSet rs =
				stm.query("select count(*) as cnt from " + PlzEintrag.getTableName2() + " where "
					+ landClause + " and lower(plz) like lower(" + JdbcLink.wrap(currText + "%")
					+ ") and plztyp != 80");
			try {
				rs.next();
				numOfEntries = Integer.decode(rs.getString("cnt"));
				rs.close();
			} catch (SQLException exc) {
				System.out.println("SQLException 1 in OrtMCComboDataProvider:");
				exc.printStackTrace();
			}
			if (numOfEntries > 0) {
				// Die einzelnen Einträge abfragen und in einen String-Array und dann in die Liste
				// schreiben
				rs =
					stm.query("select " + queryFieldsString + " from " + PlzEintrag.getTableName2()
						+ " where " + landClause + " and lower(plz) like lower("
						+ JdbcLink.wrap(currText + "%") + ")  and plztyp != 80 order by "
						+ sortFieldsString);
				try {
					plzStrings = new String[numOfEntries][numOfFields];
					int iii = 0;
					while (rs.next()) {
						String[] rowData = new String[numOfFields];
						for (int showFieldsIx = 0; showFieldsIx < numOfFields; showFieldsIx++) {
							rowData[showFieldsIx] = rs.getString(showFieldsIx + 1);
						}
						plzStrings[iii] = rowData;
						iii++;
					}
				} catch (SQLException e1) {
					System.out.println("SQLException 2 in OrtMCComboDataProvider:");
					e1.printStackTrace();
				}
			}
			return plzStrings;
		}
	}
	
	/**
	 * Die Einträge ermitteln, die in der Ort-Liste angezeigt werden sollen. Diese Routine wird
	 * aufgerufen, wenn der Text im Textfeld geändert wurde.
	 * 
	 * @return die anzuzeigenden Einträge für die Liste als zweidimensionaler String array
	 */
	class OrtMCComboDataProvider implements MCComboDataProvider {
		// ausnahmsweise direkte Abfrage auf der Datenbank aufgrund der Geschwindigkeit,
		// die hier relevant ist.
		// via Persistent/Query unsäglich langsam (Buchstabe A hat mehrere hundert
		// Einträge nur schon für die Schweiz...)
		public String[][] mCComboDataProvider(MCCombo lThis){
			// das Resultat initialisieren
			String[][] plzStrings = null;
			
			// wenn Feld leer, dann keine Auswahl anzeigen
			String currText = ortMCCombo.getText();
			if (StringTool.isNothing(currText)) {
				return plzStrings;
			}
			
			// die anzuzeigenden/Sortierungs- Felder einlesen
			String[] showFields = lThis.getShowFields();
			String[] queryFields = lThis.getQueryFields();
			String[] sortFields = lThis.getSortFields();
			// umwandeln in comma-delimited Strings für sql
			String showFieldsString = stringArrayToString(showFields, ",");
			String queryFieldsString = showFieldsString;
			if (queryFields != null) {
				queryFieldsString = stringArrayToString(queryFields, ",");
			}
			String sortFieldsString = stringArrayToString(sortFields, ",");
			
			// Anzahl anzuzeigender=abzufragender Felder ist in showFields definiert
			int numOfFields = showFields.length;
			
			// alle passenden Einträge aus der Datenbank auslesen
			// Datenbank anzapfen - oozapft is...
			Stm stm = PersistentObject.getConnection().getStatement();
			
			// Anzahl Einträge ermitteln, die passen
			int numOfEntries = 0;
			// wenn kein Land ausgewählt ist, dann via sql ALLE Länder abfragen
			String landClause = " 1=1 ";
			if (!StringTool.isNothing(landIso2Field.getText())) {
				landClause =
					" lower(land) = lower(" + JdbcLink.wrap(landIso2Field.getText()) + ") ";
			}
			ResultSet rs =
				stm.query("select count(*) as cnt from " + PlzEintrag.getTableName2() + " where "
					+ landClause + " and lower(ort27) like lower(" + JdbcLink.wrap(currText + "%")
					+ ") and plztyp != 80");
			try {
				rs.next();
				numOfEntries = Integer.decode(rs.getString("cnt"));
				rs.close();
			} catch (SQLException exc) {
				System.out.println("SQLException 1 in OrtMCComboDataProvider:");
				exc.printStackTrace();
			}
			// Die einzelnen Einträge abfragen und in einen String-Array und dann in die Liste
			// schreiben
			if (numOfEntries > 0) {
				rs =
					stm.query("select " + queryFieldsString + " from " + PlzEintrag.getTableName2()
						+ " where " + landClause + " and lower(ort27) like lower("
						+ JdbcLink.wrap(currText + "%") + ")  and plztyp != 80 order by "
						+ sortFieldsString);
				try {
					plzStrings = new String[numOfEntries][numOfFields];
					int iii = 0;
					while (rs.next()) {
						String[] rowData = new String[numOfFields];
						for (int showFieldsIx = 0; showFieldsIx < numOfFields; showFieldsIx++) {
							rowData[showFieldsIx] = rs.getString(showFieldsIx + 1);
						}
						plzStrings[iii] = rowData;
						iii++;
					}
				} catch (SQLException e1) {
					System.out.println("SQLException 2 in OrtMCComboDataProvider:");
					e1.printStackTrace();
				}
			}
			return plzStrings;
		}
	}
	
	class LandModifyListener implements ModifyListener {
		int oldSelectedItem = 0;
		
		public void modifyText(ModifyEvent arg0){
			int selected = cbLandCombo.getSelectionIndex();
			if (selected != -1) {
				// setzen des isoStrings
				String[] returnStrings = (String[]) cbLandCombo.getData("LandIso2");
				String tmpStr = returnStrings[selected];
				if (StringTool.isNothing(tmpStr)) {
					landIso2Field.setText("");
					oldSelectedItem = selected;
				} else {
					if (tmpStr.substring(0, 1).equals("-")) {
						cbLandCombo.setText(cbLandCombo.getItem(oldSelectedItem));
						tmpStr = returnStrings[oldSelectedItem];
						if (StringTool.isNothing(tmpStr))
							tmpStr = "";
						landIso2Field.setText(tmpStr);
					} else {
						landIso2Field.setText(tmpStr);
						oldSelectedItem = selected;
					}
				}
			}
			String landIso2 = landIso2Field.getText();
			// plz
			String[] plzShowField = null;
			Object[] plzReturnFields = null;
			if (StringTool.isNothing(landIso2)) {
				plzShowField = new String[] {
					"plz", "ort27", "kanton", "land"
				};
				plzReturnFields = new Object[] {
					plzMCCombo, ortMCCombo, null, landIso2Field
				};
			} else {
				plzShowField = new String[] {
					"plz", "ort27", "kanton"
				};
				plzReturnFields = new Object[] {
					plzMCCombo, ortMCCombo, null
				};
			}
			plzMCCombo.setShowFields(plzShowField);
			plzMCCombo.setSortFields(plzShowField);
			plzMCCombo.setReturnFields(plzReturnFields);
			plzMCCombo.redraw();
			// ort
			String[] ortShowField = null;
			Object[] ortReturnFields = null;
			if (StringTool.isNothing(landIso2)) {
				ortShowField = new String[] {
					"ort27", "plz", "kanton", "land"
				};
				ortReturnFields = new Object[] {
					ortMCCombo, plzMCCombo, null, landIso2Field
				};
			} else {
				ortShowField = new String[] {
					"ort27", "plz", "kanton"
				};
				ortReturnFields = new Object[] {
					ortMCCombo, plzMCCombo, null
				};
			}
			ortMCCombo.setShowFields(ortShowField);
			ortMCCombo.setSortFields(ortShowField);
			ortMCCombo.setReturnFields(ortReturnFields);
			ortMCCombo.redraw();
		}
	}
	
	/**
	 * WorkbenchPart Methods
	 */
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus(){
	// plzViewer.getControl().setFocus();
	}
	
	/*
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose(){
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}
	
	public void doSave(IProgressMonitor monitor){ /* leer */}
	
	public void doSaveAs(){ /* leer */}
	
	public boolean isDirty(){
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
	
	/**
	 * Converts a StringArray to a string, delimited by {delimiter}
	 * 
	 * @param strArray
	 * @param delimiter
	 * @return
	 */
	protected static String stringArrayToString(String[] strArray, String delimiter){
		String str = "";
		String delim = "";
		for (int i = 0; i < strArray.length; i++) {
			str = str + delim + strArray[i];
			delim = delimiter;
		}
		return str;
	}
	
	/**
	 * Erstellen der Actions, die in ViewMenu/PopupMenu, etc benutzt werden
	 */
	private void makeActions(){
		// Tester Menu Item
		testingAction = new Action("Testing") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_REFRESH));
				setToolTipText("Testing Methods");
			}
			
			@SuppressWarnings("unchecked")
			public void run(){
				
				// test settings
				String mailSubjectName = "OP-Anmeldung";
				String mailDatum = "2010_06_22";
				String mailPatientSpec = "Name, Vorname, 21.07.1976";
				String sender = "marlovits@hin.ch";
				String recipient = "marlovits@hin.ch";
				
				// create mailer
				Mailer mailer = new Mailer();
				
				// create mailSubject: OP-Anmeldung, yyyy_mm_dd, Name Vorname, Geburtsdatum
				String mailSubject = mailSubjectName + ", " + mailDatum + ", " + mailPatientSpec;
				
				// create empty mail
				Message myMsg = mailer.createMultipartMessage(mailSubject, recipient);
				// mailer.addTextPart(myMsg, "text added by addTextPart");
				
				// read pdf-file into stream
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					FileInputStream fis =
						new FileInputStream(
							"C:\\Dokumente und Einstellungen\\Harry66\\Desktop\\HIN\\hin.pdf");
					FileTool.copyStreams(fis, baos);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// add pdf-stream to mail as binary
				mailer.addBinaryPart(myMsg, mailSubject + ".pdf", baos.toByteArray());
				boolean bool = mailer.addTextPart(myMsg, "adfasdf adsf");
				mailer.send(myMsg, "marlovits@hin.ch");
				
				int dummy = 1;
				if (dummy == 0) {
					List<PersistentObjectFactory> exts =
						Extensions.getClasses("ch.elexis.PersistentReference", "Class");
					for (PersistentObjectFactory po : exts) {
						System.out.println(po.getClass().getName());
					}
					System.out.println("regular");
					
					// System.out.println("SWT.Drag: " + SWT.DRAG);
					// System.out.println("SWT.KeyDown: " + SWT.KeyDown);
					
					// int gender = MarlovitsVornamen.getGender("Harald");
					// MarlovitsVornamen.extractVornamen();
					
					JdbcLink myJdbcLink = JdbcLink.createODBCLink("FromAeskulap");
					// System.out.println("myJdbcLink: " + myJdbcLink);
					boolean err = myJdbcLink.connect("postgres", "Knorrli_66_07");
					System.out.println("err: " + err);
					String testQueryString =
						myJdbcLink.queryString("select STATION_NAME from STATION");
					System.out.println("testQueryString: " + testQueryString);
					
					JFrame frame = new JFrame("Hello!!");
					frame.setAlwaysOnTop(true);
					frame.setLocationByPlatform(true);
					frame.add(new JLabel("             Textbausteinauswahl              "));
					frame.pack();
					frame.setVisible(true);
					
					// marloCombo.dbgSet(marloCombo.DBG_TableEvent);
					// marloCombo.dbgSet(marloCombo.DBG_TextEvent);
					
					int dayDiff;
					GregorianCalendar gc1 = new GregorianCalendar(2005, Calendar.MAY, 21);
					GregorianCalendar gc2 = new GregorianCalendar(2009, Calendar.JUNE, 6);
					
					dayDiff =
						(int) ((gc1.getTimeInMillis() - gc2.getTimeInMillis()) / (24 * 60 * 60 * 1000));
					System.out.println("dayDiff: " + dayDiff);
					
					// ENVIRONS, PROPERTIES
					System.out.println(System.getenv());
					System.out.println(System.getProperties());
					
					// PROGRESS MONITOR ***********************************************
					ExecutionEvent eev = new ExecutionEvent();
					IProgressMonitor monitor = Handler.getMonitor(eev);
					monitor.beginTask("theName", 100);
					for (int i = 0; i < 100; i++) {
						monitor.worked(i);
					}
				}
			}
		};
		testingAction.setActionDefinitionId("testingAction");
		GlobalActions.registerActionHandler(this, testingAction);
		
		// alle Names importieren
		importNamesAction = new Action("Names importieren") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Übersetzungen importieren (dauert lange!!!)");
			}
			
			public void run(){
				DataImporter.importNameData();
			}
		};
		importNamesAction.setActionDefinitionId("importNamesAction");
		GlobalActions.registerActionHandler(this, importNamesAction);
		
		// alle Names importieren
		importCountriesAction = new Action("Länder importieren") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Länder Iso-Daten importieren (dauert lange!!!)");
			}
			
			public void run(){
				DataImporter.importCountryData("en,de,it,fr");
			}
		};
		importCountriesAction.setActionDefinitionId("importCountriesAction");
		GlobalActions.registerActionHandler(this, importCountriesAction);
		
		// alle Names importieren
		importTabDelimitedAction = new Action("Postleitzahlen importieren") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Importieren PLZ Schweiz");
			}
			
			public void run(){
				DataImporter.importTabDelimited();
			}
		};
		importTabDelimitedAction.setActionDefinitionId("importTabDelimitedAction");
		GlobalActions.registerActionHandler(this, importTabDelimitedAction);
	}
	
	/**
	 * nur Länder mit allen 3 Isos dürfen auswählbar sein: iso2, iso3 isonum! Muss schnell sein,
	 * deshalb direkt Zugriff mittels sql auf die Datenbank.
	 * 
	 * @param fieldName
	 *            dieses DB-Feld wird ausgelesen
	 * @param orderBy
	 *            sortieren nach diesen Feldern, comma-delimited
	 * @param locale
	 * @return
	 */
	public String[] getLaenderListe(final String fieldName, final String orderBy,
		final String locale){
		// Datenbank anzapfen
		Stm stm = PersistentObject.getConnection().getStatement();
		
		// Bedingung für isos erstellen
		String isoQuery = "";
		// isoQuery = isoQuery + "and iso2   is not null ";
		// isoQuery = isoQuery + "and iso3   is not null ";
		// isoQuery = isoQuery + "and isonum is not null ";
		// isoQuery = isoQuery + "and iso2   != " + JdbcLink.wrap("") + " ";
		// isoQuery = isoQuery + "and iso3   != " + JdbcLink.wrap("") + " ";
		
		// Anzahl Länder-Datensätze ermitteln
		int numOfRows = 0;
		ResultSet rs =
			stm
				.query("select count(*) as cnt from CH_MARLOVITS_COUNTRY where upper(entrylanguage) = '"
					+ locale.toUpperCase() + "' " + isoQuery);
		try {
			rs.next();
			numOfRows = rs.getInt("cnt");
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		
		// Länder für die aktuelle SystemSprache aus der Datenbank-Tabelle "land" einlesen
		String[] tmpStringArray = new String[numOfRows];
		rs =
			stm.query("select " + fieldName
				+ " from CH_MARLOVITS_COUNTRY where upper(entrylanguage) = '"
				+ locale.toUpperCase() + "' " + isoQuery + " order by " + orderBy);
		try {
			int i = 0;
			for (i = 0; i < numOfRows; i++) {
				rs.next();
				tmpStringArray[i] = rs.getString(fieldName);
			}
			// Array arr = rs.getArray(1);
			// System.out.println(arr.toString());
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return tmpStringArray;
	}
	
	public String[][] dataProviderForMCCombo2(final String startOfString){
		String[][] plzStrings = null;
		
		// wenn Feld leer, dann keine Auswahl anzeigen
		if (StringTool.isNothing(startOfString)) {
			return plzStrings;
		}
		
		String[] queryFields = {
			"ort27", "kanton", "plz", "land"
		};
		String[] sortFields = {
			"land", "ort27", "kanton", "plz"
		};
		String queryFieldsString = stringArrayToString(queryFields, ",");
		String sortFieldsString = stringArrayToString(sortFields, ",");
		
		// Anzahl anzuzeigender=abzufragender Felder ist in showFields definiert
		int numOfFields = sortFields.length;
		
		// alle passenden Einträge aus der Datenbank auslesen
		// Datenbank anzapfen - oozapft is...
		Stm stm = PersistentObject.getConnection().getStatement();
		
		// Anzahl Einträge ermitteln, die passen
		int numOfEntries = 0;
		// wenn kein Land ausgewählt ist, dann via sql ALLE Länder abfragen
		String landStr = "CH";
		String landClause = " lower(land) = lower(" + JdbcLink.wrap(landStr) + ") ";
		ResultSet rs =
			stm.query("select count(*) as cnt from " + PlzEintrag.getTableName2() + " where "
				+ landClause + " and lower(ort27) like lower(" + JdbcLink.wrap(startOfString + "%")
				+ ") and plztyp != 80");
		try {
			rs.next();
			numOfEntries = Integer.decode(rs.getString("cnt"));
			rs.close();
		} catch (SQLException exc) {
			System.out.println("SQLException 1 in OrtMCComboDataProvider:");
			exc.printStackTrace();
		}
		// Die einzelnen Einträge abfragen und in einen String-Array und dann in die Liste schreiben
		if (numOfEntries > 0) {
			rs =
				stm.query("select " + queryFieldsString + " from " + PlzEintrag.getTableName2()
					+ " where " + landClause + " and lower(ort27) like lower("
					+ JdbcLink.wrap(startOfString + "%") + ")  and plztyp != 80 order by "
					+ sortFieldsString);
			// plzStrings = new String[numOfFields][numOfEntries];
			// int iii = 0;
			plzStrings = recordSetToStringArray(rs, new int[] {
				0, 1, 2, 3
			});
			// rs.next();
			// for (int ii = 0; ii < numOfFields; ii++) {
			// //Array columnArray = rs.getArray(ii + 1);
			// String[] columnStrings = recordSetToStringArray(rs, ii + 1);
			// //String[] columnStrings = (String[])columnArray.getArray(0, numOfEntries);
			// //String[] columnStrings = columnArray.getArray(Map<String, Class<String>>);
			// plzStrings[ii] = columnStrings;
			// }
			// while (rs.next()) {
			// String[] rowData = new String[numOfFields];
			// for (int showFieldsIx = 0; showFieldsIx < numOfFields; showFieldsIx++) {
			// rowData[showFieldsIx] = rs.getString(showFieldsIx+1);
			// }
			// plzStrings[iii] = rowData;
			// iii++;
			int i = 1;
			// }
		}
		return plzStrings;
	}
	
	public String[][] dataProviderForMarlovitsCombo(final String startOfString){
		String[][] plzStrings = null;
		
		// wenn Feld leer, dann keine Auswahl anzeigen
		if (StringTool.isNothing(startOfString)) {
			return plzStrings;
		}
		
		String[] queryFields = {
			"ort27", "kanton", "plz", "land"
		};
		String[] sortFields = {
			"land", "ort27", "kanton", "plz"
		};
		String queryFieldsString = stringArrayToString(queryFields, ",");
		String sortFieldsString = stringArrayToString(sortFields, ",");
		
		// Anzahl anzuzeigender=abzufragender Felder ist in showFields definiert
		int numOfFields = sortFields.length;
		
		// alle passenden Einträge aus der Datenbank auslesen
		// Datenbank anzapfen - oozapft is...
		Stm stm = PersistentObject.getConnection().getStatement();
		
		// Anzahl Einträge ermitteln, die passen
		int numOfEntries = 0;
		// wenn kein Land ausgewählt ist, dann via sql ALLE Länder abfragen
		String landStr = "CH";
		String landClause = " lower(land) = lower(" + JdbcLink.wrap(landStr) + ") ";
		ResultSet rs =
			stm.query("select count(*) as cnt from " + PlzEintrag.getTableName2() + " where "
				+ landClause + " and lower(ort27) like lower(" + JdbcLink.wrap(startOfString + "%")
				+ ") and plztyp != 80");
		try {
			rs.next();
			numOfEntries = Integer.decode(rs.getString("cnt"));
			rs.close();
		} catch (SQLException exc) {
			System.out.println("SQLException 1 in OrtMCComboDataProvider:");
			exc.printStackTrace();
		}
		// Die einzelnen Einträge abfragen und in einen String-Array und dann in die Liste schreiben
		if (numOfEntries > 0) {
			rs =
				stm.query("select " + queryFieldsString + " from " + PlzEintrag.getTableName2()
					+ " where " + landClause + " and lower(ort27) like lower("
					+ JdbcLink.wrap(startOfString + "%") + ")  and plztyp != 80 order by "
					+ sortFieldsString);
			// plzStrings = new String[numOfFields][numOfEntries];
			// int iii = 0;
			plzStrings = recordSetToStringArray2(rs, new int[] {
				0, 1, 2, 3
			});
			// rs.next();
			// for (int ii = 0; ii < numOfFields; ii++) {
			// //Array columnArray = rs.getArray(ii + 1);
			// String[] columnStrings = recordSetToStringArray(rs, ii + 1);
			// //String[] columnStrings = (String[])columnArray.getArray(0, numOfEntries);
			// //String[] columnStrings = columnArray.getArray(Map<String, Class<String>>);
			// plzStrings[ii] = columnStrings;
			// }
			// while (rs.next()) {
			// String[] rowData = new String[numOfFields];
			// for (int showFieldsIx = 0; showFieldsIx < numOfFields; showFieldsIx++) {
			// rowData[showFieldsIx] = rs.getString(showFieldsIx+1);
			// }
			// plzStrings[iii] = rowData;
			// iii++;
			int i = 1;
			// }
		}
		return plzStrings;
	}
	
	@SuppressWarnings( {
		"unchecked", "null"
	})
	public static String[][] recordSetToStringArray(ResultSet rs, int[] columnIndexes/* 1-based */){
		int numOfColumns = columnIndexes.length;
		LinkedList[] linkedList = new LinkedList[numOfColumns];
		for (int i = 0; i < numOfColumns; i++) {
			linkedList[i] = new LinkedList();
		}
		try {
			while (rs.next()) {
				for (int i = 0; i < numOfColumns; i++) {
					linkedList[i].add(rs.getString(columnIndexes[i] + 1));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		int numOfEntries = linkedList[0].size();
		String[][] strArray = new String[numOfColumns][numOfEntries];
		for (int colIx = 0; colIx < numOfColumns; colIx++) {
			for (int rowIx = 0; rowIx < numOfEntries; rowIx++) {
				strArray[colIx][rowIx] = (String) linkedList[colIx].get(rowIx);
			}
		}
		return strArray;
	}
	
	public static String[][] recordSetToStringArray2(ResultSet rs, int[] columnIndexes/* 1-based */){
		int numOfColumns = columnIndexes.length;
		LinkedList[] linkedList = new LinkedList[numOfColumns];
		for (int i = 0; i < numOfColumns; i++) {
			linkedList[i] = new LinkedList();
		}
		try {
			while (rs.next()) {
				for (int i = 0; i < numOfColumns; i++) {
					linkedList[i].add(rs.getString(columnIndexes[i] + 1));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		int numOfEntries = linkedList[0].size();
		String[][] strArray = new String[numOfEntries][numOfColumns];
		for (int colIx = 0; colIx < numOfColumns; colIx++) {
			for (int rowIx = 0; rowIx < numOfEntries; rowIx++) {
				strArray[rowIx][colIx] = (String) linkedList[colIx].get(rowIx);
			}
		}
		return strArray;
	}
}
