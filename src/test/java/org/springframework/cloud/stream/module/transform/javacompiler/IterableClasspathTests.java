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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Verify that IterableClasspath behaves and also that the various iterators it can produce
 * also behave as expected. These iterators will produce JavaFileObject subtypes: 
 * NestedZipEntryJavaFileObject, ZipEntryJavaFileObject, DirEntryJavaFileObject 
 * - and these are all tested here too.
 * 
 * @author Andy Clement
 */
public class IterableClasspathTests {
	
	static String ThisClassFilename;
	static String XxxClassFilename = "com/foo/Xxx.class";
	static String YyyClassFilename = "com/bar/Yyy.class";
	
	static String NestedJarPath = "target/test-classes/outerjar.jar";
	static String SimpleJarPath = "target/test-classes/simplejar.jar";
	static String TestClassesDir = "target/test-classes";

	static {
		ThisClassFilename = IterableClasspathTests.class.getName().replace('.', '/')+".class";
	}
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Test
	public void badentries() throws Exception {
		String path = "made/up/path";
		IterableClasspath icp = new IterableClasspath(path, null, false);
		Iterator<JavaFileObject> iterator = icp.iterator();
		assertFalse(iterator.hasNext());
		exception.expect(NoSuchElementException.class);
		iterator.next();
	}
	
	@Test
	public void iteratorReuse() throws Exception {
		IterableClasspath icp = new IterableClasspath(NestedJarPath, null, false);
		Iterator<JavaFileObject> iterator = icp.iterator();
		JavaFileObject first = iterator.next();
		iterator = icp.iterator();
		JavaFileObject second = iterator.next();
		assertEquals(first,second);
	}
	
	// Search for this class in the test-classes folder
	@Test
	public void directories() throws Exception {
		IterableClasspath icp = new IterableClasspath(TestClassesDir,null,false);
		Iterator<JavaFileObject> iterator = icp.iterator();
		assertTrue(iterator.hasNext());
		
		boolean found = false;
		while (iterator.hasNext()) {
			JavaFileObject jfo = iterator.next();
			// Really must find ourselves
			if (jfo.getName().equals(ThisClassFilename)) {
				found = true;
				break;
			}
		}
		assertTrue(found);
	}
	
	@Test
	public void jars() throws Exception {
		IterableClasspath icp = new IterableClasspath(SimpleJarPath,null,false);
		JavaFileObject jfo = find(icp.iterator(),XxxClassFilename);
		assertNotNull(jfo);
		
		icp = new IterableClasspath(SimpleJarPath,"com",true);
		assertEquals(2,countEntries(icp.iterator()));

		icp = new IterableClasspath(SimpleJarPath,"com.foo",true);
		assertEquals(1,countEntries(icp.iterator()));
		
		icp = new IterableClasspath(SimpleJarPath,"com",false);
		assertEquals(0,countEntries(icp.iterator()));
	}

	@Test
	public void zipEntryJavaFileObject() throws Exception {
		IterableClasspath icp = new IterableClasspath(SimpleJarPath,null,false);
		JavaFileObject jfo = find(icp.iterator(),XxxClassFilename);
		assertNotNull(jfo);

		String absoluteJarPath = new File(SimpleJarPath).getAbsolutePath();
		ZipEntryJavaFileObject zejfo = (ZipEntryJavaFileObject)jfo;

		verifyClassFileJfo(zejfo);
		
		assertEquals("zip:"+absoluteJarPath+"!"+zejfo.getName(),zejfo.toUri().toString());
		assertEquals("com/foo/Xxx.class",zejfo.getName());
		assertEquals("fake\n",readContent(zejfo.openInputStream()));
		assertTrue(zejfo.isNameCompatible("Xxx", Kind.CLASS));
		assertFalse(zejfo.isNameCompatible("Xxx", Kind.SOURCE));
		assertFalse(zejfo.isNameCompatible("Bbb", Kind.CLASS));
		assertEquals(zejfo,zejfo);
		assertEquals(zejfo,find(icp.iterator(),XxxClassFilename));
	}
	
