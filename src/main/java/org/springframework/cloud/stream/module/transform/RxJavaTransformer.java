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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.rxjava.EnableRxJavaProcessor;
import org.springframework.cloud.stream.annotation.rxjava.RxJavaProcessor;
import org.springframework.context.annotation.Bean;

@EnableRxJavaProcessor
@EnableConfigurationProperties(ProgrammableRxJavaProcessorProperties.class)
public class RxJavaTransformer {

	private static Logger logger = LoggerFactory.getLogger(RxJavaTransformer.class);

	@Autowired
	private RuntimeJavaCompiler compiler;
	
	@Autowired
	private ProgrammableRxJavaProcessorProperties properties;

	private String rxJavaProcessorSourceTemplate = 
			"import org.springframework.cloud.stream.annotation.rxjava.*;\n"+
			"import org.springframework.cloud.stream.module.transform.ProcessorFactory;\n"+
			"public class RxClass implements ProcessorFactory {\n"+
			" public RxJavaProcessor<Object,Object> getProcessor() {\n"+
			"  %s\n"+
			" }\n"+
			"}\n";
	
	@Bean
	public RxJavaProcessor<Object,Object> processor() {
		logger.info("Compiling :'{}'",properties.getCode());
		Class<?> clazz = buildAndCompileSourceCode(properties.getCode());
		try {
			ProcessorFactory instance = (ProcessorFactory) clazz.newInstance();
			return instance.getProcessor();
		} catch (Exception e) {
			logger.error("Problem during retrieval of processor from compiled class",e);
		}
		return null;
	} 


	
	/**
	 * Create the source for and then compile and load a class that embodies
	 * the supplied methodBody. The methodBody is inserted into a method template that
	 * returns an <tt>RxJavaProcessor&lt;Object,Object&gt;</tt>. 
	 * 
	 * @param methodBody the source code for a method that should return an  <tt>RxJavaProcessor&lt;Object,Object&gt;</tt>
	 * @return the Class if the code is cleanly compiled and loaded, otherwise null
	 */
	private Class<?> buildAndCompileSourceCode(String methodBody) {
		// Some sample methodBodys:
		//
		//	return inputStream -> inputStream.map(data -> {
		//		logger.info("Got data = " + data);
		//		return data;
		//	}).buffer(5).map(data -> String.valueOf(avg(data)));
		//
		//	return input -> input.buffer(5).map(list->list.get(0));
		String sourceCode = compiler.makeSourceClassDefinition(rxJavaProcessorSourceTemplate, methodBody);
		Class<?> clazz = compiler.compile("RxClass",sourceCode);
		return clazz;
	}
	
}
