package org.springframework.cloud.dataflow.core.database.support;

import javax.sql.DataSource;

import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.DatabaseType;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.jdbc.support.MetaDataAccessException;

public class MultiSchemaTaskExecutionDaoFactoryBean extends TaskExecutionDaoFactoryBean {
	private final DataSource dataSource;
	private final String tablePrefix;
	public MultiSchemaTaskExecutionDaoFactoryBean(DataSource dataSource, String tablePrefix) {
		super(dataSource, tablePrefix);
		this.dataSource = dataSource;
		this.tablePrefix = tablePrefix;
	}

	@Override
	public TaskExecutionDao getObject() throws Exception {
		DataFieldMaxValueIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(dataSource, this.tablePrefix);
		String databaseType;
		try {
			databaseType = DatabaseType.fromMetaData(dataSource).name();
		}
		catch (MetaDataAccessException e) {
			throw new IllegalStateException(e);
		}
		dao.setTaskIncrementer(incrementerFactory.getIncrementer(databaseType, this.tablePrefix + "SEQ"));
		return dao;
	}
}
