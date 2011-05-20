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
 *    $Id: ContentProviderAdapter.java 88 2010-05-13 15:27:47Z gerry.weirich $
 *******************************************************************************/
package ch.elexis.impfplan.view;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public abstract class ContentProviderAdapter implements IStructuredContentProvider {
	
	@Override
	public abstract Object[] getElements(Object inputElement);
	
	@Override
	public void dispose(){}
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput){}
	
}
