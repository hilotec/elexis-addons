/*******************************************************************************
 * Copyright (c) 2008, G. Weirich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 * $Id: FakturaJournal.java 1056 2008-12-22 22:23:02Z  $
 *******************************************************************************/
package ch.elexis.archie.patientstatistik;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import ch.elexis.data.Patient;
import ch.unibe.iam.scg.archie.annotations.GetProperty;
import ch.unibe.iam.scg.archie.annotations.SetProperty;
import ch.unibe.iam.scg.archie.model.AbstractTimeSeries;
import ch.unibe.iam.scg.archie.ui.widgets.WidgetTypes;

public class PatientenJournal extends AbstractTimeSeries {
	String[] headings={"Name","Konsultationen","Kosten"};
	
	public PatientenJournal(){
		super("Patienten");
	}
	
	@Override
	protected List<String> createHeadings(){
		return Arrays.asList(headings);
	}
	
	@Override
	public String getDescription(){
		return "Kosten pro Patient";
	}
	@Override
	protected IStatus createContent(IProgressMonitor monitor){
		
		return null;
	}
	
}
