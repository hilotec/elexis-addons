/*******************************************************************************
 * Copyright (c) 2010, Oliver Egger, visionary ag
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *    
 *******************************************************************************/
package ch.docbox.elexis;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;

import ch.docbox.cdach.CdaChXPath;
import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.data.Anwender;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.Log;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.VersionInfo;

public class CdaMessage extends PersistentObject {
	public static final String TABLENAME = "CH_DOCBOX_ELEXIS_CDAMESSAGE";
	public static final String DBVERSION = "1.0.0";
	public static final String createDB = "CREATE TABLE " + TABLENAME + " ("
			+ "ID			VARCHAR(25) primary key," + "CreationDate VARCHAR(15),"
			+ "Deleted      CHAR(1) default '0',"
			+ "DeletedDocs  CHAR(1) default '0'," + "lastupdate   BIGINT,"
			+ "PatID		VARCHAR(25)," + "DocumentID	VARCHAR(25),"
			+ "AnwenderID	VARCHAR(25)," + "KonsultationID	VARCHAR(25),"
			+ "Downloaded   CHAR(1) default '0'," + "Date 		CHAR(24),"
			+ "Unread       CHAR(1) default '1'," + "Title 		VARCHAR(80),"
			+ "Sender 		VARCHAR(80)," + "Patient 		VARCHAR(80),"
			+ "FilesListing	VARCHAR(2048)," + "Cda			BLOB);"
			+ "CREATE INDEX CH_DOCBOX_ELEXIS_CDAMESSAGEI1 ON " + TABLENAME
			+ " (PatID);" + "CREATE INDEX CH_DOCBOX_ELEXIS_CDAMESSAGEI2 ON "
			+ TABLENAME + " (DocumentID);"
			+ "CREATE INDEX CH_DOCBOX_ELEXIS_CDAMESSAGEI3 ON " + TABLENAME
			+ " (AnwenderID);"
			+ "CREATE INDEX CH_DOCBOX_ELEXIS_CDAMESSAGEI4 ON " + TABLENAME
			+ " (ID);" + "INSERT INTO " + TABLENAME
			+ " (ID, TITLE) VALUES ('1','" + DBVERSION + "');";
	private static final JdbcLink j = getConnection();

	static {
		addMapping(TABLENAME, "CreationDate", "DeletedDocs", "PatID",
				"DocumentID", "AnwenderID", "KonsultationID", "Downloaded",
				"Unread", "Date", "Title", "Sender", "Patient", "FilesListing",
				"Cda");
		CdaMessage start = load("1");
		if (start == null) {
			init();
		} else {
			VersionInfo vi = new VersionInfo(start.get("Title"));
			if (vi.isOlder(DBVERSION)) {
				if (vi.isOlder("1.1.0")) {
					// future update script
				} else {
					MessageDialog
							.openError(
									Desk.getTopShell(),
									"Versionskonsflikt",
									"Die Datentabelle für "
											+ TABLENAME
											+ " hat eine zu alte Versionsnummer. Dies kann zu Fehlern führen");
				}

			}
		}
	}

	public static CdaMessage load(String id) {
		CdaMessage ret = new CdaMessage(id);
		if (ret.exists()) {
			return ret;
		}
		return null;
	}

	public static CdaMessage getCdaMessageEvenIfDocsDeleted(String documentId) {
		return getCdaMessage(Hub.actMandant, documentId, true);
	}

	public static CdaMessage getCdaMessage(String documentId) {
		return getCdaMessage(Hub.actMandant, documentId, false);
	}

	public static CdaMessage getCdaMessage(Anwender anwender,
			String documentId, boolean alsoDeletedDoc) {
		Query<CdaMessage> cdaMessageQuery = new Query<CdaMessage>(
				CdaMessage.class);
		cdaMessageQuery.add("AnwenderID", "=", anwender.getId());
		cdaMessageQuery.add("DocumentID", "=", documentId);
		if (!alsoDeletedDoc) {
			cdaMessageQuery.add("DeletedDocs", "=", "0");
		}
		Object[] cdaMessages = cdaMessageQuery.execute().toArray();
		if (cdaMessages == null || cdaMessages.length == 0) {
			return null;
		}
		if (cdaMessages.length > 1) {
			log.log("CdaMessage Query should give only one object back but got multiple with AnwenderID=  "
					+ anwender.getId() + ", documentID " + documentId,
					Log.ERRORS);
		}
		return (CdaMessage) cdaMessages[0];
	}

