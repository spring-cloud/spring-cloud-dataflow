/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.mockito.ArgumentCaptor;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.io.DefaultPackageReader;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.cloud.skipper.io.TempFileUtils;
import org.springframework.util.StreamUtils;

/**
 * @author Christian Tzolov
 */
public class SkipperPackageUtils {

	public static Package loadPackageFromBytes(ArgumentCaptor<UploadRequest> uploadRequestCaptor) throws IOException {
		PackageReader packageReader = new DefaultPackageReader();
		String packageName = uploadRequestCaptor.getValue().getName();
		String packageVersion = uploadRequestCaptor.getValue().getVersion();
		byte[] packageBytes = uploadRequestCaptor.getValue().getPackageFileAsBytes();
		Path targetPath = TempFileUtils.createTempDirectory("service" + packageName);
		File targetFile = new File(targetPath.toFile(), packageName + "-" + packageVersion + ".zip");
		StreamUtils.copy(packageBytes, new FileOutputStream(targetFile));
		ZipUtil.unpack(targetFile, targetPath.toFile());
		return packageReader
				.read(new File(targetPath.toFile(), packageName + "-" + packageVersion));
	}

}
