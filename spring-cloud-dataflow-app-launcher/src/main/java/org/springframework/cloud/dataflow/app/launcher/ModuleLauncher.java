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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.dataflow.app.resolver.Coordinates;
import org.springframework.cloud.dataflow.app.resolver.ModuleResolver;
import org.springframework.cloud.dataflow.app.utils.ClassloaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A component that launches one or more modules, delegating their resolution to an
 * underlying {@link ModuleResolver}.
 *
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Eric Bottard
 */
public class ModuleLauncher {

	public static final String AGGREGATE_APPLICATION_CLASS = "org.springframework.cloud.stream.aggregate.AggregateApplication";

	public static final String AGGREGATE_APPLICATION_RUN_METHOD = "run";

	public static final String MODULE_AGGREGATOR_RUNNER_THREAD_NAME = "module-aggregator-runner";

	private static Log log = LogFactory.getLog(ModuleLauncher.class);

	private static final String DEFAULT_EXTENSION = "jar";

	private static final String DEFAULT_CLASSIFIER = "";

	private static final Pattern COORDINATES_PATTERN =
			Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");

	private static final String INCLUDE_DEPENDENCIES_ARG = "includes";

	private static final String EXCLUDE_DEPENDENCIES_ARG = "excludes";

	private final ModuleResolver moduleResolver;

	/**
	 * Creates a module launcher using the provided module resolver
	 * @param moduleResolver the module resolver instance to use
	 */
	public ModuleLauncher(ModuleResolver moduleResolver) {
		this.moduleResolver = moduleResolver;
	}

	/**
	 * Launches one or more modules, with the corresponding arguments, if any.
	 *
	 * Modules must be passed in "natural" left to right order.
	 *
	 * The format of each module must conform to the <a href="http://www.eclipse.org/aether">Aether</a> convention:
	 * <code>&lt;groupId&gt;:&lt;artifactId&gt;[:&lt;extension&gt;[:&lt;classifier&gt;]]:&lt;version&gt;</code>
	 *
	 * @param moduleLaunchRequests a list of modules with their arguments
	 * @param aggregate whether the modules should be aggregated at launch
	 * @param aggregateArgs a list of arguments for the whole aggregate
	 */
	@SuppressWarnings("unchecked")
	public void launch(List<ModuleLaunchRequest> moduleLaunchRequests, boolean aggregate, Map<String,String> aggregateArgs) {
		if (moduleLaunchRequests.size() == 1 || !aggregate) {
			launchIndividualModules(moduleLaunchRequests);
		}
		else {
			launchAggregatedModules(moduleLaunchRequests, aggregateArgs != null ? aggregateArgs : Collections.EMPTY_MAP);
		}
	}

	public void launch(List<ModuleLaunchRequest> moduleLaunchRequests, boolean aggregate) {
		this.launch(moduleLaunchRequests, aggregate, Collections.<String,String>emptyMap());
	}

	public void launch(List<ModuleLaunchRequest> moduleLaunchRequests) {
		this.launch(moduleLaunchRequests, false);
	}

	/**
	 * Converts a set of semantic program arguments to "command line program arguments" that is, to the
	 * {@literal --foo=bar} form.
	 */
	private String[] toArgArray(Map<String, String> args) {
		if (args != null) {
			String[] result = new String[args.size()];
			int i = 0;
			for (Map.Entry<String, String> kv : args.entrySet()) {
				result[i++] = String.format("--%s=%s", kv.getKey(), kv.getValue());
			}
			return result;
		}
		else {
			return new String[0];
		}
	}

