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
	public static BasicProperties basicPropertiesFromXml(String XmlString)
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
			      String nodeName = element.getNodeName().toUpperCase();

			      if     (nodeName == "CONTENTTYPE")  	 prop_builder =  prop_builder.contentType    				  (element.getTextContent());
			      else if(nodeName == "DELIVERYMODE") 	 prop_builder =  prop_builder.deliveryMode   (Integer.parseInt(element.getTextContent())); 
			      else if(nodeName == "PRIORITY")     	 prop_builder =  prop_builder.priority       (Integer.parseInt(element.getTextContent())); 
			      else if(nodeName == "MESSAGEID")    	 prop_builder =  prop_builder.messageId      				  (element.getTextContent());
			      else if(nodeName == "APPID") 			 prop_builder =  prop_builder.appId							  (element.getTextContent());
			      else if(nodeName == "CLUSTERID") 	     prop_builder =  prop_builder.clusterId				  		  (element.getTextContent());
			      else if(nodeName == "CORRELATIONID") 	 prop_builder =  prop_builder.correlationId				  	  (element.getTextContent());
			      else if(nodeName == "EXPIRATION") 	 prop_builder =  prop_builder.expiration				  	  (element.getTextContent());
			      else if(nodeName == "REPLYTO") 		 prop_builder =  prop_builder.replyTo				  		  (element.getTextContent());
			      //else if(nodeName == "TIMESTAMP") 		 prop_builder =  prop_builder.timestamp				  (element.getTextContent());
			      else if(nodeName == "TYPE") 			 prop_builder =  prop_builder.type				  			  (element.getTextContent());			      
			      else if(nodeName == "HEADERS"){
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
