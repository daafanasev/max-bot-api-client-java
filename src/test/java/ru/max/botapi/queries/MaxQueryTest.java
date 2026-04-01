package ru.max.botapi.queries;


import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import ru.max.botapi.client.ClientResponse;
import ru.max.botapi.client.MaxClient;
import ru.max.botapi.client.MaxSerializer;
import ru.max.botapi.client.MaxTransportClient;
import ru.max.botapi.exceptions.APIException;
import ru.max.botapi.exceptions.AttachmentNotReadyException;
import ru.max.botapi.exceptions.ChatAccessForbiddenException;
import ru.max.botapi.exceptions.ClientException;
import ru.max.botapi.exceptions.RequiredParameterMissingException;
import ru.max.botapi.exceptions.SendMessageForbiddenException;
import ru.max.botapi.exceptions.ServiceNotAvailableException;
import ru.max.botapi.exceptions.TooManyRequestsException;
import ru.max.botapi.exceptions.TransportClientException;
import ru.max.botapi.model.Error;
import ru.max.botapi.model.User;
import ru.max.botapi.server.MaxServer;
import ru.max.botapi.server.MaxService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static spark.Spark.get;
import static spark.Spark.halt;


public class MaxQueryTest extends UnitTestBase {
    private static final Error ERROR
            = new Error("error.code", "error");

    private static final Error TOO_MANY_REQUESTS
            = new Error("too.many.requests", "error");

    private static final Error ATTACH_NOT_READY_ERROR
            = new Error("attachment.not.ready", "error");

    private static final Error CHAT_DENIED_ERROR
            = new Error("chat.denied", "error");

    private static final Error CANNON_SEND_ERROR
            = new Error("chat.denied", "chat.send.msg.no.permission.because.not.admin");

    private static final Future<ClientResponse> INTERRUPTING_FUTURE = new Future<ClientResponse>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public ClientResponse get() throws InterruptedException, ExecutionException {
            throw new InterruptedException("test interruption");
        }

