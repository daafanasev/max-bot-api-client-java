package ru.max.botapi.queries.upload;

import ru.max.botapi.client.ClientResponse;
import ru.max.botapi.client.MaxTransportClient;
import ru.max.botapi.exceptions.ClientException;
import ru.max.botapi.exceptions.TransportClientException;

import java.io.File;
import java.util.concurrent.Future;

class FileUploadExec implements UploadExec {
    private final File file;
    private final String url;

    FileUploadExec(String url, File file) {
        this.url = url;
        this.file = file;
    }

    @Override
    public Future<ClientResponse> newCall(MaxTransportClient transportClient, String token) throws ClientException,
            InterruptedException {

        try {
            return transportClient.post(url, token, file);
        } catch (TransportClientException e) {
            throw new ClientException(e);
        }
    }
}
