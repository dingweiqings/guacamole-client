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
package org.apache.guacamole.websocket;

import org.apache.guacamole.nio.ConnectionHelper;
import org.apache.guacamole.nio.GuacdProperties;
import org.apache.guacamole.nio.Bridger;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Websocket Handler Adapter , override onOpen, and add custom onMessage logic code
 */
public abstract class WebsocketHandlerAdapter {
	public static final Map<String, Bridger> GUACAD_HAND_MAP=new ConcurrentHashMap<>();

	public static final Map<String, Bridger>  WS_HAND_MAP=new ConcurrentHashMap<>();

	public abstract  GuacdProperties getGuacdProperties();

	public abstract GuacamoleConfiguration getGuacamoleConfiguration();

	public WebsocketHandlerAdapter() {
	}

	/**
	 *	Ws open connection
	 * @param session WsSession
	 */
	public void onOpen(Session session) {
		ConnectionHelper connectionHelper = ConnectionHelper.getInstance(getGuacdProperties());
		WsSession wsSession = new TomcatWsSession(session);
		Bridger tunnel = connectionHelper.openConnection(wsSession, getGuacamoleConfiguration());
		GUACAD_HAND_MAP.put(tunnel.getChannel().id().toString(), tunnel);
		WS_HAND_MAP.put(wsSession.id(), tunnel);
	}

	/**
	 * Get property
	 * @param session
	 */
	public Bridger getTransferTunnel(Session session){
		return WS_HAND_MAP.get(session.getId());
	}

}
