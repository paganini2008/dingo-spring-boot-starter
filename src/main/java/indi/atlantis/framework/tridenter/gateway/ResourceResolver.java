package indi.atlantis.framework.tridenter.gateway;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 
 * ResourceResolver
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface ResourceResolver {

	void resolve(FullHttpRequest httpRequest, Router router, String path, ChannelHandlerContext ctx) throws Exception;

}
