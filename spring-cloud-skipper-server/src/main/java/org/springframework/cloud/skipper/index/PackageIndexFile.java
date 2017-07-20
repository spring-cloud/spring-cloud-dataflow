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
package org.springframework.cloud.skipper.index;

import java.util.List;

/**
 * This describes the packages that a repository offers for installation.
 *
 * It is a YAML file that starts with a header that describes the metadata format, which
 * is followed by metadata in the form of one YAML document per App Stream component.
 *
 * @author Mark Pollack
 */
public class PackageIndexFile {

	private List<PackageSummary> packageSummaryList;

	public PackageIndexFile() {
	}

	public List<PackageSummary> getPackageSummaryList() {
		return packageSummaryList;
	}

	public void setPackageSummaryList(List<PackageSummary> packageSummaryList) {
		this.packageSummaryList = packageSummaryList;
	}
}
