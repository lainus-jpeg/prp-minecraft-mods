
package com.prp.gamingsecuritylab.client;

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
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

public class SessionTestClient implements ClientModInitializer {

    private static final Path TEST_FILE = Path.of(
        System.getProperty("user.home"),
        "PRP-Lab",
        "mock-session.txt"
    );

    private static final HttpClient HTTP_CLIENT =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void onInitializeClient() {

        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, buildContext) -> {

                dispatcher.register(
                    ClientCommands.literal("securitylab")

                        .then(
                            ClientCommands.literal("sessiontest")
                                .executes(context -> {
                                    try {
                                        String fingerprint =
                                            getMockTokenFingerprint();

                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] MOCK session " +
                                                "file accessed successfully!"
                                            )
                                        );

                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] Fingerprint: " +
                                                fingerprint
                                            )
                                        );

                                        System.out.println(
                                            "[PRP SESSION TEST] File read " +
                                            "successful. Fingerprint: " +
                                            fingerprint
                                        );

                                        return 1;

                                    } catch (Exception e) {
                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] Session test " +
                                                "FAILED: " +
                                                e.getMessage()
                                            )
                                        );
                                        return 0;
                                    }
                                })
                        )

                        .then(
                            ClientCommands.literal("sessiontransfer")
                                .executes(context -> {

                                    String webhookUrl =
                                        System.getenv(
                                            "PRP_DISCORD_WEBHOOK"
                                        );

                                    if (webhookUrl == null ||
                                        webhookUrl.isBlank()) {

                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] Webhook " +
                                                "environment variable missing."
                                            )
                                        );
                                        return 0;
                                    }

                                    if (!webhookUrl.startsWith(
                                        "https://discord.com/api/webhooks/"
                                    )) {
                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] Invalid " +
                                                "Discord webhook URL."
                                            )
                                        );
                                        return 0;
                                    }

                                    try {
                                        String fingerprint =
                                            getMockTokenFingerprint();

                                        String message =
                                            "PRP Session Lab - " +
                                            "Mock session fingerprint: " +
                                            fingerprint;

                                        String json =
                                            "{\"content\":\"" +
                                            message + "\"}";

                                        HttpRequest request =
                                            HttpRequest.newBuilder()
                                                .uri(URI.create(webhookUrl))
                                                .timeout(
                                                    Duration.ofSeconds(15)
                                                )
                                                .header(
                                                    "Content-Type",
                                                    "application/json"
                                                )
                                                .POST(
                                                    HttpRequest.BodyPublishers
                                                        .ofString(json)
                                                )
                                                .build();

                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] Sending " +
                                                "mock-session fingerprint..."
                                            )
                                        );

                                        HTTP_CLIENT.sendAsync(
                                            request,
                                            HttpResponse.BodyHandlers
                                                .ofString()
                                        ).whenComplete(
                                            (response, error) -> {
                                                if (error != null) {
                                                    System.out.println(
                                                        "[PRP SESSION TEST] " +
                                                        "Transfer failed: " +
                                                        error.getClass()
                                                            .getSimpleName()
                                                    );
                                                } else {
                                                    System.out.println(
                                                        "[PRP SESSION TEST] " +
                                                        "Discord HTTP status: " +
                                                        response.statusCode()
                                                    );
                                                }
                                            }
                                        );

                                        return 1;

                                    } catch (Exception e) {
                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] Transfer " +
                                                "FAILED: " +
                                                e.getMessage()
                                            )
                                        );
                                        return 0;
                                    }
                                })
                        )
                );
            }
        );
    }

    private static String getMockTokenFingerprint()
        throws Exception {

        Files.createDirectories(TEST_FILE.getParent());

        if (Files.notExists(TEST_FILE)) {
            String mockToken =
                "PRP-DEMO-" + UUID.randomUUID();

            Files.writeString(
                TEST_FILE,
                mockToken,
                StandardCharsets.UTF_8
            );
        }

        String mockToken = Files.readString(
            TEST_FILE,
            StandardCharsets.UTF_8
        ).trim();

        if (!mockToken.startsWith("PRP-DEMO-")) {
            throw new IllegalArgumentException(
                "File is not a PRP demo token."
            );
        }

        byte[] hash = MessageDigest
            .getInstance("SHA-256")
            .digest(
                mockToken.getBytes(StandardCharsets.UTF_8)
            );

        return HexFormat.of()
            .formatHex(hash)
            .substring(0, 12);
    }
}
