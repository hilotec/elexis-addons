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

package ch.medshare.elexis.directories.views_marlovits;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import ch.elexis.dialogs.KontaktErfassenDialog;
import ch.elexis.dialogs.PatientErfassenDialog;
import ch.elexis.util.SWTHelper;
import ch.medshare.elexis.directories_marlovits.DirectoriesContentParser;
import ch.medshare.elexis.directories_marlovits.DirectoriesHelper;
import ch.medshare.elexis.directories_marlovits.KontaktEntry;
import ch.rgw.tools.ExHandler;

public class WeisseSeitenSearchForm extends Composite {

	private final ListenerList listeners = new ListenerList();

	private List<KontaktEntry> kontakte = new Vector<KontaktEntry>();

	private String searchInfoText = "";
	private Composite infoComposite;
	private Text searchInfoTextField;
	Button previousBtn;
	Button nextBtn;
	private Text nameText;
	private Combo geoText;
	
	private int maxEntriesToRead = 10;
	private int startEntryIndex = 0;
	private int numOfPages = 0;
	private int numOfEntries = 1;
	private String country = "ch";

	public WeisseSeitenSearchForm(Composite parent, int style) {
		super(parent, style);
		createPartControl(parent);
	}

	private void createPartControl(Composite parent) {
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
		geoText.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
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
		
		searchBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				startEntryIndex = 0;
				searchAction(nameText.getText(), geoText.getText());
			}
		});
	}
	
	private void setSearchText(DirectoriesContentParser parser)	{
		boolean visible = true;
		if (parser != null)	{
			int numOfEntries = parser.getNumOfEntries();
			searchInfoText = parser.getSearchInfo();
			searchInfoTextField.setText(searchInfoText);
			infoComposite.pack(true);
			visible = true;
			numOfPages = (numOfEntries - 1) / 10;
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
			String content = DirectoriesHelper.readContent(name, geo, country, startPageNum);
			if ((content == null) || (content.equalsIgnoreCase("")))	{
				SWTHelper.alert("Shit!", "content null or empty");
			}
			DirectoriesContentParser parser = new DirectoriesContentParser(content, name, geo, country);
			kontakte = parser.extractKontakte();
			searchInfoText = parser.getSearchInfo();
			setSearchText(parser);
			if (parser.hasCitiesList())	{
				SWTHelper.showInfo("", parser.getCitiesHitListMessage());
				String[][] citiesList = parser.getCitiesHitList();
				if ((citiesList != null) && (citiesList.length > 0))	{
					String tmp = "";
					String itemsString = "";
					String delim = "";
					String itemsDelim = "";
					for (int i = 0; i < citiesList.length; i++)	{
						String city   = citiesList[i][0];
						//String city   = citiesList[i][0];
						String marker = (citiesList[i][1].equalsIgnoreCase("0") ? " *** " : "");
						itemsString = itemsString + itemsDelim + marker + citiesList[i][0] + marker;
						itemsDelim = ";";
						//for (int i2 = 0; i2 < 2; i2++){
						//	tmp = tmp + delim + citiesList[i][i2];
						//	delim = ";";
						//}
					}
					//System.out.println(tmp);
					String savedText = geoText.getText();
					geoText.setItems(itemsString.split(";"));
					geoText.setVisibleItemCount(itemsString.length());
					geoText.setFocus();
					geoText.setText(savedText);
				}
			}
		} catch (IOException e) {
			ExHandler.handle(e);
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
		readKontakte(name, geo,  startEntryIndex);
		resultChanged();
	}

	private void resultChanged() {
		for (Object listener : listeners.getListeners()) {
			if (listener != null) {
				((Listener) listener).handleEvent(null);
			}
		}
	}

	/**
	 * Retourniert String array für Dialoge
	 *  //+++++ versucht Detailinfos zu lesen
	 */
	private String[] getFields(KontaktEntry entry) {
		
		//if (country.equalsIgnoreCase("ch"))	{
			entry = DirectoriesContentParser.parseVCard(entry.getDetailLink(), country);
		//}
		///////////KontaktEntry k = getKontakte().get(1);
		
		if (1==1) {
		if (!entry.isDetail()) { // Sind Detailinformationen vorhanden
			//++++ wenn keine Detailinfo da -> nach vollem Namen suchen -> ergibt meist Detail-Info
			final String name = entry.getName() + " " //$NON-NLS-1$
				+ entry.getVorname();
			final String geo = entry.getPlz() + " " //$NON-NLS-1$
				+ entry.getOrt();
			readKontakte(name, geo, startEntryIndex); // Detail infos lesen (meist...)
			KontaktEntry detailEntry = null;
			if (getKontakte().size() == 1) {
				// nur ein Eintrag gefunden -> benutzen
				detailEntry = getKontakte().get(0);
			} else if (getKontakte().size() > 1) {
				// falls mehr als ein Eintrag gefunden -> Match auf Strasse versuchen, der erste Eintrag gewinnt...
				// kann falsch sein
				String strasse = entry.getAdresse().trim();
				for (KontaktEntry tempEntry: getKontakte()) {
					if (strasse.contains(tempEntry.getAdresse())) {
						detailEntry = tempEntry;
					}
				}
			}
			if (detailEntry != null) {
				// Falls bei Detailsuche Fehler passiert, dann sind weniger Infos vorhanden
				if (detailEntry.countNotEmptyFields() > entry.countNotEmptyFields()) {
					entry = detailEntry;
				}
			}
		}
		}
		return new String[] { entry.getName(), entry.getVorname(),
				"", entry.getAdresse(), entry.getPlz(), //$NON-NLS-1$
				entry.getOrt(), entry.getTelefon(), entry.getZusatz(),
				entry.getFax(), entry.getEmail(),
				//+++++ new:
				entry.getWebsite(), entry.getTelefon2(), entry.getMobile(), entry.getLedigname(),
				entry.getProfession(), entry.getCategory(),
				(entry.getIsOrganisation() ? "1" : "0"),
				entry.getTitle(), entry.getCountry()
		};
	
	}

	/**
	 * Öffnet Dialog zum Erfassen eines Patienten
	 */
	public void openPatientenDialog(KontaktEntry entry) {
		if (entry != null) {
			final PatientErfassenDialog dialog = new PatientErfassenDialog(
					getShell(), entry.toHashmap());
			dialog.open();
		}
	}

	/**
	 * Öffnet Dialog zum Erfassen eines Kontaktes
	 */
	public void openKontaktDialog(KontaktEntry entry) {
		if (entry != null) {
			final KontaktErfassenDialog dialog = new KontaktErfassenDialog(
					getShell(), getFields(entry));
			dialog.open();
		}
	}

	/**
	 * Kontakt Liste
	 */
	public List<KontaktEntry> getKontakte() {
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
