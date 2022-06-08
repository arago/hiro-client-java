package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.RetryException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;
import co.arago.hiro.client.util.httpclient.StreamContainer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public interface APIHandler {

    interface GetterConf {
        URL getApiUrl();

        URI getWebSocketUri();

        Long getHttpRequestTimeout();

        int getMaxRetries();

        String getUserAgent();
    }

    URL getApiUrl();

    /**
     * Get the webSocketUri or, if it is missing, construct one from apiUrl.
     *
     * @return The URI for the webSocket.
     */
    URI getWebSocketUri();

    String getUserAgent();

    Long getHttpRequestTimeout();

    int getMaxRetries();

    /**
     * Build a complete uri from the apiUrl and path. Appends a '/'.
     *
     * @param path The path to append to apiUrl.
     * @return The constructed URI
     */
    URI buildApiURI(String path);

    /**
     * Build a complete uri from the apiUrl and path. Does not append a '/'
     *
     * @param path The path to append to apiUrl.
     * @return The constructed URI
     */
    URI buildEndpointURI(String path);

    /**
     * Build a complete uri from the webSocketApi and path.
     *
     * @param path The path to append to webSocketUri.
     * @return The constructed URI
     */
    URI buildWebSocketURI(String path);

    /**
     * Create a HttpRequest.Builder with common options and headers.
     *
     * @param uri                The uri for the httpRequest.
     * @param headers            Initial headers for the httpRequest. Must NOT be null.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @return The HttpRequest.Builder
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    HttpRequest.Builder getRequestBuilder(
            URI uri,
            HttpHeaderMap headers,
            Long httpRequestTimeout) throws InterruptedException, IOException, HiroException;

    /**
     * Send a HttpRequest synchronously and return the httpResponse
     *
     * @param httpRequest The httpRequest to send
     * @param maxRetries  The amount of retries on errors. When this is null, maxRetries will be used.
     * @return A HttpResponse containing an InputStream of the incoming body part of
     *         the result.
     * @throws HiroException        When status errors occur.
     * @throws IOException          On IO errors with the connection.
     * @throws InterruptedException When the call gets interrupted.
     */
    HttpResponse<InputStream> send(
            HttpRequest httpRequest,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * <p>
     * Send a HttpRequest asynchronously and return a future for a {@link HttpResponseParser}.
     * </p>
     * <p>
     * Applies the internal {@link #checkResponse(HttpResponse, int)}.
     * The retry counter is irrelevant here. When the token expires, it will be detected and a new token will be
     * requested, but you need to handle a {@link CompletionException} and look for the cause {@link RetryException}
     * within it. If this is the cause, you need to retry the same HttpRequest again externally.
     * </p>
     * <p>
     * Also logs the response.
     * </p>
     *
     * @param httpRequest The httpRequest to send
     * @return A future for a {@link HttpResponseParser} containing the response.
     */
    CompletableFuture<HttpResponseParser> sendAsync(HttpRequest httpRequest);

    /**
     * Basic method which returns an object of type {@link HiroMessage} constructed via a JSON body httpResponse. Sets an appropriate Accept header.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as String. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return An object of clazz constructed from the JSON result or null if the response has no body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T executeWithStringBody(
            Class<T> clazz,
            URI uri,
            String method,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Basic method which returns an object of type {@link HiroMessage} constructed via a JSON body httpResponse. Sets an appropriate Accept header.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param bodyContainer      The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return An object of clazz constructed from the JSON result or null if the response has no body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T executeWithStreamBody(
            Class<T> clazz,
            URI uri,
            String method,
            StreamContainer bodyContainer,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Method to communicate via InputStreams.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param bodyContainer      The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @return A {@link HttpResponseParser} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    HttpResponseParser executeBinary(
            URI uri,
            String method,
            StreamContainer bodyContainer,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Method to communicate asynchronously via String body.
     * This logs only the request, not the result.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as String. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @return A future for the {@link HttpResponseParser}.
     * @throws HiroException        On errors with protocol handling.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    CompletableFuture<HttpResponseParser> executeAsyncWithStringBody(
            URI uri,
            String method,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout) throws InterruptedException, IOException, HiroException;

    /**
     * Method to communicate asynchronously via InputStreams.
     * This logs only the request, not the result.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param bodyContainer      The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @return A future for the {@link HttpResponseParser}.
     * @throws HiroException        On errors with protocol handling.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    CompletableFuture<HttpResponseParser> executeAsyncWithStreamBody(
            URI uri,
            String method,
            StreamContainer bodyContainer,
            HttpHeaderMap headers,
            Long httpRequestTimeout) throws InterruptedException, IOException, HiroException;

    /**
     * Basic GET method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T get(
            Class<T> clazz,
            URI uri,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Basic POST method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T post(
            Class<T> clazz,
            URI uri,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Basic PUT method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T put(
            Class<T> clazz,
            URI uri,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Basic PATCH method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T patch(
            Class<T> clazz,
            URI uri,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Basic DELETE method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T delete(
            Class<T> clazz,
            URI uri,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Basic GET method which returns an InputStream from the body of the httpResponse.
     *
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @return A {@link HttpResponseParser} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    HttpResponseParser getBinary(
            URI uri,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries)
            throws HiroException, IOException, InterruptedException;

    /**
     * Basic POST method which sends an InputStream.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link StreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T postBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Basic PUT method which sends an InputStream.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link StreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T putBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Basic PATCH method which sends an InputStream.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link StreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           httpRequestTimeout will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, maxRetries will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    <T extends HiroMessage> T patchBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException;

    /**
     * Override this to add authentication tokens.
     *
     * @param headers Map of headers with initial values.
     * @throws HiroException        On internal errors regarding hiro data processing.
     * @throws IOException          On IO errors.
     * @throws InterruptedException When a call (possibly of an overwritten method) gets interrupted.
     */
    void addToHeaders(HttpHeaderMap headers) throws InterruptedException, IOException, HiroException;

    /**
     * The default way to check responses for errors and extract error messages.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries
     * @return true for a retry, false otherwise.
     * @throws TokenUnauthorizedException When the statusCode is 401.
     * @throws HiroHttpException          When the statusCode is &lt; 200 or &gt; 399.
     * @throws HiroException              On internal errors regarding hiro data processing.
     * @throws IOException                On IO errors.
     * @throws InterruptedException       When a call (possibly of an overwritten method) gets interrupted.
     */
    boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount)
            throws HiroException, IOException, InterruptedException;

    /**
     * Needs to be implemented by a supplier of a HttpLogger.
     *
     * @return The HttpLogger to use with this class.
     */
    HttpLogger getHttpLogger();

    /**
     * @return The HttpClient to use with this class. Lazy initialization.
     */
    HttpClient getOrBuildClient();

}
