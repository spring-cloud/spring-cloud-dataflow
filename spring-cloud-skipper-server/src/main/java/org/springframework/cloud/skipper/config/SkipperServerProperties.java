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
package org.springframework.cloud.skipper.config;

import java.io.File;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Mark Pollack
 */
@ConfigurationProperties("spring.cloud.skipper")
public class SkipperServerProperties {

	/*
	 * This can be extended to support an apt-like sources.list format, eg. deb
	 * ftp://ftp.debian.org/debian sarge main contrib non-free
	 *
	 * maps onto
	 *
	 * ftp://ftp.debian.org/debian/dists/sarge/main/binary-i386/Packages
	 * ftp://ftp.debian.org/debian/dists/sarge/contrib/binary-i386/Packages
	 * ftp://ftp.debian.org/debian/dists/sarge/non-free/binary-i386/Packages
	 *
	 */
	/**
	 * List of locations for package Repositories
	 */
	private List<String> packageRepositoryUrls;

	private String skipperHome = System.getProperty("user.home") + File.separator + ".skipper";

	/**
	 * Flag indicating to sync the local contents of the index directory with the database on
	 * startup.
	 */
	private boolean synchonizeIndexOnContextRefresh = true;

	public List<String> getPackageRepositoryUrls() {
		return packageRepositoryUrls;
	}

	public void setPackageRepositoryUrls(List<String> packageRepositoryUrls) {
		this.packageRepositoryUrls = packageRepositoryUrls;
	}

	public String getSkipperHome() {
		return skipperHome;
	}

	public void setSkipperHome(String skipperHome) {
		this.skipperHome = skipperHome;
	}

	public boolean isSynchonizeIndexOnContextRefresh() {
		return synchonizeIndexOnContextRefresh;
	}

	public void setSynchonizeIndexOnContextRefresh(boolean synchonizeIndexOnContextRefresh) {
		this.synchonizeIndexOnContextRefresh = synchonizeIndexOnContextRefresh;
	}

	public String getPackageIndexDir() {
		return skipperHome + File.separator + "indexes";
	}
}
