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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

/**
 * Represents an element inside in zip which is itself inside a zip. These objects are
 * not initially created with the content of the file they represent,
 * only enough information to find that content because many will
 * typically be created but only few will be opened.
 * 
 * @author Andy Clement
 */
public class NestedZipEntryJavaFileObject implements JavaFileObject {

	private ZipFile outerZipFile;
	private ZipEntry innerZipFile;
	private ZipEntry innerZipFileEntry;

	public NestedZipEntryJavaFileObject(ZipFile outerZipFile, ZipEntry innerZipFile, ZipEntry innerZipFileEntry) {
		this.outerZipFile = outerZipFile;
		this.innerZipFile = innerZipFile;
		this.innerZipFileEntry = innerZipFileEntry;
	}

	@Override
	public String getName() {
		return innerZipFileEntry.getName(); // Example: a/b/C.class
	}

	@Override
	public URI toUri() {
		try {
			return new URI("zip:"+outerZipFile.getName()+"!"+innerZipFile.getName()+"!"+innerZipFileEntry.getName());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Unexpected URISyntaxException for string 'zip:"+outerZipFile.getName()+"!"+innerZipFile.getName()+"!"+innerZipFileEntry.getName()+"'",e);
		}
	}
	
	@Override
	public InputStream openInputStream() throws IOException {
		// Find the inner zip file inside the outer zip file, then
		// find the relevant entry, then return the stream.
		InputStream innerZipFileInputStream = this.outerZipFile.getInputStream(innerZipFile);
		ZipInputStream innerZipInputStream = new ZipInputStream(innerZipFileInputStream);
		ZipEntry nextEntry = innerZipInputStream.getNextEntry();
		while (nextEntry != null) {
			if (nextEntry.getName().equals(innerZipFileEntry.getName())) {
				return innerZipInputStream;
			}
			nextEntry = innerZipInputStream.getNextEntry();
		}
		throw new IllegalStateException("Unable to locate nested zip entry "+innerZipFileEntry.getName()+" in zip "+innerZipFile.getName()+" inside zip "+outerZipFile.getName());
	}

	@Override
	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		return new InputStreamReader(openInputStream());
	}

	@Override
	public long getLastModified() {
		return innerZipFileEntry.getTime();
	}

	@Override
	public Kind getKind() {
		return Kind.CLASS;
	}

	@Override
	public boolean delete() {
		throw new IllegalStateException("only expected to be used for input");
	}
	
	@Override
	public OutputStream openOutputStream() throws IOException {
		throw new IllegalStateException("only expected to be used for input");
	}
	
	@Override
	public Writer openWriter() throws IOException {
		throw new IllegalStateException("only expected to be used for input");
	}

	@Override
	public boolean isNameCompatible(String simpleName, Kind kind) {
		throw new IllegalStateException("not expected to be called in this scenario on "+toUri().toString());
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		throw new IllegalStateException("not expected to be called in this scenario on "+toUri().toString());
	}

	@Override
	public NestingKind getNestingKind() {
		return null; // nesting level not known
	}

	@Override
	public Modifier getAccessLevel() {
		return null; // access level not known
	}

}