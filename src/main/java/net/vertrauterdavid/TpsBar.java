package net.vertrauterdavid;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class TpsBar extends JavaPlugin implements Listener {

    private BossBar tpsBar;

    private String tps = "";
    private String mspt = "";
    private long playersTotal = 0;
    private long playersReal = 0;
    private long playersAlts = 0;
    private long playersAfk = 0;
    private long playersBedrock = 0;
    private String ping = "";

    @Override
    public void onEnable() {
        tpsBar = Bukkit.createBossBar(" ", BarColor.GREEN, BarStyle.SEGMENTED_20, BarFlag.PLAY_BOSS_MUSIC);
        tpsBar.setVisible(true);

        Bukkit.getPluginManager().registerEvents(this, this);

        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> {

            double tps = Bukkit.getTPS()[0];
            String tpsColor = "&#00FF30";
            if (tps <= 17) tpsColor = "&#FFFF00";
            if (tps <= 14) tpsColor = "&#FF6B00";
            if (tps <= 10) tpsColor = "&#FF0000";
            this.tps = decimalFormat.format(tps);

            double mspt = Bukkit.getAverageTickTime();
            String msptColor = "&#00FF30";
            if (mspt > 40) msptColor = "&#FFFF00";
            if (mspt > 60) msptColor = "&#FF6B00";
            if (mspt > 80) msptColor = "&#FF0000";
            this.mspt = decimalFormat.format(mspt);

            AtomicLong newReal = new AtomicLong();
            AtomicLong newAlts = new AtomicLong();
            AtomicLong newAfk = new AtomicLong();

            List<String> listedIps = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                String address = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();
                if (!listedIps.contains(address)) {
                    listedIps.add(address);
                    newReal.getAndIncrement();
                } else {
                    newAlts.getAndIncrement();
                }
            });

            afkTimes.entrySet().removeIf(entry -> {
                UUID uuid = entry.getKey();
                long time = entry.getValue();
                if (Bukkit.getPlayer(uuid) == null) return true;
                if (time < System.currentTimeMillis() - (1000 * 60 * 5)) {
                    newAfk.incrementAndGet();
                }
                return false;
            });

            this.playersTotal = Bukkit.getOnlinePlayers().size();
            this.playersReal = newReal.get();
            this.playersAlts = newAlts.get();
            this.playersAfk = newAfk.get();
            this.playersBedrock = new ArrayList<>(Bukkit.getOnlinePlayers()).stream().filter(player -> player.getName().startsWith(".")).toList().size();
            this.ping = decimalFormat.format(new ArrayList<>(Bukkit.getOnlinePlayers()).stream().mapToInt(Player::getPing).average().orElse(0));

            String text =
                    "§7Tps: " + tpsColor + this.tps +
                    " §8▏ " +
                    "§7Mspt: " + msptColor + this.mspt +
                    " §8▏ " +
                    "&#00FF30T: " + this.playersTotal +
                    " §8/ " +
                    "&#FFFF00R: " + this.playersReal +
                    " §8/ " +
                    "&#FF6B00A: " + this.playersAlts +
                    " §8/ " +
                    "&#ff0000A: " + this.playersAfk +
                    " §8/ " +
                    "§7B: " + this.playersBedrock +
                    " §8▏ " +
                    "§7Avg ping: &#00FF30" + this.ping;

            tpsBar.setTitle(translateColorCodes(text));
            tpsBar.setColor((mspt > 60 ? BarColor.RED : (mspt > 40 ? BarColor.YELLOW : BarColor.GREEN)));
            tpsBar.setProgress(Math.max(0, Math.min(1, (mspt / 50))));

            Bukkit.getOnlinePlayers().forEach(player -> {
                if (getConfig().getBoolean(player.getUniqueId().toString() + ".TpsBar", false)) {
                    tpsBar.addPlayer(player);
                } else {
                    tpsBar.removePlayer(player);
                }
            });
        }, 20L, 5L);

        getCommand("tpsbar").setExecutor((commandSender, command, label, args) -> {
            if (!(commandSender instanceof Player player)) return false;
            if (!(player.hasPermission("tpsbar.command"))) return false;

            if (args.length == 0 || args.length == 1) {
                Player target = (args.length == 0 ? player : Bukkit.getPlayer(args[0]));
                if (target == null) {
                    player.sendMessage("This player is not online!");
                    return false;
                }
                getConfig().set(target.getUniqueId().toString() + ".TpsBar", !getConfig().getBoolean(target.getUniqueId().toString() + ".TpsBar", false));
                saveConfig();
                player.sendMessage("Successful!");
                return true;
            }

            player.sendMessage("Please use: /" + label + " <player>");
            return true;
        });

        getCommand("tpsbar").setTabCompleter((commandSender, command, label, args) -> {
            if (!(commandSender instanceof Player player)) return new ArrayList<>();
            if (!(player.hasPermission("tpsbar.command"))) return new ArrayList<>();
            return new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()).stream().filter(content -> content.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
        });

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
        }
    }

    private final HashMap<UUID, Long> afkTimes = new HashMap<>();

    @EventHandler
    public void handle(PlayerMoveEvent event) {
        afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    public String translateColorCodes(String message) {
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

}
