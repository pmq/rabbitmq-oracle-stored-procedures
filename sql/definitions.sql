-- FIXME use a schema named 'amqp'
create or replace function amqp_exchange_declare
(brokerId IN number, exchange IN varchar2, exchange_type IN varchar2)
return NUMBER
as language java
name 'com.zenika.oracle.amqp.RabbitMQPublisher.amqpExchangeDeclare(int, java.lang.String, java.lang.String) return int';

create or replace function amqp_publish
(brokerId IN number, exchange IN varchar2, routingKey IN varchar2, message IN varchar2)
return NUMBER
as language java
name 'com.zenika.oracle.amqp.RabbitMQPublisher.amqpPublish(int, java.lang.String, java.lang.String, java.lang.String) return int';

create or replace procedure amqp_print_configuration
(brokerId IN number)
as language java
name 'com.zenika.oracle.amqp.RabbitMQPublisher.amqpPrintFullConfiguration(int)';

create or replace procedure amqp_probe_servers
(brokerId IN number)
as language java
name 'com.zenika.oracle.amqp.RabbitMQPublisher.amqpProbeAllServers(int)';

CREATE TABLE BROKER
  (
    "BROKER_ID" NUMBER(*,0),
    "HOST"      VARCHAR2(255 BYTE) NOT NULL ENABLE,
    "PORT"      NUMBER(*,0) DEFAULT 5672,
    "VHOST"     VARCHAR2(255 BYTE) DEFAULT '/',
    "USERNAME"  VARCHAR2(255 BYTE) DEFAULT 'guest',
    "PASSWORD"  VARCHAR2(255 BYTE) DEFAULT 'guest',
    CONSTRAINT "BROKER_KEY" PRIMARY KEY ("BROKER_ID", "HOST", "PORT") ENABLE
  );
