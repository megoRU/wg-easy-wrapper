package org.megoru.impl;

import com.google.gson.*;
import okhttp3.*;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class WgEasyAPIImpl implements WgEasyAPI {

    private final HttpUrl baseUrl;

    private final Gson gson;

    private final String password;
    private final boolean devMode;
    private String cookie = "default";

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
    public CompletionStage<File> getQRCode(String userId, String fileName) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .addPathSegment("qrcode.svg")
                .build();

        HttpGet request = new HttpGet(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        Request.Builder req = new Request.Builder()
                .get()
                .url(url)
                .addHeader("Content-Type", "application/json");
        if (cookie != null) {
            req.addHeader("Cookie", cookie);
        }
        return executeFile(req.build(), fileName + ".svg", FileExtension.QR_CODE);
    }

    @Override
    public CompletionStage<File> getConfig(String userId, String fileName) {
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

        Request.Builder req = new Request.Builder()
                .get()
                .url(url)
                .addHeader("Content-Type", "application/json");

        if (cookie != null) {
            req.addHeader("Cookie", cookie);
        }
        return executeFile(req.build(), fileName + ".conf", FileExtension.CONFIG);
    }

    @Override
    public CompletionStage<Create> createClient(String name) {
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
    public CompletionStage<Status> updateClientAddress(String userId, String address) {
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
    public CompletionStage<Status> disableClient(String userId) {
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
    public CompletionStage<Status> enableClient(String userId) {
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
    public CompletionStage<Status> deleteClient(String userId) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .addPathSegment(userId)
                .build();

        return delete(url, Status.class);
    }

    //TODO: Если без json можно сломать веб UI или без name
    @Override
    public CompletionStage<Status> renameClient(String userId, String name) {
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
        return put(url, json, Status.class);
    }

    @Override
    @Nullable
    public Client getClientByName(String name) throws IllegalStateException {
        Client[] clients = Arrays.stream(getClients().toCompletableFuture().join())
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
    public @Nullable Client getClientById(String userId) throws NullPointerException {
        Client[] clients = Arrays.stream(getClients().toCompletableFuture().join())
                .filter(c -> c.getId().equals(userId))
                .toArray(Client[]::new);

        if (clients.length == 0)
            throw new NullPointerException("ClientId not found: " + userId);

        return clients[0];
    }

    @Override
    public CompletionStage<Client[]> getClients() {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("wireguard")
                .addPathSegment("client")
                .build();

        return get(url, Client[].class);
    }

    @Override
    public CompletionStage<Void> setSession() {
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

        return post(url, json, Void.class);
    }

    @Override
    public CompletionStage<Session> getSession() {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("session")
                .build();

        return get(url, Session.class);
    }

    //PUT
    private <E> CompletionStage<E> put(HttpUrl url, JSONObject jsonBody, Class<E> aClass) {
        return put(url, jsonBody, new DefaultResponseTransformer<>(aClass, gson));
    }

    private <E> CompletionStage<E> put(HttpUrl url, JSONObject jsonBody, ResponseTransformer<E> responseTransformer) {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, jsonBody.toString());

        Request.Builder req = new Request.Builder()
                .put(body)
                .url(url)
                .addHeader("Content-Type", "application/json");
        if (cookie != null) {
            req.addHeader("Cookie", cookie);
        }
        return execute(req.build(), responseTransformer);
    }

    private <E> CompletionStage<E> get(HttpUrl url, Class<E> aClass) {
        return get(url, new DefaultResponseTransformer<>(aClass, gson));
    }

    private <E> CompletionStage<E> get(HttpUrl url, ResponseTransformer<E> responseTransformer) {
        Request.Builder req = new Request.Builder()
                .get()
                .url(url)
                .addHeader("Content-Type", "application/json");
        if (cookie != null) {
            req.addHeader("Cookie", cookie);
        }
        return execute(req.build(), responseTransformer);
    }

    private <E> CompletionStage<E> delete(HttpUrl url, Class<E> aClass) {
        return delete(url, new DefaultResponseTransformer<>(aClass, gson));
    }

    private <E> CompletionStage<E> delete(HttpUrl url, ResponseTransformer<E> responseTransformer) {
        Request.Builder req = new Request.Builder()
                .delete()
                .url(url)
                .addHeader("Content-Type", "application/json");
        String encodedPath = url.encodedPath();
        if (!encodedPath.equals("/api/session")) {
            req.addHeader("Cookie", cookie);
        }
        return execute(req.build(), responseTransformer);
    }

    //POST
    private <E> CompletionStage<E> post(HttpUrl url, JSONObject jsonBody, Class<E> aClass) {
        return post(url, jsonBody, new DefaultResponseTransformer<>(aClass, gson));
    }

    private <E> CompletionStage<E> post(HttpUrl url, JSONObject jsonBody, ResponseTransformer<E> responseTransformer) {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, jsonBody.toString());
        Request.Builder req = new Request.Builder()
                .post(body)
                .url(url)
                .addHeader("Content-Type", "application/json");
        String encodedPath = url.encodedPath();
        if (!encodedPath.equals("/api/session")) {
            req.addHeader("Cookie", cookie);
        }
        return execute(req.build(), responseTransformer);
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

    private CompletionStage<File> executeFile(Request request, String fileName, FileExtension fileExtension) {
        final CompletableFuture<File> future = new CompletableFuture<>();
        final OkHttpClient httpClient = new OkHttpClient();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody.string();
            int statusCode = response.code();

            if (statusCode == 200 && fileExtension.equals(FileExtension.CONFIG)) {
                File file = writeToFile(body, fileName);
                future.complete(file);
                return future;
            } else if (statusCode == 200 && fileExtension.equals(FileExtension.QR_CODE)) {
                File file = writeToFile(body, fileName);
                File png = svgToPng(file, fileName);
                future.complete(png);
                return future;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return future;
    }

    private <E> CompletionStage<E> execute(Request request, ResponseTransformer<E> responseTransformer) {
        final CompletableFuture<E> future = new CompletableFuture<>();
        final OkHttpClient httpClient = new OkHttpClient();

        try (Response response = httpClient.newCall(request).execute()) {
            int statusCode = response.code();
            String encodedPath = request.url().encodedPath();
            String method = request.method();

            if (encodedPath.equals("/api/session") && method.equals("POST")) {
                cookie = response.header("set-cookie");
            }

            ResponseBody responseBody = response.body();
            String body;
            if (responseBody != null) {
                body = responseBody.string();
                responseBody.close();
            } else {
                body = "{}";
            }

//            System.out.println(body);
//            System.out.println(cookie);
//            System.out.println(statusCode);
//            System.out.println(response);

            this.close(httpClient);

            switch (statusCode) {
                case 200:
                case 204: {
                    if (body.equals("{}")) {
                        body = "{\n" + "  \"status\": \"" + statusCode + "\"\n" + "}";
                    }
                    E transformed = responseTransformer.transform(body);
                    future.complete(transformed);
                    return future;
                }
                case 401:
                case 403: {
                    ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                    UnsuccessfulHttpException e = new UnsuccessfulHttpException(statusCode, result.getError());
                    future.completeExceptionally(e);
                    return future;
                }
                case 404: {
                    ErrorNotFound errorNotFound = gson.fromJson(body, ErrorNotFound.class);
                    UnsuccessfulHttpException e = new UnsuccessfulHttpException(statusCode, errorNotFound.getError());
                    future.completeExceptionally(e);
                    return future;
                }
                case 429: {
                    ErrorResponseToMany result = gson.fromJson(body, ErrorResponseToMany.class);
                    UnsuccessfulHttpException e = new UnsuccessfulHttpException(statusCode, result.getMessage());
                    future.completeExceptionally(e);
                    return future;
                }
                case 502: {
                    body = "{\n" +
                            "  \"error\": {\n" +
                            "    \"code\": 502,\n" +
                            "    \"message\": \"Bad Gateway\"\n" +
                            "  }\n" +
                            "}";
                    ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                    UnsuccessfulHttpException e = new UnsuccessfulHttpException(statusCode, result.getError());
                    future.completeExceptionally(e);
                    return future;
                }
                default:
                    ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                    UnsuccessfulHttpException e = new UnsuccessfulHttpException(statusCode, result.getError());
                    future.completeExceptionally(e);
                    return future;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return future;
    }

    public void close(OkHttpClient httpClient) {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    private void logResponse(ClassicHttpResponse response, String body) {
        if (!devMode) {
            return;
        }

        System.out.println("Response: " + response.getVersion());
        System.out.println(Arrays.toString(response.getHeaders()));

        String status = String.format(
                "StatusCode: %s Reason: %s",
                response.getCode(),
                response.getReasonPhrase());
        System.out.println(status);
        JsonElement jsonElement = JsonParser.parseString(body);
        String prettyJsonString = gson.toJson(jsonElement);
        System.out.println(prettyJsonString);
    }

    private enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }
}