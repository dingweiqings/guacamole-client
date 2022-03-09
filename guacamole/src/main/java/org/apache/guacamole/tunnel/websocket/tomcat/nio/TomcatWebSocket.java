package org.apache.guacamole.tunnel.websocket.tomcat.nio;
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

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.nio.Bridger;
import org.apache.guacamole.nio.ConnectionHelper;
import org.apache.guacamole.nio.GuacdProperties;
import org.apache.guacamole.rest.auth.AuthenticationService;
import org.apache.guacamole.tunnel.TunnelRequest;
import org.apache.guacamole.tunnel.http.HTTPTunnelRequest;
import org.apache.guacamole.websocket.WebsocketHandlerAdapter;
import org.apache.guacamole.websocket.WsSession;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author FastoneTeam
 * @date 2022/3/9 下午3:37
 */
public class TomcatWebSocket extends WebSocketServlet {
	String guacdServer;
	/**
	 * A service for authenticating users from auth tokens.
	 */
	private AuthenticationService authenticationService;

	public TomcatWebSocket(String guacdServer, AuthenticationService authenticationService) {
		this.guacdServer = guacdServer;
		this.authenticationService = authenticationService;
	}

	@Override
	protected StreamInbound createWebSocketInbound(String protocol, HttpServletRequest httpServletRequest) {
		return new GuacamoleStreamInbound(httpServletRequest,authenticationService);
	}
	public static class GuacamoleStreamInbound extends StreamInbound  {
		HttpServletRequest request;
		AuthenticationService authenticationService;

		public GuacamoleStreamInbound(HttpServletRequest request, AuthenticationService authenticationService) {
			this.request = request;
			this.authenticationService = authenticationService;
		}

		@Override
		protected void onOpen(WsOutbound outbound) {
			ConnectionHelper connectionHelper = ConnectionHelper.getInstance(new GuacdProperties("localhost",4822));
			WsSession wsSession = new TomcatWsOutbound(outbound, UUID.randomUUID().toString());
			final TunnelRequest tunnelRequest = new HTTPTunnelRequest(request);
			try {
				GuacamoleSession session = authenticationService.getGuacamoleSession(tunnelRequest.getAuthenticationToken());
				UserContext userContext = session.getUserContext(tunnelRequest.getAuthenticationProviderIdentifier());
				Connection connection = (Connection) tunnelRequest.getType().getConnectable(userContext, tunnelRequest.getIdentifier());
				Bridger tunnel = connectionHelper.openConnection(wsSession, connection.getConfiguration());
				TomcatWebsocketHandler.GUACAD_HAND_MAP.put(tunnel.getChannel().id().toString(), tunnel);
				TomcatWebsocketHandler.WS_HAND_MAP.put(wsSession.id(), tunnel);
			} catch (GuacamoleException e) {
				e.printStackTrace();
			}

		}

		@Override
		protected void onClose(int status) {

		}

		@Override
		protected void onBinaryData(InputStream inputStream) throws IOException {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		protected void onTextData(Reader reader) throws IOException {
			char[] buffer = new char[8196];
			int num_read;
			StringBuilder stringBuilder = new StringBuilder();
			while ((num_read = reader.read(buffer)) > 0) {
				stringBuilder.append(Arrays.copyOfRange(buffer, 0, num_read));
			}
			TomcatWebsocketHandler.WS_HAND_MAP.get(getWsOutbound().toString()).onMessage(stringBuilder.toString());
		}

		/**
		 * 		use common,another way to implement multi extends
		 */

		public static abstract class TomcatWebsocketHandler extends WebsocketHandlerAdapter{

		}
	}

	public static class TomcatWsOutbound implements WsSession{
		WsOutbound outbound;
		String id;

		public TomcatWsOutbound(WsOutbound outbound, String id) {
			this.outbound = outbound;
			this.id = id;
		}

		@Override
		public void sendText(String data) throws IOException {
			outbound.writeTextMessage(CharBuffer.wrap(data));
		}

		@Override
		public void close() throws IOException {
			outbound.close(0,null);
		}

		@Override
		public String id() {
			return id ;
		}
	}
}
