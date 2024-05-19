package net.vertrauterdavid;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class PlaceholderHook extends PlaceholderExpansion {

    private final TpsBar tpsBar;

    @Override
    public @NotNull String getIdentifier() {
        return "tpsbar";
    }

    @Override
    public @NotNull String getAuthor() {
        return "VertrauterDavid";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return Arrays.asList(
                "tps",
                "mspt",
                "players_total",
                "players_real",
                "players_alts",
                "players_afk",
                "players_bedrock",
                "ping"
        );
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        if (params.equals("tps")) {
            return String.valueOf(tpsBar.getTps());
        }

        if (params.equals("mspt")) {
            return String.valueOf(tpsBar.getMspt());
        }

        if (params.equals("players_total")) {
            return String.valueOf(tpsBar.getPlayersTotal());
        }

        if (params.equals("players_real")) {
            return String.valueOf(tpsBar.getPlayersReal());
        }

        if (params.equals("players_alts")) {
            return String.valueOf(tpsBar.getPlayersAlts());
        }

        if (params.equals("players_afk")) {
            return String.valueOf(tpsBar.getPlayersAfk());
        }

        if (params.equals("players_bedrock")) {
            return String.valueOf(tpsBar.getPlayersBedrock());
        }

        if (params.equals("ping")) {
            return String.valueOf(tpsBar.getPing());
        }

        return "";
    }

}
