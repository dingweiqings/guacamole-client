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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * guacamole server -> ws server
 */
public class InstructionDecoder extends ByteToMessageDecoder {
	/**
	 * Logger
	 */
	Logger logger = LoggerFactory.getLogger(InstructionDecoder.class);

	/**
	 * Read instruction
	 * @param str  str decode to instruction
	 * @return GuacamoleInstruction
	 */
	public GuacamoleInstruction readInstruction(String str){
		// Get instruction
		//logger.info("Decoder {}",str);
		char[] instructionBuffer = str.toCharArray();
		// If EOF, return EOF

		// Start of element
		int elementStart = 0;

		// Build list of elements
		Deque<String> elements = new LinkedList<String>();
		while (elementStart < instructionBuffer.length) {

			// Find end of length
			int lengthEnd = -1;
			//find dot position
			for (int i = elementStart; i < instructionBuffer.length; i++) {
				if (instructionBuffer[i] == '.') {
					lengthEnd = i;
					break;
				}
			}

			// Parse length
			int length = Integer.parseInt(new String(
					instructionBuffer,
					elementStart,
					lengthEnd - elementStart
			));

			//提取点号后面的元素
			elementStart = lengthEnd + 1;
			String element = new String(
					instructionBuffer,
					elementStart,
					length
			);

			// Append element to list of elements
			elements.addLast(element);

			// Read terminator after element
			elementStart += length;
			char terminator = instructionBuffer[elementStart];

			// Continue reading instructions after terminator
			elementStart++;

			// If we've reached the end of the instruction
			if (terminator == ';')
				break;

		}

		// Pull opcode off elements list
		String opcode = elements.removeFirst();
		if(logger.isTraceEnabled()){
			logger.info("Read instruction from guacamole  op code {},element {}", opcode, elements);
		}
		// Return parsed instruction
		return new GuacamoleInstruction(
				opcode,
				elements.toArray(new String[0])
		);

	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// Start of element
		//	in.markReaderIndex();
		byte[] bytes = new byte[in.readableBytes()];
		in.readBytes(bytes);
		out.add(readInstruction(new String(bytes)));
	}
}
