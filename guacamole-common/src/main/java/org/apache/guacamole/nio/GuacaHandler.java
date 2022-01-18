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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.protocol.GuacamoleProtocolCapability;
import org.apache.guacamole.protocol.GuacamoleProtocolVersion;
import org.apache.guacamole.websocket.WebsocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author kurt.ding
 * @date 2021/11/24 下午9:19
 */
public class GuacaHandler extends SimpleChannelInboundHandler<GuacamoleInstruction> {
	Logger logger = LoggerFactory.getLogger(GuacaHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, GuacamoleInstruction msg) throws Exception {
		//处理args
		logger.info("Get msg from guacad {}", msg);
		TransferTunnel tunnel = WebsocketHandler.GUACAD_HAND_MAP.get(ctx.channel().id().toString());

		if (msg.getOpcode().equals("args")) {
			GuacamoleProtocolVersion protocolVersion =
					GuacamoleProtocolVersion.VERSION_1_0_0;
			GuacamoleConfiguration config = tunnel.getConfig();
			// Build args list off provided names and config
			List<String> arg_names = msg.getArgs();
			String[] arg_values = new String[arg_names.size()];
			for (int i = 0; i < arg_names.size(); i++) {

				// Retrieve argument name
				String arg_name = arg_names.get(i);

				// Check for valid protocol version as first argument
				if (i == 0) {
					GuacamoleProtocolVersion version = GuacamoleProtocolVersion.parseVersion(arg_name);
					if (version != null) {

						// Use the lowest common version supported
						if (version.atLeast(GuacamoleProtocolVersion.LATEST))
							version = GuacamoleProtocolVersion.LATEST;

						// Respond with the version selected
						arg_values[i] = version.toString();
						protocolVersion = version;
						continue;

					}
				}

				// Get defined value for name
				String value = config.getParameter(arg_name);

				// If value defined, set that value
				if (value != null) {
					arg_values[i] = value;
				}
				// Otherwise, leave value blank
				else {
					arg_values[i] = "";
				}

			}
			GuacamoleClientInformation info = new GuacamoleClientInformation();
			// Send size
			writeGuacadInstruction(
					new GuacamoleInstruction(
							"size",
							Integer.toString(info.getOptimalScreenWidth()),
							Integer.toString(info.getOptimalScreenHeight()),
							Integer.toString(info.getOptimalResolution())
					), ctx
			);

			// Send supported audio formats
			writeGuacadInstruction(
					new GuacamoleInstruction(
							"audio",
							info.getAudioMimetypes().toArray(new String[0])
					), ctx);

			// Send supported video formats
			writeGuacadInstruction(
					new GuacamoleInstruction(
							"video",
							info.getVideoMimetypes().toArray(new String[0])
					), ctx);

			// Send supported image formats
			writeGuacadInstruction(
					new GuacamoleInstruction(
							"image",
							info.getImageMimetypes().toArray(new String[0])
					), ctx);

			// Send client timezone, if supported and available
			if (GuacamoleProtocolCapability.TIMEZONE_HANDSHAKE.isSupported(protocolVersion)) {
				String timezone = info.getTimezone();
				if (timezone != null)
					writeGuacadInstruction(new GuacamoleInstruction("timezone", info.getTimezone()), ctx);
			}
			//发起connect
			writeGuacadInstruction(new GuacamoleInstruction("connect", arg_values), ctx);
			return;
		}
		//处理connect
		if (msg.getOpcode().equals("ready")) {
			List<String> ready_args = msg.getArgs();
			if (ready_args.isEmpty())
				throw new GuacamoleServerException("No connection ID received");
			logger.info("Connect resp {}", msg);
			tunnel.setId(ready_args.get(0));
			return;
		}
		tunnel.receiveGuacadMsg(msg);
	}

	private void writeGuacadInstruction(GuacamoleInstruction instruction, ChannelHandlerContext ctx) {
		ctx.channel().writeAndFlush(instruction);
	}
}
