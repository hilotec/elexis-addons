/*******************************************************************************
 * Copyright (c) 2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    J, Kurath - Sponsoring
 *    
 * $Id: StickyNotesView.java 5518 2009-07-04 14:02:31Z rgw_ch $
 *******************************************************************************/

package ch.elexis.stickynotes.ui;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.internal.preferences.PreferenceStoreAdapter;
import org.eclipse.ui.internal.preferences.PreferencesAdapter;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.actions.GlobalEvents.SelectionListener;
import ch.elexis.actions.GlobalEvents.UserListener;
import ch.elexis.actions.Heartbeat.HeartListener;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.stickynotes.data.StickyNote;
import ch.elexis.text.EnhancedTextField;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.StringTool;

public class StickyNotesView extends ViewPart implements ActivationListener, SelectionListener, HeartListener, UserListener {
	private ScrolledForm form;
	EnhancedTextField etf;
	Patient actPatient;
	StickyNote actNote;
	SettingsPreferenceStore prefs;
	
	@Override
	public void createPartControl(Composite parent) {
		prefs=new SettingsPreferenceStore(Hub.userCfg);
		form=Desk.getToolkit().createScrolledForm(parent);
		form.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		Composite body=form.getBody();
		body.setLayout(new GridLayout());
		etf=new EnhancedTextField(body);
		etf.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		GlobalEvents.getInstance().addActivationListener(this, this);
		
	}

	@Override
	public void dispose() {
		GlobalEvents.getInstance().removeActivationListener(this, this);
		super.dispose();
	}

	@Override
	public void setFocus() {
		etf.setFocus();
	}

	public void activation(boolean mode) {
		if((mode==false)&& etf.isDirty()){
			if(actPatient!=null){
				if(actNote==null){
					actNote=StickyNote.load(actPatient);
				}
				actNote.setText(etf.getDocumentAsText());
			}
		}
		
	}

	public void visible(boolean mode) {
		if(mode){
			selectionEvent(GlobalEvents.getSelectedPatient());
			UserChanged();
			GlobalEvents.getInstance().addUserListener(this);
			GlobalEvents.getInstance().addSelectionListener(this);
			Hub.heart.addListener(this);
		}else{
			GlobalEvents.getInstance().removeUserListener(this);
			GlobalEvents.getInstance().removeSelectionListener(this);
			Hub.heart.removeListener(this);
		}
		
	}

	public void clearEvent(Class<? extends PersistentObject> template) {
		if(template.equals(Patient.class)){
			actNote=null;
			actPatient=null;
			etf.setText(""); //$NON-NLS-1$
			//form.setText(Messages.StickyNotesView_NoPatientSelected);
			setPartName("Haftnotizen");
		}
		
	}

	public void selectionEvent(PersistentObject obj) {
		if(obj instanceof Patient) {
			actPatient=(Patient)obj;
			actNote=StickyNote.load(actPatient);
			etf.setText(actNote.getText());
			//form.setText(actPatient.getLabel());
			setPartName("Haftnotizen - "+actPatient.getLabel());
			RGB rgb=PreferenceConverter.getColor(prefs, Preferences.COLBACKGROUND);
			Desk.getColorRegistry().put(Preferences.COLBACKGROUND, rgb);
			Color back=Desk.getColorRegistry().get(Preferences.COLBACKGROUND);
			rgb=PreferenceConverter.getColor(prefs, Preferences.COLFOREGROUND);
			Desk.getColorRegistry().put(Preferences.COLFOREGROUND, rgb);
			Color fore=Desk.getColorRegistry().get(Preferences.COLFOREGROUND);
			etf.getControl().setBackground(back);
			etf.getControl().setForeground(fore);
		}
		
	}

	
	public void heartbeat() {
		if(actPatient==null){
			actPatient=GlobalEvents.getSelectedPatient();
		}
		if(actPatient!=null){
			if(actNote==null){
				actNote=StickyNote.load(actPatient);
			}
			if(actNote!=null){
				// TODO handle conflicts
				
			}
		}
	}

	public void UserChanged() {
		prefs=new SettingsPreferenceStore(Hub.userCfg);
		
	}
	
}
