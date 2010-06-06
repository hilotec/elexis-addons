package ch.elexis.order.medicom;

import java.util.ArrayList;
import java.util.List;

import ch.elexis.StringConstants;
import ch.rgw.tools.StringTool;

public class IGMFile {
	private List<String> lines;

	public static final String TYPE_BASE_NO_VAT="01";
	public static final String TYPE_BASE_VAT="11";
	public static final String TYPE_BASE_VAT_PHARM="21";
	public static final String TYPE_UPDATE_PRICE="02";
	public static final String TYPE_UPDATE_EAN="06";
	public static final String TYPE_UPDATE_PRICE_EAN="07";
	public static final String TYPE_UPDATE_PRICE_VAT="10";
	public static final String TYPE_UPDATE_PRICE_VAT_PHARM="20";
	public static final String TYPE_ORDER_FULL="03";
	public static final String TYPE_ORDER_SHORT="08";
	public static final String TYPE_ORDER_SPECIAL="04";
	
	
	public IGMFile(){
		lines=new ArrayList<String>();
	}

	public String createFile(){
		return StringTool.join(lines, StringConstants.CRLF);
	}
	
	public void addLine(String type, Field...fields ){
		StringBuilder line=new StringBuilder();
		line.append(type);
		for(Field field:fields){
			line.append(field.contents);
		}
		lines.add(line.toString());
	}
	
	static class Field{
		String contents;
		public Field(String contents, int len){
			this.contents=StringTool.pad(StringTool.RIGHTS, ' ', contents, len);
		}
		public Field(int contents, int len){
			this.contents=StringTool.pad(StringTool.LEFT, '0', Integer.toString(contents), len);
		}
	}
}
