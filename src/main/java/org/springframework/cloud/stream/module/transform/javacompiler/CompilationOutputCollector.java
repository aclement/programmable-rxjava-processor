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

import java.util.ArrayList;
import java.util.List;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;

import org.springframework.stereotype.Component;

/**
 * During compilation this class will collect up the output files.
 * 
 * @author Andy Clement
 */
@Component
public class CompilationOutputCollector {
	
	private List<JavaFileObject> outputFiles = new ArrayList<>();
	
	public CompilationOutputCollector() {}
	
	/**
	 * Retrieve compiled classes that have been collected since this collector was built.
	 * Due to annotation processing it is possible other source files or metadata files
	 * may be produced during compilation - those are ignored.
	 * 
	 * @return list of compiled classes
	 */
	public List<CompiledClassDefinition> getCompiledClasses() {
		List<CompiledClassDefinition> compiledClassDefinitions = new ArrayList<>();
		for (JavaFileObject outputFile: outputFiles) {
			if (outputFile.getKind()==Kind.CLASS && (outputFile instanceof OutputJavaFileObject)) {
				CompiledClassDefinition compiledClassDefinition = new CompiledClassDefinition(outputFile.getName(),((OutputJavaFileObject)outputFile).getBytes());
				compiledClassDefinitions.add(compiledClassDefinition);
			}
		}
		return compiledClassDefinitions;
	}

	public JavaFileObject getFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
		OutputJavaFileObject ojfo = new OutputJavaFileObject(location,className,kind,sibling);
		outputFiles.add(ojfo);
		return ojfo;
	}
	
	public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) {
		OutputJavaFileObject ojfo = new OutputJavaFileObject(location,packageName,relativeName,sibling);
		outputFiles.add(ojfo);
		return ojfo;
	}
	
}