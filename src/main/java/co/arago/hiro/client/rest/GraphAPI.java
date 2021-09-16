package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.DefaultHiroItemListResponse;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.timeseries.HiroTimeseriesListMessage;
import co.arago.hiro.client.model.vertex.HiroVertexListResponse;
import co.arago.hiro.client.model.vertex.HiroVertexResponse;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;
import co.arago.hiro.client.util.httpclient.StreamContainer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GraphAPI extends AuthenticatedAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AuthenticatedAPIHandler.Conf<T> {
    }

    public static final class Builder extends Conf<Builder> {

        private Builder(String apiName, AbstractTokenAPIHandler tokenAPIHandler) {
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

    public static Builder newBuilder(AbstractTokenAPIHandler tokenAPIHandler) {
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

    protected abstract static class QueryBodyHandler<T extends QueryBodyHandler<T, R>, R> extends SendJsonAPIRequestConf<T, R> {

        protected Map<String, String> bodyMap = new HashMap<>();

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
            setBodyFromMap(bodyMap);
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
    public class QueryVertices extends QueryBodyHandler<QueryVertices, HiroVertexListResponse> {

        /**
         * @param query The query string, e.g. ogit\/_type: ogit\/Question.
         */
        protected QueryVertices(String query) {
            addToBody("query", notBlank(query, "query"));
        }

        /**
         * Set limit
         *
         * @param limit limit of entries to return
         * @return this
         */
        public QueryVertices setLimit(Integer limit) {
            return addToBody("limit", (limit != null ? limit.toString() : null));
        }

        /**
         * Set offset
         *
         * @param offset offset where to start returning entries
         * @return this
         */
        public QueryVertices setOffset(Integer offset) {
            return addToBody("offset", (offset != null ? offset.toString() : null));
        }

        /**
         * Set order
         *
         * @param order order by a field asc|desc, e.g. ogit/name desc.
         * @return this
         */
        public QueryVertices setOrder(String order) {
            return addToBody("order", order);
        }

        /**
         * Set count
         *
         * @param count return number of matching elements
         * @return this
         */
        public QueryVertices setCount(Boolean count) {
            return addToBody("count", (count != null ? count.toString() : null));
        }

        @Override
        protected QueryVertices self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListResponse execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListResponse.class,
                    getUri("query/vertices", query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public QueryVertices queryVertices(String query) {
        return new QueryVertices(query);
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
    public class QueryGremlin extends QueryBodyHandler<QueryGremlin, HiroVertexListResponse> {

        /**
         * @param ogitId The root ogitId to start the query from.
         * @param query  The query string, e.g. ogit\/_type: ogit\/Question.
         */
        protected QueryGremlin(String ogitId, String query) {
            addToBody("root", notBlank(ogitId, "ogitId / root"));
            addToBody("query", notBlank(query, "query"));
        }

        @Override
        protected QueryGremlin self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListResponse execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListResponse.class,
                    getUri("query/gremlin", query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public QueryGremlin queryGremlin(String ogitId, String query) {
        return new QueryGremlin(ogitId, query);
    }

    // ----------------------------------- Query multiple entities by ids -----------------------------------

    /**
     * query by ids
     * <p>
     * API POST /api/graph/[version]/query/ids
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_ids">API Documentation</a>
     */
    public class QueryByIds extends QueryBodyHandler<QueryByIds, HiroVertexListResponse> {

        /**
         * @param ids The comma separated list of ids, e.g. id1,id2,id3
         */
        protected QueryByIds(String ids) {
            addToBody("query", notBlank(ids, "query (csv list of ids)"));
        }

        @Override
        protected QueryByIds self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListResponse execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListResponse.class,
                    getUri("query/ids", query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public QueryByIds queryByIds(String ids) {
        return new QueryByIds(ids);
    }

    // ----------------------------------- Query entity by xid -----------------------------------

    /**
     * query by xid
     * <p>
     * API POST /api/graph/[version]/query/xid
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_xid">API Documentation</a>
     */
    public class QueryByXid extends QueryBodyHandler<QueryByXid, HiroVertexListResponse> {

        /**
         * @param xid The xid
         */
        protected QueryByXid(String xid) {
            addToBody("query", notBlank(xid, "query (ogit/_xid)"));
        }

        @Override
        protected QueryByXid self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListResponse execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListResponse.class,
                    getUri("query/xid", query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public QueryByXid queryByXid(String xid) {
        return new QueryByXid(xid);
    }

    // ----------------------------------- Query timeseries values -----------------------------------

    /**
     * query timeseries values
     * <p>
     * API POST /api/graph/[version]/query/values
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Search/post_query_values">API Documentation</a>
     */
    public class QueryTimeseries extends QueryBodyHandler<QueryTimeseries, HiroVertexListResponse> {

        /**
         * @param query The actual query.
         *              e.g. ogit\/name:"my timeseries name" for vertices.
         */
        protected QueryTimeseries(String query) {
            addToBody("query", notBlank(query, "query"));
        }

        /**
         * @param from timestamp in ms where to start returning entries (default: now - 1 hour)
         * @return this
         */
        public QueryTimeseries setFrom(long from) {
            query.put("from", String.valueOf(from));
            return this;
        }

        /**
         * @param to timestamp in ms where to end returning entries (default: now)
         * @return this
         */
        public QueryTimeseries setTo(long to) {
            query.put("to", String.valueOf(to));
            return this;
        }

        /**
         * @param limit limit of entries to return
         * @return this
         */
        public QueryTimeseries setLimit(Integer limit) {
            query.compute("limit", (k, v) -> (limit != null ? limit.toString() : null));
            return this;
        }

        /**
         * @param order order by a timestamp asc|desc|none
         * @return this
         */
        public QueryTimeseries setOrder(String order) {
            query.compute("order", (k, v) -> order);
            return this;
        }

        /**
         * @param aggregate aggregate numeric values for multiple timeseries ids with same timestamp: avg|min|max|sum|none
         * @return this
         */
        public QueryTimeseries setAggregate(String aggregate) {
            query.compute("aggregate", (k, v) -> aggregate);
            return this;
        }

        @Override
        protected QueryTimeseries self() {
            return this;
        }

        /**
         * @return {@link HiroVertexListResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListResponse execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexListResponse.class,
                    getUri("query/values", query, fragment),
                    createBody(),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public QueryTimeseries queryTimeseries(String query) {
        return new QueryTimeseries(query);
    }

    // ----------------------------------- GetEntity (Vertex / Edge) ---------------------------------

    /**
     * get entity by ogit/_id (vertex or edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/get__id_">API Documentation</a>
     */
    public class GetEntity extends APIRequestConf<GetEntity, HiroVertexResponse> {

        /**
         * @param ogitId ogit/_id of the entity. This can be a vertex id or a composed edge id.
         */
        protected GetEntity(String ogitId) {
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        /**
         * @param fields the comma separated list of fields to return
         * @return this
         */
        public GetEntity setFields(String fields) {
            query.put("fields", fields);
            return this;
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetEntity setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        /**
         * @param listMeta return list type attributes with metadata
         * @return this
         */
        public GetEntity setListMeta(boolean listMeta) {
            query.put("listMeta", String.valueOf(listMeta));
            return this;
        }

        /**
         * @param vid get specific version of Entity matching ogit/_v-id
         * @return this
         */
        public GetEntity setVid(String vid) {
            query.put("vid", vid);
            return this;
        }

        @Override
        protected GetEntity self() {
            return this;
        }

        /**
         * @return {@link HiroVertexResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            return get(HiroVertexResponse.class,
                    getUri(getRequestPath(path), query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public GetEntity getVertex(String ogitId) {
        return new GetEntity(ogitId);
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
    public GetEntity getEdge(String fromNodeId, String verb, String toNodeId) {
        return new GetEntity(createEdgeOgitId(fromNodeId, verb, toNodeId));
    }

    // ----------------------------------- UpdateEntity (vertex) -----------------------------------

    /**
     * update vertex entity by ogit/_id.
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/post__id_">API Documentation</a>
     */
    public class UpdateEntity extends SendJsonAPIRequestConf<UpdateEntity, HiroVertexResponse> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected UpdateEntity(String ogitId) {
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        /**
         * @param fullResponse return full Entity after update is applied (default returns only metadata)
         * @return this
         */
        public UpdateEntity setFullResponse(boolean fullResponse) {
            query.put("fullResponse", String.valueOf(fullResponse));
            return this;
        }

        /**
         * @param listMeta return list type attributes with metadata
         * @return this
         */
        public UpdateEntity setListMeta(boolean listMeta) {
            query.put("listMeta", String.valueOf(listMeta));
            return this;
        }


        @Override
        protected UpdateEntity self() {
            return this;
        }

        /**
         * @return {@link HiroVertexResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexResponse.class,
                    getUri(getRequestPath(path), query, fragment),
                    notBlank(body, "body with entity (vertex) data"),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public UpdateEntity updateVertex(String ogitId) {
        return new UpdateEntity(ogitId);
    }

    // ----------------------------------- DeleteEntity (vertex / edge) -----------------------------------

    /**
     * delete entity by ogit/_id (vertex or edge).
     * <p>
     * API DELETE /api/graph/[version]/{ogit/_id}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Entity/delete__id_">API Documentation</a>
     */
    public class DeleteEntity extends APIRequestConf<DeleteEntity, HiroVertexResponse> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected DeleteEntity(String ogitId) {
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        @Override
        protected DeleteEntity self() {
            return this;
        }

        /**
         * @return {@link HiroVertexResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            return delete(HiroVertexResponse.class,
                    getUri(getRequestPath(path), query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public DeleteEntity deleteVertex(String ogitId) {
        return new DeleteEntity(ogitId);
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
    public DeleteEntity disconnectVertices(String fromNodeId, String verb, String toNodeId) {
        return new DeleteEntity(createEdgeOgitId(fromNodeId, verb, toNodeId));
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
    public class CreateEntity extends SendJsonAPIRequestConf<CreateEntity, HiroVertexResponse> {

        /**
         * @param ogitType ogit/_type of the vertex.
         */
        protected CreateEntity(String ogitType) {
            appendToPath(notBlank(ogitType, "ogitType"));
        }

        @Override
        protected CreateEntity self() {
            return this;
        }

        /**
         * @return {@link HiroVertexResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexResponse.class,
                    getUri("new" + getRequestPath(path), query, fragment),
                    notBlank(body, "body with entity (vertex) data"),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public CreateEntity createVertex(String ogitType) {
        return new CreateEntity(ogitType);
    }

    // ----------------------------------- PostVerb -----------------------------------

    /**
     * connect two vertices via an edge named verb.
     * <p>
     * API POST /api/graph/[version]/connect/{ogit/_type}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Graph]_Verb/post_connect__type_">API Documentation</a>
     */
    public class PostVerb extends SendJsonAPIRequestConf<PostVerb, HiroVertexResponse> {

        /**
         * @param fromNodeId Source vertex of the edge.
         * @param verb       Verb/Name of the edge.
         * @param toNodeId   Destination vertex of the edge.
         */
        protected PostVerb(String fromNodeId, String verb, String toNodeId) {
            appendToPath(notBlank(verb, "verb"));
            setBodyFromMap(Map.of(
                    "out", notBlank(fromNodeId, "fromNodeId"),
                    "in", notBlank(toNodeId, "toNodeId")
            ));
        }

        @Override
        protected PostVerb self() {
            return this;
        }

        /**
         * @return {@link HiroVertexResponse} The Json Response
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            return post(HiroVertexResponse.class,
                    getUri("connect" + getRequestPath(path), query, fragment),
                    notBlank(body, "body"),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
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
    public PostVerb connectVertices(String fromNodeId, String verb, String toNodeId) {
        return new PostVerb(fromNodeId, verb, toNodeId);
    }

    // ----------------------------------- GetBlob -----------------------------------

    /**
     * get a binary blob / content / attachment from the graph
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/content
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Blob/get__id__content">API Documentation</a>
     */
    public class GetBlob extends APIRequestConf<GetBlob, HttpResponseParser> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected GetBlob(String ogitId) {
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        /**
         * @param contentId specific version ogit/_c-id of content
         * @return this
         */
        public GetBlob setContentId(String contentId) {
            query.put("contentId", contentId);
            return this;
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetBlob setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        @Override
        protected GetBlob self() {
            return this;
        }

        /**
         * @return A {@link HttpResponseParser} containing the InputStream of the content, the mediaType and the size
         * (if available).
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HttpResponseParser execute() throws HiroException, IOException, InterruptedException {
            return getBinary(
                    getUri(getRequestPath(path) + "/content", query, fragment),
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
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Blob/get__id__content">API Documentation</a>
     */
    public GetBlob getAttachment(String ogitId) {
        return new GetBlob(ogitId);
    }

    // ----------------------------------- PostBlob -----------------------------------

    /**
     * post a binary blob / content / attachment to the graph
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/content
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Blob/post__id__content">API Documentation</a>
     */
    public class PostBlob extends SendBinaryAPIRequestConf<PostBlob, HiroMessage> {

        /**
         * @param ogitId          ogit/_id of the vertex.
         * @param streamContainer Container with the data stream.
         */
        protected PostBlob(String ogitId, StreamContainer streamContainer) {
            super(streamContainer);
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        /**
         * @param ogitId      ogit/_id of the vertex.
         * @param inputStream InputStream for the data.
         */
        protected PostBlob(String ogitId, InputStream inputStream) {
            super(inputStream);
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        @Override
        protected PostBlob self() {
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
                    getUri(getRequestPath(path) + "/content", query, fragment),
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
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Blob/post__id__content">API Documentation</a>
     */
    public PostBlob postAttachment(String ogitId, StreamContainer streamContainer) {
        return new PostBlob(ogitId, streamContainer);
    }

    /**
     * post a binary blob / content / attachment to the graph
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/content
     *
     * @param ogitId      ogit/_id of the vertex.
     * @param inputStream InputStream with the data.
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Blob/post__id__content">API Documentation</a>
     */
    public PostBlob postAttachment(String ogitId, InputStream inputStream) {
        return new PostBlob(ogitId, inputStream);
    }

    // ----------------------------------- GetHistory -----------------------------------

    /**
     * get the history of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/history
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_History/get__id__history">API Documentation</a>
     */
    public class GetHistory extends APIRequestConf<GetHistory, DefaultHiroItemListResponse> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected GetHistory(String ogitId) {
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetHistory setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        /**
         * @param from timestamp in ms where to start returning entries
         * @return this
         */
        public GetHistory setFrom(long from) {
            query.put("from", String.valueOf(from));
            return this;
        }

        /**
         * @param to timestamp in ms where to end returning entries (default: now)
         * @return this
         */
        public GetHistory setTo(long to) {
            query.put("to", String.valueOf(to));
            return this;
        }

        /**
         * @param limit limit of entries to return
         * @return this
         */
        public GetHistory setLimit(Integer limit) {
            query.compute("limit", (k, v) -> (limit != null ? limit.toString() : null));
            return this;
        }

        /**
         * @param offset offset where to start returning entries
         * @return this
         */
        public GetHistory setOffset(Integer offset) {
            query.compute("offset", (k, v) -> (offset != null ? offset.toString() : null));
            return this;
        }

        /**
         * @param listMeta return list type attributes with metadata
         * @return this
         */
        public GetHistory setListMeta(boolean listMeta) {
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
        public GetHistory setType(String type) {
            query.compute("type", (k, v) -> type);
            return this;
        }

        /**
         * @param version get entry with specific ogit/_v value
         * @return this
         */
        public GetHistory setVersion(Integer version) {
            query.compute("version", (k, v) -> (version != null ? version.toString() : null));
            return this;
        }

        /**
         * @param vid get specific version of Entity matching ogit/_v-id
         * @return this
         */
        public GetHistory setVid(String vid) {
            query.compute("vid", (k, v) -> vid);
            return this;
        }

        @Override
        protected GetHistory self() {
            return this;
        }

        /**
         * @return A {@link DefaultHiroItemListResponse} with the Json result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public DefaultHiroItemListResponse execute() throws HiroException, IOException, InterruptedException {
            return get(DefaultHiroItemListResponse.class,
                    getUri(getRequestPath(path) + "/history", query, fragment),
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
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_History/get__id__history">API Documentation</a>
     */
    public GetHistory getHistory(String ogitId) {
        return new GetHistory(ogitId);
    }

    // ----------------------------------- GetTimeseries -----------------------------------

    /**
     * get timeseries of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/values
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Timeseries/get__id__values">API Documentation</a>
     */
    public class GetTimeseries extends APIRequestConf<GetTimeseries, HiroTimeseriesListMessage> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected GetTimeseries(String ogitId) {
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetTimeseries setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        /**
         * @param from timestamp in ms where to start returning entries (default: now - 1 hour)
         * @return this
         */
        public GetTimeseries setFrom(long from) {
            query.put("from", String.valueOf(from));
            return this;
        }

        /**
         * @param to timestamp in ms where to end returning entries (default: now)
         * @return this
         */
        public GetTimeseries setTo(long to) {
            query.put("to", String.valueOf(to));
            return this;
        }

        /**
         * @param limit limit of entries to return
         * @return this
         */
        public GetTimeseries setLimit(Integer limit) {
            query.compute("limit", (k, v) -> (limit != null ? limit.toString() : null));
            return this;
        }

        /**
         * @param order order by a timestamp asc|desc|none
         * @return this
         */
        public GetTimeseries setOrder(String order) {
            query.compute("order", (k, v) -> order);
            return this;
        }

        /**
         * @param with csv list of ids to aggregate in result
         * @return this
         */
        public GetTimeseries setWith(String with) {
            query.compute("with", (k, v) -> with);
            return this;
        }

        /**
         * @param aggregate aggregate numeric values for multiple timeseries ids with same timestamp: avg|min|max|sum|none
         * @return this
         */
        public GetTimeseries setAggregate(String aggregate) {
            query.compute("aggregate", (k, v) -> aggregate);
            return this;
        }

        @Override
        protected GetTimeseries self() {
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
                    getUri(getRequestPath(path) + "/values", query, fragment),
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
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Timeseries/get__id__values">API Documentation</a>
     */
    public GetTimeseries getTimeseries(String ogitId) {
        return new GetTimeseries(ogitId);
    }

    // ----------------------------------- GetTimeseriesHistory -----------------------------------

    /**
     * get timeseries history of an entity (vertex / edge)
     * <p>
     * API GET /api/graph/[version]/{ogit/_id}/values/history
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Timeseries/get__id__values_history">API Documentation</a>
     */
    public class GetTimeseriesHistory extends APIRequestConf<GetTimeseriesHistory, HiroMessage> {

        /**
         * @param ogitId    ogit/_id of the vertex.
         * @param timestamp Timestamp in ms.
         */
        protected GetTimeseriesHistory(String ogitId, long timestamp) {
            appendToPath(notBlank(ogitId, "ogitId"));
            query.put("timestamp", String.valueOf(timestamp));
        }

        /**
         * @param includeDeleted allow getting if ogit/_is-deleted=true
         * @return this
         */
        public GetTimeseriesHistory setIncludeDeleted(boolean includeDeleted) {
            query.put("includeDeleted", String.valueOf(includeDeleted));
            return this;
        }

        @Override
        protected GetTimeseriesHistory self() {
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
                    getUri(getRequestPath(path) + "/values/history", query, fragment),
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
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Query]_Timeseries/get__id__values">API Documentation</a>
     */
    public GetTimeseriesHistory getTimeseriesHistory(String ogitId, long timestamp) {
        return new GetTimeseriesHistory(ogitId, timestamp);
    }

    // ----------------------------------- PostTimeseries -----------------------------------

    /**
     * post timeseries to an entity
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/values
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Timeseries/post__id__values">API Documentation</a>
     */
    public class PostTimeseries extends SendJsonAPIRequestConf<PostTimeseries, HiroMessage> {

        /**
         * @param ogitId ogit/_id of the vertex.
         */
        protected PostTimeseries(String ogitId) {
            appendToPath(notBlank(ogitId, "ogitId"));
        }

        /**
         * Set the timeseries.
         *
         * @param hiroTimeseriesListMessage The message for the body.
         */
        public void setTimeseries(HiroTimeseriesListMessage hiroTimeseriesListMessage) {
            setBodyFromMessage(hiroTimeseriesListMessage);
        }

        @Override
        protected PostTimeseries self() {
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
                    getUri(getRequestPath(path) + "/values", query, fragment),
                    notBlank(body, "body for timeseries data"),
                    headers,
                    httpRequestTimeout,
                    maxRetries
            );
        }
    }

    /**
     * post timeseries of an entity (vertex / edge)
     * <p>
     * API POST /api/graph/[version]/{ogit/_id}/values
     *
     * @param ogitId ogit/_id of the vertex.
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/graph.yaml#/[Storage]_Timeseries/post__id__values">API Documentation</a>
     */
    public PostTimeseries postTimeseries(String ogitId) {
        return new PostTimeseries(ogitId);
    }

}
