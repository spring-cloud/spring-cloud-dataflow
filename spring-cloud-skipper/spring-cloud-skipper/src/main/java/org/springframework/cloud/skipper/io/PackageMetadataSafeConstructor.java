/*
 * Copyright 2024 the original author or authors.
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

import org.springframework.cloud.skipper.domain.PackageMetadata;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Extends {@link SafeConstructor} so that we can construct an instance of {@link org.springframework.cloud.skipper.domain.PackageMetadata}
 * When deserializing yaml for deploying apps in stream definitions.
 *
 * @author Glenn Renfro
 */
class PackageMetadataSafeConstructor extends SafeConstructor {
	private static final String API_VERSION = "apiVersion";
	private static final String ORIGIN = "origin";
	private static final String REPOSITORY_ID = "repositoryId";
	private static final String REPOSITORY_NAME = "repositoryName";
	private static final String PACKAGE_KIND = "kind";
	private static final String NAME = "name";
	private static final String DISPLAY_NAME = "displayName";
	private static final String PACKAGE_VERSION = "version";
	private static final String PACKAGE_SOURCE_URL = "packageSourceUrl";
	private static final String PACKAGE_HOME_URL = "packageHomeUrl";
	private static final String TAGS = "tags";
	private static final String MAINTAINER = "maintainer";
	private static final String DESCRIPTION = "description";
	private static final String SHA256 = "sha256";
	private static final String ICON_URL = "iconUrl";

	PackageMetadataSafeConstructor() {
		super(new LoaderOptions());
		this.loadingConfig.setTagInspector(tag -> tag.getClassName().equals(PackageMetadata.class.getName()));
		this.yamlConstructors.put(new TypeDescription(PackageMetadata.class).getTag(), new ConstructYamlPackageMetadata());
		rootTag = new Tag(new TypeDescription(PackageMetadata.class).getType());
	}

	private class ConstructYamlPackageMetadata implements Construct {
		@Override
		public Object construct(Node node) {
			MappingNode mappingNode = (MappingNode) node;
			PackageMetadata packageMetadata = new PackageMetadata();
			try {
				for (NodeTuple tuple : mappingNode.getValue()) {
					ScalarNode keyNode = (ScalarNode) tuple.getKeyNode();
					ScalarNode valueNode = (ScalarNode) tuple.getValueNode();
					String key = keyNode.getValue();
					setKeyValue(packageMetadata, key, valueNode.getValue());
				}
			}
			catch (ClassCastException cce) {
				throw new YAMLException("Unable to Parse yaml to PackageMetadata type", cce);
			}
			return packageMetadata;
		}

		@Override
		public void construct2ndStep(Node node, Object object) {

		}

		public PackageMetadata setKeyValue(PackageMetadata packageMetadata, String key, String value) {
			switch (key) {
				case API_VERSION:
					packageMetadata.setApiVersion(value);
					break;
				case ORIGIN:
					packageMetadata.setOrigin(value);
					break;
				case REPOSITORY_ID:
					packageMetadata.setRepositoryId(isLong(value) ? Long.parseLong(value) : null);
					break;
				case REPOSITORY_NAME:
					packageMetadata.setRepositoryName(value);
					break;
				case PACKAGE_KIND:
					packageMetadata.setKind(value);
					break;
				case NAME:
					packageMetadata.setName(value);
					break;
				case DISPLAY_NAME:
					packageMetadata.setDisplayName(value);
					break;
				case PACKAGE_VERSION:
					packageMetadata.setVersion(value);
					break;
				case PACKAGE_SOURCE_URL:
					packageMetadata.setPackageSourceUrl(value);
					break;
				case PACKAGE_HOME_URL:
					packageMetadata.setPackageHomeUrl(value);
					break;
				case TAGS:
					packageMetadata.setTags(value);
					break;
				case MAINTAINER:
					packageMetadata.setMaintainer(value);
					break;
				case DESCRIPTION:
					packageMetadata.setDescription(value);
					break;
				case SHA256:
					packageMetadata.setSha256(value);
					break;
				case ICON_URL:
					packageMetadata.setIconUrl(value);
					break;
			}
			return packageMetadata;
		}
		private boolean isLong(String str) {
			if (str == null || str.isEmpty()) {
				return false;
			}
			try {
				Long.parseLong(str);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
}
