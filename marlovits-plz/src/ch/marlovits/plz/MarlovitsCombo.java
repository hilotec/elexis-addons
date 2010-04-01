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
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ch.marlovits.plz;


import java.awt.MouseInfo;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleTextAdapter;
import org.eclipse.swt.accessibility.AccessibleTextEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

/**
 * The CCombo class represents a selectable user interface object
 * that combines a text field and a list and issues notification
 * when an item is selected from the list.
 * <p>
 * CCombo was written to work around certain limitations in the native
 * combo box. Specifically, on win32, the height of a CCombo can be set;
 * attempts to set the height of a Combo are ignored. CCombo can be used
 * anywhere that having the increased flexibility is more important than
 * getting native L&F, but the decision should not be taken lightly. 
 * There is no is no strict requirement that CCombo look or behave
 * the same as the native combo box.
 * </p>
 * <p>
 * Note that although this class is a subclass of <code>Composite,
 * it does not make sense to add children to it, or set a layout on it.
 * </p>
 * <dl>
 * <dt>Styles:
 * <dd>BORDER, READ_ONLY, FLAT
 * <dt>Events:
 * <dd>DefaultSelection, Modify, Selection, Verify
 * </dl>
 */
enum MARLOVITSCOMBO_DISPLAYLINES   {fixed,	// number of displayed lines as in visibleItemCount
									parent,	// number of displayed lines fitting parent
									app,	// number of displayed lines fitting application window
									screen	//  number of displayed lines fitting screen (current monitor)
	
}

public final class MarlovitsCombo extends Composite {
	
	int			leftMarginOffset = -4;   // +++++ for WindowsXP
	
	Text		text;
	int			visibleItemCount = 5;
	Shell		popup;
	Button		arrow;
	boolean		hasFocus;
	Listener	listener, filter;
	Color		foreground;
	Color		background;
	Font		font;
	int			currListFocus = -1;
	boolean		mouseIsDownInList = false;
	int			textLinkedColumnIndex = 0;				// list from which text for text field is extracted
	// for list formatting
	int			columnSpacing = 9;						// space between columns, best if uneven
	int			columnLeftMargin  = columnSpacing / 2;	// space on the left of leftmost list
	int			columnRightMargin = columnLeftMargin;	// space on the right of leftmost list
	boolean		drawDividerLines = true;
	Color		dividerLineColor;
	
	int			focusItem = -1;
	int			focusItemLastTopIx = -1;
	int			savedTopIx = -1;
	
	GC 			popupGC;

	
	private TableViewer	tableViewer;
	private Table		table;
	TestListener tempMouseListener = new TestListener();
	boolean		isTrackingTable;
	int			startTrackingItem = -1;
	
	// dbg sesttings
	private	long	printWhat = 0x00000000;
	static long	DBG_TextEvent  = 1;
	static long	DBG_TableEvent = 2;
	static long	DBG_ListenerDispatcher = 4;
	
/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT which is applicable to instances of this
 * class, or must be built by <em>bitwise OR'ing together 
 * (that is, using the <code>int "|" operator) two or more
 * of those <code>SWT style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a widget which will be the parent of the new instance (cannot be null)
 * @param style the style of widget to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent
 * </ul>
 *
 * @see SWT#BORDER
 * @see SWT#READ_ONLY
 * @see SWT#FLAT
 * @see Widget#getStyle()
 */
/*******************************************************************************
 * new methods for this new Composite
 * START
 *******************************************************************************/
	class TestListener implements MouseMoveListener	{
		public void mouseMove(MouseEvent e) {
			System.out.println("testListener: mouseMove");
		}
	}
public Table getTable()	{
	return table;
}
public void setDrawDividerLines(final boolean doDrawDividerLines)	{
	drawDividerLines = doDrawDividerLines;
}
public boolean getDrawDividerLines()	{
	return drawDividerLines;
}
public void setDividerLineColor(final int newColor)	{
	dividerLineColor = getDisplay().getSystemColor(newColor);
}
public void setDividerLineColor(final Color newColor)	{
	dividerLineColor = newColor;
}
public Color getDividerLineColor()	{
	return dividerLineColor;
}
public void setColumnSpacing(final int colSpacing)	{
	columnSpacing = colSpacing;
}
public int getColumnSpacing()	{
	return columnSpacing;
}
public void setColumnLeftMargin(final int newLeftMargin)	{
	columnLeftMargin = newLeftMargin;
}
public int getColumnLeftMargin()	{
	return columnLeftMargin;
}
public void setColumnRightMargin(final int newRightMargin)	{
	columnRightMargin = newRightMargin;
}
public int getColumnRightMargin()	{
	return columnRightMargin;
}
/*******************************************************************************
 * new methods for this new Composite
 * END
 *******************************************************************************/
public MarlovitsCombo(Composite parent, int style) {
	super (parent, style = checkStyle (style));
	
	dividerLineColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
	
	// create the text field ****************************************************
	int textStyle = SWT.SINGLE;
	if ((style & SWT.READ_ONLY) != 0) textStyle |= SWT.READ_ONLY;
	if ((style & SWT.FLAT) != 0) textStyle |= SWT.FLAT;
	text = new Text(this, textStyle);

	// create drop down arrow ***************************************************
	int arrowStyle = SWT.ARROW | SWT.DOWN ;
	if ((style & SWT.FLAT) != 0) arrowStyle |= SWT.FLAT;
	arrow = new Button(this, arrowStyle);
	
	// create listener dispatcher for all events ********************************
	listener = new Listener() {
		public void handleEvent(Event event) {
			if (popup == event.widget) {
				dbg("listener: popup == event.widget", DBG_ListenerDispatcher);
				popupEvent(event);
				return;
			}
			if (text == event.widget) {
				dbg("listener: text == event.widget", DBG_ListenerDispatcher);
				textEvent(event);
				return;
			}
			if (table == event.widget){
				dbg("listener: table == event.widget", DBG_ListenerDispatcher);
				tableEvent(event);
				return;
			}
			if (arrow == event.widget) {
				dbg("listener: arrow == event.widget", DBG_ListenerDispatcher);
				arrowEvent(event);
				return;
			}
			if (MarlovitsCombo.this == event.widget) {
				dbg("listener: MarlovitsCombo.this == event.widget", DBG_ListenerDispatcher);
				comboEvent(event);
				return;
			}
			if (getShell() == event.widget) {
				dbg("listener: getShell() == event.widget", DBG_ListenerDispatcher);
				getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (isDisposed()) return;
						handleFocus(SWT.FocusOut);
					}
				});
			}
		}
	};
	filter = new Listener() {
		public void handleEvent(Event event) {
			Shell shell = ((Control)event.widget).getShell();
			if (shell == MarlovitsCombo.this.getShell()) {
				handleFocus(SWT.FocusOut);
			}
		}
	};
	
	int [] comboEvents = {SWT.Dispose, SWT.FocusIn, SWT.Move, SWT.Resize};
	for (int i = 0; i < comboEvents.length; i++)	{
		this.addListener(comboEvents[i], listener);
	}
	
	int [] textEvents = {SWT.DefaultSelection, SWT.KeyDown, SWT.KeyUp, SWT.MenuDetect, SWT.Modify, SWT.MouseDown, SWT.MouseUp, SWT.MouseDoubleClick, SWT.MouseWheel, SWT.Traverse, SWT.FocusIn, SWT.Verify};
	for (int i = 0; i < textEvents.length; i++)	{
		text.addListener(textEvents[i], listener);
	}
	
	int [] arrowEvents = {SWT.MouseDown, SWT.MouseUp, SWT.Selection, SWT.FocusIn};
	for (int i = 0; i < arrowEvents.length; i++)	{
		arrow.addListener(arrowEvents[i], listener);
	}
	
	String[][] tmp = null;
	createPopup(tmp, -1);
	initAccessible();
}
static int checkStyle (int style) {
	int mask = SWT.BORDER | SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
	return style & mask;
}
/**
 * Adds the argument to the end of the receiver's list.
 *
 * @param string the new item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see #add(String,int)
 */
