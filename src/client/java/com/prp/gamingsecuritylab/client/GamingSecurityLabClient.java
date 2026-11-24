
package com.prp.gamingsecuritylab.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class GamingSecurityLabClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, buildContext) -> {

                dispatcher.register(
                    ClientCommands.literal("securitylab")

                        .executes(context -> {
                            context.getSource().sendFeedback(
                                Component.literal(
                                    "Gaming Security Lab - PRP test environment is working!"
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
            }
        );
    }
}
