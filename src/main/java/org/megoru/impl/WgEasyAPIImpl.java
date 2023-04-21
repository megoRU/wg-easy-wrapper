package org.megoru.impl;

import com.google.gson.*;
import okhttp3.HttpUrl;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.megoru.entity.ErrorResponse;
import org.megoru.entity.ErrorResponseToMany;
import org.megoru.entity.api.*;
import org.megoru.io.DefaultResponseTransformer;
import org.megoru.io.ResponseTransformer;
import org.megoru.io.UnsuccessfulHttpException;

import javax.imageio.ImageIO;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    public File getQRCode(String userId, String fileName) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .addPathSegment("qrcode.svg")
                .build();

        HttpGet request = new HttpGet(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        return execute(request, fileName + ".svg", FileExtension.QR_CODE);
    }

    @Override
    public File getConfig(String userId, String fileName)  {
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

        return execute(request, fileName + ".conf", FileExtension.CONFIG);
    }

    @Override
    public Create createClient(String name) {
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
    public Status updateClientAddress(String userId, String address)   {
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
    public Status disableClient(String userId) {
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
    public Status enableClient(String userId)   {
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
    public Status deleteClient(String userId)   {
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
    public Status renameClient(String userId, String name) {
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
    public Client getClientByName(String name) throws IllegalStateException, NullPointerException {
        Client[] clients = Arrays.stream(getClients())
                .filter(c -> c.getName().equals(name))
                .toArray(Client[]::new);

        if (clients.length > 1)
            throw new IllegalStateException("Clients must be 1. Value: " + clients.length);

        if (clients.length == 0)
            throw new NullPointerException("Client not found: " + name);

        return clients[0];
    }

    @Override
    public @Nullable Client getClientById(String userId) throws NullPointerException {
        Client[] clients = Arrays.stream(getClients())
                .filter(c -> c.getId().equals(userId))
                .toArray(Client[]::new);

        if (clients.length == 0)
            throw new NullPointerException("ClientId not found: " + userId);

        return clients[0];
    }

    @Override
    public Client[] getClients()   {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .build();

        return get(url, new DefaultResponseTransformer<>(Client[].class, gson));
    }

    @Override
    public Session getSession() {
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

    private <E> E get(HttpUrl url, ResponseTransformer<E> responseTransformer) {
        HttpGet request = new HttpGet(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        return execute(request, responseTransformer);
    }

    private <E> E post(HttpUrl url, JSONObject jsonBody, ResponseTransformer<E> responseTransformer) {
        HttpPost request = new HttpPost(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpEntity stringEntity = new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);
        return execute(request, responseTransformer);
    }

    private <E> E delete(HttpUrl url, ResponseTransformer<E> responseTransformer) {
        HttpDelete request = new HttpDelete(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        return execute(request, responseTransformer);
    }

    private <E> E put(HttpUrl url, JSONObject jsonBody, ResponseTransformer<E> responseTransformer) {
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

    private File svgToPng(File file, String fileName) {
        Transcoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) 512);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) 512);
        try (FileInputStream inputStream = new FileInputStream(file)) {
            TranscoderInput input = new TranscoderInput(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outputStream);
            t.transcode(input, output);
            outputStream.flush();
            outputStream.close();
            byte[] imgData = outputStream.toByteArray();
            int lastIndexOf = fileName.lastIndexOf(".");
            File outputfile = new File(fileName.substring(0, lastIndexOf) + ".png");
            ImageIO.write(ImageIO.read(new ByteArrayInputStream(imgData)), "png", outputfile);
            return outputfile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    private void execute(ClassicHttpRequest request) {
        try {
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse response = httpClient.execute(request, context);
            try (response) {
                CookieStore cookieStore = context.getCookieStore();
                List<Cookie> cookie = cookieStore.getCookies();

                if (cookie.isEmpty()) throw new RuntimeException("Cookie is null");
                else setCookie(cookie.get(0));
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private File execute(ClassicHttpRequest request, String fileName, FileExtension fileExtension) {
        try {
            return httpClient.execute(request, new BasicHttpContext(), httpResponse -> {
                HttpEntity entity = httpResponse.getEntity();
                String body = EntityUtils.toString(entity);
                if (httpResponse.getCode() == 200 && fileExtension.equals(FileExtension.CONFIG)) {
                    return writeToFile(body, fileName);
                } else if (httpResponse.getCode() == 200 && fileExtension.equals(FileExtension.QR_CODE)) {
                    File file = writeToFile(body, fileName);
                    return svgToPng(file, fileName);
                }
                try {
                    throw new UnsuccessfulHttpException(httpResponse.getCode(), "Client Not Found");
                } catch (UnsuccessfulHttpException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private <E> E execute(ClassicHttpRequest request, ResponseTransformer<E> responseTransformer) {
        try {
            return httpClient.execute(request, new BasicHttpContext(), httpResponse -> {
                HttpEntity entity = httpResponse.getEntity();
                String body = "{}";
                if (entity != null) {
                    try (InputStream inputStream = entity.getContent()) {
                        body = new String(inputStream.readAllBytes());
                    }
                }

                logResponse(httpResponse, body);

                try {
                    switch (httpResponse.getCode()) {
                        case 200:
                        case 204: {
                            if (body.equals("{}")) {
                                body = "{\n" + "  \"status\": \"" + httpResponse.getCode() + "\"\n" + "}";
                            }
                            return responseTransformer.transform(body);
                        }
                        case 401:
                        case 403: {
                            ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                            throw new UnsuccessfulHttpException(httpResponse.getCode(), result.getError());
                        }
                        case 404: {
                            ErrorNotFound errorNotFound = gson.fromJson(body, ErrorNotFound.class);
                            throw new UnsuccessfulHttpException(httpResponse.getCode(), errorNotFound.getError());
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
                            throw new UnsuccessfulHttpException(httpResponse.getCode(), result.getError());
                        }
                        default:
                            ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                            throw new UnsuccessfulHttpException(httpResponse.getCode(), result.getError());
                    }

                } catch (UnsuccessfulHttpException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void logResponse(ClassicHttpResponse response, String body) {
        if (!devMode) {
            return;
        }

        String status = String.format(
                "StatusCode: %s Reason: %s",
                response.getCode(),
                response.getReasonPhrase());
        System.out.println(status);
        JsonElement jsonElement = JsonParser.parseString(body);
        String prettyJsonString = gson.toJson(jsonElement);
        System.out.println(prettyJsonString);
    }
}