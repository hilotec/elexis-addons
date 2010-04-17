/*******************************************************************************
 * Copyright (c) 2007, medshare and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    M. Imhof - initial implementation
 *    
 * $Id: WeisseSeitenSearchForm.java 4646 2008-10-29 07:39:39Z michael_imhof $
 *******************************************************************************/

package ch.marlovits.addressSearch.views;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import ch.elexis.dialogs.KontaktErfassenDialog;
import ch.elexis.dialogs.PatientErfassenDialog;
import ch.elexis.util.SWTHelper;
import ch.marlovits.addressSearch.directories.PhoneBookContentParser;

public class PhoneBookSearchForm extends Composite {

	private final ListenerList listeners = new ListenerList();

	private List<HashMap<String, String>> kontakte;
	//private List<HashMap<String, String>> kontakte = new Vector<KontaktEntry>();

	private String searchInfoText = "";
	private Composite parentComposite;
	private Shell hitListComposite = null;
	private Button hitListCloseBox;
	private org.eclipse.swt.widgets.List hitList = null;
	private Composite infoComposite;
	private Text searchInfoTextField;
	Button previousBtn;
	Button nextBtn;
	private Text nameText;
	private Combo geoText;
	
	private int entriesToRead = 10;
	private int startEntryIndex = 0;
	private int numOfPages = 0;
	private int numOfEntries = 1;
	private String country = "ch";
	private PhoneBookContentParser parser = null;

	int			leftMarginOffset = 4;   // +++++ for WindowsXP

	public PhoneBookContentParser getPhoneBookContentParser()	{
		return parser;
	}
	
	public PhoneBookSearchForm(Composite parent, int style) {
		super(parent, style);
		createPartControl(parent);
	}

	private void createPartControl(final Composite parent) {
		parentComposite = parent;
		
		/*
		parent.addDragDetectListener(new DragDetectListener()	{
			@Override
			public void dragDetected(DragDetectEvent e) {
				SWTHelper.alert("", "dragDetected1");
			}
		}
		);
		parent.getParent().addDragDetectListener(new DragDetectListener()	{
			@Override
			public void dragDetected(DragDetectEvent e) {
				SWTHelper.alert("", "dragDetected2");
			}
		}
		);
		parent.getParent().getParent().addDragDetectListener(new DragDetectListener()	{
			@Override
			public void dragDetected(DragDetectEvent e) {
				SWTHelper.alert("", "dragDetected3");
			}
		}
		);
		parent.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				SWTHelper.alert("", "focusGained");
			}
			@Override
			public void focusLost(FocusEvent e) {
				SWTHelper.alert("", "focusLost");
			}
		});
		parent.addControlListener(new ControlListener(){
			@Override
			public void controlMoved(ControlEvent e) {
				if (hitListComposite != null)	{
					hitListComposite.dispose();
					hitListComposite = null;
				}
				if (hitListComposite == null)	{
					createHitListComposite(parent);
				}
				repositionHitList();
			}
			@Override
			public void controlResized(ControlEvent e) {
				if (hitListComposite != null)	{
					hitListComposite.dispose();
					hitListComposite = null;
				}
				if (hitListComposite == null)	{
					createHitListComposite(parent);
				}
				repositionHitList();
			}
		}
		);
		parent.getParent().addControlListener(new ControlListener(){
			@Override
			public void controlMoved(ControlEvent e) {
				if (hitListComposite != null)	{
					hitListComposite.dispose();
					hitListComposite = null;
				}
				if (hitListComposite == null)	{
					createHitListComposite(parent);
				}
				repositionHitList();
			}
			@Override
			public void controlResized(ControlEvent e) {
				if (hitListComposite != null)	{
					hitListComposite.dispose();
					hitListComposite = null;
				}
				if (hitListComposite == null)	{
					createHitListComposite(parent);
				}
				repositionHitList();
			}
		}
		);
		
		parent.getParent().getParent().addControlListener(new ControlListener(){
			@Override
			public void controlMoved(ControlEvent e) {
				if (hitListComposite != null)	{
					hitListComposite.dispose();
					hitListComposite = null;
				}
				if (hitListComposite == null)	{
					createHitListComposite(parent);
				}
				repositionHitList();
			}
			@Override
			public void controlResized(ControlEvent e) {
				if (hitListComposite != null)	{
					hitListComposite.dispose();
					hitListComposite = null;
				}
				if (hitListComposite == null)	{
					createHitListComposite(parent);
				}
				repositionHitList();
			}
		}
		);
		*/
		createParser();
		
		setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		parent.getParent().addControlListener(new ControlListener(){
			@Override
			public void controlMoved(ControlEvent e) {
				if (hitListComposite != null)	{
					hitListComposite.dispose();
					hitListComposite = null;
				}
				if (hitListComposite == null)	{
					createHitListComposite(parent);
				}
				repositionHitList();
			}
			@Override
			public void controlResized(ControlEvent e) {
				if (hitListComposite != null)	{
					hitListComposite.dispose();
					hitListComposite = null;
				}
				if (hitListComposite == null)	{
					createHitListComposite(parent);
				}
				repositionHitList();
			}
		}
		);
		
		setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		setLayout(new GridLayout(1, false));
		setLayout(new GridLayout(3, false));
		
		Composite comp1 = new Composite(this, SWT.NONE);
		comp1.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		comp1.setLayout(new GridLayout(3, false));
		
		Label nameLabel = new Label(comp1, SWT.NONE);
		nameLabel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		nameLabel.setText(Messages.getString("WeisseSeitenSearchForm.label.werWasWo")); //$NON-NLS-1$
		
		Label geoLabel = new Label(comp1, SWT.NONE);
		geoLabel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		geoLabel.setText(Messages.getString("WeisseSeitenSearchForm.label.Ort")); //$NON-NLS-1$
		
		new Label(comp1, SWT.NONE); // Platzhalter
		
		nameText = new Text(comp1, SWT.BORDER);
		nameText.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		geoText = new Combo(comp1, SWT.BORDER);
		geoText.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		geoText.addKeyListener(new KeyListener()	{
			@Override
			public void keyPressed(KeyEvent e) {
			}
			@Override
			public void keyReleased(KeyEvent e) {
				if (parser != null)	{
					int currLen = geoText.getText().length();
					String[][] citiesList = {{""}, {""}};
					if (currLen > 0)	{
						citiesList = parser.getCitySuggestions(geoText.getText());
					}
					if (hitListComposite != null)	{
						hitListComposite.dispose();
						hitListComposite = null;
					}
					if (hitListComposite == null)	{
						createHitListComposite(parent);
					}
					if ((citiesList.length > 0) && (hitListComposite != null)) {
						fillCitiesCombo(citiesList);
						repositionHitList();
						hitListComposite.setVisible(true);
					}
					repositionHitList();
				}
			}
			
		});
		geoText.addSelectionListener(new SelectionListener()	{
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				String currText = geoText.getText();
				if (currText.startsWith(" ***"))	{
					SWTHelper.showInfo("", "Dieser Punkt kann nicht ausgewählt werden!");
					geoText.setText("");
				}
			}
		});
		
