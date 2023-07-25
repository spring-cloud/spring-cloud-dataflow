EXEC sp_rename 'task_execution_metadata', 'task_execution_metadata_lc';
EXEC sp_rename 'task_execution_metadata_lc', 'TASK_EXECUTION_METADATA';
EXEC sp_rename 'task_execution_metadata_seq', 'task_execution_metadata_seq_lc';
EXEC sp_rename 'task_execution_metadata_seq_lc', 'TASK_EXECUTION_METADATA_SEQ';