package ch.marlovits.plz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import ch.elexis.Desk;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.data.Query;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.StringTool;

//public class PlzSelectorDialog extends TrayDialog {
public class PlzSelectorDialog extends TrayDialog implements IActivationListener{
	private static final String[] COLUMN_TEXT = {
		"Land",
		"Postleitzahl",
		"Ort",
		"Kanton",
		"Strasse",
		"Kanton_",
		"Kantonkuerzel_",
	};
	private static final int[] COLUMN_WIDTH = {
		40, // Land
		80, // LandISO3
		80, // PLZ
		180, // Ort
		80, // Strasse
		80, // Kanton
		80, // Kantonkürzel
	};
	
	private static final int COL_LAND            = 0;
	private static final int COL_POSTLEITZAHL    = 1;
	private static final int COL_ORT             = 2;
	private static final int COL_KANTON          = 3;
	private static final int COL_STRASSE         = 4;
	private static final int COL_KANTON_         = 5;
	private static final int COL_KANTONKUERZEL_  = 6;
	
	//LabeledInputField liBeleg, liDate, liBetrag;
	private TableViewer plzViewer;
	String		landStr;
	String		plzStr;
	String		ortStr;
	
	Text		text;
	Plz			last, act;
	Combo		cbCats;
	Combo		cbLandCombo;
	Text		landIso2Field;
	Text		plzField;
	Text		strasse;
	Composite	compKanton;
	Combo		cbKantonIso;
	Combo		cbKantonName;
	Composite	compKanton2;
	Combo		cbKantonIso2;
	Combo		cbKantonName2;
	Composite	compKanton3;
	Combo		cbKantonIso3;
	Combo		cbKantonName3;
	String		lang;
	int				numOfRegions;
	Composite[]		compKantonArray = {null, null, null, null, null, null, null, null, null, null};
	Label[]			labelKantonArray = {null, null, null, null, null, null, null, null, null, null};
	Combo[]			comboIsoKantonArray = {null, null, null, null, null, null, null, null, null, null};
	Combo[]			comboNameKantonArray = {null, null, null, null, null, null, null, null, null, null};
	Composite		top;
	String			currLandIso;
	List<PlzEintrag> plzList;
	PlzEintrag		resultPlz;
	Composite			fParent;
	boolean		isOK = false;
	
	/**
	 * Constructor für PlzDialog bei vorhandener Plz (PLZ editieren)
	 * @param shell
	 * @param plz
	 */
	PlzSelectorDialog(Shell shell, final String land, final String plz, final String ort)	{
		super(shell);
		this.landStr	= land;
		this.plzStr		= plz;
		this.ortStr		= ort;
		plzList = null;
	}
	
	/**
	 * Constructor für PlzDialog bei noch nicht vorhandener Plz (PLZ erfassen)
	 * @param shell
	 */
	PlzSelectorDialog(Shell shell){
		super(shell);
	}
	
	public boolean getDoubleClicked()	{
		return isOK;
	}
	
	@Override
	public boolean close() {
		System.out.println("Close called");
		resultPlz = plzList.get(0);
		return super.close();
	}
	
	public PlzEintrag getResult()	{
		return resultPlz;
	}
	
	/**
	 * Constructor für PlzDialog bei noch nicht vorhandener Plz (PLZ erfassen)
	 * @param shell
	 */
	PlzSelectorDialog(Shell shell, List<PlzEintrag> plzList){
		super(shell);
		this.plzList = plzList;
	}
	
	/**
	 * Dialog für die Änderung vorhandener/Eingabe neuer Postleitzahlen
	 */
	@Override // createPartControl createDialogArea
	protected Control createDialogArea(Composite parent){
		fParent = parent;
		FormToolkit tk = Desk.getToolkit();
		//Form form = tk.createForm(parent);
		//form.getBody().setLayout(new GridLayout(1, false));
		
		//TableViewer plzViewer = new TableViewer(form.getBody(), SWT.SINGLE | SWT.FULL_SELECTION);
		plzViewer = new TableViewer(parent, SWT.SINGLE | SWT.FULL_SELECTION);
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
				if (plzList == null)	{
					return getPostleitzahlen().toArray();
				} else {
					return plzList.toArray();
				}
			}
			
