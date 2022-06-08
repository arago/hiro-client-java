package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.token.TokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.DefaultHiroItemListMessage;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.timeseries.HiroTimeseriesListMessage;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;
import co.arago.hiro.client.model.vertex.HiroVertexMessage;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;
import co.arago.hiro.client.util.httpclient.StreamContainer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GraphAPI extends AbstractAuthenticatedAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractAuthenticatedAPIHandler.Conf<T> {

        public abstract GraphAPI build();
    }

    public static final class Builder extends Conf<Builder> {

        private Builder(String apiName, TokenAPIHandler tokenAPIHandler) {
            setApiName(apiName);
            setTokenApiHandler(tokenAPIHandler);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public GraphAPI build() {
            return new GraphAPI(this);
        }
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    /**
     * Create this APIHandler by using its Builder.
     *
     * @param builder The builder to use.
     */
    protected GraphAPI(Conf<?> builder) {
        super(builder);
    }

    public static Conf<?> newBuilder(TokenAPIHandler tokenAPIHandler) {
        return new Builder("graph", tokenAPIHandler);
    }

    /**
     * @param fromNodeId Source vertex of the edge.
     * @param verb       Verb/Name of the edge.
     * @param toNodeId   Destination vertex of the edge.
     * @return The ogitId for the edge.
     */
    public String createEdgeOgitId(String fromNodeId, String verb, String toNodeId) {
        return notBlank(fromNodeId, "fromNodeId") + "$$"
                + notBlank(verb, "verb") + "$$"
                + notBlank(toNodeId, "toNodeId");
    }

    // ###############################################################################################
    // ## API Requests ##
    // ###############################################################################################

    /**
     * A config class that contains common parameters for all Graph Queries.
     *
     * @param <T> The Builder type
     * @param <R> The type of the result expected from {@link #execute()}
     */
    protected abstract static class QueryBodyConf<T extends QueryBodyConf<T, R>, R> extends SendBodyAPIRequestConf<T, R> {

        protected Map<String, String> bodyMap = new HashMap<>();

        public QueryBodyConf(String... pathParts) {
            super(pathParts);
        }

        /**
         * Set arbitrary string to the {@link #bodyMap}.
         *
         * @param id    Key for the map
         * @param value Value for the map. Existing keys in the map will be removed when value == null.
         * @return {@link #self()};
         */
        public T addToBody(String id, String value) {
            bodyMap.compute(id, (k, v) -> value);
            return self();
        }

        /**
         * Set query fields
         *
         * @param fields the comma separated list of fields to return.
         * @return this
         */
        public T setFields(String fields) {
            return addToBody("fields", fields);
        }

        /**
         * Set listMeta
         *
         * @param listMeta return list type attributes with metadata
         * @return this
         */
        public T setListMeta(Boolean listMeta) {
            return addToBody("listMeta", (listMeta != null ? listMeta.toString() : null));
        }

        /**
         * Set includeDeleted
         *
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public T setIncludeDeleted(Boolean includeDeleted) {
            return addToBody("includeDeleted", (includeDeleted != null ? includeDeleted.toString() : null));
        }

        protected String createBody() {
            setJsonBodyFromMap(bodyMap);
            return body;
        }
    }

    // ----------------------------------- Query vertices -----------------------------------

    /**
     * query vertices
     * <p>
     * API POST /api/graph/[version]/query/vertices
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_vertices">API Documentation</a>
     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-query-string-query.html#query-string-syntax">ElasticSearch query syntax</a>
     */
    public class QueryVerticesCommand extends QueryBodyConf<QueryVerticesCommand, HiroVertexListMessage> {

        /**
         * @param query The query string, e.g. ogit\/_type: ogit\/Question.
         */
        protected QueryVerticesCommand(String query) {
            super("query", "vertices");
            addToBody("query", notBlank(query, "query"));
        }

        /**
         * Set limit
         *
         * @param limit limit of entries to return
         * @return this
         */
        public QueryVerticesCommand setLimit(Integer limit) {
            return addToBody("limit", (limit != null ? limit.toString() : null));
        }

        /**
         * Set offset
         *
         * @param offset offset where to start returning entries
         * @return this
         */
        public QueryVerticesCommand setOffset(Integer offset) {
            return addToBody("offset", (offset != null ? offset.toString() : null));
        }

        /**
         * Set order
         *
         * @param order order by a field asc|desc, e.g. ogit/name desc.
         * @return this
         */
        public QueryVerticesCommand setOrder(String order) {
            return addToBody("order", order);
        }

        /**
         * Set count
         *
         * @param count return number of matching elements
         * @return this
         */
        public QueryVerticesCommand setCount(Boolean count) {
            return addToBody("count", (count != null ? count.toString() : null));
        }

        @Override
        protected QueryVerticesCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListMessage.class,
                    getEndpointUri(path, query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * query vertices
     * <p>
     * API POST /api/graph/[version]/query/vertices
     *
     * @param query The query string, e.g. ogit\/_type: ogit\/Question.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_vertices">API Documentation</a>
     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-query-string-query.html#query-string-syntax">ElasticSearch query syntax</a>
     */
    public QueryVerticesCommand queryVerticesCommand(String query) {
        return new QueryVerticesCommand(query);
    }

    // ----------------------------------- Query gremlin -----------------------------------

    /**
     * query gremlin
     * <p>
     * API POST /api/graph/[version]/query/gremlin
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_gremlin">API Documentation</a>
     * @see <a href="https://github.com/spmallette/GremlinDocs">Gremlin query syntax</a>
     */
    public class QueryGremlinCommand extends QueryBodyConf<QueryGremlinCommand, HiroVertexListMessage> {

        /**
         * @param ogitId The root ogitId to start the query from.
         * @param query  The query string, e.g. ogit\/_type: ogit\/Question.
         */
        protected QueryGremlinCommand(String ogitId, String query) {
            super("query", "gremlin");
            addToBody("root", notBlank(ogitId, "ogitId / root"));
            addToBody("query", notBlank(query, "query"));
        }

        @Override
        protected QueryGremlinCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListMessage.class,
                    getEndpointUri(path, query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * query gremlin
     * <p>
     * API POST /api/graph/[version]/query/gremlin
     *
     * @param ogitId The root ogitId to start the query from.
     * @param query  The query string, e.g. ogit\/_type: ogit\/Question.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_gremlin">API Documentation</a>
     * @see <a href="https://github.com/spmallette/GremlinDocs">Gremlin query syntax</a>
     */
    public QueryGremlinCommand queryGremlinCommand(String ogitId, String query) {
        return new QueryGremlinCommand(ogitId, query);
    }

    // ----------------------------------- Query multiple entities by ids -----------------------------------

    /**
     * query by ids
     * <p>
     * API POST /api/graph/[version]/query/ids
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_ids">API Documentation</a>
     */
    public class QueryByIdsCommand extends QueryBodyConf<QueryByIdsCommand, HiroVertexListMessage> {

        /**
         * @param ids The comma separated list of ids, e.g. id1,id2,id3
         */
        protected QueryByIdsCommand(String ids) {
            super("query", "ids");
            addToBody("query", notBlank(ids, "query (csv list of ids)"));
        }

        @Override
        protected QueryByIdsCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListMessage.class,
                    getEndpointUri(path, query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * query by ids
     * <p>
     * API POST /api/graph/[version]/query/ids
     *
     * @param ids The comma separated list of ids, e.g. id1,id2,id3
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_ids">API Documentation</a>
     */
    public QueryByIdsCommand queryByIdsCommand(String ids) {
        return new QueryByIdsCommand(ids);
    }

    // ----------------------------------- Query entity by xid -----------------------------------

    /**
     * query by xid
     * <p>
     * API POST /api/graph/[version]/query/xid
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_xid">API Documentation</a>
     */
    public class QueryByXidCommand extends QueryBodyConf<QueryByXidCommand, HiroVertexListMessage> {

        /**
         * @param xid The xid
         */
        protected QueryByXidCommand(String xid) {
            super("query", "xid");
            addToBody("query", notBlank(xid, "query (ogit/_xid)"));
        }

        @Override
        protected QueryByXidCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListMessage.class,
                    getEndpointUri(path, query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * query by xid
     * <p>
     * API POST /api/graph/[version]/query/xid
     *
     * @param xid The xid
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_xid">API Documentation</a>
     */
    public QueryByXidCommand queryByXidCommand(String xid) {
        return new QueryByXidCommand(xid);
    }

    // ----------------------------------- Query timeseries values -----------------------------------

    /**
     * query timeseries values
     * <p>
     * API POST /api/graph/[version]/query/values
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_values">API Documentation</a>
     */
    public class QueryTimeseriesCommand extends QueryBodyConf<QueryTimeseriesCommand, HiroVertexListMessage> {

        /**
         * @param query The actual query.
         *              e.g. ogit\/name:"my timeseries name" for vertices.
         */
        protected QueryTimeseriesCommand(String query) {
            super("query", "values");
            addToBody("query", notBlank(query, "query"));
        }

        /**
         * @param from timestamp in ms where to start returning entries (default: now - 1 hour)
         * @return this
         */
        public QueryTimeseriesCommand setFrom(long from) {
            query.put("from", String.valueOf(from));
            return this;
        }

        /**
         * @param to timestamp in ms where to end returning entries (default: now)
         * @return this
         */
        public QueryTimeseriesCommand setTo(long to) {
            query.put("to", String.valueOf(to));
            return this;
        }

        /**
         * @param limit limit of entries to return
         * @return this
         */
        public QueryTimeseriesCommand setLimit(Integer limit) {
            query.put("limit", (limit != null) ? limit.toString() : null);
            return this;
        }

        /**
         * @param order order by a timestamp asc|desc|none
         * @return this
         */
        public QueryTimeseriesCommand setOrder(String order) {
            query.put("order", order);
            return this;
        }

        /**
         * @param aggregate aggregate numeric values for multiple timeseries ids with same timestamp: avg|min|max|sum|none
         * @return this
         */
        public QueryTimeseriesCommand setAggregate(String aggregate) {
            query.put("aggregate", aggregate);
            return this;
        }

        @Override
        protected QueryTimeseriesCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListMessage.class,
                    getEndpointUri(path, query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * query timeseries values
     * <p>
     * API POST /api/graph/[version]/query/values
     *
     * @param query The query
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_values">API Documentation</a>
     */
    public QueryTimeseriesCommand queryTimeseriesCommand(String query) {
        return new QueryTimeseriesCommand(query);
    }

    // ----------------------------------- GetEntity (Vertex / Edge) ---------------------------------

    /**
     * get entity by ogit/_id (vertex or edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/get__id_">API Documentation</a>
     */
    public class GetEntityCommand extends APIRequestConf<GetEntityCommand, HiroVertexMessage> {

        /**
         * @param ogitId ogit/_id of the entity. This can be a vertex id or a composed edge id.
         */
        protected GetEntityCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"));
        }

        /**
         * @param fields the comma separated list of fields to return
         * @return this
         */
        public GetEntityCommand setFields(String fields) {
            query.put("fields", fields);
            return this;
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetEntityCommand setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        /**
         * @param listMeta return list type attributes with metadata
         * @return this
         */
        public GetEntityCommand setListMeta(boolean listMeta) {
            query.put("listMeta", String.valueOf(listMeta));
            return this;
        }

        /**
         * @param vid get specific version of Entity matching ogit/_v-id
         * @return this
         */
        public GetEntityCommand setVid(String vid) {
            query.put("vid", vid);
            return this;
        }

        @Override
        protected GetEntityCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return get(HiroVertexMessage.class,
                    getEndpointUri(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get entity by ogit/_id (vertex)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}
     *
     * @param ogitId ogit/_id of the vertex.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/get__id_">API Documentation</a>
     */
    public GetEntityCommand getVertexCommand(String ogitId) {
        return new GetEntityCommand(ogitId);
    }

    /**
     * get entity by ogit/_id (edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}
     *
     * @param fromNodeId Source vertex of the edge.
     * @param verb       Verb/Name of the edge.
     * @param toNodeId   Destination vertex of the edge.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/get__id_">API Documentation</a>
     */
    public GetEntityCommand getEdgeCommand(String fromNodeId, String verb, String toNodeId) {
        return new GetEntityCommand(createEdgeOgitId(fromNodeId, verb, toNodeId));
    }

    // ----------------------------------- UpdateEntity (vertex) -----------------------------------

    /**
     * update vertex entity by ogit/_id.
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/post__id_">API Documentation</a>
     */
    public class UpdateEntityCommand extends SendBodyAPIRequestConf<UpdateEntityCommand, HiroVertexMessage> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected UpdateEntityCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"));
        }

        /**
         * @param attributes The complete entity data. Map 'attributes' needs to contain a key "ogit/_id" with a
         *                   non-blank value.
         */
        protected UpdateEntityCommand(Map<String, Object> attributes) {
            this((String) attributes.get("ogit/_id"));
            setJsonBodyFromMap(attributes);
        }

        /**
         * @param fullResponse return full Entity after update is applied (default returns only metadata)
         * @return this
         */
        public UpdateEntityCommand setFullResponse(boolean fullResponse) {
            query.put("fullResponse", String.valueOf(fullResponse));
            return this;
        }

        /**
         * @param listMeta return list type attributes with metadata
         * @return this
         */
        public UpdateEntityCommand setListMeta(boolean listMeta) {
            query.put("listMeta", String.valueOf(listMeta));
            return this;
        }

        @Override
        protected UpdateEntityCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexMessage.class,
                    getEndpointUri(path, query, fragment),
                    notBlank(body, "body with entity (vertex) data"),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * update vertex entity by ogit/_id.
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}
     *
     * @param ogitId ogit/_id of the vertex.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/post__id_">API Documentation</a>
     */
    public UpdateEntityCommand updateVertexCommand(String ogitId) {
        return new UpdateEntityCommand(ogitId);
    }

    /**
     * update vertex entity by ogit/_id.
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}
     *
     * @param attributes The complete entity data. Map 'attributes' needs to contain a key "ogit/_id" with a
     *                   non-blank value.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/post__id_">API Documentation</a>
     */
    public UpdateEntityCommand updateVertexCommand(Map<String, Object> attributes) {
        return new UpdateEntityCommand(attributes);
    }

    // ----------------------------------- DeleteEntity (vertex / edge) -----------------------------------

    /**
     * delete entity by ogit/_id (vertex or edge).
     * <p>
     * API DELETE /api/graph/[version]/{ogit/_id}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/delete__id_">API Documentation</a>
     */
    public class DeleteEntityCommand extends APIRequestConf<DeleteEntityCommand, HiroVertexMessage> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected DeleteEntityCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"));
        }

        @Override
        protected DeleteEntityCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return delete(HiroVertexMessage.class,
                    getEndpointUri(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * delete entity by ogit/_id (vertex).
     * <p>
     * API DELETE /api/graph/[version]/{ogit/_id}
     *
     * @param ogitId ogit/_id of the vertex.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/delete__id_">API Documentation</a>
     */
    public DeleteEntityCommand deleteVertexCommand(String ogitId) {
        return new DeleteEntityCommand(ogitId);
    }

    /**
     * delete entity by ogit/_id (edge).
     * <p>
     * API DELETE /api/graph/[version]/{ogit/_id}
     *
     * @param fromNodeId Source vertex of the edge.
     * @param verb       Verb/Name of the edge.
     * @param toNodeId   Destination vertex of the edge.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/delete__id_">API Documentation</a>
     */
    public DeleteEntityCommand disconnectVertices(String fromNodeId, String verb, String toNodeId) {
        return new DeleteEntityCommand(createEdgeOgitId(fromNodeId, verb, toNodeId));
    }

    // ----------------------------------- CreateEntity (vertex) -----------------------------------

    /**
     * create vertex entity using ogit/_type.
     * <p>
     * If defined in OGIT for entity type one may set in content ogit/isPermanent = true to make entity immutable
     * <p>
     * API POST /api/graph/[version]/new/{ogit/_type}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/post_new__type_">API Documentation</a>
     */
    public class CreateEntityCommand extends SendBodyAPIRequestConf<CreateEntityCommand, HiroVertexMessage> {

        /**
         * @param ogitType ogit/_type of the vertex.
         */
        protected CreateEntityCommand(String ogitType) {
            super("new", notBlank(ogitType, "ogitType"));
        }

        /**
         * @param attributes The complete entity data. Map 'attributes' needs to contain a key "ogit/_type" with a
         *                   non-blank value.
         */
        protected CreateEntityCommand(Map<String, Object> attributes) {
            this((String) attributes.get("ogit/_type"));
            setJsonBodyFromMap(attributes);
        }

        @Override
        protected CreateEntityCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexMessage.class,
                    getEndpointUri(path, query, fragment),
                    notBlank(body, "body with entity (vertex) data"),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * create vertex entity using ogit/_type.
     * <p>
     * If defined in OGIT for entity type one may set in content ogit/isPermanent = true to make entity immutable
     * <p>
     * API POST /api/graph/[version]/new/{ogit/_type}
     *
     * @param ogitType ogit/_type of the vertex.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/post_new__type_">API Documentation</a>
     */
    public CreateEntityCommand createVertexCommand(String ogitType) {
        return new CreateEntityCommand(ogitType);
    }

    /**
     * create vertex entity using ogit/_type.
     * <p>
     * If defined in OGIT for entity type one may set in content ogit/isPermanent = true to make entity immutable
     * <p>
     * API POST /api/graph/[version]/new/{ogit/_type}
     *
     * @param attributes The complete entity data. Map 'attributes' needs to contain a key "ogit/_type" with a
     *                   non-blank value.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/post_new__type_">API Documentation</a>
     */
    public CreateEntityCommand createVertexCommand(Map<String, Object> attributes) {
        return new CreateEntityCommand(attributes);
    }

    // ----------------------------------- PostVerb -----------------------------------

    /**
     * connect two vertices via an edge named verb.
     * <p>
     * API POST /api/graph/[version]/connect/{ogit/_type}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Verb/post_connect__type_">API Documentation</a>
     */
    public class PostVerbCommand extends SendBodyAPIRequestConf<PostVerbCommand, HiroVertexMessage> {

        /**
         * @param fromNodeId Source vertex of the edge.
         * @param verb       Verb/Name of the edge.
         * @param toNodeId   Destination vertex of the edge.
         */
        protected PostVerbCommand(String fromNodeId, String verb, String toNodeId) {
            super("connect", notBlank(verb, "verb"));
            setJsonBodyFromMap(Map.of(
                    "out", notBlank(fromNodeId, "fromNodeId"),
                    "in", notBlank(toNodeId, "toNodeId")));
        }

        @Override
        protected PostVerbCommand self() {
            return this;
        }

        /**
         * @return {@link HiroVertexMessage} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexMessage.class,
                    getEndpointUri(path, query, fragment),
                    notBlank(body, "body"),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * connect two vertices via an edge named verb.
     * <p>
     * API POST /api/graph/[version]/connect/{ogit/_type}
     *
     * @param fromNodeId Source vertex of the edge.
     * @param verb       Verb/Name of the edge.
     * @param toNodeId   Destination vertex of the edge.
     * @return New instance of the request
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Verb/post_connect__type_">API Documentation</a>
     */
    public PostVerbCommand connectVerticesCommand(String fromNodeId, String verb, String toNodeId) {
        return new PostVerbCommand(fromNodeId, verb, toNodeId);
    }

    // ----------------------------------- GetBlob -----------------------------------

    /**
     * get a binary blob / content / attachment from the graph
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/content
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Blob/get__id__content">API Documentation</a>
     */
    public class GetBlobCommand extends APIRequestConf<GetBlobCommand, HttpResponseParser> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected GetBlobCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"), "content");
        }

        /**
         * @param contentId specific version ogit/_c-id of content
         * @return this
         */
        public GetBlobCommand setContentId(String contentId) {
            query.put("contentId", contentId);
            return this;
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetBlobCommand setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        @Override
        protected GetBlobCommand self() {
            return this;
        }

        /**
         * @return A {@link HttpResponseParser} containing the InputStream of the content, the mediaType and the size
         *         (if available).
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HttpResponseParser execute() throws HiroException, IOException, InterruptedException {
            return getBinary(
                    getEndpointUri(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get content / blob / attachment of vertex
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/content
     *
     * @param ogitId ogit/_id of the vertex.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Blob/get__id__content">API Documentation</a>
     */
    public GetBlobCommand getAttachmentCommand(String ogitId) {
        return new GetBlobCommand(ogitId);
    }

    // ----------------------------------- PostBlob -----------------------------------

    /**
     * post a binary blob / content / attachment to the graph
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/content
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Blob/post__id__content">API Documentation</a>
     */
    public class PostBlobCommand extends SendStreamAPIRequestConf<PostBlobCommand, HiroMessage> {

        /**
         * @param ogitId          ogit/_id of the vertex.
         * @param streamContainer Container with the data stream.
         */
        protected PostBlobCommand(String ogitId, StreamContainer streamContainer) {
            super(streamContainer, notBlank(ogitId, "ogitId"), "content");
        }

        /**
         * @param ogitId      ogit/_id of the vertex.
         * @param inputStream InputStream for the data.
         */
        protected PostBlobCommand(String ogitId, InputStream inputStream) {
            super(inputStream, notBlank(ogitId, "ogitId"), "content");
        }

        @Override
        protected PostBlobCommand self() {
            return this;
        }

        /**
         * @return A {@link HiroMessage} containing the contentId of the blob.
         * @throws HiroException            When the call returns a http status error.
         * @throws IOException              When the call got an IO error.
         * @throws InterruptedException     When the call gets interrupted.
         * @throws IllegalArgumentException When the Content-Type is missing.
         */
        public HiroMessage execute() throws HiroException, IOException, InterruptedException {
            notBlank(streamContainer.getContentType(), "contentType");
            return postBinary(HiroMessage.class,
                    getEndpointUri(path, query, fragment),
                    streamContainer,
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * post a binary blob / content / attachment to the graph
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/content
     *
     * @param ogitId          ogit/_id of the vertex.
     * @param streamContainer Container with the data stream.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Blob/post__id__content">API Documentation</a>
     */
    public PostBlobCommand postAttachmentCommand(String ogitId, StreamContainer streamContainer) {
        return new PostBlobCommand(ogitId, streamContainer);
    }

    /**
     * post a binary blob / content / attachment to the graph
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/content
     *
     * @param ogitId      ogit/_id of the vertex.
     * @param inputStream InputStream with the data.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Blob/post__id__content">API Documentation</a>
     */
    public PostBlobCommand postAttachmentCommand(String ogitId, InputStream inputStream) {
        return new PostBlobCommand(ogitId, inputStream);
    }

    // ----------------------------------- GetHistory -----------------------------------

    /**
     * get the history of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/history
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_History/get__id__history">API Documentation</a>
     */
    public class GetHistoryCommand extends APIRequestConf<GetHistoryCommand, DefaultHiroItemListMessage> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected GetHistoryCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"), "history");
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetHistoryCommand setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        /**
         * @param from timestamp in ms where to start returning entries
         * @return this
         */
        public GetHistoryCommand setFrom(long from) {
            query.put("from", String.valueOf(from));
            return this;
        }

        /**
         * @param to timestamp in ms where to end returning entries (default: now)
         * @return this
         */
        public GetHistoryCommand setTo(long to) {
            query.put("to", String.valueOf(to));
            return this;
        }

        /**
         * @param limit limit of entries to return
         * @return this
         */
        public GetHistoryCommand setLimit(Integer limit) {
            query.put("limit", (limit != null) ? limit.toString() : null);
            return this;
        }

        /**
         * @param offset offset where to start returning entries
         * @return this
         */
        public GetHistoryCommand setOffset(Integer offset) {
            query.put("offset", (offset != null) ? offset.toString() : null);
            return this;
        }

        /**
         * @param listMeta return list type attributes with metadata
         * @return this
         */
        public GetHistoryCommand setListMeta(boolean listMeta) {
            query.put("listMeta", String.valueOf(listMeta));
            return this;
        }

        /**
         * @param type response format:
         *             <ul>
         *             <li>full - full event</li>
         *             <li>element - only event body</li>
         *             <li>diff - diff to previous event</li>
         *             </ul>
         * @return this
         */
        public GetHistoryCommand setType(String type) {
            query.put("type", type);
            return this;
        }

        /**
         * @param version get entry with specific ogit/_v value
         * @return this
         */
        public GetHistoryCommand setVersion(Integer version) {
            query.put("version", (version != null) ? version.toString() : null);
            return this;
        }

        /**
         * @param vid get specific version of Entity matching ogit/_v-id
         * @return this
         */
        public GetHistoryCommand setVid(String vid) {
            query.put("vid", vid);
            return this;
        }

        @Override
        protected GetHistoryCommand self() {
            return this;
        }

        /**
         * @return A {@link DefaultHiroItemListMessage} with the Json result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public DefaultHiroItemListMessage execute() throws HiroException, IOException, InterruptedException {
            return get(DefaultHiroItemListMessage.class,
                    getEndpointUri(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get the history of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/history
     *
     * @param ogitId ogit/_id of the vertex.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_History/get__id__history">API Documentation</a>
     */
    public GetHistoryCommand getHistoryCommand(String ogitId) {
        return new GetHistoryCommand(ogitId);
    }

    // ----------------------------------- GetTimeseries -----------------------------------

    /**
     * get timeseries of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/values
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Timeseries/get__id__values">API Documentation</a>
     */
    public class GetTimeseriesCommand extends APIRequestConf<GetTimeseriesCommand, HiroTimeseriesListMessage> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected GetTimeseriesCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"), "values");
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetTimeseriesCommand setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        /**
         * @param from timestamp in ms where to start returning entries (default: now - 1 hour)
         * @return this
         */
        public GetTimeseriesCommand setFrom(long from) {
            query.put("from", String.valueOf(from));
            return this;
        }

        /**
         * @param to timestamp in ms where to end returning entries (default: now)
         * @return this
         */
        public GetTimeseriesCommand setTo(long to) {
            query.put("to", String.valueOf(to));
            return this;
        }

        /**
         * @param limit limit of entries to return
         * @return this
         */
        public GetTimeseriesCommand setLimit(Integer limit) {
            query.put("limit", (limit != null) ? limit.toString() : null);
            return this;
        }

        /**
         * @param order order by a timestamp asc|desc|none
         * @return this
         */
        public GetTimeseriesCommand setOrder(String order) {
            query.put("order", order);
            return this;
        }

        /**
         * @param with csv list of ids to aggregate in result
         * @return this
         */
        public GetTimeseriesCommand setWith(String with) {
            query.put("with", with);
            return this;
        }

        /**
         * @param aggregate aggregate numeric values for multiple timeseries ids with same timestamp: avg|min|max|sum|none
         * @return this
         */
        public GetTimeseriesCommand setAggregate(String aggregate) {
            query.put("aggregate", aggregate);
            return this;
        }

        @Override
        protected GetTimeseriesCommand self() {
            return this;
        }

        /**
         * @return A {@link HiroTimeseriesListMessage} with the Json result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HiroTimeseriesListMessage execute() throws HiroException, IOException, InterruptedException {
            return get(HiroTimeseriesListMessage.class,
                    getEndpointUri(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get timeseries of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/values
     *
     * @param ogitId ogit/_id of the vertex.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Timeseries/get__id__values">API Documentation</a>
     */
    public GetTimeseriesCommand getTimeseriesCommand(String ogitId) {
        return new GetTimeseriesCommand(ogitId);
    }

    // ----------------------------------- GetTimeseriesHistory -----------------------------------

    /**
     * get timeseries history of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/values/history
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Timeseries/get__id__values_history">API Documentation</a>
     */
    public class GetTimeseriesHistoryCommand extends APIRequestConf<GetTimeseriesHistoryCommand, HiroMessage> {

        /**
         * @param ogitId    ogit/_id of the vertex.
         * @param timestamp Timestamp in ms.
         */
        protected GetTimeseriesHistoryCommand(String ogitId, long timestamp) {
            super(notBlank(ogitId, "ogitId"), "values", "history");
            query.put("timestamp", String.valueOf(timestamp));
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetTimeseriesHistoryCommand setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        @Override
        protected GetTimeseriesHistoryCommand self() {
            return this;
        }

        /**
         * @return A {@link HiroMessage} with the Json result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HiroMessage execute() throws HiroException, IOException, InterruptedException {
            return get(HiroMessage.class,
                    getEndpointUri(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get timeseries history of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/values/history
     *
     * @param ogitId    ogit/_id of the vertex.
     * @param timestamp Timestamp in ms.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Timeseries/get__id__values">API Documentation</a>
     */
    public GetTimeseriesHistoryCommand getTimeseriesHistoryCommand(String ogitId, long timestamp) {
        return new GetTimeseriesHistoryCommand(ogitId, timestamp);
    }

    // ----------------------------------- PostTimeseries -----------------------------------

    /**
     * post timeseries to an entity
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/values
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Timeseries/post__id__values">API Documentation</a>
     */
    public class PostTimeseriesCommand extends SendBodyAPIRequestConf<PostTimeseriesCommand, HiroMessage> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected PostTimeseriesCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"), "values");
        }

        /**
         * Set the timeseries.
         *
         * @param hiroTimeseriesListMessage The message for the body.
         */
        public void setTimeseries(HiroTimeseriesListMessage hiroTimeseriesListMessage) {
            setJsonBodyFromMessage(hiroTimeseriesListMessage);
        }

        @Override
        protected PostTimeseriesCommand self() {
            return this;
        }

        /**
         * @return {@link HiroMessage} The Json Response. Can be empty when status code is 202.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroMessage execute() throws HiroException, IOException, InterruptedException {
            return post(HiroMessage.class,
                    getEndpointUri(path, query, fragment),
                    notBlank(body, "body for timeseries data"),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * post timeseries of an entity (vertex / edge)
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/values
     *
     * @param ogitId ogit/_id of the vertex.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Timeseries/post__id__values">API Documentation</a>
     */
    public PostTimeseriesCommand postTimeseriesCommand(String ogitId) {
        return new PostTimeseriesCommand(ogitId);
    }

}
