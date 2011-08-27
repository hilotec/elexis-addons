package ch.marlovits.plz;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import au.com.bytecode.opencsv.CSVReader;
import ch.elexis.Desk;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.importers.ExcelWrapper;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.rgw.tools.ExHandler;

public class PLZView extends ViewPart implements ISaveablePart2, IActivationListener {
	
	private static final String SRC_ENCODING = "UTF-8";
	public static final String ID = "ch.marlovits.plz.PLZView";
	
	/**
	 * 
	 * Stand per 10.04.2009 für "http://de.wikipedia.org/wiki/ISO-3166-1-Kodierliste"
	 * 
	 * Die HTML-Marker, welche den jeweiligen Anfang/Ende von Datenblöcken markieren
	 * 
	 * Start der Tabelle mit den Land-Iso-Codes;
	 * <table class="wikitable sortable" style="vertical-align:top; width:100%;">
	 * Suche nach:
	 * <table
	 * class="wikitable sortable"
	 * 
	 * Ende der Tabelle mit den Land-Iso-Codes:
	 * </table>
	 * 
	 * dann folgen die Zeilen, jeweils mit Start-Marker
	 * <tr>
	 * und End-Marker
	 * </tr>
	 * 
	 * die erste Zeile ist die Titel-Zeile, wird ausgelassen
	 * 
	 * in jeder Zeile folgen dann jeweils 8 Daten-Zellen
	 */
	private static final String BASE_URL_DE = "http://de.wikipedia.org";
	private static final String URL_DE = "http://de.wikipedia.org/wiki/ISO-3166-1-Kodierliste";
	private static final String WIKI_LAND_STARTINFO_MARKER =
		"<table class=" + "\"" + "wikitable sortable" + "\"";
	private static final String WIKI_LAND_STARTCELLS_MARKER = "<td";
	private static final String WIKI_LAND_ENDINFO_MARKER = "</table>";
	private static final String WIKI_LAND_SKIPDATA_MARKER = "<td><span style";
	
	private static final String WIKI_LAND_SUBISO_STARTMARKER =
		"<table class=\"prettytable sortable\"";
	private static final String WIKI_LAND_SUBISO_ENDMARKER = "</table>";
	
	private static final String TABLEROW_STARTMARKER = "<tr";
	private static final String TABLEROW_ENDMARKER = "</tr>";
	private static final String TABLEDATA_STARTMARKER = "<td>";
	private static final String TABLEDATA_ENDMARKER = "</td>";
	
	private final List<LandEintrag> landIsoEntries = new Vector<LandEintrag>();
	
	// command from org.eclipse.ui
	private static final String COMMAND_COPY = "org.eclipse.ui.edit.copy";
	private static final String COMMAND_DELETE = "org.eclipse.ui.edit.delete";
	
	private FormToolkit tk;
	private Form form;
	private TableViewer plzViewer;
	
	private Action exportToClipboardAction;
	private Action copyAction;
	private Action deleteAction;
	private Action newAction;
	private Action importAction;
	private Action importFromWiki;
	private Action testingAction;
	
	// column indices
	private static final int COL_LAND = 0;
	private static final int COL_LANDISO3 = 1;
	private static final int COL_PLZ = 2;
	private static final int COL_ORT = 3;
	private static final int COL_STRASSE = 4;
	private static final int COL_KANTON = 5;
	private static final int COL_KANTONKUERZEL = 6;
	
	private static final String[] COLUMN_TEXT = {
		"Land", "LandISO3", "PLZ", "Ort", "Strasse", "Kanton", "Kantonkuerzel",
	};
	
	private static final String[] DB_COLUMN_TEXT = {
		"Land", "LandISO3", "Plz", "Ort", "Strasse", "Kanton", "Kantonkuerzel",
	};
	
	private static final int[] COLUMN_WIDTH = {
		80, // Land
		80, // LandISO3
		80, // PLZ
		80, // Ort
		80, // Strasse
		80, // Kanton
		80, // Kantonkürzel
	};
	
