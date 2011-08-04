package ch.elexis.importer.rtf;

import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.rtf.RTFEditorKit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

public class Rtf2XML {
    private DefaultStyledDocument rtfSource;
    private org.w3c.dom.Document xmlTarget;
    private org.w3c.dom.Element xmlRoot;

    private void expandElement(javax.swing.text.Element rtfElement) {
        for (int i = 0; i < rtfElement.getElementCount(); i++) {
            javax.swing.text.Element rtfNextElement = rtfElement.getElement(i);
            if (rtfNextElement.isLeaf()) {
                try {
                    addElement(rtfNextElement);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                expandElement(rtfNextElement);
            }
        }
    }

    private void addElement(javax.swing.text.Element rtfElement)
            throws UnsupportedEncodingException, BadLocationException {

        String style = new String(rtfSource.getLogicalStyle(rtfElement.getStartOffset())
                .getName().getBytes("ISO-8859-1"));

        String text = new String(rtfSource.getText(rtfElement.getStartOffset(),
                rtfElement.getEndOffset() - rtfElement.getStartOffset())
                .getBytes("UTF-8"));

        org.w3c.dom.Element node = xmlTarget.createElement("p");
        node.appendChild(xmlTarget.createTextNode(text));
        node.setAttribute("style", style);
        xmlRoot.appendChild(node);
    }

    public void convert(String sourceFileName) throws Exception {
        rtfSource = new DefaultStyledDocument();
        RTFEditorKit kit = new RTFEditorKit();
        kit.read(new FileInputStream(sourceFileName), rtfSource, 0);

        xmlTarget = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();

        BranchElement rtfRoot = (BranchElement) rtfSource.getDefaultRootElement();
        xmlRoot = xmlTarget.createElement("data");
        expandElement(rtfRoot);
        xmlTarget.appendChild(xmlRoot);

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.transform(new DOMSource(xmlTarget),
                new StreamResult(new FileOutputStream(sourceFileName + ".xml")));
    }

    
}