//IO +++++
public void add(String[] rowCells) {
	int index = table.getItemCount();
	add(rowCells, index);
}
/**
 * Adds the argument to the receiver's list at the given
 * zero-relative index.
 * <p>
 * Note: To add an item at the end of the list, use the
 * result of calling <code>getItemCount() as the
 * index or use <code>add(String).
 * </p>
 *
 * @param string the new item
 * @param index the index for the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list (inclusive)
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see #add(String)
 */
//IO +++++
public void add(String[] rowCells, int index) {
	checkWidget();
	
	if (table == null) return;
	
	int itemCount = table.getItemCount();
	
	// test for out of range
	if ((index < 0) || (index > itemCount))	{
		return;
	}
	
	// if no columns yet created -> create popup
	int colCount = table.getColumnCount();
	if (colCount == 0){
		String[][] tmpStrings = new String[1][1];
		tmpStrings[0] = rowCells;
		createPopup(tmpStrings, -1);
		return;
	}
	
	// if numOfColumns of table < rowCells.length -> not allowed
	if (table.getColumnCount() < rowCells.length)	{
		return;
	}
	
	// setting item
	TableItem tableItem = new TableItem(table, SWT.None, index);
	tableItem.setText(rowCells);
}
/**
 * Adds the  to the collection of listeners who will
 * be notified when the receiver's text is modified, by sending
 * it one of the messages defined in the <code>ModifyListener
 * interface.
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see ModifyListener
 * @see #removeModifyListener
 */
// IO +++++
public void addModifyListener(ModifyListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Modify, typedListener);
}
/**
 * Adds the listener to the collection of listeners who will
 * be notified when the user changes the receiver's selection, by sending
 * it one of the messages defined in the <code>SelectionListener
 * interface.
 * <p>
 * <code>widgetSelected is called when the combo's list selection changes.
 * <code>widgetDefaultSelected is typically called when ENTER is pressed the combo's text area.
 * </p>
 *
 * @param listener the listener which should be notified when the user changes the receiver's selection
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see SelectionListener
 * @see #removeSelectionListener
 * @see SelectionEvent
 */
// IO +++++
public void addSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Selection,typedListener);
	addListener(SWT.DefaultSelection,typedListener);
}
/**
 * Adds the listener to the collection of listeners who will
 * be notified when the receiver's text is verified, by sending
 * it one of the messages defined in the <code>VerifyListener
 * interface.
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see VerifyListener
 * @see #removeVerifyListener
 * 
 * @since 3.3
 */
// IO +++++
public void addVerifyListener(VerifyListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Verify,typedListener);
}
// IO +++++
void arrowEvent(Event event) {
	switch(event.type) {
		case SWT.FocusIn: {
			handleFocus(SWT.FocusIn);
			break;
		}
		case SWT.MouseDown: {
			Event mouseEvent = new Event ();
			mouseEvent.button	 = event.button;
			mouseEvent.count	 = event.count;
			mouseEvent.stateMask = event.stateMask;
			mouseEvent.time		 = event.time;
			mouseEvent.x		 = event.x; mouseEvent.y = event.y;
			notifyListeners(SWT.MouseDown, mouseEvent);
			event.doit = mouseEvent.doit;
			//dropDown(true);
			break;
		}
		case SWT.MouseUp: {
			Event mouseEvent	 = new Event ();
			mouseEvent.button	 = event.button;
			mouseEvent.count	 = event.count;
			mouseEvent.stateMask = event.stateMask;
			mouseEvent.time		 = event.time;
			mouseEvent.x		 = event.x;
			mouseEvent.y		 = event.y;
			notifyListeners(SWT.MouseUp, mouseEvent);
			event.doit = mouseEvent.doit;
			break;
		}
		case SWT.Selection: {
			dropDown(!isDropped ());
			break;
		}
	}
}
/**
 * Sets the selection in the receiver's text field to an empty
 * selection starting just before the first character. If the
 * text field is editable, this has the effect of placing the
 * i-beam at the start of the text.
 * <p>
 * Note: To clear the selected items in the receiver's list, 
 * use <code>deselectAll().
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see #deselectAll
 */
// IO +++++
public void clearSelection() {
	checkWidget();
	text.clearSelection();
	table.deselectAll();
}
// IO +++++
void comboEvent(Event event) {
	System.out.println("comboEvent");
	switch(event.type) {
		case SWT.Dispose:
			if (popup != null && !popup.isDisposed ()) {
				table.removeListener(SWT.Dispose, listener);
				popup.dispose();
			}
			Shell shell = getShell();
			shell.removeListener(SWT.Deactivate, listener);
			Display display = getDisplay();
			display.removeFilter(SWT.FocusIn, filter);
			popup = null;
			text  = null;
			table = null;
			arrow = null;
			break;
		case SWT.FocusIn:
			Control focusControl = getDisplay().getFocusControl();
			if ((focusControl == arrow) || (focusControl == table)) return;
			if (isDropped()) {
				table.setFocus();
			} else {
				text.setFocus();
			}
			break;
		case SWT.Move:
			dropDown(false);
			break;
		case SWT.Resize:
			internalLayout(false);
			break;
	}
}
/**
 * calc size of this Composite
 */
// IO +++++
public Point computeSize(int wHint, int hHint, boolean changed)	{
	checkWidget();
	
	// init result
	int width  = 0;
	int height = 0;
	
	// loop through items of column linked to text field, find widest string (in pixels)
	GC gc = new GC(text);
	int spacer = gc.stringExtent(" ").x; //$NON-NLS-1$
	int textWidth = gc.stringExtent(text.getText()).x;
	TableItem[] items = table.getItems();
	for (int i = 0; i < table.getItemCount(); i++) {
		String str = items[i].getText(textLinkedColumnIndex);
		textWidth = Math.max(gc.stringExtent(items[i].getText(textLinkedColumnIndex)).x, textWidth);
	}
	gc.dispose();
	
	// recalc sizes of text/arrow/table
	Point textSize  = text. computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
	Point arrowSize = arrow.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
	Point tableSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
	int borderWidth = getBorderWidth();
	
	// height is text or arrow height
	height = Math.max(textSize.y, arrowSize.y);
	
	// width is calculated text width + spacers + border widths
	width  = Math.max(textWidth + 2 * spacer + arrowSize.x + 2 * borderWidth, tableSize.x);
	//width = listSize.x;
	
	if (wHint != SWT.DEFAULT) width = wHint;
	if (hHint != SWT.DEFAULT) height = hHint;
	return new Point(width + 2 * borderWidth, height + 2 * borderWidth);
}
/**
 * Copies the selected text.
 * <p>
 * The current selection is copied to the clipboard.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.3
 */
