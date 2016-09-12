/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.springframework.boot.loader.ExecutableArchiveLauncher;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * Strategy interface for creating a ClassLoader that mimics the one used when
 * a boot uber-jar runs.
 *
 * @author Eric Bottard
 */
public class BootClassLoaderCreation {

	private final Archive archive;

	private final ClassLoader parent;

	private static final AsciiBytes BOOT_13_LIBS_LOCATION = new AsciiBytes("lib/");

	private static final AsciiBytes BOOT_14_LIBS_LOCATION = new AsciiBytes("BOOT-INF/lib/");

	public BootClassLoaderCreation(Archive archive, ClassLoader parent) {
		this.archive = archive;
		this.parent = parent;
	}

	public URLClassLoader createClassLoader() {
		AsciiBytes layout = BOOT_13_LIBS_LOCATION;
		for (Archive.Entry entry : archive.getEntries()) {
			if (entry.getName().startsWith(BOOT_14_LIBS_LOCATION)) {
				layout = BOOT_14_LIBS_LOCATION;
				break;
			}
		}
		ClassLoaderExposingLauncher launcher = new ClassLoaderExposingLauncher(layout);

		return launcher.createClassLoader();
	}

	private class ClassLoaderExposingLauncher extends ExecutableArchiveLauncher {

		private final AsciiBytes libsLocation;

		public ClassLoaderExposingLauncher(AsciiBytes libsLocation) {
			super(archive);
			this.libsLocation = libsLocation;
		}

		public URLClassLoader createClassLoader() {
			try {
				return (URLClassLoader) createClassLoader(getClassPathArchives());
			}
			catch (Exception e) {
				throw new IllegalStateException("Error while creating ClassLoader for " + archive);
			}
		}

		@Override
		protected boolean isNestedArchive(Archive.Entry entry) {
			return !entry.isDirectory() && entry.getName().startsWith(libsLocation);
		}

		@Override
		protected ClassLoader createClassLoader(URL[] urls) throws Exception {
			return new LaunchedURLClassLoader(urls, parent);
		}

		@Override
		protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
			archives.add(0, getArchive());
		}


	}
}
