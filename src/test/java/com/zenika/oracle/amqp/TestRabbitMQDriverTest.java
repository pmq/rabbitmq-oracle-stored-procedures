package com.zenika.oracle.amqp;

import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 * @author Pierre Queinnec <pierre.queinnec@zenika.com>
 */
@Ignore
public class TestRabbitMQDriverTest {

	@Test
	public void testAmqpPublishIntStringStringString() {
		TestRabbitMQDriver.amqpPublish(1, "oracle", "test.unit", "Hello World!");
	}

}