	public void launchAggregatedModules(List<ModuleLaunchRequest> moduleLaunchRequests, Map<String,String> aggregateArgs) {
		try {
			List<String> mainClassNames = new ArrayList<>();
			LinkedHashSet<URL> jarURLs = new LinkedHashSet<>();
			List<String> seenArchives = new ArrayList<>();
			final List<String[]> arguments = new ArrayList<>();
			final ClassLoader classLoader;
			if (!(aggregateArgs.containsKey(EXCLUDE_DEPENDENCIES_ARG) ||
					aggregateArgs.containsKey(INCLUDE_DEPENDENCIES_ARG))) {
				for (ModuleLaunchRequest moduleLaunchRequest : moduleLaunchRequests) {
					Resource resource = resolveModule(moduleLaunchRequest.getModule());
					JarFileArchive jarFileArchive = new JarFileArchive(resource.getFile());
					jarURLs.add(jarFileArchive.getUrl());
					for (Archive archive : jarFileArchive.getNestedArchives(ArchiveMatchingEntryFilter.FILTER)) {
						// avoid duplication based on unique JAR names
						String urlAsString = archive.getUrl().toString();
						String jarNameWithExtension = urlAsString.substring(0, urlAsString.lastIndexOf("!/"));
						String jarNameWithoutExtension =
								jarNameWithExtension.substring(jarNameWithExtension.lastIndexOf("/") + 1);
						if (!seenArchives.contains(jarNameWithoutExtension)) {
							seenArchives.add(jarNameWithoutExtension);
							jarURLs.add(archive.getUrl());
						}
					}
					mainClassNames.add(jarFileArchive.getMainClass());
					arguments.add(toArgArray(moduleLaunchRequest.getArguments()));
				}
				classLoader = ClassloaderUtils.createModuleClassloader(jarURLs.toArray(new URL[jarURLs.size()]));
			} else {
				// First, resolve modules and extract main classes - while slightly less efficient than just
				// doing the same processing after resolution, this ensures that module artifacts are processed
				// correctly for extracting their main class names. It is not possible in the general case to
				// identify, after resolution, whether a resource represents a module artifact which was part of the
				// original request or not. We will include the first module as root and the next as direct dependencies
				Coordinates root = null;
				ArrayList<Coordinates> includeCoordinates = new ArrayList<>();
				for (ModuleLaunchRequest moduleLaunchRequest : moduleLaunchRequests) {
					Coordinates moduleCoordinates
							= toCoordinates(moduleLaunchRequest.getModule());
					if (root == null) {
						root = moduleCoordinates;
					}
					else {
						includeCoordinates.add(toCoordinates(moduleLaunchRequest.getModule()));
					}
					Resource moduleResource = resolveModule(moduleLaunchRequest.getModule());
					JarFileArchive moduleArchive = new JarFileArchive(moduleResource.getFile());
					mainClassNames.add(moduleArchive.getMainClass());
					arguments.add(toArgArray(moduleLaunchRequest.getArguments()));
				}
				for (String include :
						StringUtils.commaDelimitedListToStringArray(aggregateArgs.get(INCLUDE_DEPENDENCIES_ARG))) {
					includeCoordinates.add(toCoordinates(include));
				}
				// Resolve all artifacts - since modules have been specified as direct dependencies, they will take
				// precedence in the resolution order, ensuring that the already resolved artifacts will be returned as
				// part of the response.
				Resource[] libraries = moduleResolver.resolve(root,
						includeCoordinates.toArray(new Coordinates[includeCoordinates.size()]),
						StringUtils.commaDelimitedListToStringArray(aggregateArgs.get(EXCLUDE_DEPENDENCIES_ARG)));
				for (Resource library : libraries) {
					jarURLs.add(library.getURL());
				}
				classLoader = new URLClassLoader(jarURLs.toArray(new URL[jarURLs.size()]));
			}

			final List<Class<?>> mainClasses = new ArrayList<>();
			for (String mainClass : mainClassNames) {
				mainClasses.add(ClassUtils.forName(mainClass, classLoader));
			}
			Runnable moduleAggregatorRunner = new ModuleAggregatorRunner(classLoader, mainClasses,
					toArgArray(aggregateArgs), arguments);
			Thread moduleAggregatorRunnerThread = new Thread(moduleAggregatorRunner);
			moduleAggregatorRunnerThread.setContextClassLoader(classLoader);
			moduleAggregatorRunnerThread.setName(MODULE_AGGREGATOR_RUNNER_THREAD_NAME);
			moduleAggregatorRunnerThread.start();
		} catch (Exception e) {
			throw new RuntimeException("failed to start aggregated modules: " +
					StringUtils.collectionToCommaDelimitedString(moduleLaunchRequests), e);
		}
	}