// IO +++++
public void copy () {
	checkWidget ();
	text.copy ();
}
/*
void createLists(int numOfLists)	{
	// *** adjusting style ***************************************************
	int style = getStyle ();
	int tableStyle = SWT.SINGLE;
	if ((style & SWT.FLAT) != 0) tableStyle |= SWT.FLAT;
	if ((style & SWT.RIGHT_TO_LEFT) != 0) tableStyle |= SWT.RIGHT_TO_LEFT;
	if ((style & SWT.LEFT_TO_RIGHT) != 0) tableStyle |= SWT.LEFT_TO_RIGHT;
	
	// *** create lists == columns *******************************************
	// disposing of unused lists/columns
	int currListCount = 0;
	if (lists != null)	{
		currListCount = lists.length;
		if (numOfLists < currListCount)	{
			for (int i = numOfLists; i < currListCount; i++)	{
				lists[i].dispose();
			}
		}
	}
	List[] newLists = new List[numOfLists];
	if (lists != null)	{
		System.arraycopy(lists, 0, newLists, 0, lists.length);
	}
	lists = newLists;
	// creating lists/columns
	for (int listIx = 0; listIx < numOfLists; listIx++)	{
		// detect or create column
		List currList = null;
		if (listIx < currListCount){
			currList = lists[listIx];
		} else {
			currList = new List(popup, tableStyle);
			lists[listIx] = currList;
		}
		// set font/colors, etc
		if (font       != null) currList.setFont(font);
		if (foreground != null) currList.setForeground (foreground);
		if (background != null) currList.setBackground (background);
		// set listeners for lists
		int [] listEvents = {SWT.MouseUp, SWT.MouseDown, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.Dispose, SWT.MouseHover, SWT.MouseMove};
		for (int eventIx = 0; eventIx < listEvents.length; eventIx++)	{
			currList.addListener(listEvents[eventIx], listener);
		}
	}
}
*/
// IO +++++
void createPopup(TableItem[] items, int selectionIndex) {
	String[][] stringItems = tableItemsToStrings(items);
	createPopup(stringItems, selectionIndex);
}
// IO +++++
void createPopup(String[][] items, int selectionIndex) {
	// create shell and list
	popup = new Shell (getShell (), SWT.ON_TOP | SWT.TOOL);
	
	// *** adjusting style ***************************************************
	int style = getStyle ();
	int tableStyle = SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION;
	if ((style & SWT.FLAT) != 0) tableStyle |= SWT.FLAT;
	if ((style & SWT.RIGHT_TO_LEFT) != 0) tableStyle |= SWT.RIGHT_TO_LEFT;
	if ((style & SWT.LEFT_TO_RIGHT) != 0) tableStyle |= SWT.LEFT_TO_RIGHT;
	
	// create table **********************************************************
	// create table viewer
	tableViewer = new TableViewer(popup, tableStyle);
	GridData gd = new GridData();
	gd.horizontalAlignment = SWT.FILL;
	gd.verticalAlignment   = SWT.FILL;
	gd.grabExcessHorizontalSpace = true;
	gd.grabExcessVerticalSpace   = true;
	tableViewer.getControl().setLayoutData(gd);
	
	// create table
	table = tableViewer.getTable();
	table.setHeaderVisible(false);
	table.setLinesVisible(false);
	
	// count rows and items
	if (items != null)	{
		int numOfRows    = items.length;
		if (numOfRows == 0) return;
		int numOfColumns = items[0].length;
		
		// delete unused columns
		int currColumnCount = table.getColumnCount();
		if (numOfColumns < currColumnCount)	{
			for (int i = numOfColumns; i < currColumnCount; i++)	{
				table.getColumns()[i].dispose();
			}
		}
		
		// create additional columns if necessary
		for (int colIx = 0; colIx < numOfColumns; colIx++)	{
			// detect or create column
			TableColumn currColumn = null;
			if (colIx < currColumnCount){
				currColumn = table.getColumns()[colIx];
			} else {
				currColumn = new TableColumn(table, SWT.NULL);
			}
		}
		
		// set items *************************************************************
		// no flickering when redrawing for XP
		popup.setRedraw(false);
		
		// insert data into table
		for (int rowIx = 0; rowIx < numOfRows; rowIx++)	{
			TableItem tableItem = new TableItem(table, SWT.NULL);
			tableItem.setText(items[rowIx]);
		}
		
		// optimize size of the columns
		for (int colIx = 0; colIx < numOfColumns; colIx++)	{
			table.getColumn(colIx).setWidth(1000);   // bug workaround for Windows XP which makes colums smaller but not bigger in pack()
			table.getColumn(colIx).pack();
		}
		
		// no flickering when redrawing for XP
		popup.setRedraw(true);
	}
	
	// (re)install event listeners
	int [] tableEvents = {SWT.MouseMove, SWT.MouseUp, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.FocusOut, SWT.MouseMove, SWT.DRAG, SWT.NONE, SWT.MouseDown, SWT.MouseExit, SWT.MouseEnter};
	for (int i = 0; i < tableEvents.length; i++)	{
		table.addListener(tableEvents[i], listener);
	}
	
	int [] popupEvents = {SWT.Close, SWT.Paint, SWT.Deactivate, SWT.MouseDown, SWT.MouseMove};
	for (int i = 0; i < popupEvents.length; i++)	{
		popup.addListener(popupEvents[i], listener);
	}
}

/**
 * Cuts the selected text.
 * <p>
 * The current selection is first copied to the
 * clipboard and then deleted from the widget.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.3
 */
// IO +++++
public void cut () {
	checkWidget ();
	text.cut ();
}
/**
 * Deselects the item at the given zero-relative index in the receiver's 
 * list.  If the item at the index was already deselected, it remains
 * deselected. Indices that are out of range are ignored.
 *
 * @param index the index of the item to deselect
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void deselect (int index) {
	checkWidget ();
	// index testing not necessary since out of range is ignored
	table.deselect(index);
}
/**
 * Deselects all selected items in the receiver's list.
 * <p>
 * Note: To clear the selection in the receiver's text field,
 * use <code>clearSelection().
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see #clearSelection
 */
// IO +++++
public void deselectAll() {
	checkWidget();
	text.setText("");  //$NON-NLS-1$
	table.deselectAll();
}
// IMPORTED
protected void resizeColums()	{
	checkWidget();
	int width = 0;
	for (int colIx = 0; colIx < table.getColumnCount(); colIx++)	{
		TableColumn currColumn = table.getColumns()[colIx];
		currColumn.setResizable(true);
		currColumn.pack();
		width = width + currColumn.getWidth();
	}
	table.pack();
}
// ALMOST IO  +++++
void dropDown(boolean drop) {
	if (drop == isDropped()) return;
	if ((table == null) || (table.getItemCount() == 0)) return;
	if (!drop) {
		popup.setVisible(false);
		if (!isDisposed() && isFocusControl()) {
		}
		return;
	}

	if (getShell() != popup.getParent()) {
		TableItem[] items = getItems();
		int selectionIndex = table.getSelectionIndex();
		table.removeListener(SWT.Dispose, listener);
		table = null;
		popup.dispose();
		popup = null;
		// recreate lists
		createPopup(items, selectionIndex);
	}
	
	int itemCount = table.getItemCount();
	itemCount = (itemCount == 0) ? visibleItemCount : Math.min(visibleItemCount, itemCount);
	int itemHeight = table.getItemHeight() * itemCount;
	
	// calculating size of Table and setting the size
	Point tableSize = table.computeSize(SWT.DEFAULT, itemHeight, false);
	int listWidth = tableSize.x;
	table.setBounds(0, 0, listWidth,  tableSize.y);
	
	// calculate and set size of popup
	// need the combosize, App main window, parent composite bounds, desktop
	Point comboSize = getSize();
	Display desktop = getDisplay();
	
	// recalc optimized column sizes
	resizeColums();
	
	// get table size, uncorrected
	tableSize = table.getSize();
	
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
	table.setBounds(leftMarginOffset, 0, tableSize.x, tableSize.y);
	
	int sbWidth = 0;
	if (!table.getVerticalBar().getVisible()){
		sbWidth = table.getVerticalBar().getSize().x;
		System.out.println("sbWidth: " + sbWidth);
	}
	popup.setBounds(rect.x, rect.y + comboSize.y, tableSize.x + 2 - sbWidth + leftMarginOffset, tableSize.y + 2);
	
	// ++++++++++++++++++++ find left margin width of table
	//System.out.println("table.getClientArea(): " + table.getClientArea());
	//System.out.println("table.getBounds(): " + table.getBounds());
	int fullWidth = table.getBounds().width;
	int addedColumnWidth = 0;
	for (int i = 0; i < table.getColumnCount(); i++){
		addedColumnWidth = addedColumnWidth + table.getColumn(i).getWidth();
	}
	//System.out.println("fullWidth: " + fullWidth);
	//System.out.println("addedColumnWidth: " + addedColumnWidth);
	//table.getColumn(0).
	
	// scroll selected item into view if necessary
	int currTop = table.getTopIndex();
	int currSel = table.getSelectionIndex();
	int numOfShownItems = table.getBounds().height / itemHeight;
	if ((currSel < currTop) || (currSel >= (currTop + numOfShownItems)))	{
		//if (savedTopIx != -1)	{
		//	table.setTopIndex(savedTopIx);
		//} //else {
			table.showSelection();
		//}
	}
	
	// make visible
	popup.setVisible(true);
	
	// donno if I should or not... +++++
	if (isFocusControl()) table.setFocus();
}
/*
 * Return the lowercase of the first non-'&' character following
 * an '&' character in the given string. If there are no '&'
 * characters in the given string, return '\0'.
 */
