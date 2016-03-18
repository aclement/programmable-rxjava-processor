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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputJavaFileObject implements JavaFileObject {

	private final static Logger logger = LoggerFactory.getLogger(OutputJavaFileObject.class);
	
	private Location location;
	private String packageName;
	private String relativeName;
	private FileObject sibling;
	private String className;
	private Kind kind;
	
	private ByteArrayOutputStream baos;

	public OutputJavaFileObject(Location location, String packageName, String relativeName, FileObject sibling) {
		this.location = location;
		this.packageName = packageName;
		this.relativeName = relativeName;
		this.sibling = sibling;
	}

	public OutputJavaFileObject(Location location, String className, Kind kind, FileObject sibling) {
		this.location = location;
		this.className = className;
		this.kind = kind;
		this.sibling = sibling;
	}

	public byte[] getBytes() {
		return baos.toByteArray();
	}

	public String toString() {
		return "OutputJavaFileObject: Location="+location+",className="+className+",kind="+kind+",relativeName="+relativeName+",sibling="+sibling+",packageName="+packageName;
	}
	
	@Override
	public URI toUri() {
		System.out.println("> toUri "+this.toString());
		URI uri = null;
		String name = null;
		if (className != null) {
			name = className.replace('.', '/');
		} else {
			name = relativeName;
		}
		try {
		 uri = URI.create("file:///" + name);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		System.out.println("< toUri returning "+uri);
		return uri;
	}

	@Override
	public String getName() {
		System.err.println("getName");
		return className;
	}

	@Override
	public InputStream openInputStream() throws IOException {
		System.err.println("openInputStream");
		byte[] empty = "{}".getBytes();
		return new ByteArrayInputStream(empty);
//		throw new IllegalStateException();
//		// TODO Auto-generated method stub
//		return null;
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		logger.debug("opening output stream for {}"+getName());
		baos = new ByteArrayOutputStream();
		return baos;
	}

	@Override
	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		System.err.println("openReader");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		System.err.println("getCharContent");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Writer openWriter() throws IOException {
		System.err.println("openWriter");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLastModified() {
		System.err.println("getLastModified");

		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean delete() {
		System.err.println("delete");

		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Kind getKind() {
		System.err.println("getKind");
		return kind;
	}

	@Override
	public boolean isNameCompatible(String simpleName, Kind kind) {
		// TODO Auto-generated method stub
		System.err.println("isNameCompatible");
		return false;
	}

	@Override
	public NestingKind getNestingKind() {
		System.err.println("getNestingKind");

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Modifier getAccessLevel() {
		System.err.println("getAccessLevel");

		// TODO Auto-generated method stub
		return null;
	}
	
}