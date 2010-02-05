/*******************************************************************************
 * Copyright (c) 2009-2010, G. Weirich and Elexis
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListenerImpl;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.actions.Heartbeat.HeartListener;
import ch.elexis.data.Anwender;
import ch.elexis.data.Patient;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.stickynotes.data.StickyNote;
import ch.elexis.text.EnhancedTextField;
import ch.elexis.util.SWTHelper;

public class StickyNotesView extends ViewPart implements IActivationListener, HeartListener {
	private ScrolledForm form;
	EnhancedTextField etf;
	Patient actPatient;
	StickyNote actNote;
	SettingsPreferenceStore prefs;
	
	private final ElexisEventListenerImpl eeli_pat = new ElexisEventListenerImpl(Patient.class) {
		@Override
		public void runInUi(ElexisEvent ev){
			if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
				doSelect((Patient) ev.getObject());
			} else if (ev.getType() == ElexisEvent.EVENT_DESELECTED) {
				deselect();
			}
		}
	};
	
	private final ElexisEventListenerImpl eeli_user =
		new ElexisEventListenerImpl(Anwender.class, ElexisEvent.EVENT_USER_CHANGED) {
		
		@Override
		public void catchElexisEvent(ElexisEvent ev){
			prefs = new SettingsPreferenceStore(Hub.userCfg);
		}
		
	};
	
	@Override
	public void createPartControl(Composite parent){
		prefs = new SettingsPreferenceStore(Hub.userCfg);
		form = Desk.getToolkit().createScrolledForm(parent);
		form.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		etf = new EnhancedTextField(body);
		etf.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		GlobalEventDispatcher.addActivationListener(this, this);
		
	}
	
	@Override
	public void dispose(){
		GlobalEventDispatcher.removeActivationListener(this, this);
		super.dispose();
	}
	
	@Override
	public void setFocus(){
		etf.setFocus();
	}
	
	public void activation(boolean mode){
		if ((mode == false) && etf.isDirty()) {
			if (actPatient != null) {
				if (actNote == null) {
					actNote = StickyNote.load(actPatient);
				}
				actNote.setText(etf.getDocumentAsText());
			}
		}
		
	}
	
	public void visible(boolean mode){
		if (mode) {
			eeli_pat.catchElexisEvent(ElexisEvent.createPatientEvent());
			eeli_user.catchElexisEvent(ElexisEvent.createUserEvent());
			ElexisEventDispatcher.getInstance().addListeners(eeli_pat, eeli_user);
			Hub.heart.addListener(this);
		} else {
			ElexisEventDispatcher.getInstance().removeListeners(eeli_pat, eeli_user);
			Hub.heart.removeListener(this);
		}
		
	}
	
	private void deselect(){
		actNote = null;
		actPatient = null;
		etf.setText(""); //$NON-NLS-1$
		// form.setText(Messages.StickyNotesView_NoPatientSelected);
		setPartName(Messages.StickyNotesView_StickyNotesName);
	}
	
	private void doSelect(Patient pat){
		if (pat == null) {
			deselect();
			
		} else {
			
			actPatient = pat;
			actNote = StickyNote.load(actPatient);
			etf.setText(actNote.getText());
			// form.setText(actPatient.getLabel());
			setPartName(Messages.StickyNotesView_StickyNotesNameDash + actPatient.getLabel());
			RGB rgb = PreferenceConverter.getColor(prefs, Preferences.COLBACKGROUND);
			Desk.getColorRegistry().put(Preferences.COLBACKGROUND, rgb);
			Color back = Desk.getColorRegistry().get(Preferences.COLBACKGROUND);
			rgb = PreferenceConverter.getColor(prefs, Preferences.COLFOREGROUND);
			Desk.getColorRegistry().put(Preferences.COLFOREGROUND, rgb);
			Color fore = Desk.getColorRegistry().get(Preferences.COLFOREGROUND);
			etf.getControl().setBackground(back);
			etf.getControl().setForeground(fore);
		}
	}
	
	public void heartbeat(){
		if (actPatient == null) {
			actPatient = ElexisEventDispatcher.getSelectedPatient();
		}
		if (actPatient != null) {
			if (actNote == null) {
				actNote = StickyNote.load(actPatient);
			}
			if (actNote != null) {
				// TODO handle conflicts
				
			}
		}
	}
	
}
