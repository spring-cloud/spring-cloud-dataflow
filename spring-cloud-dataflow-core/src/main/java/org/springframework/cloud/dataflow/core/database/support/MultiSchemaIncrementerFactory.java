package org.springframework.cloud.dataflow.core.database.support;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

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
			IncrementerType type = getIncrementerType(incrementerName);
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

	private IncrementerType getIncrementerType(String incrementerName) {

		try (Connection connection = this.dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			String[] types = {"TABLE", "SEQUENCE"};
			ResultSet tables = metaData.getTables(null, null, "%", types);
//			int count = tables.getMetaData().getColumnCount();
//			for (int i = 1; i <= count; i++) {
//				logger.debug("Column:{}:{}", tables.getMetaData().getColumnName(i), tables.getMetaData().getColumnTypeName(i));
//			}
			while (tables.next()) {

				if (tables.getString("TABLE_NAME").equals(incrementerName)) {
					String tableType = tables.getString("TABLE_TYPE");
					logger.debug("Found Table:{}:{}", incrementerName, tableType);
					if (tableType != null && tableType.toUpperCase().contains("SEQUENCE")) {
						return IncrementerType.SEQUENCE;
					}
					return IncrementerType.TABLE;
				}
			}
		} catch (SQLException sqe) {
			logger.warn(sqe.getMessage(), sqe);
		}
		return IncrementerType.DEFAULT;
	}
}
