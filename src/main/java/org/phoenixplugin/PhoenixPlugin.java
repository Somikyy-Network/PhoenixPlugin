package org.phoenixplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhoenixPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Long> phoenixRespawnTimes = new HashMap<>();
    private final long phoenixRespawnDelay = 10000; // 10 секунд

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        PluginCommand summonPhoenixCommand = this.getCommand("summon_phoenix");
        if (summonPhoenixCommand != null) {
            summonPhoenixCommand.setExecutor(this::onSummonPhoenixCommand);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private boolean onSummonPhoenixCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        summonPhoenix(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 1)); // 10 секунд иммунитета к огню
        return true;
    }

    private void summonPhoenix(Player player) {
        Location location = player.getLocation();
        Zombie phoenix = (Zombie) player.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        phoenix.setCustomName(ChatColor.GOLD + "Phoenix");
        phoenix.setCustomNameVisible(true);
        phoenix.setFireTicks(Integer.MAX_VALUE);
        phoenix.setHealth(20.0);
        phoenix.setCanPickupItems(false);
        phoenix.setSilent(true);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!phoenix.isDead()) {
                for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                    if (nearbyPlayer.getLocation().distance(phoenix.getLocation()) < 10) {
                        phoenix.launchProjectile(SmallFireball.class);
                    }
                }
            }
        }, 0L, 20L);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie && "Phoenix".equals(event.getEntity().getCustomName())) {
            UUID entityUUID = event.getEntity().getUniqueId();
            phoenixRespawnTimes.put(entityUUID, System.currentTimeMillis() + phoenixRespawnDelay);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Zombie && "Phoenix".equals(event.getEntity().getCustomName())) {
            UUID entityUUID = event.getEntity().getUniqueId();
            if (phoenixRespawnTimes.containsKey(entityUUID) &&
                    System.currentTimeMillis() < phoenixRespawnTimes.get(entityUUID)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Zombie && "Phoenix".equals(event.getEntity().getCustomName())) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                event.setCancelled(true);
            }
        }
    }
}