	@Test
	public void dirEntryJavaFileObject() throws Exception {
		IterableClasspath icp = new IterableClasspath(TestClassesDir,null,false);
		Iterator<JavaFileObject> iterator = icp.iterator();
		JavaFileObject jfo = find(iterator,ThisClassFilename);
		assertNotNull(jfo);
		
		File f = new File(TestClassesDir);
		String absoluteDirPath = f.getAbsolutePath();

		String thisClassName = IterableClasspathTests.class.getName().replace('.', '/')+".class";
		verifyClassFileJfo(jfo);System.out.println(jfo.getName());
		assertEquals("file:"+absoluteDirPath+File.separator+jfo.getName(),jfo.toUri().toString());
		assertEquals(thisClassName,jfo.getName());
		InputStream is = jfo.openInputStream();
		assertNotNull(is);
		is.close();
		String name = IterableClasspathTests.class.getName();
		name = name.substring(name.lastIndexOf(".")+1);
		assertTrue(jfo.isNameCompatible(name, Kind.CLASS));
		assertFalse(jfo.isNameCompatible(name, Kind.SOURCE));
		assertFalse(jfo.isNameCompatible("Bbb", Kind.CLASS));
		assertEquals(jfo,jfo);
		assertEquals(jfo,find(icp.iterator(),ThisClassFilename));
	}
	
	@Test
	public void directoriesNotUsingHasNext() throws Exception {
		IterableClasspath icp = new IterableClasspath(TestClassesDir,null,false);
		Iterator<JavaFileObject> iterator = icp.iterator();
		assertTrue(iterator.hasNext());
		String thisClass = this.getClass().getName().replace('.', '/')+".class";
		boolean found = false;
		try {
			JavaFileObject jfo;
			while ((jfo = iterator.next())!=null) {
				// Really must find ourselves
				if (jfo.getName().equals(thisClass)) {
					found = true;
					break;
				}
			}
		} catch (NoSuchElementException nsee) {
			fail("Expected to find this class");
		}
		assertTrue(found);
	}
	
	
	@Test
	public void nestedJars() throws Exception {
		IterableClasspath icp = new IterableClasspath(NestedJarPath, null, false);
		Iterator<JavaFileObject> iterator = icp.iterator();
		Assert.assertTrue(iterator.hasNext());
		
		JavaFileObject fooJfo = iterator.next();
		assertEquals("Foo.class", fooJfo.getName());
		File outerjarFile = new File(NestedJarPath);
		String outerjarpath = outerjarFile.getAbsolutePath();
		assertEquals("zip:"+outerjarpath+"!lib/innerjar.jar!Foo.class",fooJfo.toUri().toString());
		assertNotNull(fooJfo);
		assertTrue(iterator.hasNext());

		JavaFileObject barJfo = iterator.next(); // Is a NestedZipEntryJavaFileObject
		assertEquals("Bar.class", barJfo.getName());
		assertEquals("zip:"+outerjarpath+"!lib/innerjar.jar!Bar.class",barJfo.toUri().toString());
		assertFalse(iterator.hasNext());
		
		verifyClassFileJfo(barJfo);
		assertTrue(barJfo.isNameCompatible("Bar", Kind.CLASS));
		assertFalse(barJfo.isNameCompatible("Bar", Kind.SOURCE));
		assertFalse(barJfo.isNameCompatible("Foo", Kind.CLASS));
		assertEquals(barJfo,barJfo);
		assertEquals(barJfo,find(icp.iterator(),"Bar.class"));
		
		assertEquals("hello\n", readContent(fooJfo.openInputStream()));
		assertEquals("world\n", readContent(barJfo.openInputStream()));
		
		assertNotEquals(barJfo,fooJfo);
	}
	
	@Test
	public void jarsAndDirs() throws Exception {
		IterableClasspath icp = new IterableClasspath(NestedJarPath+File.pathSeparator+TestClassesDir+File.pathSeparator+SimpleJarPath, null, false);
		JavaFileObject fooClass = find(icp.iterator(),"Foo.class");
		assertNotNull(fooClass);
		JavaFileObject thisClass = find(icp.iterator(),ThisClassFilename);
		assertNotNull(thisClass);
		JavaFileObject xxxClass = find(icp.iterator(),XxxClassFilename);
		assertNotNull(xxxClass);
		JavaFileObject yyyClass = find(icp.iterator(),YyyClassFilename);
		assertNotNull(yyyClass);
		icp.close();
	}

