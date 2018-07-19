CREATE INDEX IF NOT EXISTS batch_job_execution_params_job_execution_id_idx ON batch_job_execution_params (job_execution_id) ;
CREATE INDEX IF NOT EXISTS task_execution_params_task_execution_id_idx ON task_execution_params (task_execution_id) ;
CREATE INDEX IF NOT EXISTS batch_job_instance_job_key_idx ON batch_job_instance (job_key,job_name) ;
CREATE INDEX IF NOT EXISTS batch_step_execution_job_execution_id_idx ON batch_step_execution (job_execution_id) ;
