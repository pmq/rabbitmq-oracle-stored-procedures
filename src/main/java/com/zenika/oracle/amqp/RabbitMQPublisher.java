package com.zenika.oracle.amqp;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Inspired by the great pg_amqp. Seems there's no way to hook on the transactional behavior though (the equivalent of
 * Postgres XactCallback). The static implementation seems pretty mandatory for these kind of functions/procedures. Also
 * not a lot of optimizations are possible due to the JVM behavior with respect to threading.
 * 
 * @author Pierre Queinnec <pierre.queinnec@zenika.com>
 */
public class RabbitMQPublisher {

	// -1 means infinity; value is in milliseconds
	private final static int CONNECTION_CLOSE_TIMEOUT = -1;

	// 0 means infinity; value is in milliseconds
	private final static int CONNECTION_OPEN_TIMEOUT = 0;

	private final static String BROKER_SQL = "select host, port, vhost, username, password from broker where broker_id = ? order by host desc, port";
	static {
		// comment this out if you don't have the corresponding SYS:java.util.PropertyPermission
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void amqpExchangeDeclare(int brokerId, String exchange, String type) {
		Connection connection = null;
		Channel channel = null;
		try {
			BrokerConnectionState connectionState = getConnectionState(brokerId);
			connection = openConnection(connectionState);
			channel = connection.createChannel();

			// declare the exchange
			DeclareOk declareOk = channel.exchangeDeclare(exchange, type);

			if (declareOk == null) {
				// FIXME find the Oracle-way of handling this
			}

		} catch (IOException ioe) {
			// FIXME find the Oracle-way of handling this
			ioe.printStackTrace();

		} finally {
			try {
				if (channel != null) {
					channel.close();
				}

				if (connection != null) {
					connection.close(CONNECTION_CLOSE_TIMEOUT);
				}

			} catch (IOException e) {
				// FIXME find the Oracle-way of handling this
				e.printStackTrace();
			}
		}
	}

	/**
	 * FIXME find how to hook on the TX behavior?
	 */
	public static void amqpPublish(int brokerId, String exchange, String routingKey, String message) {
		amqpPublish(brokerId, exchange, routingKey, message, null);
	}

	/**
	 * FIXME timeout intelligently. FIXME test whether we can declare a type conversion for a Map.
	 */
	public static void amqpPublish(int brokerId, String exchange, String routingKey, String message,
			Map<String, String> properties) {

		Connection connection = null;
		Channel channel = null;
		try {
			BrokerConnectionState connectionState = getConnectionState(brokerId);
			connection = openConnection(connectionState);
			channel = connection.createChannel();

			// send the message
			channel.basicPublish(exchange, routingKey, false, false, null, message.getBytes());

		} catch (IOException ioe) {
			// FIXME find the Oracle-way of handling this
			ioe.printStackTrace();

		} finally {
			try {
				if (channel != null) {
					channel.close();
				}

				if (connection != null) {
					connection.close(CONNECTION_CLOSE_TIMEOUT);
				}

			} catch (IOException e) {
				// FIXME find the Oracle-way of handling this
				e.printStackTrace();
			}
		}
	}

	public static void amqpPrintFullConfiguration(int brokerId) {
		BrokerConnectionState connectionState = new BrokerConnectionState();
		fillAllAdresses(connectionState, brokerId);

		if (connectionState.addresses != null) {
			Iterator<FullAddress> fullAddressesIter = connectionState.addresses.iterator();

			while (fullAddressesIter.hasNext()) {
				FullAddress currFullAddress = (FullAddress) fullAddressesIter.next();
				System.out.println(currFullAddress);
			}
		}
	}

	public static void amqpProbeAllServers(int brokerId) {
		BrokerConnectionState connectionState = new BrokerConnectionState();
		fillAllAdresses(connectionState, brokerId);

		if (connectionState.addresses != null) {
			Iterator<FullAddress> fullAddressesIter = connectionState.addresses.iterator();

			while (fullAddressesIter.hasNext()) {
				FullAddress currFullAddress = (FullAddress) fullAddressesIter.next();

				Connection currConnection = null;
				try {
					currConnection = openConnection(currFullAddress);
					System.out.println(currFullAddress + " : SUCCESSFUL");

				} catch (IOException ioe) {
					System.out.println(currFullAddress + " : FAILED (" + ioe.getMessage() + ')');

				} finally {
					if (currConnection != null) {
						try {
							currConnection.close(CONNECTION_CLOSE_TIMEOUT);

						} catch (IOException ioe) {
							System.out.println("encountered error: " + ioe.getMessage());
						}
					}
				}
			}
		}
	}

	private static BrokerConnectionState getConnectionState(int brokerId) {
		BrokerConnectionState connectionState = new BrokerConnectionState();
		fillAllAdresses(connectionState, brokerId);

		return connectionState;
	}

	private static void fillAllAdresses(BrokerConnectionState connectionState, int brokerId) {
		try {
			java.sql.Connection conn = DriverManager.getConnection("jdbc:default:connection:");

			PreparedStatement statement = conn.prepareStatement(BROKER_SQL);
			statement.setInt(1, brokerId);
			ResultSet results = statement.executeQuery();

			while (results.next()) {
				String host = results.getString(1);
				int port = results.getInt(2);
				String vhost = results.getString(3);
				String username = results.getString(4);
				String password = results.getString(5);

				FullAddress currAddress = new FullAddress(new Address(host, port), vhost, username, password);
				connectionState.addresses.add(currAddress);
			}

		} catch (SQLException sqle) {
			// FIXME verify this
			sqle.printStackTrace();
		}
	}

	private static Connection openConnection(BrokerConnectionState connectionState) {
		boolean connected = false;
		Connection connection = null;

		if (connectionState.addresses != null) {
			Iterator<FullAddress> fullAddressesIter = connectionState.addresses.iterator();

			while (!connected && fullAddressesIter.hasNext()) {
				FullAddress currFullAddress = (FullAddress) fullAddressesIter.next();

				// DEBUG
				System.err.println("trying to connect to " + currFullAddress);

				// try to open the connection
				try {
					connection = openConnection(currFullAddress);
					connected = true;
					System.err.println("connected to " + currFullAddress);

				} catch (IOException ioe) {
					// we catch SocketTimeoutException
					System.err.println("cannot connect to " + currFullAddress);
					// FIXME comment out
					ioe.printStackTrace();
				}
			}
		}

		// DEBUG
		if (!connected) {
			System.err.println("giving up, no AMQP servers is responding");
		}

		return connection;
	}

	private static Connection openConnection(FullAddress address) throws IOException {
		Connection connection = null;

		// DEBUG
		System.err.println("trying to connect to " + address);

		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost(address.address.getHost());
		connectionFactory.setPort(address.address.getPort());
		connectionFactory.setUsername(address.username);
		connectionFactory.setPassword(address.password);
		connectionFactory.setVirtualHost(address.vhost);

		connectionFactory.setConnectionTimeout(CONNECTION_OPEN_TIMEOUT);

		// try to open the connection
		connection = connectionFactory.newConnection();
		System.err.println("connected to " + address);
		return connection;
	}

	private static class BrokerConnectionState {
		public List<FullAddress> addresses;

		public BrokerConnectionState() {
			this.addresses = new ArrayList<FullAddress>();
		}
	}

	private static class FullAddress {
		public Address address;
		public String vhost;
		public String username;
		public String password;

		public FullAddress(Address address, String vhost, String username, String password) {
			super();
			this.address = address;
			this.vhost = vhost;
			this.username = username;
			this.password = password;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.username);
			builder.append('@');
			builder.append(this.address.getHost());
			builder.append(':');
			builder.append(this.address.getPort());
			builder.append(this.vhost);

			return builder.toString();
		}
	}

}
