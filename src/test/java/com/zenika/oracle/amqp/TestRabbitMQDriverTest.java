package com.zenika.oracle.amqp;

//import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
//import org.junit.Before;
import org.junit.BeforeClass;
//import org.junit.Ignore;
import org.junit.Test;


//import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
import com.zenika.oracle.amqp.RabbitMQPublisher.BrokerConnectionState;


/**
 * 
 * @author Pierre Queinnec <pierre.queinnec@zenika.com>
 */
//@Ignore
public class TestRabbitMQDriverTest {
	
	static BrokerConnectionState connectionState;
	
	@BeforeClass
    public static void allTestsStarted() throws IOException, TimeoutException {
		connectionState = RabbitMQPublisher.createConnectionState("172.21.10.15", 5672, "/", "guest", "guest");
    }
	
	@AfterClass
    public static void allTestsFineshed() throws IOException, TimeoutException {
		
    }

	@Test
	public void testAmqpPublishIntStringStringString() {		
		TestRabbitMQDriver.amqpPublish(1, "oracle2", "test.unit", "Hello World!");
		
	}
	
	@Test
	public void testRabbitPublisher() {				
		RabbitMQPublisher.amqpPublish(connectionState, "oracle5", "test.unit", "Hello World!", null);
		
	}
	
	@Test
	public void testRabbitPropPublisher() {				
		RabbitMQPublisher.amqpPublish(connectionState, "oracle5", "test.unit", "PROP"
				,"<properities>"
						+ "<DELIVERYMODE>2</DELIVERYMODE>"
						+ "<HEADERS>"
							+ "<content_type>application/vnd.masstransit+json</content_type>"
							+ "<head1>test</head1>"
						+ "</HEADERS>"
						+ "<MESSAGEID>1</MESSAGEID>"
						+ "<CONTENTTYPE>XML</CONTENTTYPE>"
						+ "<PRIORITY>10</PRIORITY>"
						+ "<CONTENTENCODING>iso-8859-5</CONTENTENCODING>"
				+ "</properities>");
		
	}

}
