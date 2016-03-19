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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic iterable that will produce an iterator that returns classes found
 * on a specified classpath that meet specified criteria.
 * 
 * @author Andy Clement
 */
public class IterableClasspath extends CloseableFilterableJavaFileObjectIterable {

	private static Logger logger = LoggerFactory.getLogger(IterableClasspath.class);
	
	private List<File> classpathEntries = new ArrayList<>();
	
	private List<ZipFile> openArchives = new ArrayList<>();

	IterableClasspath(String path, String packageNameFilter, boolean includeSubpackages) {
		super(packageNameFilter, includeSubpackages);
		StringTokenizer tokenizer = new StringTokenizer(path, File.pathSeparator);
		while (tokenizer.hasMoreElements()) {
			String nextEntry = tokenizer.nextToken();
			File f = new File(nextEntry);
			if (f.exists()) {
				classpathEntries.add(f);
			} else {
				logger.debug("no exist {}",f);
			}
		}
	}

	public void close() {
		for (ZipFile openArchive : openArchives) {
			try {
				openArchive.close();
			} catch (IOException ioe) {
				logger.debug("Unexpected error closing archive {}",openArchive,ioe);
			}
		}
		openArchives.clear();
	}

	public Iterator<JavaFileObject> iterator() {
		return new ClasspathEntriesIterator();
	}

	class ClasspathEntriesIterator implements Iterator<JavaFileObject> {
		private int currentClasspathEntriesIndex = 0;

		// Either a directory or an archive will be open at any one time
		private File openDirectory = null;
		private DirEnumeration openDirectoryEnumeration = null;

		private ZipFile openArchive = null;
		private ZipEntry nestedZip = null;
		private Stack<Enumeration<? extends ZipEntry>> openArchiveEnumeration = null;

		// Computed during hasNext(), returned on calling next()
		private JavaFileObject nextEntry = null;

		public boolean hasNext() {
			try {
				while (openArchive!=null || currentClasspathEntriesIndex < classpathEntries.size()) {
					if (openArchive == null && openDirectory == null) {
						// Open the next item
						File nextFile = classpathEntries.get(currentClasspathEntriesIndex);
						if (nextFile.isDirectory()) {
							openDirectory = nextFile;
							openDirectoryEnumeration = new DirEnumeration(nextFile);
						} else {
							openArchive = new ZipFile(nextFile);
							openArchives.add(openArchive);
							openArchiveEnumeration = new Stack<Enumeration<? extends ZipEntry>>();
							openArchiveEnumeration.push(openArchive.entries());
						}
						currentClasspathEntriesIndex++;
					}
					if (openArchiveEnumeration != null) {
						while (!openArchiveEnumeration.isEmpty()) {
							while (openArchiveEnumeration.peek().hasMoreElements()) {
								ZipEntry entry = openArchiveEnumeration.peek().nextElement();
								String entryName = entry.getName();
								if (accept(entryName)) {
									if (nestedZip!=null) {
										nextEntry = new NestedZipEntryJavaFileObject(openArchive,nestedZip, entry);										
									} else {
										nextEntry = new ZipEntryJavaFileObject(openArchive, entry);
									}
									return true;
								} else if (nestedZip == null && entryName.startsWith("lib/") && entryName.endsWith(".jar")) {
									// nested jar in uber jar
									logger.debug("opening nested archive {}",entry.getName());
									ZipInputStream zis = new ZipInputStream(openArchive.getInputStream(entry));
//									nextEntry = new NestedZipEntryJavaFileObject(openArchive.firstElement(),openArchive.peek(),entry);
									Enumeration<? extends ZipEntry> nestedZipEnumerator = new ZipEnumerator(zis);
									nestedZip = entry;
									openArchiveEnumeration.push(nestedZipEnumerator);
								}
							}
							openArchiveEnumeration.pop();
							if (nestedZip ==null) openArchive = null;
							else nestedZip = null;
						}
						openArchiveEnumeration = null;
						openArchive = null;
					} else if (openDirectoryEnumeration != null) {
						while (openDirectoryEnumeration.hasMoreElements()) {
							File entry = openDirectoryEnumeration.nextElement();
							String name = openDirectoryEnumeration.getName(entry);
							if (accept(name)) {
								nextEntry = new DirEntryJavaFileObject(openDirectoryEnumeration.getDirectory(), entry);
								return true;
							}
						}
						openDirectoryEnumeration = null;
						openDirectory = null;
					}
				}
			} catch (IOException ioe) {
				logger.debug("Unexpected error whilst processing classpath entries",ioe);
			}
			return false;
		}

		public JavaFileObject next() {
			JavaFileObject retval = nextEntry;
			nextEntry = null;
			return retval;
		}

	}

	static class ZipEnumerator implements Enumeration<ZipEntry>{

		private ZipInputStream zis;
		private ZipEntry nextEntry = null;

		public ZipEnumerator(ZipInputStream zis) {
			this.zis = zis;
		}

		@Override
		public boolean hasMoreElements() {
			try {
				nextEntry = zis.getNextEntry();
			} catch (IOException ioe) {
				nextEntry=null;
			}
			return nextEntry!=null;
		}

		@Override
		public ZipEntry nextElement() {
			ZipEntry retval = nextEntry;
			nextEntry = null;
			return retval;
		}
		
	}
}