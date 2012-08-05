package com.zenika.oracle.amqp;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

	private final static int EXIT_SUCCESS = 0;
	private final static int E_CANNOT_SEND = -1;
	private final static int E_CANNOT_CLOSE = -2;

	// enable/disable the debug output
	private final static boolean ENABLE_DEBUG = true;

	// -1 means infinity; value is in milliseconds
	private final static int CONNECTION_CLOSE_TIMEOUT = 2000;

	// 0 means infinity; value is in milliseconds
	private final static int CONNECTION_OPEN_TIMEOUT = 2000;

	private final static String ORACLE_INTERNAL_JDBC_URL = "jdbc:default:connection:";
	private final static String BROKER_SQL = "select host, port, vhost, username, password from broker where broker_id = ? order by host desc, port";

	private static Hashtable<Integer, FullAddress> state;
	static {
		// comment this out if you don't have the corresponding SYS:java.util.PropertyPermission
		System.setProperty("java.net.preferIPv4Stack", "true");
		state = new Hashtable<Integer, FullAddress>();
	}

	/**
	 * Declare an AMQP exchange on the currently used broker with the given ID.
	 * 
	 * @param brokerId
	 *            the ID of the broker in the configuration table
	 * @param exchange
	 *            the name of the AMQP exchange
	 * @param type
	 *            the type of the exchange, as defined by the RabbitMQ driver
	 * @see Channel#exchangeDeclare(String, String)
	 * @return an error code, see the source
	 */
	public static int amqpExchangeDeclare(int brokerId, String exchange, String type) {
		// FIXME declare on all brokers for brokerId?
		Connection connection = null;
		Channel channel = null;
		try {
			BrokerConnectionState connectionState = getConnectionState(brokerId);
			connection = openConnection(connectionState);
			channel = connection.createChannel();

			// declare the exchange
			channel.exchangeDeclare(exchange, type);

		} catch (IOException ioe) {
			ioe.printStackTrace();
			return E_CANNOT_SEND;

		} finally {
			try {
				if (channel != null) {
					channel.close();
				}

				if (connection != null) {
					connection.close(CONNECTION_CLOSE_TIMEOUT);
				}

			} catch (IOException e) {
				e.printStackTrace();
				return E_CANNOT_CLOSE;
			}
		}

		// everything went OK
		return EXIT_SUCCESS;
	}

	/**
	 * Publish an AMQP message to the given exchange.
	 * 
	 * @param brokerId
	 *            the ID of the broker in the configuration table
	 * @param exchange
	 *            the name of the AMQP exchange
	 * @param routingKey
	 *            the AMQP routing key
	 * @param message
	 *            the payload
	 * @return an error code, see the source
	 */
	public static int amqpPublish(int brokerId, String exchange, String routingKey, String message) {
		return amqpPublish(brokerId, exchange, routingKey, message, null);
	}

	/**
	 * FIXME timeout intelligently. FIXME test whether we can declare a type conversion for a Map.
	 */
	public static int amqpPublish(int brokerId, String exchange, String routingKey, String message,
			Map<String, String> properties) {

		Connection connection = null;
		Channel channel = null;
		try {
			BrokerConnectionState connectionState = getConnectionState(brokerId);
			connection = openConnection(connectionState);
			channel = connection.createChannel();

			// send the message
			channel.basicPublish(exchange, routingKey, false, false, null, message.getBytes());

			// remember the current broker used
			state.put(brokerId, connectionState.currentAddress);

		} catch (IOException ioe) {
			ioe.printStackTrace();
			return E_CANNOT_SEND;

		} finally {
			try {
				if (channel != null) {
					channel.close();
				}

				if (connection != null) {
					connection.close(CONNECTION_CLOSE_TIMEOUT);
				}

			} catch (IOException e) {
				e.printStackTrace();
				return E_CANNOT_CLOSE;
			}
		}

		// everything went OK
		return EXIT_SUCCESS;
	}

	/**
	 * Print the current configuration for broker definitions. In case there's more than one broker per ID, the active
	 * one is indicated. The resulting output is written on stdout.
	 * 
	 * @param brokerId
	 *            the ID of the broker
	 */
	public static void amqpPrintFullConfiguration(int brokerId) {
		BrokerConnectionState connectionState = new BrokerConnectionState();
		fillAllAdresses(connectionState, brokerId);

		// retrieve the active broker instance
		FullAddress activeBroker = state.get(brokerId);
		boolean foundActiveInDb = false;

		if (connectionState.addresses != null) {
			Iterator<FullAddress> fullAddressesIter = connectionState.addresses.iterator();

			while (fullAddressesIter.hasNext()) {
				FullAddress currFullAddress = (FullAddress) fullAddressesIter.next();

				if (currFullAddress.equals(activeBroker)) {
					System.out.println(currFullAddress + " (active)");
					foundActiveInDb = true;

				} else {
					System.out.println(currFullAddress);
				}
			}
		}

		if (activeBroker == null) {
			System.out.println("WARNING: no previously used broker was found");

		} else if (!foundActiveInDb) {
			System.out.println("WARNING: the previously used broker (" + activeBroker
					+ ") is not anymore in the DB configuration");
		}
	}

	/**
	 * Probe the current state for the defined brokers by trying to connect. In case there's more than one broker per
	 * ID, probe them all. The resulting output is written on stdout.
	 * 
	 * @param brokerId
	 *            the ID of the broker
	 */
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

		// fill in the previously used broker instance
		connectionState.currentAddress = state.get(brokerId);

		return connectionState;
	}

	private static void fillAllAdresses(BrokerConnectionState connectionState, int brokerId) {
		try {
			java.sql.Connection conn = DriverManager.getConnection(ORACLE_INTERNAL_JDBC_URL);

			try {
				PreparedStatement statement = conn.prepareStatement(BROKER_SQL);

				try {
					statement.setInt(1, brokerId);
					ResultSet results = statement.executeQuery();

					try {
						while (results.next()) {
							String host = results.getString(1);
							int port = results.getInt(2);
							String vhost = results.getString(3);
							String username = results.getString(4);
							String password = results.getString(5);

							FullAddress currAddress = new FullAddress(new Address(host, port), vhost, username,
									password);
							connectionState.addresses.add(currAddress);
						}

					} finally {
						results.close();
					}

				} finally {
					statement.close();
				}

			} finally {
				conn.close();
			}

		} catch (SQLException sqle) {
			// FIXME make up a more user-friendly error
			sqle.printStackTrace();
		}
	}

	private static Connection openConnection(BrokerConnectionState connectionState) {
		boolean connected = false;
		Connection connection = null;

		if (connectionState.addresses != null) {
			// sort brokers so that the previously used one is first
			List<FullAddress> sortedBrokers = connectionState.getSortedBrokersToTry();
			Iterator<FullAddress> sortedBrokersIter = sortedBrokers.iterator();

			while (!connected && sortedBrokersIter.hasNext()) {
				FullAddress currFullAddress = (FullAddress) sortedBrokersIter.next();

				if (ENABLE_DEBUG) {
					System.err.println("trying to connect to " + currFullAddress);
				}

				// try to open the connection
				try {
					connection = openConnection(currFullAddress);

					// connection established
					connected = true;
					connectionState.currentAddress = currFullAddress;

					if (ENABLE_DEBUG) {
						System.err.println("connected to " + currFullAddress);
					}

				} catch (IOException ioe) {
					// we catch SocketTimeoutException
					if (ENABLE_DEBUG) {
						System.err.println("cannot connect to " + currFullAddress + " (" + ioe.getMessage() + ')');
					}
				}
			}
		}

		if (!connected) {
			if (ENABLE_DEBUG) {
				System.err.println("giving up, no broker is responding");
			}
		}

		return connection;
	}

	private static Connection openConnection(FullAddress address) throws IOException {
		Connection connection = null;

		if (ENABLE_DEBUG) {
			System.err.println("trying to connect to " + address);
		}

		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost(address.address.getHost());
		connectionFactory.setPort(address.address.getPort());
		connectionFactory.setUsername(address.username);
		connectionFactory.setPassword(address.password);
		connectionFactory.setVirtualHost(address.vhost);

		connectionFactory.setConnectionTimeout(CONNECTION_OPEN_TIMEOUT);

		// try to open the connection
		connection = connectionFactory.newConnection();

		if (ENABLE_DEBUG) {
			System.err.println("connected to " + address);
		}

		return connection;
	}

	private static class BrokerConnectionState {
		public List<FullAddress> addresses;
		public FullAddress currentAddress;

		public BrokerConnectionState() {
			this.addresses = new ArrayList<FullAddress>();
		}

		@SuppressWarnings("unused")
		public boolean isCurrentAddressStillValid() {
			return this.addresses.contains(this.currentAddress);
		}

		public List<FullAddress> getSortedBrokersToTry() {
			int currentOffset = this.addresses.indexOf(this.currentAddress);
			if (currentOffset != -1) {
				// rotate so that order is retained, and previously used broker is first
				Collections.rotate(this.addresses, -currentOffset);
			}

			return this.addresses;
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((address == null) ? 0 : address.hashCode());
			result = prime * result + ((password == null) ? 0 : password.hashCode());
			result = prime * result + ((username == null) ? 0 : username.hashCode());
			result = prime * result + ((vhost == null) ? 0 : vhost.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FullAddress other = (FullAddress) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			if (password == null) {
				if (other.password != null)
					return false;
			} else if (!password.equals(other.password))
				return false;
			if (username == null) {
				if (other.username != null)
					return false;
			} else if (!username.equals(other.username))
				return false;
			if (vhost == null) {
				if (other.vhost != null)
					return false;
			} else if (!vhost.equals(other.vhost))
				return false;
			return true;
		}

	}

}
