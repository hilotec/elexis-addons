/*******************************************************************************
 * Copyright (c) 2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *    $Id: EditVaccinationDialog.java 88 2010-05-13 15:27:47Z gerry.weirich $
 *******************************************************************************/
package ch.elexis.impfplan.view;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.impfplan.model.VaccinationType;
import ch.elexis.selectors.DisplayPanel;
import ch.elexis.selectors.FieldDescriptor;
import ch.elexis.util.SWTHelper;

@SuppressWarnings("unchecked")
public class EditVaccinationDialog extends TitleAreaDialog{
	VaccinationType vt;

	FieldDescriptor<VaccinationType>[] fields=new FieldDescriptor[]{
		new FieldDescriptor<VaccinationType>("Name der Impfung",VaccinationType.NAME,FieldDescriptor.Typ.STRING,null),
		new FieldDescriptor<VaccinationType>("Impfstoff",VaccinationType.PRODUCT,null),
		new FieldDescriptor<VaccinationType>("Empfohlenes Alter (von-bis)",VaccinationType.RECOMMENDED_AGE,null),
		new FieldDescriptor<VaccinationType>("Empfohlener Abstand zur 2. Impfung",VaccinationType.DELAY1TO2,null),
		new FieldDescriptor<VaccinationType>("Empfohlener Abstand zur 3. Impfung",VaccinationType.DELAY2TO3,null),
		new FieldDescriptor<VaccinationType>("Empfohlener Abstand zur 4. Impfung",VaccinationType.DELAY3TO4,null),
		new FieldDescriptor<VaccinationType>("Empfohlener Abstand zwischen Rappels",VaccinationType.DELAY_REP,null),
		new FieldDescriptor<VaccinationType>("Bemerkungen",VaccinationType.REMARKS,null),
		
	};
	public EditVaccinationDialog(Shell shell, VaccinationType vacc){
		super(shell);
		vt=vacc;
		
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		DisplayPanel panel=new DisplayPanel(parent, fields, 2, 2, new IAction[0]);
		panel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		panel.setAutosave(true);
		panel.setObject(vt);
		return panel;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Impfung eintragen");
		getShell().setText("Impfung definieren");
		getShell().setSize(800,600);
		SWTHelper.center(getShell());
	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}

	
}
