/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.springframework.boot.loader.ExecutableArchiveLauncher;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;

/**
 * Strategy interface for creating a ClassLoader that mimics the one used when a boot
 * uber-jar runs.
 *
 * @author Eric Bottard
 */
public class BootClassLoaderFactory {

	private static final String BOOT_13_LIBS_LOCATION = "lib/";

	private static final String BOOT_14_LIBS_LOCATION = "BOOT-INF/lib/";

	private static final String BOOT_14_CLASSESS_LOCATION = "BOOT-INF/classes/";

	private final Archive archive;

	private final ClassLoader parent;

	/**
	 * Create a new factory for dealing with the given boot uberjar archive.
	 *
	 * @param archive a boot uberjar Archive
	 * @param parent the parent classloader to set for new created ClassLoaders
	 */
	public BootClassLoaderFactory(Archive archive, ClassLoader parent) {
		this.archive = archive;
		this.parent = parent;
	}

	public URLClassLoader createClassLoader() {
		boolean useBoot14Layout = false;
		for (Archive.Entry entry : archive) {
			if (entry.getName().startsWith(BOOT_14_LIBS_LOCATION)) {
				useBoot14Layout = true;
				break;
			}
		}

		ClassLoaderExposingLauncher launcher = useBoot14Layout ? new Boot14ClassLoaderExposingLauncher()
				: new Boot13ClassLoaderExposingLauncher();

		return launcher.createClassLoader();
	}

	private abstract class ClassLoaderExposingLauncher extends ExecutableArchiveLauncher {
		ClassLoaderExposingLauncher() {
			super(archive);
		}

		@Override
		protected ClassLoader createClassLoader(URL[] urls) throws Exception {
			return new LaunchedURLClassLoader(urls, parent);
		}

		public URLClassLoader createClassLoader() {
			try {
				return (URLClassLoader) createClassLoader(getClassPathArchivesIterator());
			}
			catch (Exception e) {
				throw new IllegalStateException("Error while creating ClassLoader for " + archive);
			}
		}

	}

	private class Boot13ClassLoaderExposingLauncher extends ClassLoaderExposingLauncher {

		@Override
		protected boolean isNestedArchive(Archive.Entry entry) {
			return !entry.isDirectory() && entry.getName().startsWith(BOOT_13_LIBS_LOCATION);
		}

		@Override
		protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
			archives.add(0, getArchive());
		}
	}

	private class Boot14ClassLoaderExposingLauncher extends ClassLoaderExposingLauncher {
		@Override
		protected boolean isNestedArchive(Archive.Entry entry) {
			return (!entry.isDirectory() && entry.getName().startsWith(BOOT_14_LIBS_LOCATION))
					|| (entry.isDirectory() && entry.getName().equals(BOOT_14_CLASSESS_LOCATION));
		}

		@Override
		protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
			archives.add(0, getArchive());
		}
	}
}