	public static Object[] getCdaMessages() {
		return getCdaMessages(Hub.actMandant, false);
	}

	public static Object[] getCdaMessages(Anwender anwender,  boolean alsoDeletedDoc) {
		if (anwender != null) {
			log.log("getCdaMessages for "+anwender.getId(),Log.DEBUGMSG);
			Query<CdaMessage> cdaMessageQuery = new Query<CdaMessage>(
					CdaMessage.class);
			cdaMessageQuery.add("AnwenderID", "=", anwender.getId());
			if (!alsoDeletedDoc) {
				cdaMessageQuery.add("DeletedDocs", "=", "0");
			}
			cdaMessageQuery.orderBy(true, "CreationDate");
			Object[] objects =  cdaMessageQuery.execute().toArray();
			log.log("returned cdaMessages"+objects.length,Log.DEBUGMSG);
			return objects;
		}
		return new CdaMessage[0];
	}

	public CdaMessage(String documentId, String title, GregorianCalendar date) {
		create(null);
		TimeTool timeTool = new TimeTool();
		timeTool.set(date);
		set(new String[] { "AnwenderID", "DocumentID", "Title", "Date",
				"CreationDate", "Unread" }, Hub.actMandant.getId(), documentId,
				title, timeTool.toString(TimeTool.DATE_GER),
				timeTool.toString(TimeTool.TIMESTAMP), "1");
	}

	public boolean setDownloaded(String sender, String patient) {
		return set(new String[] { "Sender", "Patient", "Downloaded" }, sender,
				patient, "1");
	}

