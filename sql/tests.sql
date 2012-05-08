-- print System.{out|err} if debugging
SET SERVEROUTPUT ON
CALL dbms_java.set_output(5000);

-- define the broker instances
insert into broker (BROKER_ID,HOST,PORT,VHOST,USERNAME,PASSWORD) values (1,'192.168.56.1',5671,'/','guest','guest');
insert into broker (BROKER_ID,HOST,PORT,VHOST,USERNAME,PASSWORD) values (1,'192.168.56.1',5672,'/','guest','guest');
insert into broker (BROKER_ID,HOST,PORT,VHOST,USERNAME,PASSWORD) values (1,'192.168.56.1',5673,'/','guest','guest');

-- tests
call amqp_exchange_declare(1, 'oracle', 'fanout');
call amqp_publish(1, 'oracle', 'key', 'Hello World!');
call amqp_print_configuration(1);
call amqp_probe_servers(1);