			public void dispose(){
				// nothing to do
			}
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
				// nothing to do
			}
		});
		
		/**
		 * setzt das Feld resultPlz
		 */
		plzViewer.addSelectionChangedListener(new ISelectionChangedListener()	{
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				resultPlz = (PlzEintrag) selection.getFirstElement();
				System.out.println("Selection Changed");
				System.out.println(resultPlz.get("Ort27") + "/" + resultPlz.get("Plz"));
			}
		});
		
		/**
		 * schliesst das Fenster
		 */
		plzViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				//Window w = null;
				//processWindowEvent(WindowEvent esss);
				//w.getToolkit().getSystemEventQueue().postEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
				isOK = true;
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				resultPlz = (PlzEintrag) selection.getFirstElement();
				System.out.println("Doubleclick");
				getShell().close();
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
				if (!(element instanceof PlzEintrag)) {
					return "";
				}
				PlzEintrag plz = (PlzEintrag) element;
				String text = "";
				switch (columnIndex) {
				case COL_LAND:
					text = plz.getFieldData("Land");
					break;
				case COL_POSTLEITZAHL:
					text = plz.getFieldData("Plz");
					break;
				case COL_ORT:
					text = plz.getFieldData("Ort27");
					break;
				case COL_KANTON:
					text = plz.getFieldData("Kanton");
					break;
				case COL_STRASSE:
					text = plz.getFieldData("Strasse");
					break;
				case COL_KANTON_:
					text = plz.getFieldData("Sprachcode");
					break;
				case COL_KANTONKUERZEL_:
					text = plz.getFieldData("Gemeindenr");
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
		
		plzViewer.setInput(this);
		
		// und nun der Rückgabewert
		return parent;
	}
	
	
	
	/**
	 * Dialog für die Erfassung neuer PLZ-Daten/Änderung von PLZ-Daten
	 */
	@Override
	public void create(){
		super.create();
		getShell().setText("Auswahl Postleitzahl");
	}
	
	
	@Override
	protected void okPressed(){
		// return oder
		super.okPressed();
	}
	
	
	private List<PlzEintrag> getPostleitzahlen(){
		// Erstellen des Return-Arrays
		List<PlzEintrag> postleitzahlen = new ArrayList<PlzEintrag>();
		
		// Erstellen einer Query auf Plz und alle Datensätze einlesen, sortieren nach ID
		//Query<ch.marlovits.plz.Plz> query = new Query<ch.marlovits.plz.Plz>(ch.marlovits.plz.Plz.class);
		Query<PlzEintrag> query = new Query<PlzEintrag>(PlzEintrag.class);
		query.insertTrue();
		if (!StringTool.isNothing(plzStr))	{
			query.add("Plz", "=", plzStr, true);
		}
		if (!StringTool.isNothing(ortStr))	{
			query.add("Ort27", "like", ortStr + "%", true);
		}
		query.add("Plztyp", "!=", "80", true);
		query.orderBy(false, "Plz");
		List<PlzEintrag> plzList = query.execute();
		
		// Die aus der Datenbank eingelesenen Werte in den Return-Array schreiben
		if (plzList != null) {
			postleitzahlen.addAll(plzList);
		}
		
		// Sortieren der Daten
		Collections.sort(postleitzahlen, new Comparator<PlzEintrag>() {
			// Anfägliche Sortierung nach ID
			public int compare(PlzEintrag plz1, PlzEintrag plz2){
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
				//String sNumber1 = plz1.get("ID");
				//String sNumber2 = plz2.get("ID");
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
	
	public void activation(boolean mode){
		// TODO Auto-generated method stub
		
	}
	
	public void visible(boolean mode){
		// TODO Auto-generated method stub
		
	}
}