// IO +++++
char _findMnemonic(String string) {
	if (string == null) return '\0';
	int index = 0;
	int length = string.length();
	do {
		while (index < length && string.charAt(index) != '&') index++;
		if (++index >= length) return '\0';
		if (string.charAt(index) != '&') return Character.toLowerCase(string.charAt (index));
		index++;
	} while(index < length);
 	return '\0';
}
/* 
 * Return the Label immediately preceding the receiver in the z-order, 
 * or null if none. 
 */
// IO +++++
Label getAssociatedLabel() {
	Control[] siblings = getParent().getChildren();
	for (int i = 0; i < siblings.length; i++) {
		if (siblings [i] == this) {
			if (i > 0 && siblings [i-1] instanceof Label) {
				return (Label) siblings [i-1];
			}
		}
	}
	return null;
}
// IO +++++
public Control [] getChildren () {
	checkWidget();
	return new Control [0];
}
/**
 * Gets the editable state.
 *
 * @return whether or not the receiver is editable
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.0
 */
// IO +++++
public boolean getEditable () {
	checkWidget ();
	return text.getEditable();
}
/**
 * Returns the item at the given, zero-relative index in the
 * receiver's list. Throws an exception if the index is out
 * of range.
 *
 * @param index the index of the item to return
 * @return the item at the given index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */

// IO +++++
public String getCell(int columnIx, int rowIx) {
	checkWidget();
	if (rowIx < 0) return null;
	if (columnIx < 0) return null;
	int numOfRows = table.getItemCount();
	if (rowIx >= numOfRows) return null;
	TableItem tableItem = table.getItem(rowIx);
	if (tableItem == null) return null;
	int numOfCols = table.getColumnCount();
	if (columnIx >= numOfCols) return null;
	return tableItem.getText(columnIx);
}


// IO +++++
public TableItem getItem(int rowIx) {
	checkWidget();
	if (rowIx < 0) return null;
	if (rowIx >= table.getItemCount()) return null;
	return table.getItem(rowIx);
}
// IO +++++
public String[] getItemStringArray(int rowIx) {
	checkWidget();
	TableItem tableItem = getItem(rowIx);
	if (tableItem == null) return null;
	int numOfCols = table.getColumnCount();
	String[] stringCells = new String[numOfCols];
	for (int i = 0; i < numOfCols; i++)	{
		stringCells[i] = tableItem.getText(i);
	}
	return stringCells;
}

/**
 * Returns the number of items contained in the receiver's list.
 *
 * @return the number of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public int getItemCount() {
	checkWidget();
	return table.getItemCount();
}
/**
 * Returns the height of the area which would be used to
 * display <em>one of the items in the receiver's list.
 *
 * @return the height of one item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public int getItemHeight () {
	checkWidget();
	return table.getItemHeight();
}
/**
 * Returns an array of <code>Strings which are the items
 * in the receiver's list. 
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the items in the receiver's list
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
//IO +++++
public TableItem[] getItems()	{
	checkWidget();
	return table.getItems();
}
// NEW
public String[][] tableItemsToStrings(TableItem[] tableItems)	{
	int numOfColumns = tableItems.length;
	int numOfItems = tableItems[0].getParent().getColumnCount();
	String[][] stringItems = new String[numOfColumns][numOfItems];
	for (int colIx = 0 ; colIx < numOfColumns; colIx++){
		for (int rowIx = 0 ; rowIx < numOfColumns; rowIx++){
			stringItems[colIx][rowIx] = tableItems[rowIx].getText(colIx);
		}
	}
	return stringItems;
}
// IO +++++
public String[][] getItemsStringArray() {
	checkWidget();
	TableItem[] tableItems = getItems();
	String[][] stringItems = tableItemsToStrings(tableItems);
	return stringItems;
}
/**
 * Returns <code>true if the receiver's list is visible,
 * and <code>false otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, this method
 * may still indicate that it is considered visible even though
 * it may not actually be showing.
 * </p>
 *
 * @return the receiver's list's visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.4
 */
// IO +++++
public boolean getListVisible() {
	checkWidget();
	return isDropped();
}
// IO +++++
public Menu getMenu() {
	return text.getMenu();
}
/**
 * Returns a <code>Point whose x coordinate is the start
 * of the selection in the receiver's text field, and whose y
 * coordinate is the end of the selection. The returned values
 * are zero-relative. An "empty" selection as indicated by
 * the the x and y coordinates having the same value.
 *
 * @return a point representing the selection start and end
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public Point getSelection() {
	checkWidget();
	return text.getSelection();
}
/**
 * Returns the zero-relative index of the item which is currently
 * selected in the receiver's list, or -1 if no item is selected.
 *
 * @return the index of the selected item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public int getSelectionIndex() {
	checkWidget();
	return table.getSelectionIndex();
}
// IO +++++
public int getStyle() {
	int style = super.getStyle();
	style &= ~SWT.READ_ONLY;
	if (!text.getEditable()) style |= SWT.READ_ONLY; 
	return style;
}
/**
 * Returns a string containing a copy of the contents of the
 * receiver's text field.
 *
 * @return the receiver's text
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public String getText() {
	checkWidget();
	return text.getText();
}
/**
 * Returns the height of the receivers's text field.
 *
 * @return the text height
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public int getTextHeight() {
	checkWidget();
	return text.getLineHeight();
}
/**
 * Returns the maximum number of characters that the receiver's
 * text field is capable of holding. If this has not been changed
 * by <code>setTextLimit(), it will be the constant
 * <code>Combo.LIMIT.
 * 
 * @return the text limit
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public int getTextLimit() {
	checkWidget();
	return text.getTextLimit();
}
/**
 * Gets the number of items that are visible in the drop
 * down portion of the receiver's list.
 *
 * @return the number of items that are visible
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.0
 */
// IO +++++
public int getVisibleItemCount() {
	checkWidget();
	return visibleItemCount;
}
// IO +++++
void handleFocus(int type) {
	if (isDisposed()) return;
	switch (type) {
		case SWT.FocusIn: {
			if (hasFocus) return;
			if (getEditable()) text.selectAll();
			hasFocus = true;
			Shell shell = getShell();
			shell.removeListener(SWT.Deactivate, listener);
			shell.addListener(SWT.Deactivate, listener);
			Display display = getDisplay();
			display.removeFilter(SWT.FocusIn, filter);
			display.addFilter(SWT.FocusIn, filter);
			Event e = new Event();
			notifyListeners(SWT.FocusIn, e);
			break;
		}
		case SWT.FocusOut: {
			if (!hasFocus) return;
			Control focusControl = getDisplay().getFocusControl();
			if ((focusControl == arrow) || (focusControl == table)) return;
			hasFocus = false;
			Shell shell = getShell ();
			shell.removeListener(SWT.Deactivate, listener);
			Display display = getDisplay();
			display.removeFilter(SWT.FocusIn, filter);
			Event e = new Event();
			notifyListeners(SWT.FocusOut, e);
			break;
		}
	}
}
/**
 * Searches the receiver's list starting at the first item
 * (index 0) until an item is found that is equal to the 
 * argument, and returns the index of that item. If no item
 * is found, returns -1.
 *
 * @param string the search item
 * @return the index of the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */

// IO +++++
public int indexOf(int columnIx, String string) {
	return indexOf(columnIx, string, 0);
}
// IO +++++
public int indexOf(String string) {
	return indexOf(string, 0);
}
/**
 * Searches the receiver's list starting at the given, 
 * zero-relative index until an item is found that is equal
 * to the argument, and returns the index of that item. If
 * no item is found or the starting index is out of range,
 * returns -1.
 *
 * @param string the search item
 * @param start the zero-relative index at which to begin the search
 * @return the index of the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */

