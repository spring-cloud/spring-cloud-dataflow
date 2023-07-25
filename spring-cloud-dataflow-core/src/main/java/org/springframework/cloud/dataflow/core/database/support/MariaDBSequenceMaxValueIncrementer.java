package org.springframework.cloud.dataflow.core.database.support;

import javax.sql.DataSource;

import org.springframework.jdbc.support.incrementer.AbstractSequenceMaxValueIncrementer;

public class MariaDBSequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {
	public MariaDBSequenceMaxValueIncrementer() {
	}

	public MariaDBSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}

	protected String getSequenceQuery() {
		return "select next value for " + this.getIncrementerName();
	}
}
