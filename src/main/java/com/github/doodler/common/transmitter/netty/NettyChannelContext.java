package com.github.doodler.common.transmitter.netty;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.github.doodler.common.transmitter.ChannelContext;
import com.github.doodler.common.transmitter.Partitioner;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class NettyChannelContext extends NettyChannelContextSupport
        implements ChannelContext<Channel> {

    private final List<Channel> channelHolds = new CopyOnWriteArrayList<Channel>();

    @Override
    public void addChannel(Channel channel, int weight) {
        for (int i = 0; i < weight; i++) {
            channelHolds.add(channel);
        }
        if (log.isTraceEnabled()) {
            log.trace("Current channel's count: " + countOfChannels());
        }
    }

    @Override
    public Channel getChannel(SocketAddress address) {
        for (Channel channel : channelHolds) {
            if (channel.remoteAddress() != null && channel.remoteAddress().equals(address)) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public void removeChannel(SocketAddress address) {
        for (Channel channel : channelHolds) {
            if (channel.remoteAddress() != null && channel.remoteAddress().equals(address)) {
                channelHolds.remove(channel);
            }
        }
    }

    @Override
    public int countOfChannels() {
        return channelHolds.size();
    }

    @Override
    public Channel selectChannel(Object data, Partitioner partitioner) {
        return channelHolds.isEmpty() ? null : partitioner.selectChannel(data, channelHolds);
    }

    @Override
    public Collection<Channel> getChannels() {
        return channelHolds;
    }

}