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
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.tools.JavaFileObject;

public class IterableUrlList implements CloseableJavaFileObjectIterable {

		private URL[] urls;
		private String packageName;
		private boolean subpackages;

		IterableUrlList(URL[] urls, String packageName, boolean subpackages) {
			this.urls = urls;
			this.packageName = packageName.replace('.', '/');
			this.subpackages = subpackages;
			System.out.println("UrlsIterable for #" + urls.length + " urls");
		}

		@Override
		public void close() {
		}

		@Override
		public Iterator<JavaFileObject> iterator() {
			return new UrlsIterator();
		}

		class UrlsIterator implements Iterator<JavaFileObject> {

			int urlPtr = 0;
			private ZipFile openJar;
			private Enumeration<? extends ZipEntry> openJarEnumerator;
			JavaFileObject nextEntry = null;
			File openDir = null;
			DirEnumeration openDirEnumeration = null;

			@Override
			public boolean hasNext() {
				try {
					while (urlPtr < urls.length) {
						if (openJar == null && openDir==null) {
							URL url = urls[urlPtr];
//							System.out.println("UrlsIterator: opening " + url);
							// TODO use equals but does it come with trailing
							// crap?
							if (url.getProtocol().startsWith("jar")) {
//								System.out.println("Walking jarUrlConnection "+url);
								JarURLConnection jarUrlConnection = (JarURLConnection) url.openConnection();
								openJar = jarUrlConnection.getJarFile();
								openJarEnumerator = openJar.entries();
							} else if (url.getProtocol().startsWith("file")) {
								File f = new File(url.toURI());
								if (f.getName().endsWith("zip") || f.getName().endsWith("jar")) {
//									System.out.println("Walking zip/jar "+f);
									openJar = new ZipFile(f);
									openJarEnumerator = openJar.entries();
								} else {
//									System.out.println("Walking dir "+f);
									openDir = f;
									openDirEnumeration = new DirEnumeration(f);
								}
							} else {
								System.out.println("ERROR: UrlsIterator - can't handle: " + url);
							}
							urlPtr++;
						}
						if (openJarEnumerator != null) {
							while (openJarEnumerator.hasMoreElements()) {
								ZipEntry entry = openJarEnumerator.nextElement();
								if (entry.getName().endsWith(".class")) {
									if (packageName == null
											|| (entry.getName().startsWith(packageName) && (subpackages == true || entry
													.getName().substring(packageName.length()+1).indexOf('/') == -1))) {
										System.out.println("entry = "+entry);
										nextEntry = new ZipEntryJavaFileObject(openJar, entry);
										System.out.println("UrlsIterator: nextEntry " + nextEntry.getName());
										return true;
									}
								}
							}
							openJarEnumerator = null;
							openJar = null;
						} else if (openDirEnumeration != null) {
							while (openDirEnumeration.hasMoreElements()) {
								File entry = openDirEnumeration.nextElement();
								String path = entry.getPath();
								String basepath = openDirEnumeration.getDirectory().getPath();
								String name = path.substring(basepath.length()+1);
								// name = a/b/c/d/E.class
								// packageName = a/b/c
								if (name.endsWith(".class")) {
									if (packageName == null || (name.startsWith(packageName) && (subpackages==true || name.substring(packageName.length()+1).indexOf('/')==-1))) {
										nextEntry = new DirEntryJavaFileObject(openDirEnumeration.getDirectory(), entry);
										return true;
									}
								}
							}
							openDirEnumeration = null;
							openDir = null;
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
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