package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import java.util.*

object ConfigApiImplClient {

    internal val ellipses = FcText.literal("...")

    private val ellipsesWidth by lazy{
        MinecraftClient.getInstance().textRenderer.getWidth(ellipses)
    }

    fun ellipses(input: Text, maxWidth: Int): Text{
        return if (MinecraftClient.getInstance().textRenderer.getWidth(input) <= maxWidth)
            input
        else
            MinecraftClient.getInstance().textRenderer.trimToWidth(input.string,maxWidth - ellipsesWidth).trimEnd().lit().append(ellipses)
    }

    internal fun registerConfig(config: Config, baseConfig: Config){
        ClientConfigRegistry.registerConfig(config, baseConfig)
    }

    internal fun openScreen(scope: String) {
        ClientConfigRegistry.openScreen(scope)
    }

    internal fun handleForwardedUpdate(update: String, player: UUID, scope: String, summary: String) {
        ClientConfigRegistry.handleForwardedUpdate(update, player, scope, summary)
    }

    internal fun getPlayerPermissionLevel(): Int{
        val client = MinecraftClient.getInstance()
        if(client.server != null && client?.server?.isRemote != true) return 4 // single player game, they can change whatever they want
        var i = 0
        while(client.player?.hasPermissionLevel(i) == true){
            i++
        }
        return i
    }
}