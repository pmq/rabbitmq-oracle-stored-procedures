-- print System.{out|err}
SET SERVEROUTPUT ON
CALL dbms_java.set_output(5000);

-- tests
call amqp_exchange_declare(1, 'oracle', 'fanout');
call amqp_publish(1, 'oracle', 'key', 'Hello World!');
call amqp_print_configuration(1);
call amqp_probe_servers(1);
