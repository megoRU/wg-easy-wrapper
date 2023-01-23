package org.megoru.impl;

import com.google.gson.*;
import okhttp3.HttpUrl;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.megoru.entity.ErrorResponse;
import org.megoru.entity.ErrorResponseToMany;
import org.megoru.entity.api.*;
import org.megoru.io.DefaultResponseTransformer;
import org.megoru.io.ResponseTransformer;
import org.megoru.io.UnsuccessfulHttpException;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;

public class WgEasyAPIImpl implements WgEasyAPI {

    private final HttpUrl baseUrl;

    private final Gson gson;
    private CloseableHttpClient httpClient = HttpClients.createDefault();
    private final String password;
    private final boolean devMode;

    protected WgEasyAPIImpl(String password, String domain, boolean devMode) {
        this.devMode = devMode;
        this.password = password;

        baseUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(domain)
                .build();

        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            @Override
            public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                String replace = json.getAsJsonPrimitive().getAsString().replaceAll(".[0-9]+Z", "");
                TemporalAccessor parse = formatter.parse(replace);
                return LocalDateTime.from(parse);
            }

        }).setPrettyPrinting().create();

        setSession();
    }

    protected WgEasyAPIImpl(String password, String ip, int port, boolean devMode) {
        this.devMode = devMode;
        this.password = password;

        baseUrl = new HttpUrl.Builder()
                .scheme("http")
                .host(ip)
                .port(port)
                .build();

        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            @Override
            public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                String replace = json.getAsJsonPrimitive().getAsString().replaceAll(".[0-9]+Z", "");
                TemporalAccessor parse = formatter.parse(replace);
                return LocalDateTime.from(parse);
            }

        }).setPrettyPrinting().create();

        setSession();
    }

    @Override
    public File getQRCode(String userId, String fileName) throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .addPathSegment("qrcode.svg")
                .build();

        HttpGet request = new HttpGet(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        return execute(request, fileName + ".svg");
    }

    @Override
    public File getConfig(String userId, String fileName) throws UnsuccessfulHttpException {
        //https://vpn.megoru.ru/api/wireguard/client/83e7877e-9eea-4695-823e-b729cddb5d8c/configuration
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .addPathSegment("configuration")
                .build();

        HttpGet request = new HttpGet(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        return execute(request, fileName + ".conf");
    }

    @Override
    public Create createClient(String name) throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .build();

        JSONObject json = new JSONObject();

        try {
            json.put("name", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return post(url, json, new DefaultResponseTransformer<>(Create.class, gson));
    }

    @Override
    public Status updateClientAddress(String userId, String address) throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .addPathSegment("address")
                .build();

        JSONObject json = new JSONObject();


        try {
            json.put("address ", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return put(url, json, new DefaultResponseTransformer<>(Status.class, gson));
    }

    @Override
    public Status disableClient(String userId) throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .addPathSegment("disable")
                .build();

        JSONObject json = new JSONObject();

        return post(url, json, new DefaultResponseTransformer<>(Status.class, gson));
    }

    @Override
    public Status enableClient(String userId) throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .addPathSegment("enable")
                .build();

        JSONObject json = new JSONObject();

        return post(url, json, new DefaultResponseTransformer<>(Status.class, gson));
    }

    @Override
    public Status deleteClient(String userId) throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .build();

        return delete(url, new DefaultResponseTransformer<>(Status.class, gson));
    }

    //TODO: Если без json можно сломать веб UI или без name
    @Override
    public Status renameClient(String userId, String name) throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .addPathSegment("name")
                .build();

        JSONObject json = new JSONObject();

        try {
            json.put("name", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return put(url, json, new DefaultResponseTransformer<>(Status.class, gson));
    }

    @Override
    @Nullable
    public Client getClientByName(String name) throws UnsuccessfulHttpException, IllegalStateException {
        Client[] clients = Arrays.stream(getClients())
                .filter(c -> c.getName().equals(name))
                .toArray(Client[]::new);

        if (clients.length > 1)
            throw new IllegalStateException("Clients must be 1. Value: " + clients.length);

        if (clients.length == 0)
            return null;

        return clients[0];
    }

    @Override
    public @Nullable Client getClientById(String userId) throws UnsuccessfulHttpException, NullPointerException {
        Client[] clients = Arrays.stream(getClients())
                .filter(c -> c.getId().equals(userId))
                .toArray(Client[]::new);

        if (clients.length == 0)
            throw new NullPointerException("Client not found");

        return clients[0];
    }

    @Override
    public Client[] getClients() throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .build();

        return get(url, new DefaultResponseTransformer<>(Client[].class, gson));
    }

    @Override
    public Session getSession() throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("session")
                .addQueryParameter("password", password)
                .build();

        return get(url, new DefaultResponseTransformer<>(Session.class, gson));
    }

    @Override
    public void setSession() {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("session")
                .build();

        JSONObject json = new JSONObject();

        try {
            json.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HttpPost request = new HttpPost(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        HttpEntity stringEntity = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);

        execute(request);
    }

    private void execute(HttpRequestBase request) {
        try {
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse response = httpClient.execute(request, context);
            try (response) {
                CookieStore cookieStore = context.getCookieStore();
                Cookie cookie = cookieStore.getCookies().get(0);
                setCookie(cookie);
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private <E> E get(HttpUrl url, ResponseTransformer<E> responseTransformer) throws UnsuccessfulHttpException {
        HttpGet request = new HttpGet(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        return execute(request, responseTransformer);
    }

    private <E> E post(HttpUrl url, JSONObject jsonBody, ResponseTransformer<E> responseTransformer) throws UnsuccessfulHttpException {
        HttpPost request = new HttpPost(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpEntity stringEntity = new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);
        return execute(request, responseTransformer);
    }

    private <E> E delete(HttpUrl url, ResponseTransformer<E> responseTransformer) throws UnsuccessfulHttpException {
        HttpDelete request = new HttpDelete(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        return execute(request, responseTransformer);
    }

    private <E> E put(HttpUrl url, JSONObject jsonBody, ResponseTransformer<E> responseTransformer) throws UnsuccessfulHttpException {
        HttpPut request = new HttpPut(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpEntity stringEntity = new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);

        return execute(request, responseTransformer);
    }

    private void setCookie(Cookie cookies) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(cookies);
        httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
    }

    private File writeToFile(String text, String fileName) {
        try {
            int read;
            File file = new File(fileName);
            InputStream inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            while ((read = inputStream.read()) != -1) {
                fileOutputStream.write(read);
            }
            fileOutputStream.flush();
            fileOutputStream.close();

            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File execute(HttpRequestBase request, String fileName) throws UnsuccessfulHttpException {
        try {
            CloseableHttpResponse response = httpClient.execute(request);

            HttpEntity entity = response.getEntity();
            String body = EntityUtils.toString(entity);

            if (response.getStatusLine().getStatusCode() == 200) {
                return writeToFile(body, fileName);
            }

            throw new UnsuccessfulHttpException(response.getStatusLine().getStatusCode(), "Client Not Found");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private <E> E execute(HttpRequestBase request, ResponseTransformer<E> responseTransformer) throws UnsuccessfulHttpException {
        try {
            CloseableHttpResponse response = httpClient.execute(request);

            HttpEntity entity = response.getEntity();
            String body = "{}";
            if (entity != null) {
                 body = EntityUtils.toString(entity);
            }

            if (devMode) {
                String status = String.format(
                        "StatusCode: %s Reason: %s",
                        response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase());
                System.out.println(status);

                JsonElement jsonElement = JsonParser.parseString(body);
                String prettyJsonString = gson.toJson(jsonElement);
                System.out.println(prettyJsonString);
            }

            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                case 204: {
                    return responseTransformer.transform(body);
                }
                case 401:
                case 403: {
                    ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                    throw new UnsuccessfulHttpException(response.getStatusLine().getStatusCode(), result.getError());
                }
                case 404: {
                    ErrorNotFound errorNotFound = gson.fromJson(body, ErrorNotFound.class);
                    throw new UnsuccessfulHttpException(response.getStatusLine().getStatusCode(), errorNotFound.getError());
                }
                case 429: {
                    ErrorResponseToMany result = gson.fromJson(body, ErrorResponseToMany.class);
                    throw new UnsuccessfulHttpException(result.getStatusCode(), result.getMessage());
                }
                case 502: {
                    body = "{\n" +
                            "  \"error\": {\n" +
                            "    \"code\": 502,\n" +
                            "    \"message\": \"Bad Gateway\"\n" +
                            "  }\n" +
                            "}";
                    ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                    throw new UnsuccessfulHttpException(response.getStatusLine().getStatusCode(), result.getError());
                }
                default:
                    ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                    throw new UnsuccessfulHttpException(response.getStatusLine().getStatusCode(), result.getError());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
