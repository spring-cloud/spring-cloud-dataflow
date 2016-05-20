/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.stream.configuration.metadata;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Used to retrieve metadata about the configuration properties that can alter a module behavior.
 *
 * @author Eric Bottard
 */
public class ModuleConfigurationMetadataResolver {

	public ModuleConfigurationMetadataResolver() {
		JarFile.registerUrlProtocolHandler();
	}

	/**
	 * Return metadata about configuration properties (as groups) that are documented via
	 * <a href="http://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html">
	 * Spring Boot configuration metadata</a> and visible in a module.
	 *
	 * @param module a Spring Cloud Stream module; typically a Boot uberjar,
	 * but directories are supported as well
	 */
	public List<ConfigurationMetadataGroup> listPropertyGroups(Resource module) {
		List<ConfigurationMetadataGroup> result = new ArrayList<>();
		ClassLoader classLoader = null;
		try {
			File moduleFile = module.getFile();
			Archive archive = moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive(moduleFile);
			classLoader = createClassLoader(archive);
			ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
			ResourcePatternResolver moduleResourceLoader = new PathMatchingResourcePatternResolver(classLoader);
			for (Resource r : moduleResourceLoader.getResources("classpath*:/META-INF/*spring-configuration-metadata.json")) {
				builder.withJsonResource(r.getInputStream());
			}
			for (ConfigurationMetadataGroup group : builder.build().getAllGroups().values()) {
				result.add(group);
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Exception trying to list configuration properties for module " + module, e);
		}
		finally {
			if (classLoader instanceof Closeable) {
				try {
					((Closeable) classLoader).close();
				}
				catch (IOException e) {
					// ignore
				}
			}
		}
		return result;
	}

	/**
	 * Return metadata about configuration properties that are documented via
	 * <a href="http://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html">
	 * Spring Boot configuration metadata</a> and visible in a module.
	 *
	 * @param module a Spring Cloud Stream module; typically a Boot uberjar,
	 * but directories are supported as well
	 */
	public List<ConfigurationMetadataProperty> listProperties(Resource module) {
		List<ConfigurationMetadataProperty> result = new ArrayList<>();
		ClassLoader classLoader = null;
		try {
			File moduleFile = module.getFile();
			Archive archive = moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive(moduleFile);
			classLoader = createClassLoader(archive);
			ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
			ResourcePatternResolver moduleResourceLoader = new PathMatchingResourcePatternResolver(classLoader);
			for (Resource r : moduleResourceLoader.getResources("classpath*:/META-INF/*spring-configuration-metadata.json")) {
				builder.withJsonResource(r.getInputStream());
			}
			for (ConfigurationMetadataProperty property : builder.build().getAllProperties().values()) {
						result.add(property);
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Exception trying to list configuration properties for module " + module, e);
		}
		finally {
			if (classLoader instanceof Closeable) {
				try {
					((Closeable) classLoader).close();
				}
				catch (IOException e) {
					// ignore
				}
			}
		}
		return result;
	}

	/**
	 * Return a {@link ClassLoader} for accessing resources in the provided
	 * {@link Archive}. The caller is responsible for disposing of the
	 * class loader.
	 *
	 * @param archive the archive for which to return a class loader
	 * @return class loader for the given archive
	 * @throws Exception if the class loader cannot be created
	 */
	protected ClassLoader createClassLoader(Archive archive) throws Exception {
		return new ClassLoaderExposingJarLauncher(archive).createClassLoader();
	}


	/**
	 * Extension of {@link ModuleJarLauncher} used for exposing a {@link ClassLoader}
	 * for the provided {@link Archive}.
	 */
	private static class ClassLoaderExposingJarLauncher extends ModuleJarLauncher {

		public ClassLoaderExposingJarLauncher(Archive archive) {
			super(archive);
		}

		protected ClassLoader createClassLoader() throws Exception {
			List<Archive> classPathArchives = getClassPathArchives();
			return createClassLoader(classPathArchives);
		}
	}
}
