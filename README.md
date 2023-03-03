## WG-EASY-WRAPPER

An API wrapper for [wg-easy](https://github.com/WeeJeWel/wg-easy/) written in Java by @megoRU

### Maven

https://jitpack.io/#megoRU/wg-easy-wrapper

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.megoRU</groupId>
    <artifactId>wg-easy-wrapper</artifactId>
    <version>v1.5</version>
</dependency>
```

## Examples

### Get all Clients (peers)

```java
public class Main {
    public static void main(String[] args) {
        WgEasyAPI api = new WgEasyAPI.Builder()
                .password("password")
                .domain("vpn.megoru.ru")
                .build();
        try {
            Clients[] client = api.getClients();

            for (Clients client : client) {
                System.out.println(client.getId()); //139987fc-266a-45bb-b3c4-3e1d8d2e180c
                                                    // ...
            }
        } catch (UnsuccessfulHttpException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

### Disable Client (peer)

```java
public class Main {
    public static void main(String[] args) {
        WgEasyAPI api = new WgEasyAPI.Builder()
                .password("password")
                .domain("vpn.megoru.ru")
                .build();
        try {
            Status status = api.disableClient("139987fc-266a-45bb-b3c4-3e1d8d2e180c");
        } catch (UnsuccessfulHttpException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

### Create Client (peer)

```java
public class Main {
    public static void main(String[] args) {
        WgEasyAPI api = new WgEasyAPI.Builder()
                .password("password")
                .domain("vpn.megoru.ru")
                .build();
        try {
            Create create = api.createClient("mego");
            System.out.println(create.getCreatedAt()); //2023-01-12T18:20:12
        } catch (UnsuccessfulHttpException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

## Dependencies

1. [Gson](https://github.com/google/gson)
2. [Apache HttpClient](https://github.com/apache/httpcomponents-client)
3. [JSON-java](https://github.com/stleary/JSON-java)
4. [okhttp](https://github.com/square/okhttp)

## Links

* [Contact me](https://megoru.ru)