// IO +++++
public int indexOf(String string, int start) {
	checkWidget();
	if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	
	if (start < 0) return -1;
	TableItem[] tableItems = table.getItems();
	int numOfItems  = tableItems.length;
	if (start >= numOfItems) return -1;
	int numOfColums = table.getColumnCount();
	for (int i = start; i < numOfItems; i++){
		TableItem tableItem = tableItems[i];
		for (int colIx = 0; colIx < numOfColums; colIx++){
			String cellText = tableItem.getText(colIx);
			if (cellText.equals(string))	{
				return i;
			}
		}
	}
	return -1;
}


// IO +++++
public int indexOf(int columnIx, String string, int start) {
	checkWidget();
	if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (start < 0) return -1;
	int numOfColums = table.getColumnCount();
	if (columnIx >= numOfColums) return -1;
	TableItem[] tableItems = table.getItems();
	int numOfItems  = tableItems.length;
	if (start >= numOfItems) return -1;
	for (int i = start; i < numOfItems; i++){
		TableItem tableItem = tableItems[i];
		String cellText = tableItem.getText(columnIx);
		if (cellText.equals(string))	{
			return i;
		}
	}
	return -1;
}

// IO +++++
void initAccessible() {
	AccessibleAdapter accessibleAdapter = new AccessibleAdapter() {
		public void getName(AccessibleEvent e) {
			String name = null;
			Label label = getAssociatedLabel();
			if (label != null) {
				name = stripMnemonic(label.getText());
			}
			e.result = name;
		}
		public void getKeyboardShortcut(AccessibleEvent e) {
			String shortcut = null;
			Label label = getAssociatedLabel();
			if (label != null) {
				String text = label.getText();
				if (text != null) {
					char mnemonic = _findMnemonic(text);
					if (mnemonic != '\0') {
						shortcut = "Alt+"+mnemonic; //$NON-NLS-1$
					}
				}
			}
			e.result = shortcut;
		}
		public void getHelp(AccessibleEvent e) {
			e.result = getToolTipText();
		}
	};
	getAccessible().addAccessibleListener(accessibleAdapter);
	text.getAccessible().addAccessibleListener(accessibleAdapter);
	table.getAccessible().addAccessibleListener(accessibleAdapter);
	arrow.getAccessible().addAccessibleListener(new AccessibleAdapter() {
		public void getName(AccessibleEvent e) {
			e.result = isDropped() ? SWT.getMessage("SWT_Close") : SWT.getMessage("SWT_Open"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		public void getKeyboardShortcut(AccessibleEvent e) {
			e.result = "Alt+Down Arrow"; //$NON-NLS-1$
		}
		public void getHelp(AccessibleEvent e) {
			e.result = getToolTipText();
		}
	});
	
	getAccessible().addAccessibleTextListener(new AccessibleTextAdapter() {
		public void getCaretOffset(AccessibleTextEvent e) {
			e.offset = text.getCaretPosition();
		}
		public void getSelectionRange(AccessibleTextEvent e) {
			Point sel = text.getSelection();
			e.offset = sel.x;
			e.length = sel.y - sel.x;
		}
	});
	
	getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
		public void getChildAtPoint(AccessibleControlEvent e) {
			Point testPoint = toControl(e.x, e.y);
			if (getBounds().contains(testPoint)) {
				e.childID = ACC.CHILDID_SELF;
			}
		}
		
		public void getLocation(AccessibleControlEvent e) {
			Rectangle location = getBounds();
			Point pt = getParent().toDisplay(location.x, location.y);
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
			e.result = getText ();
		}
	});

	text.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter () {
		public void getRole(AccessibleControlEvent e) {
			e.detail = text.getEditable() ? ACC.ROLE_TEXT : ACC.ROLE_LABEL;
		}
	});

	arrow.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
		public void getDefaultAction(AccessibleControlEvent e) {
			e.result = isDropped () ? SWT.getMessage("SWT_Close") : SWT.getMessage("SWT_Open"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	});
}
// IO +++++
boolean isDropped() {
	return popup.getVisible();
}
// IO +++++
public boolean isFocusControl() {
	checkWidget();
	if (text.isFocusControl() || arrow.isFocusControl() || popup.isFocusControl() || table.isFocusControl()) {
		return true;
	}
	return super.isFocusControl();
}
// IO +++++
void internalLayout(boolean changed) {
	if (isDropped()) dropDown(false);
	Rectangle rect = getClientArea();
	int width  = rect.width;
	int height = rect.height;
	Point arrowSize = arrow.computeSize(SWT.DEFAULT, height, changed);
	text.setBounds(0, 0, width - arrowSize.x, height);
	arrow.setBounds(width - arrowSize.x, 0, arrowSize.x, 17/*arrowSize.y*/);
}
// IO +++++
void tableEvent(Event event) {
//	dropDown(false);
	switch(event.type) {
		case SWT.MouseMove:
			dbg("tableEvent: SWT.MouseMove", DBG_TableEvent);
			if (System.getProperties().getProperty("os.name").equals("Windows XP")){ 
			}
			int itemHeight = table.getItemHeight();
			if (isTrackingTable == false)	{
				//table.setCapture(true);
				System.out.println("List: MouseMove");
				int itemSel = (event.y - table.getHeaderHeight()) / itemHeight;
				int newSelection = table.getTopIndex() + itemSel;
				// windows behaviour
				Point pt = new Point((int) (MouseInfo.getPointerInfo().getLocation().getX()), (int) MouseInfo.getPointerInfo().getLocation().getY());
				pt = table.toControl(pt);
				drawFocus(newSelection);
			} else {
				table.setCapture(true);
				Point pt = new Point((int) (MouseInfo.getPointerInfo().getLocation().getX()), (int) MouseInfo.getPointerInfo().getLocation().getY());
				Point globalPt = pt;
				pt = table.toControl(pt);
				int itemSel = (pt.y - table.getHeaderHeight()) / itemHeight;
				int newSelection = table.getTopIndex() + itemSel;
				int currSel = table.getSelectionIndex();
				System.out.println("itemSel: " + itemSel);
//				System.out.println("newSelection: " + newSelection);
//				System.out.println("currSel: " + currSel);
				Rectangle bounds = popup.getBounds();
				bounds = new Rectangle(bounds.x, bounds.y, bounds.width - ((table.getVerticalBar() != null) ? table.getVerticalBar().getSize().x - leftMarginOffset : 0), bounds.height);
//				System.out.println("bounds" + bounds);
//				System.out.println("pt" + pt);
				if (bounds.contains(globalPt))	{
					//System.out.println("drinnen");
					if (currSel != newSelection)	{
						table.setSelection(newSelection);
						text.setText(table.getItem(newSelection).getText(textLinkedColumnIndex));
						text.selectAll();
					}
				} else {
					if (globalPt.y > (bounds.y + bounds.height)) {
						// scroll down
						table.setTopIndex(table.getTopIndex() + 1);
					} else	{
						if (globalPt.y < bounds.y) {
							// scroll up
							table.setTopIndex(table.getTopIndex() - 1);
							drawFocus(table.getTopIndex());
							System.out.println("table.getTopIndex(): " + table.getTopIndex());
						} else {
							if (table.getSelectionIndex() != startTrackingItem){
								table.setSelection(startTrackingItem);
								text.setText(table.getItem(startTrackingItem).getText(textLinkedColumnIndex));
								text.selectAll();
							}
							drawFocus(newSelection);
						}
					}
				}
			}
			break;
		case SWT.Dispose:
			dbg("tableEvent: SWT.Dispose", DBG_TableEvent);
			if (getShell() != popup.getParent()) {
				TableItem[] items = getItems();
				int selectionIndex = table.getSelectionIndex ();
				popup = null;
				table = null;
				createPopup(items, selectionIndex);
			}
			break;
		case SWT.FocusIn: {
			dbg("tableEvent: SWT.FocusIn", DBG_TableEvent);
			handleFocus(SWT.FocusIn);
			break;
		}
		case SWT.MouseUp: {
			dbg("tableEvent: SWT.MouseUp", DBG_TableEvent);
			if (event.button != 1) return;
			dropDown(false);
			mouseIsDownInList = false;

			Display desktop = getDisplay();
//			cmp = this;
//			while ((cmp = cmp.getParent()) != null)	{
//				cmp.removeListener(SWT.MouseMove, (Listener) tempMouseListener);
//			}
			desktop.removeListener(SWT.MouseMove, (Listener) tempMouseListener);
			break;
		}
		case SWT.Selection: {
			dbg("tableEvent: SWT.Selection", DBG_TableEvent);
			int index = table.getSelectionIndex();
			if (index == -1) return;
			text.setText(table.getItem(index).getText(textLinkedColumnIndex));
			text.selectAll();
			Event e = new Event();
			e.time      = event.time;
			e.stateMask = event.stateMask;
			e.doit      = event.doit;
			notifyListeners(SWT.Selection, e);
			event.doit = e.doit;
			savedTopIx = table.getTopIndex();
			break;
		}
		case SWT.Traverse: {
			dbg("tableEvent: SWT.Traverse", DBG_TableEvent);
			switch (event.detail) {
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_ARROW_PREVIOUS:
				case SWT.TRAVERSE_ARROW_NEXT:
					event.doit = false;
					break;
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
					event.doit = text.traverse(event.detail);
					event.detail = SWT.TRAVERSE_NONE;
					if (event.doit) dropDown(false);
					return;
			}
			Event e = new Event ();
			e.time      = event.time;
			e.detail    = event.detail;
			e.doit      = event.doit;
			e.character = event.character;
			e.keyCode   = event.keyCode;
			notifyListeners(SWT.Traverse, e);
			event.doit   = e.doit;
			event.detail = e.detail;
			break;
		}
		case SWT.KeyUp: {		
			dbg("tableEvent: SWT.KeyUp", DBG_TableEvent);
			Event e = new Event ();
			e.time      = event.time;
			e.character = event.character;
			e.keyCode   = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.KeyUp, e);
			break;
		}
		case SWT.MouseDown:	{
			dbg("tableEvent: SWT.MouseDown", DBG_TableEvent);
			event.doit = false;
			Event e = new Event ();
			e.time      = event.time;
			e.detail    = event.detail;
			e.doit      = false;
			e.character = event.character;
			e.keyCode   = event.keyCode;
			notifyListeners(SWT.MouseDown, e);
			event.doit = e.doit;
			mouseIsDownInList = true;

			
			isTrackingTable = true;
			Display desktop = getDisplay();
			table.setCapture(true);
			
			startTrackingItem = table.getSelectionIndex();
			
//			Composite cmp = this;
//			while ((cmp = cmp.getParent()) != null)	{
//				cmp.addListener(SWT.MouseMove, (Listener) tempMouseListener);
//			}
			desktop.addListener(SWT.MouseMove, (Listener) tempMouseListener);

			break;
		}
		case SWT.KeyDown: {
			dbg("tableEvent: SWT.KeyDown", DBG_TableEvent);
			if (event.character == SWT.ESC) { 
				// Escape key cancels popup list
				dropDown(false);
			}
			if ((event.stateMask & SWT.ALT) != 0 && (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN)) {
				dropDown(false);
			}
			if (event.character == SWT.CR) {
				// Enter causes default selection
				dropDown(false);
				Event e = new Event();
				e.time      = event.time;
				e.stateMask = event.stateMask;
				notifyListeners(SWT.DefaultSelection, e);
			}
			// At this point the widget may have been disposed.
			// If so, do not continue.
			if (isDisposed ()) break;
			Event e = new Event();
			e.time      = event.time;
			e.character = event.character;
			e.keyCode   = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.KeyDown, e);
			break;
		}
	}
	if ((event.type != SWT.MouseMove) && (event.type != SWT.MouseDown) && (event.type != SWT.MouseUp) && (event.type != SWT.Selection))	{
		isTrackingTable = false;
		table.setCapture(false);
	}
	if (event.type == SWT.MouseUp)	{
		focusItem = -1;
		focusItemLastTopIx = -1;
	}
}
/**
 * Pastes text from clipboard.
 * <p>
 * The selected text is deleted from the widget
 * and new text inserted from the clipboard.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.3
 */
