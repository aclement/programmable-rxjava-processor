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

import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common superclass for iterables that need to handle closing when finished
 * with and that need to handle possible constraints on the values that
 * are iterated over.
 * 
 * @author Andy Clement
 */
public abstract class CloseableFilterableJavaFileObjectIterable implements Iterable<JavaFileObject> {

	private final static Logger logger = LoggerFactory.getLogger(CloseableFilterableJavaFileObjectIterable.class);

	// If set specifies the package the iterator consumer is interested in. Only
	// return results in this package.
	private String packageNameFilter;

	// Indicates whether the consumer of the iterator wants to see classes
	// that are in subpackages of those matching the filter.
	private boolean includeSubpackages;

	public CloseableFilterableJavaFileObjectIterable(String packageNameFilter, boolean includeSubpackages) {
		if (packageNameFilter!=null && packageNameFilter.contains(File.separator)) {
			throw new IllegalArgumentException("Package name filters should use dots to separate components: "+packageNameFilter);
		}
		this.packageNameFilter = packageNameFilter==null?null:packageNameFilter.replace('.', File.separatorChar) + "/";
		this.includeSubpackages = includeSubpackages;
	}
	
	/**
	 * Used by subclasses to check values against any specified constraints.
	 * 
	 * @param name the name to check against the criteria
	 * @return true if the name is a valid iterator result based on the specified criteria
	 */
	protected boolean accept(String name) {
//		logger.debug("checking {} against constraints packageNameFilter={} includeSubpackages={}",name,packageNameFilter,includeSubpackages);
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

	abstract void close();
}