package co.arago.hiro.client.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This handles logging of HttpRequests and HttpResults.
 */
public class HttpLogger {
    final Logger log = LoggerFactory.getLogger(HttpLogger.class);

    protected Boolean logBody = true;

    /**
     * A list of paths from an URI. If such a filter matches, do not log body data.
     */
    protected final Set<String> filter = new HashSet<>();

    private String parsePath(URI uri) {
        return StringUtils.endsWith(uri.getPath(), "/") ? uri.getPath() : uri.getPath() + "/";
    }

    /*+
     * Add an uri path to the filter
     */
    public synchronized void addFilter(URI uri) {
        filter.add(parsePath(uri));
    }

    /*+
     * Remove an uri path from the filter
     */
    public synchronized void removeFilter(URI uri) {
        filter.remove(parsePath(uri));
    }

    protected boolean filterMatch(URI uri) {
        return StringUtils.startsWithAny(parsePath(uri), filter.toArray(new String[0]));
    }

    /**
     * Enable or disable logging of bodies. The values in the {@link #filter} override this.
     *
     * @param logBody The flag to set
     */
    public void setLogBody(Boolean logBody) {
        this.logBody = logBody;
    }

    /**
     * Hide critical data from header logging
     *
     * @param key   Header name
     * @param value Value of header
     * @return A secure string representation of the value of the header field.
     */
    protected String processHeaderField(String key, List<String> value) {
        if (StringUtils.equalsAnyIgnoreCase(key, "Authorization", "Cookie", "Set-Cookie")) {
            String result = value.get(0);
            if (result.length() > 20) {
                return result.substring(0, 10) + "...--- FIELD OBSCURED INTENTIONALLY ---..." + result.substring(result.length() - 10) + " (len: " + result.length() + ")";
            } else {
                return "<hidden>";
            }
        }

        return String.join(",", value);
    }

    /**
     * Log a request from the data inside HttpRequest.
     *
     * @param httpRequest The httpRequest to log.
     * @param body        The body of the httpRequest.
     */
    public void logRequest(HttpRequest httpRequest, Object body) {
        if (!log.isDebugEnabled())
            return;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append(System.lineSeparator())
                .append("####### HttpRequest #######")
                .append(System.lineSeparator())
                .append(httpRequest.method())
                .append(" ")
                .append(httpRequest.uri())
                .append(System.lineSeparator());

        for (Map.Entry<String, List<String>> headerEntry : httpRequest.headers().map().entrySet()) {
            stringBuilder
                    .append(headerEntry.getKey())
                    .append(": ")
                    .append(processHeaderField(headerEntry.getKey(), headerEntry.getValue()))
                    .append(System.lineSeparator());
        }

        if (body != null) {
            synchronized (this) {
                if (logBody && !filterMatch(httpRequest.uri())) {
                    if (body instanceof String) {
                        stringBuilder.append(System.lineSeparator()).append(body);
                    } else if (body instanceof InputStream) {
                        stringBuilder.append(System.lineSeparator()).append("--- stream ---");
                    }
                } else {
                    stringBuilder.append(System.lineSeparator()).append("--- BODY HIDDEN INTENTIONALLY ---");
                }
            }
        }

        stringBuilder.append(System.lineSeparator());

        log.debug(stringBuilder.toString());
    }

    /**
     * Log a response from the data inside HttpResponse.
     *
     * @param httpResponse The httpResponse to log.
     * @param body         The body of the httpResponse.
     */
    public void logResponse(HttpResponse<?> httpResponse, Object body) {
        if (!log.isDebugEnabled())
            return;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append(System.lineSeparator())
                .append("####### HttpResponse #######")
                .append(System.lineSeparator());

        for (Map.Entry<String, List<String>> headerEntry : httpResponse.headers().map().entrySet()) {
            stringBuilder
                    .append(headerEntry.getKey())
                    .append(": ")
                    .append(processHeaderField(headerEntry.getKey(), headerEntry.getValue()))
                    .append(System.lineSeparator());
        }

        if (body != null) {
            synchronized (this) {
                if (logBody && !(filterMatch(httpResponse.request().uri()) && httpResponse.statusCode() == 200)) {
                    if (body instanceof String) {
                        stringBuilder.append(System.lineSeparator()).append((String) body);
                    } else if (body instanceof InputStream) {
                        stringBuilder.append(System.lineSeparator()).append("--- stream ---");
                    }

                } else {
                    stringBuilder.append(System.lineSeparator()).append("--- BODY HIDDEN INTENTIONALLY ---");
                }
            }
        }

        stringBuilder.append(System.lineSeparator());

        log.debug(stringBuilder.toString());
    }

}
