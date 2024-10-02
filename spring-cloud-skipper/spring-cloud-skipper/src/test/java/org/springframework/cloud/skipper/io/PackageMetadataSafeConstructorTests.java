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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.cloud.skipper.domain.PackageMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class PackageMetadataSafeConstructorTests {
	private String testYaml = """
		!!org.springframework.cloud.skipper.domain.PackageMetadata
		apiVersion: skipper.spring.io/v1
		description: time --management.endpoints.web.exposure.include=health,info,bindings
		  --management.metrics.tags.application.type=${spring.cloud.dataflow.stream.app.type:unknown}
		  --management.metrics.tags.stream.name=${spring.cloud.dataflow.stream.name:unknown}
		  --management.metrics.tags.application=${spring.cloud.dataflow.stream.name:unknown}-${spring.cloud.dataflow.stream.app.label:unknown}-${spring.cloud.dataflow.stream.app.type:unknown}
		  --management.metrics.tags.instance.index=${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}
		  --wavefront.application.service=${spring.cloud.dataflow.stream.app.label:unknown}-${spring.cloud.dataflow.stream.app.type:unknown}-${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}
		  --management.metrics.tags.application.guid=${spring.cloud.application.guid:unknown}
		  --management.metrics.tags.application.name=${vcap.application.application_name:${spring.cloud.dataflow.stream.app.label:unknown}}
		  --wavefront.application.name=${spring.cloud.dataflow.stream.name:unknown} | log
		  --management.endpoints.web.exposure.include=health,info,bindings --management.metrics.tags.application.type=${spring.cloud.dataflow.stream.app.type:unknown}
		  --management.metrics.tags.stream.name=${spring.cloud.dataflow.stream.name:unknown}
		  --management.metrics.tags.application=${spring.cloud.dataflow.stream.name:unknown}-${spring.cloud.dataflow.stream.app.label:unknown}-${spring.cloud.dataflow.stream.app.type:unknown}
		  --management.metrics.tags.instance.index=${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}
		  --wavefront.application.service=${spring.cloud.dataflow.stream.app.label:unknown}-${spring.cloud.dataflow.stream.app.type:unknown}-${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}
		  --management.metrics.tags.application.guid=${spring.cloud.application.guid:unknown}
		  --management.metrics.tags.application.name=${vcap.application.application_name:${spring.cloud.dataflow.stream.app.label:unknown}}
		  --wavefront.application.name=${spring.cloud.dataflow.stream.name:unknown}
		displayName: null
		iconUrl: null
		kind: SpringCloudDeployerApplication
		maintainer: dataflow
		name: ticktock
		origin: null
		packageFile: null
		packageHomeUrl: null
		packageSourceUrl: null
		repositoryId: 11
		repositoryName: repositoryName
		sha256: null
		tags: null
		version: 1.0.0""";

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
		assertThatThrownBy( () -> yaml.load("""
			!!org.springframework.cloud.skipper.domain.PackageMetadata
			apiVersion: !!javax.script.ScriptEngineManager [!!java.lang.String ["helloworld"]]"""))
			.isInstanceOf(YAMLException.class);
	}

}
