package com.auction.app.web.stream;

import com.auction.domain.auction.AuctionClosed;
import com.auction.domain.auction.AuctionEventSubscriber;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.BidPlaced;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Keeps the open SSE streams of this instance per auction and pushes the events coming back from Kafka to them.
 */
@Slf4j
@Component
public class AuctionStreamBroadcaster implements AuctionEventSubscriber {

    /** Clients reconnect after this; the frontend does it automatically. */
    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<AuctionId, Set<SseEmitter>> streams = new ConcurrentHashMap<>();

    SseEmitter subscribe(AuctionId auctionId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        streams.computeIfAbsent(auctionId, id -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable remove = () -> remove(auctionId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        send(auctionId, emitter, () -> SseEmitter.event().name("connected")
                .data(new StreamMessages.Connected(auctionId.value()), MediaType.APPLICATION_JSON));
        return emitter;
    }

    @Override
    public void onBidPlaced(BidPlaced event) {
        broadcast(event.auctionId(), () -> SseEmitter.event().id(event.bidId().value().toString()).name("bid")
                .data(StreamMessages.BidMessage.from(event), MediaType.APPLICATION_JSON));
    }

    /** Sends the closing, then ends the streams of that auction: nothing more will happen on it. */
    @Override
    public void onAuctionClosed(AuctionClosed event) {
        broadcast(event.auctionId(), () -> SseEmitter.event().name("closed")
                .data(StreamMessages.ClosedMessage.from(event), MediaType.APPLICATION_JSON));
        Set<SseEmitter> closed = streams.remove(event.auctionId());
        if (closed != null) {
            closed.forEach(SseEmitter::complete);
        }
    }

    /** SSE comment line, so proxies do not drop idle streams (nginx closes after 60 s of silence). */
    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        streams.forEach((auctionId, emitters) ->
                emitters.forEach(emitter -> send(auctionId, emitter, () -> SseEmitter.event().comment("heartbeat"))));
    }

    /** Open streams of an auction on this instance (monitoring, tests). */
    public int subscriberCount(AuctionId auctionId) {
        return streams.getOrDefault(auctionId, Set.of()).size();
    }

    private void broadcast(AuctionId auctionId, Supplier<SseEmitter.SseEventBuilder> event) {
        streams.getOrDefault(auctionId, Set.of()).forEach(emitter -> send(auctionId, emitter, event));
    }

    private void send(AuctionId auctionId, SseEmitter emitter, Supplier<SseEmitter.SseEventBuilder> event) {
        try {
            emitter.send(event.get());
        } catch (IOException | IllegalStateException e) {
            // the browser went away: forget this stream
            log.debug("Dropping stream of auction {}: {}", auctionId.value(), e.getMessage());
            remove(auctionId, emitter);
        }
    }

    private void remove(AuctionId auctionId, SseEmitter emitter) {
        streams.computeIfPresent(auctionId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
