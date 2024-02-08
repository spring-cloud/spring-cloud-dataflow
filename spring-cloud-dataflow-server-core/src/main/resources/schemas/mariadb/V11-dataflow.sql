/**
 * Remove aggregate views
 */

DROP VIEW AGGREGATE_TASK_EXECUTION;
DROP VIEW AGGREGATE_TASK_EXECUTION_PARAMS;
DROP VIEW AGGREGATE_JOB_EXECUTION;
DROP VIEW AGGREGATE_JOB_INSTANCE;
DROP VIEW AGGREGATE_TASK_BATCH;
DROP VIEW AGGREGATE_STEP_EXECUTION;

/*
 * Prefix Task V2 and Batch V4 tables with V2_ prefix.
 * Allow user to determine what they should do with these tables.
 */
ALTER TABLE TASK_EXECUTION RENAME TO V2_TASK_EXECUTION;
ALTER TABLE TASK_EXECUTION_PARAMS RENAME TO V2_TASK_EXECUTION_PARAMS;
ALTER TABLE TASK_TASK_BATCH RENAME TO V2_TASK_TASK_BATCH;
ALTER TABLE TASK_LOCK RENAME TO V2_TASK_LOCK;
ALTER TABLE TASK_SEQ RENAME TO V2_TASK_SEQ;
ALTER TABLE TASK_EXECUTION_METADATA RENAME TO V2_TASK_EXECUTION_METADATA;
ALTER TABLE TASK_EXECUTION_METADATA_SEQ RENAME TO V2_TASK_EXECUTION_METADATA_SEQ;
ALTER TABLE BATCH_JOB_INSTANCE RENAME TO V2_BATCH_JOB_INSTANCE;
ALTER TABLE BATCH_JOB_EXECUTION RENAME TO V2_BATCH_JOB_EXECUTION;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS RENAME TO V2_BATCH_JOB_EXECUTION_PARAMS;
ALTER TABLE BATCH_STEP_EXECUTION RENAME TO V2_BATCH_STEP_EXECUTION;
ALTER TABLE BATCH_STEP_EXECUTION_CONTEXT RENAME TO V2_BATCH_STEP_EXECUTION_CONTEXT;
ALTER TABLE BATCH_JOB_EXECUTION_CONTEXT RENAME TO V2_BATCH_JOB_EXECUTION_CONTEXT;
ALTER TABLE BATCH_STEP_EXECUTION_SEQ RENAME TO V2_BATCH_STEP_EXECUTION_SEQ;
ALTER TABLE BATCH_JOB_EXECUTION_SEQ RENAME TO V2_BATCH_JOB_EXECUTION_SEQ;
ALTER TABLE BATCH_JOB_SEQ RENAME TO V2_BATCH_JOB_SEQ;


/*
 * Remove BOOT3_ prefix Task V3 and Batch V5 tables .
 */

ALTER TABLE BOOT3_TASK_EXECUTION RENAME TO TASK_EXECUTION;
ALTER TABLE BOOT3_TASK_EXECUTION_PARAMS RENAME TO TASK_EXECUTION_PARAMS;
ALTER TABLE BOOT3_TASK_TASK_BATCH RENAME TO TASK_TASK_BATCH;
ALTER TABLE BOOT3_TASK_LOCK RENAME TO TASK_LOCK;
ALTER TABLE BOOT3_TASK_SEQ RENAME TO TASK_SEQ;
ALTER TABLE BOOT3_TASK_EXECUTION_METADATA RENAME TO TASK_EXECUTION_METADATA;
ALTER TABLE BOOT3_TASK_EXECUTION_METADATA_SEQ RENAME TO TASK_EXECUTION_METADATA_SEQ;
ALTER TABLE BOOT3_BATCH_JOB_INSTANCE RENAME TO BATCH_JOB_INSTANCE;
ALTER TABLE BOOT3_BATCH_JOB_EXECUTION RENAME TO BATCH_JOB_EXECUTION;
ALTER TABLE BOOT3_BATCH_JOB_EXECUTION_PARAMS RENAME TO BATCH_JOB_EXECUTION_PARAMS;
ALTER TABLE BOOT3_BATCH_STEP_EXECUTION RENAME TO BATCH_STEP_EXECUTION;
ALTER TABLE BOOT3_BATCH_STEP_EXECUTION_CONTEXT RENAME TO BATCH_STEP_EXECUTION_CONTEXT;
ALTER TABLE BOOT3_BATCH_JOB_EXECUTION_CONTEXT RENAME TO BATCH_JOB_EXECUTION_CONTEXT;
ALTER TABLE BOOT3_BATCH_STEP_EXECUTION_SEQ RENAME TO BATCH_STEP_EXECUTION_SEQ;
ALTER TABLE BOOT3_BATCH_JOB_EXECUTION_SEQ RENAME TO BATCH_JOB_EXECUTION_SEQ;
ALTER TABLE BOOT3_BATCH_JOB_SEQ RENAME TO BATCH_JOB_SEQ;

