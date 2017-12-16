package org.springframework.cloud.dataflow.registry.service;

import java.net.URI;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Christian Tzolov
 */
public interface AppRegistryService extends AppRegistryCommon {

	/**
	 * @param name applicaiton name
	 * @param type application version
	 * @return the default application for this name:type pair. Returns null if no name:type
	 * application exists or if none of the name:type applications is set as default.
	 */
	AppRegistration getDefaultApp(String name, ApplicationType type);

	/**
	 * Set an application with name, type and version as the default for all name:type
	 * applications. The previous default name:type application is set to non-default.
	 * @param name application name
	 * @param type applicaiton type
	 * @param version application version.
	 */
	void setDefaultApp(String name, ApplicationType type, String version);

	Page<AppRegistration> findAll(Pageable pageable);

	AppRegistration save(String name, ApplicationType type, String version, URI uri, URI metadataUri);

	void delete(String name, ApplicationType type, String version);

	Resource getResource(String uri);

	boolean appExist(String name, ApplicationType type, String version);

	Page<AppRegistration> findAllByTypeAndNameIsLike(ApplicationType type, String name, Pageable pageable);
}
