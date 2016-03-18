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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

public class DirEntryJavaFileObject implements ExtendedJavaFileObject {

	private File file;
	private File basedir;

	public DirEntryJavaFileObject(File basedir, File file) {
		this.basedir = basedir;
		this.file = file;
	}
	
	public String getClassName() {
		String basedirPath = basedir.toString();
		String filePath = file.toString();
		// will return a/b/c/d/E.class
		return filePath.substring(basedirPath.length()+1);
	}

	@Override
	public URI toUri() {
		System.out.println(">>>>>toUri()");
		throw new IllegalStateException();
	}

	@Override
	public String getName() {
//		System.out.println("DEJFO.getName()");
		return file.getName(); // a/b/C.class
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return new FileInputStream(file);
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		System.out.println(">>>>>openOutputStream()");
		throw new IllegalStateException();
	}

	@Override
	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		return new InputStreamReader(new FileInputStream(file));
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		System.out.println(">>>>>getCharContent()");
		throw new IllegalStateException();
	}

	@Override
	public Writer openWriter() throws IOException {
		System.out.println(">>>>>openWriter()");
		throw new IllegalStateException();
	}

	@Override
	public long getLastModified() {
		return file.lastModified();
	}

	@Override
	public boolean delete() {
		System.out.println(">>>>>delete()");

		throw new IllegalStateException();
	}

	@Override
	public Kind getKind() {
		return Kind.CLASS;
	}

	@Override
	public boolean isNameCompatible(String simpleName, Kind kind) {
		System.out.println(">>>>>isNameCompatible()");
		throw new IllegalStateException();
	}

	@Override
	public NestingKind getNestingKind() {
		System.out.println(">>>>>getNestingKind()");
		throw new IllegalStateException();
	}

	@Override
	public Modifier getAccessLevel() {
		System.out.println(">>>>>getAccessLevel()");
		throw new IllegalStateException();
	}
	
}