create or replace procedure AmqpToyStressTest
AS
	result number;
BEGIN
	WHILE( true )
	LOOP
		result := HR.amqp_publish(1, 'oracle', 'stress-test', 'RabbitMQ rocks!');
		-- dbms_lock.sleep( 0.1 );
	END LOOP;
END AmqpToyStressTest;

-- run it with:
-- call AmqpToyStressTest();
