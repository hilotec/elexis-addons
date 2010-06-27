package ch.elexis.global_inbox;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.services.GlobalServiceDescriptors;
import ch.elexis.services.IDocumentManager;
import ch.elexis.text.FileDocument;
import ch.elexis.util.Extensions;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.viewers.ViewerConfigurer.ContentProviderAdapter;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.TimeTool;

public class InboxContentProvider extends ContentProviderAdapter {
	ArrayList<File> files = new ArrayList<File>();
	InboxView view;
	LoadJob loader;

	public void setView(InboxView view) {
		this.view = view;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	public void reload() {
		loader.run(null);
	}

	public InboxContentProvider() {
		loader = new LoadJob();
		loader.schedule(1000);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return files == null ? null : files.toArray();
	}

	Pattern patMatch = Pattern.compile("([0-9]+)_(.+)");

	private void addFiles(List<File> list, File dir) {
		File[] contents = dir.listFiles();
		for (File file : contents) {
			if (file.isDirectory()) {
				addFiles(list, file);
			} else {
				Matcher matcher = patMatch.matcher(file.getName());
				if (matcher.matches()) {
					String num = matcher.group(1);
					String nam = matcher.group(2);
					List<Patient> lPat = new Query(Patient.class,
							Patient.FLD_PATID, num).execute();
					if (lPat.size() == 1) {
						Patient pat = lPat.get(0);
						String cat = Activator.getDefault().getCategory(file);
						if (cat.equals("-") || cat.equals("??")) {
							cat = null;
						}
						IDocumentManager dm = (IDocumentManager) Extensions
								.findBestService(GlobalServiceDescriptors.DOCUMENT_MANAGEMENT);
						try {
							FileDocument fd = new FileDocument(pat, nam, cat,
									file,
									new TimeTool().toString(TimeTool.DATE_GER),
									"");
							dm.addDocument(fd);
							fd.delete();
							Activator.getDefault().getContentProvider()
									.reload();
							return;
						} catch (Exception ex) {
							ExHandler.handle(ex);
							SWTHelper.alert(Messages.InboxView_error,
									ex.getMessage());
						}
					}
				}
				list.add(file);
			}
		}
	}

	class LoadJob extends Job {

		public LoadJob() {
			super("GlobalInbox"); //$NON-NLS-1$
			setPriority(DECORATE);
			setUser(false);
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String filepath = Hub.localCfg.get(Preferences.PREF_DIR, null);
			if (filepath == null) {
				return new Status(Status.ERROR, Activator.PLUGIN_ID,
						"Es ist in den Einstellungen kein Eingangsverzeichnis definiert");
			}
			File dir = new File(filepath);
			Object dm = Extensions
					.findBestService(GlobalServiceDescriptors.DOCUMENT_MANAGEMENT);
			if (dm == null) {
				return new Status(
						Status.ERROR,
						Activator.PLUGIN_ID,
						Messages.InboxContentProvider_thereIsNoDocumentManagerHere);
			}
			if (dir == null || !dir.isDirectory()) {
				return new Status(Status.ERROR, Activator.PLUGIN_ID,
						Messages.InboxContentProvider_noInboxDefined);
			}
			IDocumentManager documentManager = (IDocumentManager) dm;
			String[] cats = documentManager.getCategories();

			if (cats != null) {
				for (String cat : cats) {
					File subdir = new File(dir, cat);
					if (!subdir.exists()) {
						subdir.mkdirs();
					}
				}
			}

			files.clear();
			addFiles(files, dir);
			if (view != null) {
				view.reload();
			}
			schedule(120000L);
			return Status.OK_STATUS;
		}

	}

}