// IO +++++
public void paste () {
	checkWidget();
	text.paste();
}
// IO +++++
void popupEvent(Event event) {
	switch(event.type) {
		case SWT.MouseMove:
			tableEvent(event);
			break;
		case SWT.MouseDown:
			System.out.println("popup MouseDown");
			break;
		case SWT.Paint:
			break;
		case SWT.Close:
			event.doit = false;
			dropDown(false);
			break;
		case SWT.Deactivate:
			/*
			 * Bug in GTK. When the arrow button is pressed the popup control receives a
			 * deactivate event and then the arrow button receives a selection event. If 
			 * we hide the popup in the deactivate event, the selection event will show 
			 * it again. To prevent the popup from showing again, we will let the selection 
			 * event of the arrow button hide the popup.
			 * In Windows, hiding the popup during the deactivate causes the deactivate 
			 * to be called twice and the selection event to be disappear.
			 */
			if (!"carbon".equals(SWT.getPlatform())) {
				Point point = arrow.toControl(getDisplay().getCursorLocation());
				Point size = arrow.getSize();
				Rectangle rect = new Rectangle(0, 0, size.x, size.y);
				if (!rect.contains(point)) dropDown(false);
			} else {
				dropDown(false);
			}
			break;
	}
}
// IO +++++
public void redraw() {
	super.redraw();
	text. redraw();
	arrow.redraw();
	if (popup.isVisible())	{
		table.redraw();
	}
}
// IO +++++
public void redraw(int x, int y, int width, int height, boolean all) {
	super.redraw(x, y, width, height, true);
}

/**
 * Removes the item from the receiver's list at the given
 * zero-relative index.
 *
 * @param index the index for the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void remove(int index) {
	checkWidget();
	table.remove(index);
}
/**
 * Removes the items from the receiver's list which are
 * between the given zero-relative start and end 
 * indices (inclusive).
 *
 * @param start the start of the range
 * @param end the end of the range
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if either the start or end are not between 0 and the number of elements in the list minus 1 (inclusive)
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void remove(int start, int end) {
	checkWidget();
	table.remove(start, end);
}
/**
 * Searches the receiver's list starting at the first item
 * until an item is found that is equal to the argument, 
 * and removes that item from the list.
 *
 * @param string the item to remove
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null
 *    <li>ERROR_INVALID_ARGUMENT - if the string is not found in the list
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
/*
//+++++ Data
public void remove(String string) {
	checkWidget();
	if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	int firstFound = indexOf(string);
	if (firstFound < 0) return;
	for (int i = 0; i < lists.length; i++)	{
		lists[i].remove(firstFound);
	}
}
*/
/*
//+++++ Data
public void remove(int columnIx, String string) {
	checkWidget();
	if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	int firstFound = indexOf(columnIx, string);
	if (firstFound < 0) return;
	for (int i = 0; i < lists.length; i++)	{
		lists[i].remove(firstFound);
	}
}
*/
/**
 * Removes all of the items from the receiver's list and clear the
 * contents of receiver's text field.
 * <p>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void removeAll() {
	checkWidget();
	text.setText(""); //$NON-NLS-1$
	table.removeAll();
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the receiver's text is modified.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see ModifyListener
 * @see #addModifyListener
 */
