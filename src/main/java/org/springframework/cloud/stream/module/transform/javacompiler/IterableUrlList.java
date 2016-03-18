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
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterable that can return an iterator that goes through a series of urls
 * returning classes found by opening those urls (which may be archives or
 * directories).
 * 
 * @author Andy Clement
 */
public class IterableUrlList extends CloseableFilterableJavaFileObjectIterable {

	private Logger logger = LoggerFactory.getLogger(IterableUrlList.class);

	private URL[] urls;
	
	private List<ZipFile> openArchives = new ArrayList<>();

	IterableUrlList(URL[] urls, String packageNameFilter, boolean includeSubpackages) {
		super(packageNameFilter, includeSubpackages);
		this.urls = urls;
		if (logger.isDebugEnabled()) {
			logger.debug("Urls being iterated over: {}",urlsString());
		}
	}
		
	private String urlsString() {
		StringBuilder s = new StringBuilder();
		for (URL url: urls) {
			s.append(url).append(File.pathSeparatorChar);
		}
		return s.toString();
	}

	@Override
	public Iterator<JavaFileObject> iterator() {
		return new UrlsIterator();
	}
	
	@Override
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

	class UrlsIterator implements Iterator<JavaFileObject> {
		private int currentUrlPointer = 0;
		
		private File openDir = null;
		private ZipFile openArchive;
		private Enumeration<? extends ZipEntry> openArchiveEnumerator;
		private DirEnumeration openDirEnumeration = null;

		private JavaFileObject nextEntry = null;

		@Override
		public boolean hasNext() {
			try {
				while (currentUrlPointer < urls.length) {
					if (openArchive == null && openDir == null) {
						URL url = urls[currentUrlPointer];
						if (url.getProtocol().equals("jar") || url.getProtocol().equals("zip")) {
							JarURLConnection jarUrlConnection = (JarURLConnection) url.openConnection();
							openArchive = jarUrlConnection.getJarFile();
							openArchives.add(openArchive);
							openArchiveEnumerator = openArchive.entries();
						} else if (url.getProtocol().equals("file")) {
							File file = new File(url.toURI());
							if (file.getName().endsWith("zip") || file.getName().endsWith("jar")) {
								openArchive = new ZipFile(file);
								openArchives.add(openArchive);
								openArchiveEnumerator = openArchive.entries();
							} else {
								openDir = file;
								openDirEnumeration = new DirEnumeration(file);
							}
						} else {
							logger.debug("Unable to handle URL: "+url);
						}
						currentUrlPointer++;
					}
					if (openArchiveEnumerator != null) {
						while (openArchiveEnumerator.hasMoreElements()) {
							ZipEntry entry = openArchiveEnumerator.nextElement();
							if (accept(entry.getName())) {
								nextEntry = new ZipEntryJavaFileObject(openArchive, entry);
								return true;
							}
						}
						openArchiveEnumerator = null;
						openArchive = null;
					} else if (openDirEnumeration != null) {
						while (openDirEnumeration.hasMoreElements()) {
							File entry = openDirEnumeration.nextElement();
							String path = entry.getPath();
							String basepath = openDirEnumeration.getDirectory().getPath();
							String name = path.substring(basepath.length() + 1);
							// Example name = a/b/c/d/E.class
							if (accept(name)) {
								nextEntry = new DirEntryJavaFileObject(openDirEnumeration.getDirectory(), entry);
								return true;
							}
						}
						openDirEnumeration = null;
						openDir = null;
					}
				}
			} catch (Throwable t) {
				logger.debug("Unexpected error whilst processing URLs",t);
			}
			return false;
		}

		@Override
		public JavaFileObject next() {
			JavaFileObject retval = nextEntry;
			nextEntry = null;
			return retval;
		}

	}

}