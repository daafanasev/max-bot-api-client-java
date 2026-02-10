/*
 * ---------------------------------------------------------------------
 * Max chat Bot API
 * ---------------------------------------------------------------------
 * Copyright (C) 2025 COMMUNICATION PLATFORM LLC
 * ---------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---------------------------------------------------------------------
 */

package ru.max.botapi.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.max.botapi.Version;
import ru.max.botapi.client.impl.JacksonSerializer;
import ru.max.botapi.client.impl.OkHttpTransportClient;
import ru.max.botapi.exceptions.*;
import ru.max.botapi.model.Error;
import ru.max.botapi.queries.MaxQuery;
import ru.max.botapi.queries.QueryParam;
import ru.max.botapi.queries.upload.MaxUploadQuery;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

public class MaxClient implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String ENDPOINT_ENV_VAR_NAME = "MAX_BOTAPI_ENDPOINT";
    private static final String ENDPOINT = "https://platform-api.max.ru";
    private final String accessToken;
    private final MaxTransportClient transport;
    private final MaxSerializer serializer;
    private final String endpoint;

    public MaxClient(String accessToken, MaxTransportClient transport, MaxSerializer serializer) {
        this.endpoint = createEndpoint();
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
    }

    public static MaxClient create(String accessToken) {
        Objects.requireNonNull(accessToken, "No access token given. Get it using https://max.ru/MasterBot");
        OkHttpTransportClient transport = new OkHttpTransportClient();
        JacksonSerializer serializer = new JacksonSerializer();
        return new MaxClient(accessToken, transport, serializer);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public MaxSerializer getSerializer() {
        return serializer;
    }

    public MaxTransportClient getTransport() {
        return transport;
    }

    public <T> Future<T> newCall(MaxUploadQuery<T> query) throws ClientException {
        try {
            String url = buildURL(query);
            Future<ClientResponse> call = query.getUploadExec().newCall(getTransport());
            return new FutureResult<>(call, rawResponse -> handleResponse(rawResponse, query, url));
        } catch (InterruptedException e) {
            throw new ClientException(e);
        }
    }

    public <T> Future<T> newCall(MaxQuery<T> query) throws ClientException {
        MaxTransportClient.Method method = query.getMethod();
        String url = buildURL(query);
        byte[] requestBody = getSerializer().serialize(query.getBody());

        Future<ClientResponse> call;
        try {
            switch (method) {
                case GET:
                    call = getTransport().get(url);
                    break;
                case POST:
                    call = getTransport().post(url, requestBody);
                    break;
                case PUT:
                    call = getTransport().put(url, requestBody);
                    break;
                case DELETE:
                    call = getTransport().delete(url);
                    break;
                case PATCH:
                    call = getTransport().patch(url, requestBody);
                    break;
                default:
                    throw new ClientException(400, "Method " + method.name() + " is not supported.");
            }
        } catch (TransportClientException e) {
            throw new ClientException(e);
        }

        return new FutureResult<>(call, rawResponse -> handleResponse(rawResponse, query, url));
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }

    public String buildURL(MaxQuery<?> query) throws ClientException {
        String url = query.getUrl();
        StringBuilder sb = new StringBuilder(url);
        if (!url.regionMatches(true, 0, "http", 0, 4)) {
            sb.insert(0, getEndpoint());
        }

        if (url.indexOf('?') == -1) {
            sb.append('?');
        } else {
            sb.append('&');
        }

        sb.append("access_token=").append(getAccessToken());
        sb.append('&');
        sb.append("v=").append(Version.get());

        List<QueryParam<?>> params = query.getParams();
        if (params == null) {
            return sb.toString();
        }

        for (QueryParam<?> param : params) {
            String name = param.getName();
            if (param.getValue() == null) {
                if (param.isRequired()) {
                    throw new RequiredParameterMissingException("Required param " + name + " is missing.");
                }

                continue;
            }

            sb.append('&');
            sb.append(name);
            sb.append('=');
            try {
                sb.append(encodeParam(param.format()));
            } catch (UnsupportedEncodingException e) {
                throw new ClientException(e);
            }
        }

        return sb.toString();
    }

    String getEnvironment(String name) {
        return System.getenv(name);
    }

    protected String encodeParam(String paramValue) throws UnsupportedEncodingException {
        return URLEncoder.encode(paramValue, StandardCharsets.UTF_8.name());
    }

    private <T> T handleResponse(ClientResponse response, MaxQuery<T> query, String url) throws ClientException, APIException {
        String responseBody = response.getBodyAsString();
        if (response.getStatusCode() == 503) {
            LOG.error("Error 503 while executing query, query url: {}, query body: {}, responseBody: {}",
                    url,
                    query.getBody(),
                    responseBody);
            throw new ServiceNotAvailableException(responseBody);
        }

        MaxSerializer serializer = getSerializer();
        if (response.getStatusCode() / 100 == 2) {
            return serializer.deserialize(responseBody, query.getResponseType());
        }

        LOG.error("Error while executing query, query url: {}, query body: {}, responseBody: {}",
                url,
                query.getBody(),
                responseBody);

        Error error;
        try {
            error = serializer.deserialize(responseBody, Error.class);
        } catch (SerializationException e) {
            throw new APIException(response.getStatusCode(), responseBody);
        }

        if (error == null) {
            throw new APIException(response.getStatusCode());
        }

        throw ExceptionMapper.map(response.getStatusCode(), error);
    }

    private String createEndpoint() {
        String env = getEnvironment(ENDPOINT_ENV_VAR_NAME);
        if (env != null) {
            return env;
        }

        return System.getProperty("max.botapi.endpoint", ENDPOINT);
    }
}
