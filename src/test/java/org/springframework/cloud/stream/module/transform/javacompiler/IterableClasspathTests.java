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

import java.io.InputStream;
import java.util.Iterator;

import javax.tools.JavaFileObject;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * @author Andy Clement
 */
public class IterableClasspathTests {

	@Test
	public void nestedJars() throws Exception {
		IterableClasspath icp = new IterableClasspath("target/test-classes/outerjar.jar", null, false);
		Iterator<JavaFileObject> iterator = icp.iterator();
		Assert.assertTrue(iterator.hasNext());
		
		JavaFileObject foo = iterator.next();
		assertEquals("Foo.class", foo.getName());
		System.out.println(foo.toUri().getPath());
		assertEquals("zip:target/test-classes/outerjar.jar!lib/innerjar.jar!Foo.class",foo.toUri().toString());
		assertNotNull(foo);
		assertTrue(iterator.hasNext());

		JavaFileObject bar = iterator.next();
		assertEquals("Bar.class", bar.getName());
		assertEquals("zip:target/test-classes/outerjar.jar!lib/innerjar.jar!Bar.class",bar.toUri().toString());
		assertFalse(iterator.hasNext());
		
		InputStream is = foo.openInputStream();
		byte[] bs = new byte[100];
		int i = is.read(bs);
		Assert.assertEquals("hello\n", new String(bs, 0, i));
		
		is = bar.openInputStream();
		bs = new byte[100];
		i = is.read(bs);
		Assert.assertEquals("world\n", new String(bs, 0, i));
		
		
	}

}
