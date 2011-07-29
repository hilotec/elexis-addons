package ch.elexis.importer.rtf;

import java.io.FileInputStream;

import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;


public class Parser {
	public String extractText(String file) throws Exception{
		FileInputStream stream = new FileInputStream(file);
		RTFEditorKit kit = new RTFEditorKit();
		Document doc = kit.createDefaultDocument();
		kit.read(stream, doc, 0);
		return doc.getText(0, doc.getLength());
		
	}
}
