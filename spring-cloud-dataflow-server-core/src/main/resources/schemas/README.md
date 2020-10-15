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

    V3 dataflow schema.
    
6. V4-dataflow.sql

    V4 dataflow schema.

To completely disable flyway use Boot property:

    spring.flyway.enabled=false

## Bootstrap Server with DDL Files
Spring Boot already have a feature to execute arbitrary sql files for schema generation
and data inserting. As ddl schema files are bundled with dataflow server it's possible
to tell server to execute these files during a startup.

We've also bundled additional Boot application profile configurations which will define
these files, disable flyway and set datasource initialization mode.

For example if there is an existing PostgreSQL server running locally, starting a server
with below command would automatically create needed schemas:

    java -jar spring-cloud-dataflow-server.jar \
      --spring.datasource.url='jdbc:postgresql://localhost:5432/dataflow' \
      --spring.datasource.username=spring \
      --spring.datasource.password=spring \
      --spring.datasource.driverClassName=org.postgresql.Driver \
      --spring.profiles.active=init-postgresql

Available profiles are `init-postgresql`, `init-mysql`, `init-sqlserver`,
`init-db2` and `init-oracle`.