	List<ch.marlovits.plz.Plz> getPostleitzahlen(){
		// Erstellen des Return-Arrays
		List<ch.marlovits.plz.Plz> postleitzahlen = new ArrayList<ch.marlovits.plz.Plz>();
		
		// Erstellen einer Query auf Plz und alle Datensätze einlesen, sortieren nach ID
		Query<ch.marlovits.plz.Plz> query =
			new Query<ch.marlovits.plz.Plz>(ch.marlovits.plz.Plz.class);
		query.insertTrue();
		query.orderBy(false, "ID");
		List<ch.marlovits.plz.Plz> plzList = query.execute();
		
		// Die aus der Datenbank eingelesenen Werte in den Return-Array schreiben
		if (plzList != null) {
			postleitzahlen.addAll(plzList);
		}
		
		// Sortieren der Daten
		Collections.sort(postleitzahlen, new Comparator<Plz>() {
			// Anfägliche Sortierung nach ID
			public int compare(Plz plz1, Plz plz2){
				// beide gleich gibt es nicht
				if (plz1 == null && plz2 == null) {
					return 0;
				}
				// plz1 ist null, plz2 nicht. setze plz2 vor plz1
				if (plz1 == null) {
					return 1;
				}
				// plz2 ist null, plz1 nicht. setze plz1 vor plz2
				if (plz2 == null) {
					return -1;
				}
				// beide nicht null
				// String sNumber1 = plz1.get("ID");
				// String sNumber2 = plz2.get("ID");
				try {
					Integer number1 = new Integer(plz1.get("ID"));
					Integer number2 = new Integer(plz2.get("ID"));
					return number1.compareTo(number2);
				} catch (NumberFormatException ex) {
					// error, consider equal
					return 0;
				}
			}
			
			// compare on id
			public boolean equals(Object obj){
				return (this == obj);
			}
		});
		return postleitzahlen;
	}
	
	public void createPartControl(Composite parent){
		parent.setLayout(new FillLayout());
		tk = Desk.getToolkit();
		form = tk.createForm(parent);
		form.getBody().setLayout(new GridLayout(1, false));
		
		// general infos
		Composite generalArea = tk.createComposite(form.getBody());
		generalArea.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		generalArea.setLayout(new GridLayout(2, false));
		
		// Titel setzen
		form.setText("Harrys Postleitzahlen");
		
		// Tabelle erstellen
		plzViewer = new TableViewer(form.getBody(), SWT.SINGLE | SWT.FULL_SELECTION);
		Table table = plzViewer.getTable();
		table.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		tk.adapt(table);
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// Tabellen-Überschriften erstellen
		TableColumn[] tc = new TableColumn[COLUMN_TEXT.length];
		for (int i = 0; i < COLUMN_TEXT.length; i++) {
			tc[i] = new TableColumn(table, SWT.NONE);
			tc[i].setText(COLUMN_TEXT[i]);
			tc[i].setWidth(COLUMN_WIDTH[i]);
		}
		
		// Tabellen-Inhalt
		plzViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement){
				return getPostleitzahlen().toArray();
			}
			
