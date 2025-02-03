/*
 * Copyright 2017-2025 Fred Feng (paganini.fy@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dingo.netty;

import io.netty.channel.ChannelHandler.Sharable;
import com.github.dingo.KeepAliveTimeoutException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 
 * @Description: KeepAlivePolicy
 * @Author: Fred Feng
 * @Date: 28/12/2024
 * @Version 1.0.0
 */
@Sharable
public abstract class KeepAlivePolicy extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case READER_IDLE:
                    whenReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    whenWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    whenBothIdle(ctx);
                    break;
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


    protected void whenReaderIdle(ChannelHandlerContext ctx) {
        throw new KeepAliveTimeoutException("Reading Idle.");
    }

    protected void whenWriterIdle(ChannelHandlerContext ctx) {
        throw new KeepAliveTimeoutException("Writing Idle.");
    }

    protected void whenBothIdle(ChannelHandlerContext ctx) {
        throw new KeepAliveTimeoutException("Reading or Writing Idle.");
    }

}
