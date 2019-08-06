    create table AUDIT_RECORDS (
       id bigint not null,
        audit_Action bigint,
        audit_data varchar(MAX),
        audit_Operation bigint,
        correlation_id varchar(255),
        created_by varchar(255),
        created_On datetime2,
        primary key (id)
    );
