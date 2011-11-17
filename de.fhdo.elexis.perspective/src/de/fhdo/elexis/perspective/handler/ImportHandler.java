package de.fhdo.elexis.perspective.handler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.PerspectiveDescriptor;
import org.eclipse.ui.internal.registry.PerspectiveRegistry;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Imports selected perspectives from given .xml files
 * 
 * This class pops up a FileDialog to select one or more stored perspectives to be restored
 * An error correction routine is provided if perspectives with the same name are tried to restore
 * 
 * @author Bernhard Rimatzki, Thorsten Wagner, Pascal Proksch, Sven Lüttmann
 * @version 1.0
 * 
 */

public class ImportHandler extends AbstractHandler implements IHandler {

	@Override
	@SuppressWarnings("all") 
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IWorkbenchWindow mainWindow = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		PerspectiveRegistry perspRegistry = (PerspectiveRegistry) WorkbenchPlugin.getDefault().getPerspectiveRegistry();
		String importMessage = "";

		//
		// Open a FileDialog to select the .xml files with stored perspectives
		// Only display .xml Files to select
		//
		FileDialog diag = new FileDialog(mainWindow.getShell(), SWT.MULTI);
		
		String[] filterNames = {"XML"};
		String[] filterExtensions = {"*.xml"};
		
		diag.setFilterNames(filterNames);
		diag.setFilterExtensions(filterExtensions);
		
		if( diag.open() == null )
			return null;
		


		//
		// Since it is possible to select multiple perspectives to be restored we have to iterate over the selected files
		//
		for ( String file : diag.getFileNames() )
		{	
			String filename = diag.getFilterPath() +"/"+ file;
			FileReader reader;
			XMLMemento memento = null;
			
			try {
				reader = new FileReader( new File( filename ) );
				memento = XMLMemento.createReadRoot(reader);
				PerspectiveDescriptor newPersp = new PerspectiveDescriptor(null, null, null);
				
				//
				// Get the label and the ID of the stored perspective
				//
				String label = memento.getChild("descriptor").getString("label");
				String id = memento.getChild("descriptor").getString("id");

				//
				// Find the perspective by label within the preference store
				//
				PerspectiveDescriptor pd = (PerspectiveDescriptor)perspRegistry.findPerspectiveWithLabel(label);
				
				String[] buttonLabels = {"Abbrechen","Überschreiben","Neu benennen"};
		
				while(pd != null) { 

					//
					// If pd != null the perspective is already present in the preference store though we have to store it with a different name
					//
					String notDeleted = "";
					String dialogMessage = "Name der zu importierenden Perspektive \""+ label  + "\" ist bereits vorhanden.\n" + 
					"Was soll getan werden?";
				
					MessageDialog mesDiag = new MessageDialog(mainWindow.getShell(), "Perspektive überschreiben", null, dialogMessage, 0, buttonLabels, 0);
					int ergMesDiag = mesDiag.open();
					
					
					if(ergMesDiag == 0)  //Cancel was pressed
						return null;
					else if(ergMesDiag == 1) // Overwrite was pressed
					{
						perspRegistry.deletePerspective( pd );
						PerspectiveDescriptor pd2 = (PerspectiveDescriptor)perspRegistry.findPerspectiveWithLabel(label);
						
						//
						// If the perspective could not be deleted, the user have to choose another name
						//
						if(pd2 != null)
						{
							notDeleted = "Gewählte Perspektive kann nicht überschrieben werden! \n";
							ergMesDiag = 2;
						}
						
						//
						// After the Perspective has been deleted the descriptor has to be null
						//
						pd = null;
					}
					
					if(ergMesDiag == 2) // Rename was pressed
					{

						String dialogMessageOverride = notDeleted + "Wählen Sie einen neuen Namen für die Perspektive:";;
						InputDialog inputDiag = new InputDialog(mainWindow.getShell(), "Perspektive umbenennen", dialogMessageOverride, null, null);
						
						inputDiag.open();
						
						String[] idsplit = id.split("\\.");
						System.out.println("ID: " + idsplit.length);
						id = "";
						label = inputDiag.getValue();
						
						for(int i = 0; i < idsplit.length-1; i++) {
							id += idsplit[i] + ".";
						}
						
						id += label;
						
						
						//
						// Create a new perspective with the new name
						//
						newPersp = new PerspectiveDescriptor( id, label, pd );
						
						pd = (PerspectiveDescriptor)perspRegistry.findPerspectiveWithLabel(label);
					}
				}
				
				memento.getChild("descriptor").putString("label", label);
				memento.getChild("descriptor").putString("id", id);
				
				newPersp.restoreState(memento);
				
				

				reader.close();

				//
				// Save the new generated perspective in the preference store
				//
				perspRegistry.saveCustomPersp(newPersp, memento);
				
				importMessage += file + " gespeichert als: " + newPersp.getLabel() + "\n";

			} catch (WorkbenchException e) {
				unableToLoadPerspective(e.getStatus());
			}catch (IOException e) {
				unableToLoadPerspective(null);	
			}
		}
		
		MessageDialog.openInformation(mainWindow.getShell(), "Erfolgreich importiert!", "Perspektive(n) erfolgreich importiert\n" + importMessage);
		
		return null;
	}
	
	
	
	private void unableToLoadPerspective(IStatus status) {
		String msg = "Unable to load perspective.";
		
		if (status == null) {
			IStatus errStatus = new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, msg); 
			StatusManager.getManager().handle(errStatus,
					StatusManager.SHOW | StatusManager.LOG);
		} else {
			StatusAdapter adapter = new StatusAdapter(status);
			adapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY, msg);
			StatusManager.getManager().handle(adapter,
					StatusManager.SHOW | StatusManager.LOG);
		}
	}
}