        @Override
        public ClientResponse get(long timeout, @NotNull TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            throw new InterruptedException("test interruption");
        }
    };

    @BeforeClass
    public static void before() {
        ObjectMapper mapper = new ObjectMapper();
        get("/serviceunavailable", ((request, response) -> halt(503)));
        get("/internalerror", ((request, response) -> halt(500, "not json body")));
        get("/emptybody", ((request, response) -> halt(500, null)));
        get("/errorbody", ((request, response) -> halt(500, mapper.writeValueAsString(ERROR))));
        get("/toomanyrequests", ((request, response) -> halt(429, mapper.writeValueAsString(TOO_MANY_REQUESTS))));
        get("/attachnotready", ((request, response) -> halt(400, mapper.writeValueAsString(ATTACH_NOT_READY_ERROR))));
        get("/accessdenied", ((request, response) -> halt(403, mapper.writeValueAsString(CHAT_DENIED_ERROR))));
        get("/cannotsend", ((request, response) -> halt(403, mapper.writeValueAsString(CANNON_SEND_ERROR))));
        get("/ok", ((request, response) -> "{}"));
    }

    @Test(expected = ServiceNotAvailableException.class)
    public void shouldThrowServiceUnavailableException() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(client, "/serviceunavailable", Void.class,
                MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test(expected = APIException.class)
    public void shouldThrowAPIException() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(client, "/internalerror", Void.class,
                MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test(expected = APIException.class)
    public void shouldThrowAPIException2() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(client, "/emptybody", Void.class, MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test(expected = APIException.class)
    public void shouldParseError() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(client, "/errorbody", Void.class, MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test(expected = TooManyRequestsException.class)
    public void shouldThrowTMRException() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(client, "/toomanyrequests", Void.class,
                MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test(expected = AttachmentNotReadyException.class)
    public void shouldThrowANRException() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(client, "/attachnotready", Void.class,
                MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test(expected = ChatAccessForbiddenException.class)
    public void shouldThrowAccessDeniedException() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(client, "/accessdenied", Void.class,
                MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test(expected = SendMessageForbiddenException.class)
    public void shouldThrowSMFException() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(client, "/cannotsend", Void.class,
                MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test(expected = ClientException.class)
    public void shouldThrowClientException() throws Exception {
        MaxQuery<Void> query = new MaxQuery<>(invalidClient, "/me", Void.class, MaxTransportClient.Method.GET);
        query.execute();
    }

    @Test
    public void testAsync() throws Exception {
        Future<User> future = new MaxQuery<>(client, "/ok", User.class, MaxTransportClient.Method.GET).enqueue();
        future.get();
    }

    @Test(expected = ClientException.class)
    public void testAsyncError() throws Throwable {
        Future<User> future = new MaxQuery<>(invalidClient, "/ok", User.class,
                MaxTransportClient.Method.GET).enqueue();
        try {
            future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = ClientException.class)
    public void shouldThrowExceptionOnUnsupportedMethodCall() throws Exception {
        new MaxQuery<>(client, "/me", User.class, MaxTransportClient.Method.OPTIONS).execute();
    }

    @Test(expected = ClientException.class)
    public void shouldWrapTransportException() throws Exception {
        MaxTransportClient transport = mock(MaxTransportClient.class);
        MaxSerializer serializer = mock(MaxSerializer.class);
        when(transport.post(anyString(), anyString(), any(byte[].class))).thenThrow(new TransportClientException("test exception"));
        MaxClient clientMock = new MaxClient(MaxService.ACCESS_TOKEN, transport, serializer);
        new MaxQuery<>(clientMock, "/me", User.class, MaxTransportClient.Method.POST).execute();
    }

    @Test
    public void shouldAppendParamsToUrlIfItAlreadyHasParams() throws Exception {
        MaxQuery<User> query = new MaxQuery<>(client, "/me?param=value", User.class,
                MaxTransportClient.Method.GET);
        String param2Name = "param2";
        String param2Value = "value2";
        QueryParam<String> param2 = new QueryParam<>(param2Name, query);
        param2.setValue(param2Value);
        String url = client.buildURL(query);
        HttpUrl parsed = HttpUrl.parse(url);
        assertThat(parsed.queryParameter("param"), is("value"));
        assertThat(parsed.queryParameter(param2Name), is(param2Value));
    }

    @Test(expected = RequiredParameterMissingException.class)
    public void shouldThrowExceptionIfParamIsMissing() throws Exception {
        MaxQuery<User> query = new MaxQuery<>(client, "/me", User.class, MaxTransportClient.Method.GET);
        String param2Name = "param2";
        new QueryParam<String>(param2Name, query).required();
        client.buildURL(query);
    }

    @Test(expected = ClientException.class)
    public void shouldWrapInterruptedException() throws Exception {
        MaxTransportClient transport = mock(MaxTransportClient.class);
        MaxSerializer serializer = mock(MaxSerializer.class);
        when(transport.post(anyString(), anyString(), any(byte[].class))).thenReturn(INTERRUPTING_FUTURE);

        MaxClient clientMock = new MaxClient(MaxService.ACCESS_TOKEN, transport, serializer);
        new MaxQuery<>(clientMock, "/me", User.class, MaxTransportClient.Method.POST).execute();
    }

    @Test(expected = ClientException.class)
    public void shouldWrapEncodingException() throws Exception {
        MaxClient client = new MaxClient(MaxService.ACCESS_TOKEN, transport, serializer) {
            @Override
            public String getEndpoint() {
                return MaxServer.ENDPOINT;
            }

            @Override
            protected String encodeParam(String paramValue) throws UnsupportedEncodingException {
                throw new UnsupportedEncodingException("test");
            }
        };

        MaxQuery<User> query = new MaxQuery<User>(client, "/me", User.class, MaxTransportClient.Method.POST);

        new QueryParam<>("param", "value", query);
        client.buildURL(query);
    }
}