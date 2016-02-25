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

package org.springframework.cloud.dataflow.app.launcher;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * @author Marius Bogoevici
 */
class ArchiveMatchingEntryFilter implements Archive.EntryFilter {

	public static final ArchiveMatchingEntryFilter FILTER = new ArchiveMatchingEntryFilter();

	private static final AsciiBytes LIB = new AsciiBytes("lib/");

	@Override
	public boolean matches(Archive.Entry entry) {
		return isNestedArchive(entry);
	}

	protected boolean isNestedArchive(Archive.Entry entry) {
		return !entry.isDirectory() && entry.getName().startsWith(LIB);
	}
}
