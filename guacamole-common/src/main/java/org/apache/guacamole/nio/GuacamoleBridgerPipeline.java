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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.nio.charset.StandardCharsets;

/**
 *  Netty pipeline
 */
public class GuacamoleBridgerPipeline extends ChannelInitializer<SocketChannel> {
	/**
	 * MAX FRAME LENGTH
	 */
	private final static int MAX_FRAME_LENGTH = 1024 * 1024;

	/**
	 * Init channel
	 * @param ch socket channel
	 */
	@Override
	protected void initChannel(SocketChannel ch) {
		ChannelPipeline ph = ch.pipeline();
		ByteBuf delimiter = Unpooled.copiedBuffer(";".getBytes(StandardCharsets.UTF_8));
		ph.addFirst("framer", new DelimiterBasedFrameDecoder(MAX_FRAME_LENGTH, false, delimiter));
		ph.addLast("decoder", new InstructionDecoder());
		ph.addLast("handler", new GuacamoleHandler()); //handle  msg from guacamole
		ph.addLast("encoder", new InstructionEnc());
	}
}