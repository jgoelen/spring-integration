/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.xml.config;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Node;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.xml.transformer.support.XPathExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MultiValueMap;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class XPathHeaderEnricherParserTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private ApplicationContext context;

	private final Message<?> message =
			MessageBuilder.withPayload("<person name='John Doe' age='42' married='true'/>").build();


	@Test
	public void testParse() {
		EventDrivenConsumer consumer = (EventDrivenConsumer) context.getBean("parseOnly");
		assertEquals(2, TestUtils.getPropertyValue(consumer, "handler.order"));
		assertEquals(123L, TestUtils.getPropertyValue(consumer, "handler.messagingTemplate.sendTimeout"));
		assertEquals(-1, TestUtils.getPropertyValue(consumer, "phase"));
		assertFalse(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class));
		SmartLifecycleRoleController roleController = context.getBean(SmartLifecycleRoleController.class);
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.getPropertyValue(roleController, "lifecycles",
				MultiValueMap.class).get("foo");
		assertThat(list, contains((SmartLifecycle) consumer));
	}

	@Test
	public void stringResultByDefault() {
		Message<?> result = this.getResultMessage();
		assertEquals("John Doe", result.getHeaders().get("name"));
	}

	@Test
	public void numberResult() {
		Message<?> result = this.getResultMessage();
		assertEquals(42, result.getHeaders().get("age"));
	}

	@Test
	public void booleanResult() {
		Message<?> result = this.getResultMessage();
		assertEquals(Boolean.TRUE, result.getHeaders().get("married"));
	}

	@Test
	public void nodeResult() {
		Message<?> result = this.getResultMessage();
		Object header = result.getHeaders().get("node-test");
		assertTrue(header instanceof Node);
		Node node = (Node) header;
		assertEquals("42", node.getTextContent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nodeListResult() {
		Message<?> result = this.getResultMessage();
		Object header = result.getHeaders().get("node-list-test");
		assertThat(header, instanceOf(List.class));
		List<Node> nodeList = (List<Node>) header;
		assertNotNull(nodeList);
		assertEquals(3, nodeList.size());
	}

	@Test
	public void expressionRef() {
		Message<?> result = getResultMessage();
		assertEquals(84d, result.getHeaders().get("ref-test"));
	}

	@Test
	public void testDefaultHeaderEnricher() {
		assertFalse(getEnricherProperty("defaultHeaderEnricher", "defaultOverwrite"));
		assertTrue(getEnricherProperty("defaultHeaderEnricher", "shouldSkipNulls"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCustomHeaderEnricher() {
		assertTrue(getEnricherProperty("customHeaderEnricher", "defaultOverwrite"));
		assertFalse(getEnricherProperty("customHeaderEnricher", "shouldSkipNulls"));
		Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd =
				TestUtils.getPropertyValue(this.context.getBean("customHeaderEnricher"),
						"handler.transformer.headersToAdd", Map.class);
		HeaderValueMessageProcessor<?> headerValueMessageProcessor = headersToAdd.get("foo");
		assertThat(headerValueMessageProcessor, instanceOf(XPathExpressionEvaluatingHeaderValueMessageProcessor.class));
		assertSame(this.context.getBean("xmlPayloadConverter"),
				TestUtils.getPropertyValue(headerValueMessageProcessor, "converter"));
	}

	@Test
	public void childOverridesDefaultOverwrite() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> request = MessageBuilder.fromMessage(this.message)
				.setHeader("foo", "bar")
				.setReplyChannel(replyChannel)
				.build();
		this.context.getBean("defaultInput", MessageChannel.class).send(request);
		Message<?> reply = replyChannel.receive();
		assertNotNull(reply);
		assertEquals("John Doe", reply.getHeaders().get("foo"));
	}

	@Test
	public void childOverridesCustomOverwrite() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> request = MessageBuilder.fromMessage(this.message)
				.setHeader("foo", "bar")
				.setReplyChannel(replyChannel)
				.build();
		this.context.getBean("customInput", MessageChannel.class).send(request);
		Message<?> reply = replyChannel.receive();
		assertNotNull(reply);
		assertEquals("bar", reply.getHeaders().get("foo"));
	}


	private Message<?> getResultMessage() {
		this.input.send(message);
		return output.receive(0);
	}

	private boolean getEnricherProperty(String beanName, String propertyName) {
		Object endpoint = this.context.getBean(beanName);
		Object handler = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Object enricher = new DirectFieldAccessor(handler).getPropertyValue("transformer");
		return (boolean) new DirectFieldAccessor(enricher).getPropertyValue(propertyName);
	}

}
