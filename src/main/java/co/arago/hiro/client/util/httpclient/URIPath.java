package co.arago.hiro.client.util.httpclient;

import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
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
        scanAndAppendPaths(pathParts);
    }

    public URIPath(String... pathParts) {
        this(Arrays.asList(pathParts));
    }

    public URIPath clear() {
        this.path.clear();
        return this;
    }

    private void scanAndAppendPaths(List<String> pathParts) {
        for (String part : pathParts) {
            String[] subParts = part.split("/");
            for (String subPart : subParts) {
                if (StringUtils.isNotBlank(subPart))
                    this.path.add(subPart);
            }
        }
    }

    private void scanAndPrependPaths(List<String> pathParts) {
        for (int i = pathParts.size() - 1; i >= 0; i--) {
            String[] subParts = pathParts.get(i).split("/");
            for (int j = subParts.length - 1; j >= 0; j--) {
                if (StringUtils.isNotBlank(subParts[j]))
                    this.path.add(subParts[j]);
            }
        }
    }

    /**
     * @param pathParts Path fragments to add to the back of the path.
     * @return {@link #self()}
     */
    public URIPath append(List<String> pathParts) {
        scanAndAppendPaths(pathParts);
        return this;
    }

    /**
     * @param pathParts Path fragments to add to the back of the path.
     * @return {@link #self()}
     */
    public URIPath append(String... pathParts) {
        return append(Arrays.asList(pathParts));
    }

    /**
     * @param pathParts Path fragments to add to the front of the path.
     * @return {@link #self()}
     */
    public URIPath prepend(List<String> pathParts) {
        scanAndPrependPaths(pathParts);
        return this;
    }

    /**
     * @param pathParts Path fragments to add to the front of the path.
     * @return {@link #self()}
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
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }


}
