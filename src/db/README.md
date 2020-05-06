# Database DDL Files
This is a collection of needed sql files to create a database schema order
to run servers without flyway activate schema management.

Generic order to apply ddl files is:

1. V1-dataflow.sql

    Initial dataflow schema.

2. V1-skipper.sql

    Initial skipper schema.

3. V2-dataflow.sql

    V2 dataflow schema.

4. V2-dataflow-after.sql

    Needed if you are upgrading from V1 with existing stream definitions.

5. V3-dataflow.sql

    V3 dataflow schema

To completely disable flyway use Boot property:

    spring.flyway.enabled=false
