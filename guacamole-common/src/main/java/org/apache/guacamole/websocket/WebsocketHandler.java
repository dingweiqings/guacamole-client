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
import org.apache.guacamole.nio.TransferTunnel;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 使用websocket时，继承这个endpoint，在和浏览器onOpen，onMessage 调用即可
 * @author kurt.ding
 * @date 2022/1/7 下午2:49
 */
public abstract class WebsocketHandler {
	public static final Map<String, TransferTunnel> GUACAD_HAND_MAP=new ConcurrentHashMap<>();

	public static final Map<String,TransferTunnel>  WS_HAND_MAP=new ConcurrentHashMap<>();

	public abstract  GuacdProperties getGuacdProperties();

	public abstract GuacamoleConfiguration getGuacamoleConfiguration();

	public WebsocketHandler() {
	}

	public void onOpen(Session session) {
		ConnectionHelper connectionHelper = ConnectionHelper.getInstance(getGuacdProperties());
		WsSession wsSession = new TomcatWsSession(session);
		TransferTunnel tunnel = connectionHelper.openConnection(wsSession, getGuacamoleConfiguration());
		GUACAD_HAND_MAP.put(tunnel.getChannel().id().toString(), tunnel);
		WS_HAND_MAP.put(wsSession.id(), tunnel);
	}

	public TransferTunnel getTransferTunnel(Session session){
		return WS_HAND_MAP.get(session.getId());
	}

}
