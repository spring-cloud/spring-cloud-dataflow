package org.springframework.cloud.dataflow.core.database.support;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

public class MultiSchemaIncrementerFactory extends DefaultDataFieldMaxValueIncrementerFactory {
	private final static Logger logger = LoggerFactory.getLogger(MultiSchemaIncrementerFactory.class);

	private final DataSource dataSource;

	public MultiSchemaIncrementerFactory(DataSource dataSource) {
		super(dataSource);
		this.dataSource = dataSource;
	}

	@Override
	public DataFieldMaxValueIncrementer getIncrementer(String incrementerType, String incrementerName) {
		DatabaseType databaseType;
		try {
			databaseType = DatabaseType.fromMetaData(this.dataSource);
		} catch (MetaDataAccessException e) {
			throw new IllegalStateException(e);
		}
		if (databaseType != null) {
			IncrementerType type = getIncrementerType(databaseType, incrementerName);
			if (type == IncrementerType.SEQUENCE) {
				switch (databaseType) {
					case SQLSERVER:
						return new SqlServerSequenceMaxValueIncrementer(this.dataSource, incrementerName);
					case MARIADB:
						return new MariaDBSequenceMaxValueIncrementer(this.dataSource, incrementerName);
				}
			}
		}
		return super.getIncrementer(incrementerType, incrementerName);
	}

	private IncrementerType getIncrementerType(DatabaseType databaseType, String incrementerName) {

		try (Connection connection = this.dataSource.getConnection()) {
			if(databaseType == DatabaseType.SQLSERVER) {
				try(Statement statement = connection.createStatement()) {
					try(ResultSet sequences = statement.executeQuery("SELECT name FROM sys.sequences")) {
						while (sequences.next()) {
							String sequenceName = sequences.getString(1);
							logger.debug("Sequence:{}", sequenceName);
							if(sequenceName.equalsIgnoreCase(incrementerName)) {
								return IncrementerType.SEQUENCE;
							}
						}
					}
				} catch (Throwable x) {
					logger.warn("Ignoring error:" + x);
				}
			}
			DatabaseMetaData metaData = connection.getMetaData();
			String[] types = {"TABLE", "SEQUENCE"};
			try (ResultSet tables = metaData.getTables(null, null, "%", types)) {
				while (tables.next()) {
					String tableName = tables.getString("TABLE_NAME");
					if (tableName.equalsIgnoreCase(incrementerName)) {
						String tableType = tables.getString("TABLE_TYPE");
						logger.debug("Found Table:{}:{}", incrementerName, tableType);
						if (tableType != null && tableType.toUpperCase(Locale.ROOT).contains("SEQUENCE")) {
							return IncrementerType.SEQUENCE;
						}
						return IncrementerType.TABLE;
					}
				}
			}
		} catch (SQLException sqe) {
			logger.warn(sqe.getMessage(), sqe);
		}
		return IncrementerType.DEFAULT;
	}
}
