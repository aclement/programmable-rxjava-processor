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

package org.springframework.cloud.stream.module.transform;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.stream.annotation.Bindings;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration Tests for the Programmable RxJava Processor.
 *
 * @author Andy Clement
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ProgrammableRxJavaProcessorApplication.class)
@WebIntegrationTest(randomPort = true)
@DirtiesContext
public abstract class ProgrammableRxJavaProcessorIntegrationTests {

	@Autowired
	@Bindings(RxJavaTransformer.class)
	protected Processor channels;

	@Autowired
	protected MessageCollector collector;
	
	@WebIntegrationTest({"code=return input -> input.buffer(5).map(list->list.get(4));"})
	public static class UsingScriptIntegrationTests extends ProgrammableRxJavaProcessorIntegrationTests {
		@Test
		public void testSimple() {
			channels.input().send(new GenericMessage<Object>(100));
			channels.input().send(new GenericMessage<Object>(200));
			channels.input().send(new GenericMessage<Object>(300));
			channels.input().send(new GenericMessage<Object>(400));
			channels.input().send(new GenericMessage<Object>(500));
			assertThat(collector.forChannel(channels.output()), receivesPayloadThat(is(500)));
		}
	}

}