// IO +++++
public void removeModifyListener(ModifyListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	removeListener(SWT.Modify, listener);	
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the user changes the receiver's selection.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see SelectionListener
 * @see #addSelectionListener
 */
// IO +++++
public void removeSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	removeListener(SWT.Selection, listener);
	removeListener(SWT.DefaultSelection,listener);	
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control is verified.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 *
 * @see VerifyListener
 * @see #addVerifyListener
 * 
 * @since 3.3
 */
// IO +++++
public void removeVerifyListener(VerifyListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	removeListener(SWT.Verify, listener);
}
/**
 * Selects the item at the given zero-relative index in the receiver's 
 * list.  If the item at the index was already selected, it remains
 * selected. Indices that are out of range are ignored.
 *
 * @param index the index of the item to select
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void select(int index) {
	checkWidget();
	if (index == -1) {
		table.deselectAll();
		text.setText (""); //$NON-NLS-1$
		return;
	}
	if (0 <= index && index < table.getItemCount()) {
		if (index != getSelectionIndex()) {
			text.setText(table.getItem(index).getText(textLinkedColumnIndex));
			text.selectAll();
			table.select(index);
			table.showSelection();  // +++++ really???
		}
	}
}
// IO +++++
public void setBackground(Color color) {
	super.setBackground(color);
	background = color;
	if (text  != null) text. setBackground(color);
	if (table != null) table.setBackground(color);
	if (arrow != null) arrow.setBackground(color);
}
/**
 * Sets the editable state.
 *
 * @param editable the new editable state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.0
 */
// IO +++++
public void setEditable(boolean editable) {
	checkWidget();
	text.setEditable(editable);
}
// IO +++++
public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	if (popup != null) popup.setVisible(false);
	if (text  != null) text. setEnabled(enabled);
	if (arrow != null) arrow.setEnabled(enabled);
}
// IO +++++
public boolean setFocus() {
	checkWidget();
	if (!isEnabled() || !isVisible()) return false;
	if (isFocusControl()) return true;
	return text.setFocus();
}
// IO +++++
public void setFont(Font font) {
	this.font = font;
	super.setFont(font);
	text. setFont(font);
	table.setFont(font);
	internalLayout(true);
}
// IO +++++
public void setForeground(Color color) {
	super.setForeground(color);
	foreground = color;
	if (text  != null) text. setForeground(color);
	if (table != null) table.setForeground(color);
	if (arrow != null) arrow.setForeground(color);
}
/**
 * Sets the text of the item in the receiver's list at the given
 * zero-relative index to the string argument. This is equivalent
 * to <code>remove'ing the old item at the index, and then
 * <code>add'ing the new item at that index.
 *
 * @param index the index for the item
 * @param string the new text for the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)
 *    <li>ERROR_NULL_ARGUMENT - if the string is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void setCell(int columnIx, int index, String string) {
	checkWidget();
	if ((columnIx < 0) || (columnIx >= table.getItemCount()))	{
		return;
	}
	TableItem tableItem = table.getItem(index);
	tableItem.setText(columnIx, string);
}
//IO +++++
public void setItem(int rowIx, String[] items) {
	checkWidget();
	if (rowIx < 0) return;
	int numOfRows = table.getItemCount();
	if (rowIx >= numOfRows) return;
	TableItem tableItem = table.getItem(rowIx);
	if (tableItem == null) return;
	tableItem.setText(items);
}

/**
 * Sets the receiver's list to be the given array of items.
 *
 * @param items the array of items
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the items array is null
 *    <li>ERROR_INVALID_ARGUMENT - if an item in the items array is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
/*
//+++++ Data
public void setItems(int columnIx, String[] items) {
	checkWidget();
	if ((columnIx < 0) || (columnIx >= table.getItemCount()))	{
		return;
	}
	lists[columnIx].setItems(items);
	if (!text.getEditable()) text.setText(""); //$NON-NLS-1$
}
*/
// IO +++++
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
/*	checkWidget ();
	int numOfLists = items.length;
	createLists(numOfLists);
	for (int i = 0; i < numOfLists; i++)	{
		String[] itm = items[i];
		lists[i].setItems(itm);
	}
	if (!text.getEditable()) text.setText(""); //$NON-NLS-1$
*/
}
/**
 * Sets the layout which is associated with the receiver to be
 * the argument which may be null.
 * <p>
 * Note: No Layout can be set on this Control because it already
 * manages the size and position of its children.
 * </p>
 *
 * @param layout the receiver's new layout or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void setLayout(Layout layout) {
	checkWidget();
	return;
}
/**
 * Marks the receiver's list as visible if the argument is <code>true,
 * and marks it invisible otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, marking
 * it visible may not actually cause it to be displayed.
 * </p>
 *
 * @param visible the new visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.4
 */
// IO +++++
public void setListVisible(boolean visible) {
	checkWidget();
	dropDown(visible);
}
// IO +++++
public void setMenu(Menu menu) {
	text.setMenu(menu);
}
/**
 * Sets the selection in the receiver's text field to the
 * range specified by the argument whose x coordinate is the
 * start of the selection and whose y coordinate is the end
 * of the selection. 
 *
 * @param selection a point representing the new selection start and end
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void setSelection(Point selection) {
	checkWidget();
	if (selection == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	text.setSelection(selection.x, selection.y);
}

/**
 * Sets the contents of the receiver's text field to the
 * given string.
 * <p>
 * Note: The text field in a <code>Combo is typically
 * only capable of displaying a single line of text. Thus,
 * setting the text to a string containing line breaks or
 * other special characters will probably cause it to 
 * display incorrectly.
 * </p>
 *
 * @param string the new text
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */

