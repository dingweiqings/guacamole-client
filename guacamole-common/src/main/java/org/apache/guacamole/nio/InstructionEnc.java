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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author kurt.ding
 * @date 2021/11/25 下午4:40
 * ws server -> browser,guaca
 */
public class InstructionEnc extends MessageToByteEncoder<GuacamoleInstruction> {
	Logger logger = LoggerFactory.getLogger(InstructionEnc.class);

	@Override
	protected void encode(ChannelHandlerContext ctx, GuacamoleInstruction instruction, ByteBuf out) throws Exception {
		//	logger.info("Encode to byte buf ,send to guacad  {}", instruction);
		ByteBuf buffer = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(instruction.toString()), StandardCharsets.UTF_8);
		out.writeBytes(buffer);
		buffer.release();
	}
}
