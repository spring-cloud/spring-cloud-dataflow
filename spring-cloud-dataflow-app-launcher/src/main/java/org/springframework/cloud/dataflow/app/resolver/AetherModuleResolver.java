/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.app.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.repository.DefaultProxySelector;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * An implementation of ModuleResolver using <a href="http://www.eclipse.org/aether/>aether</a> to resolve the module
 * artifact (uber jar) in a local Maven repository, downloading the latest update from a remote repository if
 * necessary.
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
public class AetherModuleResolver implements ModuleResolver {

	private static final Log log = LogFactory.getLog(AetherModuleResolver.class);

	private static final String DEFAULT_CONTENT_TYPE = "default";

	private final File localRepository;

	private final List<RemoteRepository> remoteRepositories;

	private final RepositorySystem repositorySystem;

	private volatile boolean offline = false;

	private final AetherProxyProperties proxyProperties;

	private Authentication authentication;

	/**
	 * Create an instance specifying the locations of the local and remote repositories.
	 * @param localRepository the root path of the local maven repository
	 * @param remoteRepositories a Map containing pairs of (repository ID,repository URL). This
	 * may be null or empty if the local repository is off line.
	 * @param proxyProperties the proxy properties for the maven proxy settings.
	 */
	public AetherModuleResolver(File localRepository, Map<String, String> remoteRepositories,
			final AetherProxyProperties proxyProperties) {
		Assert.notNull(localRepository, "Local repository path cannot be null");
		if (log.isDebugEnabled()) {
			log.debug("Local repository: " + localRepository);
			if (!CollectionUtils.isEmpty(remoteRepositories)) {
				// just listing the values, ids are simply informative
				log.debug("Remote repositories: " + StringUtils.collectionToCommaDelimitedString(remoteRepositories.values()));
			}
		}
		this.proxyProperties = proxyProperties;
		if (isProxyEnabled() && proxyHasCredentials()) {
			this.authentication = new Authentication() {
				@Override
				public void fill(AuthenticationContext context, String key, Map<String, String> data) {
					context.put(context.USERNAME, proxyProperties.getAuth().getUsername());
					context.put(context.PASSWORD, proxyProperties.getAuth().getPassword());
				}

				@Override
				public void digest(AuthenticationDigest digest) {
					digest.update(AuthenticationContext.USERNAME, proxyProperties.getAuth().getUsername(),
							AuthenticationContext.PASSWORD, proxyProperties.getAuth().getPassword());
				}
			};
		}
		if (!localRepository.exists()) {
			Assert.isTrue(localRepository.mkdirs(),
					"Unable to create directory for local repository: " + localRepository);
		}
		this.localRepository = localRepository;
		this.remoteRepositories = new LinkedList<>();
		if (!CollectionUtils.isEmpty(remoteRepositories)) {
			for (Map.Entry<String, String> remoteRepo : remoteRepositories.entrySet()) {
				RemoteRepository.Builder remoteRepositoryBuilder = new RemoteRepository.Builder(remoteRepo.getKey(),
						DEFAULT_CONTENT_TYPE, remoteRepo.getValue());
				if (isProxyEnabled()) {
					if(this.authentication != null) {
						//todo: Set direct authentication for the remote repositories
						remoteRepositoryBuilder.setProxy(new Proxy(proxyProperties.getProtocol(), proxyProperties.getHost(),
								proxyProperties.getPort(), authentication));	
					}
					else {
						//If proxy doesn't need authentication to use it
						remoteRepositoryBuilder.setProxy(new Proxy(proxyProperties.getProtocol(), proxyProperties.getHost(),
								proxyProperties.getPort()));						
					}
				}
				this.remoteRepositories.add(remoteRepositoryBuilder.build());
			}
		}
		repositorySystem = newRepositorySystem();
	}

	/**
	 * Check if the proxy settings are provided.
	 *
	 * @return boolean true if the proxy settings are provided.
	 */
	private boolean isProxyEnabled() {
		return (this.proxyProperties != null && this.proxyProperties.getHost() != null && proxyProperties.getPort() > 0);
	}

	/**
	 * Check if the proxy setting has username/password set.
	 *
	 * @return boolean true if both the username/password are set
	 */
	private boolean proxyHasCredentials() {
		return (this.proxyProperties != null && this.proxyProperties.getAuth() != null &&
				this.proxyProperties.getAuth().getUsername() != null && this.proxyProperties.getAuth().getPassword() != null);
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	/**
	 * Resolve an artifact and return its location in the local repository. Aether performs the normal
	 * Maven resolution process ensuring that the latest update is cached to the local repository.
	 * @param coordinates the Maven coordinates of the artifact
	 * @return a {@link FileSystemResource} representing the resolved artifact in the local repository
	 * @throws RuntimeException if the artifact does not exist or the resolution fails
	 */
	@Override
	public Resource resolve(Coordinates coordinates) {
		return this.resolve(coordinates, null, null)[0];
	}

	/*
	 * Create a session to manage remote and local synchronization.
	 */
	private DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system, String localRepoPath) {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		LocalRepository localRepo = new LocalRepository(localRepoPath);
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
		session.setOffline(this.offline);
		if (isProxyEnabled()) {
			DefaultProxySelector proxySelector = new DefaultProxySelector();
			Proxy proxy = new Proxy(proxyProperties.getProtocol(), proxyProperties.getHost(), proxyProperties.getPort(),
					authentication);
			proxySelector.add(proxy, proxyProperties.getNonProxyHosts());
			session.setProxySelector(proxySelector);
		}
		return session;
	}

