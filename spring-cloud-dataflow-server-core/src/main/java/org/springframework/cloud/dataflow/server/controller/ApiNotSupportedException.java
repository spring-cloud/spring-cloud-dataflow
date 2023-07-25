package org.springframework.cloud.dataflow.server.controller;

public class ApiNotSupportedException extends RuntimeException {

	public ApiNotSupportedException(String message) {
		super(message);
	}
}
