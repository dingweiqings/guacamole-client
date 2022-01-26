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
import org.apache.guacamole.GuacamoleException;
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
 * Ws ---> Bridger ----> Guacamole Server(GS)
 * Ws  &lt;--- Bridger &lt;---- Guacamole Server(GS)
 */
public class Bridger {
	/**
	 * Logger
	 */
	Logger logger = LoggerFactory.getLogger(Bridger.class);
	/**
	 * Bridger <-> Guacamole Server , Channel to communicate with GS
	 */
	Channel channel;
	/**
	 * Ws  <-> Bridger  ,WsSession to communicate with ws client
	 */
	WsSession session;
	/**
	 *  Bridger use this GuacamoleConfiguration when handshake with GS
	 */
	GuacamoleConfiguration config;
	/**
	 * Decoder for ws
	 */
	WsDecoder browserDecoder;
	/**
	 * guacamole connection id
	 *
	 */
	private String id;
	/**
	 * ws connection uuid
	 */
	private UUID uuid = UUID.randomUUID();

	/***
	 * Constructor
	 * @param channel GS channel  in this Bridger
	 * @param session  Ws session in this bridger
	 * @param configuration  guacamole configuration in this bridger
	 */
	public Bridger(Channel channel, WsSession session, GuacamoleConfiguration configuration) {
		this.channel = channel;
		this.session = session;
		this.config = configuration;
		this.browserDecoder = new WsDecoder();
	}

	/**
	 * Get property
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 *
	 * @param configuration
	 * guacamole configuration in this bridger
	 */
	public Bridger(GuacamoleConfiguration configuration){
		this.config=configuration;
		this.browserDecoder=new WsDecoder();
	}
	/**
	 * Get property
	 */
	public GuacamoleConfiguration getConfig() {
		return config;
	}

	/**
	 * Set property
	 * @param config   configuration in this bridger
	 */
	public void setConfig(GuacamoleConfiguration config) {
		this.config = config;
	}
	/**
	 * Get property
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set property
	 * @param id id of the connection communicating with Guacamole Server
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * Get property
	 */
	public UUID getUUID() {
		return uuid;
	}
	/**
	 * Set property ,Ws uuid
	 */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	/**
	 * close left socket and right tcp channel
	 * @throws IOException
	 */
	public void close() throws IOException {
		channel.close();
		session.close();
	}

	/**
	 * Send instruction to ws client
	 * @param instruction which to send ws client
	 * @throws GuacamoleConnectionClosedException
	 * @throws GuacamoleClientException
	 * @throws IOException
	 */
	public void sendInstruction(GuacamoleInstruction instruction) throws GuacamoleConnectionClosedException,GuacamoleClientException,IOException {
		session.sendText(instruction.toString());
	}

	/**
	 * When bridger receives guacamole instruction , then send to ws peer in browser
	 *
	 * @param ins
	 */
	public void receiveGuacamoleInstruction(GuacamoleInstruction ins) {
		try {
			session.sendText(ins.toString());
		} catch (IOException e) {
			logger.error("Bridger send msg to  error",e);
			try {
				close();
			} catch (IOException ex) {
				logger.error("Bridger send msg to  error",ex);
			}
		}
	}


	/**
	 * When bridger get msg from ws peer ,then send to guacamole server
	 *
	 * @param msg
	 */
	public void onMessage(String msg) {
		if(logger.isTraceEnabled()){
			logger.info("Send msg to guacd {}",msg);
		}
		GuacamoleInstruction instruction = null;
		try {
			instruction = browserDecoder.decode(msg);
		} catch (GuacamoleException e) {
			logger.error("Decoder ws msg error ",e);
		}
		//handle ws msg by code
		if (instruction.getOpcode().equals(GuacamoleTunnel.INTERNAL_DATA_OPCODE) || instruction.getOpcode().equals("nop")) {

			// Respond to ping requests
			List<String> args = instruction.getArgs();
			//ws ping
			if (args.size() >= 2 && args.get(0).equals("ping")) {
				try {
					session.sendText(
							new GuacamoleInstruction(
									GuacamoleTunnel.INTERNAL_DATA_OPCODE,
									"ping", args.get(1)
							).toString()
					);
				} catch (IOException e) {
					logger.error("Send msg to ws peer error",e);
				}
			}
			return;
		}

		writeGuacamoleInstruction(instruction);
	}

	/**
	 * Send instruction to GS
	 * @param instruction
	 * @return
	 */
	public ChannelFuture writeGuacamoleInstruction(GuacamoleInstruction instruction) {
		return channel.writeAndFlush(instruction);
	}

	/**
	 * say hello
	 * @throws GuacamoleServerException
	 */
	public void sayHello() throws GuacamoleServerException {

		// Get protocol / connection ID
		String select_arg = config.getConnectionID();
		if (select_arg == null)
			select_arg = config.getProtocol();

		// Send requested protocol or connection ID
		writeGuacamoleInstruction(new GuacamoleInstruction("select", select_arg));
	}

}
