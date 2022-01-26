/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.guacamole.nio;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * nio client connection group
 */
public class BridgerBootstrap {
	/**
	 * Logger
	 */
	Logger logger = LoggerFactory.getLogger(BridgerBootstrap.class);

	/**
	 * Timeout config
	 */
	private final static long CONNECT_WAIT_TIMEOUT = 1000;
	/**
	 * Guacamole properties
	 */
	private final GuacdProperties guacdProperties;

	/**
	 * create  nio event group
	 */
	private EventLoopGroup group = new NioEventLoopGroup();

	/**
	 * client bootstrap
	 */

	private Bootstrap bootstrap = new Bootstrap();

	/**
	 * Constructor
	 * @param guacdProperties  guacamole server host and port
	 */
	public BridgerBootstrap(GuacdProperties guacdProperties) {
		this.guacdProperties = guacdProperties;
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
				.group(group).channel(NioSocketChannel.class)

				.handler(new GuacamoleBridgerPipeline());
	}

	/**
	 * Connect to Guacamole Server
	 * @throws IOException
	 * 			it occurs timeout or error when channel connects guacamole server  throw IOException
	 */
	public Channel connect() throws IOException {
		ChannelFuture f = bootstrap.connect(guacdProperties.getHostname(), guacdProperties.getPort());
		f.awaitUninterruptibly(CONNECT_WAIT_TIMEOUT);
		if (f.isCancelled()) {
			// Connection attempt cancelled by user
			logger.debug("Connect cancelled by client");
		} else if (!f.isSuccess()) {
			logger.error("Connect error {}", f.cause().getCause());
		} else {
			logger.info("Connect host {} port {} success", guacdProperties.getHostname(), guacdProperties.getPort());
			return f.channel();
		}
		throw new IOException("Connection error");
	}
}
