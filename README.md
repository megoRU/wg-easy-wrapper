## WG-EASY-WRAPPER

Java API wrapper for [wg-easy](https://github.com/wg-easy/wg-easy), created by [@megoRU](https://github.com/megoRU).

---

## 📦 Installation (Maven)

Add the JitPack repository and dependency:

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
    <version>v3.5.1</version>
</dependency>
```

---

## 🚀 Examples

### 👥 Get All Clients (Peers)

```java
WgEasyAPI api = new WgEasyAPI.Builder()
        .password("password")
        .host("http://222.222.222.222:55222")
        .build();

Clients[] clients = api.getClients();
for (Clients client : clients) {
    System.out.println(client.getId());
}
```

### ❌ Disable Client

```java
WgEasyAPI api = new WgEasyAPI.Builder()
        .password("password")
        .host("http://222.222.222.222:55222")
        .build();

Status status = api.disableClient("139987fc-266a-45bb-b3c4-3e1d8d2e180c");
```

### ➕ Create Client

```java
WgEasyAPI api = new WgEasyAPI.Builder()
        .password("password")
        .host("http://222.222.222.222:55222")
        .build();

Create create = api.createClient("mego");
System.out.println(create.getCreatedAt());
```

---

## 📚 Dependencies

* [Gson](https://github.com/google/gson)
* [Apache HttpClient](https://github.com/apache/httpcomponents-client)
* [JSON-java](https://github.com/stleary/JSON-java)
* [OkHttp](https://github.com/square/okhttp)

## LICENSE

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).

---

## 🔗 Links

* 🌐 [Contact me](https://megoru.ru)
