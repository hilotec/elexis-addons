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
 * $Id$
 *******************************************************************************/

package ch.elexis.stickynotes.ui;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.rgw.io.Settings;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	final static String PREFBRANCH = "sticky_notes/"; //$NON-NLS-1$
	final static String COLBACKGROUND = PREFBRANCH + "col_background"; //$NON-NLS-1$
	final static String COLFOREGROUND = PREFBRANCH + "col_foreground"; //$NON-NLS-1$
	
	public Preferences(){
		super(GRID);
		Settings cfg = Hub.userCfg;
		setPreferenceStore(new SettingsPreferenceStore(cfg));
	}
	
	public void init(IWorkbench workbench){
	// TODO Auto-generated method stub
	
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new ColorFieldEditor(COLBACKGROUND, Messages.Preferences_BackgroundColor,
			getFieldEditorParent()));
		addField(new ColorFieldEditor(COLFOREGROUND, Messages.Preferences_ForegroundColor,
			getFieldEditorParent()));
	}
	
}
