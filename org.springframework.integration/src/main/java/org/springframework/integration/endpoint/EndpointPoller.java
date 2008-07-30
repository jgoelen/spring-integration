/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.MessageExchangeTemplate;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.PollableSource;

/**
 * @author Mark Fisher
 */
public class EndpointPoller implements EndpointVisitor {

	private final MessageExchangeTemplate template;


	public EndpointPoller() {
		 this.template = new MessageExchangeTemplate();
		 this.template.setSendTimeout(0);
	}

	public void visitEndpoint(MessageEndpoint endpoint) {
		MessageSource<?> source = endpoint.getSource();
		if (source == null) {
			throw new ConfigurationException("unable to poll for endpoint '"
					+ endpoint + "', source is null");
		}
		if (!(source instanceof PollableSource)) {
			throw new ConfigurationException("unable to poll for endpoint '"
					+ endpoint + ", source is not a PollableSource");
		}
		this.template.receiveAndForward((PollableSource<?>) source, endpoint);
	}

}
