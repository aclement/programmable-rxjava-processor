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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.NoSuchElementException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * 
 * @author Andy Clement
 */
public class DirEnumerationTests {
	
	// Created so they can be searched for in the tests below
	static class Foo {}
	static class Bar {}
	
	static String FooClassFilename;
	static String BarClassFilename;

	static {
		FooClassFilename = Foo.class.getName();
		FooClassFilename = FooClassFilename.substring(FooClassFilename.lastIndexOf('.')+1)+".class";
		BarClassFilename = Bar.class.getName();
		BarClassFilename = BarClassFilename.substring(BarClassFilename.lastIndexOf('.')+1)+".class";
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void badpath() throws Exception {
		DirEnumeration e = new DirEnumeration(new File("made/up/path"));
		assertFalse(e.hasMoreElements());
		expectedException.expect(NoSuchElementException.class);
		e.nextElement();
	}
	
	@Test
	public void workingCorrectly() throws Exception {
		DirEnumeration e = new DirEnumeration(new File("target/test-classes"));
		assertTrue(e.hasMoreElements());
		boolean foundFoo = false;
		while (e.hasMoreElements()) {
			File nextFile = e.nextElement();
			System.out.println(nextFile);
			if (nextFile.getName().equals(FooClassFilename)) {
				foundFoo = true;
				break;
			}
		}
		assertTrue(foundFoo);
	}
	

	@Test
	public void checkOtherOperations() throws Exception {
		DirEnumeration e = new DirEnumeration(new File("target/test-classes"));
		assertEquals("test-classes",e.getDirectory().getName());
		File foo = find(e,FooClassFilename);
		String expectedPath = Foo.class.getName().replace('.', '/')+".class";
		assertEquals(expectedPath,e.getName(foo));
		expectedException.expect(IllegalStateException.class);
		e.getName(File.createTempFile("tmp",null));
	}
	
	// ---
	
	private File find(DirEnumeration e, String name) {
		while (e.hasMoreElements()) {
			File nextFile = e.nextElement();
			System.out.println(nextFile.getName());
			if (nextFile.getName().equals(name)) {
				return nextFile;
			}
		}
		return null;
	}
	
}
