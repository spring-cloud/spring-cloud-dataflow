package org.springframework.cloud.dataflow.server.repository.support;

import org.springframework.util.StringUtils;

public class SchemaUtilities {
	private SchemaUtilities() {
	}

	public static String getQuery(String query, String prefix, String defaultPrefix) {
		return StringUtils.replace(query, "%PREFIX%", prefix != null ? prefix : defaultPrefix);
	}
	public static String getQuery(String query, String prefix) {
		return StringUtils.replace(query, "%PREFIX%", prefix);
	}
}
