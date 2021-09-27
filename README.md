# HIRO Graph API Client (Java version)

This is a client library to handle the API of the [HIRO Graph](#graph-client-hirograph).

This library also contains classes for handling the [WebSockets](#websockets) `event-ws` and `action-ws` API.

__Status__

* Technical preview

For more information about HIRO Automation, look at https://www.arago.co/

For more information about the APIs this library covers, see https://developer.hiro.arago.co/7.0/api/ (Currently
implemented are `app`, `auth`, `graph`, `event-ws` and `action-ws` )

## Prerequisites and dependencies

You need at least Java 11.

https://github.com/arago/java-project and the respective packages under https://github.com/orgs/arago/packages or
the Maven Central Library.

## Quickstart

To use this library, you will need an account at https://id.arago.co/ and access to an OAuth Client-Id and Client-Secret
to access the HIRO Graph. See also https://developer.hiro.arago.co.

Most of the documentation is done in the sourcecode.

### Straightforward graph example

```Java
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.rest.GraphAPI;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;

import java.io.IOException;

class Example {
    public static void main(String[] args) throws HiroException, IOException, InterruptedException {

        // Build an API handler which takes care of API paths via /api/versions and security tokens.
        try (PasswordAuthTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(API_URL)
                .setCredentials(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET)
                .build()
        ) {

            // Use the actual API you want with the handler
            GraphAPI graphAPI = GraphAPI.newBuilder(handler)
                    .build();

            // Execute a command from GraphAPI
            HiroVertexListMessage queryResult = graphAPI
                    .queryVerticesCommand("ogit\\/_type:\"ogit/MARS/Machine\"")
                    .setLimit(4)
                    .execute();

            System.out.println(queryResult.toPrettyJsonString());
        }
    }
}
```

## TokenApiHandler

Authorization against the HIRO Graph is done via tokens. These tokens are handled by classes of
type `AbstractTokenAPIHandler` in this library. Each of the Hiro-Client-Objects (`GraphAPI`, `AuthAPI`, etc.) need to
have some kind of TokenApiHandler in their builder for construction.

This library supplies the following TokenApiHandlers:

---

### FixedTokenAPIHandler

A simple TokenApiHandler that is generated with a preset-token at construction. Cannot update its token.

---

### EnvironmentTokenAPIHandler

A TokenApiHandler that reads an environment variable (default is `HIRO_TOKEN`) from the runtime environment. Will only
update its token when the environment variable changes externally.

---

### PasswordAuthTokenAPIHandler

This TokenApiHandler logs into the HiroAuth backend and obtains a token from login credentials. This is also the only
TokenApiHandler (so far) that automatically tries to renew a token from the backend when it has expired.

---

All code examples in this documentation can use these TokenApiHandlers interchangeably, depending on how such a token is
provided.

The GraphAPI example from above with another customized TokenApiHandler:

```java
import co.arago.hiro.client.connection.token.EnvironmentTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.rest.GraphAPI;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;

import java.io.IOException;

class Example {
    public static void main(String[] args) throws HiroException, IOException, InterruptedException {

        // Build an API handler which takes care of API paths via /api/versions and security tokens.
        try (EnvironmentTokenAPIHandler handler = EnvironmentTokenAPIHandler.newBuilder()
                .setApiUrl(API_URL)
                .build()
        ) {

            // Use the actual API you want with the handler
            GraphAPI graphAPI = GraphAPI.newBuilder(handler)
                    .build();

            // Execute a command from GraphAPI
            HiroVertexListMessage queryResult = graphAPI
                    .queryVerticesCommand("ogit\\/_type:\"ogit/MARS/Machine\"")
                    .setLimit(4)
                    .execute();

            System.out.println(queryResult.toPrettyJsonString());
        }
    }
}
```

### Specific API paths

This TokenApiHandler is also responsible to determine the most up-to-date paths for the API calls. If you want to have a
specific path for your API, you have to set it up with the Builder of that API Client, like

```java
AuthAPI.newBuilder(handler).setApiPath("/api/auth/6.1").build();
```

### Cleanup

Releasing all resources assigned to a connection is done by closing the `TokenAPIHandler` (unless the `httpClient` is
provided externally, see [External HTTP Client](#external-http-client)). You cannot close the API Clients (
i.e. `GraphAPI`), because the handler carries the information about a connection to HIRO, and it can be shared between
multiple API Clients.

### Handler sharing

When you need to access multiple APIs, it is a good idea to share the TokenApiHandler between them. This avoids
unnecessary API version requests and unnecessary token requests with the PasswordAuthTokenAPIHandler for instance.

```java
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.hiro.client.rest.GraphAPI;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;

import java.io.IOException;

class Example {
    public static void main(String[] args) throws HiroException, IOException, InterruptedException {

        // Build an API handler which takes care of API paths via /api/versions and security tokens.
        try (PasswordAuthTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(API_URL)
                .setCredentials(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET)
                .build()
        ) {

            // Use the actual APIs you want with the handler

            GraphAPI graphAPI = GraphAPI.newBuilder(handler)
                    .build();

            AuthAPI authAPI = AuthAPI.newBuilder(handler)
                    .build();
        }

    }
}
```

### External HTTP Client

An external httpClient can be provided to the TokenAPIHandlers via their Builders using `setClient(HttpClient client)`.
If this is the case, a call to `close()` of such a handler will have no effect. It has to be closed externally.

Example with an external httpClient:

```java
import co.arago.hiro.client.Config;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.rest.GraphAPI;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;
import co.arago.util.json.JsonUtil;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Example {
    public static void main(String[] args) throws HiroException, IOException, InterruptedException {

        // The external httpClient
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newFixedThreadPool(1))
                .build();

        try {
            // Build an API handler which takes care of API paths via /api/versions and security tokens.
            // Use an external httpClient.
            PasswordAuthTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                    .setHttpClient(httpClient)
                    .setApiUrl(API_URL)
                    .setCredentials(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET)
                    .build();

            // Use the actual API you want with the handler
            GraphAPI graphAPI = GraphAPI.newBuilder(handler)
                    .build();

            // Execute a command from GraphAPI
            HiroVertexListMessage queryResult = graphAPI
                    .queryVerticesCommand("ogit\\/_type:\"ogit/MARS/Machine\"")
                    .setLimit(4)
                    .execute();

            System.out.println(queryResult.toPrettyJsonString());
        } finally {
            // Shutdown the connection by shutting down its executorService.
            Executor executor = httpClient.executor().orElse(null);
            if (executor instanceof ExecutorService) {
                ExecutorService executorService = (ExecutorService) executor;

                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                }

            }
        }
    }
}
```

# The API Clients

The API Clients are mostly straightforward to use, since all public methods of these classes represent an API call.
Documentation is available in source code as well.

Currently, supported are

* [AppAPI](https://core.arago.co/help/specs/?url=definitions/app.yaml)
* [AuthAPI](https://core.arago.co/help/specs/?url=definitions/auth.yaml)
* [GraphAPI](https://core.arago.co/help/specs/?url=definitions/graph.yaml)

For usage, see the examples above.

# The WebSocket Clients

There are two websocket clients supported:

* [event-ws](https://core.arago.co/help/specs/?url=definitions/events-ws.yaml)
* [action-ws](https://core.arago.co/help/specs/?url=definitions/action-ws.yaml)

Both of them have special client classes in this library.

When messages from the websockets are received, they are handled in sequential order in their own thread. This can prove
to be a problem when you need to react to those messages swiftly. Isolating each handling of an incoming message in its
own thread is not subject of this library and has to be done by the project using it.

## Event Websocket

* [event-ws](https://core.arago.co/help/specs/?url=definitions/events-ws.yaml)

The event websocket gets triggered when changes to vertices happen. These can be `CREATE`, `UPDATE` or `DELETE`.

Example skeleton code:

```java
import co.arago.hiro.client.Config;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.websocket.events.impl.EventsMessage;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.hiro.client.websocket.EventWebSocket;
import co.arago.hiro.client.websocket.listener.EventWebSocketListener;
import co.arago.util.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Paths;

class Example {
    public static void main(String[] args) throws HiroException, IOException, InterruptedException {

        // Build an API handler which takes care of API paths via /api/versions and security tokens.
        try (PasswordAuthTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(API_URL)
                .setCredentials(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET)
                .build()
        ) {


            // Obtain my default scope. THIS WILL MOST LIKELY CHANGE IN THE FUTURE.
            AuthAPI authAPI = AuthAPI.newBuilder(handler).build();
            String defaultScope = authAPI
                    .getMeProfileCommand()
                    .execute()
                    .getAttributeAsString("ogit/Auth/Account/defaultScope");

            try (EventWebSocket eventWebSocket = EventWebSocket.newBuilder(
                            handler,
                            new EventWebSocketListener() {
                                @Override
                                public void onCreate(EventsMessage eventsMessage) {
                                    // React when a vertex has been created
                                }

                                @Override
                                public void onUpdate(EventsMessage eventsMessage) {
                                    // React when a vertex has been updated
                                }

                                @Override
                                public void onDelete(EventsMessage eventsMessage) {
                                    // React when a vertex has been deleted
                                }
                            })
                    .addScope(defaultScope)
                    .addEventsFilter(
                            "default",
                            "(element.ogit/_type = ogit/Automation/AutomationIssue)"
                    )
                    .build()
            ) {

                eventWebSocket.start();

                // Listen for 1 second for incoming events.
                Thread.sleep(1000);
            }
        }
    }
}
```

I contrast to the other API Client objects the websockets need to be closed explicitly after usage.

## Action Websocket

* [action-ws](https://core.arago.co/help/specs/?url=definitions/action-ws.yaml)

The action websocket receives messages from HIRO KIs. The project then has to handle those messages and send a response
back.

This library handles the protocol overhead like ACK and NACK messages and persistent storage of messages for consistent
communication in the background.

Receiving and sending of action messages and results should be done asynchronously - which is NOT subject of this
library.

Example skeleton code:

```java
import co.arago.hiro.client.Config;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.websocket.action.impl.ActionHandlerSubmit;
import co.arago.hiro.client.websocket.ActionWebSocket;
import co.arago.hiro.client.websocket.listener.ActionWebSocketListener;
import co.arago.util.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Paths;

class Example {
    public static void main(String[] args) throws HiroException, IOException, InterruptedException {


        // Build an API handler which takes care of API paths via /api/versions and security tokens.
        try (PasswordAuthTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(API_URL)
                .setCredentials(USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET)
                .setAcceptAllCerts(config.accept_all_certs)
                .build()
        ) {

            try (ActionWebSocket actionWebSocket = ActionWebSocket.newBuilder(
                            handler,
                            new ActionWebSocketListener() {

                                @Override
                                public void onActionSubmit(
                                        ActionWebSocket actionWebSocket,
                                        ActionHandlerSubmit submit
                                ) throws Exception {

                                    // Handle message

                                    actionWebSocket.sendActionResult(
                                            submit.getId(),
                                            ActionWebSocket.newResultParams()
                                                    .setMessage("All is fine")
                                    );
                                }

                                @Override
                                public void onConfigChanged(
                                        ActionWebSocket actionWebSocket
                                ) {
                                    // Handle reload of configuration if necessary.
                                }
                            })
                    .build()
            ) {

                actionWebSocket.start();

                // Listen for 1 second for incoming actions.
                Thread.sleep(1000);
            }

        }
    }
}
```
