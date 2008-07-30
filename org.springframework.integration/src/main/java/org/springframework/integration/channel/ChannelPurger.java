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

package org.springframework.integration.channel;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * A utility class for purging {@link Message Messages} from one or more
 * {@link PollableChannel PollableChannels}. Any message that does <em>not</em>
 * match the provided {@link MessageSelector} will be removed from the channel.
 * If no {@link MessageSelector} is provided, then <em>all</em> messages will be
 * cleared from the channel.
 * <p>
 * Note that the {@link #purge()} method operates on a snapshot of the messages
 * within a channel at the time that the method is invoked. It is therefore
 * possible that new messages will arrive on the channel during the purge
 * operation and thus will <em>not</em> be removed. Likewise, messages to be
 * purged may have been removed from the channel while the operation is taking
 * place. Such messages will not be included in the returned list.
 * 
 * @author Mark Fisher
 */
public class ChannelPurger {

	private final PollableChannel[] channels;

	private final MessageSelector selector;


	public ChannelPurger(PollableChannel ... channels) {
		this(null, channels);
	}

	public ChannelPurger(MessageSelector selector, PollableChannel ... channels) {
		Assert.notEmpty(channels, "at least one channel is required");
		if (channels.length == 1) {
			Assert.notNull(channels[0], "channel must not be null");
		}
		this.selector = selector;
		this.channels = channels;
	}


	public final List<Message<?>> purge() {
		List<Message<?>> purgedMessages = new ArrayList<Message<?>>();
		for (PollableChannel channel : this.channels) {
			List<Message<?>> results = (this.selector == null) ?
					channel.clear() : channel.purge(this.selector);
			if (results != null) {
				purgedMessages.addAll(results);
			}
		}
		return purgedMessages;
	}

}
