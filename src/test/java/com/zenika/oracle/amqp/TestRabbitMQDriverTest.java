package com.zenika.oracle.amqp;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
//import org.junit.Before;
import org.junit.BeforeClass;
//import org.junit.Ignore;
import org.junit.Test;

//import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 
 * @author Pierre Queinnec <pierre.queinnec@zenika.com>
 */
//@Ignore
public class TestRabbitMQDriverTest {
	
	static Connection connection;
	
	@BeforeClass
    public static void allTestsStarted() throws IOException, TimeoutException {
        
        ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost("172.21.10.15");
		connectionFactory.setPort(5672);
		connectionFactory.setUsername("guest");
		connectionFactory.setPassword("guest");

		connection = connectionFactory.newConnection();
		System.out.println("All tests started");
    }
	
	@AfterClass
    public static void allTestsFineshed() throws IOException, TimeoutException {
		connection.close();
    }

	
	@Test
	public void testConnectionIsOpen() throws IOException, TimeoutException {
		assertEquals(connection.isOpen(),true);		
	}
	
	@Test
	public void testAmqpExchangeDeclare() throws IOException {
		Channel channel = connection.createChannel();
		channel.exchangeDeclare("oracle2", "fanout");
		boolean b = true;
		channel.exchangeDeclare("oracle3", "fanout",b);
		channel.exchangeDeclare("oracle4", "fanout",false);
	}

	@Test
	public void testAmqpPublishIntStringStringString() {		
		TestRabbitMQDriver.amqpPublish(1, "oracle2", "test.unit", "Hello World!");
		
	}

}
