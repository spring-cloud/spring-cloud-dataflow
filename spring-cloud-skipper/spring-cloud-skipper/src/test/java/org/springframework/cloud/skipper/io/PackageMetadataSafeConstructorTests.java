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

import org.junit.jupiter.api.Test;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class PackageMetadataSafeConstructorTests {
	private String testYaml = "!!org.springframework.cloud.skipper.domain.PackageMetadata\n" +
		"apiVersion: skipper.spring.io/v1\n" +
		"description: time --management.endpoints.web.exposure.include=health,info,bindings\n" +
		"  --management.metrics.tags.application.type=${spring.cloud.dataflow.stream.app.type:unknown}\n" +
		"  --management.metrics.tags.stream.name=${spring.cloud.dataflow.stream.name:unknown}\n" +
		"  --management.metrics.tags.application=${spring.cloud.dataflow.stream.name:unknown}-${spring.cloud.dataflow.stream.app.label:unknown}-${spring.cloud.dataflow.stream.app.type:unknown}\n" +
		"  --management.metrics.tags.instance.index=${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}\n" +
		"  --wavefront.application.service=${spring.cloud.dataflow.stream.app.label:unknown}-${spring.cloud.dataflow.stream.app.type:unknown}-${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}\n" +
		"  --management.metrics.tags.application.guid=${spring.cloud.application.guid:unknown}\n" +
		"  --management.metrics.tags.application.name=${vcap.application.application_name:${spring.cloud.dataflow.stream.app.label:unknown}}\n" +
		"  --wavefront.application.name=${spring.cloud.dataflow.stream.name:unknown} | log\n" +
		"  --management.endpoints.web.exposure.include=health,info,bindings --management.metrics.tags.application.type=${spring.cloud.dataflow.stream.app.type:unknown}\n" +
		"  --management.metrics.tags.stream.name=${spring.cloud.dataflow.stream.name:unknown}\n" +
		"  --management.metrics.tags.application=${spring.cloud.dataflow.stream.name:unknown}-${spring.cloud.dataflow.stream.app.label:unknown}-${spring.cloud.dataflow.stream.app.type:unknown}\n" +
		"  --management.metrics.tags.instance.index=${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}\n" +
		"  --wavefront.application.service=${spring.cloud.dataflow.stream.app.label:unknown}-${spring.cloud.dataflow.stream.app.type:unknown}-${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}\n" +
		"  --management.metrics.tags.application.guid=${spring.cloud.application.guid:unknown}\n" +
		"  --management.metrics.tags.application.name=${vcap.application.application_name:${spring.cloud.dataflow.stream.app.label:unknown}}\n" +
		"  --wavefront.application.name=${spring.cloud.dataflow.stream.name:unknown}\n" +
		"displayName: null\n" +
		"iconUrl: null\n" +
		"kind: SpringCloudDeployerApplication\n" +
		"maintainer: dataflow\n" +
		"name: ticktock\n" +
		"origin: null\n" +
		"packageFile: null\n" +
		"packageHomeUrl: null\n" +
		"packageSourceUrl: null\n" +
		"repositoryId: 11\n" +
		"repositoryName: repositoryName\n" +
		"sha256: null\n" +
		"tags: null\n" +
		"version: 1.0.0";

	@Test
	void safeConstructor() {
		DumperOptions options = new DumperOptions();
		Representer representer = new Representer(options);
		representer.getPropertyUtils().setSkipMissingProperties(true);
		Yaml yaml = new Yaml(new PackageMetadataSafeConstructor(), representer);
		PackageMetadata packageMetadata =  yaml.load(testYaml);
		assertThat(packageMetadata.getApiVersion()).isEqualTo("skipper.spring.io/v1");
		assertThat(packageMetadata.getOrigin()).isEqualTo("null");
		assertThat(packageMetadata.getRepositoryId()).isEqualTo(11L);
		assertThat(packageMetadata.getRepositoryName()).isEqualTo("repositoryName");
		assertThat(packageMetadata.getKind()).isEqualTo("SpringCloudDeployerApplication");
		assertThat(packageMetadata.getName()).isEqualTo("ticktock");
		assertThat(packageMetadata.getDisplayName()).isEqualTo("null");
		assertThat(packageMetadata.getVersion()).isEqualTo("1.0.0");
		assertThat(packageMetadata.getPackageSourceUrl()).isEqualTo("null");
		assertThat(packageMetadata.getPackageHomeUrl()).isEqualTo("null");
		assertThat(packageMetadata.getTags()).isEqualTo("null");
		assertThat(packageMetadata.getMaintainer()).isEqualTo("dataflow");
		assertThat(packageMetadata.getDescription()).contains("--management.endpoints.web.exposure.include=health,info,bindings");
		assertThat(packageMetadata.getSha256()).isEqualTo("null");
		assertThat(packageMetadata.getIconUrl()).isEqualTo("null");
	}

	@Test
	void badYaml() {
		DumperOptions options = new DumperOptions();
		Representer representer = new Representer(options);
		representer.getPropertyUtils().setSkipMissingProperties(true);
		Yaml yaml = new Yaml(new PackageMetadataSafeConstructor(), representer);
		assertThatThrownBy( () -> yaml.load("!!org.springframework.cloud.skipper.domain.PackageMetadata\n" +
			"apiVersion: !!javax.script.ScriptEngineManager [!!java.lang.String [\"helloworld\"]]"))
			.isInstanceOf(YAMLException.class);
	}

}
