package ch.elexis.impfplan.model;

import java.lang.reflect.Method;

import ch.elexis.StringConstants;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.PersistentObjectFactory;

public class ImpfplanFactory extends PersistentObjectFactory {
	
	@SuppressWarnings("unchecked")
	public PersistentObject createFromString(String code){
		try {
			String[] ci = code.split(StringConstants.DOUBLECOLON);
			Class clazz = Class.forName(ci[0]);
			Method load = clazz.getMethod("load", new Class[] { String.class}); //$NON-NLS-1$
			return (PersistentObject) (load.invoke(null, new Object[] {
				ci[1]
			}));
		} catch (Exception ex) {
			// ExHandler.handle(ex);
			return null;
		}
	}
	
	@Override
	public PersistentObject doCreateTemplate(Class<? extends PersistentObject> typ){
		try {
			return (PersistentObject) typ.newInstance();
		} catch (Exception ex) {
			// ExHandler.handle(ex);
			return null;
		}
	}
	
}
