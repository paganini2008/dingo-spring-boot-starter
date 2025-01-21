package com.github.dingo.grizzly;

import static com.github.dingo.TransmitterConstants.MODE_SYNC;
import java.io.IOException;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.springframework.beans.factory.annotation.Autowired;
import com.github.dingo.ChannelEvent;
import com.github.dingo.ChannelEventListener;
import com.github.dingo.Packet;
import com.github.dingo.PacketHandlerExecution;
import com.github.dingo.PerformanceInspector;
import com.github.dingo.TransmitterConstants;
import com.github.dingo.ChannelEvent.EventType;
import com.github.doodler.common.events.EventPublisher;
import com.github.doodler.common.utils.ExceptionUtils;
import com.github.doodler.common.utils.IdUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @Description: GrizzlyServerHandler
 * @Author: Fred Feng
 * @Date: 08/01/2025
 * @Version 1.0.0
 */
@Slf4j
public class GrizzlyServerHandler extends BaseFilter {

    @Autowired
    private EventPublisher<Packet> eventPublisher;

    @Autowired
    private PerformanceInspector performanceInspector;

    @Autowired(required = false)
    private ChannelEventListener<Connection<?>> channelEventListener;

    @Autowired
    private PacketHandlerExecution packetHandlerExecution;

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        Packet packet = ctx.getMessage();
        if (isPing(packet)) {
            if (channelEventListener != null) {
                channelEventListener.fireChannelEvent(new ChannelEvent<Connection<?>>(
                        ctx.getConnection(), EventType.PING, true, null));
            }
            ctx.write(Packet.PONG);
            return ctx.getStopAction();
        } else {
            if (TransmitterConstants.MODE_SYNC.equalsIgnoreCase(packet.getMode())) {
                Packet result = packet.copy();
                long timestamp = result.getTimestamp();
                String instanceId = result.getStringField("instanceId");
                try {
                    Object returnData = packetHandlerExecution.executeHandlerChain(packet);
                    if (returnData != null) {
                        if (returnData instanceof Packet) {
                            result = (Packet) returnData;
                        } else {
                            result.setObject(returnData);
                        }
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(e.getMessage(), e);
                    }
                    result.setField("errorMsg", e.getMessage());
                    result.setField("errorDetails", ExceptionUtils.toString(e));
                } finally {
                    performanceInspector.update(instanceId, MODE_SYNC, timestamp, s -> {
                        s.getSample().accumulatedExecutionTime
                                .add(System.currentTimeMillis() - timestamp);
                        s.getSample().totalExecutions.increment();
                    });
                }
                result.setField("server", ctx.getConnection().getLocalAddress().toString());
                result.setField("salt", IdUtils.getShortUuid());
                ctx.write(result);
            } else {
                eventPublisher.publish(packet);
            }
            return ctx.getInvokeAction();
        }
    }

    private boolean isPing(Object data) {
        return (data instanceof Packet) && ((Packet) data).isPing();
    }

    @Override
    public NextAction handleAccept(FilterChainContext ctx) throws IOException {
        fireChannelEvent(ctx.getConnection(), EventType.CONNECTED, null);
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        fireChannelEvent(ctx.getConnection(), EventType.CLOSED, null);
        return ctx.getInvokeAction();
    }

    @Override
    public void exceptionOccurred(FilterChainContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
        fireChannelEvent(ctx.getConnection(), EventType.ERROR, cause);
    }

    private void fireChannelEvent(Connection<?> channel, EventType eventType, Throwable cause) {
        if (channelEventListener != null) {
            channelEventListener.fireChannelEvent(
                    new ChannelEvent<Connection<?>>(channel, eventType, true, cause));
        }
    }

}