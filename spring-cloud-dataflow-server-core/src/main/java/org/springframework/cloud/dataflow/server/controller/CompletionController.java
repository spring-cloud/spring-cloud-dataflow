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

package org.springframework.cloud.dataflow.server.controller;

import java.util.List;

import javax.validation.constraints.Min;

import org.springframework.cloud.dataflow.completion.CompletionProposal;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.completion.TaskCompletionProvider;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the DSL completion features of {@link StreamCompletionProvider} and {@link TaskCompletionProvider} as a REST API.
 *
 * @author Eric Bottard
 * @author Andy Clement
 */
@RestController
@Validated
@RequestMapping("/completions")
@ExposesResourceFor(CompletionProposalsResource.class)
public class CompletionController {

	private final StreamCompletionProvider completionProvider;

	private final TaskCompletionProvider taskCompletionProvider;

	private final Assembler assembler = new Assembler();

	/**
	 * Create a controller for the provided {@link StreamCompletionProvider} and {@link TaskCompletionProvider}.
	 */
	public CompletionController(StreamCompletionProvider completionProvider, TaskCompletionProvider taskCompletionProvider) {
		this.completionProvider = completionProvider;
		this.taskCompletionProvider = taskCompletionProvider;
	}

	/**
	 * Return a list of possible completions given a prefix string that the user has started typing.
	 *
	 * @param start the amount of text written so far
	 * @param detailLevel the level of detail the user wants in completions, starting at 1.
	 * Higher values request more detail, with values typically in the range [1..5]
	 */
	@RequestMapping(value = "/stream")
	public CompletionProposalsResource completions(
			@RequestParam("start") String start,
			@RequestParam(value = "detailLevel", defaultValue = "1")
			@Min(value=1, message="The provided detail level must be greater than zero.") int detailLevel) {
		return assembler.toResource(completionProvider.complete(start, detailLevel));
	}

	/**
	 * Return a list of possible completions given a prefix string that the user has started typing.
	 *
	 * @param start the amount of text written so far
	 * @param detailLevel the level of detail the user wants in completions, starting at 1.
	 * Higher values request more detail, with values typically in the range [1..5]
	 */
	@RequestMapping(value = "/task")
	public CompletionProposalsResource taskCompletions(
			@RequestParam("start") String start,
			@RequestParam(value = "detailLevel", defaultValue = "1")
			@Min(value=1, message="The provided detail level must be greater than zero.") int detailLevel) {
		return assembler.toResource(taskCompletionProvider.complete(start, detailLevel));
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link CompletionProposal}s to {@link CompletionProposalsResource}s.
	 */
	static class Assembler extends ResourceAssemblerSupport<List<CompletionProposal>, CompletionProposalsResource> {

		public Assembler() {
			super(CompletionController.class, CompletionProposalsResource.class);
		}

		@Override
		public CompletionProposalsResource toResource(List<CompletionProposal> proposals) {
			CompletionProposalsResource result = new CompletionProposalsResource();
			for (CompletionProposal proposal : proposals) {
				result.addProposal(proposal.getText(), proposal.getExplanation());
			}
			return result;
		}
	}
}
