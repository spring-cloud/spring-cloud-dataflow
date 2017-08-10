/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * @author Mark Pollack
 */
@Service
public class FileManifestStore implements ManifestStore {

	private SkipperServerProperties skipperServerProperties;

	@Autowired
	public FileManifestStore(SkipperServerProperties skipperServerProperties) {
		this.skipperServerProperties = skipperServerProperties;
	}

	@Override
	public void store(Release release) {
		final File releaseDir = new File(getManfiestDir() + File.separator
				+ release.getName() + "-v"
				+ release.getVersion());
		releaseDir.mkdirs();
		File manifestFile = new File(releaseDir, "manifest.yml");
		writeText(manifestFile, release.getManifest());

	}

	private void writeText(final File target, final String body) {
		try (OutputStream stream = new FileOutputStream(target, false)) {
			StreamUtils.copy(body, Charset.forName("UTF-8"), stream);
		}
		catch (final Exception e) {
			throw new IllegalStateException("Cannot write file " + target, e);
		}
	}

	public String getManfiestDir() {
		return skipperServerProperties.getSkipperHome() + File.separator + "manifests";
	}
}
