/*
 * Copyright 2015-2024 the original author or authors.
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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A simple (web, not REST) controller to trigger a redirect to the index page of the
 * admin ui (which comes packaged as a dependency).
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
@Controller
@RequestMapping({ UiController.WEB_UI_INDEX_PAGE_ROUTE, UiController.WEB_UI_INDEX_PAGE_ROUTE + "/" })
public class UiController {

	public static final String WEB_UI_INDEX_PAGE_ROUTE = "/dashboard";

	/**
	 * Turn a relative link of the UI app to an absolute one, prepending its path.
	 *
	 * @param path relative UI path
	 * @return the absolute UI path
	 */
	public static String dashboard(String path) {
		return WEB_UI_INDEX_PAGE_ROUTE + path;
	}

	@RequestMapping
	public String index() {
		return "redirect:" + WEB_UI_INDEX_PAGE_ROUTE + "/index.html";
	}

}
