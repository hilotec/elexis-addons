package ch.elexis.regiomed.estudio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import ch.ct.patientenerfassung.client.PatientenErfClient;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.Fall;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.Person;
import ch.elexis.data.Xid;
import ch.elexis.tarmedprefs.TarmedRequirements;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class RegiomedAction extends Action implements IAction {

	public static final String ROSENSTUDIO_ID="ch.elexis.adapter.estudio";
	static final String crlf=StringTool.crlf;
	private FileWriter fout;
	public RegiomedAction() {
		setImageDescriptor(getImageDescriptor("icons"+File.separator+"rose.gif"));
		setToolTipText("eStudio aufrufen und Daten übertragen");
	}
	
	
	@Override
	public void run() {
		Patient pat=ElexisEventDispatcher.getSelectedPatient();
		String KSK=Hub.actMandant.getXid(TarmedRequirements.DOMAIN_KSK);
		try{
			String inipath=Hub.localCfg.get(PreferencePage.INI_PATH,null);
			String propfile=Hub.localCfg.get(PreferencePage.PROP_PATH, null);
			if((inipath==null) || (propfile==null)){
				SWTHelper.alert("Fehler bei Konfiguration", "Es ist kein Pfad für das ini-File oder das property-file angegeben");
				return;
			}
			File inifile=new File(inipath);
			if(!inifile.canWrite()){
				SWTHelper.alert("Fehler bei Konfiguration", "Kann patient.ini nicht schreiben");
				return;
			}
			fout=new FileWriter(inifile);
			fout.write("[Patient]"+crlf);
			writeLn("ArztNr",TarmedRequirements.normalizeKSK(KSK, false));
			writeLn("PatientNr",pat.get("PatientNr"));
			String anr=pat.getGeschlecht().equals(Person.MALE) ? "Herr" : "Frau";
			writeLn("Anrede",anr);
			writeLn("Name",pat.getName());
			writeLn("Vorname",pat.getVorname());
			String geb=pat.getGeburtsdatum();
			TimeTool ttg=new TimeTool();
			if(ttg.set(geb)){
				writeLn("Geburt",ttg.toString(TimeTool.DATE_GER));
			}
			writeLn("Sex",(pat.getGeschlecht().equals(Person.MALE) ? "M" : "W"));
			writeLn("PLZ",pat.get("Plz"));
			writeLn("Ort",pat.get("Ort"));
			writeLn("Strasse",pat.get("Strasse"));
			writeLn("TelefonP",pat.get("Telefon1"));
			writeLn("TelefonG",pat.get("Telefon2"));
			Fall[] faelle=pat.getFaelle();
			for(Fall fall:faelle){
				if(fall.getAbrechnungsSystem().equals("KVG")){
					Kontakt kk=fall.getRequiredContact(TarmedRequirements.INSURANCE);
					String vnr=fall.getRequiredString(TarmedRequirements.INSURANCE_NUMBER);
					writeLn("KVMitgliedNr",vnr);
					if(kk!=null){
						String kkEAN=kk.getXid(Xid.DOMAIN_EAN);
						if(!StringTool.isNothing(kkEAN)){
							writeLn("KVEanNr",kkEAN);
						}
					}
					break;
				}
			}
			fout.close();
			PatientenErfClient pec=new PatientenErfClient();
			PatientenErfClient.patientenErfassungProp.load(new FileInputStream(propfile));
			PatientenErfClient.patientenErfassungProp.setProperty("patientenfile", inipath);
			PatientenErfClient.patientenErfassungProp.store(new FileOutputStream(propfile), null);
			pec.speichernPatient();
		}catch(Exception ex){
			ExHandler.handle(ex);
			SWTHelper.alert("Fehler beim Aufruf", "Konnte Rosenstudio nicht starten");
		}
	}
	
	private void writeLn(String parm, String val) throws Exception{
		if(!StringTool.isNothing(val)){
			fout.write(parm+"="+val+StringTool.crlf);
		}
	}
	
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(ROSENSTUDIO_ID, path);
	}
}
