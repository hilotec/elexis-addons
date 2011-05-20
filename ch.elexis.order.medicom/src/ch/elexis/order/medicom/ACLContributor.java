package ch.elexis.order.medicom;

import ch.elexis.admin.ACE;
import ch.elexis.admin.IACLContributor;

public class ACLContributor implements IACLContributor {
	public static final ACE MEDICOM = new ACE(ACE.ACE_ROOT, "Medicom");
	public static final ACE SETTINGS = new ACE(MEDICOM, "Einstellungen");
	public static final ACE ORDER = new ACE(MEDICOM, "bestellen");
	
	@Override
	public ACE[] getACL(){
		return new ACE[] {
			SETTINGS, ORDER
		};
	}
	
	@Override
	public ACE[] reject(ACE[] acl){
		return null;
	}
	
}
