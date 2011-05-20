package at.herzpraxis.elexis.connect.cobasmira.packages;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME =
		"at.herzpraxis.elexis.connect.cobasmira.packages.messages"; //$NON-NLS-1$
	
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	
	private Messages(){}
	
	public static String getString(String key){
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
