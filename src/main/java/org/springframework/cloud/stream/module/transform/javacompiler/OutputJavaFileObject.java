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
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;
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
	
	private byte[] content = null;
	private long lastModifiedTime = 0;
	private URI uri = null;

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
		return content;
	}

	public String toString() {
		return "OutputJavaFileObject: Location="+location+",className="+className+",kind="+kind+",relativeName="+relativeName+",sibling="+sibling+",packageName="+packageName;
	}
	
	@Override
	public URI toUri() {
		// These memory based output files 'pretend' to be relative to the file system root
		if (uri == null) {
			String name = null;
			if (className != null) {
				name = className.replace('.', '/');
			} else {
				name = relativeName;
			}
			
			String uriString = null;
			try {
				uriString = "file:/"+name+(kind==null?"":kind.extension);
				uri = new URI(uriString);
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Unexpected URISyntaxException for string '" + uriString + "'", e);
			}
		}
		return uri;
	}

	@Override
	public String getName() {
		System.err.println("getName");
		return className;
	}

	@Override
	public InputStream openInputStream() throws IOException {
		if (content == null) {
			throw new FileNotFoundException();
		}
		logger.debug("opening input stream for {}",getName());
		return new ByteArrayInputStream(content);
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		logger.debug("opening output stream for {}",getName());
		return new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				super.close();
				lastModifiedTime = System.currentTimeMillis();
				content = this.toByteArray();
			}
		};
	}

	@Override
	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		System.err.println("openReader");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		// Could be supported where kind != CLASS, if necessary
		throw new UnsupportedOperationException("getCharContent() not supported on file object: " + getName());
	}

	@Override
	public Writer openWriter() throws IOException {
		if (kind == Kind.CLASS) {
			throw new UnsupportedOperationException("openWriter() not supported on file object: " + getName());
		}
		return new CharArrayWriter() {
			@Override
			public void close() {
				lastModifiedTime = System.currentTimeMillis();
				content = new String(toCharArray()).getBytes(); // Ignoring encoding...
			};
		};
	}

	@Override
	public long getLastModified() {
		return lastModifiedTime;
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public Kind getKind() {
		return kind;
	}

	@Override
	public boolean isNameCompatible(String simpleName, Kind kind) {
		if (kind != this.kind) {
			return false;
		}
//		String name = getName();
//		int lastSlash = name.lastIndexOf('/');
//		return name.substring(lastSlash + 1).equals(simpleName + ".class");
		// TODO Auto-generated method stub
		System.err.println("isNameCompatible");
		return false;
	}

	@Override
	public NestingKind getNestingKind() {
		return null;
	}

	@Override
	public Modifier getAccessLevel() {
		return null;
	}
	
}