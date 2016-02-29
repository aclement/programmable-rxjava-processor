/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.stream.module.transform;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Compile Java at runtim and load it.
 * 
 * @author Andy Clement
 */
@Service
public class RuntimeJavaCompiler {

	private static Logger logger = LoggerFactory.getLogger(RuntimeJavaCompiler.class);

	private JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

	/**
	 * Make a full source code definition for a class by applying the specified inserts
	 * to the supplied template. Insert positions are specified in the source using the
	 * <tt>%s</tt> placeholder.
	 * 
	 * @param sourceTemplate template for a class with <tt>%s</tt> imports for configurable pieces
	 * @param inserts the String inserts to place into the template
	 * @return the template with insert strings replaced as specified
	 */
	public String makeSourceClassDefinition(String sourceTemplate, String inserts) {
		return String.format(sourceTemplate, inserts);
	}
	
	/**
	 * Compile the named class consisting of the supplied source code. If successful load the class
	 * and return it.
	 * @param className the name of the class
	 * @param classSourceCode the full source code for the class
	 * @return a Class object if the class can be compiled and loaded successfully, otherwise null
	 */
	public Class<?> compile(String className, String classSourceCode) {
		logger.info("Compiling source for "+className);
		DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
		JavaFileObject sourceFile = new StringBasedJavaSourceFileObject(className, classSourceCode);
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceFile);
		// TODO pass real file manager to avoid disk usage for intermediate .class file
		CompilationTask task = compiler.getTask(null, null, diagnosticCollector, null, null, compilationUnits);
		boolean success = task.call();
		if (!success) {
			for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
				StringBuilder message = new StringBuilder();
				// TODO split the source we have by \n and use the proper positions here to produce a better message
				message.append(diagnostic.getKind()+":"+diagnostic.getMessage(null));
				// getPosition(),getStartPosition(), getEndPosition, getSource()
			}
		}
		if (success) {
			try {
				// TODO too crude, loads from disk
				URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { new File("").toURI().toURL() });
				Class<?> clazz = Class.forName(className, true, classLoader);
				try {
					new File(className+".class").delete();
				} catch (Exception e) {
					// unable to tidyup?
				}
				return clazz;
			} catch (Exception e) {
				logger.error("Unexpected problem loading the compiled class ",e);
			}
		}
		return null;
	}

	static class StringBasedJavaSourceFileObject extends SimpleJavaFileObject {

		private final String sourceCode;

		StringBasedJavaSourceFileObject(String sourceName, String sourceCode) {
			super(URI.create("string:///" + sourceName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.sourceCode = sourceCode;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return sourceCode;
		}
	}
}