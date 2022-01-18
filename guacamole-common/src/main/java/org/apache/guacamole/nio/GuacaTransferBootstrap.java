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
 * @author kurt.ding
 * @date 2021/11/24 下午9:13
 */
public class GuacaTransferBootstrap  {
	private final static long CONNECT_WAIT_TIMEOUT = 1000;
	private final GuacdProperties guacdProperties;
	Logger logger = LoggerFactory.getLogger(GuacaTransferBootstrap.class);
	/// 通过nio方式来接收连接和处理连接,默
	private EventLoopGroup group = new NioEventLoopGroup();
	private Bootstrap bootstrap = new Bootstrap();

	public GuacaTransferBootstrap(GuacdProperties guacdProperties) {
		this.guacdProperties = guacdProperties;
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
				.group(group).channel(NioSocketChannel.class)

				.handler(new GuacaTransferPipeline());
	}

	public Channel connect() throws InterruptedException, IOException {
		//同步等待连接服务端
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

	public Bootstrap getBootstrap() {
		return bootstrap;
	}
}
