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
package org.springframework.cloud.skipper.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Main UI root controller. For now it's only task is to redirect the root URL to the Rest
 * API endpoint {@code /api }
 *
 * @author Gunnar Hillert
 */
@Controller
public class RootController {

	/**
	 * Handles the root URL of Skipper. Redirects users to the REST API entry point at
	 * {@code /api }.
	 *
	 * @return RedirectView to api
	 */
	@RequestMapping("/")
	public RedirectView index() {
		return new RedirectView("/api");
	}

}
