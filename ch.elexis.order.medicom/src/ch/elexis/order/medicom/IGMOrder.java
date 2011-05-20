package ch.elexis.order.medicom;

import ch.elexis.data.Artikel;

public class IGMOrder extends IGMFile {
	private String ident;
	
	public IGMOrder(String kundennummer){
		ident = kundennummer;
	}
	
	public void addLine(Artikel art, int menge){
		addLine(IGMFile.TYPE_ORDER_FULL, new Field(0, 1), new Field(ident, 10), new Field(Integer
			.parseInt(art.getPharmaCode()), 7), new Field(art.getName(), 50), new Field(menge, 4),
			new Field(art.getEAN(), 13), new Field("1", 1));
		
	}
	
}
