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

import org.apache.guacamole.DecoderWsMsgException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.protocol.GuacamoleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * <p>
 *     ws server
 */
public class WsDecoder {
	/**
	 * Logger for this class.
	 */
	Logger logger = LoggerFactory.getLogger(WsDecoder.class);

	/**
	 * Guacamole instruction parser.
	 */
	GuacamoleParser parser = new GuacamoleParser();

	/**
	 * Parser Instruction
	 * @param str str decode to instruction
	 * @return
	 * @throws GuacamoleException
	 */
	public GuacamoleInstruction decode(String str) throws GuacamoleException {
		int length = str.length();
		int offset = 0;
		while (length > 0) {

			// Pass as much data through the parser as possible
			int parsed;
			while ((parsed = parser.append(str.toCharArray(), offset, length)) != 0) {
				offset += parsed;
				length -= parsed;
			}

			// If no instruction is available, it must be incomplete
			if (!parser.hasNext())
				throw new DecoderWsMsgException("Filtered write() contained an incomplete instruction.");

			// Write single instruction through filter
		}
		return parser.next();
	}
}
