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

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * A JavaFileObject backed by a String form of source code.
 * 
 * @author Andy Clement
 */
class StringBasedJavaSourceFileObject extends SimpleJavaFileObject {

	private final String sourceCode;

	StringBasedJavaSourceFileObject(String sourceName, String sourceCode) {
		super(URI.create("string:///" + sourceName.replace('.', '/') + ".java"), Kind.SOURCE);
		this.sourceCode = sourceCode;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return sourceCode;
	}
	
	public String getSourceCode() {
		return this.sourceCode;
	}
}