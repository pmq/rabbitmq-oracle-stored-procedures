package com.zenika.oracle.amqp;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * A simple test using Spring-AMQP.
 * 
 * Use the following declaration: <code>
 * create or replace procedure test_spring_amqp as language java name
 * 'com.zenika.oracle.amqp.TestSpringAMQP.test1()';
 * </code>
 * 
 * Then call it like this: <code>
 * call test_spring_amqp();
 * </code>
 * 
 * @author Pierre Queinnec <pierre.queinnec@zenika.com>
 */
public class TestSpringAMQP {

	public static void test1() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setHost("192.168.56.1");
		connectionFactory.setPort(5672);

		AmqpTemplate amqpTemplate = new RabbitTemplate(connectionFactory);

		MessageProperties properties = new MessageProperties();
		properties.setContentType("text/plain+oracle");
		Message message = new Message("Hello from Spring-AMQP...".getBytes(), properties);
		amqpTemplate.send("oracle", "test", message);

		connectionFactory.destroy();
	}

}
