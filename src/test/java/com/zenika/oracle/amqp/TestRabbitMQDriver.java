package com.zenika.oracle.amqp;

import java.io.IOException;
import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * A simple test using the RabbitMQ Java driver. Used to test the Oracle JVM behavior.
 * 
 * @author Pierre Queinnec <pierre.queinnec@zenika.com>
 */
public class TestRabbitMQDriver {

	static {
		// System.setProperty("java.net.preferIPv4Stack", "true");
		System.out.println("SimpleRabbitMQ.static");
	}

	public TestRabbitMQDriver() {
		System.out.println("SimpleRabbitMQ.SimpleRabbitMQ()");
	}

	/**
	 * FIXME find how to hook on the TX behavior?
	 */
	public static void amqpPublish(int brokerId, String exchange, String routingKey, String message) {
		amqpPublish(brokerId, exchange, routingKey, message, null);
	}

	/**
	 * FIXME test wether we can declare a type conversion for a Map.
	 * 
	 * @throws IOException
	 */
	public static void amqpPublish(int brokerId, String exchange, String routingKey, String message,
			Map<String, String> properties) {

		System.out.println("SimpleRabbitMQ.amqpPublish() #1");
		try {
			ConnectionFactory connectionFactory = new ConnectionFactory();
			connectionFactory.setHost("192.168.56.1");
			connectionFactory.setPort(5672);

			System.out.println("SimpleRabbitMQ.amqpPublish() #2");
			Connection connection = connectionFactory.newConnection();
			System.out.println("SimpleRabbitMQ.amqpPublish() #3");
			Channel channel = connection.createChannel();

			System.out.println("SimpleRabbitMQ.amqpPublish() #4");
			channel.basicPublish(exchange, routingKey, false, false, null, message.getBytes());
			System.out.println("SimpleRabbitMQ.amqpPublish() #5");
			channel.close();
			System.out.println("SimpleRabbitMQ.amqpPublish() #6");
			connection.close();

		} catch (Exception e) {
			System.out.println("SimpleRabbitMQ.amqpPublish() #7");
			e.printStackTrace();
		}
	}

}
