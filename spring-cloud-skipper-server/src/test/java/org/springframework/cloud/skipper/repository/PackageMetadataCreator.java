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
package org.springframework.cloud.skipper.repository;

import org.springframework.cloud.skipper.domain.PackageMetadata;

/**
 * @author Mark Pollack
 */
public class PackageMetadataCreator {

	public static void createTwoPackages(PackageMetadataRepository repository) {
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setApiVersion("1");
		packageMetadata.setOrigin("www.example.com");
		packageMetadata.setKind("skipper");
		packageMetadata.setName("package1");
		packageMetadata.setVersion("1.0.0");
		packageMetadata.setIconUrl("http://www.gilligansisle.com/images/a2.gif");
		packageMetadata.setDescription("A very cool project");
		packageMetadata.setMaintainer("Alan Hale Jr.");
		repository.save(packageMetadata);
		packageMetadata = new PackageMetadata();
		packageMetadata.setApiVersion("1");
		packageMetadata.setOrigin("www.example.com");
		packageMetadata.setKind("skipper");
		packageMetadata.setName("package2");
		packageMetadata.setVersion("2.0.0");
		packageMetadata.setIconUrl("http://www.gilligansisle.com/images/a1.gif");
		packageMetadata.setMaintainer("Bob Denver");
		packageMetadata.setDescription("Another very cool project");
		repository.save(packageMetadata);
	}
}
