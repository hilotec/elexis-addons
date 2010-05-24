// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   PatientenErfClient.java

package ch.ct.patientenerfassung.client;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Provider;
import java.security.Security;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import ch.elexis.regiomed.estudio.RegiomedAction;
import ch.elexis.util.PlatformHelper;

// Referenced classes of package ch.ct.patientenerfassung.client:
//            PatientenErfServerServiceLocator, PatientenErfServer, ChCtPatientenerfassungPatient

public class PatientenErfClient
{

    public PatientenErfClient()
    {
        langResource = null;
    }

    public void speichernPatient()
        throws Exception
    {
        JDialog statusDialog = null;
        String cacerts=PlatformHelper.getBasePath(RegiomedAction.ROSENSTUDIO_ID)+File.separator+"cacerts";
        try
        {
            //patientenErfassungProp.load(new FileInputStream("patientenerfassung.properties"));
            log.debug("Sprache:".concat(String.valueOf(String.valueOf(patientenErfassungProp.getProperty("sprache", "de")))));
            Locale currentLocale = new Locale(patientenErfassungProp.getProperty("sprache", "de"), "CH");
            langResource = ResourceBundle.getBundle("LangResources", currentLocale);
            log.debug("Patientenfile:".concat(String.valueOf(String.valueOf(patientenErfassungProp.getProperty("patientenfile")))));
            log.debug("jndiName:".concat(String.valueOf(String.valueOf(patientenErfassungProp.getProperty("jndiName")))));
            System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            System.setProperty("javax.net.ssl.trustStore", cacerts);
            System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
            PatientenErfServerServiceLocator serviceLocator = new PatientenErfServerServiceLocator();
            log.debug("Webservice address:".concat(String.valueOf(String.valueOf(serviceLocator.getPatientenErfassungAddress()))));
            log.debug("WSDD Service name:".concat(String.valueOf(String.valueOf(serviceLocator.getPatientenErfassungWSDDServiceName()))));
            PatientenErfServer stub = serviceLocator.getPatientenErfassung();
            ChCtPatientenerfassungPatient patient = new ChCtPatientenerfassungPatient();
            statusDialog = new JDialog();
            GridBagLayout g = new GridBagLayout();
            statusDialog.getContentPane().setLayout(g);
            statusDialog.setTitle((String)langResource.getObject("Patientenerfassung"));
            JLabel jLabel = new JLabel((String)langResource.getObject("PatientendatenWerdenGelesen"));
            jLabel.setForeground(Color.darkGray);
            GridBagConstraints gbC = new GridBagConstraints();
            gbC.anchor = 17;
            gbC.gridy = -1;
            gbC.gridx = 0;
            g.setConstraints(jLabel, gbC);
            Properties prop = new Properties();
            prop.load(new FileInputStream(patientenErfassungProp.getProperty("patientenfile")));
            patient.setKonkordatsNr(prop.getProperty("ArztNr"));
            log.debug("Artznr:".concat(String.valueOf(String.valueOf(prop.getProperty("ArztNr")))));
            patient.setArztpatientennr(prop.getProperty("PatientNr"));
            log.debug("PatientNr:".concat(String.valueOf(String.valueOf(prop.getProperty("PatientNr")))));
            patient.setGebDat(prop.getProperty("Geburt"));
            log.debug("GeburtsDatum:".concat(String.valueOf(String.valueOf(prop.getProperty("Geburt")))));
            patient.setGeschlecht((short)1);
            if(prop.getProperty("Sex") != null && prop.getProperty("Sex").equals("M"))
                patient.setGeschlecht((short)1);
            if(prop.getProperty("Sex") != null && (prop.getProperty("Sex").equals("F") || prop.getProperty("Sex").equals("W")))
                patient.setGeschlecht((short)2);
            log.debug("Geschlecht:".concat(String.valueOf(String.valueOf(prop.getProperty("Sex")))));
            if(prop.getProperty("Anrede") != null && prop.getProperty("Anrede").equals("Herr"))
                patient.setAnrede((short)1);
            if(prop.getProperty("Anrede") != null && prop.getProperty("Anrede").equals("Frau"))
                patient.setAnrede((short)2);
            patient.setName(prop.getProperty("Name"));
            log.debug("Name:".concat(String.valueOf(String.valueOf(prop.getProperty("Name")))));
            patient.setVorname(prop.getProperty("Vorname"));
            log.debug("Vorname:".concat(String.valueOf(String.valueOf(prop.getProperty("Vorname")))));
            patient.setStrassenr(prop.getProperty("Strasse"));
            log.debug("Strasse:".concat(String.valueOf(String.valueOf(prop.getProperty("Strasse")))));
            patient.setPlz(prop.getProperty("PLZ"));
            log.debug("PLZ:".concat(String.valueOf(String.valueOf(prop.getProperty("PLZ")))));
            if(prop.getProperty("Ort") != null)
                patient.setOrt(prop.getProperty("Ort"));
            if(prop.getProperty("ORT") != null)
                patient.setOrt(prop.getProperty("ORT"));
            log.debug("Ort:".concat(String.valueOf(String.valueOf(prop.getProperty("Ort")))));
            log.debug("ORT:".concat(String.valueOf(String.valueOf(prop.getProperty("ORT")))));
            patient.setTelefonPrivat(prop.getProperty("TelefonP"));
            log.debug("TelefonP:".concat(String.valueOf(String.valueOf(prop.getProperty("TelefonP")))));
            patient.setTelefonGeschaeft(prop.getProperty("TelefonG"));
            log.debug("TelefonG:".concat(String.valueOf(String.valueOf(prop.getProperty("TelefonG")))));
            if(prop.getProperty("Telefax") != null)
                patient.setTelefax(prop.getProperty("Telefax"));
            log.debug("Telefax:".concat(String.valueOf(String.valueOf(prop.getProperty("Telefax")))));
            if(prop.getProperty("EMail") != null)
                patient.setEmail(prop.getProperty("EMail"));
            else
                patient.setEmail("");
            log.debug("EMail:".concat(String.valueOf(String.valueOf(prop.getProperty("EMail")))));
            if(prop.getProperty("Kanton") != null)
                patient.setKanton(prop.getProperty("Kanton"));
            log.debug("Kanton:".concat(String.valueOf(String.valueOf(prop.getProperty("Kanton")))));
            patient.setKVNummer(prop.getProperty("KVMitgliedNr"));
            log.debug("KVMitgliedNr:".concat(String.valueOf(String.valueOf(prop.getProperty("KVMitgliedNr")))));
            patient.setUVNummer(prop.getProperty("UVMitgliedNr"));
            log.debug("UVMitgliedNr:".concat(String.valueOf(String.valueOf(prop.getProperty("UVMitgliedNr")))));
            patient.setSprache("de");
            log.debug("Sprache:".concat(String.valueOf(String.valueOf(patient.getSprache()))));
            if(prop.getProperty("KVEanNr") != null)
                patient.setKrankenvers(prop.getProperty("KVEanNr"));
            else
                patient.setKrankenvers("1");
            log.debug("KVEanNr:".concat(String.valueOf(String.valueOf(patient.getKrankenvers()))));
            if(prop.getProperty("UVEanNr") != null)
                patient.setUnfallvers(prop.getProperty("UVEanNr"));
            else
                patient.setUnfallvers("2");
            log.debug("UVEanNr:".concat(String.valueOf(String.valueOf(patient.getUnfallvers()))));
            patient.setTitelfirmaheim("");
            patient.setLand("CH");
            log.debug("call speichernPatient()");
            JLabel jLabel1 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Arztnr"))))).append(": ").append(prop.getProperty("ArztNr")))));
            jLabel1.setForeground(Color.black);
            gbC.gridy = 1;
            g.setConstraints(jLabel1, gbC);
            JLabel jLabel2 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("PatientNr"))))).append(": ").append(prop.getProperty("PatientNr")))));
            jLabel2.setForeground(Color.black);
            gbC.gridy = 2;
            g.setConstraints(jLabel2, gbC);
            JLabel jLabel3 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Anrede"))))).append(": ").append(prop.getProperty("Anrede")))));
            jLabel3.setForeground(Color.black);
            gbC.gridy = 3;
            g.setConstraints(jLabel3, gbC);
            JLabel jLabel4 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Name"))))).append(": ").append(prop.getProperty("Name")))));
            jLabel4.setForeground(Color.black);
            gbC.gridy = 4;
            g.setConstraints(jLabel4, gbC);
            JLabel jLabel5 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Vorname"))))).append(": ").append(prop.getProperty("Vorname")))));
            jLabel5.setForeground(Color.black);
            gbC.gridy = 5;
            g.setConstraints(jLabel5, gbC);
            JLabel jLabel6 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Geburtsdatum"))))).append(": ").append(prop.getProperty("Geburt")))));
            jLabel6.setForeground(Color.black);
            gbC.gridy = 6;
            g.setConstraints(jLabel6, gbC);
            JLabel jLabel7 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Geschlecht"))))).append(": ").append(prop.getProperty("Sex")))));
            jLabel7.setForeground(Color.black);
            gbC.gridy = 7;
            g.setConstraints(jLabel7, gbC);
            JLabel jLabel8 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("PLZ"))))).append(": ").append(prop.getProperty("PLZ")))));
            jLabel8.setForeground(Color.black);
            gbC.gridy = 8;
            g.setConstraints(jLabel8, gbC);
            JLabel jLabel9 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Ort"))))).append(": ").append(prop.getProperty("Ort")))));
            jLabel9.setForeground(Color.black);
            gbC.gridy = 9;
            g.setConstraints(jLabel9, gbC);
            JLabel jLabel10 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Kanton"))))).append(": ").append(prop.getProperty("Kanton")))));
            jLabel10.setForeground(Color.black);
            gbC.gridy = 10;
            g.setConstraints(jLabel10, gbC);
            JLabel jLabel11 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("Strasse"))))).append(": ").append(prop.getProperty("Strasse")))));
            jLabel11.setForeground(Color.black);
            gbC.gridy = 11;
            g.setConstraints(jLabel11, gbC);
            JLabel jLabel12 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("TelefonP"))))).append(": ").append(prop.getProperty("TelefonP")))));
            jLabel12.setForeground(Color.black);
            gbC.gridy = 12;
            g.setConstraints(jLabel12, gbC);
            JLabel jLabel13 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("TelefonG"))))).append(": ").append(prop.getProperty("TelefonG")))));
            jLabel13.setForeground(Color.black);
            gbC.gridy = 13;
            g.setConstraints(jLabel13, gbC);
            JLabel jLabel14 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("KVMitgliedNr"))))).append(": ").append(prop.getProperty("KVMitgliedNr")))));
            jLabel14.setForeground(Color.black);
            gbC.gridy = 14;
            g.setConstraints(jLabel14, gbC);
            JLabel jLabel15 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("UVMitgliedNr"))))).append(": ").append(prop.getProperty("UVMitgliedNr")))));
            jLabel15.setForeground(Color.black);
            gbC.gridy = 15;
            g.setConstraints(jLabel15, gbC);
            JLabel jLabel16 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("KVEAN-Nr"))))).append(": ").append(prop.getProperty("KVEanNr")))));
            jLabel16.setForeground(Color.black);
            gbC.gridy = 16;
            g.setConstraints(jLabel16, gbC);
            JLabel jLabel17 = new JLabel(String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(langResource.getObject("UVEAN-Nr"))))).append(": ").append(prop.getProperty("UVEanNr")))));
            jLabel17.setForeground(Color.black);
            gbC.gridy = 17;
            g.setConstraints(jLabel17, gbC);
            statusDialog.getContentPane().add(jLabel);
            statusDialog.getContentPane().add(jLabel1);
            statusDialog.getContentPane().add(jLabel2);
            statusDialog.getContentPane().add(jLabel3);
            statusDialog.getContentPane().add(jLabel4);
            statusDialog.getContentPane().add(jLabel5);
            statusDialog.getContentPane().add(jLabel6);
            statusDialog.getContentPane().add(jLabel7);
            statusDialog.getContentPane().add(jLabel8);
            statusDialog.getContentPane().add(jLabel9);
            statusDialog.getContentPane().add(jLabel10);
            statusDialog.getContentPane().add(jLabel11);
            statusDialog.getContentPane().add(jLabel12);
            statusDialog.getContentPane().add(jLabel13);
            statusDialog.getContentPane().add(jLabel14);
            statusDialog.getContentPane().add(jLabel15);
            statusDialog.getContentPane().add(jLabel16);
            statusDialog.getContentPane().add(jLabel17);
            statusDialog.setModal(false);
            statusDialog.setSize(400, 400);
            statusDialog.setLocation(400, 400);
            statusDialog.setResizable(false);
            statusDialog.validate();
            statusDialog.setVisible(true);
            statusDialog.show();
            int result = stub.speichernPatient(patient, patientenErfassungProp.getProperty("jndiName"));
            log.debug("Patientennummer :".concat(String.valueOf(String.valueOf(result))));
            String text = null;
            if(result == -2)
                text = String.valueOf(langResource.getObject("FehlerDerArztIstNichtVorhanden")) + String.valueOf(patient.getKonkordatsNr());
            else
                text = (String)langResource.getObject("PatientendatenWurdenErfolgreichUebermittelt");
            JOptionPane.showMessageDialog(null, text, (String)langResource.getObject("Resultat"), 1);
            statusDialog.dispose();
            try
            {
                Runtime.getRuntime().exec(patientenErfassungProp.getProperty("browser"));
            }
            catch(Exception e)
            {
                log.error(e.toString(), e);
            }
            
        }
        catch(Exception e)
        {
            log.error(e.toString(), e);
            JOptionPane.showMessageDialog(null, e.toString(), (String)langResource.getObject("Fehler"), 0);
            statusDialog.dispose();
            System.exit(0);
        }
        catch(Throwable t)
        {
            log.error(t.toString(), t);
            JOptionPane.showMessageDialog(null, t.toString(), (String)langResource.getObject("Fehler"), 0);
            statusDialog.dispose();
            System.exit(0);
        }
    }

    public static void main(String args[])
        throws Exception
    {
        if(args.length > 0)
        {
            log.debug("Patienten-ini file:".concat(String.valueOf(String.valueOf(args[0]))));
            patientenErfassungProp.load(new FileInputStream("patientenerfassung.properties"));
            patientenErfassungProp.setProperty("patientenfile", args[0]);
            patientenErfassungProp.store(new FileOutputStream("patientenerfassung.properties"), null);
        }
        PatientenErfClient patErfClient = new PatientenErfClient();
        patErfClient.speichernPatient();
    }

    public String convertGebDate(String gebDat)
    {
        if(gebDat.equals(""))
            return gebDat;
        else
            return String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(gebDat.substring(6))))).append(".").append(gebDat.substring(4, 6)).append(".").append(gebDat.substring(0, 4))));
    }

    public short convertGeschlecht(String geschlecht)
    {
        return (short)(geschlecht.equals("M") ? 1 : 2);
    }

    static Class _mthclass$(String x$0)
    {
        try
        {
            return Class.forName(x$0);
        }
        catch(ClassNotFoundException x$1)
        {
            throw new NoClassDefFoundError(x$1.getMessage());
        }
    }

    private static final Logger log;
    public static Properties patientenErfassungProp = new Properties();
    static final String erfolgreich = "Patientendaten wurden erfolgreich \374bermittelt.";
    static final String arztFehlt = "FEHLER: Der Arzt ist nicht vorhanden, Konkordatsnummer: ";
    private ResourceBundle langResource;

    static 
    {
        log = Logger.getLogger(ch.ct.patientenerfassung.client.PatientenErfClient.class);
    }
}
