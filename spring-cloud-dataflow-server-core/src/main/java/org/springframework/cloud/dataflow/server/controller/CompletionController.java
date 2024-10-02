/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.List;

import jakarta.validation.constraints.Min;

import org.springframework.cloud.dataflow.completion.CompletionProposal;
import org.springframework.cloud.dataflow.completion.StreamCompletionProvider;
import org.springframework.cloud.dataflow.completion.TaskCompletionProvider;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the DSL completion features of {@link StreamCompletionProvider} and
 * {@link TaskCompletionProvider} as a REST API.
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
	 * Create a controller for the provided {@link StreamCompletionProvider} and
	 * {@link TaskCompletionProvider}.
	 *
	 * @param completionProvider the stream completion provider
	 * @param taskCompletionProvider the task completion provider
	 */
	public CompletionController(StreamCompletionProvider completionProvider,
			TaskCompletionProvider taskCompletionProvider) {
		this.completionProvider = completionProvider;
		this.taskCompletionProvider = taskCompletionProvider;
	}

	/**
	 * Return a list of possible completions given a prefix string that the user has
	 * started typing.
	 *
	 * @param start the amount of text written so far
	 * @param detailLevel the level of detail the user wants in completions, starting at
	 * 1. Higher values request more detail, with values typically in the range [1..5]
	 * @return the list of completion proposals
	 */
	@RequestMapping("/stream")
	public CompletionProposalsResource completions(@RequestParam String start,
			@RequestParam(defaultValue = "1") @Min(value = 1, message = "The provided detail level must be greater than zero.") int detailLevel) {
		return assembler.toModel(completionProvider.complete(start, detailLevel));
	}

	/**
	 * Return a list of possible completions given a prefix string that the user has
	 * started typing.
	 *
	 * @param start the amount of text written so far
	 * @param detailLevel the level of detail the user wants in completions, starting at
	 * 1. Higher values request more detail, with values typically in the range [1..5]
	 * @return the list of completion proposals
	 */
	@RequestMapping("/task")
	public CompletionProposalsResource taskCompletions(@RequestParam String start,
			@RequestParam(defaultValue = "1") @Min(value = 1, message = "The provided detail level must be greater than zero.") int detailLevel) {
		return assembler.toModel(taskCompletionProvider.complete(start, detailLevel));
	}

	/**
	 * {@link org.springframework.hateoas.server.ResourceAssembler} implementation that converts
	 * {@link CompletionProposal}s to {@link CompletionProposalsResource}s.
	 */
	static class Assembler extends RepresentationModelAssemblerSupport<List<CompletionProposal>, CompletionProposalsResource> {

		public Assembler() {
			super(CompletionController.class, CompletionProposalsResource.class);
		}

		@Override
		public CompletionProposalsResource toModel(List<CompletionProposal> proposals) {
			CompletionProposalsResource result = new CompletionProposalsResource();
			for (CompletionProposal proposal : proposals) {
				result.addProposal(proposal.getText(), proposal.getExplanation());
			}
			return result;
		}
	}
}
