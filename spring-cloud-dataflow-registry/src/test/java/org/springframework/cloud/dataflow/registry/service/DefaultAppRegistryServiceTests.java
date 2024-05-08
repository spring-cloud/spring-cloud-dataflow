/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.registry.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultAppRegistryService}.
 *
 * @author Christian Tzolov
 * @author Chris Schaefer
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Corneil du Plessis
 */
public class DefaultAppRegistryServiceTests {

	private final AppRegistrationRepository appRegistrationRepository = mock(AppRegistrationRepository.class);

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final AppRegistryService appRegistryService = new DefaultAppRegistryService(appRegistrationRepository,
			new AppResourceCommon(new MavenProperties(), resourceLoader), mock(DefaultAuditRecordService.class));

	@Test
	public void testNotFound() {
		AppRegistration registration = appRegistryService.find("foo", ApplicationType.source);
		assertThat(registration).isNull();
	}

	@Test
	public void testFound() {
		AppRegistration registration = appRegistration();
		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(
				eq(registration.getName()), eq(registration.getType()))).thenReturn(registration);

		AppRegistration registration2 = appRegistryService.find("foo", ApplicationType.source);
		assertThat(registration2.getName()).isEqualTo("foo");
		assertThat(registration2.getType()).isEqualTo(ApplicationType.source);
	}

	@Test
	public void testMetadataResourceResolvesWhenAvailable() {
		AppRegistration registration = appRegistration();
		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(
				eq(registration.getName()), eq(registration.getType()))).thenReturn(registration);

		AppRegistration registration2 = appRegistryService.find("foo", ApplicationType.source);
		Resource appMetadataResource = appRegistryService.getAppMetadataResource(registration2);

		assertThat(appMetadataResource.getFilename()).isEqualTo("foo-source-metadata");
	}

	@Test
	public void testMetadataResourceNotAvailableResolvesToMainResource() {
		AppRegistration registration = appRegistration();
		registration.setMetadataUri(null);
		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(
				eq(registration.getName()), eq(registration.getType()))).thenReturn(registration);

		AppRegistration registration2 = appRegistryService.find("foo", ApplicationType.source);
		Resource appMetadataResource = appRegistryService.getAppMetadataResource(registration2);

		assertThat(appMetadataResource.getFilename()).isEqualTo("foo-source");
	}

	@Test
	public void testFindAll() {
		AppRegistration fooSource = appRegistration("foo", ApplicationType.source, true);
		AppRegistration fooSink = appRegistration("foo", ApplicationType.sink, false);
		AppRegistration barSource = appRegistration("bar", ApplicationType.source, true);
		when(appRegistrationRepository.findAll()).thenReturn(Arrays.asList(fooSource, fooSink, barSource));

		List<AppRegistration> registrations = appRegistryService.findAll();

		assertThat(registrations).haveAtLeastOne(appRegistrationWith("foo", URI.create("classpath:/foo-source"), URI.create("classpath:/foo-source-metadata"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("bar", URI.create("classpath:/bar-source"), URI.create("classpath:/bar-source-metadata"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("foo", URI.create("classpath:/foo-sink"), null, ApplicationType.sink));
	}

	static Condition<AppRegistration> appRegistrationWith(String name, URI uri, URI metadata, ApplicationType type) {
		return metadata != null ?
			new Condition<>(item -> name.equals(item.getName()) && uri.equals(item.getUri()) && metadata.equals(item.getMetadataUri()) && type.equals(item.getType()), "AppRegistrationWith") :
			new Condition<>(item -> name.equals(item.getName()) && uri.equals(item.getUri()) && item.getMetadataUri() == null && type.equals(item.getType()), "AppRegistrationWith");
	}
	@Test
	public void testFindAllPageable() {
		AppRegistration fooSource = appRegistration("foo", ApplicationType.source, true);
		AppRegistration fooSink = appRegistration("foo", ApplicationType.sink, false);
		AppRegistration barSource = appRegistration("bar", ApplicationType.source, true);

		PageRequest pageRequest1 = PageRequest.of(0, 2);
		when(appRegistrationRepository.findAll(eq(pageRequest1)))
				.thenReturn(new PageImpl(Arrays.asList(fooSink, barSource), pageRequest1, 3));
		Page<AppRegistration> registrations1 = appRegistryService.findAll(pageRequest1);

		assertEquals(3, registrations1.getTotalElements());
		assertEquals(2, registrations1.getContent().size());
		assertEquals("foo", registrations1.getContent().get(0).getName());
		assertEquals("bar", registrations1.getContent().get(1).getName());

		PageRequest pageRequest2 = PageRequest.of(1, 2);
		when(appRegistrationRepository.findAll(eq(pageRequest2)))
				.thenReturn(new PageImpl(Collections.singletonList(fooSource), pageRequest2, 3));
		Page<AppRegistration> registrations2 = appRegistryService.findAll(pageRequest2);

		assertEquals(3, registrations2.getTotalElements());
		assertEquals(1, registrations2.getContent().size());
		assertEquals("foo", registrations2.getContent().get(0).getName());
	}

	@Test
	public void testSaveNonDefaultApp() {
		AppRegistration fooSource = appRegistration("foo", ApplicationType.source, true);
		assertFalse(fooSource.isDefaultVersion());
		AppRegistration saved = appRegistryService.save(fooSource);
		verify(appRegistrationRepository, times(1)).findAppRegistrationByNameAndTypeAndVersion(
				eq(fooSource.getName()), eq(fooSource.getType()), eq(fooSource.getVersion()));

		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(1)).save(appRegistrationCaptor.capture());
		assertTrue(appRegistrationCaptor.getValue().isDefaultVersion());
	}

	@Test
	public void testSaveDefault() {
		AppRegistration fooSource = appRegistration("foo", ApplicationType.source, true);
		assertFalse(fooSource.isDefaultVersion());
		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(
				eq(fooSource.getName()), eq(fooSource.getType()))).thenReturn(fooSource);

		appRegistryService.save(fooSource);

		verify(appRegistrationRepository, times(1)).findAppRegistrationByNameAndTypeAndVersion(
				eq(fooSource.getName()), eq(fooSource.getType()), eq(fooSource.getVersion()));
		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(1)).save(appRegistrationCaptor.capture());
		assertFalse(appRegistrationCaptor.getValue().isDefaultVersion());
	}

	@Test
	public void testSaveExistingApp() {
		AppRegistration fooSource = appRegistration("foo", ApplicationType.source, true);
		AppRegistration fooSource2 = appRegistration("foo", ApplicationType.source, true);
		fooSource2.setUri(null);
		fooSource2.setMetadataUri(null);

		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq(fooSource2.getName()), eq(fooSource2.getType()), eq(fooSource2.getVersion())))
				.thenReturn(fooSource2);

		appRegistryService.save(fooSource);

		verify(appRegistrationRepository, times(1)).findAppRegistrationByNameAndTypeAndVersion(
				eq(fooSource.getName()), eq(fooSource.getType()), eq(fooSource.getVersion()));
		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(1)).save(appRegistrationCaptor.capture());

		assertEquals(fooSource.getUri(), fooSource2.getUri());
		assertEquals(fooSource.getMetadataUri(), fooSource2.getMetadataUri());
	}

	@Test
	public void testImportAllOverwrite() {
		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq("foo"), eq(ApplicationType.source), eq("1.0"))).thenReturn(appRegistration());
		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq("bar"), eq(ApplicationType.sink), eq("1.0"))).thenReturn(appRegistration());
		assertThat(appRegistryService.importAll(false, new ClassPathResource("AppRegistryTests-importAllOverwrite.properties", getClass()))).isEmpty();
	}

	@Test
	public void testImportRealWorldJarsWithMetadata() {
		appRegistryService.importAll(true,
				new ClassPathResource("AppRegistryTests-import-with-metadata.properties", getClass()));
		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(1)).save(appRegistrationCaptor.capture());
		List<AppRegistration> registrations = appRegistrationCaptor.getAllValues();
		AppRegistration appRegistration = registrations.get(0);
		assertThat(appRegistration.getName()).isEqualTo("cassandra");
		assertThat(appRegistration.getUri()).isEqualTo(URI.create("http://repo.spring.io/release/org/springframework/cloud/stream/app/cassandra-sink-rabbit/2.1.0.RELEASE/cassandra-sink-rabbit-2.1.0.RELEASE.jar"));
		assertThat(appRegistration.getMetadataUri()).isEqualTo(URI.create("http://repo.spring.io/release/org/springframework/cloud/stream/app/cassandra-sink-rabbit/2.1.0.RELEASE/cassandra-sink-rabbit-2.1.0.RELEASE-metadata.jar"));
		assertThat(appRegistration.getType()).isEqualTo(ApplicationType.sink);
	}

	@Test
	public void testImportAll() {

		final boolean overwrite = true;

		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq("foo"), eq(ApplicationType.source), eq("1.0"))).thenReturn(appRegistration());

		appRegistryService.importAll(!overwrite,
				new ClassPathResource("AppRegistryTests-importAll.properties", getClass()));

		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(2)).save(appRegistrationCaptor.capture());

		List<AppRegistration> registrations = appRegistrationCaptor.getAllValues();

		assertThat(registrations).haveAtLeastOne(appRegistrationWith("bar", URI.create("http:/bar-source-1.0.0"), URI.create("http:/bar-source-metadata-1.0.0"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("foo", URI.create("http:/foo-sink-1.0.0"), null, ApplicationType.sink));
		//
		// Now import with overwrite = true
		//
		reset(appRegistrationRepository);

		appRegistryService.importAll(overwrite,
				new ClassPathResource("AppRegistryTests-importAll.properties", getClass()));

		appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(3)).save(appRegistrationCaptor.capture());

		registrations = appRegistrationCaptor.getAllValues();

		assertThat(registrations).haveAtLeastOne(appRegistrationWith("foo", URI.create("http:/foo-source-1.0.0"), URI.create("http:/foo-source-metadata-1.0.0"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("bar", URI.create("http:/bar-source-1.0.0"), URI.create("http:/bar-source-metadata-1.0.0"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("foo", URI.create("http:/foo-sink-1.0.0"), null, ApplicationType.sink));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testImportMixedVersions() {

		final boolean overwrite = true;

		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq("foo"), eq(ApplicationType.source), eq("1.0"))).thenReturn(appRegistration());

		appRegistryService.importAll(!overwrite,
				new ClassPathResource("AppRegistryTests-importMixedVersions1.properties", getClass()));

		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(4)).save(appRegistrationCaptor.capture());

		List<AppRegistration> registrations = appRegistrationCaptor.getAllValues();
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.0.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.0.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.2.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.2.RELEASE"), ApplicationType.sink));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.sink));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testImportMixedVersionsMultiFile() {

		final boolean overwrite = true;

		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq("foo"), eq(ApplicationType.source), eq("1.0"))).thenReturn(appRegistration());

		appRegistryService.importAll(!overwrite,
				new ClassPathResource("AppRegistryTests-importMixedVersions2.properties", getClass()),
				new ClassPathResource("AppRegistryTests-importMixedVersions3.properties", getClass()));

		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(4)).save(appRegistrationCaptor.capture());

		List<AppRegistration> registrations = appRegistrationCaptor.getAllValues();

		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.0.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.0.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.2.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.2.RELEASE"), ApplicationType.sink));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.sink));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testImportMixedVersionsWithSpaceAndComments() {

		final boolean overwrite = true;

		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq("foo"), eq(ApplicationType.source), eq("1.0"))).thenReturn(appRegistration());

		appRegistryService.importAll(!overwrite,
				new ClassPathResource("AppRegistryTests-importMixedVersions4.properties", getClass()));

		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(4)).save(appRegistrationCaptor.capture());

		List<AppRegistration> registrations = appRegistrationCaptor.getAllValues();

		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.0.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.0.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.2.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.2.RELEASE"), ApplicationType.sink));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.sink));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testImportMixedVersionsWithMixedOrder() {

		final boolean overwrite = true;

		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq("foo"), eq(ApplicationType.source), eq("1.0"))).thenReturn(appRegistration());

		appRegistryService.importAll(!overwrite,
				new ClassPathResource("AppRegistryTests-importMixedVersions5.properties", getClass()));

		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(4)).save(appRegistrationCaptor.capture());

		List<AppRegistration> registrations = appRegistrationCaptor.getAllValues();

		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.0.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.0.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.2.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.2.RELEASE"), ApplicationType.sink));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.sink));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testImportMixedVersionsWithMissingAndOnlyMetadata() {

		final boolean overwrite = true;

		when(appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				eq("foo"), eq(ApplicationType.source), eq("1.0"))).thenReturn(appRegistration());

		appRegistryService.importAll(!overwrite,
				new ClassPathResource("AppRegistryTests-importMixedVersions6.properties", getClass()));

		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(3)).save(appRegistrationCaptor.capture());

		List<AppRegistration> registrations = appRegistrationCaptor.getAllValues();
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.1.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.1.RELEASE"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("time", URI.create("maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.0.RELEASE"), null, ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("log", URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.2.RELEASE"), URI.create("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.2.RELEASE"), ApplicationType.sink));
	}

	@Test
	public void testImportAllDockerLatest() {

		appRegistryService.importAll(false,
				new ClassPathResource("AppRegistryTests-importAll-docker-latest.properties", getClass()));

		ArgumentCaptor<AppRegistration> appRegistrationCaptor = ArgumentCaptor.forClass(AppRegistration.class);
		verify(appRegistrationRepository, times(2)).save(appRegistrationCaptor.capture());

		List<AppRegistration> registrations = appRegistrationCaptor.getAllValues();

		assertThat(registrations).haveAtLeastOne(appRegistrationWith("foo", URI.create("docker:springcloudstream/foo-source-kafka:latest"), URI.create("maven://org.springframework.cloud.stream.app:foo-source-kafka:jar:metadata:2.1.2.BUILD-SNAPSHOT"), ApplicationType.source));
		assertThat(registrations).haveAtLeastOne(appRegistrationWith("foo", URI.create("docker:springcloudstream/foo-sink-kafka:latest"), URI.create("maven://org.springframework.cloud.stream.app:foo-sink-kafka:jar:metadata:2.1.2.BUILD-SNAPSHOT"), ApplicationType.sink));
	}

	@Test
	public void testDelete() throws URISyntaxException {
		AppRegistration fooSource = appRegistration("foo", ApplicationType.source, true);
		appRegistryService.delete(fooSource.getName(), fooSource.getType(), fooSource.getVersion());
		verify(appRegistrationRepository, times(1))
				.deleteAppRegistrationByNameAndTypeAndVersion(
						eq(fooSource.getName()), eq(fooSource.getType()), eq(fooSource.getVersion()));
	}

	@Test
	public void testDeleteAll() throws URISyntaxException {
		List<AppRegistration> appsToDelete = Collections.emptyList();
		appRegistryService.deleteAll(appsToDelete);
		verify(appRegistrationRepository, times(1)).deleteAll(appsToDelete);
	}

	@Test
	public void testMultipleBootVersions() {
		// given
		Resource resource = new ClassPathResource("AppRegistryTests-importMultipleBootVersions.properties", getClass());
		// when
		List<AppRegistration> result = appRegistryService.importAll(false, resource);
		// then
		List<AppRegistration> boot2 = result.stream().filter(r -> r.getBootVersion().equals(AppBootSchemaVersion.BOOT2)).collect(Collectors.toList());
		List<AppRegistration> boot3 = result.stream().filter(r -> r.getBootVersion().equals(AppBootSchemaVersion.BOOT3)).collect(Collectors.toList());
		assertEquals(1L, boot2.size());
		assertEquals(1L, boot3.size());
		assertEquals("2.0.1", boot2.get(0).getVersion());
		assertEquals("3.0.0", boot3.get(0).getVersion());
	}
	@Test
	public void testMultipleBootVersionsExpectError() {
		// given
		Resource resource = new ClassPathResource("AppRegistryTests-importInvalidBootVersions.properties", getClass());
		// when
		try {
			appRegistryService.importAll(false, resource);
			fail("Expected Exception");
		} catch (IllegalArgumentException x) {
			// then
			assertTrue(x.toString().contains("Invalid"));
		}
	}
	@Test
	public void testBootVersionsMissingURI() {
		// given
		Resource resource = new ClassPathResource("AppRegistryTests-importBootVersionsMissingURI.properties", getClass());
		// when
		try {
			appRegistryService.importAll(false, resource);
			fail("Expected Exception");
		} catch (IllegalArgumentException x) {
			// then
			assertNotNull(x.getMessage());
			System.out.println("Exception:" + x.getMessage());
			assertTrue(x.getMessage().startsWith("Expected uri for bootVersion") || x.getMessage().startsWith("Expected previous to be same type and name for"));
		}
	}

	private AppRegistration appRegistration() {
		return appRegistration("foo", ApplicationType.source, true);
	}

	private AppRegistration appRegistration(String name, ApplicationType type, boolean withMetadata) {
		AppRegistration registration = new AppRegistration();
		registration.setName(name);
		registration.setType(type);
		registration.setUri(URI.create("classpath:/" + name + "-" + type));
		if (withMetadata) {
			registration.setMetadataUri(URI.create("classpath:/" + name + "-" + type + "-metadata"));
		}
		registration.setVersion("6.6.6");
		return registration;
	}
}
