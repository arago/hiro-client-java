package co.arago.hiro.client.websocket;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.websocket.events.EventsFilter;
import co.arago.hiro.client.model.websocket.events.impl.*;
import co.arago.hiro.client.websocket.listener.EventWebSocketListener;
import co.arago.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * The handler for Event WebSocket.
 *
 * @see <a href="https://core.arago.co/help/specs/?url=definitions/events-ws.yaml#/">API Documentation</a>
 */
public class EventWebSocket extends AuthenticatedWebSocketHandler {
    final static Logger log = LoggerFactory.getLogger(EventWebSocket.class);

    public final static String API_NAME = "events-ws";

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AuthenticatedWebSocketHandler.Conf<T> {

        private Set<String> scopes = new HashSet<>();
        private Map<String, EventsFilter> eventsFilterMap = new LinkedHashMap<>();
        private EventWebSocketListener eventWebSocketListener;

        public Conf() {
            setName(API_NAME);
            setApiName(API_NAME);
            setQuery("allscopes", "false");
        }

        /**
         * Set query parameter "delta"
         *
         * @param delta Subscribe to delta events stream
         * @return {@link #self()}
         * @see <a href="https://core.arago.co/help/specs/?url=definitions/events-ws.yaml#/[Connect]/get__connect_">API Documentation</a>
         */
        public T setDelta(boolean delta) {
            setQuery("delta", String.valueOf(delta));
            return self();
        }

        /**
         * Set query parameter "groupId"
         *
         * @param groupId Subscribe to specific group of events listeners
         * @return {@link #self()}
         * @see <a href="https://core.arago.co/help/specs/?url=definitions/events-ws.yaml#/[Connect]/get__connect_">API Documentation</a>
         */
        public T setGroupId(String groupId) {
            setQuery("groupId", groupId);
            return self();
        }

        /**
         * Set query parameter "offset"
         *
         * @param offset Offset for events
         * @return {@link #self()}
         * @see <a href="https://core.arago.co/help/specs/?url=definitions/events-ws.yaml#/[Connect]/get__connect_">API Documentation</a>
         */
        public T setOffset(String offset) {
            setQuery("offset", offset);
            return self();
        }

        /**
         * Set query parameter "allscopes"
         *
         * @param allScopes Subscribe to all data scopes by default. Default is "false".
         * @return {@link #self()}
         * @see <a href="https://core.arago.co/help/specs/?url=definitions/events-ws.yaml#/[Connect]/get__connect_">API Documentation</a>
         */
        public T setAllScopes(boolean allScopes) {
            setQuery("allscopes", String.valueOf(allScopes));
            return self();
        }

        public Set<String> getScopes() {
            return scopes;
        }

        /**
         * Set a list of scopes for the events.
         *
         * @param scopes List of scopes (ogit - ids of the scopes).
         * @return {@link #self()}
         */
        public T setScopes(Set<String> scopes) {
            this.scopes = scopes;
            return self();
        }

        /**
         * Add a single scope to {@link #scopes}.
         *
         * @param scope The scope to add
         * @return {@link #self()}
         */
        public T addScope(String scope) {
            this.scopes.add(scope);
            return self();
        }

        public Map<String, EventsFilter> getEventsFilterMap() {
            return eventsFilterMap;
        }

        /**
         * Set the filters
         *
         * @param eventsFilterMap A map of filters to use. The keys are the
         *                        {@link EventsFilter#id}.
         * @return {@link #self()}
         */
        public T setEventsFilterMap(Map<String, EventsFilter> eventsFilterMap) {
            this.eventsFilterMap = eventsFilterMap;
            return self();
        }

        /**
         * Add a single filter to the map of event filters.
         *
         * @param id      ID of the filter
         * @param content Content of the filter
         * @return {@link #self()}
         */
        public T addEventsFilter(String id, String content) {
            this.eventsFilterMap.put(id, new EventsFilter(id, content));
            return self();
        }

        /**
         * Add a single filter to the map of event filters.
         *
         * @param id      ID of the filter
         * @param content Content of the filter
         * @param type    Type of the filter (usually only "jfilter")
         * @return {@link #self()}
         */
        public T addEventsFilter(String id, String content, String type) {
            this.eventsFilterMap.put(id, new EventsFilter(id, content, type));
            return self();
        }

        public EventWebSocketListener getEventWebSocketListener() {
            return eventWebSocketListener;
        }

        /**
         * Set eventWebSocketListener. This will receive incoming data.
         *
         * @param eventWebSocketListener Reference to the listener to use.
         * @return {@link #self()}
         */
        public T setEventWebSocketListener(EventWebSocketListener eventWebSocketListener) {
            this.eventWebSocketListener = eventWebSocketListener;
            return self();
        }
    }

