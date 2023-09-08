package net.momirealms.customfishing.command.sub;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.IStringTooltip;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import net.momirealms.biomeapi.BiomeAPI;
import net.momirealms.customfishing.adventure.AdventureManagerImpl;
import net.momirealms.customfishing.api.CustomFishingPlugin;
import net.momirealms.customfishing.api.integration.SeasonInterface;
import net.momirealms.customfishing.api.manager.AdventureManager;
import net.momirealms.customfishing.api.mechanic.condition.FishingPreparation;
import net.momirealms.customfishing.api.mechanic.effect.Effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebugCommand {

    public static DebugCommand INSTANCE = new DebugCommand();

    public CommandAPICommand getDebugCommand() {
        return new CommandAPICommand("debug")
                .withSubcommands(
                        getLootChanceCommand(),
                        getBiomeCommand(),
                        getSeasonCommand()
                );
    }

    public CommandAPICommand getBiomeCommand() {
        return new CommandAPICommand("biome")
                .executesPlayer((player, arg) -> {
                    AdventureManagerImpl.getInstance().sendMessage(player, BiomeAPI.getBiome(player.getLocation()));
                });
    }

    public CommandAPICommand getSeasonCommand() {
        return new CommandAPICommand("season")
                .executesPlayer((player, arg) -> {
                    SeasonInterface seasonInterface = CustomFishingPlugin.get().getIntegrationManager().getSeasonInterface();
                    if (seasonInterface == null) {
                        AdventureManagerImpl.getInstance().sendMessageWithPrefix(player, "NO SEASON PLUGIN");
                        return;
                    }
                    AdventureManagerImpl.getInstance().sendMessage(player, seasonInterface.getSeason(player.getLocation().getWorld()));
                });
    }

    public CommandAPICommand getLootChanceCommand() {
        return new CommandAPICommand("loot-chance")
                .withArguments(new BooleanArgument("lava fishing").replaceSuggestions(ArgumentSuggestions.stringsWithTooltips(info ->
                        new IStringTooltip[] {
                                StringTooltip.ofString("true", "loots in lava"),
                                StringTooltip.ofString("false", "loots in water")
                        })))
                .executesPlayer((player, arg) -> {
                    Effect initialEffect = CustomFishingPlugin.get().getEffectManager().getInitialEffect();
                    FishingPreparation fishingPreparation = new FishingPreparation(player, CustomFishingPlugin.get());
                    boolean inLava = (boolean) arg.getOrDefault("lava fishing", false);
                    fishingPreparation.insertArg("{lava}", String.valueOf(inLava));
                    fishingPreparation.mergeEffect(initialEffect);
                    //TODO apply totems

                    var map = CustomFishingPlugin.get().getFishingManager().getPossibleLootKeysWithWeight(initialEffect, fishingPreparation);
                    List<LootWithWeight> loots = new ArrayList<>();
                    double sum = 0;
                    for (Map.Entry<String, Double> entry : map.entrySet()) {
                        double weight = entry.getValue();
                        String loot = entry.getKey();
                        if (weight <= 0) continue;
                        loots.add(new LootWithWeight(loot, weight));
                        sum += weight;
                    }
                    LootWithWeight[] lootArray = loots.toArray(new LootWithWeight[0]);
                    quickSort(lootArray, 0,lootArray.length - 1);
                    AdventureManager adventureManager = AdventureManagerImpl.getInstance();
                    adventureManager.sendMessage(player, "<red>---------- results ---------");
                    for (LootWithWeight loot : lootArray) {
                        adventureManager.sendMessage(player, "<hover:show_text:'<blue>GET'><click:run_command:/cfishing items loot get "+ loot.key() + ">" +loot.key() + "</click></hover>: <gold>" + String.format("%.2f", loot.weight()*100/sum) + "% <gray>(" + String.format("%.2f", loot.weight()) + ")");
                    }
                    adventureManager.sendMessage(player, "<red>----------- end -----------");
                });
    }

    public record LootWithWeight(String key, double weight) {
    }

    public static void quickSort(LootWithWeight[] loot, int low, int high) {
        if (low < high) {
            int pi = partition(loot, low, high);
            quickSort(loot, low, pi - 1);
            quickSort(loot, pi + 1, high);
        }
    }

    public static int partition(LootWithWeight[] loot, int low, int high) {
        double pivot = loot[high].weight();
        int i = low - 1;
        for (int j = low; j <= high - 1; j++) {
            if (loot[j].weight() > pivot) {
                i++;
                swap(loot, i, j);
            }
        }
        swap(loot, i + 1, high);
        return i + 1;
    }

    public static void swap(LootWithWeight[] loot, int i, int j) {
        LootWithWeight temp = loot[i];
        loot[i] = loot[j];
        loot[j] = temp;
    }
}
