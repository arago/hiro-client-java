package co.arago.hiro.client.websocket;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.websocket.events.*;
import co.arago.hiro.client.util.RequiredFieldChecker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The handler for Event WebSocket.
 *
 * @see <a href="https://core.arago.co/help/specs/?url=definitions/events-ws.yaml#/">API Documentation</a>
 */
public class EventWebSocket extends AbstractWebSocketHandler {
    final Logger log = LoggerFactory.getLogger(EventWebSocket.class);

    public static abstract class Conf<T extends Conf<T>> extends AbstractWebSocketHandler.Conf<T> {

        private Set<String> scopes = new HashSet<>();

        private Map<String, EventsFilter> eventsFilterMap = new LinkedHashMap<>();

        public Set<String> getScopes() {
            return scopes;
        }

        /**
         * Set a list of scopes for the events.
         *
         * @param scopes List of scopes (ogit - ids of the scopes).
         * @return this
         */
        public T setScopes(Set<String> scopes) {
            this.scopes = scopes;
            return self();
        }

        /**
         * Add a single scope to {@link #scopes}.
         *
         * @param scope The scope to add
         * @return this
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
         * @return this
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
         * @return this
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
         * @return this
         */
        public T addEventsFilter(String id, String content, String type) {
            this.eventsFilterMap.put(id, new EventsFilter(id, content, type));
            return self();
        }
    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public EventWebSocket build() {
            RequiredFieldChecker.notNull(getTokenApiHandler(), "tokenApiHandler");
            RequiredFieldChecker.notNull(getWebSocketListener(), "webSocketListener");
            if (StringUtils.isBlank(getApiName()) && (StringUtils.isAnyBlank(getEndpoint(), getProtocol())))
                RequiredFieldChecker.anyError("Either 'apiName' or 'endpoint' and 'protocol' have to be set.");
            return new EventWebSocket(this);
        }
    }

    protected class EventWebSocketListener extends AbstractWebSocketHandler.WebSocketListener {

        /**
         * Constructor
         *
         * @param name     Name of the handler (mainly for logging)
         * @param listener The listener which received messages.
         */
        public EventWebSocketListener(String name, Listener listener) {
            super(name, listener);
        }

        /**
         * Sets the status from {@link Status#STARTING} or {@link Status#RESTARTING} to
         * {@link Status#RUNNING_PRELIMINARY}. Any other state will result in an Exception.
         * Sets the filters, scopes and calls {@link TokenRefreshHandler#start()}. Sets {@link Status#FAILED}
         * when an exception occurs so the websocket will be taken down.
         *
         * @param webSocket The webSocket using this Listener.
         * @throws IllegalStateException When the status is neither {@link Status#STARTING} nor
         *                               {@link Status#RESTARTING}.
         */
        @Override
        public void onOpen(WebSocket webSocket) {
            super.onOpen(webSocket);

            try {
                for (String scopeId : scopes) {
                    subscribeScope(scopeId);
                }

                for (Map.Entry<String, EventsFilter> filterEntry : eventsFilterMap.entrySet()) {
                    addEventsFilter(filterEntry.getValue());
                }

                tokenRefreshHandler.start();

            } catch (HiroException | IOException | InterruptedException e) {
                setStatus(Status.FAILED);
                onError(webSocket, e);
            }

        }

        /**
         * Receives close messages. This will try to restart the WebSocket unless
         * the status is {@link Status#CLOSING}.
         * Calls {@link TokenRefreshHandler#stop()}.
         *
         * @param webSocket  The webSocket using this Listener.
         * @param statusCode Status code sent from the remote side.
         * @param reason     Reason text sent from the remote side.
         * @return null
         */
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            try {
                return super.onClose(webSocket, statusCode, reason);
            } finally {
                tokenRefreshHandler.stop();
            }
        }
    }

    private final TokenRefreshHandler tokenRefreshHandler = new TokenRefreshHandler();

    private final Set<String> scopes;
    private final Map<String, EventsFilter> eventsFilterMap;

    protected EventWebSocket(Conf<?> builder) {
        super(builder);
        this.scopes = builder.getScopes();
        this.eventsFilterMap = builder.getEventsFilterMap();

        if (! query.containsKey("allscopes"))
            query.put("allscopes", "false");
    }

    // ###############################################################################################
    // ## Token refresh thread ##
    // ###############################################################################################

    private class TokenRefreshHandler {

        private class TokenRefreshThread implements Runnable {

            @Override
            public void run() {
                try {
                    send(new WebSocketTokenRefreshMessage(tokenAPIHandler.getToken()).toJsonString());

                    tokenRefreshExecutor.schedule(this, msTillNextStart(), TimeUnit.MILLISECONDS);
                } catch (IOException | InterruptedException | HiroException e) {
                    log.error("Cannot refresh token.", e);
                }
            }

        }

        private ScheduledExecutorService tokenRefreshExecutor;

        public synchronized void start() {
            if (tokenRefreshExecutor != null)
                stop();

            tokenRefreshExecutor = Executors.newSingleThreadScheduledExecutor();
            tokenRefreshExecutor.schedule(new TokenRefreshThread(), msTillNextStart(), TimeUnit.MILLISECONDS);
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

        private long msTillNextStart() {
            Instant now = Instant.now();
            Instant nextStart = tokenAPIHandler.expiryInstant();

            return nextStart.toEpochMilli() - now.toEpochMilli();
        }
    }

    // ###############################################################################################
    // ## Event Filter Handling ##
    // ###############################################################################################

    public void addEventsFilter(EventsFilter filter) throws HiroException, IOException, InterruptedException {
        send(new WebSocketEventRegisterMessage(filter).toJsonString());
        synchronized (this) {
            eventsFilterMap.put(filter.id, filter);
        }
    }

    public void removeEventsFilter(String filterId) throws HiroException, IOException, InterruptedException {
        send(new WebSocketEventUnregisterMessage(filterId).toJsonString());
        synchronized (this) {
            eventsFilterMap.remove(filterId);
        }
    }

    public void clearEventsFilter() throws HiroException, IOException, InterruptedException {
        send(new WebSocketClearEventsMessage().toJsonString());
        synchronized (this) {
            eventsFilterMap.clear();
        }
    }

    // ###############################################################################################
    // ## Scope Handling ##
    // ###############################################################################################

    public void subscribeScope(String scopeId) throws HiroException, IOException, InterruptedException {
        send(new WebSocketSubscribeScopeMessage(scopeId).toJsonString());
        synchronized (this) {
            scopes.add(scopeId);
        }
    }

}
