package org.springframework.cloud.dataflow.common.persistence.type;

import java.util.function.BiConsumer;

import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.BaseUserTypeSupport;
import org.hibernate.usertype.UserType;

import org.springframework.util.Assert;

/**
 * A {@link UserType} that provides for Hibernate and Postgres incompatibility for columns of
 * type text.
 *
 * @author Corneil du Plessis
 * @author Chris Bono
 * @since 3.0.0
 */
public class DatabaseAwareLobUserType extends BaseUserTypeSupport<String> {

	@Override
	protected void resolve(BiConsumer<BasicJavaType<String>, JdbcType> resolutionConsumer) {
		resolutionConsumer.accept(StringJavaType.INSTANCE, getDbDescriptor());
	}

	public static AdjustableJdbcType getDbDescriptor() {
		if( isPostgres() ) {
			return VarcharJdbcType.INSTANCE;
		}
		else {
			return ClobJdbcType.DEFAULT;
		}
	}

	private static boolean isPostgres() {
		Boolean postgresDatabase = DatabaseTypeAwareInitializer.getPostgresDatabase();
		Assert.notNull(postgresDatabase, "Expected postgresDatabase to be set");
		return postgresDatabase;
	}
}
