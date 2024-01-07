package com.github.kbinani.turnaround;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public class Main extends JavaPlugin implements Listener {
  private class Arounder {
    private final @Nonnull Player player;
    private final double x;
    private final double y;
    private final double z;
    private final double r;
    private final double w;
    private final float p;
    private final BukkitTask timer;
    private final long startTimeMillis;
    private final double startTheta;

    Arounder(Player player, double x, double y, double z, double radius, double omega) {
      this.player = player;
      this.x = x;
      this.y = y;
      this.z = z;
      this.r = radius;
      this.w = omega;
      this.p = player.getPitch();
      this.timer = Bukkit.getServer().getScheduler().runTaskTimer(Main.this, this::tick, 0, 1);
      this.startTimeMillis = System.currentTimeMillis();
      this.startTheta = Math.atan2(player.getZ() - z, player.getX() - x);
    }

    private void tick() {
      double sec = (System.currentTimeMillis() - this.startTimeMillis) / 1000.0;
      double t = this.startTheta + this.w * sec;
      double x = this.x + this.r * Math.cos(t);
      double z = this.z + this.r * Math.sin(t);
      double yaw = t * 180 / Math.PI + 90;
      var location = new Location(player.getWorld(), x, this.y, z, (float) yaw, this.p);
      player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }

    void terminate() {
      this.timer.cancel();
    }
  }

  private final Map<UUID, Arounder> sessions = new HashMap<>();

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player)) {
      return false;
    }
    if (!label.equals("turnaround")) {
      return false;
    }
    if (args.length == 0) {
      return sendHelpMessage(player);
    }
    var sub = args[0];
    return switch (sub) {
      case "start" -> start(player, args);
      case "stop" -> stop(player);
      default -> sendHelpMessage(player);
    };
  }

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
  }

  @Override
  public void onDisable() {
    for (var session : sessions.values()) {
      session.terminate();;
    }
    sessions.clear();
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    stop(e.getPlayer());
  }

  @EventHandler
  public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
    stop(e.getPlayer());
  }

  private boolean start(Player player, String[] args) {
    stop(player);

    if (args.length != 6) {
      return sendHelpMessage(player);
    }
    double x, y, z, r, w;
    try {
      x = Double.parseDouble(args[1]);
      y = Double.parseDouble(args[2]);
      z = Double.parseDouble(args[3]);
      r = Double.parseDouble(args[4]);
      w = Double.parseDouble(args[5]) * Math.PI / 180.0;
    } catch (Throwable e) {
      player.sendMessage(text("invalid number format").color(RED));
      return false;
    }
    var session = new Arounder(player, x, y, z, r, w);
    sessions.put(player.getUniqueId(), session);
    return true;
  }

  private boolean stop(Player player) {
    var id = player.getUniqueId();
    var current = sessions.get(id);
    if (current != null) {
      current.terminate();
      sessions.remove(id);
    }
    return true;
  }

  private boolean sendHelpMessage(@Nonnull Player player) {
    player.sendMessage(text("/turnaround start {x} {y} {z} {radius} {omega(degrees/sec)}"));
    player.sendMessage(text("/turnaround stop"));
    return false;
  }
}
