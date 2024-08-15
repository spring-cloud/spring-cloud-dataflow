package org.springframework.cloud.dataflow.server.db.support;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.jdbc.support.MetaDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@JdbcTest(properties = "spring.jpa.hibernate.ddl-auto=none")
abstract class SingleDbDatabaseTypeTests {

	@Test
	void shouldSupportRowNumberFunction(@Autowired DataSource dataSource) throws MetaDataAccessException {
		assertThat(DatabaseType.supportsRowNumberFunction(dataSource)).isEqualTo(supportsRowNumberFunction());
	}

	protected boolean supportsRowNumberFunction() {
		return true;
	}

	@SpringBootConfiguration
	static class FakeApp {

	}

}
