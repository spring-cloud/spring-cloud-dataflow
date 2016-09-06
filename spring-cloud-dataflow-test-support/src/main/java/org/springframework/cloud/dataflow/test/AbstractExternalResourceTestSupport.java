/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.test;

import static org.junit.Assert.fail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.util.Assert;

/**
 * Abstract base class for JUnit {@link Rule}s that detect the presence of some external
 * resource. If the resource is indeed present, it will be available during the test
 * lifecycle through {@link #getResource()}. If it is not, tests will either fail or be
 * skipped, depending on the value of system property
 * {@value #SCS_EXTERNAL_SERVERS_REQUIRED}.
 *
 * @author Eric Bottard
 * @author Gary Russell
 */
public abstract class AbstractExternalResourceTestSupport<R> implements TestRule {

	public static final String SCS_EXTERNAL_SERVERS_REQUIRED = "SCS_EXTERNAL_SERVERS_REQUIRED";

	protected R resource;

	private String resourceDescription;

	protected final Log logger = LogFactory.getLog(getClass());

	protected AbstractExternalResourceTestSupport(String resourceDescription) {
		Assert.hasText(resourceDescription, "resourceDescription is required");
		this.resourceDescription = resourceDescription;
	}

	@Override
	public Statement apply(final Statement base, Description description) {
		try {
			obtainResource();
		}
		catch (Exception e) {
			maybeCleanup();

			return failOrSkip(e);
		}

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				try {
					base.evaluate();
				}
				finally {
					try {
						cleanupResource();
					}
					catch (Exception ignored) {
						logger.warn("Exception while trying to cleanup proper resource",
								ignored);
					}
				}
			}

		};
	}

	private Statement failOrSkip(final Exception e) {
		String serversRequired = System.getenv(SCS_EXTERNAL_SERVERS_REQUIRED);
		if ("true".equalsIgnoreCase(serversRequired)) {
			logger.error(resourceDescription + " IS REQUIRED BUT NOT AVAILABLE", e);
			fail(resourceDescription + " IS NOT AVAILABLE");
			// Never reached, here to satisfy method signature
			return null;
		}
		else {
			logger.error(resourceDescription + " IS NOT AVAILABLE, SKIPPING TESTS", e);
			return new Statement() {

				@Override
				public void evaluate() throws Throwable {
					Assume.assumeTrue("Skipping test due to " + resourceDescription
							+ " not being available " + e, false);
				}
			};
		}
	}

	private void maybeCleanup() {
		if (resource != null) {
			try {
				cleanupResource();
			}
			catch (Exception ignored) {
				logger.warn("Exception while trying to cleanup failed resource", ignored);
			}
		}
	}

	public R getResource() {
		return resource;
	}

	/**
	 * Perform cleanup of the {@link #resource} field, which is guaranteed to be non null.
	 *
	 * @throws Exception any exception thrown by this method will be logged and swallowed
	 */
	protected abstract void cleanupResource() throws Exception;

	/**
	 * Try to obtain and validate a resource. Implementors should either set the
	 * {@link #resource} field with a valid resource and return normally, or throw an
	 * exception.
	 */
	protected abstract void obtainResource() throws Exception;

}
