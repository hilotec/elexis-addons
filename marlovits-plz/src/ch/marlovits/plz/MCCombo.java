/*******************************************************************************
 * Copyright (c) 2009 Harald Marlovits.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Harald Marlovits	 - initial implementation
 *                         initially based on CCombo, IBM, see below
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


package ch.marlovits.plz;

import java.awt.MouseInfo;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

public class MCCombo extends Composite {
	static final int ITEMS_SHOWING = 5;
	
	private boolean		selected;			// pressed enter/esc/tab for selection
	private Text        text;
	private Shell       popup;
	private boolean     hasFocus;
	private String      theText;
	private String      oldText;
	private TableViewer	tableViewer;
	private Table		table;
	private String[]	showFields   = null;  // shown fields in table (database fields)
	                                          // the fields are used for querying from the db
	                                          // if queryFields is not null, then queryFields is
	                                          // used for querying
	private String[]	queryFields  = null;  // used for querying from the db (database fields)
	                                          // if more complex queries are needed, eg:
	                                          //    'a' || databasefield as showFieldName1
	                                          // 
	private String[]	sortFields   = null;  // sort  fields in table, must be fields from showFields
	private Object[]	returnFields = null;  // fields into which data should be written
	                                          // same number of items as in showFields, same order
	                                          // must be fields from showFields
	private MCComboDataProvider dataProviderClass;
	boolean isTrackingTable = false;
	TestListener tempMouseListener = new TestListener();
	
	// test caller
	public void callDataProvider()	{
		String[][] tmp = dataProviderClass.mCComboDataProvider(this);
		if (tmp == null){
			dropDown(false);
		} else	{
			setItems(tmp);
			if (table.getItemCount() > 0)	{
				dropDown(true);
			} else {
				dropDown(false);
			}
		}
	}
	public void installDataProvider(MCComboDataProvider pDataProviderClass)	{
		dataProviderClass = pDataProviderClass;
	}
	// this is the interface which must be implemeted as a real class
	public interface MCComboDataProvider	{
	    public String[][] mCComboDataProvider (MCCombo lThis);
	}
	public void setReturnFields(Object[] returnFields)	{
		this.returnFields = returnFields;
	}
	public Object[] getReturnFields()	{
		return returnFields;
	}
	public void setQueryFields(String[] queryFields)	{
		this.queryFields = queryFields;
	}
	public String[] getQueryFields()	{
		return queryFields;
	}
	public void setShowFields(String[] showFields)	{
		this.showFields = showFields;
	}
	public String[] getShowFields()	{
		return showFields;
	}
	public void setSortFields(String[] sortFields)	{
		this.sortFields = sortFields;
	}
	public String[] getSortFields()	{
		return sortFields;
	}
	public MCCombo(Composite parent, int style) {
		super(parent, checkStyle(style));
		
		style = getStyle();
		
		// das Textfeld erstellen ***************************************************
		int textStyle = SWT.SINGLE;
		if ((style & SWT.READ_ONLY) != 0) textStyle |= SWT.READ_ONLY;
		if ((style & SWT.FLAT) != 0) textStyle |= SWT.FLAT;
		text = new Text(this, textStyle);
		
		// die Shell erstellen, in welcher die Liste/Tabelle erstellt wird **********
		//popup = new Shell(getShell(), SWT.NO_TRIM);
		popup = new Shell(getShell(), SWT.NO_TRIM | SWT.TOOL);
		
		int listStyle = SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION;
		
		if ((style & SWT.FLAT) != 0) listStyle |= SWT.FLAT;
		if ((style & SWT.RIGHT_TO_LEFT) != 0) listStyle |= SWT.RIGHT_TO_LEFT;
		if ((style & SWT.LEFT_TO_RIGHT) != 0) listStyle |= SWT.LEFT_TO_RIGHT;
		
		// meine eigene Popup-Liste mit mehreren Spalten erstellen in popup *********
		tableViewer = new TableViewer(popup, listStyle);
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.verticalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		
		tableViewer.getControl().setLayoutData(gd);
		table = tableViewer.getTable();
		table.setHeaderVisible(false);
		table.setLinesVisible(false);
		
		selected = false;
		
		// die Listener installieren ************************************************
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				if (popup == event.widget) {
					//System.out.println("event.widget == popup, calling popupEvent(event)");
					popupEvent(event);
					return;
				}
				if (text == event.widget) {
					//System.out.println("event.widget == text");
					textEvent(event);
					return;
				}
				if (table == event.widget) {
					//System.out.println("event.widget == list, calling listEvent(event)");
					listEvent(event);
					return;
				}
				if (MCCombo.this == event.widget) {
					//System.out.println("event.widget == MyCComboSaved2, calling comboEvent(event)");
					comboEvent(event);
					return;
				}
//				if (table.getColumn(0) == event.widget) {
//					System.out.println("event.widget == table.getColumn(0), calling columnEvent(event)");
//					columnEvent(event);
//					return;
//				}
				System.out.println("other: event.widget == " + event.widget);
			}
		};
		
		int [] comboEvents = {SWT.Dispose, SWT.Move, SWT.Resize, SWT.DRAG, SWT.DragDetect, SWT.Traverse};
		for (int i=0; i<comboEvents.length; i++) this.addListener(comboEvents [i], listener);
		
		int [] popupEvents = {SWT.Close, SWT.Paint, SWT.Deactivate, SWT.MouseMove, SWT.MouseExit};
		for (int i=0; i<popupEvents.length; i++) popup.addListener(popupEvents [i], listener);
		
		int [] textEvents = {SWT.KeyDown, SWT.KeyUp, SWT.Modify, SWT.MouseDown, SWT.MouseUp, SWT.Traverse, SWT.FocusIn, SWT.FocusOut, SWT.DEL};
		for (int i=0; i<textEvents.length; i++) text.addListener(textEvents [i], listener);
		
		int [] listEvents = {SWT.MouseUp, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.FocusOut, SWT.MouseMove, SWT.DRAG, SWT.NONE, SWT.MouseDown, SWT.MouseHover, SWT.MouseExit, SWT.MouseEnter};
		for (int i=0; i<listEvents.length; i++) table.addListener(listEvents [i], listener);
		
//		int [] tableViewerEvents = {SWT.MouseUp, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.FocusOut, SWT.MouseMove, SWT.DRAG, SWT.NONE, SWT.MouseDown, SWT.MouseHover, SWT.MouseExit};
//		for (int i=0; i<tableViewerEvents.length; i++) table.getColumn(0).addListener(tableViewerEvents [i], listener);
		
		initAccessible();
	}
/*
	void columnEvent(Event event) {
		switch(event.type) {
			case SWT.MouseUp:
				System.out.println("MouseUp");
				break;
			case SWT.Selection:
				System.out.println("Selection");
				break;
			case SWT.Traverse:
				System.out.println("KeyUp");
				break;
			//case SWT.KeyDown:
			//	break;
			case SWT.KeyUp:
				System.out.println("KeyUp");
				break;
			case SWT.FocusIn:
				System.out.println("FocusIn");
				break;
			case SWT.FocusOut:
				System.out.println("FocusOut");
				break;
			case SWT.MouseMove:
				System.out.println("MouseMove");
				break;
			case SWT.DRAG:
				System.out.println("DRAG");
				break;
			case SWT.NONE:
				System.out.println("NONE");
				break;
			case SWT.MouseDown:
				System.out.println("MouseDown");
				break;
			case SWT.MouseHover:
				System.out.println("MouseHover");
				break;
			case SWT.MouseExit:
				System.out.println("MouseExit");
				break;
			default:
				System.out.println("other");
				//textEvent(event);
				break;
		}
	}

*/	
	public Table getList()	{
		return table;
	}
	public TableViewer getTableViewer()	{
		return tableViewer;
	}
	public Table getTable()	{
		return table;
	}
	public void setListVisible(boolean visibility)	{
		popup.setVisible(visibility);
	}
	public void setTableViewerVisible(boolean visibility)	{
		popup.setVisible(visibility);
		}
	public void setTableVisible(boolean visibility)	{
		table.setVisible(visibility);
		}
	static int checkStyle(int style) {
		int mask = SWT.BORDER | SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		return style & mask;
	}
	//TODO
	/*
	public void add(String string) {
		checkWidget();
		if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		list.add(string);
	}
	*/
	//TODO
	/*
	public void add(String string, int index) {
		checkWidget();
		if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		list.add(string, index);
	}
	*/
	public void addModifyListener(ModifyListener listener) {;
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Modify, typedListener);
	}
	public void addSelectionListener(SelectionListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Selection,typedListener);
		addListener(SWT.DefaultSelection,typedListener);
	}
	public void clearSelection() {
		checkWidget();
		text.clearSelection();
		table.deselectAll();
	}
	void comboEvent(Event event) {
		switch(event.type) {
			case SWT.Dispose:
				if (popup != null && !popup.isDisposed()) popup.dispose();
				popup = null;  
				text = null;  
				tableViewer = null;
				break;
			case SWT.Move:
				//System.out.println("SWT.Move");
				dropDown(false);
				break;
			case SWT.Resize:
				internalLayout();
				break;
//			default:
//				text.setFocus();
//				//textEvent(event);
//				break;
//			case SWT.Traverse:
//				System.out.println("comboEvent Traverse");
//				break;
		}
	}
	public Point computeSize(int wHint, int hHint, boolean changed) {
		Point listSize = new Point(0, 0);
		checkWidget();
		int width = 0, height = 0;
		Point textSize  = text.computeSize(wHint, SWT.DEFAULT, changed);
		resizeColums();
		//listSize  = table.computeSize(wHint, SWT.DEFAULT, changed);
		listSize.x = table.getBorderWidth();
		listSize.y = table.getItemHeight() * ITEMS_SHOWING;
		int borderWidth = getBorderWidth();
		
		height = Math.max(hHint, textSize.y  + 2*borderWidth);
		//width = Math.max(wHint, Math.max(textSize.x + 2*borderWidth, listSize.x + 2)  );
		width = 2*borderWidth + 2;
		return new Point(width, height);
	}
	public void deselect(int index) {
		checkWidget();
		table.deselect(index);
	}
	public void deselectAll() {
		checkWidget();
		table.deselectAll();
	}
	public void dropDown(boolean drop) {
		// no unnecessary redraw: exit if text is still the same 
//		if (oldText == null) oldText = "";
//		if (oldText.equals(text.getText()))	{
//			return;
//		}
		
		// need to recalc vertical size
		//if (drop == isDropped()) return;
		if ((drop) && (selected == true))	{
			return;
		}
		boolean justVertical = false;
		if (drop == isDropped())	{
			justVertical = true;
		}
		// if no drop then hide popup
		if (!drop) {
			popup.setVisible(false);
			text.setFocus();
			// no unnecessary redraws
			oldText = text.getText();
			return;
		}
		// if no items in list then hide popup
		if (table.getItemCount() < 1)	{
			popup.setVisible(false);
			text.setFocus();
			// no unnecessary redraws
			oldText = text.getText();
			return;
		}
		int index = table.getSelectionIndex();
		if (index != -1) table.setTopIndex(index);
		
		// need the combosize, App main window, parent composite bounds, desktop
		Point comboSize = getSize();
		Display desktop = getDisplay();
		
		
		
		table.setHeaderVisible(true);
		table.setToolTipText("ToolTipText");
		table.getColumn(0).setAlignment(SWT.RIGHT);
		table.getColumn(1).setAlignment(SWT.RIGHT);
		table.getColumn(2).setAlignment(SWT.RIGHT);
		
		
		
		
		// recalc optimized column sizes
		resizeColums();
		
		// get table size, uncorrected
		Point tableSize = table.getSize();
		
		// rect contains the coordinates for placing topleft of popup relative to parent
		Rectangle rect = desktop.map(getParent(), null, getBounds());
		
		// calc  uncorrected vertical table size 
		tableSize.y = table.getItemHeight() * Math.min(table.getItemCount(), 32000);
		
		// correct vertical table size
		Point pt = text.getLocation();
		pt = text.toDisplay(pt);
		pt.y = pt.y + text.getBounds().height + 2 * text.getBorderWidth();
		pt.x = pt.x +                         + 2 * text.getBorderWidth();
		int headerHeight = table.getHeaderHeight();
		int verticalSpace = desktop.getClientArea().height - headerHeight - pt.y - 4;
		if (tableSize.y > verticalSpace)	{
			if (System.getProperties().getProperty("os.name").equals("Windows XP")){ 
				tableSize.y = (int)((verticalSpace / table.getItemHeight()) - 2) * table.getItemHeight() + headerHeight;
			} else {
				tableSize.y = (int)(verticalSpace / table.getItemHeight()) * table.getItemHeight() + headerHeight;
			}
		}
		// correct horizontal table position: hSize remains, we just move the popup to the left
		int horizontalSpace = desktop.getClientArea().width  - tableSize.x - 2;
		if (pt.x > horizontalSpace)	{
			rect.x = desktop.getClientArea().width - tableSize.x - 2;
		}
		
		// set size of table and popup
		table.setBounds(1, 1, tableSize.x, tableSize.y);
		popup.setBounds(rect.x, rect.y + comboSize.y, tableSize.x + 2, tableSize.y + 2);
		
		// now make the list visible again/redraw
		popup.setVisible(true);
		
		// no unnecessary redraws
		oldText = text.getText();
	}
	public Control [] getChildren() {
		checkWidget();
		return new Control [0];
	}
	boolean getEditable() {
		return text.getEditable();
	}
	//TODO
	/*
	public String getItem(int index) {
		checkWidget();
		return list.getItem(index);
	}
	*/
	public TableItem getItemNew(int index) {
		checkWidget();
		return table.getItem(index);
	}
	public int getItemCount() {
		checkWidget();
		return table.getItemCount();
	}
	public int getItemHeight() {
		checkWidget();
		return table.getItemHeight();
	}
	//TODO
	/*
	public String[] getItems() {
		checkWidget();
		return list.getItems();
	}
	*/
	public TableItem[] getItemsNew() {
		checkWidget();
		return table.getItems();
	}
	public Point getSelection() {
		checkWidget();
		return text.getSelection();
	}
	public int getSelectionIndex() {
		checkWidget();
		return table.getSelectionIndex();
	}
	public String getText() {
		checkWidget();
		return text.getText();
	}
	public int getTextHeight() {
		checkWidget();
		return text.getLineHeight();
	}
	public int getTextLimit() {
		checkWidget();
		return text.getTextLimit();
	}
	//TODO
	/*
	public int indexOf(String string) {
		checkWidget();
		if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		return list.indexOf(string);
	}
	*/
	//TODO
	/*
	public int indexOf(String string, int start) {
		checkWidget();
		if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		return list.indexOf(string, start);
	}
	*/
	void initAccessible() {
		getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getHelp(AccessibleEvent e) {
				e.result = getToolTipText();
			}
		});
			
		getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			public void getChildAtPoint(AccessibleControlEvent e) {
				Point testPoint = toControl(new Point(e.x, e.y));
				if (getBounds().contains(testPoint)) {
					e.childID = ACC.CHILDID_SELF;
				}
			}
			
			public void getLocation(AccessibleControlEvent e) {
				Rectangle location = getBounds();
				Point pt = toDisplay(new Point(location.x, location.y));
				e.x = pt.x;
				e.y = pt.y;
				e.width = location.width;
				e.height = location.height;
			}
			
			public void getChildCount(AccessibleControlEvent e) {
				e.detail = 0;
			}
			
			public void getRole(AccessibleControlEvent e) {
				e.detail = ACC.ROLE_COMBOBOX;
			}
			
			public void getState(AccessibleControlEvent e) {
				e.detail = ACC.STATE_NORMAL;
			}
	
			public void getValue(AccessibleControlEvent e) {
				e.result = getText();
			}
		});
	}
	boolean isDropped() {
		return popup.getVisible();
	}
	public boolean isFocusControl() {
		checkWidget();
		if (text.isFocusControl() || table.isFocusControl() || popup.isFocusControl()) {
			return true;
		} else {
			return super.isFocusControl();
		}
	}
	/**
	 * Neu Berechnen/Zeichnen des ganzen Konstrukts
	 */
	void internalLayout() {
		if (isDropped()) dropDown(false);
		
		Rectangle rect = getClientArea();
		int width = rect.width;
		int height = rect.height;
		text.setBounds(0, 0, width, height);
		
		Point size = getSize();
		int itemHeight = table.getItemHeight() * ITEMS_SHOWING;
		Point listSize = table.computeSize(SWT.DEFAULT, itemHeight, true);
		table.setBounds(1, 1, Math.max(size.x - 2, listSize.x), listSize.y);
	}
	/**
	 * Sets the fields linked to this control. The data found in the current selection will be inserted 
	 * into these fields. 
	 * The fields are defined in returnFields[]. 
	 * Every object that has the setText method can be used in this array.
	 * @return true on success/if data has been found, false on error
	 */
	boolean setLinkedFields()	{
		// returnFields must be set
		if (returnFields == null)	{
			//System.out.println("setLinkedFields(): the field returnFields has not yet been set.");
			return false;
		}
		// no selected row in table -> exit
		if (table.getSelectionIndex() == -1)	{
			return false;
		}
		// loop through returnFields[] which contains the fields as objects-array
		for (int fieldIx = 0; fieldIx < returnFields.length; fieldIx++){
			try {
				// get the object from the list
				Object obj = returnFields[fieldIx];
				if (obj == null) continue;
				// find the class of this object
				Class<?> c = Class.forName(obj.getClass().getName());
				// create return params class
				Class[] params = new Class[1];
				params[0] =String.class;
				// get the setText method
				Method setTextCaller = c.getMethod("setText", params);
				// build params: read currently selected
				TableItem tableItem = table.getSelection()[0];
				String[] input={new String(tableItem.getText(fieldIx))};
				// call the method
				setTextCaller.invoke(obj, input);
			} catch(ClassNotFoundException x) {
			    x.printStackTrace();
			    return false;
			} catch(IllegalAccessException x) {
			    x.printStackTrace();
			    return false;
			} catch(SecurityException e) {
				e.printStackTrace();
			    return false;
			} catch(NoSuchMethodException e) {
				e.printStackTrace();
			    return false;
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
			    return false;
			} catch(InvocationTargetException e) {
				e.printStackTrace();
			    return false;
			}
		}
		if (1==0) return true;
		return true;
	}
	class TestListener implements MouseMoveListener	{
		public void mouseMove(MouseEvent e) {
			System.out.println("testListener: mouseMove");
		}
	}
	void listEvent(Event event) {
		//System.out.println("listEvent called");
		switch(event.type) {
//		case SWT.SCROLL_LINE:
//			System.out.println("List: SCROLL_LINE");
//			break;
		case SWT.MouseEnter:
			System.out.println("List: MouseEnter");
			//isTrackingTable = true;
			break;
		case SWT.MouseExit:
			System.out.println("List: MouseExit");
			//isTrackingTable = false;
			break;
		case SWT.MouseDown:
			text.setFocus();
			isTrackingTable = true;
			Display desktop = getDisplay();
			table.setCapture(true);
//			Composite cmp = this;
//			while ((cmp = cmp.getParent()) != null)	{
//				cmp.addListener(SWT.MouseMove, (Listener) tempMouseListener);
//			}
			desktop.addListener(SWT.MouseMove, (Listener) tempMouseListener);
			break;
		case SWT.MouseUp: {
			if (event.button != 1) return;
			dropDown(false);
			Event e = new Event();
			e.time = event.time;
			notifyListeners(SWT.DefaultSelection, e);
			isTrackingTable = false;
			desktop = getDisplay();
//			cmp = this;
//			while ((cmp = cmp.getParent()) != null)	{
//				cmp.removeListener(SWT.MouseMove, (Listener) tempMouseListener);
//			}
			desktop.removeListener(SWT.MouseMove, (Listener) tempMouseListener);
			break;
		}
		case SWT.MouseMove: {
			int itemHeight = table.getItemHeight();
			if (isTrackingTable == false)	{
				//table.setCapture(true);
				System.out.println("List: MouseMove");
				int itemSel = (event.y - table.getHeaderHeight()) / itemHeight;
				//System.out.println("itemSel: " + itemSel);
				int newSelection = table.getTopIndex() + itemSel;
				int currSel = table.getSelectionIndex();
				// windows behaviour
				if (currSel != newSelection)	{
					table.setSelection(newSelection);
				}
			} else {
				table.setCapture(true);
				Point pt = new Point((int) (MouseInfo.getPointerInfo().getLocation().getX()), (int) MouseInfo.getPointerInfo().getLocation().getY());
				pt = table.toControl(pt);
				int itemSel = (pt.y - table.getHeaderHeight()) / itemHeight;
				//System.out.println("itemSel: " + itemSel);
				int newSelection = table.getTopIndex() + itemSel;
				int currSel = table.getSelectionIndex();
				Rectangle bounds = popup.getBounds();
				//if (bounds.contains(pt))	{
					if (currSel != newSelection)	{
						table.setSelection(newSelection);
					}
				//}
			}
		}
		case SWT.FocusIn: {
				if (hasFocus) return;
				hasFocus = true;
				if (getEditable()) text.selectAll();
				Event e = new Event();
				e.time = event.time;
				notifyListeners(SWT.FocusIn, e);
				System.out.println("listEvent FocusIn");
				break;
			}
			case SWT.FocusOut: {
				//System.out.println("SWT.FocusOut");
				event.display.asyncExec(new Runnable() {
					public void run() {
						if (MCCombo.this.isDisposed()) return;
						Control focusControl = getDisplay().getFocusControl();
						if (focusControl == text) return;
						hasFocus = false;
						Event e = new Event();
						notifyListeners(SWT.FocusOut, e);
					}
				});
				System.out.println("listEvent FocusOut");
				break;
			}
			case SWT.Selection: {
				//System.out.println("SWT.Selection");
				int index = table.getSelectionIndex();
				if (index == -1) return;
				text.setText(table.getItem(index).getText(0));
				setLinkedFields();
				text.selectAll();
				table.setSelection(index);
				table.setFocus();
				event.doit = false;
				if (1==1) break;
				Event e = new Event();
				e.time = event.time;
				e.stateMask = event.stateMask;
				e.doit = event.doit;
				notifyListeners(SWT.Selection, e);
				event.doit = e.doit;
				setLinkedFields();
				break;
			}
			case SWT.Traverse: {
				//System.out.println("listEvent: SWT.Traverse: before switch");
				switch(event.detail) {
					case SWT.TRAVERSE_TAB_NEXT:
					case SWT.TRAVERSE_RETURN:
					case SWT.TRAVERSE_ESCAPE:
					case SWT.TRAVERSE_ARROW_PREVIOUS:
					case SWT.TRAVERSE_ARROW_NEXT:
						event.doit = false;
						break;
				}
				//System.out.println("SWT.Traverse: after switch");
				Event e = new Event();
				e.time    = event.time;
				e.detail  = event.detail;
				e.doit    = event.doit;
				e.keyCode = event.keyCode;
	//			if	((event.keyCode == SWT.ARROW_LEFT) || (event.keyCode == SWT.ARROW_RIGHT))	{
	//				event.doit = false;
	//				e.doit = false;
	//				System.out.println("traverse cancel");
	//				break;
	//			}
				notifyListeners(SWT.Traverse, e);
				//System.out.println("event.keyCode: " + event.keyCode);
				event.doit = e.doit;
				break;
			}
			case SWT.KeyUp: {		
				//System.out.println("listEvent: SWT.KeyUp");
				int index = table.getSelectionIndex();
				if (index == -1) return;
				////////////text.setText(table.getItem(index).getText(0));
				Event e = new Event();
				e.time      = event.time;
				e.character = event.character;
				e.keyCode   = event.keyCode;
				e.stateMask = event.stateMask;
				if	((event.keyCode == SWT.ARROW_LEFT) || (event.keyCode == SWT.ARROW_RIGHT))	{
					//event.doit = false;
					//e.doit = false;
					//System.out.println("keyUp cancel");
					break;
				}
				notifyListeners(SWT.KeyUp, e);
				break;
			}
			case SWT.KeyDown: {
				System.out.println("listEvent: SWT.KeyDown");
				if (event.character == SWT.ESC) { 
					// escape key cancels popup list
					dropDown(false);
				}
				if (event.character == SWT.CR || event.character == '\t') {
					// Enter and Tab cause default selection
					dropDown(false);
					selected = !selected;
					Event e = new Event();
					e.time      = event.time;
					e.stateMask = event.stateMask;
					notifyListeners(SWT.DefaultSelection, e);
				}
				if ((event.keyCode == SWT.ARROW_LEFT) || (event.keyCode == SWT.ARROW_RIGHT)) {
					// Enter and Tab cause default selection
					//text.setSelection(text.getText().length());
					text.setFocus();
					event.doit = false;
					Event e = new Event();
					e.time      = event.time;
					e.type = event.type;
					e.button = event.button;
					e.character = event.character;
					e.keyCode = event.keyCode;
					e.stateMask = event.stateMask;
					notifyListeners(SWT.DefaultSelection, e);
				}
				int oldIndex = table.getSelectionIndex();
				if (event.keyCode == SWT.ARROW_UP) {
					switch(oldIndex)	{
					case(0):
						text.setText("set from keyup");
						break;
					default:
						break;
					}
				}
				//At this point the widget may have been disposed.
				// If so, do not continue.
				if (isDisposed()) break;
				Event e = new Event();
				e.time      = event.time;
				e.character = event.character;
				e.keyCode   = event.keyCode;
				e.stateMask = event.stateMask;
				notifyListeners(SWT.KeyDown, e);
				break;
				
			}
		}
	}
	void popupEvent(Event event) {
		switch(event.type) {
			case SWT.Paint:
				// draw black rectangle around list
				//System.out.println("popup: draw");
				Rectangle listRect;
				listRect = table.getBounds();
				Color black = getDisplay().getSystemColor(SWT.COLOR_BLACK);
				event.gc.setForeground(black);
				event.gc.drawRectangle(0, 0, listRect.width + 1, listRect.height + 1);
				break;
			case SWT.Close:
				//System.out.println("popup: close");
				event.doit = false;
				//dropDown(false);
				break;
			case SWT.Deactivate:
				//System.out.println("popup: deactivate");
				//dropDown(false);
				break;
			case SWT.MouseMove:
				System.out.println("popupEvent: MouseMove");
				break;
			case SWT.MouseHover:
				System.out.println("popupEvent: MouseHover");
				break;
			default:
				//System.out.println("popup: alles andere");
		}
	}
	public void redraw(int x, int y, int width, int height, boolean all) {
		checkWidget();
		if (!all) return;
		Point location = text.getLocation();
		text.redraw(x - location.x, y - location.y, width, height, all);
		location = table.getLocation();
		table.redraw(x - location.x, y - location.y, width, height, all);
	}
	public void remove(int index) {
		checkWidget();
		table.remove(index);
	}
	public void remove(int start, int end) {
		checkWidget();
		table.remove(start, end);
	}
	//TODO
	/*
	public void remove(String string) {
		checkWidget();
		if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (isOldStyle){
			list.remove(string);
		} else {
			//table.remove(string);
		}
	}
	*/
	public void removeAll() {
		checkWidget();
		text.setText(""); //$NON-NLS-1$
		table.deselectAll();
		table.removeAll();
	}
	public void removeModifyListener(ModifyListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		removeListener(SWT.Modify, listener);	
	}
	public void removeSelectionListener(SelectionListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection,listener);	
	}
	public void select(int index) {
		checkWidget();
		if (index == -1) {
			table.deselectAll();
			text.setText(""); //$NON-NLS-1$
			return;
		}
		if (0 <= index && index < table.getItemCount()) {
			if (index != getSelectionIndex()) {
				// TODO
				/////////////////////////text.setText(table.getItem(index));
				text.selectAll();
				table.select(index);
	//			TableItem currItem = table.getItem(index);
	//			table.setSelection(currItem);
	//			int[] indices = {index, index + 1, index + 2, index + 3};
	//			table.select(index, index+10);
				table.showSelection();
			}
		}
	}
	public void setBackground(Color color) {
		super.setBackground(color);
		if (text != null) text.setBackground(color);
		if (table != null) table.setBackground(color);
	}
	public boolean setFocus() {
		checkWidget();
		return text.setFocus();
	}
	public void setFont(Font font) {
		super.setFont(font);
		text.setFont(font);
		table.setFont(font);
		internalLayout();
	}
	public void setForeground(Color color) {
		super.setForeground(color);
		if (text != null) text.setForeground(color);
		if (table != null) table.setForeground(color);
	}
	//TODO
	/*
	public void setItem(int index, String string) {
		checkWidget();
		if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		list.setItem(index, string);
	}
	*/
	protected void resizeColums()	{
		//////////////////////////////////////////////
		if (1==0) return;
		
		checkWidget();
		int width = 0;
		//////////////////////table.setBounds(0, 0, 500, 500);
		for (int colIx = 0; colIx < table.getColumnCount(); colIx++)	{
			TableColumn currColumn = table.getColumns()[colIx];
			currColumn.setResizable(true);
			currColumn.pack();
			width = width + currColumn.getWidth();
		}
		//table.setSize(width, table.getSize().y);  // wrong: missing scrollbar!
		table.pack();
	}
	public void setItems(String[][] items) {
	//	setDataProviderCaller();
		checkWidget();
		
		if (items == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		int style = getStyle();
		
		// no unnecessary redraw: exit if text is still the same 
	//	if ((oldText != null) && (oldText.equals(text.getText())))	{
	//		return;
	//	}
		
		/////////////////////if ((style & SWT.READ_ONLY) != 0) text.setText(""); //$NON-NLS-1$
		
		// remove old content
		table.removeAll();
		
		// create columns
		int numOfRows    = items.length;
		if (numOfRows == 0) return;
		int numOfColumns = items[0].length;
		
		int currColumnCount = table.getColumnCount();
		if (numOfColumns < currColumnCount)	{
			for (int i = numOfColumns; i < currColumnCount; i++)	{
				table.getColumns()[i].dispose();
			}
		}
		
		for (int colIx = 0; colIx < numOfColumns; colIx++)	{
			// detect or create column
			TableColumn currColumn = null;
			if (colIx < currColumnCount){
				currColumn = table.getColumns()[colIx];
			} else {
				currColumn = new TableColumn(table, SWT.NULL);
			}
		}
		// no flickering when redrawing for XP
		popup.setRedraw(false);
		// insert data into table
		for (int rowIx = 0; rowIx < numOfRows; rowIx++)	{
			TableItem tableItem = new TableItem(table, SWT.NULL);
			tableItem.setText(items[rowIx]);
		}
		// optimize size of the columns
		for (int colIx = 0; colIx < numOfColumns; colIx++)	{
			table.getColumn(colIx).setWidth(1000);   // bug workaround for Windows XP which makes colums smaller but not bigger in .pack
			table.getColumn(colIx).pack();
		}
		// no flickering when redrawing for XP
		popup.setRedraw(true);
		
		// no unnecessary redraw
		//oldText = text.getText();
		table.getItem(0).setGrayed(true);
	}
	public void setSelection(Point selection) {
		checkWidget();
		if (selection == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		text.setSelection(selection.x, selection.y);
	}
	//TODO
	public void setText(String string) {
		checkWidget();
		
		text.setText(string);
		
//		
//		if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
//		int index = list.indexOf(string);
//		if (index == -1) {
//			list.deselectAll();
//			text.setText(string);
//			return;
//		}
//		text.setText(string);
//		text.selectAll();
//		list.setSelection(index);
//		list.showSelection();
	}
	public void setTextNew(String string) {
		checkWidget();
		if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		// TODO int index = list.indexOf(string);
		int index = 0;  // TODO muss weg
		if (index == -1) {
			table.deselectAll();
			text.setText(string);
			return;
		}
		text.setText(string);
		text.selectAll();
		// TODO table.setSelection(index);
		table.showSelection();
	}
	public void setTextLimit(int limit) {
		checkWidget();
		text.setTextLimit(limit);
	}
	public void setToolTipText(String string) {
		checkWidget();
		super.setToolTipText(string);
		text.setToolTipText(string);		
	}
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible) popup.setVisible(false);
	}
	void textEvent(Event event) {
		switch(event.type) {
			case SWT.FocusIn: {
				//System.out.println("textEvent: FocusIn");
				if (hasFocus) return;
				hasFocus = true;
				if (getEditable()) text.selectAll();
				Event e = new Event();
				e.time = event.time;
				notifyListeners(SWT.FocusIn, e);
				//dropDown(true);
				System.out.println("textEvent FocusIn");
				break;
			}
			case SWT.FocusOut: {
				//System.out.println("textEvent: FocusOut");
				event.display.asyncExec(new Runnable() {
					public void run() {
						if (MCCombo.this.isDisposed()) return;
						Control focusControl = getDisplay().getFocusControl();
						if (focusControl == table) return;
						hasFocus = false;
						Event e = new Event();
						notifyListeners(SWT.FocusOut, e);
					}
				});
				System.out.println("textEvent FocusOut");
				//dropDown(false);
				break;
			}
			case SWT.KeyDown: {
				boolean handled = false;
				//System.out.println("textEvent: KeyDown");
				if (event.character != SWT.CR) {
					selected = false;
				}
				if (event.character == SWT.ESC) { // escape key cancels popup list
					dropDown(selected);
					text.setText(theText);
					text.setSelection(text.getText().length());
					event.doit = false;
					theText = text.getText();
					table.removeAll();
					table.deselectAll();
					text.setFocus();
					handled = true;
					selected = !selected;
					break;
				}
				if (event.character == SWT.CR) {
					dropDown(selected);
					Event e = new Event();
					e.time = event.time;
					e.stateMask = event.stateMask;
					notifyListeners(SWT.DefaultSelection, e);
					event.doit = false;
					theText = text.getText();
					// this sets the linked fields from the selection
					setLinkedFields();
					selected = !selected;
					table.removeAll();
					table.deselectAll();
					text.setFocus();
					handled = true;
					break;
				}
				//At this point the widget may have been disposed.
				// If so, do not continue.
				if (isDisposed()) break;
				
				if (event.keyCode == SWT.HOME) {
					table.setSelection(0);
					text.setText(table.getSelection()[0].getText());
					text.setSelection(text.getText().length());
					event.doit = false;
					handled = true;				
				}
				if (event.keyCode == SWT.END) {
					table.setSelection(table.getItemCount() - 1);
					text.setText(table.getSelection()[0].getText());
					text.setSelection(text.getText().length());
					event.doit = false;
					handled = true;
				}
				if (event.keyCode == SWT.PAGE_DOWN) {
					Rectangle rect = table.getBounds();
					int itemHeight = table.getItemHeight();
					int numOfDisplayedRows = (rect.height / itemHeight);
					table.setSelection(Math.min(table.getItemCount() - 1, table.getSelectionIndex() + numOfDisplayedRows - 1));
					text.setText(table.getSelection()[0].getText());
					text.setSelection(text.getText().length());
					event.doit = false;
					handled = true;
				}
				if (event.keyCode == SWT.PAGE_UP) {
					Rectangle rect = table.getBounds();
					int itemHeight = table.getItemHeight();
					int numOfDisplayedRows = (rect.height / itemHeight);
					table.setSelection(Math.max(0, table.getSelectionIndex() - numOfDisplayedRows + 1));
					text.setText(table.getSelection()[0].getText());
					text.setSelection(text.getText().length());
					event.doit = false;
					handled = true;
				}
				if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN) {
					int oldIndex = getSelectionIndex();
					if (event.keyCode == SWT.ARROW_UP) {
						switch(oldIndex) {
						case -1: {
							// no item is currently selected -> save Text in theText, select last item, set text field, select end of text
							theText = text.getText();
							table.setSelection(table.getItemCount() - 1);
							text.setText(table.getSelection()[0].getText());
							text.setSelection(text.getText().length());
							break;
						}
						case 0: {
							// first item is currently selected -> deselect, set saved text, select end of text
							table.deselectAll();
							text.setText(theText);
							text.setSelection(text.getText().length());
							break;
						}
						default: {
							// another item is selected -> select previous item, set text field, select end of text
							table.setSelection(oldIndex - 1);
							text.setText(table.getSelection()[0].getText());
							text.setSelection(text.getText().length());
							break;
						}
						}
						event.doit = false;
					}
					if (event.keyCode == SWT.ARROW_DOWN) {
						switch(oldIndex) {
						case -1: {
							// no item is currently selected -> save Text in theText, select first item, set text field, select end of text
							theText = text.getText();
							table.setSelection(0);
							text.setText(table.getSelection()[0].getText());
							text.setSelection(text.getText().length());
							break;
						}
						default: {
							if (oldIndex == table.getItemCount() - 1)	{
								// last item is currently selected -> deselect, set saved text, select end of text
								table.deselectAll();
								text.setText(theText);
								text.setSelection(text.getText().length());
							} else {
								// another item is selected -> select next item, set text field, select end of text
								table.setSelection(oldIndex + 1);
								table.setSelection(oldIndex + 1);
								text.setText(table.getSelection()[0].getText());
								text.setSelection(text.getText().length());
							}
							break;
						}
						}
						event.doit = false;
					} else	{
						//table.deselectAll();
					}
					
					if (oldIndex != getSelectionIndex()) {
						Event e = new Event();
						e.time = event.time;
						e.character = event.character;
						e.keyCode = event.keyCode;
						e.stateMask = event.stateMask;
						notifyListeners(SWT.Selection, e);
					}
					
					//At this point the widget may have been disposed.
					// If so, do not continue.
					if (isDisposed()) break;
					handled = true;
				}
				if (handled == false)	{
					// all other keys
					table.deselectAll();
				} else {
				}
				
				// Further work : Need to add support for incremental search in 
				// pop up list as characters typed in text widget
				Event e = new Event();
				e.time = event.time;
				e.character = event.character;
				e.keyCode = event.keyCode;
				e.stateMask = event.stateMask;
				//notifyListeners(SWT.KeyDown, e);
				//System.out.println("textEvent.KeyDown: " + e.character);				
				
				break;
			}
			case SWT.KeyUp: {
				//if (1==1) break;
				//System.out.println("textEvent: KeyUp");
				Event e = new Event();
				e.time = event.time;
				e.character = event.character;
				e.keyCode = event.keyCode;
				e.stateMask = event.stateMask;
				char ch = event.character;
				notifyListeners(SWT.KeyUp, e);
				callDataProvider();
				break;
			}
			case SWT.Modify: {
				if (1==1) break;
				//System.out.println("textEvent: Modify");
				table.deselectAll();
				Event e = new Event();
				e.time = event.time;
				notifyListeners(SWT.Modify, e);
				//System.out.println("textEvent.Modify");
				callDataProvider();
				
				break;
			}
			case SWT.MouseDown: {
				//System.out.println("textEvent: MouseDown");
				if (event.button != 1) return;
				if (text.getEditable()) return;
				boolean dropped = isDropped();
				text.selectAll();
				//if (!dropped) setFocus();
				//dropDown(!dropped);
				dropDown(true);
				Event e = new Event();
				e.time = event.time;
				e.x = event.x;
				e.y = event.y;
				notifyListeners(SWT.MouseDown, event);
				break;
			}
			case SWT.MouseUp: {
				//System.out.println("textEvent: MouseUp");
				if (event.button != 1) return;
				if (text.getEditable()) return;
				text.selectAll();
				break;
			}
			case SWT.Traverse: {		
				//System.out.println("textEvent: Traverse");
				switch(event.detail) {
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_ARROW_PREVIOUS:
				case SWT.TRAVERSE_ARROW_NEXT:
				case SWT.ARROW_DOWN:
				case SWT.ARROW_UP:
					//System.out.println("textEvent: first part");
					// The enter causes default selection and
					// the arrow keys are used to manipulate the list contents so
					// do not use them for traversal.
					event.doit = false;
					return;
					//break;
				}
				if (event.keyCode == SWT.TAB) {
					dropDown(false);
				}
				Event e = new Event();
				e.time = event.time;
				e.detail = event.detail;
				e.doit = event.doit;
				e.keyCode = event.keyCode;
				notifyListeners(SWT.Traverse, e);
				event.doit = e.doit;
				break;
			}
		}
	}
}