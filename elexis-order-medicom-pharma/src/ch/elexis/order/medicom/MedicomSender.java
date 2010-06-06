package ch.elexis.order.medicom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.mail.Message;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.jdom.Element;

import ch.elexis.Hub;
import ch.elexis.data.Bestellung;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Organisation;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Bestellung.Item;
import ch.elexis.dialogs.KontaktSelektor;
import ch.elexis.exchange.IDataSender;
import ch.elexis.exchange.XChangeException;
import ch.elexis.exchange.elements.XChangeElement;
import ch.elexis.mail.Mailer;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;


public class MedicomSender implements IDataSender {

	private Bestellung best;
	private IGMOrder order;
	
	@Override
	public boolean canHandle(Class<? extends PersistentObject> clazz) {
		if (clazz.equals(Bestellung.class)) {
			return true;
		}
		return false;
	}

	@Override
	public void finalizeExport() throws XChangeException {
		Mailer mailer=new Mailer();
		String sender=Hub.actMandant.getMailAddress();
		if(!StringTool.isMailAddress(sender)){
			throw new XChangeException("Mandator has no valid e-mail address");
		}
		String receiver=Hub.globalCfg.get(Preferences.MAIL,null);
		if(!StringTool.isMailAddress(receiver)){
			throw new XChangeException("Configuration error: No destination mail address");
		}
		Message msg=mailer.createMultipartMessage("Bestellung", sender);
		String orderText=order.createFile();
		try {
			File out=File.createTempFile("mail", ".eml");
			FileWriter fwout=new FileWriter(out);
			fwout.write(orderText);
			fwout.close();
			Program.launch(out.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mailer.addTextPart(msg, orderText);
		Result<String> result=mailer.send(msg, receiver);
		if(!result.isOK()){
			throw new XChangeException("Error sending mail "+result.toString());
		}

	}

	@Override
	public XChangeElement store(Object output) throws XChangeException {
		if (output instanceof Bestellung) {
			boolean changeLieferant = false;
			boolean askedChangeLieferant = false;
			Kontakt defaultLieferant = null;
			Kontakt hauptlieferant = null;
			best = (Bestellung) output;
			List<Bestellung.Item> items = best.asList();
			Iterator<Item> iter = items.iterator();
			String identity=Hub.globalCfg.get(Preferences.CUSTOMER, null);
			if(identity==null){
				SWTHelper.showError("Konfiguration nicht korrekt", "Bitte tragen Sie in Einstellungen Ihre Kundennummer ein");
				throw new XChangeException("Configuration error: No userID");
			}
			order=new IGMOrder(identity);
			while (iter.hasNext()) {
				Kontakt adressat = null;
				Item it = (Item) iter.next();
				adressat = it.art.getLieferant();
				if (hauptlieferant == null) {
					if (adressat.exists()) {
						hauptlieferant = adressat;
					}
				}
				if (!adressat.exists()) {
					if (defaultLieferant == null) {
						KontaktSelektor ksl = new KontaktSelektor(
								Hub.getActiveShell(),
								Organisation.class,
								Messages
										.getString("Medicom.UnknownDistributor.Situation"),
								Messages
										.getString("Medicom.UnknownDistributor.Todo"));
						ksl.create();
						ksl
								.getShell()
								.setText(
										Messages
												.getString("Medicom.UnknownDistributor.Title"));
						if (ksl.open() == org.eclipse.jface.dialogs.Dialog.OK) {
							defaultLieferant = (Organisation) ksl
									.getSelection();
						}
					}
					if (defaultLieferant != null) {
						adressat = defaultLieferant;
						it.art.setLieferant(defaultLieferant);
					}
				}
				if (hauptlieferant != null) {
					if (!askedChangeLieferant) {
						if (!it.art.getLieferant().getId().equals(
								hauptlieferant.getId())) {
							askedChangeLieferant = true;
							if (MessageDialog
									.openConfirm(
											Hub.plugin.getWorkbench()
													.getActiveWorkbenchWindow()
													.getShell(),
											Messages
													.getString("Medicom.MultipleDistributors.Title"),
											Messages
													.getString("Medicom.MultipleDistributors.Text")
													+ "\n"
													+ "\n"
													+ hauptlieferant
															.getLabel(true))) {
								changeLieferant = true;
							}
						}
					}
					if (changeLieferant) {
						it.art.setLieferant(hauptlieferant);
						defaultLieferant = hauptlieferant;
					}
				}

				if (it.art.getLieferant().getId().equals(adressat.getId())) {
					order.addLine(it.art,it.num);
				}
			}
			return null;
		} else {
			throw new XChangeException("inacceptable data type "
					+ output.getClass().getName());
		}
	}

}
