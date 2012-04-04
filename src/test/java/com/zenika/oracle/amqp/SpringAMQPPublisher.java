package com.zenika.oracle.amqp;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import org.springframework.amqp.core.AbstractExchange;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * A test implementation using Spring-AMQP. Do not use it.
 * 
 * @author Pierre Queinnec <pierre.queinnec@zenika.com>
 */
public class SpringAMQPPublisher {

	// FIXME use a schema 'amqp' like pg_amqp
	private final static String BROKER_SQL = "select host, port, vhost, username, password from broker where broker_id = ?";

	private static Map<Integer, BrokerConnectionState> connections;
	static {
		connections = new Hashtable<Integer, SpringAMQPPublisher.BrokerConnectionState>();
	}

	public static void amqpExchangeDeclare(int brokerId, String exchange, String type) {
		try {
			BrokerConnectionState connectionState = getConnectionState(brokerId);
			connectionState.amqpAdmin.declareExchange(new StringTypedExchange(exchange, type));

		} finally {
			amqpDisconnect(brokerId);
		}
	}

	/**
	 * FIXME find how to hook on the TX behavior?
	 */
	public static void amqpPublish(int brokerId, String exchange, String routingKey, String message) {
		amqpPublish(brokerId, exchange, routingKey, message, null);
	}

	/**
	 * FIXME test whether we can declare a type conversion for a Map.
	 */
	public static void amqpPublish(int brokerId, String exchange, String routingKey, String message,
			Map<String, String> properties) {

		try {
			BrokerConnectionState connectionState = getConnectionState(brokerId);

			MessageProperties messageProperties = new MessageProperties();
			if (properties != null) {
				for (Map.Entry<String, String> currEntry : properties.entrySet()) {
					messageProperties.setHeader(currEntry.getKey(), currEntry.getValue());
				}
			}

			Message amqpMessage = new Message(message.getBytes(), messageProperties);
			connectionState.amqpTemplate.send(exchange, routingKey, amqpMessage);

		} finally {
			amqpDisconnect(brokerId);
		}
	}

	/**
	 * No TX.
	 */
	public static void amqpAutonomousPublish() {
		// FIXME examine the Oracle TX behavior
	}

	public static void amqpDisconnect(Integer brokerId) {
		if (brokerId != null) {
			BrokerConnectionState previousConnectionState = connections.remove(brokerId);
			if (previousConnectionState != null) {
				previousConnectionState.connectionFactory.destroy();
			}
		}
	}

	/**
	 * Print each active connections, one per output line.
	 */
	public static void printConnections() {
		if (connections.isEmpty()) {
			System.out.println("no connections");
		}

		for (Map.Entry<Integer, BrokerConnectionState> activeConnections : connections.entrySet()) {
			StringBuilder builder = new StringBuilder();
			builder.append(activeConnections.getKey());
			builder.append(": ");
			builder.append(activeConnections.getValue().connectionFactory.toString());

			System.out.println(builder.toString());
		}
	}

	private static BrokerConnectionState getConnectionState(Integer brokerId) {
		BrokerConnectionState connectionState = connections.get(brokerId);

		if (connectionState == null) {
			connectionState = connect(brokerId);
		}

		return connectionState;
	}

	private static BrokerConnectionState connect(Integer brokerId) {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		fillConnectionFactory(connectionFactory, brokerId);

		// DEBUG
		// connectionFactory.setHost("192.168.56.1");

		AmqpAdmin amqpAdmin = new RabbitAdmin(connectionFactory);
		AmqpTemplate amqpTemplate = new RabbitTemplate(connectionFactory);

		BrokerConnectionState connectionState = new BrokerConnectionState();
		connectionState.connectionFactory = connectionFactory;
		connectionState.amqpAdmin = amqpAdmin;
		connectionState.amqpTemplate = amqpTemplate;
		connections.put(brokerId, connectionState);

		return connectionState;
	}

	private static void fillConnectionFactory(CachingConnectionFactory connectionFactory, int brokerId) {
		try {
			java.sql.Connection conn = DriverManager.getConnection("jdbc:default:connection:");

			PreparedStatement statement = conn.prepareStatement(BROKER_SQL);
			statement.setInt(1, brokerId);
			ResultSet results = statement.executeQuery();

			// FIXME should load-balance between hosts with same ID?
			while (results.next()) {
				String host = results.getString(1);
				int port = results.getInt(2);
				String vhost = results.getString(3);
				String username = results.getString(4);
				String password = results.getString(5);

				connectionFactory.setHost(host);
				connectionFactory.setPort(port);
				connectionFactory.setVirtualHost(vhost);
				connectionFactory.setUsername(username);
				connectionFactory.setPassword(password);
			}

		} catch (SQLException sqle) {
			// FIXME verify this
			sqle.printStackTrace();
		}
	}

	private static class BrokerConnectionState {
		public CachingConnectionFactory connectionFactory;
		public AmqpAdmin amqpAdmin;
		public AmqpTemplate amqpTemplate;
	}

	private static class StringTypedExchange extends AbstractExchange {
		private String type;

		public StringTypedExchange(String name, String type) {
			super(name);
			this.type = type;
		}

		@Override
		public String getType() {
			return this.type;
		}
	}

}
