-- print System.{out|err} if debugging
SET SERVEROUTPUT ON
CALL dbms_java.set_output(5000);

-- define the broker instances
insert into broker (BROKER_ID,HOST,PORT,VHOST,USERNAME,PASSWORD) values (1,'192.168.56.1',5671,'/','guest','guest');
insert into broker (BROKER_ID,HOST,PORT,VHOST,USERNAME,PASSWORD) values (1,'192.168.56.1',5672,'/','guest','guest');
insert into broker (BROKER_ID,HOST,PORT,VHOST,USERNAME,PASSWORD) values (1,'192.168.56.1',5673,'/','guest','guest');

-- tests
select amqp_exchange_declare(1, 'oracle', 'fanout') from dual;
select amqp_publish(1, 'oracle', 'key', 'Hello World!') from dual;
select amqp_publish(1, 'oracle', 'key', 'Hello World!'
	,'<properities>
	      <DELIVERYMODE>2</DELIVERYMODE>
	      <CONTENTTYPE>application/vnd.masstransit+json</CONTENTTYPE>
	      <MESSAGEID>'||regexp_replace(rawtohex(sys_guid()),'([A-F0-9]{8})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{4})([A-F0-9]{12})', '\1-\2-\3-\4-\5') ||'</MESSAGEID>
		  <HEADEARS>
				<content_type>application/vnd.masstransit+json</content_type>
				<head1>test</head1>
		  </HEADEARS>
		  <CONTENTTYPE>XML</CONTENTTYPE>
		  <PRIORITY>10</PRIORITY>
		  <CONTENTENCODING>iso-8859-5</CONTENTENCODING>		  
	  </properities>') from dual;
select amqp_publish(brokerid     => 1,
					exchange     => 'oracle',
					routingkey   => 'key',
					message    	 => 'Hello World!',
					xml_string_properties => '<properities></properities>') from dual;
call amqp_print_configuration(1);
call amqp_probe_servers(1);
