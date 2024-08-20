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
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.megoru.entity.ErrorResponse;
import org.megoru.entity.api.Client;
import org.megoru.entity.api.Session;
import org.megoru.entity.api.Status;
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

public class WgEasyAPIImpl implements WgEasyAPI {

    private final HttpUrl baseUrl;

    private final Gson gson;

    private BasicCookieStore cookieStore = new BasicCookieStore();
    private final String password;
    private final boolean devMode;

    protected WgEasyAPIImpl(String password, String host, boolean devMode) {
        this.devMode = devMode;
        this.password = password;
        baseUrl = HttpUrl.get(host);

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
    public InputStream getConfig(String userId) {
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

        return executeInputStream(request);
    }

    @Override
    public File getConfig(String userId, String fileName) {
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
    public Status createClient(String name) throws UnsuccessfulHttpException {
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

        return post(url, json, new DefaultResponseTransformer<>(Status.class, gson));
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
    public Client getClientByName(String name) throws IllegalStateException, UnsuccessfulHttpException {
        Client[] clients = Arrays.stream(getClients())
                .filter(c -> c.getName().equals(name))
                .toArray(Client[]::new);

        if (clients.length > 1)
            throw new IllegalStateException("Clients must be 1. Value: " + clients.length);

        //Не менять ибо нарушаем логику.
        if (clients.length == 0)
            return null;

        return clients[0];
    }

    @Override
    public @Nullable Client getClientById(String userId) throws NullPointerException, UnsuccessfulHttpException {
        Client[] clients = Arrays.stream(getClients())
                .filter(c -> c.getId().equals(userId))
                .toArray(Client[]::new);

        if (clients.length == 0)
            throw new NullPointerException("ClientId not found: " + userId);

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

    private synchronized void setCookie(Cookie cookies) {
        cookieStore = new BasicCookieStore();
        cookieStore.addCookie(cookies);
    }

    private InputStream getInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
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
            CloseableHttpClient httpClient = HttpClients
                    .custom()
                    .setConnectionReuseStrategy(((requests, response, contexts) -> false))
                    .setDefaultCookieStore(cookieStore)
                    .useSystemProperties()
                    .build();

            CloseableHttpResponse response = httpClient.execute(request, context);
            try (response) {
                CookieStore cookieStore = context.getCookieStore();
                List<Cookie> cookie = cookieStore.getCookies();

                if (devMode) {
                    System.out.println("cookie: " + context.getResponse().getVersion());
                    System.out.println(Arrays.toString(context.getResponse().getHeaders()));
                }

                if (cookie.isEmpty()) throw new RuntimeException("Cookie is null");
                else setCookie(cookie.get(0));
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private InputStream executeInputStream(ClassicHttpRequest request) {
        CloseableHttpClient httpClient = HttpClients
                .custom()
                .setConnectionReuseStrategy(((requests, response, context) -> false))
                .setDefaultCookieStore(cookieStore)
                .useSystemProperties()
                .build();

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            try {
                int statusCode = response.getCode();
                HttpEntity entity = response.getEntity();
                String body = entity != null ? EntityUtils.toString(entity) : null;
                if (body == null) body = "{}";

                logResponse(response, "{}");

                if (statusCode == 200) {
                    return getInputStream(body);
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException();
    }

    private File execute(ClassicHttpRequest request, String fileName, FileExtension fileExtension) {
        CloseableHttpClient httpClient = HttpClients
                .custom()
                .setConnectionReuseStrategy(((requests, response, context) -> false))
                .setDefaultCookieStore(cookieStore)
                .useSystemProperties()
                .build();

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            try {
                int statusCode = response.getCode();
                HttpEntity entity = response.getEntity();
                String body = entity != null ? EntityUtils.toString(entity) : null;
                if (body == null) body = "{}";

                logResponse(response, "{}");

                if (statusCode == 200 && fileExtension.equals(FileExtension.QR_CODE)) {
                    File file = writeToFile(body, fileName);
                    return svgToPng(file, fileName);
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException();
    }

    @Nullable
    private <E> E execute(ClassicHttpRequest request, ResponseTransformer<E> responseTransformer) throws UnsuccessfulHttpException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                String body = getBodyFromEntity(response.getEntity());

                logResponse(response, body);

                return handleResponse(statusCode, body, responseTransformer);
            } catch (ParseException | IOException e) {
                throw new RuntimeException("Failed to execute HTTP request", e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to close the HTTP client", e);
        }
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .setConnectionReuseStrategy(((requests, response, context) -> false))
                .setDefaultCookieStore(cookieStore)
                .useSystemProperties()
                .build();
    }

    private String getBodyFromEntity(HttpEntity entity) throws IOException, ParseException {
        String body = entity != null ? EntityUtils.toString(entity) : "{}";
        return body != null ? body : "{}";
    }

    private <E> E handleResponse(int statusCode, String body, ResponseTransformer<E> transformer) throws UnsuccessfulHttpException {
        switch (statusCode) {
            case 200:
            case 204:
                return transformOrUseDefaultBody(body, transformer, statusCode);
            case 401:
            case 403:
            case 404:
            case 429:
                ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
                throw new UnsuccessfulHttpException(statusCode, errorResponse.getError());
            case 502:
                body = createDefault502Body();
                // Fall through to default case since the handling is the same.
            default:
                ErrorResponse defaultErrorResponse = gson.fromJson(body, ErrorResponse.class);
                throw new UnsuccessfulHttpException(statusCode, defaultErrorResponse.getError());
        }
    }

    private <E> E transformOrUseDefaultBody(String body, ResponseTransformer<E> transformer, int statusCode) {
        if (body.equals("{}")) {
            body = "{ \"status\": \"" + statusCode + "\" }";
        }
        return transformer.transform(body);
    }

    private String createDefault502Body() {
        return "{ \"error\": { \"code\": 502, \"message\": \"Bad Gateway\" } }";
    }

    private void logResponse(ClassicHttpResponse response, String body) {
        if (!devMode) {
            return;
        }

        System.out.println("Response: " + response.getVersion());
        System.out.println(Arrays.toString(response.getHeaders()));

        System.out.println("body " + body);

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