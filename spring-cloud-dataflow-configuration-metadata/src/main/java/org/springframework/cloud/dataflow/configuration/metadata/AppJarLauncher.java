/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata;


import java.lang.reflect.Method;
import java.net.URL;

import org.springframework.boot.loader.JarLauncher;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link JarLauncher} that supports launching an application archive.
 *
 * Also, it restricts the classloader of the launched application to the contents
 * of the uber-jar and the extension classloader of the JVM.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
public class AppJarLauncher extends JarLauncher {

	public AppJarLauncher(Archive archive) {
		super(archive);
	}

	@Override
	public void launch(String[] args) {
		super.launch(args);
	}

	@Override
	protected void launch(String[] args, String mainClass, ClassLoader classLoader)
			throws Exception {
		if (ClassUtils.isPresent(
				"org.apache.catalina.webresources.TomcatURLStreamHandlerFactory",
				classLoader)) {
			// Ensure the method is invoked on a class that is loaded by the provided
			// class loader (not the current context class loader):
			Method method = ReflectionUtils
					.findMethod(classLoader.loadClass("org.apache.catalina.webresources.TomcatURLStreamHandlerFactory"),
							"disable");
			ReflectionUtils.invokeMethod(method, null);
		}
		super.launch(args, mainClass, classLoader);
	}

	@Override
	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return ClassloaderUtils.createModuleClassloader(urls);
	}

}
