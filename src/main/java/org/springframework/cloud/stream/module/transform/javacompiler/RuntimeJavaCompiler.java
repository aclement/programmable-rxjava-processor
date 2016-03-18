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
package org.springframework.cloud.stream.module.transform.javacompiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Compile Java source at runtime and load it.
 * 
 * @author Andy Clement
 */
@Service
public class RuntimeJavaCompiler {
	
	private JavaCompiler compiler =  ToolProvider.getSystemJavaCompiler();
	
	private static Logger logger = LoggerFactory.getLogger(RuntimeJavaCompiler.class);
	
	/**
	 * Compile the named class consisting of the supplied source code. If successful load the class
	 * and return it. Multiple classes may get loaded if the source code included anonymous/inner/local
	 * classes.
	 * @param className the name of the class (dotted form, e.g. com.foo.bar.Goo)
	 * @param classSourceCode the full source code for the class
	 * @return a CompilationResult that encapsulates what happened during compilation (classes/messages produced)
	 */
	public CompilationResult compile(String className, String classSourceCode) {
		logger.info("Compiling source for class {} using compiler {}",className,compiler.getClass().getName());
		
		DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
		MemoryBasedJavaFileManager fileManager = new MemoryBasedJavaFileManager();
		JavaFileObject sourceFile = new StringBasedJavaSourceFileObject(className, classSourceCode);
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceFile);
		CompilationTask task = compiler.getTask(null, fileManager , diagnosticCollector, null, null, compilationUnits);

		boolean success = task.call();
		CompilationResult compilationResult = new CompilationResult(success);
		
		// If successful there may be no errors but there might be info/warnings
		for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
			CompilationMessage.Kind kind = (diagnostic.getKind()==Kind.ERROR?CompilationMessage.Kind.ERROR:CompilationMessage.Kind.OTHER);
			String sourceCode = ((StringBasedJavaSourceFileObject)diagnostic.getSource()).getSourceCode();
			int startPosition = (int)diagnostic.getPosition();
			if (startPosition == Diagnostic.NOPOS) {
				startPosition = (int)diagnostic.getStartPosition();
			}
			CompilationMessage compilationMessage = new CompilationMessage(kind,diagnostic.getMessage(null),sourceCode,startPosition,(int)diagnostic.getEndPosition());
			compilationResult.recordCompilationMessage(compilationMessage);
		}
		if (success) {			
			List<CompiledClassDefinition> ccds = fileManager.getCompiledClasses();			
			List<Class<?>> classes = new ArrayList<>();
			try (SimpleClassLoader ccl = new SimpleClassLoader(this.getClass().getClassLoader())) {
				for (CompiledClassDefinition ccd: ccds) {
					Class<?> clazz = ccl.defineClass(ccd.getName(), ccd.getBytes());
					classes.add(clazz);
				}
			} catch (IOException ioe) {
				logger.debug("Unexpected exception defining classes",ioe);
			}
			compilationResult.setCompiledClasses(classes);
		}
		return compilationResult;
	}
}