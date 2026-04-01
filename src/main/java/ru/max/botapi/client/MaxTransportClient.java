/*
 * ------------------------------------------------------------------------
 * Max chat Bot API
 * ------------------------------------------------------------------------
 * Copyright (C) 2025 COMMUNICATION PLATFORM LLC
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package ru.max.botapi.client;

import org.jetbrains.annotations.Nullable;
import ru.max.botapi.exceptions.TransportClientException;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Future;

public interface MaxTransportClient extends Closeable {
    Future<ClientResponse> get(String url, String token) throws TransportClientException;

    Future<ClientResponse> post(String url, String token,  @Nullable byte[] body) throws TransportClientException;

    Future<ClientResponse> post(String url, String token, File file) throws TransportClientException, InterruptedException;

    Future<ClientResponse> post(String url, String token, String filename, InputStream inputStream) throws TransportClientException;

    Future<ClientResponse> put(String url, String token, @Nullable byte[] requestBody) throws TransportClientException;

    Future<ClientResponse> delete(String url, String token) throws TransportClientException;

    Future<ClientResponse> patch(String url, String token, @Nullable byte[] requestBody) throws TransportClientException;

    enum Method {
        GET, POST, PUT, HEAD, DELETE, PATCH, OPTIONS
    }
}
