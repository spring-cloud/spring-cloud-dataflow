/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.domain;

/**
 * The identifier that uniquely identifies a package.
 *
 * @author Mark Pollack
 */
public class PackageIdentifier {

	private String repositoryName;

	private String packageName;

	private String packageVersion;

	public PackageIdentifier() {
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPackageVersion() {
		return packageVersion;
	}

	public void setPackageVersion(String packageVersion) {
		this.packageVersion = packageVersion;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("PackageIdentifier{");
		sb.append("repositoryName='").append(repositoryName).append('\'');
		sb.append(", packageName='").append(packageName).append('\'');
		sb.append(", packageVersion='").append(packageVersion).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
