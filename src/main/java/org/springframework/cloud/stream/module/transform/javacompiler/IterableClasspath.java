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
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic iterable that will produce an iterator that returns classes found
 * on a specified classpath that meet specified criteria.
 * 
 * @author Andy Clement
 */
public class IterableClasspath implements CloseableJavaFileObjectIterable {

	private static Logger logger = LoggerFactory.getLogger(IterableClasspath.class);
	
	// If set specifies the package the iterator consumer is interested in. Only
	// return results in this package.
	private String packageNameFilter;

	// Indicates whether the consumer of the iterator wants to see classes
	// that are in subpackages of those matching the filter.
	private boolean includeSubpackages;
	
	private List<File> classpathEntries = new ArrayList<>();
	
	private List<ZipFile> openArchives = new ArrayList<>();

	IterableClasspath(String classpathProperty, String packageNameFilter, boolean includeSubpackages) {
		this.packageNameFilter = packageNameFilter.replace('.', '/') + "/";
		this.includeSubpackages = includeSubpackages;
		String cp = System.getProperty(classpathProperty);
		StringTokenizer tokenizer = new StringTokenizer(cp, File.pathSeparator);
		while (tokenizer.hasMoreElements()) {
			String nextEntry = tokenizer.nextToken();
			File f = new File(nextEntry);
			if (f.exists()) {
				classpathEntries.add(f);
			}
		}
	}

	public void close() {
		for (ZipFile openArchive : openArchives) {
			try {
				openArchive.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
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
		private Enumeration<? extends ZipEntry> openArchiveEnumeration = null;

		// Computed during hasNext(), returned on calling next()
		private JavaFileObject nextEntry = null;

		ClasspathEntriesIterator() {
		}
		
		/**
		 * @param name the name to check against the criteria
		 * @return true if the name is a valid iterator result based on the specified criteria
		 */
		private boolean accept(String name) {
			if (!name.endsWith(".class")) {
				return false;
			}
			if (packageNameFilter == null) {
				return true;
			}
			if (includeSubpackages == true) {
				return name.startsWith(packageNameFilter);
			} else {
				return name.startsWith(packageNameFilter) && name.indexOf("/",packageNameFilter.length())==-1;
			}
		}

		public boolean hasNext() {
			try {
				while (currentClasspathEntriesIndex < classpathEntries.size()) {
					if (openArchive == null && openDirectory == null) {
						// Open the next item
						File nextFile = classpathEntries.get(currentClasspathEntriesIndex);
						if (nextFile.isDirectory()) {
							openDirectory = nextFile;
							openDirectoryEnumeration = new DirEnumeration(nextFile);
						} else {
							openArchive = new ZipFile(nextFile);
							openArchiveEnumeration = openArchive.entries();
						}
						currentClasspathEntriesIndex++;
					}
					if (openArchiveEnumeration != null) {
						while (openArchiveEnumeration.hasMoreElements()) {
							ZipEntry entry = openArchiveEnumeration.nextElement();
							if (accept(entry.getName())) {
								nextEntry = new ZipEntryJavaFileObject(openArchive, entry);
								return true;
							}
						}
						openArchiveEnumeration = null;
						openArchive = null;
					} else if (openDirectoryEnumeration != null) {
						while (openDirectoryEnumeration.hasMoreElements()) {
							File entry = openDirectoryEnumeration.nextElement();
							if (accept(entry.getName())) {
								nextEntry = new DirEntryJavaFileObject(openDirectoryEnumeration.getDirectory(), entry);
								return true;
							}
						}
						openDirectoryEnumeration = null;
						openDirectory = null;
					}
				}
				return false;
			} catch (IOException ioe) {
				logger.debug("Unexpected error whilst processing classpath entries",ioe);
				return false;
			}
		}

		public JavaFileObject next() {
			JavaFileObject retval = nextEntry;
			nextEntry = null;
			return retval;
		}

	}

}