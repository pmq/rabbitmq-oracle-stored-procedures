-- FIXME use a schema named 'amqp'
create or replace procedure amqp_exchange_declare
(brokerId IN number, exchange IN varchar2, exchange_type IN varchar2)
as language java
name 'com.zenika.oracle.amqp.RabbitMQPublisher.amqpExchangeDeclare(int, java.lang.String, java.lang.String)';

create or replace procedure amqp_publish
(brokerId IN number, exchange IN varchar2, routingKey IN varchar2, message IN varchar2)
as language java
name 'com.zenika.oracle.amqp.RabbitMQPublisher.amqpPublish(int, java.lang.String, java.lang.String, java.lang.String)';

create or replace procedure amqp_print_configuration
(brokerId IN number)
as language java
name 'com.zenika.oracle.amqp.RabbitMQPublisher.amqpPrintFullConfiguration(int)';

create or replace procedure amqp_probe_servers
(brokerId IN number)
as language java
name 'com.zenika.oracle.amqp.RabbitMQPublisher.amqpProbeAllServers(int)';
