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

	GuacaTransferBootstrap guacaTransferBootstrap ;

	GuacdProperties guacdProperties;
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
		guacaTransferBootstrap = new GuacaTransferBootstrap(guacdProperties);
	}

	private  TransferTunnel createTunnel(GuacamoleConfiguration configuration){
		return new TransferTunnel(configuration);
	}
	public TransferTunnel openConnection(WsSession wsSession, GuacamoleConfiguration  configuration) {
		TransferTunnel tunnel = createTunnel(configuration);
		tunnel.session = wsSession;
		try {
			tunnel.channel = guacaTransferBootstrap.connect();
			//TunnelEndpointNetty.GUACAD_HAND_MAP.put(channel!!.id().toString(), this)
			//TunnelEndpointNetty.BROWSER_HAND_MAP.put(session!!.id().toString(), this)
			tunnel.handshake();
		} catch (GuacamoleException e) {
			logger.warn("Create tunnel failure ", e);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			// open时，先告诉前端 tunnel的uuid
			tunnel.sendInstruction(
					new GuacamoleInstruction(
							GuacamoleTunnel.INTERNAL_DATA_OPCODE,
							tunnel.getUUID().toString()
				)
			);
			//TODO register netty io loop,when get guacamole server msg, get access handler send to broswer
			//服务器宕机，如何恢复
			//路由表

		}                    // Catch any thrown guacamole exception and attempt
		// to pass within the WebSocket connection, logging
		// each error appropriately.
		catch (GuacamoleClientException e) {
			logger.info("WebSocket connection terminated: {}", e.getMessage());
			logger.debug("WebSocket connection terminated due to client error.", e);
			closeConnection(tunnel,
					getUseSession(tunnel), e.getStatus().getGuacamoleStatusCode(),
					e.getWebSocketCode()
			);
		} catch (GuacamoleConnectionClosedException e) {
			logger.debug("Connection to guacd closed.", e);
			closeConnection(tunnel,getUseSession(tunnel), GuacamoleStatus.SUCCESS);
		} catch (GuacamoleException e) {
			logger.error("Connection to guacd terminated abnormally: {}", e.getMessage());
			logger.debug("Internal error during connection to guacd.", e);
			closeConnection(tunnel,
					tunnel.session, e.getStatus().getGuacamoleStatusCode(),
					e.getWebSocketCode()
			);
		} catch (IOException e) {
			logger.debug("I/O error prevents further reads.", e);
			closeConnection(tunnel,getUseSession(tunnel), GuacamoleStatus.SERVER_ERROR);
		}
		return tunnel;
	}
	private void closeConnection(TransferTunnel tunnel, WsSession session, GuacamoleStatus guacStatus) {
		this.closeConnection(tunnel,session, guacStatus.getGuacamoleStatusCode(), guacStatus.getWebSocketCode());
	}

	private void closeConnection(TransferTunnel tunnel, WsSession wsSession, int guacamoleStatusCode, int webSocketCode) {
		try {
			CloseReason.CloseCode code = CloseReason.CloseCodes.getCloseCode(webSocketCode);
			String message = Integer.toString(guacamoleStatusCode);
			//TODO send close reason to user
			tunnel.session.sendText(message);
			logger.info("Close session {}  {} {} ", tunnel.session.id(), code, message);
			tunnel.close();

		} catch (IOException e) {
			logger.debug("Unable to close WebSocket connection.", e);
		}
	}
	private WsSession getUseSession(TransferTunnel tunnel){
		return tunnel.session;
	}
}