	public boolean setCda(String cda) {
		if (cda != null) {
			try {
				setBinary("Cda", cda.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				return false;
			}
		}
		return true;
	}

	public String getCda() {
		try {
			return new String(getBinary("Cda"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}catch (NullPointerException e) {
		}
		return null;
	}

	public static void init() {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(
					createDB.getBytes("UTF-8"));
			j.execScript(bais, true, false);
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
	}

	@Override
	public String getLabel() {
		StringBuilder sb = new StringBuilder();
		sb.append(get("Date")).append(" ").append(get("Title"));
		return sb.toString();
	}

	static public boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	public boolean deleteDocs() {
		if (this.getFiles() != null && this.getFiles().length > 0) {
			try {
				deleteDirectory(new File(this.getPath()));
			} catch (Exception e) {
			}
		}
		setDeletedDocs();
		return true;
	}

	/**
	 * currently all files are opened, if multiple we have not yet a selection
	 * possiblity
	 * 
	 * @return
	 */
	public boolean execute() {
		String files[] = this.getFiles();
		if (files != null && files.length > 0) {
			for (String file : files) {
				int pos = file.lastIndexOf(".");
				String ext = "";
				if (pos>0) {
					ext = file.substring(pos);
				}
				if (ext!=null) {
					ext = ext.trim();
				}
				String path = getPath(file);
				if (path!=null) {
					path = path.trim();
				}
				try {
					Program program = Program.findProgram(ext);
					if (program != null) {
						program.execute(path);
					} else {
						if (Program.launch(path) == false) {
							Runtime.getRuntime().exec(path);
						}
					}
				} catch (Exception ex) {
					ExHandler.handle(ex);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	protected String getTableName() {
		return TABLENAME;
	}

	protected CdaMessage(String id) {
		super(id);
	}

	protected CdaMessage() {
	}

	public boolean isDownloaded() {
		return getInt("Downloaded") != 0;
	}

	public boolean isDeletedDocs() {
		return getInt("DeletedDocs") != 0;
	}

	public void setDeletedDocs() {
		set(new String[] { "DeletedDocs" }, "1");
	}

	public void setRead() {
		set(new String[] { "Unread" }, "0");
	}

	public boolean isUnread() {
		return getInt("Unread") != 0;
	}

	public String getDate() {
		return get("Date");
	}

	public String getCreationDate() {
		return get("CreationDate");
	}

	public String getTitle() {
		return get("Title");
	}

	public String getSender() {
		return get("Sender");
	}

	public String getPatient() {
		return get("Patient");
	}

	public String getFilesListing() {
		return get("FilesListing");
	}

	public String[] getFiles() {
		return getFilesListing().split("\n");
	}

	public boolean hasAssignedToOmnivore() {
		return "ok".equals(getKonsultationId());
	}

	private String getKonsultationId() {
		return get("KonsultationID");
	}

	public void setAssignedToOmnivore() {
		set(new String[] { "KonsultationID" }, "ok");
	}

	/**
	 * returns the path where we will store the attachmetns
	 */
	public String getPath(String fileName) {
		String path = UserDocboxPreferences.getPathFiles();
		String pathSeparator = System.getProperty("file.separator");
		if (!path.endsWith(pathSeparator)) {
			path = path + pathSeparator;
		}
		path = path + getId();
		if (fileName != null) {
			path += pathSeparator + fileName;
		}
		return path;
	}

	private String getPath() {
		return getPath(null);
	}

	/**
	 * unzips the attachments to the specified directory in a subdirectory and
	 * sets the files extracted in the field fileslistings
	 * 
	 * @param attachment
	 *            byte array of a zip file
	 * @return true if successful false otherwise
	 */
	public boolean unzipAttachment(byte[] attachment) {
		ArrayList<String> fileList = new ArrayList<String>();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(attachment);
		ZipInputStream zipInputStream = new ZipInputStream(inputStream);
		ZipEntry zipEntry = null;
		String path = this.getPath();
		try {
			File directory = new File(path);
			if (!directory.exists()) {
				directory.mkdir();
			}
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				if (!zipEntry.isDirectory()) {
					String fileName = zipEntry.getName();
					fileList.add(fileName);
					if (fileName.contains("/")) {
						fileName = fileName.replaceAll("/", "");
					}
					if (fileName.contains("\\")) {
						fileName = fileName.replaceAll("\\\\", "");
					}
					log.log("exporting file out of attachment to " + path + ","
							+ fileName, Log.DEBUGMSG);
					File file = new File(directory, fileName);
					if (!file.exists()) {
						file.createNewFile();
					}
					FileOutputStream fileOutputStream = new FileOutputStream(
							file);
					byte[] bytesEntry = new byte[1024];
					int read = 0;
					while ((read = zipInputStream.read(bytesEntry)) != -1) {
						fileOutputStream.write(bytesEntry, 0, read);
					}
					fileOutputStream.close();
				}
				zipInputStream.closeEntry();
			}
			zipInputStream.close();
			String fileListConcatenated = "";
			for (int i = 0; i < fileList.size(); ++i) {
				fileListConcatenated += fileList.get(i);
				if (i < fileList.size() - 1) {
					fileListConcatenated += " \n";
				}
			}
			return set("FilesListing", fileListConcatenated);
		} catch (Exception e) {
			log.log("Exception " + e.toString(), Log.ERRORS);
		}
		return false;
	}

	@Override
	public boolean isDragOK() {
		return true;
	}
	
	public boolean isEqualsPatient(Patient patient) {
		CdaChXPath cdaChXPath = new CdaChXPath();
		String cda = getCda();
		if (patient==null) {
			return false;
		}
		if (cda != null) {
			cdaChXPath.setPatientDocument(cda);
			String lastName = cdaChXPath
					.getPatientLastName();
			String firstName = cdaChXPath
					.getPatientFirstName();
			String patientId = cdaChXPath.getPatientNumber();
			if (patientId!=null && patientId.equals(patient.getId())) {
				return true;
			}
			if ((lastName==null || lastName.equals(patient.getName())) && (firstName==null || firstName.equals(patient.getVorname())) ) {
				return true;
			}
		}
		return false;
	}
	
	static public boolean deleteCdaMessages(Anwender anwender) {
		Object[] cdaMessages = getCdaMessages(anwender, true);
		if (cdaMessages!=null) {
			log.log("trying to remove cdamessage "+cdaMessages.length, Log.DEBUGMSG);
			try {
				for (Object cdaMessageObject : cdaMessages) {
					CdaMessage cdaMessage = (CdaMessage) cdaMessageObject;
					log.log("deleting docs with id"+cdaMessage.getId(),Log.DEBUGMSG);
					cdaMessage.deleteDocs();
					log.log("deleting cdaMessage with id"+cdaMessage.getId(),Log.DEBUGMSG);
					cdaMessage.delete();
				}
			} catch (Exception e) {
				log.log("deleting message failed",Log.DEBUGMSG);
			}
		}
		return true;
	}
	
	

}
