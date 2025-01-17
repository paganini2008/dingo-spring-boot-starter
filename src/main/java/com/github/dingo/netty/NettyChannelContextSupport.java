/**
 * Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.dingo.netty;

import java.net.SocketAddress;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.dingo.ChannelContext;
import com.github.dingo.ChannelEvent;
import com.github.dingo.ChannelEventListener;
import com.github.dingo.ConnectionKeeper;
import com.github.dingo.Packet;
import com.github.dingo.RequestFutureHolder;
import com.github.dingo.ChannelEvent.EventType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 
 * @Description: NettyChannelContextSupport
 * @Author: Fred Feng
 * @Date: 27/12/2024
 * @Version 1.0.0
 */
public abstract class NettyChannelContextSupport extends ChannelInboundHandlerAdapter
        implements ChannelContext<Channel> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private ConnectionKeeper connectionKeeper;
    private ChannelEventListener<Channel> channelEventListener;

    public ConnectionKeeper getConnectionKeeper() {
        return connectionKeeper;
    }

    public void setConnectionKeeper(ConnectionKeeper connectionKeeper) {
        this.connectionKeeper = connectionKeeper;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        addChannel(ctx.channel());

        fireChannelEvent(ctx.channel(), EventType.CONNECTED, null);

        if (log.isInfoEnabled()) {
            log.info("Current active channel's count: " + countOfChannels());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        removeChannel(remoteAddress);

        fireReconnectionIfNecessary(remoteAddress);
        fireChannelEvent(ctx.channel(), EventType.CLOSED, null);

        if (log.isInfoEnabled()) {
            log.info("Current active channel's count: " + countOfChannels());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();

        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        removeChannel(remoteAddress);

        fireReconnectionIfNecessary(remoteAddress);
        fireChannelEvent(ctx.channel(), EventType.ERROR, cause);

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object data) throws Exception {
        if (isPong(data)) {
            fireChannelEvent(ctx.channel(), EventType.PONG, null);
        } else {
            String requestId = ctx.channel().attr(NettyClient.REQUEST_ID).get();
            if (StringUtils.isNotBlank(requestId)) {
                RequestFutureHolder.getRequest(requestId).complete(data);
            }
        }
    }

    private boolean isPong(Object data) {
        return (data instanceof Packet) && ((Packet) data).isPong();
    }

    @Override
    public void setChannelEventListener(ChannelEventListener<Channel> channelEventListener) {
        this.channelEventListener = channelEventListener;
    }

    public ChannelEventListener<Channel> getChannelEventListener() {
        return channelEventListener;
    }

    private void fireChannelEvent(Channel channel, EventType eventType, Throwable cause) {
        if (channelEventListener != null) {
            channelEventListener
                    .fireChannelEvent(new ChannelEvent<Channel>(channel, eventType, cause));
        }
    }

    private void fireReconnectionIfNecessary(SocketAddress remoteAddress) {
        if (connectionKeeper != null) {
            connectionKeeper.reconnect(remoteAddress);
        }
    }

}
