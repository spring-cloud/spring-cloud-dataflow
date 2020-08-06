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
package org.springframework.cloud.dataflow.completion;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.core.dsl.Token;
import org.springframework.cloud.dataflow.core.dsl.TokenKind;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.core.io.Resource;

/**
 * Support class to be used by various strategies to gather {@link CompletionProposal}s
 *
 * @see RecoveryStrategy
 * @see ExpansionStrategy
 *
 * @author Oleg Zhurakousky
 */
class ProposalsCollectorSupportUtils {

	private final AppRegistryService appRegistry;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	ProposalsCollectorSupportUtils(AppRegistryService appRegistry, ApplicationConfigurationMetadataResolver metadataResolver) {
		this.appRegistry = appRegistry;
		this.metadataResolver = metadataResolver;
	}

	void addPropertiesProposals(String text, String startsWith, AppRegistration appRegistration, Set<String> alreadyPresentOptions, List<CompletionProposal> collector, int detailLevel){
		Resource metadataResource = appRegistry.getAppMetadataResource(appRegistration);
		// For visible properties, use their simple name
		if (metadataResource != null) {
			CompletionProposal.Factory proposals = CompletionProposal.expanding(text);
			for (ConfigurationMetadataProperty property : metadataResolver.listProperties(metadataResource)) {
				String name = property.getName();
				if (!alreadyPresentOptions.contains(name) && name.startsWith(startsWith)) {
					collector.add(proposals
							.withSeparateTokens("--" + property.getName() + "=", property.getShortDescription()));
				}
			}
			// For other properties (including visible in full form), use their id
			if (detailLevel > 1) {
				for (ConfigurationMetadataProperty property : metadataResolver.listProperties(metadataResource, true)) {
					String id = property.getId();
					if (!alreadyPresentOptions.contains(id) && id.startsWith(startsWith)) {
						collector.add(proposals
								.withSeparateTokens("--" + property.getId() + "=", property.getShortDescription()));
					}
				}
			}
		}
	}

	void addValueHintsProposals(final String dsl, AppRegistration appRegistration, final List<CompletionProposal> collector, final String propertyName, final ValueHintProvider[] valueHintProviders){
		final Resource metadataResource = this.appRegistry.getAppMetadataResource(appRegistration);
		if (metadataResource != null) {
			final URLClassLoader classLoader = metadataResolver.createAppClassLoader(metadataResource);
			this.doWithClassLoader(classLoader, () -> {
				CompletionProposal.Factory proposals = CompletionProposal.expanding(dsl);
				List<ConfigurationMetadataProperty> visible = metadataResolver.listProperties(metadataResource);
				for (ConfigurationMetadataProperty property : metadataResolver.listProperties(metadataResource, true)) {
					if (CompletionUtils.isMatchingProperty(propertyName, property, visible)) {
						for (ValueHintProvider valueHintProvider : valueHintProviders) {
							for (ValueHint valueHint : valueHintProvider.generateValueHints(property, classLoader)) {
								collector.add(proposals.withSuffix(String.valueOf(valueHint.getValue()),
										valueHint.getShortDescription()));
							}
						}
					}
				}
				return null;
			});
		}
	}

	boolean addAlreadyTypedValueHintsProposals(final String text, AppRegistration appRegistration, final List<CompletionProposal> collector, final String propertyName, final ValueHintProvider[] valueHintProviders, final String alreadyTyped){
		final Resource metadataResource = this.appRegistry.getAppMetadataResource(appRegistration);
		boolean result = false;
		if (metadataResource == null) {
			result = false;
		} else {
			final URLClassLoader classLoader = metadataResolver.createAppClassLoader(metadataResource);
			result =  this.doWithClassLoader(classLoader, () -> {
				CompletionProposal.Factory proposals = CompletionProposal.expanding(text);
				List<ConfigurationMetadataProperty> allProps = metadataResolver.listProperties(metadataResource, true);
				List<ConfigurationMetadataProperty> visibleProps = metadataResolver.listProperties(metadataResource);
				for (ConfigurationMetadataProperty property : allProps) {
					if (CompletionUtils.isMatchingProperty(propertyName, property, visibleProps)) {
						for (ValueHintProvider valueHintProvider : valueHintProviders) {
							List<ValueHint> valueHints = valueHintProvider.generateValueHints(property, classLoader);
							if (!valueHints.isEmpty() && valueHintProvider.isExclusive(property)) {
								collector.clear();
							}
							for (ValueHint valueHint : valueHints) {
								String candidate = String.valueOf(valueHint.getValue());
								if (!candidate.equals(alreadyTyped) && candidate.startsWith(alreadyTyped)) {
									collector.add(proposals.withSuffix(candidate.substring(alreadyTyped.length()),
											valueHint.getShortDescription()));
								}
							}
							if (!valueHints.isEmpty() && valueHintProvider.isExclusive(property)) {
								return true;
							}
						}
					}
				}
				return false;
			});
		}
		return result;
	}

	AppRegistration findAppRegistration(String appName, ApplicationType... appTypes){
		AppRegistration lastAppRegistration = null;
		for (ApplicationType appType : appTypes) {
			lastAppRegistration = this.appRegistry.find(appName, appType);
			if (lastAppRegistration != null) {
				return lastAppRegistration;
			}
		}
		return null;
	}

	static String computeStartsWith(CheckPointedParseException exception){
		List<Token> tokens = exception.getTokens();
		int tokenPointer = tokens.size() - 1;
		while (!tokens.get(tokenPointer - 1).isKind(TokenKind.DOUBLE_MINUS)) {
			tokenPointer--;
		}
		StringBuilder startsWithBuffer = null;
		for (startsWithBuffer = new StringBuilder(); tokenPointer < tokens.size(); tokenPointer++) {
			Token t = tokens.get(tokenPointer);
			if (t.isIdentifier()) {
				startsWithBuffer.append(t.stringValue());
			}
			else {
				startsWithBuffer.append(t.getKind().getTokenChars());
			}
		}
		String startsWith = startsWithBuffer.toString();
		return startsWith;
	}

	/**
	 * Emulates 'try-with-resources' by closing class loader (ignoring any exceptions) after executing callback.
	 */
	private <T> T doWithClassLoader(URLClassLoader classLoader, Callback<T> callback) {
		try {
			return callback.invoke();
		}
		finally {
			try {
				classLoader.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
	}

	private static interface Callback<T> {
		T invoke();
	}
}
