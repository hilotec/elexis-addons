package ch.elexis.importer.rtf;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;

import ch.elexis.StringConstants;
import ch.elexis.data.Patient;
import ch.elexis.exchange.KontaktMatcher;
import ch.elexis.exchange.KontaktMatcher.CreateMode;
import ch.elexis.services.GlobalServiceDescriptors;
import ch.elexis.services.IDocumentManager;
import ch.elexis.text.GenericDocument;
import ch.elexis.util.Extensions;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.TimeTool;

public class Importer extends ImporterPage {
	Parser parser = new Parser();
	Log log = Log.get("RTF importer");
	
	// Betrifft: Muster Max, Placebogasse 34, Winterthur, 20.09.58
	//Pattern namePattern = Pattern
		//.compile("^Betrifft:\\s*([^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^,]+)\\s*");
	Pattern namePattern = Pattern
	.compile("^Betrifft:\\s*");

	@Override
	public IStatus doImport(IProgressMonitor monitor) throws Exception{
		File dir = new File(this.results[0]);
		importDirectory(dir);
		return Status.OK_STATUS;
	}
	
	private void importDirectory(File dir){
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					importDirectory(file);
				} else {
					if (file.getName().toLowerCase().endsWith(".rtf")) {
						try {
							String text = parser.extractText(file.getAbsolutePath());
							Matcher m = namePattern.matcher(text);
							if (m.find()) {
								String name = m.group(1);
								String firstname = "";
								String street = m.group(2);
								String[] place = KontaktMatcher.normalizeAddress(m.group(3));
								String birthdate = m.group(4);
								String[] pers = name.split(" +", 2);
								if (pers.length > 1) {
									name = pers[0];
									firstname = pers[1];
								}
								
								Patient pat =
									KontaktMatcher.findPatient(name, firstname, birthdate,
										StringConstants.EMPTY, street, place[0], place[1],
										StringConstants.EMPTY, CreateMode.FAIL);
								if(pat==null){
									log.log("Did not find patient matching "+name+" "+firstname+" "+birthdate+" in "+file.getAbsolutePath(),Log.WARNINGS);
								}else{
									IDocumentManager dm =
										(IDocumentManager) Extensions
											.findBestService(GlobalServiceDescriptors.DOCUMENT_MANAGEMENT);
									try {
										GenericDocument fd =
											new GenericDocument(pat, file.getName(), "Vitomed Import", file, new TimeTool()
												.toString(TimeTool.DATE_GER), "", null);
										dm.addDocument(fd);
										fd = null;
									} catch (Exception ex) {
										ExHandler.handle(ex);
										log.log("Import failed: "+file.getAbsolutePath()+" "+ex.getMessage(), Log.ERRORS);
									}

								}
							} else {
								log.log(
									"Did not find patient pattern in " + file.getAbsolutePath(),
									Log.WARNINGS);
							}
						} catch (Exception ex) {
							log.log("Failed importing " + file.getAbsolutePath(), Log.ERRORS);
						}
					}
				}
			}
		}
	}
	
	@Override
	public String getTitle(){
		return "RTF import";
	}
	
	@Override
	public String getDescription(){
		return "Import a directory with rtf files into the document manager";
	}
	
	@Override
	public Composite createPage(Composite parent){
		DirectoryBasedImporter dbi = new DirectoryBasedImporter(parent, this);
		dbi.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		return dbi;
	}
	
}
