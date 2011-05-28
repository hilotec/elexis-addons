package at.medevit.elexis.properties.propertyPage.provider;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.swtdesigner.ResourceManager;

public class CountryComboLabelProvider extends LabelProvider {
	
	@Override
	public Image getImage(Object element){
		String el = (String) element;
		if(el.trim().equalsIgnoreCase("AT")) return ResourceManager.getPluginImage("at.medevit.elexis.properties", "rsc/icons/at.png");
		if(el.trim().equalsIgnoreCase("CH")) return ResourceManager.getPluginImage("at.medevit.elexis.properties", "rsc/icons/ch.png");
		if(el.trim().equalsIgnoreCase("DE")) return ResourceManager.getPluginImage("at.medevit.elexis.properties", "rsc/icons/de.png");
		if(el.trim().equalsIgnoreCase("FR")) return ResourceManager.getPluginImage("at.medevit.elexis.properties", "rsc/icons/fr.png");
		if(el.trim().equalsIgnoreCase("IT")) return ResourceManager.getPluginImage("at.medevit.elexis.properties", "rsc/icons/it.png");
		if(el.trim().equalsIgnoreCase("FL")) return ResourceManager.getPluginImage("at.medevit.elexis.properties", "rsc/icons/fl.png");
		return null;
	}

	@Override
	public String getText(Object element){
		return (String) element;
	}

}
