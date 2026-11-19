
package com.prp.gamingsecuritylab.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

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
                );

            }
        );
    }
}
