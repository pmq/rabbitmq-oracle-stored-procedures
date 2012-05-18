-- !! CHANGE 'HR.' TO YOUR USER IN THE FOLLOWING BLOCK
BEGIN
  DBMS_SCHEDULER.create_program (
    program_name   => 'publish_amqp',
    program_type   => 'PLSQL_BLOCK',
    program_action => 'DECLARE result number; BEGIN result := HR.amqp_publish(1, ''oracle'', ''key'', ''Hello World!''); END;',
    enabled        => TRUE,
    comments       => 'Publish an Hello World message to RabbitMQ.');
END;
/

--BEGIN
--  DBMS_SCHEDULER.drop_program (program_name => 'publish_amqp');
--END;
--/

--SELECT owner, program_name, enabled FROM dba_scheduler_programs;

BEGIN
  DBMS_SCHEDULER.create_schedule (
    schedule_name   => 'every_5_seconds_schedule',
    start_date      => SYSTIMESTAMP,
    repeat_interval => 'freq=secondly; interval=5',
    end_date        => NULL,
    comments        => 'Repeats every five seconds, forever.');
END;
/

--BEGIN
--  DBMS_SCHEDULER.drop_schedule (schedule_name => 'every_5_seconds_schedule');
--END;
--/

--SELECT owner, schedule_name FROM dba_scheduler_schedules;

BEGIN
  DBMS_SCHEDULER.create_job (
    job_name      => 'test_amqp',
    program_name  => 'publish_amqp',
    schedule_name => 'every_5_seconds_schedule',
    enabled       => TRUE,
    comments      => 'Publish a test AMQP message every 5 seconds.');
END;
/

--BEGIN
--  DBMS_SCHEDULER.drop_job (job_name => 'test_amqp');
--END;
--/

--SELECT owner, job_name, enabled FROM dba_scheduler_jobs;

-- tests
--BEGIN
--  -- synchronously run job to debug
--  DBMS_SCHEDULER.run_job (job_name => 'test_amqp', use_current_session => TRUE);
--
--  -- stop job
--  DBMS_SCHEDULER.stop_job (job_name => 'test_amqp');
--END;
--/

--BEGIN
--  -- enable job
--  DBMS_SCHEDULER.enable (name => 'test_amqp');
--
--  -- disable job
--  DBMS_SCHEDULER.disable (name => 'test_amqp');
--END;
--/
