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
 *    $Id: ImpfplanController.scala 88 2010-05-13 15:27:47Z gerry.weirich $
 *******************************************************************************/
package ch.elexis.impfplan.controller

object ImpfplanController {
	import ch.elexis.data._;
	
	import ch.elexis.impfplan.model._;
	import scala.collection.JavaConversions._;
	
	/**
		return a Collection of all Vaccinations this patient has
	*/
	def getVaccinations(pat: Patient) : Iterable[Vaccination] = 
	   new JCollectionWrapper(new Query[Vaccination](classOf[Vaccination],Vaccination.PATIENT_ID,pat.getId).execute);
		
	/**
		return a Collection of all Vaccinations that are defined in this system
	 */
	def allVaccs : Iterable[VaccinationType] =	JCollectionWrapper(new Query[VaccinationType](classOf[VaccinationType]).execute) filter (x => (!x.getId().equals("VERSION")));
		
	
	/**
		return a Collection of all Vaccinations that are due or overdue this moment for the given Patient
	*/
	def getVaccinationsDue(pat : Patient) : Iterable[VaccinationType]= {
		getVaccinations(pat).map(i => VaccinationType.load(i.get(Vaccination.VACCINATION_TYPE)));
	}
	
	def isIn(vaccinations: Iterable[Vaccination], vaccType : VaccinationType) : Boolean ={
		
			false;
	}
}
