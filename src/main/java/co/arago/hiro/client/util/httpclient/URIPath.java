package co.arago.hiro.client.util.httpclient;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple builder class for URI paths.
 */
public class URIPath {
    protected final LinkedList<String> path = new LinkedList<>();

    public URIPath() {
    }

    public URIPath(List<String> pathParts) {
        append(pathParts);
    }

    public URIPath(String... pathParts) {
        this(Arrays.asList(pathParts));
    }

    public URIPath clear() {
        this.path.clear();
        return this;
    }

    /**
     * @param pathParts Path fragments to add to the back of the path.
     * @return this
     */
    public URIPath append(List<String> pathParts) {
        for (String part : pathParts) {
            if (StringUtils.isNotBlank(part))
                this.path.add(part);
        }
        return this;
    }

    /**
     * @param pathParts Path fragments to add to the back of the path.
     * @return this
     */
    public URIPath append(String... pathParts) {
        return append(Arrays.asList(pathParts));
    }

    /**
     * @param pathParts Path fragments to add to the front of the path.
     * @return this
     */
    public URIPath prepend(List<String> pathParts) {
        for (int i = pathParts.size() - 1; i >= 0; i--) {
            if (StringUtils.isNotBlank(pathParts.get(i)))
                this.path.addFirst(pathParts.get(i));
        }
        return this;
    }

    /**
     * @param pathParts Path fragments to add to the front of the path.
     * @return this
     */
    public URIPath prepend(String... pathParts) {
        return prepend(Arrays.asList(pathParts));
    }

    /**
     * Create a valid escaped path string.
     *
     * @return The encoded path without a leading '/'.
     */
    public String build() {
        return path.stream()
                .map(part -> URLPartEncoder.encodeNoPlus(part, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }

}
