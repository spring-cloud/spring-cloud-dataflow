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
package org.springframework.cloud.skipper.io;

import java.io.File;

import org.springframework.cloud.skipper.domain.Package;

/**
 * @author Mark Pollack
 */
public interface PackageWriter {

	/**
	 * Writes the package to the specified directory. File name is determined from package
	 * metadata.
	 * @param pkg the package to write
	 * @param directory the directory where to create the zip file of the package.
	 * @return the zip file in the specified directory.
	 */
	File write(Package pkg, File directory);
}
