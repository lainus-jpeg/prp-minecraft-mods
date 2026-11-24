
package com.prp.gamingsecuritylab.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class GamingSecurityLabClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, buildContext) -> {

                // Main command and file-access experiment
                dispatcher.register(
                    ClientCommands.literal("securitylab")

                        .executes(context -> {
                            context.getSource().sendFeedback(
                                Component.literal(
                                    "[Security Lab] PRP test environment is working!"
                                )
                            );
                            return 1;
                        })

                        .then(
                            ClientCommands.literal("filetest")
                                .executes(context -> {

                                    Path testFile = Path.of(
                                        "C:/Users/liand/PRP-Lab/test-data.txt"
                                    );

                                    try {
                                        String data = Files.readString(testFile);

                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] File access SUCCESSFUL! "
                                                + "Read " + data.length() + " characters."
                                            )
                                        );

                                    } catch (IOException | SecurityException e) {

                                        context.getSource().sendFeedback(
                                            Component.literal(
                                                "[Security Lab] File access FAILED: "
                                                + e.getClass().getSimpleName()
                                            )
                                        );
                                    }

                                    return 1;
                                })
                        )
                );

                // Experiment 2: Outbound webhook communication
                dispatcher.register(
                    ClientCommands.literal("securitylabwebhook")
                        .executes(context -> {

                            String webhookUrl =
                                System.getenv("PRP_DISCORD_WEBHOOK");

                            if (webhookUrl == null || webhookUrl.isBlank()) {
                                context.getSource().sendFeedback(
                                    Component.literal(
                                        "[Security Lab] Webhook not configured."
                                    )
                                );
                                return 0;
                            }

                            String json = """
                                {"content":"PRP Gaming Security Lab - Minecraft mod network test successful!"}
                                """;

                            CompletableFuture.runAsync(() -> {

                                try {
                                    HttpClient client = HttpClient.newBuilder()
                                        .connectTimeout(Duration.ofSeconds(10))
                                        .build();

                                    HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create(webhookUrl))
                                        .timeout(Duration.ofSeconds(15))
                                        .header("Content-Type", "application/json")
                                        .POST(
                                            HttpRequest.BodyPublishers.ofString(json)
                                        )
                                        .build();

                                    HttpResponse<String> response = client.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString()
                                    );

                                    System.out.println(
                                        "[Security Lab] Webhook HTTP status: "
                                        + response.statusCode()
                                    );

                                } catch (Exception e) {

                                    System.err.println(
                                        "[Security Lab] Webhook test FAILED: "
                                        + e.getClass().getSimpleName()
                                    );
                                }
                            });

                            context.getSource().sendFeedback(
                                Component.literal(
                                    "[Security Lab] Webhook test started."
                                )
                            );

                            return 1;
                        })
                );
            }
        );
    }
}
