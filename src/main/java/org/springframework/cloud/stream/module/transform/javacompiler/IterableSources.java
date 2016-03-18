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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

public class IterableSources implements CloseableJavaFileObjectIterable {

	private List<String> sources;

	public IterableSources(List<String> sources) {
		this.sources = sources;
	}

	public void close() {
	}

	@Override
	public Iterator<JavaFileObject> iterator() {
		return new SourceIterator();
	}

	class SourceIterator implements Iterator<JavaFileObject> {

		int sourcePointer = 0;

		@Override
		public boolean hasNext() {
			return sourcePointer < sources.size();
		}

		@Override
		public JavaFileObject next() {
			return new SourceJavaFileObject(sources.get(sourcePointer++));
		}

		void close() {
		}
	}

	static class SourceJavaFileObject implements JavaFileObject {
		private String code;

		public SourceJavaFileObject(String code) {
			this.code = code;
		}

		@Override
		public URI toUri() {
			throw new IllegalStateException();
		}

		@Override
		public String getName() {
			throw new IllegalStateException();
		}

		@Override
		public InputStream openInputStream() throws IOException {
			throw new IllegalStateException();
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			throw new IllegalStateException();
		}

		@Override
		public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
			throw new IllegalStateException();
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			throw new IllegalStateException();
		}

		@Override
		public Writer openWriter() throws IOException {
			throw new IllegalStateException();
		}

		@Override
		public long getLastModified() {
			throw new IllegalStateException();
		}

		@Override
		public boolean delete() {
			throw new IllegalStateException();
		}

		@Override
		public Kind getKind() {
			throw new IllegalStateException();
		}

		@Override
		public boolean isNameCompatible(String simpleName, Kind kind) {
			throw new IllegalStateException();
		}

		@Override
		public NestingKind getNestingKind() {
			throw new IllegalStateException();
		}

		@Override
		public Modifier getAccessLevel() {
			throw new IllegalStateException();
		}

	}
}