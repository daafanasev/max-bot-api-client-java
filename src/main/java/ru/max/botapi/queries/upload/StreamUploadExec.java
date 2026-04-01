package ru.max.botapi.queries.upload;

import ru.max.botapi.client.ClientResponse;
import ru.max.botapi.client.MaxTransportClient;
import ru.max.botapi.exceptions.ClientException;
import ru.max.botapi.exceptions.TransportClientException;

import java.io.InputStream;
import java.util.concurrent.Future;

class StreamUploadExec implements UploadExec {
    private final String fileName;
    private final InputStream input;
    private final String url;

    StreamUploadExec(String url, String fileName, InputStream input) {
        this.url = url;
        this.fileName = fileName;
        this.input = input;
    }

    @Override
    public Future<ClientResponse> newCall(MaxTransportClient transportClient, String token) throws ClientException {
        try {
            return transportClient.post(url, token, fileName, input);
        } catch (TransportClientException e) {
            throw new ClientException(e);
        }
    }
}