		Button searchBtn = new Button(comp1, SWT.NONE);
		searchBtn.setText(Messages.getString("WeisseSeitenSearchForm.btn.Suchen")); //$NON-NLS-1$
		
		infoComposite = new Composite(comp1, SWT.NONE);
		infoComposite.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));		
		GridLayout gl1 = new GridLayout(6, false);
		gl1.marginWidth     =  0;
		gl1.verticalSpacing =  0;
		gl1.marginTop       = -5;    // strange...
		gl1.marginBottom    = -5;    // strange...
		infoComposite.setLayout(gl1);
		
		searchInfoTextField = new Text(infoComposite, SWT.NONE);
		searchInfoTextField.setEnabled(false);
		searchInfoTextField.setText(searchInfoText);
		
		previousBtn = new Button(infoComposite, SWT.NONE);
		previousBtn.setText("<");
		previousBtn.setEnabled(true);
		previousBtn.setVisible(false);
		previousBtn.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println("numOfPages: " + numOfPages);
				System.out.println("startEntryIndex: " + startEntryIndex);
				if (startEntryIndex >= 0)	{
					startEntryIndex = (startEntryIndex == 0) ? 0 : startEntryIndex - 1;
					System.out.println("startEntryIndex: " + startEntryIndex);
					searchAction(nameText.getText(), geoText.getText());
				}
				if (startEntryIndex == 0)	{
					previousBtn.setEnabled(false);
				} else	{
					previousBtn.setEnabled(true);
				}
			}
		});
		
		nextBtn = new Button(infoComposite, SWT.NONE);
		nextBtn.setText(">");
		nextBtn.setEnabled(true);
		nextBtn.setVisible(false);
		nextBtn.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println("numOfPages: " + numOfPages);
				System.out.println("startEntryIndex: " + startEntryIndex);
				if (startEntryIndex < numOfPages+1)	{
					startEntryIndex = startEntryIndex + 1;
					startEntryIndex = (startEntryIndex > numOfPages) ? numOfPages : startEntryIndex;
					System.out.println("startEntryIndex: " + startEntryIndex);
					searchAction(nameText.getText(), geoText.getText());
					previousBtn.setEnabled(true);
				}
				if (startEntryIndex >= numOfPages)	{
					nextBtn.setEnabled(false);
				} else	{
					nextBtn.setEnabled(true);
				}
		}
		});
		
		nameText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR) {
					startEntryIndex = 0;
					searchAction(nameText.getText(), geoText.getText());
				}
			}
		});
		
		geoText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR) {
					startEntryIndex = 0;
					searchAction(nameText.getText(), geoText.getText());
				}
			}
		});
		
		geoText.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				if (hitListComposite == null) return;
				hitListComposite.setVisible(true);
				repositionHitList();
			}
			@Override
			public void focusLost(FocusEvent e) {
				if (hitListComposite == null) return;
				hitListComposite.setVisible(false);
			}
		});
		
		geoText.addControlListener(new ControlListener() {
			@Override
			public void controlMoved(ControlEvent e) {
				System.out.println("controlMoved");
			}
			@Override
			public void controlResized(ControlEvent e) {
				System.out.println("controlResized");
			}
		});
		
		searchBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				startEntryIndex = 0;
				searchAction(nameText.getText(), geoText.getText());
			}
		});
	}
	
	private void repositionHitList()	{
		if (hitListComposite == null) return;
		if (hitList.getItemCount() > 0)	{
			Display desktop = getDisplay();
			if (desktop == null) return;
			Rectangle rect = desktop.map(getParent(), null, geoText.getBounds());
			Point bestSize = hitList.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			hitListComposite.setBounds(	rect.x + getBounds().x + leftMarginOffset + 1,
										rect.y + getBounds().y + leftMarginOffset + geoText.getBounds().height,
										bestSize.x,
										bestSize.y);
			hitList.setBounds(-1, -1, bestSize.x, bestSize.y);
			hitListComposite.setVisible(true);
		} else	{
			hitListComposite.setVisible(false);
		}
	}
	
	private void createHitListComposite(Composite parent)	{
		// hitsList composite
		hitListComposite = new Shell (getShell (), SWT.ON_TOP | SWT.TOOL);
		hitList = new org.eclipse.swt.widgets.List(hitListComposite, SWT.BORDER | SWT.V_SCROLL);
		hitListComposite.moveAbove(null);
		repositionHitList();
	}
	
	private int getMaxStringWidth()	{
		// loop through items of column linked to text field, find widest string (in pixels)
		GC gc = new GC(geoText);
		int spacer = gc.stringExtent(" ").x; //$NON-NLS-1$
		int textWidth = gc.stringExtent(geoText.getText()).x;
		String[] items = hitList.getItems();
		for (int i = 0; i < items.length; i++) {
			String str = items[i];
			textWidth = Math.max(gc.stringExtent(items[i]).x, textWidth);
		}
		gc.dispose();
		return textWidth;
	}
	
	private void setSearchText()	{
	//private void setSearchText(PhoneBookContentParser parser)	{
		boolean visible = true;
		if (parser != null)	{
			int numOfEntries = parser.getNumOfEntries();
			searchInfoText = parser.getSearchInfo();
			searchInfoTextField.setText(searchInfoText);
			infoComposite.pack(true);
			visible = true;
			numOfPages = (numOfEntries - 1) / entriesToRead;
			previousBtn.setEnabled(false);
			if (numOfPages > 0)	{
				nextBtn.setEnabled(true);
			}
		} else	{
			visible = false;
			numOfPages = 0;
		}
		searchInfoTextField.setVisible(visible);
		previousBtn.setVisible(visible);
		nextBtn.setVisible(visible);
	}
	
	public void setCountry(final String countryIso2)	{
		country = countryIso2;
		createParser();
		String[][] citiesList = parser.getCitySuggestions(geoText.getText());
		fillCitiesCombo(citiesList);
		repositionHitList();
	}
	
	public String getCountry()	{
		return country;
	}
	
	/**
	 * create the content parser for the currently selected country
	 */
	private void createParser()	{
		// *** call constructor for parser for currently selected country
		try {
			System.out.println("blubb");
			Class<?> cls;
			cls = Class.forName("ch.marlovits.addressSearch.directories.PhoneBookContentParser_" + country);
			Class<?> partypes[] = new Class[3];
			partypes[0] = String.class;
			partypes[1] = String.class;
			partypes[2] = int.class;
			Constructor<?> ct = cls.getConstructor(partypes);
			Object arglist[] = new Object[3];
			arglist[0] = new String("");
			arglist[1] = new String("");
			arglist[2] = new Integer(0);
			parser = (PhoneBookContentParser) ct.newInstance(arglist);
		} catch (ClassNotFoundException e) {
			SWTHelper.alert("Implementierungsfehler", "Für das ausgewählte Land (" + country.toUpperCase() + ") ist die Suchroutine nicht implementiert.");
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Liest Kontaktinformationen anhand der Kriterien name & geo.
	 * Bei der Suche wird die Kontakteliste und der InfoText abgefüllt.
	 */
	private void readKontakte(final String name, final String geo, final int startPageNum) {
		final Cursor backupCursor = getShell().getCursor();
		final Cursor waitCursor = new Cursor(getShell().getDisplay(),
				SWT.CURSOR_WAIT);

		getShell().setCursor(waitCursor);

		try {
			createParser();
			kontakte = parser.extractKontakte();
			searchInfoText = parser.getSearchInfo();
			setSearchText(/*parser*/);
		} catch (SecurityException e) {
			e.printStackTrace();
		} finally {
			getShell().setCursor(backupCursor);
		}
	}
	
	/**
	 * Aktion wenn Such-Button klicked oder Default-Action (Return).
	 */
	private void searchAction(String name, String geo) {
		String savedText = geoText.getText();
		geoText.setRedraw(false);
		geoText.setItems(new String[] {""});
		geoText.setText(savedText);
		geoText.setRedraw(true);
		readKontakte(name, geo, startEntryIndex);
		resultChanged();
		if (parser.hasCitiesList())	{
			SWTHelper.showInfo("", parser.getCitiesListMessage());
			String[][] citiesList = parser.getCitiesList();
			fillCitiesCombo(citiesList);
		}
		if (parser.noCityFound())	{
			SWTHelper.showInfo("", parser.getCitiesListMessage());
			String[][] citiesList = parser.getCitySuggestions("Sing");
			fillCitiesCombo(citiesList);
			}
	}
	
	private void fillCitiesCombo(final String[][] citiesList)	{
		hitList.removeAll();
		if ((citiesList != null) && (citiesList.length > 0))	{
			for (int i = 0; i < citiesList.length; i++)	{
				String marker = (citiesList[i][1].equalsIgnoreCase("0") ? " *** " : "");
				hitList.add(marker + citiesList[i][0] + marker);
			}
		}
/*		if ((citiesList != null) && (citiesList.length > 0))	{
			String itemsString = "";
			String itemsDelim = "";
			for (int i = 0; i < citiesList.length; i++)	{
				String marker = (citiesList[i][1].equalsIgnoreCase("0") ? " *** " : "");
				itemsString = itemsString + itemsDelim + marker + citiesList[i][0] + marker;
				itemsDelim = ";";
			}
			String savedText = geoText.getText();
			geoText.setItems(itemsString.split(";"));
			geoText.setVisibleItemCount(itemsString.length());
			geoText.setFocus();
			geoText.setText(savedText);
		}
*/	}
	
	private void resultChanged() {
		for (Object listener : listeners.getListeners()) {
			if (listener != null) {
				((Listener) listener).handleEvent(null);
			}
		}
	}
	
	/**
	 * Öffnet Dialog zum Erfassen eines Patienten
	 */
	public void openPatientenDialog(HashMap<String, String> kontaktHash) {
		if (kontaktHash != null) {
			final PatientErfassenDialog dialog = new PatientErfassenDialog(getShell(), kontaktHash);
			dialog.open();
		}
	}
	
	/**
	 * Öffnet Dialog zum Erfassen eines Kontaktes
	 */
	public void openKontaktDialog(HashMap<String, String> kontaktHash) {
		if (kontaktHash != null) {
			final KontaktErfassenDialog dialog = new KontaktErfassenDialog(getShell(), kontaktHash);
			dialog.open();
		}
	}

	/**
	 * Kontakt Liste
	 */
	public List<HashMap<String, String>> getKontakte() {
		return this.kontakte;
	}

	/**
	 * Infotext zum Suchresultat: z.B. "123 Treffer"
	 */
	public String getSearchInfoText() {
		return this.searchInfoText;
	}

	public void addResultChangeListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeResultChangeListener(Listener listener) {
		listeners.add(listener);
	}

}