	/*
	 * Aether's components implement {@link org.eclipse.aether.spi.locator.Service} to ease manual wiring.
	 * Using the prepopulated {@link DefaultServiceLocator}, we need to register the repository connector 
	 * and transporter factories
	 */
	private RepositorySystem newRepositorySystem() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
			@Override
			public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
				throw new RuntimeException(exception);
			}
		});
		return locator.getService(RepositorySystem.class);
	}


	/**
	 * Resolve a set of artifacts based on their coordinates, including their dependencies, and return the locations of
	 * the transitive set in the local repository. Aether performs the normal Maven resolution process ensuring that the
	 * latest update is cached to the local repository. A number of additional includes and excludes can be specified,
	 * allowing to override the transitive dependencies of the original set. Includes and their transitive dependencies
	 * will always
	 *
	 * @param root the Maven coordinates of the artifacts
	 * @return a {@link FileSystemResource} representing the resolved artifact in the local repository
	 * @throws RuntimeException if the artifact does not exist or the resolution fails
	 */
	@Override
	public Resource[] resolve(Coordinates root, Coordinates[] includes, String[] excludePatterns) {
		Assert.notNull(root, "Root cannot be null");
		validateCoordinates(root);
		if (!ObjectUtils.isEmpty(includes)) {
			for (Coordinates include : includes) {
				Assert.notNull(include, "Includes cannot be null");
				validateCoordinates(include);
			}
		}
		List<Resource> result = new ArrayList<>();
		Artifact rootArtifact = toArtifact(root);
		RepositorySystemSession session = newRepositorySystemSession(repositorySystem,
				localRepository.getAbsolutePath());
		if (ObjectUtils.isEmpty(includes) && ObjectUtils.isEmpty(excludePatterns)) {
			ArtifactResult resolvedArtifact;
			try {
				resolvedArtifact = repositorySystem.resolveArtifact(session,
						new ArtifactRequest(rootArtifact, remoteRepositories, JavaScopes.RUNTIME));
			}
			catch (ArtifactResolutionException e) {
				throw new RuntimeException(e);
			}
			result.add(toResource(resolvedArtifact));
		}
		else {
			try {
				CollectRequest collectRequest = new CollectRequest();
				collectRequest.setRepositories(remoteRepositories);
				collectRequest.setRoot(new Dependency(rootArtifact, JavaScopes.RUNTIME));
				Artifact[] includeArtifacts = new Artifact[!ObjectUtils.isEmpty(includes) ? includes.length : 0];
				int i = 0;
				for (Coordinates include : includes) {
					Artifact includedArtifact = toArtifact(include);
					collectRequest.addDependency(new Dependency(includedArtifact, JavaScopes.RUNTIME));
					includeArtifacts[i++] = includedArtifact;
				}
				DependencyResult dependencyResult =
						repositorySystem.resolveDependencies(session,
								new DependencyRequest(collectRequest,
										new ModuleDependencyFilter(includeArtifacts, excludePatterns)));
				for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
					// we are only interested in the jars
					if ("jar".equalsIgnoreCase(artifactResult.getArtifact().getExtension())) {
						result.add(toResource(artifactResult));
					}
				}
			}
			catch (DependencyResolutionException e) {
				throw new RuntimeException(e);
			}
		}
		return result.toArray(new Resource[result.size()]);
	}

	private void validateCoordinates(Coordinates coordinates) {
		Assert.hasText(coordinates.getGroupId(), "'groupId' cannot be blank.");
		Assert.hasText(coordinates.getArtifactId(), "'artifactId' cannot be blank.");
		Assert.hasText(coordinates.getExtension(), "'extension' cannot be blank.");
		Assert.hasText(coordinates.getVersion(), "'version' cannot be blank.");
	}

	public FileSystemResource toResource(ArtifactResult resolvedArtifact) {
		return new FileSystemResource(resolvedArtifact.getArtifact().getFile());
	}

	private Artifact toArtifact(Coordinates root) {
		return new DefaultArtifact(root.getGroupId(),
				root.getArtifactId(),
				root.getClassifier() != null ? root.getClassifier() : "",
				root.getExtension(),
				root.getVersion());
	}
}
