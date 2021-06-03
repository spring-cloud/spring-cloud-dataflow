-- old table should be dropped if exists
-- drop table TASK_SEQ;
-- Set start with a value of 1 greater than current value in TASK_SEQ table, if TASK_SEQ table has value greater than 1
create sequence TASK_SEQ start with 1 increment by 1;