			public void dispose(){
			// nothing to do
			}
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
			// nothing to do
			}
		});
		plzViewer.setLabelProvider(new ITableLabelProvider() {
			public void addListener(ILabelProviderListener listener){
			// nothing to do
			}
			
			public void removeListener(ILabelProviderListener listener){
			// nothing to do
			}
			
			public void dispose(){
			// nothing to do
			}
			
			public String getColumnText(Object element, int columnIndex){
				if (!(element instanceof Plz)) {
					return "";
				}
				
				Plz plz = (Plz) element;
				String text = "";
				
				switch (columnIndex) {
				case COL_LAND:
					text = plz.getFieldData("Land");
					break;
				case COL_LANDISO3:
					text = plz.getFieldData("LandISO3");
					break;
				case COL_PLZ:
					text = plz.getFieldData("Plz");
					break;
				case COL_ORT:
					text = plz.getFieldData("Ort");
					break;
				case COL_STRASSE:
					text = plz.getFieldData("Strasse");
					break;
				case COL_KANTON:
					text = plz.getFieldData("Kanton");
					break;
				case COL_KANTONKUERZEL:
					text = plz.getFieldData("Kantonkuerzel");
					break;
				}
				
				return text;
			}
			
			public Image getColumnImage(Object element, int columnIndex){
				return null;
			}
			
			public boolean isLabelProperty(Object element, String property){
				return false;
			}
		});
		
		plzViewer.setInput(getViewSite());
		
		// Erstellen der Actions für die Menus, etc
		makeActions();
		
		// Erstellen des ViewMenus
		ViewMenus menu = new ViewMenus(getViewSite());
		menu.createMenu(exportToClipboardAction, null, copyAction, null, importFromWiki, null,
			testingAction);
		
		// Erstellen des KontextMenus
		menu.createViewerContextMenu(plzViewer, newAction, null, copyAction, deleteAction, null,
			importAction);
		
		menu.createToolbar(newAction, deleteAction);
		
		GlobalEventDispatcher.addActivationListener(this, this);
		plzViewer.addSelectionChangedListener(GlobalEventDispatcher.getInstance()
			.getDefaultListener());
		
		// Doppelclick öffnen Eingabe-Dialog
		plzViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event){
				IStructuredSelection sel = (IStructuredSelection) plzViewer.getSelection();
				if (!sel.isEmpty()) {
					Plz plz = (Plz) sel.getFirstElement();
					if (new PlzDialog(getSite().getShell(), plz).open() == Dialog.OK) {
						plzViewer.refresh();
					}
				}
			}
		});
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus(){
		plzViewer.getControl().setFocus();
	}
	
	@Override
	public void dispose(){
		GlobalEventDispatcher.removeActivationListener(this, this);
		plzViewer.removeSelectionChangedListener(GlobalEventDispatcher.getInstance()
			.getDefaultListener());
		super.dispose();
	}
	
	/*
	 * SelectionListener methods
	 */

	public void selectionEvent(PersistentObject obj){
	// Plz selectedPlz = (Plz) obj;
	// plzViewer.refresh();
	// setPatient(selectedPatient);
	}
	
	public void clearEvent(Class template){
		plzViewer.refresh();
		// if (template.equals(Patient.class)) {
		// setPatient(null);
		// }
	}
	
	/*
	 * ActivationListener
	 */

	public void activation(boolean mode){
	// nothing to do
	}
	
	public void visible(boolean mode){
		if (mode == true) {
			// ElexisEventDispatcher.addListeners(this); // do we need this?
			
			// Patient patient = GlobalEvents.getSelectedPatient();
			// setPatient(patient);
			plzViewer.refresh();
		} else {
			// ElexisEventDispatcher.emoveListeners(this); // do we need this?
			// setPatient(null);
			plzViewer.refresh();
		}
	};
	
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
	
	private void makeActions(){
		// Alle Postleitzahlen in die Zwischenablage kopieren
		exportToClipboardAction = new Action("Export (Zwischenablage)") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EXPORT));
				setToolTipText("Alle Postleitzahlen in Zwischenablage kopieren");
			}
			
			public void run(){
				exportToClipboard();
			}
		};
		exportToClipboardAction.setActionDefinitionId("exportToClipboardAction");
		GlobalActions.registerActionHandler(this, exportToClipboardAction);
		
		// Ausgewählten PLZ-Eintrag in die Zwischenablage kopieren
		copyAction = new Action("Kopieren") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_CLIPBOARD));
				setToolTipText("Ausgewählten Datensatz in die Zwischenablage kopieren");
			}
			
			public void run(){
			// exportToClipboard();
			}
		};
		copyAction.setActionDefinitionId(COMMAND_COPY);
		GlobalActions.registerActionHandler(this, copyAction);
		
		// Ausgewählten PLZ-Eintrag löschen
		deleteAction = new Action("Löschen...") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_DELETE));
				setToolTipText("Ausgewählte Postleitzahl löschen");
			}
			
			public void run(){
			// TODO
			}
		};
		deleteAction.setActionDefinitionId(COMMAND_DELETE);
		GlobalActions.registerActionHandler(this, deleteAction);
		
		// Erstellen eines neuen PLZ-Eintrages
		newAction = new Action("Neue Postleitzahl...") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
				setToolTipText("Neue Postleitzahl erfassen");
			}
			
			public void run(){
				// new PlzDialog(getSite().getShell(), true).open();
				if (new PlzDialog(getSite().getShell()).open() == Dialog.OK) {
					plzViewer.refresh();
				}
			}
		};
		newAction.setActionDefinitionId("NEUE_POSTLEITZAHL");
		GlobalActions.registerActionHandler(this, newAction);
		
		// Importieren von PLZ-Definitionen aus einer csv-Datei
		importAction = new Action("Importieren...") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Importieren von Postleitzahlen aus Dateien");
			}
			
			public void run(){
				try {
					doImport();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		newAction.setActionDefinitionId("PLZ_IMPORT");
		GlobalActions.registerActionHandler(this, importAction);
		
		// Importieren von Ländern und Regionen aus Wikipedia
		importFromWiki = new Action("Landdaten importieren aus Wiki...") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Importieren der Land- und Regions-Daten aus Wikipedia");
			}
			
			public void run(){
				try {
					extractLandData("de");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		newAction.setActionDefinitionId("WIKI_LAND_REGION_IMPORT");
		GlobalActions.registerActionHandler(this, importFromWiki);
		
		// Importieren von Ländern und Regionen aus Wikipedia
		testingAction = new Action("Testing...") {
			{
				setToolTipText("Aufruf der Testroutine");
			}
			
			public void run(){
				try {
					testingAction();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		testingAction.setActionDefinitionId("CH_MARLOVITS_PLU_TESTINGACTION");
		GlobalActions.registerActionHandler(this, testingAction);
	}
	
	public void testingAction(){
		OpenGeoDB openGeoDB = new OpenGeoDB();
		openGeoDB.createOpenGeoDBTables();
	}
	
	/**
	 * Exportiert alle Postleitzahlen in die Zwischenablage. Format: tab-delimited, die erste Zeile
	 * enthält die Feldnamen
	 * 
	 * @param -
	 * @return -
	 */
	private void exportToClipboard(){
		String clipboardText = "";
		String lineSeparator = System.getProperty("line.separator");
		
		List<Plz> plzs = Plz.getShownPostleitzahlen();
		StringBuffer sbTable = new StringBuffer();
		StringBuffer sbHeader = new StringBuffer();
		
		// Tabellen-Überschriften erstellen
		String delimiter = "";
		for (int i = 0; i < COLUMN_TEXT.length; i++) {
			sbHeader.append(delimiter);
			sbHeader.append(COLUMN_TEXT[i]);
			delimiter = "\t";
		}
		sbHeader.append(lineSeparator);
		sbTable.append(sbHeader);
		
		// Tabellen-Inhalt einlesen
		for (Plz plz : plzs) {
			delimiter = "";
			StringBuffer sbLine = new StringBuffer();
			for (int i = 0; i < COLUMN_TEXT.length; i++) {
				sbLine.append(delimiter);
				sbLine.append(plz.getFieldData(DB_COLUMN_TEXT[i]));
				delimiter = "\t";
			}
			sbLine.append(lineSeparator);
			sbTable.append(sbLine);
		}
		clipboardText = sbTable.toString();
		
		// Daten ins Clipboard kopieren
		Clipboard clipboard = new Clipboard(Desk.getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		Transfer[] transfers = new Transfer[] {
			textTransfer
		};
		Object[] data = new Object[] {
			clipboardText
		};
		clipboard.setContents(data, transfers);
		clipboard.dispose();
	}
	
	public void doImport(){
		FileDialog fd = new FileDialog(getSite().getShell(), SWT.OPEN);
		fd.setFilterExtensions(new String[] {
			"*.csv", "*.xls", "*.*"
		});
		fd.setFilterNames(new String[] {
			"Comma Separated Values", "Excel-Dateien", "Alle Dateien"
		});
		String fileName = fd.open();
		if (fileName == null) {
			return;
		}
		if (fileName.endsWith(".xls")) {
			importExcel(fileName);
		} else if (fileName.endsWith(".csv")) {
			importCSV(fileName);
		} else {
			// TODO
		}
	}
	
	/*
	 * ///// FileDialog fd = new FileDialog(getSite().getShell(),SWT.OPEN);
	 * fd.setFilterExtensions(new String[]{"*.csv", "*.xls", "*.*"}); fd.setFilterNames(new
	 * String[]{"Comma Separated Values", "Excel-Dateien", "Alle Dateien"}); String fileName =
	 * fd.open(); if (fileName == null) { return; } InputStreamReader isr=new InputStreamReader(new
	 * FileInputStream(fileName),SRC_ENCODING); CSVReader reader = new CSVReader(isr); String []
	 * line; //monitor.subTask("Postleitzahlen einlesen"); while ((line = reader.readNext()) !=
	 * null) { // TODO hier könnte man noch ggf ; durch , ersetzen (Excel!) //line =
	 * StringTool.convertEncoding(line, SRC_ENCODING); new Plz(line[0], line[1], line[2], line[3],
	 * line[4], line[5], line[6]); //monitor.worked(1); } //monitor.done(); }
	 */
	private void importExcel(final String file){
		ExcelWrapper xl = new ExcelWrapper();
		if (!xl.load(file, 0)) {
			return;
		}
		for (int i = xl.getFirstRow(); i <= xl.getLastRow(); i++) {
			List<String> row = xl.getRow(i);
			// importLine(row.toArray(new String[0]));
			String[] line;
			line = row.toArray(new String[0]);
			// line = StringTool.convertEncoding(line, SRC_ENCODING);
			new Plz(line[0], line[1], line[2], line[3], line[4], line[5], line[6]);
		}
		return;
	}
	
	private void importCSV(final String file){
		try {
			CSVReader cr = new CSVReader(new FileReader(file));
			String[] line;
			while ((line = cr.readNext()) != null) {
				// importLine(line);
				// TODO hier könnte man noch ggf ; durch , ersetzen (Excel -> csv !)
				// line = StringTool.convertEncoding(line, SRC_ENCODING);
				new Plz(line[0], line[1], line[2], line[3], line[4], line[5], line[6]);
			}
			return;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return;
		}
		
	}
	
	/***************************************************************************/
	/***************************************************************************/
	/***************************************************************************/
	/**
	 * Liest Inhalt einer ganzen Seite einer übergebenen URL
	 * 
	 * @param url
	 *            : die einzulesende URL
	 * @return die ganze HTML-Seite als String
	 */
	public static String readHTMLPage(final String url) throws IOException, MalformedURLException{
		URL content = new URL(url);
		InputStream input = content.openStream();
		StringBuffer sb = new StringBuffer();
		int count = 0;
		char[] c = new char[10000];
		InputStreamReader isr = new InputStreamReader(input);
		try {
			while ((count = isr.read(c)) > 0) {
				sb.append(c, 0, count);
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}
		// TO DO: clean up ist nicht ok/nicht vollständig
		return cleanupUmlaute(cleanupText(sb.toString()));
	}
	
	private static String cleanupText(String text){
		text = text.replace("</nobr>", "").replace("<nobr>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		text = text.replace("&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("<b class=\"searchWords\">", ""); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("</b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace((char) 160, ' '); // Spezielles Blank Zeichen wird
		// ersetzt
		return text;
	}
	
	private static String cleanupUmlaute(String text){
		text = text.replace("&#xE4;", "ä");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xC4;", "Ä");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xF6;", "ö");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xD6;", "Ö");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xFC;", "ü");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xDC;", "Ü");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xE8;", "è");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xE9;", "é");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xEA;", "ê");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xE0;", "à");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xA0;", " ");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("%C3%9F", "ss");//$NON-NLS-1$ //$NON-NLS-2$
		
		return text;
	}
	
	/**
	 * Extrahieren der Iso-Länderdaten aus Wikipedia
	 * 
	 * @param language
	 *            : die jeweilige Sprache, für welche die Daten extrahiert werden sollen, die URL
	 *            wird entsprechend gewählt
	 */
	private void extractLandData(final String language){
		// Einlesen der ganzen Seite in der gewünschten Sprache in die Variable wholeHTMLPage
		String wholeHTMLPage = null;
		try {
			// TO DO: abhängig von language
			wholeHTMLPage = readHTMLPage(URL_DE);
		} catch (MalformedURLException e) {
			SWTHelper.alert("Fehler", "Die URL '" + URL_DE + "' ist nicht korrekt formatiert.");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			SWTHelper.alert("Fehler", "Die URL '" + URL_DE + "' kann nicht eingelesen werden.");
			e.printStackTrace();
		}
		
		// Tabellen-Inhalt aus Text extrahieren
		int tableStartPos = wholeHTMLPage.indexOf(WIKI_LAND_STARTINFO_MARKER, 0);
		int tableContentStartPos = wholeHTMLPage.indexOf(TABLEROW_STARTMARKER, tableStartPos);
		int tableDataStartPos = wholeHTMLPage.indexOf(TABLEROW_STARTMARKER, tableStartPos);
		int tableEndPos = wholeHTMLPage.indexOf(WIKI_LAND_ENDINFO_MARKER, tableDataStartPos);
		String landTableContent = wholeHTMLPage.substring(tableContentStartPos, tableEndPos);
		
		// durch die Tabelle loopen und die einzelnen Datensätze einlesen
		int currRowPos = 0;
		int nextRowPos = 0;
		String rowData = "";
		int i = 0;
		while ((currRowPos != -1)) {
			nextRowPos =
				landTableContent.indexOf(TABLEROW_STARTMARKER, currRowPos
					+ TABLEROW_STARTMARKER.length());
			rowData = landTableContent.substring(currRowPos, nextRowPos);
			currRowPos = nextRowPos;
			extractLandRowData(rowData, language);
			i++;
		}
	}
	
	/**
	 * Extrahieren der einzelnen Zeilen aus der Iso-Länder-HTML-Tabelle
	 * 
	 * @param rowData
	 *            : HTML-Inhalt einer Zeile, ohne enclosing
	 *            <tr>
	 *            </tr>
	 * @param language
	 *            : die Sprache dieses Eintrages
	 */
	private void extractLandRowData(final String rowData, final String language){
		String landName;
		String landWikiLink;
		String landIso2;
		String landIso3;
		String landIsoNum;
		String landTld;
		String landIoc;
		String landIso3166_2;
		
		// initialisieren der Zähler, etc
		int currDataPos = 0;
		int nextDataPos = 0;
		String cellData;
		String subContentHTML = null;
		String[] linkAndText;
		
		// die erste Zelle enthält den Namen des Landes und dessen Wikipedia-Link
		currDataPos =
			rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos
				+ WIKI_LAND_STARTCELLS_MARKER.length());
		if (currDataPos == -1) {
			return; // wenn kein <td vorhanden ist, dann ist es ein header - skip
		}
		nextDataPos =
			rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos
				+ WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = rowData.substring(currDataPos, nextDataPos);
		String theString = cellData.substring(0, WIKI_LAND_SKIPDATA_MARKER.length());
		if (theString.equals(WIKI_LAND_SKIPDATA_MARKER)) {
			return; // wenn die erste Zelle den WIKI_LAND_SKIPDATA_MARKER enthält, dann überspringen
		}
		cellData = extractCellData(cellData);
		linkAndText = splitHyperlinkCell(cellData);
		landName = linkAndText[1];
		landWikiLink = BASE_URL_DE + linkAndText[0];
		currDataPos = nextDataPos;
		
		// Alpha 2
		nextDataPos =
			rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos
				+ WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landIso2 = left(cellData, 2);
		
		// Alpha 3
		nextDataPos =
			rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos
				+ WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landIso3 = left(cellData, 3);
		
		// Numeric
		nextDataPos =
			rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos
				+ WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landIsoNum = left(cellData, 3);
		
		// TopLevelDomain
		nextDataPos =
			rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos
				+ WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landTld = left(cellData, 3);
		
		// IOC
		nextDataPos =
			rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos
				+ WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		currDataPos = nextDataPos;
		landIoc = left(cellData, 3);
		
		// ISO3166-2: enthält den Iso2 und dessen Wikipedia-Link zur Seite mit den Sub-Infos
		nextDataPos =
			rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos
				+ WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		linkAndText = splitHyperlinkCell(cellData);
		// System.out.println(BASE_URL_DE + linkAndText[0]); // link
		currDataPos = nextDataPos;
		landIso3166_2 = left(linkAndText[1], 2);
		
		// Erstellen eines neuen Eintrages in der Tabelle CH_MARLOVITS_LAND
		new LandEintrag(landName, landIso2, landIso3, landIsoNum, landTld, landIoc, landIso3166_2,
			landWikiLink, language);
		
		// Einlesen der Informationen aus den Unter-Seiten
		subContentHTML = null;
		
		// if (landIso2.equals("éé")) {
		
		try {
			String combinedURL = BASE_URL_DE + linkAndText[0];
			subContentHTML = readHTMLPage(combinedURL);
			extractSubIsos(subContentHTML, language);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// }
	}
	
	/**
	 * Entfernt die enclosing <td>/</td>
	 * 
	 * @param cellDataWithEnclosings
	 * @return input ohne enclosing <td>/</td>
	 */
	private String extractCellData(final String cellDataWithEnclosings){
		String cellContents = null;
		
		// fast exit, falls leer
		if ((cellDataWithEnclosings == null) || (cellDataWithEnclosings == "")) {
			return cellContents;
		}
		int contentStart = cellDataWithEnclosings.indexOf(TABLEDATA_STARTMARKER, 0);
		if (contentStart != -1) {
			// has <td>[Content]</td>
			int contentEnd =
				cellDataWithEnclosings.indexOf(TABLEDATA_ENDMARKER, TABLEDATA_STARTMARKER.length());
			cellContents =
				cellDataWithEnclosings.substring(contentStart + TABLEDATA_STARTMARKER.length(),
					contentEnd);
		} else {
			// has <td [...]> [Content] </td>
			// TO DO
			Pattern pattern = Pattern.compile("<td.*>");
			Matcher matcher = pattern.matcher(cellDataWithEnclosings);
			cellContents = matcher.replaceAll("");
			pattern = Pattern.compile("</td>");
			matcher = pattern.matcher(cellContents);
			cellContents = matcher.replaceAll("");
		}
		return cellContents;
	}
	
	/**
	 * Extrahiert aus einem HTML-String mit einem href den ersten Link und den reinen Text-Teil
	 * 
	 * @param htmlString
	 *            : html-String mit href-Teil
	 * @return String[]: {linkPart, textPart}
	 */
	private String[] splitHyperlinkCell(final String htmlString){
		// Initialisieren
		String linkPart = "";
		String textPart = "";
		final String hrefStartMarker = "<a href=\"";
		final String hrefEndMarker = "\">";
		
		// leerer String - fast exit
		if ((htmlString == null) || (htmlString.equals(""))) {
			linkPart = "";
			textPart = htmlString;
			return new String[] {
				linkPart, textPart
			};
		}
		
		// es wird nur der erste Link extrahiert
		int hrefStart = htmlString.indexOf(hrefStartMarker, 0);
		
		// es gibt keinen Link
		if (hrefStart == -1) {
			linkPart = "";
			textPart = htmlString;
			return new String[] {
				linkPart, textPart
			};
		}
		
		// Teil links des href gehört zum Text-Teil
		textPart = textPart + htmlString.substring(0, hrefStart);
		
		// href extrahieren
		int hrefEnd = htmlString.indexOf(hrefEndMarker, hrefStart) + hrefEndMarker.length();
		String hrefPart = htmlString.substring(hrefStart, hrefEnd);
		
		// Link aus href extrahieren
		linkPart = extractHrefLink(hrefPart);
		
		// Text rechts des hrefs gehört zum Text-Teil, es müssen alle </a> entfernt werden
		textPart =
			textPart + htmlString.substring(hrefEnd, htmlString.length()).replace("</a>", "");
		
		// jetzt werden alle restlichen HTML-Tags <XXX> </XXX> entfernt
		textPart = stripHTMLTags(textPart);
		
		// returns, etc, entfernen
		textPart = replaceReturns(textPart, " ");
		
		// Rückgabe
		return new String[] {
			linkPart, textPart
		};
	}
	
	/**
	 * 
	 * @param href
	 *            : voller href, aus welchem der Link extrahiert werden soll
	 * @return der Link, der in href vorhanden ist
	 */
	private String extractHrefLink(final String href){
		// Initialisieren
		final String hrefLinkStartMarker = "<a href=\"";
		final String hrefLinkEndMarker = "\"";
		
		// Start des href suchen
		int hrefLinkStart = href.indexOf(hrefLinkStartMarker, 0);
		if (hrefLinkStart == -1) {
			return "";
		}
		// Ende des href suchen
		hrefLinkStart = hrefLinkStart + hrefLinkStartMarker.length();
		int hrefLinkEnd = href.indexOf(hrefLinkEndMarker, hrefLinkStart);
		
		// extrahieren, Rückgabe
		return href.substring(hrefLinkStart, hrefLinkEnd);
	}
	
	/**
	 * Extrahieren der Daten aus den Info-Seiten für die Sub-Isos "ISO 3166-2". Es können mehrere
	 * Tabellen mit den Infos vorhanden sein!
	 * 
	 * @param pageHTML
	 * @author Harald Marlovits
	 */
	private void extractSubIsos(final String pageHTML, final String language){
		int startOfTable = pageHTML.indexOf(WIKI_LAND_SUBISO_STARTMARKER, 0);
		int endOfTable = 0;
		String subTableContent = null;
		// alle vorhandenen Tabellen durchlaufen
		int subTableIndex = 0;
		while (startOfTable != -1) {
			endOfTable = pageHTML.indexOf(WIKI_LAND_SUBISO_ENDMARKER, startOfTable);
			startOfTable = pageHTML.indexOf(TABLEROW_STARTMARKER, startOfTable);
			subTableContent = pageHTML.substring(startOfTable, endOfTable);
			startOfTable = pageHTML.indexOf(WIKI_LAND_SUBISO_STARTMARKER, startOfTable);
			System.out.println("**************************************");
			// System.out.print(subTableContent);
			extractSubCellTableData(subTableContent, subTableIndex, language);
			subTableIndex++;
		}
	}
	
	private void extractSubCellTableData(final String tableData, final int subTableIndex,
		final String language){
		// durch die Tabelle loopen und die einzelnen Datensätze einlesen
		int currRowPos = 0;
		int nextRowPos = 0;
		String rowData = "";
		
		int i = 0;
		
		String headerMarker = "<th";
		
		// calc the number of columns for this table
		// always need the first (iso name) and the last column (iso-code)
		// if > 2 columns, then concat second column in parenthesis to first column
		
		String currName = "";
		
		int columnCount = 0;
		while (currRowPos != -1) {
			nextRowPos =
				tableData.indexOf(TABLEROW_STARTMARKER, currRowPos + TABLEROW_STARTMARKER.length());
			rowData =
				tableData.substring(currRowPos, (nextRowPos == -1) ? tableData.length()
						: nextRowPos);
			currRowPos = nextRowPos;
			System.out.println("********************");
			// Anzahl Spalten ermitteln
			if (columnCount == 0) {
				String regex = "<t[dh]>";
				Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(rowData);
				while (m.find()) {
					columnCount++;
				}
			}
			// Inhalt extrahieren, <tr> / </tr> entfernen
			Pattern pattern = Pattern.compile("<tr.*>");
			Matcher matcher = pattern.matcher(rowData);
			rowData = matcher.replaceAll("");
			pattern = Pattern.compile("</tr>");
			matcher = pattern.matcher(rowData);
			rowData = matcher.replaceAll("");
			int startOfContent = rowData.indexOf("<t");
			rowData = rowData.substring(startOfContent);
			
			// wenn <th> dann enthält die Zelle den Namen von Kanton/District/Region, etc
			int headerMarkerStart = rowData.indexOf(headerMarker, 0);
			if (headerMarkerStart != -1) {
				headerMarkerStart = rowData.indexOf(">", headerMarkerStart) + 1;
				int endMarker = rowData.indexOf("</t", headerMarkerStart);
				currName = rowData.substring(headerMarkerStart, endMarker);
				System.out.println(currName);
			} else {
				exctractSubCellRowData(rowData, currName, subTableIndex, language, columnCount);
			}
			i++;
		}
	}
	
	/**
	 * Entfernt aus einem String alle tabs/returns/newlines/formsfeeds
	 * 
	 * @param inputString
	 *            : zu bearbeitender String
	 * @param replacement
	 *            : damit werden die gefundenen Vorkommen ersetzt
	 * @return inputString, von welchem alle whiteSpaces weggestrippt sind
	 */
	private String replaceReturns(final String inputString, final String replacement){
		String tmp = inputString;
		tmp = tmp.replaceAll("\\t", replacement);
		tmp = tmp.replaceAll("\\n", replacement);
		tmp = tmp.replaceAll("\\f", replacement);
		tmp = tmp.replaceAll("\\r", replacement);
		return tmp;
	}
	
	/**
	 * Entfernt aus einem String alle HTML-Tags <XXX> und </XXX>
	 * 
	 * @param inputString
	 * @return inputString, von welchem alle whiteSpaces weggestrippt sind
	 */
	private String stripHTMLTags(final String inputString){
		String tmp = inputString;
		tmp = tmp.replaceAll("</.*>", "");
		tmp = tmp.replaceAll("<.*>", "");
		return tmp;
	}
	
	/**
	 * entfernen von: leading spaces, trailing spaces und mehrfach-Spaces im String
	 * 
	 * @param source
	 *            : zu bearbeitender String
	 * @return gestrippter String
	 */
	public static String fullTrim(final String source){
		String tmp = source;
		// leading Spaces strippen
		tmp = tmp.replaceAll("^\\s+", "");
		// trailing Spaces strippen
		tmp = tmp.replaceAll("\\s+$", "");
		// mehrfach-Spaces durch einfachen Space ersetzen
		tmp = tmp.replaceAll("\\s+", " ");
		return tmp;
	}
	
	private void exctractSubCellRowData(final String rowData, final String kantonName,
		final int subTableIndex, final String language, final int columnCount){
		// Die erste Spalte enthält die zu benutzende Bezeichnung
		// die letzte Spalte enthält den ISO-Code
		// falls zweite Spalte vorhanden, dann in Klammern an Spalte 1 anfügen
		
		String localData = rowData;
		// die Endmarker werden immer allesamt entfernt
		localData = localData.replace("</td>", "");
		// erstelle String Array, split on <td>
		String[] cellDataStringArray = localData.split("<td>");
		
		String nameRow = replaceReturns(cellDataStringArray[1], " ");
		String codeRow = replaceReturns(cellDataStringArray[columnCount], " ");
		
		// nameRow: enthält den Link zu Sub-Info und den Iso-Namen, splitten
		String[] landLink_landName = splitHyperlinkCell(nameRow);
		
		// codeRow: nur Text links von Space ist gültig, Rest wegstrippen
		codeRow = codeRow.split(" ")[0];
		// codeRow: splitten auf "-", links ist Iso2 des Landes, rechts Iso des "Kantons"
		String[] landIso_SubIso = codeRow.split("-");
		
		// falls mehr als 2 Spalten, dann 2. Spalte in Klammern an den Namen anhängen
		String kantonname = landLink_landName[1];
		kantonname = replaceReturns(kantonname, "");
		if (columnCount > 2) {
			kantonname =
				kantonname + " (" + stripHTMLTags(replaceReturns(cellDataStringArray[2], "")) + ")";
			kantonname = fullTrim(kantonname);
		}
		
		// die Werte zusammentragen
		String kantonfullcode = codeRow;
		String kantonsubcode = landIso_SubIso[1];
		String kantonland = landIso_SubIso[0];
		String kantonindex = "" + subTableIndex;
		String kantonkind = kantonName;
		String kantonwikilink = BASE_URL_DE + landLink_landName[0];
		String kantonlanguage = language;
		
		// debug
		System.out.println("kantonname:     " + kantonname);
		System.out.println("kantonfullcode: " + kantonfullcode);
		System.out.println("kantonsubcode:  " + kantonsubcode);
		System.out.println("kantonland:     " + kantonland);
		System.out.println("kantonindex:    " + kantonindex);
		System.out.println("kantonkind:     " + kantonkind);
		System.out.println("kantonwikilink: " + kantonwikilink);
		System.out.println("kantonlanguage: " + kantonlanguage);
		
		// den Eintrag erstellen
		new KantonEintrag(kantonname, kantonfullcode, kantonsubcode, kantonland, kantonindex,
			kantonkind, kantonwikilink, kantonlanguage);
	}
	
	/**
	 * Rückgabe des linken Anteils des Eingabe-Strings
	 * 
	 * @param input
	 *            : String, dessen linker Teil zurückgegeben werden soll
	 * @param count
	 *            : Anzahl Zeichen, die zurückgegeben werden sollen
	 * @return gestrippter String oder "", wenn input null oder leer
	 */
	private String left(final String input, final int count){
		if ((input == null) || (input.equals(""))) {
			return "";
		} else {
			return input.substring(0, count);
		}
	}
	
}