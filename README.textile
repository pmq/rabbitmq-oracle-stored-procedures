h2. WTF is this?

A set of stored procedures for publishing AMQP messages to a RabbitMQ broker from an Oracle DB with Java installed.

h2. No seriously... WTF is this?

If you come from a PostgreSQL background, think pg_amqp for Oracle.
If you don't, then this is a set of Oracle functions and procedures that enables you to publish AMQP 0-9-1 messages to a RabbitMQ broker, directly from PL/SQL.

h2. You sold me. How do I install this?

In short:
# if you want to test right away and can login using the HR account, you're all set
# if not, edit the load-into-oracle.sh and change the USER_PASS environment variable to your user/password and the SCHEMA one to your schema 
# load the classes into the Oracle JVM by running the load-into-oracle.sh script
# execute the definitions.sql file with your user in the correct schema
# that's it!

Now you can test the system to see if everything works:
# read tests.sql and change the settings in the BROKER table to your RabbitMQ broker connection info (IP, port, user, password, virtual host)
# read system.sql, change the RabbitMQ broker connection info (IP, port) to the same info as previous line, and get your DBA to execute that for you
# try to publish a test message - see the sample in tests.sql
# try to declare an exchange if you need that - see the sample in tests.sql

If you need to troubleshoot:
# the key is getting access to stdout and stderr - read the top lines in tests.sql to achieve that
# the most common problem is security - read system.sql
# the second most common problem is IPv6, in the latest Oracle versions a newer Java is included and it defaults to IPv6 when connecting to RabbitMQ - the code takes care at initialization of switching to IPv4, but you'll need the permission from your DBA to set this particular JVM system property, so as previously said, you'll need to get her to execute system.sql

h2. Bonus! Running Spring-AMQP in the Oracle DB

This is really something that shouldn't be in this repo, but I left it in case someone more clever that me finds a use for this. Read the (very raw) load-spring-amqp-into-oracle.sh script if for a reason you want to do this. If you do, I'd be glad if you dropped me a mail about your use case!
