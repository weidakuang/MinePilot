package dev.mcai.companion.codex;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dev.mcai.companion.MinecraftAiCompanion;
import dev.mcai.companion.agent.AgentRuntime;
import net.minecraftforge.fml.loading.FMLPaths;

/** Stateless, authenticated, loopback-only Streamable HTTP MCP endpoint. */
public final class CodexMcpServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 25_766;
    public static final String ENDPOINT_PATH = "/mcp";
    public static final String TOKEN_FILE_NAME = "codex-mcp.token";
    private static final int MAX_REQUEST_BYTES = 256 * 1024;
    private static final Gson GSON = new Gson();

    private final HttpServer http;
    private final ExecutorService executor;
    private final CodexToolService tools;
    private final byte[] bearerToken;
    private final Path tokenFile;
    private final int port;

    private CodexMcpServer(
            HttpServer http,
            ExecutorService executor,
            CodexToolService tools,
            byte[] bearerToken,
            Path tokenFile,
            int port
    ) {
        this.http = http;
        this.executor = executor;
        this.tools = tools;
        this.bearerToken = bearerToken;
        this.tokenFile = tokenFile;
        this.port = port;
    }

    public static CodexMcpServer start(AgentRuntime runtime) throws IOException {
        int port = configuredPort();
        String token = newToken();
        Path tokenFile = FMLPaths.CONFIGDIR.get()
                .resolve("mcai-companion")
                .resolve(TOKEN_FILE_NAME);
        writeOwnerOnlyToken(tokenFile, token);

        ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "MinePilot-Codex-MCP");
            thread.setDaemon(true);
            return thread;
        });
        HttpServer http;
        try {
            http = HttpServer.create(new InetSocketAddress(
                    InetAddress.getByName("127.0.0.1"), port), 0);
        } catch (IOException failure) {
            executor.shutdownNow();
            Files.deleteIfExists(tokenFile);
            throw failure;
        }
        CodexMcpServer result = new CodexMcpServer(
                http,
                executor,
                new CodexToolService(runtime),
                token.getBytes(StandardCharsets.UTF_8),
                tokenFile,
                port
        );
        http.createContext(ENDPOINT_PATH, result::handle);
        http.setExecutor(executor);
        http.start();
        MinecraftAiCompanion.LOGGER.info(
                "MinePilot Codex MCP listening on http://127.0.0.1:{}{}; token stored in {}",
                port,
                ENDPOINT_PATH,
                tokenFile
        );
        return result;
    }

    public Path tokenFile() {
        return tokenFile;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!exchange.getRemoteAddress().getAddress().isLoopbackAddress()) {
                sendText(exchange, 403, "Loopback clients only");
                return;
            }
            if (!validHost(exchange.getRequestHeaders().getFirst("Host"))) {
                sendText(exchange, 403, "Invalid Host header");
                return;
            }
            if (!validOrigin(exchange.getRequestHeaders().getFirst("Origin"))) {
                sendText(exchange, 403, "Invalid Origin header");
                return;
            }
            if (!authorized(exchange.getRequestHeaders().getFirst("Authorization"))) {
                exchange.getResponseHeaders().set(
                        "WWW-Authenticate", "Bearer realm=\"MinePilot\"");
                sendText(exchange, 401, "Unauthorized");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                sendText(exchange, 405, "Only POST is supported");
                return;
            }

            JsonObject request;
            try {
                byte[] requestBytes = readBounded(exchange.getRequestBody());
                request = JsonParser.parseString(
                        new String(requestBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException oversizedOrUnreadable) {
                sendText(exchange, 413, "MCP request body is unavailable or too large");
                return;
            } catch (RuntimeException malformed) {
                sendJson(exchange, rpcError(null, -32700, "Parse error"));
                return;
            }
            JsonElement id = request.get("id");
            String method = string(request, "method");
            if (method == null || !"2.0".equals(string(request, "jsonrpc"))) {
                sendJson(exchange, rpcError(id, -32600, "Invalid Request"));
                return;
            }
            if (id == null || id.isJsonNull()) {
                tools.notification(method);
                exchange.sendResponseHeaders(202, -1);
                return;
            }

            JsonObject response;
            try {
                response = rpcResult(id, tools.dispatch(method, request));
            } catch (CodexToolService.RpcException failure) {
                response = rpcError(id, failure.code(), failure.getMessage());
            } catch (RuntimeException failure) {
                MinecraftAiCompanion.LOGGER.warn(
                        "MinePilot Codex MCP request failed: {}", method, failure);
                response = rpcError(id, -32603, "Internal error");
            }
            sendJson(exchange, response);
        }
    }

    private boolean validHost(String host) {
        return host != null && (host.equalsIgnoreCase("127.0.0.1:" + port)
                || host.equalsIgnoreCase("localhost:" + port));
    }

    private static boolean validOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return true;
        }
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            return "http".equalsIgnoreCase(uri.getScheme())
                    && ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host));
        } catch (RuntimeException malformed) {
            return false;
        }
    }

    private boolean authorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        byte[] supplied = authorization.substring("Bearer ".length())
                .getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(bearerToken, supplied);
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_REQUEST_BYTES + 1);
        if (bytes.length > MAX_REQUEST_BYTES) {
            throw new IOException("MCP request exceeds 256 KiB");
        }
        return bytes;
    }

    private static void sendJson(HttpExchange exchange, JsonObject response) throws IOException {
        byte[] body = GSON.toJson(response).getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
    }

    private static void sendText(HttpExchange exchange, int status, String text)
            throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private static JsonObject rpcResult(JsonElement id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id.deepCopy());
        response.add("result", result);
        return response;
    }

    private static JsonObject rpcError(JsonElement id, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id == null ? null : id.deepCopy());
        response.add("error", error);
        return response;
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static int configuredPort() {
        String raw = System.getenv().getOrDefault(
                "MINEPILOT_MCP_PORT", Integer.toString(DEFAULT_PORT));
        int port = Integer.parseInt(raw);
        if (port < 1_024 || port > 65_535) {
            throw new IllegalArgumentException("MINEPILOT_MCP_PORT must be in [1024, 65535]");
        }
        return port;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void writeOwnerOnlyToken(Path tokenFile, String token) throws IOException {
        Files.createDirectories(tokenFile.getParent());
        Path temporary = Files.createTempFile(tokenFile.getParent(), "codex-mcp-", ".tmp");
        try {
            Files.writeString(temporary, token + System.lineSeparator(), StandardCharsets.UTF_8);
            restrictToOwner(temporary);
            try {
                Files.move(temporary, tokenFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, tokenFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void restrictToOwner(Path file) throws IOException {
        PosixFileAttributeView posix = Files.getFileAttributeView(
                file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posix != null) {
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            );
            posix.setPermissions(permissions);
            return;
        }

        AclFileAttributeView acl = Files.getFileAttributeView(
                file, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (acl == null) {
            throw new IOException(
                    "The token filesystem cannot enforce owner-only permissions");
        }
        UserPrincipal owner = Files.getOwner(file, LinkOption.NOFOLLOW_LINKS);
        AclEntry ownerOnly = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(owner)
                .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                .build();
        acl.setAcl(List.of(ownerOnly));
    }

    @Override
    public void close() {
        http.stop(0);
        executor.shutdownNow();
        try {
            Files.deleteIfExists(tokenFile);
        } catch (IOException failure) {
            MinecraftAiCompanion.LOGGER.warn(
                    "Unable to remove stale MinePilot Codex MCP token file {}", tokenFile);
        }
    }
}
