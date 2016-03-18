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

/**
 * Walks a directory hierarchy from some base directory discovering files.
 * 
 * @author Andy Clement
 */
public class DirEnumeration implements Enumeration<File> {
	
	private File basedir;
	private List<File> filesToReturn;
	private List<File> directoriesToExplore;

	public DirEnumeration(File basedir) {
		this.basedir = basedir;
	}

	@Override
	public boolean hasMoreElements() {
		if (filesToReturn == null) { // Indicates we haven't started yet
			filesToReturn = new ArrayList<>();
			directoriesToExplore = new ArrayList<>();
			visitDirectory(basedir);
		}
		if (filesToReturn.size() == 0) {
			while (filesToReturn.size() == 0 && directoriesToExplore.size() != 0) {
				File nextDir = directoriesToExplore.get(0);
				directoriesToExplore.remove(0);
				visitDirectory(nextDir);
			}
		}
		return filesToReturn.size() != 0;
	}

	@Override
	public File nextElement() {
		File toReturn = filesToReturn.get(0);
		filesToReturn.remove(0);
		return toReturn;
	}

	private void visitDirectory(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file: files) {
				if (file.isDirectory()) {
					directoriesToExplore.add(file);
				} else {
					filesToReturn.add(file);
				}
			}
		}
		// System.out.println("After checking " + dir + "  filesToReturn=#" + filesToReturn.size() + "  dirsToExplore=#"
		//	+ directoriesToExplore.size());
	}

	public File getDirectory() {
		return basedir;
	}

}
