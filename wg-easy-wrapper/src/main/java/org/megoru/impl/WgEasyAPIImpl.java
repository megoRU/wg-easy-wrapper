package org.megoru.impl;

import com.google.gson.*;
import okhttp3.HttpUrl;
import org.apache.http.Header;
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
import org.megoru.entity.api.Clients;
import org.megoru.entity.api.NoContent;
import org.megoru.entity.api.Session;
import org.megoru.io.DefaultResponseTransformer;
import org.megoru.io.ResponseTransformer;
import org.megoru.io.UnsuccessfulHttpException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.stream.Collectors;

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
    public NoContent disableClient(String userId) throws UnsuccessfulHttpException {
        return null;
    }

    @Override
    public NoContent enableClient(String userId) throws UnsuccessfulHttpException {
        return null;
    }

    @Override
    public NoContent deleteClient(String userId) throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .build();

        return delete(url, new DefaultResponseTransformer<>(NoContent.class, gson));
    }

    @Override
    public NoContent renameClient(String userId, String name) throws UnsuccessfulHttpException {




        return null;
    }

    @Override
    @Nullable
    public Clients getClientId(String name) throws UnsuccessfulHttpException, IllegalStateException {
        Clients[] clients = Arrays.stream(getClients())
                .filter(c -> c.getName().equals(name))
                .toArray(Clients[]::new);

        if (clients.length > 1)
            throw new IllegalStateException("Clients must be 1. Value: " + clients.length);

        if (clients.length == 0)
            return null;

        return clients[0];
    }

    @Override
    public Clients[] getClients() throws UnsuccessfulHttpException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .build();

        return get(url, new DefaultResponseTransformer<>(Clients[].class, gson));
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

    private void setCookie(Cookie cookies) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(cookies);
        httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
    }

    @Nullable
    private <E> E execute(HttpRequestBase request, ResponseTransformer<E> responseTransformer) throws UnsuccessfulHttpException {
        try {
            CloseableHttpResponse response = httpClient.execute(request);

            HttpEntity entity = response.getEntity();
            String body;

            if (entity != null) {
                body = EntityUtils.toString(entity);
            } else {
                body = "{\n" +
                        "  \"body\": \"No Content\"\n" +
                        "}";
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
                case 403:
                case 404: {
                    ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                    throw new UnsuccessfulHttpException(response.getStatusLine().getStatusCode(), result.getError());
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
