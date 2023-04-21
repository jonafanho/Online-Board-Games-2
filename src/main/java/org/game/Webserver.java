package org.game;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOChannelInitializer;
import com.corundumstudio.socketio.SocketIOServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.function.BiConsumer;

public class Webserver {

	public final SocketIOServer server;

	public Webserver(int port) {
		final Configuration configuration = new Configuration();
		configuration.setPort(port);
		configuration.getSocketConfig().setReuseAddress(true);
		configuration.setAllowCustomRequests(true);

		server = new SocketIOServer(configuration);
		server.setPipelineFactory(new CustomSocketIOChannelInitializer());
	}

	private static class CustomSocketIOChannelInitializer extends SocketIOChannelInitializer {

		@Override
		protected void addSocketioHandlers(ChannelPipeline pipeline) {
			super.addSocketioHandlers(pipeline);
			pipeline.addBefore(WRONG_URL_HANDLER, "custom", new CustomChannelInboundHandlerAdapter());
		}
	}

	private static class CustomChannelInboundHandlerAdapter extends SimpleChannelInboundHandler<FullHttpRequest> {

		@Override
		protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) {
			getBuffer(fullHttpRequest.uri().split("\\?")[0], (byteBuf, mimeType) -> {
				final HttpResponse httpResponse;
				if (byteBuf == null) {
					httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
				} else {
					httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
					httpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, mimeType);
				}
				channelHandlerContext.channel().writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
			}, true);
		}

		private static void getBuffer(String fileName, BiConsumer<ByteBuf, String> consumer, boolean shouldRetry) {
			final URL url = Main.class.getResource("/assets/website" + fileName);

			if (url != null) {
				try {
					final File file = new File(url.toURI());
					try (final FileInputStream fileInputStream = new FileInputStream(file)) {
						try (final FileChannel fileChannel = fileInputStream.getChannel()) {
							consumer.accept(Unpooled.wrappedBuffer(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())), getMimeType(fileName));
							return;
						}
					}
				} catch (Exception ignored) {
				}
			}

			if (shouldRetry) {
				getBuffer("/index.html", consumer, false);
			} else {
				consumer.accept(null, "");
			}
		}

		private static String getMimeType(String fileName) {
			final String[] fileNameSplit = fileName.split("\\.");
			final String fileExtension = fileNameSplit.length == 0 ? "" : fileNameSplit[fileNameSplit.length - 1];
			switch (fileExtension) {
				case "js":
					return "text/javascript";
				case "json":
					return "application/json";
				default:
					return "text/" + fileExtension;
			}
		}
	}
}
