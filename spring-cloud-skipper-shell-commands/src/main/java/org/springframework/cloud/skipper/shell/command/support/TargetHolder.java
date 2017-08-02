/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command.support;

import org.springframework.cloud.skipper.client.SkipperClientProperties;
import org.springframework.util.Assert;

/**
 * A singleton object that can be passed around while changing the target instance.
 *
 * @author Mark Pollack
 */
public class TargetHolder {

	private Target target;

	public TargetHolder(Target target) {
		this.target = target;
	}

	/**
	 * Constructor.
	 */
	public TargetHolder() {
		target = new Target(SkipperClientProperties.DEFAULT_TARGET, null, null, false);
	}

	/**
	 * Return the {@link Target} which encapsulates not only the Target URI but also
	 * success/error messages + status.
	 *
	 */
	public Target getTarget() {
		return target;
	}

	/**
	 * Set the Skipper Server {@link Target}.
	 *
	 * @param target Must not be null.
	 */
	public void setTarget(Target target) {
		Assert.notNull(target, "The provided target must not be null.");
		this.target = target;
	}

}
