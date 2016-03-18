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

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.cloud.stream.annotation.rxjava.RxJavaProcessor;
import org.springframework.cloud.stream.module.transform.javacompiler.RuntimeJavaCompiler;

import rx.Observable;

public class TemplateTests {

	
	@Test
	public void templateUsageLocalClasses() {
		String insert = "class Foo {\n"
				+ "			public Integer average(java.util.List<Integer> lis) {\n"
				+ "				int sum = 0;\n"
				+ "				for (Integer i: lis) {\n"
				+ "					sum+=i;\n"
				+ "				}\n"
				+ "				return (sum/lis.size());\n"
				+ "			}\n"
				+ "		}\n"
				+ "		return input -> input.map(s -> Integer.valueOf((String)s)).buffer(5).map(new Foo()::average);\n";
	}
	
	public RxJavaProcessor<Object,Object> getProcessor(List<Class<?>> clazzes) {
		for (Class<?> clazz: clazzes) {
			try {
				Object o = null;
				try {
					o = clazz.newInstance();
					if (o instanceof ProcessorFactory) {
						ProcessorFactory instance = (ProcessorFactory) o;
						return instance.getProcessor();
					}
				} catch (InstantiationException ie) {
					
				}
			} catch (Exception e) {
				Assert.fail(e.toString());
			}
		}
		return null;
	}
	
	// ---

	private String captureOutputDuringRunOfMainMethod(Class<?> clazz) {
		PrintStream oldOut = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(baos));
			Method m = clazz.getDeclaredMethod("main");
			m.invoke(null);
			System.out.flush();
		} catch (Exception e) {
			
		} finally {
			System.setOut(oldOut);;
		}
		return new String(baos.toByteArray());
	}
	
}
