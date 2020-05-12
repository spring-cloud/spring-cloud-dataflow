/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.service.LauncherService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class DefaultLauncherService implements LauncherService {

	private final LauncherRepository launcherRepository;

	public DefaultLauncherService(LauncherRepository launcherRepository) {
		this.launcherRepository = launcherRepository;
	}

	@Override
	public Page<Launcher> getAllLaunchers(Pageable pageable) {
		return this.launcherRepository.findAll(pageable);
	}

	@Override
	public Page<Launcher> getLaunchersWithSchedules(Pageable pageable) {
		Iterable<Launcher> allLaunchers = this.launcherRepository.findAll();
		List<Launcher> launchers = new ArrayList<>();
		Iterator<Launcher> launcherIterator = allLaunchers.iterator();
		while(launcherIterator.hasNext()) {
			Launcher launcher = launcherIterator.next();
			if (launcher.getScheduler() != null) {
				launchers.add(launcher);
			}
		}
		return new PageImpl<>(launchers, pageable, launchers.size()) ;
	}
}
