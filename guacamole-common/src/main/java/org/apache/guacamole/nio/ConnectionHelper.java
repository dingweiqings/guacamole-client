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

import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleConnectionClosedException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.protocol.GuacamoleStatus;
import org.apache.guacamole.websocket.WsSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import java.io.IOException;

/**
 * Connection Helper class
 */
public class ConnectionHelper {
	/**
	 * Logger for this class
	 */
	Logger logger= LoggerFactory.getLogger(ConnectionHelper.class);
	/**
	 * Bootstrap
	 */
	BridgerBootstrap bridgerBootstrap;

	/**
	 * Guacamole Properties
	 */
	GuacdProperties guacdProperties;

	/**
	 * SINGLE INSTANCE
	 */
	private static ConnectionHelper INSTANCE;

	public static ConnectionHelper getInstance(GuacdProperties properties) {
		if (INSTANCE == null) {
			synchronized (ConnectionHelper.class) {
				if (INSTANCE == null) {
					INSTANCE = new ConnectionHelper(properties);
				}
			}
		}
		return INSTANCE;
	}

	private ConnectionHelper(GuacdProperties guacdProperties) {
		this.guacdProperties=guacdProperties;
		bridgerBootstrap = new BridgerBootstrap(guacdProperties);
	}

	/**
	 * Create tunnel
	 * @param configuration  guacamole configuration
	 */
	private Bridger createOneBridger(GuacamoleConfiguration configuration){
		return new Bridger(configuration);
	}

	/**
	 * Open connection
	 * @param wsSession  websocket session
	 * @param configuration  guacamole configuration
	 * @return
	 */
	public Bridger openConnection(WsSession wsSession, GuacamoleConfiguration  configuration) {
		Bridger tunnel = createOneBridger(configuration);
		tunnel.session = wsSession;
		try {
			tunnel.channel = bridgerBootstrap.connect();
			//TunnelEndpointNetty.GUACAD_HAND_MAP.put(channel!!.id().toString(), this)
			//TunnelEndpointNetty.BROWSER_HAND_MAP.put(session!!.id().toString(), this)
			tunnel.sayHello();
		} catch (GuacamoleException e) {
			logger.warn("Create tunnel failure ", e);
		} catch (IOException e) {
			logger.warn("Create tunnel failure ", e);
		}
		try {
			// open,send tunnel uuid
			tunnel.sendInstruction(
					new GuacamoleInstruction(
							GuacamoleTunnel.INTERNAL_DATA_OPCODE,
							tunnel.getUUID().toString()
				)
			);
		}
		// Catch any thrown guacamole exception and attempt
		// to pass within the WebSocket connection, logging
		// each error appropriately.
		catch (GuacamoleClientException e) {
			logger.info("WebSocket connection terminated: {}", e.getMessage());
			logger.debug("WebSocket connection terminated due to client error.", e);
			closeConnection(tunnel,
					 e.getStatus().getGuacamoleStatusCode(),
					e.getWebSocketCode()
			);
		} catch (GuacamoleConnectionClosedException e) {
			logger.error("Connection to guacamole server closed.", e.getMessage());
			logger.debug("Connection to guacamole server closed.", e);
			closeConnection(tunnel, GuacamoleStatus.SUCCESS);
		} catch (IOException e) {
			logger.error("It occurs I/O error when send instruction to guacamole server.", e.getMessage());
			logger.debug("I/O error prevents further reads.", e);
			closeConnection(tunnel, GuacamoleStatus.SERVER_ERROR);
		}
		return tunnel;
	}

	/**
	 * Close connection
	 * @param bridger the bridger that need to close
	 * @param guacadStatus close status given by  guacamole server
	 */
	private void closeConnection(Bridger bridger,  GuacamoleStatus guacadStatus) {
		this.closeConnection(bridger, guacadStatus.getGuacamoleStatusCode(), guacadStatus.getWebSocketCode());
	}

	/**
	 * Close connection
	 * @param bridger  the bridger that need to close
	 * @param guacamoleStatusCode close status given by  guacamole server
	 * @param webSocketCode  ws status code given by ws client or ws server
	 */
	private void closeConnection(Bridger bridger,  int guacamoleStatusCode, int webSocketCode) {
		try {
			CloseReason.CloseCode code = CloseReason.CloseCodes.getCloseCode(webSocketCode);
			String message = Integer.toString(guacamoleStatusCode);
			//TODO send close reason to user
			bridger.session.sendText(message);
			logger.info("Close session {}  {} {} ", bridger.session.id(), code, message);
			bridger.close();

		} catch (IOException e) {
			logger.debug("Unable to close WebSocket connection.", e);
		}
	}
}