	public void launchIndividualModules(List<ModuleLaunchRequest> moduleLaunchRequests) {
		List<ModuleLaunchRequest> reversed = new ArrayList<>(moduleLaunchRequests);
		Collections.reverse(reversed);
		for (ModuleLaunchRequest moduleLaunchRequest : reversed) {
			String module = moduleLaunchRequest.getModule();
			Map<String, String> arguments = moduleLaunchRequest.getArguments();
			if (arguments.containsKey(INCLUDE_DEPENDENCIES_ARG) || arguments.containsKey(EXCLUDE_DEPENDENCIES_ARG)) {
				String includes = arguments.get(INCLUDE_DEPENDENCIES_ARG);
				String excludes = arguments.get(EXCLUDE_DEPENDENCIES_ARG);
				launchModuleWithDependencies(module, toArgArray(arguments),
						StringUtils.commaDelimitedListToStringArray(includes),
						StringUtils.commaDelimitedListToStringArray(excludes));
			} else {
				launchModule(module, toArgArray(arguments));
			}
		}
	}

	private void launchModule(String module, String[] args) {
		try {
			Resource resource = resolveModule(module);
			JarFileArchive jarFileArchive = new JarFileArchive(resource.getFile());
			ModuleJarLauncher jarLauncher = new ModuleJarLauncher(jarFileArchive);
			jarLauncher.launch(args);
		}
		catch (IOException e) {
			throw new RuntimeException("failed to launch module: " + module, e);
		}
	}

	private void launchModuleWithDependencies(String module, String[] args, String[] includes, String[] excludes) {
		try {
			Resource[] libraries = this.moduleResolver.resolve(toCoordinates(module),
					toCoordinateArray(includes), excludes);
			List<Archive> archives = new ArrayList<>();
			for (Resource library : libraries) {
				archives.add(new JarFileArchive(library.getFile()));
			}
			MultiArchiveLauncher jarLauncher = new MultiArchiveLauncher(archives);
			jarLauncher.launch(args);
		}
		catch (IOException e) {
			throw new RuntimeException("failed to launch module: " + module, e);
		}
	}

	private Resource resolveModule(String moduleCoordinates) {
		Coordinates coordinates = toCoordinates(moduleCoordinates);
		return this.moduleResolver.resolve(coordinates);
	}

	private Coordinates toCoordinates(String coordinates) {
		Matcher matcher = COORDINATES_PATTERN.matcher(coordinates);
		Assert.isTrue(matcher.matches(), "Bad artifact coordinates " + coordinates
				+ ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
		String groupId = matcher.group(1);
		String artifactId = matcher.group(2);
		String extension = StringUtils.hasLength(matcher.group(4)) ? matcher.group(4) : DEFAULT_EXTENSION;
		String classifier = StringUtils.hasLength(matcher.group(6)) ? matcher.group(6) : DEFAULT_CLASSIFIER;
		String version = matcher.group(7);
		return new Coordinates(groupId, artifactId, extension, classifier, version);
	}

	private Coordinates[] toCoordinateArray(String[] coordinateList) {
		List<Coordinates> result = new ArrayList<>();
		for (String coordinates : coordinateList) {
			result.add(toCoordinates(coordinates));
		}
		return result.toArray(new Coordinates[result.size()]);
	}

	private class ModuleAggregatorRunner implements Runnable {

		private final ClassLoader classLoader;

		private final String[] parentArgs;

		private final List<Class<?>> mainClasses;

		private final List<String[]> arguments;

		public ModuleAggregatorRunner(ClassLoader classLoader, List<Class<?>> mainClasses, String[] parentArgs,
									  List<String[]> moduleArguments) {
			this.classLoader = classLoader;
			this.parentArgs = parentArgs;
			this.mainClasses = mainClasses;
			this.arguments = moduleArguments;
		}

		@Override
		public void run() {
			try {
				// we expect the class and method to be found on the module classpath
				Class<?> moduleAggregatorClass = ClassUtils.forName(AGGREGATE_APPLICATION_CLASS, classLoader);
				Method aggregateMethod = ReflectionUtils.findMethod(moduleAggregatorClass,
						AGGREGATE_APPLICATION_RUN_METHOD, Class[].class, String[].class, String[][].class);
				aggregateMethod.invoke(null,
						mainClasses.toArray(new Class<?>[mainClasses.size()]),
						parentArgs, arguments.toArray(new String[][] {}));
			} catch (Exception e) {
				log.error("failed to launch aggregated modules :"
						+ StringUtils.collectionToCommaDelimitedString(mainClasses), e);
				throw new RuntimeException(e);
			}
		}
	}
}
