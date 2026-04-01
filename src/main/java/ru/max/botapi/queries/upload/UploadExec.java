package ru.max.botapi.queries.upload;

import ru.max.botapi.client.ClientResponse;
import ru.max.botapi.client.MaxTransportClient;
import ru.max.botapi.exceptions.ClientException;

import java.util.concurrent.Future;

public interface UploadExec {
    Future<ClientResponse> newCall(MaxTransportClient transportClient, String token) throws ClientException, InterruptedException;
}
