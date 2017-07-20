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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.repositories.PackageSummaryRepository;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Synchronizes the contents of local package index files with contents in the database.
 *
 * @author Mark Pollack
 */
@Component
public class PackageIndexSynchronizer {

	private PackageIndexDownloader packageIndexDownloader;

	private PackageSummaryRepository packageSummaryRepository;

	private SkipperServerProperties skipperServerProperties;

	@Autowired
	public PackageIndexSynchronizer(PackageIndexDownloader packageIndexDownloader,
			PackageSummaryRepository packageSummaryRepository,
			SkipperServerProperties skipperServerProperties) {
		this.packageIndexDownloader = packageIndexDownloader;
		this.packageSummaryRepository = packageSummaryRepository;
		this.skipperServerProperties = skipperServerProperties;
	}

	public void loadAll() {
		packageIndexDownloader.downloadPackageIndexes();
		List<File> indexFiles = packageIndexDownloader.getIndexFiles();
		List<PackageSummary> packageSummaryList = deserializeFromIndexFiles(indexFiles);
		packageSummaryRepository.save(packageSummaryList);
	}

	@EventListener
	public void loadAllOnApplicationStartup(ContextRefreshedEvent contextRefreshedEvent) {
		if (this.skipperServerProperties.isSynchonizeIndexOnContextRefresh()) {
			loadAll();
		}

	}

	// Package protected for testing
	List<PackageSummary> deserializeFromIndexFiles(List<File> indexFiles) {
		List<PackageSummary> packageSummaryList = new ArrayList<>();
		YAMLMapper yamlMapper = new YAMLMapper();
		for (File indexFile : indexFiles) {
			try {
				MappingIterator<PackageSummary> it = yamlMapper.readerFor(PackageSummary.class).readValues(indexFile);
				while (it.hasNextValue()) {
					PackageSummary packageSummary = it.next();
					packageSummaryList.add(packageSummary);
				}
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Can't parse Release manifest YAML", e);
			}
		}
		return packageSummaryList;
	}
}
