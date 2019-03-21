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

package org.springframework.cloud.dataflow.registry.service;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.AbstractAppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Convenience wrapper for the {@link } that operates on higher level
 * {@link DefaultAppRegistryService} objects and supports on-demand loading of
 * {@link Resource}s.
 * <p>
 * <p>
 * Stores AppRegistration with up to two keys:
 * </p>
 * <ul>
 * <li>{@literal <type>.<name>}: URI for the actual app</li>
 * <li>{@literal <type>.<name>.metadata}: Optional URI for the app metadata</li>
 * </ul>
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Thomas Risberg
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Oleg Zhurakousky
 * @author Christian Tzolov
 */
@Transactional
public class DefaultAppRegistryService extends AbstractAppRegistryCommon implements AppRegistryService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAppRegistryService.class);

	private final AppRegistrationRepository appRegistrationRepository;

	public DefaultAppRegistryService(AppRegistrationRepository appRegistrationRepository,
			ResourceLoader resourceLoader, MavenProperties mavenProperties) {
		super(resourceLoader, mavenProperties);
		Assert.notNull(appRegistrationRepository, "'appRegistrationRepository' must not be null");
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		this.appRegistrationRepository = appRegistrationRepository;
	}

	@Override
	public AppRegistration find(String name, ApplicationType type) {
		return this.getDefaultApp(name, type);
	}

	@Override
	public AppRegistration find(String name, ApplicationType type, String version) {
		return this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(name, type, version);
	}

	@Override
	public AppRegistration getDefaultApp(String name, ApplicationType type) {
		return this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(name, type);
	}

	@Override
	public void setDefaultApp(String name, ApplicationType type, String version) {
		AppRegistration oldDefault = this.appRegistrationRepository
				.findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(name, type);
		if (oldDefault != null) {
			oldDefault.setDefaultVersion(false);
			this.appRegistrationRepository.save(oldDefault);
		}

		AppRegistration newDefault = this.appRegistrationRepository
				.findAppRegistrationByNameAndTypeAndVersion(name, type, version);

		if (newDefault == null) {
			throw new NoSuchAppRegistrationException(name, type, version);
		}

		newDefault.setDefaultVersion(true);

		this.appRegistrationRepository.save(newDefault);
	}

	@Override
	public List<AppRegistration> findAll() {
		return this.appRegistrationRepository.findAll();
	}

	@Override
	public Page<AppRegistration> findAllByTypeAndNameIsLike(ApplicationType type, String name, Pageable pageable) {
		if (!StringUtils.hasText(name) && type == null) {
			return findAll(pageable);
		}
		else if (StringUtils.hasText(name) && type == null) {
			return this.appRegistrationRepository.findAllByNameContainingIgnoreCase(name, pageable);
		}
		else if (StringUtils.hasText(name)) {
			return this.appRegistrationRepository.findAllByTypeAndNameContainingIgnoreCase(type, name, pageable);
		}
		else {
			return this.appRegistrationRepository.findAllByType(type, pageable);
		}
	}

	@Override
	public Page<AppRegistration> findAll(Pageable pageable) {
		return this.appRegistrationRepository.findAll(pageable);
	}

	@Override
	public AppRegistration save(String name, ApplicationType type, String version, URI uri, URI metadataUri) {
		return this.save(new AppRegistration(name, type, version, uri, metadataUri));
	}

	@Override
	public AppRegistration save(AppRegistration app) {
		AppRegistration appRegistration = this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				app.getName(), app.getType(), app.getVersion());
		if (appRegistration != null) {
			appRegistration.setUri(app.getUri());
			appRegistration.setMetadataUri(app.getMetadataUri());
			return this.appRegistrationRepository.save(appRegistration);
		}
		else {
			if (getDefaultApp(app.getName(), app.getType()) == null) {
				app.setDefaultVersion(true);
			}
			return this.appRegistrationRepository.save(app);
		}
	}

	/**
	 * Deletes an {@link AppRegistration}. If the {@link AppRegistration} does not exist, a
	 * {@link NoSuchAppRegistrationException} will be thrown.
	 *
	 * @param name Name of the AppRegistration to delete
	 * @param type Type of the AppRegistration to delete
	 * @param version Version of the AppRegistration to delete
	 */
	public void delete(String name, ApplicationType type, String version) {
		this.appRegistrationRepository.deleteAppRegistrationByNameAndTypeAndVersion(name, type, version);
	}

	@Override
	protected boolean isOverwrite(AppRegistration app, boolean overwrite) {
		return overwrite || this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(app.getName(),
				app.getType(), app.getVersion()) == null;
	}

	@Override
	public boolean appExist(String name, ApplicationType type) {
		return getDefaultApp(name, type) != null;
	}

	@Override
	public boolean appExist(String name, ApplicationType type, String version) {
		return find(name, type, version) != null;
	}
}