	@Test
	public void packageFiltering() throws Exception {
		String path = NestedJarPath+File.pathSeparator+TestClassesDir+File.pathSeparator+SimpleJarPath;
		IterableClasspath icp = new IterableClasspath(path, null, false);
		Iterator<JavaFileObject> iterator = icp.iterator();
		while (iterator.hasNext()) {
			System.out.println(iterator.next().getName());
		}
		
		icp = new IterableClasspath(path,"com",true);
		assertNotNull(find(icp.iterator(),XxxClassFilename));
		assertNotNull(find(icp.iterator(),YyyClassFilename));
		assertNull(find(icp.iterator(),ThisClassFilename));
		assertNull(find(icp.iterator(),ThisClassFilename));
		assertNull(find(icp.iterator(),"Foo.class"));

		icp = new IterableClasspath(path,"com",false);
		assertNull(find(icp.iterator(),XxxClassFilename));
		assertNull(find(icp.iterator(),YyyClassFilename));
		assertNull(find(icp.iterator(),ThisClassFilename));
		assertNull(find(icp.iterator(),ThisClassFilename));
		assertNull(find(icp.iterator(),"Foo.class"));

		icp = new IterableClasspath(path,"org",true);
		assertNull(find(icp.iterator(),XxxClassFilename));
		assertNull(find(icp.iterator(),YyyClassFilename));
		assertNotNull(find(icp.iterator(),ThisClassFilename));
		assertNotNull(find(icp.iterator(),ThisClassFilename));
		assertNull(find(icp.iterator(),"Foo.class"));

		icp = new IterableClasspath(path,"org.springframework",true);
		assertNull(find(icp.iterator(),XxxClassFilename));
		assertNull(find(icp.iterator(),YyyClassFilename));
		assertNotNull(find(icp.iterator(),ThisClassFilename));
		assertNotNull(find(icp.iterator(),ThisClassFilename));
		assertNull(find(icp.iterator(),"Foo.class"));

		// Should not work because it needs to be dotted, not slashed
		exception.expect(IllegalArgumentException.class);
		icp = new IterableClasspath(path,"org/springframework",true);
	}
	
	// ---
	
	private JavaFileObject find(Iterator<JavaFileObject> iterator, String lookingFor) {
		while (iterator.hasNext()) {
			JavaFileObject jfo = iterator.next();
			// Really must find ourselves
			if (jfo.getName().equals(lookingFor)) {
				return jfo;
			}
		}
		return null;
	}

	private int countEntries(Iterator<JavaFileObject> iterator) {
		int count = 0;
		while (iterator.hasNext()) {
			count++;
			iterator.next();
		}
		return count;
	}

	public static String readContent(InputStream is) throws Exception {
		byte[] bs = new byte[100000];
		int offset = 0;
		int read = -1;
		while ((read=is.read(bs,offset,100000-offset))!=-1) {
			offset+=read;
		}
		return new String(bs,0,offset);
	}

	public static String readContent(Reader r) throws Exception {
		char[] bs = new char[100000];
		int offset = 0;
		int read = -1;
		while ((read=r.read(bs,offset,100000-offset))!=-1) {
			offset+=read;
		}
		return new String(bs,0,offset);
	}

	/**
	 * Check some common behaviours for JavaFileObjects that represent classes.
	 */
	private void verifyClassFileJfo(JavaFileObject jfo) throws Exception {
		try {
			jfo.openOutputStream();
			fail("openOutputStream() should not work");
		} catch (IllegalStateException ise) {
			// expected
		}
		try {
			jfo.openWriter();
			fail("openWriter() should not work");
		} catch (IllegalStateException ise) {
			// expected
		}
		assertFalse(jfo.delete());
		assertEquals(JavaFileObject.Kind.CLASS,jfo.getKind());
		long lmt = jfo.getLastModified();
		if (lmt<=0) {
			fail("Expected a real last modified time, not: "+lmt);
		}
		assertNull(jfo.getNestingKind()); // null indicates unknown
		assertNull(jfo.getAccessLevel()); // null indicates unknown
		try {
			jfo.getCharContent(true);
			fail("getCharContent() should not work");
		} catch (UnsupportedOperationException uoe) {
			// expected
		}
		try {
			jfo.openReader(true);
			fail("openReader() should not work");
		} catch (UnsupportedOperationException uoe) {
			// expected
		}
		jfo.hashCode();
	}

}
