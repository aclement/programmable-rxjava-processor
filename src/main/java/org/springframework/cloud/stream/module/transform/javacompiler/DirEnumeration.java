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

import java.io.*;
import java.util.*;

public class DirEnumeration implements Enumeration<File> {

	private final static boolean DEBUG = false;
	
	private File basedir;
	private List<File> filesToReturn;
	private List<File> dirsToExplore;

	public DirEnumeration(File basedir) {
		if (DEBUG) System.out.println("DirEnumeration, walking: " + basedir);
		this.basedir = basedir;
	}

	private void updateLists(File dir) {
		File[] files = dir.listFiles((File f) -> !f.isDirectory());
		if (files != null) {
			for (File f : files) {
				filesToReturn.add(f);
			}
		}
		File[] dirs = dir.listFiles((File f) -> f.isDirectory());
		if (dirs != null) {
			for (File f : dirs) {
				dirsToExplore.add(f);
			}
		}
		if (DEBUG) System.out.println("After checking " + dir + "  filesToReturn=#" + filesToReturn.size() + "  dirsToExplore=#"
				+ dirsToExplore.size());
	}

	@Override
	public boolean hasMoreElements() {
		if (filesToReturn == null) { // Indicates we haven't started yet
			filesToReturn = new ArrayList<>();
			dirsToExplore = new ArrayList<>();
			updateLists(basedir);
		}
		if (filesToReturn.size() == 0) {
			while (filesToReturn.size() == 0 && dirsToExplore.size() != 0) {
				File nextDir = dirsToExplore.get(0);
				dirsToExplore.remove(0);
				updateLists(nextDir);
			}
		}
		return filesToReturn.size() != 0;
	}

	@Override
	public File nextElement() {
		File toReturn = filesToReturn.get(0);
		if (DEBUG) System.out.println("DirEnumeration: basedir=" + basedir + " returning " + toReturn);
		filesToReturn.remove(0);
		return toReturn;
	}

	public File getDirectory() {
		return basedir;
	}

}
