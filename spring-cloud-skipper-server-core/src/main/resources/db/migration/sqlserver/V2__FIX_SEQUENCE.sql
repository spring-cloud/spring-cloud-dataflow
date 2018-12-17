-- stash old hibernate_sequence table
exec sp_rename 'hibernate_sequence', 'hibernate_sequence_old';  

-- create new sequence with value from hibernate_sequence_old
declare @max int;
select @max = max(next_val) from hibernate_sequence_old;
exec('create sequence hibernate_sequence start with ' + @max + ' increment by 1;');

-- drop old table
drop table hibernate_sequence_old;