//+++++ Data
public void setText(String string) {
	if (1==1) return;
	checkWidget();
	if (string == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	int index = indexOf(textLinkedColumnIndex, string);
	if (index == -1) {
		table.deselectAll();
		text.setText(string);
		return;
	}
	text.setText(string);
	text.selectAll();
	table.setSelection(index);
	table.showSelection();
}

/**
 * Sets the maximum number of characters that the receiver's
 * text field is capable of holding to be the argument.
 *
 * @param limit new text limit
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_CANNOT_BE_ZERO - if the limit is zero
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 */
// IO +++++
public void setTextLimit(int limit) {
	checkWidget();
	text.setTextLimit(limit);
}
// IO +++++
public void setToolTipText(String string) {
	checkWidget();
	super.setToolTipText(string);
	arrow.setToolTipText(string);
	text. setToolTipText(string);		
}
// IO +++++
public void setVisible(boolean visible) {
	super.setVisible(visible);
	/* 
	 * At this point the widget may have been disposed in a FocusOut event.
	 * If so then do not continue.
	 */
	if (isDisposed()) return;
	if (!visible) popup.setVisible(false);
}
/**
 * Sets the number of items that are visible in the drop
 * down portion of the receiver's list.
 *
 * @param count the new number of items to be visible
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver
 * </ul>
 * 
 * @since 3.0
 */
// IO +++++
public void setVisibleItemCount(int count) {
	checkWidget();
	if (count < 0) return;
	visibleItemCount = count;
}
// iO +++++
String stripMnemonic(String string) {
	int index = 0;
	int length = string.length();
	do {
		while((index < length) && (string.charAt(index) != '&')) index++;
		if (++index >= length) return string;
		if (string.charAt(index) != '&') {
			return string.substring(0, index-1) + string.substring(index, length);
		}
		index++;
	} while(index < length);
 	return string;
}
//DONE +++++ LIST
void textEvent(Event event) {
	switch (event.type) {
		case SWT.FocusIn: {
			dbg("textEvent: SWT.FocusIn", DBG_TextEvent);
			handleFocus(SWT.FocusIn);
			break;
		}
		case SWT.DefaultSelection: {
			dbg("textEvent: SWT.DefaultSelection", DBG_TextEvent);
			dropDown(false);
			Event e = new Event ();
			e.time      = event.time;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.DefaultSelection, e);
			break;
		}
		case SWT.KeyDown: {
			dbg("textEvent: SWT.KeyDown", DBG_TextEvent);
			Event keyEvent = new Event ();
			keyEvent.time      = event.time;
			keyEvent.character = event.character;
			keyEvent.keyCode   = event.keyCode;
			keyEvent.stateMask = event.stateMask;
			notifyListeners(SWT.KeyDown, keyEvent);
			if (isDisposed()) break;
			event.doit = keyEvent.doit;
			if (!event.doit) break;
			if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN) {
				event.doit = false;
				if ((event.stateMask & SWT.ALT) != 0) {
					boolean dropped = isDropped();
					text.selectAll();
					if (!dropped) setFocus();
					dropDown(!dropped);
					break;
				}
				
				int oldIndex = getSelectionIndex();
				if (event.keyCode == SWT.ARROW_UP) {
					select (Math.max(oldIndex - 1, 0));
				} else {
					select(Math.min(oldIndex + 1, getItemCount() - 1));
				}
				if (oldIndex != getSelectionIndex()) {
					Event e = new Event();
					e.time      = event.time;
					e.stateMask = event.stateMask;
					notifyListeners(SWT.Selection, e);
				}
				if (isDisposed()) break;
			}
			
			// Further work : Need to add support for incremental search in 
			// pop up list as characters typed in text widget
			break;
		}
		case SWT.KeyUp: {
			dbg("textEvent: SWT.KeyUp", DBG_TextEvent);
			Event e = new Event();
			e.time      = event.time;
			e.character = event.character;
			e.keyCode   = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.KeyUp, e);
			event.doit = e.doit;
			break;
		}
		case SWT.MenuDetect: {
			dbg("textEvent: SWT.MenuDetect", DBG_TextEvent);
			Event e = new Event();
			e.time = event.time;
			notifyListeners(SWT.MenuDetect, e);
			break;
		}
		case SWT.Modify: {
			dbg("textEvent: SWT.Modify", DBG_TextEvent);
/////////			table.deselectAll();
			Event e = new Event();
			e.time = event.time;
			notifyListeners(SWT.Modify, e);
			break;
		}
		case SWT.MouseDown: {
			dbg("textEvent: SWT.MouseDown", DBG_TextEvent);
			Event mouseEvent = new Event();
			mouseEvent.button    = event.button;
			mouseEvent.count     = event.count;
			mouseEvent.stateMask = event.stateMask;
			mouseEvent.time      = event.time;
			mouseEvent.x         = event.x;
			mouseEvent.y         = event.y;
			notifyListeners(SWT.MouseDown, mouseEvent);
			if (isDisposed()) break;
			event.doit = mouseEvent.doit;
			if (!event.doit) break;
			if (event.button != 1) return;
			if (text.getEditable()) return;
			boolean dropped = isDropped();
			text.selectAll();
			if (!dropped) setFocus();
			dropDown(!dropped);
			break;
		}
		case SWT.MouseUp: {
			dbg("textEvent: SWT.MouseUp", DBG_TextEvent);
			Event mouseEvent = new Event();
			mouseEvent.button    = event.button;
			mouseEvent.count     = event.count;
			mouseEvent.stateMask = event.stateMask;
			mouseEvent.time      = event.time;
			mouseEvent.x         = event.x;
			mouseEvent.y         = event.y;
			notifyListeners(SWT.MouseUp, mouseEvent);
			if (isDisposed()) break;
			event.doit = mouseEvent.doit;
			if (!event.doit) break;
			if (event.button != 1) return;
			if (text.getEditable()) return;
			isTrackingTable = false;
			text.selectAll();
			break;
		}
		case SWT.MouseDoubleClick: {
			dbg("textEvent: SWT.MouseDoubleClick", DBG_TextEvent);
			Event mouseEvent = new Event();
			mouseEvent.button    = event.button;
			mouseEvent.count     = event.count;
			mouseEvent.stateMask = event.stateMask;
			mouseEvent.time      = event.time;
			mouseEvent.x         = event.x;
			mouseEvent.y         = event.y;
			notifyListeners(SWT.MouseDoubleClick, mouseEvent);
			break;
		}
		case SWT.MouseWheel: {
			dbg("textEvent: SWT.MouseWheel", DBG_TextEvent);
			Event keyEvent = new Event();
			keyEvent.time      = event.time;
			keyEvent.keyCode   = event.count > 0 ? SWT.ARROW_UP : SWT.ARROW_DOWN;
			keyEvent.stateMask = event.stateMask;
			notifyListeners(SWT.KeyDown, keyEvent);
			if (isDisposed()) break;
			event.doit = keyEvent.doit;
			if (!event.doit) break;
			if (event.count != 0) {
				event.doit = false;
				int oldIndex = getSelectionIndex();
				if (event.count > 0) {
					select(Math.max(oldIndex - 1, 0));
				} else {
					select(Math.min(oldIndex + 1, getItemCount() - 1));
				}
				if (oldIndex != getSelectionIndex()) {
					Event e = new Event();
					e.time      = event.time;
					e.stateMask = event.stateMask;
					notifyListeners(SWT.Selection, e);
				}
				if (isDisposed()) break;
			}
			break;
		}
		case SWT.Traverse: {		
			dbg("textEvent: SWT.Traverse", DBG_TextEvent);
			switch (event.detail) {
				case SWT.TRAVERSE_ARROW_PREVIOUS:
				case SWT.TRAVERSE_ARROW_NEXT:
					// The enter causes default selection and
					// the arrow keys are used to manipulate the list contents so
					// do not use them for traversal.
					event.doit = false;
					break;
				case SWT.TRAVERSE_TAB_PREVIOUS:
					event.doit = traverse(SWT.TRAVERSE_TAB_PREVIOUS);
					event.detail = SWT.TRAVERSE_NONE;
					return;
			}		
			Event e = new Event();
			e.time      = event.time;
			e.detail    = event.detail;
			e.doit      = event.doit;
			e.character = event.character;
			e.keyCode   = event.keyCode;
			notifyListeners(SWT.Traverse, e);
			event.doit   = e.doit;
			event.detail = e.detail;
			break;
		}
		case SWT.Verify: {
			dbg("textEvent: SWT.Verify", DBG_TextEvent);
			Event e = new Event();
			e.text      = event.text;
			e.start     = event.start;
			e.end       = event.end;
			e.character = event.character;
			e.keyCode   = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.Verify, e);
			event.doit = e.doit;
			break;
		}
	}
}

public void dbgSet(long debugType)	{
	printWhat = printWhat | debugType;
}

public void dbgUnset(long debugType)	{
	printWhat = printWhat ^ debugType;
}

public void dbg(final String msg, long debugType)	{
	if ((printWhat & debugType) != 0)	{
		System.out.println(msg);
	}
}
/**
 * Draws a focus rect around the selected line specified by the param item
 * @param item
 */
// 
void drawFocus(int item)	{
	// redraw old focus rect
	// draw focus for this item
	// except a line that may be selected
	int currSel = table.getSelectionIndex();
	int itemInList = item - table.getTopIndex();
	if (item == -1)	{
//		if (1==1) return;
		System.out.println("item == " + item);
		itemInList = table.getItemCount();
	}
	if (itemInList == focusItem)	{
		return;
	}
	int itemHeight = table.getItemHeight();
		
	Rectangle popupRect = popup.getBounds();
	Point pt = new Point(popupRect.x, popupRect.y);
	pt = popup.toControl(pt);
	
	int vOffset;
	GC listGC = new GC(table);
	Rectangle origFocus;
	
	// *** undraw old focus ***************************************************
	vOffset = focusItem * itemHeight;
	origFocus = table.getBounds();
	origFocus = new Rectangle(0, 0,	origFocus.width, origFocus.height - origFocus.y);
	table.redraw(origFocus.x - leftMarginOffset, pt.y + vOffset + 1, origFocus.width - ((table.getVerticalBar() != null) ? table.getVerticalBar().getSize().x - leftMarginOffset : 0), itemHeight, true);
	// *** draw new focus ***************************************************
	if (item != currSel)	{
		vOffset = itemInList * itemHeight;
		origFocus = table.getBounds();
		origFocus = new Rectangle(0, 0,	origFocus.width, origFocus.height - origFocus.y);
		listGC.drawFocus(origFocus.x - leftMarginOffset, pt.y + vOffset + 1, origFocus.width - ((table.getVerticalBar() != null) ? table.getVerticalBar().getSize().x - leftMarginOffset : 0), itemHeight);
	}
	
	listGC.dispose();
	
	focusItem = itemInList;
	focusItemLastTopIx = table.getTopIndex();
}

}