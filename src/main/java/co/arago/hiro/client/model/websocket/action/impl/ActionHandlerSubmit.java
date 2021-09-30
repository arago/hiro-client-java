package co.arago.hiro.client.model.websocket.action.impl;

import co.arago.hiro.client.model.websocket.action.AbstractActionHandlerIdMessage;
import co.arago.hiro.client.model.websocket.action.ActionMessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * <pre>
 *     {
 *       "type": "submitAction",
 *       "id": "id1",
 *       "handler": "handlerId",
 *       "capability": "ExecuteCommand",
 *       "parameters": {},
 *       "timeout": 30000
 *     }
 * </pre>
 */
public class ActionHandlerSubmit extends AbstractActionHandlerIdMessage {

    private static final long serialVersionUID = 9174246058907777607L;

    public String handler;
    public String capability;
    public Map<String, Object> parameters;
    /**
     * Timeout in ms. Default is 5 min.
     */
    private Long timeout = 300000L;

    private final Instant timestamp;
    private Instant expiresAt;

    @JsonCreator
    public ActionHandlerSubmit(
            @JsonProperty(value = "id", required = true) String id
    ) {
        super(id);
        this.timestamp = Instant.now();
        this.expiresAt = timestamp.plus(timeout, ChronoUnit.MILLIS);
    }

    @Override
    public ActionMessageType getType() {
        return ActionMessageType.SUBMIT_ACTION;
    }

    public String getHandler() {
        return handler;
    }

    public String getCapability() {
        return capability;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * @return timeout in ms.
     */
    public Long getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout and {@link #expiresAt}. 'timeout' must be greater than 0 and not null
     * or {@link #expiresAt} will remain unchanged.
     *
     * @param timeout Timeout in ms
     */
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
        if (timeout != null && timeout > 0) {
            this.expiresAt = timestamp.plus(timeout, ChronoUnit.MILLIS);
        }
    }

    /**
     * Instant of creation of this object plus timeout ms.
     *
     * @return The instant of expiration.
     */
    public Instant expiresAt() {
        return expiresAt;
    }
}
