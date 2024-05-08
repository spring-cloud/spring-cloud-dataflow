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

package org.springframework.cloud.skipper.server.controller.docs;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.io.DefaultPackageReader;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.cloud.skipper.server.repository.jpa.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.repository.jpa.RepositoryRepository;
import org.springframework.cloud.skipper.server.service.PackageService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService;
import org.springframework.cloud.skipper.server.util.ConfigValueUtils;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.restdocs.hypermedia.HypermediaDocumentation;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.RequestParametersSnippet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;

/**
 * Sets up Spring Rest Docs via {@link #setupMocks()} and also provides common snippets to
 * be used by the various documentation tests.
 *
 * @author Gunnar Hillert
 * @author Eddú Meléndez Gonzales
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@EnableWebMvc
@ActiveProfiles("repo-test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs("target/generated-snippets")
@SpringBootTest(properties = { "spring.cloud.skipper.server.synchonizeIndexOnContextRefresh=false",
		"spring.cloud.skipper.server.enableReleaseStateUpdateService=false",
		"spring.main.allow-bean-definition-overriding=true"
}, classes = ServerDependencies.class)
public abstract class BaseDocumentation {

	protected MockMvc mockMvc;

	@Autowired
	protected WebApplicationContext wac;

	@Autowired
	private MockMvcRestDocumentationConfigurer restDocumentationConfigurer;

	@Autowired
	public WebApplicationContext context;


	@Autowired
	protected RepositoryRepository repositoryRepository;

	@Autowired
	protected ReleaseRepository releaseRepository;

	@Autowired
	protected PackageMetadataRepository packageMetadataRepository;

	@Autowired
	protected ReleaseService releaseService;

	@Autowired
	protected SkipperStateMachineService skipperStateMachineService;

	@Autowired
	protected PackageService packageService;


	/**
	 * Snippet, documenting common pagination properties.
	 */
	protected final ResponseFieldsSnippet paginationProperties = responseFields(
			fieldWithPath("page").description("Pagination properties"),
			fieldWithPath("page.size").description("The size of the page being returned"),
			fieldWithPath("page.totalElements").description("Total elements available for pagination"),
			fieldWithPath("page.totalPages").description("Total amount of pages"),
			fieldWithPath("page.number").description("Page number of the page returned (zero-based)"));
	/**
	 * Snippet for link properties. Set to ignore common links.
	 */
	protected final List<FieldDescriptor> defaultLinkProperties = Arrays.asList(
			fieldWithPath("_links.first.href").ignored().optional(),
			fieldWithPath("_links.self.href").ignored().optional(),
			fieldWithPath("_links.next.href").ignored().optional(),
			fieldWithPath("_links.last.href").ignored().optional(),
			fieldWithPath("_links.self.templated").ignored().optional(),
			fieldWithPath("_links.profile.href").ignored().optional(),
			fieldWithPath("_links.repository.href").ignored().optional(),
			fieldWithPath("_links.search.href").ignored().optional());
	/**
	 * Snippet for common pagination-related request parameters.
	 */
	protected final RequestParametersSnippet paginationRequestParameterProperties = requestParameters(
			parameterWithName("page").description("The zero-based page number (optional)"),
			parameterWithName("size").description("The requested page size (optional)"));
	protected RestDocumentationResultHandler documentationHandler;

	/**
	 * {@link LinksSnippet} for common links. Common links are set to be ignored.
	 *
	 * @param descriptors Provide addition link descriptors
	 * @return the link snipped
	 */
	public static LinksSnippet linksForSkipper(LinkDescriptor... descriptors) {
		return HypermediaDocumentation.links(
				linkWithRel("self").ignored(),
				linkWithRel("first").ignored().optional(),
				linkWithRel("next").ignored().optional(),
				linkWithRel("last").ignored().optional(),
				linkWithRel("profile").ignored(),
				linkWithRel("search").ignored(),
				linkWithRel("deployer").ignored().optional(),
				linkWithRel("curies").ignored().optional()).and(descriptors);
	}

	@BeforeEach
	public void setupMocks() {
		this.prepareDocumentationTests(this.context);
	}

	private void prepareDocumentationTests(WebApplicationContext context) {
		this.documentationHandler = document("{class-name}/{method-name}",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()));

		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(this.restDocumentationConfigurer.uris()
						.withScheme("http")
						.withHost("localhost")
						.withPort(7577))
				.alwaysDo(this.documentationHandler)
				.build();
	}

	protected InstallProperties createInstallProperties(String releaseName) {
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName("default");
		installProperties.setConfigValues(getSampleConfigValues());
		return installProperties;
	}

	private ConfigValues getSampleConfigValues() {
		ConfigValues configValues = new ConfigValues();
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		Yaml yaml = new Yaml(dumperOptions);
		Map<String, String> configMap = new HashMap<>();
		configMap.put("config1", "value1");
		configMap.put("config2", "value2");
		configValues.setRaw(yaml.dump(configMap));
		return configValues;
	}

	protected UpgradeProperties createUpdateProperties(String releaseName) {
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		upgradeProperties.setConfigValues(getSampleConfigValues());
		return upgradeProperties;
	}

	protected Release createTestRelease() {
		return createTestRelease("test", StatusCode.DEPLOYED);
	}

	protected Release createTestRelease(String name, StatusCode statusCode) {
		Release release = new Release();
		release.setName(name);
		release.setVersion(1);
		release.setPlatformName("default");
		release.setConfigValues(getSampleConfigValues());
		return updateReleaseStatus(updateReleaseManifest(release), statusCode);
	}

	protected Release updateReleaseManifest(Release release) {
		Resource resource = new ClassPathResource("/repositories/sources/test/log/log-1.0.0");
		PackageReader packageReader = new DefaultPackageReader();
		try {
			Package pkg = packageReader.read(resource.getFile());
			release.setPkg(pkg);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		Map<String, Object> mergedMap = ConfigValueUtils.mergeConfigValues(release.getPkg(), release.getConfigValues());
		Manifest manifest = new Manifest();
		manifest.setData(ManifestUtils.createManifest(release.getPkg(), mergedMap));
		release.setManifest(manifest);
		return release;
	}

	protected Release updateReleaseStatus(Release release, StatusCode statusCode) {
		Info releaseInfo = new Info();
		Status status = new Status();
		status.setStatusCode(StatusCode.DELETED);
		releaseInfo.setStatus(status);
		release.setInfo(releaseInfo);
		return release;
	}

	protected static String convertObjectToJson(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		String json = mapper.writeValueAsString(object);
		return json;
	}

}
