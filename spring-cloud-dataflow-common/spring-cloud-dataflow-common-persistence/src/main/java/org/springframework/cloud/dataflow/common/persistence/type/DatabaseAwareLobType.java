/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.common.persistence.type;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.springframework.util.Assert;

/**
 * Provide for Hibernate and Postgres incompatibility for columns of type text.
 * @author Corneil du Plessis
 */
public class DatabaseAwareLobType extends AbstractSingleColumnStandardBasicType<String> {

	public static final DatabaseAwareLobType INSTANCE = new DatabaseAwareLobType();



	public DatabaseAwareLobType() {
		super( getDbDescriptor(), StringJavaType.INSTANCE );
	}

	public static AdjustableJdbcType getDbDescriptor() {
		if( isPostgres() ) {
			return VarcharJdbcType.INSTANCE;
		}
		else {
			return ClobJdbcType.DEFAULT;
		}
	}

	/**
	 * This method will be used to set an indicator that the database driver in use is PostgreSQL.
	 * if postgresDB true if PostgreSQL.
	 */
	private static boolean isPostgres() {
		Boolean postgresDatabase = DatabaseTypeAwareInitializer.getPostgresDatabase();
		Assert.notNull(postgresDatabase, "Expected postgresDatabase to be set");
		return postgresDatabase;
	}

	@Override
	public String getName() {
		return "database_aware_lob";
	}
}