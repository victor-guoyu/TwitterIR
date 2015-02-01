package ir;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Victor Guo
 * 
 *         XML Query Reader
 */
public class QueryReader {

    public QueryReader(File xml, QueryProcessor processor) throws Exception {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document queryDoc = docBuilder.parse(xml);
        queryDoc.getDocumentElement().normalize();
        NodeList nodeList = queryDoc.getElementsByTagName("top");
        for (int i=0; i<nodeList.getLength(); i++) {
            Node current = nodeList.item(i);
            if (current.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) current;
                String queryString = element.getElementsByTagName("title").item(0).getTextContent();
                processor.process(queryString);
            }
        }
    }

    public static interface QueryProcessor {
        public void process(String queryString) throws Exception;
    }
}