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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleConnectionClosedException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.websocket.WsSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * //TODO  refactor this to an interface
 */
public class TransferTunnel {
	Logger logger = LoggerFactory.getLogger(TransferTunnel.class);
	Channel channel;
	WsSession session;

	GuacamoleConfiguration config;
	GuacaBrowserDecoder browserDecoder;

	private String id;
	private UUID uuid = UUID.randomUUID();

	public TransferTunnel(Channel channel, WsSession session, GuacamoleConfiguration configuration) {
		this.channel = channel;
		this.session = session;
		this.config = configuration;
		this.browserDecoder = new GuacaBrowserDecoder();
	}

	public Channel getChannel() {
		return channel;
	}

	public TransferTunnel(GuacamoleConfiguration configuration){
		this.config=configuration;
		this.browserDecoder=new GuacaBrowserDecoder();
	}

	public GuacamoleConfiguration getConfig() {
		return config;
	}

	public void setConfig(GuacamoleConfiguration config) {
		this.config = config;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public UUID getUUID() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * 关闭维持的两个socket
	 */
	public void close() throws IOException {
		channel.close();
		session.close();
	}

	public void sendInstruction(GuacamoleInstruction instruction) throws GuacamoleConnectionClosedException,GuacamoleClientException,IOException {
		session.sendText(instruction.toString());
	}

	/**
	 * 转发给浏览器
	 *
	 * @param instruction
	 */
	public void receiveGuacadMsg(GuacamoleInstruction instruction) {
		try {
			session.sendText(instruction.toString());
		} catch (IOException e) {
			e.printStackTrace();
			try {
				close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}


	/**
	 * 发送给guaca
	 *
	 * @param msg
	 */
	public void onMessage(String msg) {
		logger.info("Send msg to guacd {}",msg);
		GuacamoleInstruction instruction = null;
		try {
			instruction = browserDecoder.decode(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (instruction.getOpcode().equals(GuacamoleTunnel.INTERNAL_DATA_OPCODE) || instruction.getOpcode().equals("nop")) {

			// Respond to ping requests
			List<String> args = instruction.getArgs();
			//如果是和ws server的ping 则，回复
			if (args.size() >= 2 && args.get(0).equals("ping")) {
				try {
					session.sendText(
							new GuacamoleInstruction(
									GuacamoleTunnel.INTERNAL_DATA_OPCODE,
									"ping", args.get(1)
							).toString()
					);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//如果是和ws server 间的握手，忽略
			return;
		}

		writeGuacadInstruction(instruction);
	}

	public ChannelFuture writeGuacadInstruction(GuacamoleInstruction instruction) {
		return channel.writeAndFlush(instruction);
	}

	public void handshake() throws GuacamoleServerException {

		// Get protocol / connection ID
		String select_arg = config.getConnectionID();
		if (select_arg == null)
			select_arg = config.getProtocol();

		// Send requested protocol or connection ID
		writeGuacadInstruction(new GuacamoleInstruction("select", select_arg)).addListener((future) -> {
			logger.info("Init tunnel future {}", future.get());
		});
	}

	public GuacamoleInstruction expect(GuacamoleInstruction instruction, String opcode) throws GuacamoleServerException {
		if (instruction == null)
			throw new GuacamoleServerException("End of stream while waiting for \"" + opcode + "\".");

		// Ensure instruction has expected opcode
		if (!instruction.getOpcode().equals(opcode))
			throw new GuacamoleServerException("Expected \"" + opcode + "\" instruction but instead received \"" + instruction.getOpcode() + "\".");

		return instruction;
	}
}