    public static final class Builder extends Conf<Builder> {

        private Builder(AbstractTokenAPIHandler tokenAPIHandler, EventWebSocketListener webSocketListener) {
            setTokenApiHandler(tokenAPIHandler);
            setEventWebSocketListener(webSocketListener);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public EventWebSocket build() {
            return new EventWebSocket(this);
        }
    }

    // ###############################################################################################
    // ## InternalEventListener ##
    // ###############################################################################################

    /**
     * Listener class for Event WebSockets.
     */
    protected class InternalEventListener implements SpecificWebSocketListener {

        /**
         * Reference to the external listener
         */
        protected final EventWebSocketListener eventWebSocketListener;

        /**
         * Constructor
         *
         * @param listener The listener which received messages.
         */
        public InternalEventListener(EventWebSocketListener listener) {
            eventWebSocketListener = listener;
        }

        /**
         * Sets the filters, scopes and calls {@link TokenRefreshHandler#start()}.
         *
         * @param webSocket The webSocket using this HiroWebSocketListener.
         * @throws InterruptedException On interrupt on sending.
         * @throws IOException          On IO errors or when messages cannot be parsed to JSON strings.
         * @throws ExecutionException   When sending fails generally.
         * @throws TimeoutException     When sending of data took longer than {@link #webSocketRequestTimeout} ms.
         */
        @Override
        public void onOpen(WebSocket webSocket) throws IOException, ExecutionException, InterruptedException, TimeoutException {
            List<CompletableFuture<WebSocket>> futures = new ArrayList<>();

            for (String scopeId : scopes) {
                futures.add(webSocket.sendText(new SubscribeScopeMessage(scopeId).toJsonString(), true));
            }

            for (Map.Entry<String, EventsFilter> filterEntry : eventsFilterMap.entrySet()) {
                futures.add(webSocket.sendText(new EventRegisterMessage(filterEntry.getValue()).toJsonString(), true));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(webSocketRequestTimeout, TimeUnit.MILLISECONDS);

            eventWebSocketListener.onOpen();

            tokenRefreshHandler.start();
        }

        @Override
        public void onMessage(WebSocket webSocket, HiroMessage message) throws Exception {
            eventWebSocketListener.onEvent(JsonUtil.DEFAULT.toObject(message, EventsMessage.class));
        }

        /**
         * Receives close messages.
         * <p>
         * This will stop the {@link #tokenRefreshHandler}.
         *
         * @param webSocket  Reference to the websocket.
         * @param statusCode Status code sent from the remote side.
         * @param reason     Reason text sent from the remote side.
         */
        @Override
        public void onClose(WebSocket webSocket, int statusCode, String reason) {
            tokenRefreshHandler.stop();
            eventWebSocketListener.onClose(statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable t) {
            eventWebSocketListener.onError(t);
        }
    }

    // ###############################################################################################
    // ## Token refresh thread ##
    // ###############################################################################################

    private class TokenRefreshHandler {

        private class TokenRefreshThread implements Runnable {

            @Override
            public void run() {
                try {
                    send(new TokenRefreshMessage(tokenAPIHandler.getToken()).toJsonString());

                    Long msTillNextStart = msTillNextStart();
                    if (msTillNextStart != null) {
                        tokenRefreshExecutor.schedule(this, msTillNextStart, TimeUnit.MILLISECONDS);
                    }
                } catch (IOException | InterruptedException | HiroException e) {
                    log.error("Cannot refresh token.", e);
                }
            }

        }

        private ScheduledExecutorService tokenRefreshExecutor;

        public synchronized void start() {
            if (tokenRefreshExecutor != null)
                stop();

            Long msTillNextStart = msTillNextStart();
            if (msTillNextStart != null) {
                tokenRefreshExecutor = Executors.newSingleThreadScheduledExecutor();
                tokenRefreshExecutor.schedule(new TokenRefreshThread(), msTillNextStart, TimeUnit.MILLISECONDS);
            }
        }

        public synchronized void stop() {
            if (tokenRefreshExecutor == null)
                return;

            tokenRefreshExecutor.shutdown();
            try {
                if (!tokenRefreshExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    tokenRefreshExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                tokenRefreshExecutor.shutdownNow();
            }

            tokenRefreshExecutor = null;
        }

        private Long msTillNextStart() {
            Instant nextStart = tokenAPIHandler.expiryInstant();
            if (nextStart == null)
                return null;

            return nextStart.toEpochMilli() - Instant.now().toEpochMilli();
        }
    }

    // #############################################################################################
    // ## Main part ##
    // ###############################################################################################

    private final TokenRefreshHandler tokenRefreshHandler = new TokenRefreshHandler();

    private final Set<String> scopes;
    private final Map<String, EventsFilter> eventsFilterMap;

    /**
     * Protected Constructor. Use {@link #newBuilder(AbstractTokenAPIHandler, EventWebSocketListener)}.
     *
     * @param builder The {@link Builder} to use.
     */
    protected EventWebSocket(Conf<?> builder) {
        super(builder);
        this.scopes = builder.getScopes();
        this.eventsFilterMap = builder.getEventsFilterMap();

        this.internalListener = new InternalListener(
                name + "-listener",
                new InternalEventListener(
                        notNull(builder.getEventWebSocketListener(), "eventWebSocketListener")
                )
        );
    }

    /**
     * Get a {@link Builder} for {@link EventWebSocket}.
     *
     * @param tokenAPIHandler        The API handler for this websocket.
     * @param eventWebSocketListener The listener for this websocket.
     * @return The {@link Builder} for {@link EventWebSocket}.
     */
    public static Builder newBuilder(
            AbstractTokenAPIHandler tokenAPIHandler,
            EventWebSocketListener eventWebSocketListener
    ) {
        return new EventWebSocket.Builder(tokenAPIHandler, eventWebSocketListener);
    }

    @Override
    public void close() {
        super.close();
        tokenRefreshHandler.stop();
    }

    // ###############################################################################################
    // ## Event Filter Handling ##
    // ###############################################################################################

    public void addEventsFilter(EventsFilter filter) throws HiroException, IOException, InterruptedException {
        send(new EventRegisterMessage(filter).toJsonString());
        synchronized (this) {
            eventsFilterMap.put(filter.id, filter);
        }
    }

    public void removeEventsFilter(String filterId) throws HiroException, IOException, InterruptedException {
        send(new EventUnregisterMessage(filterId).toJsonString());
        synchronized (this) {
            eventsFilterMap.remove(filterId);
        }
    }

    public void clearEventsFilter() throws HiroException, IOException, InterruptedException {
        send(new ClearEventsMessage().toJsonString());
        synchronized (this) {
            eventsFilterMap.clear();
        }
    }

    // ###############################################################################################
    // ## Scope Handling ##
    // ###############################################################################################

    public void subscribeScope(String scopeId) throws HiroException, IOException, InterruptedException {
        send(new SubscribeScopeMessage(scopeId).toJsonString());
        synchronized (this) {
            scopes.add(scopeId);
        }
    }

}
