package com.zenika.oracle.amqp;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.rabbitmq.client.AMQP.BasicProperties;

public class xml {
	public static BasicProperties getMapFromXml(String XmlString)
	{
		//Map<String, String> properties = new HashMap<String,String>();
		BasicProperties.Builder prop_builder = new BasicProperties.Builder();
	    try {
	    	DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    	InputSource is = new InputSource();
		    is.setCharacterStream(new StringReader(XmlString));
		    try {
				Document doc = db.parse(is);
				NodeList nodes = doc.getFirstChild().getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
			      Element element = (Element) nodes.item(i);

			      if     (element.getNodeName() == "CONTENTTYPE")  prop_builder =  prop_builder.contentType (element.getTextContent());
			      else if(element.getNodeName() == "DELIVERYMODE") prop_builder =  prop_builder.deliveryMode(Integer.parseInt(element.getTextContent())); 
			      else if(element.getNodeName() == "PRIORITY")     prop_builder =  prop_builder.priority    (Integer.parseInt(element.getTextContent())); 
			      else if(element.getNodeName() == "MESSAGEID")    prop_builder =  prop_builder.messageId   (element.getTextContent()); 
			      else if(element.getNodeName() == "HEADERS"){
			    	  NodeList header_nodes = element.getChildNodes();
			    	  Map<String, Object> headers = new HashMap<String,Object>();
			    	  for (int j = 0; j < header_nodes.getLength(); j++) {
			    		  Element header = (Element) header_nodes.item(j);
			    	  	  headers.put(header.getNodeName(), header.getTextContent());
			    	  }
			    	  prop_builder =  prop_builder.headers(headers);			    	  
			      }
			    }
			} catch (SAXException e) {
			} catch (IOException e) {}
		    
		} catch (ParserConfigurationException e) {		
		}
	    return prop_builder.build();
	}
}
