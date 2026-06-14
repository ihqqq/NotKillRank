package me.ihqqq.notkillrank.listener;

import me.chancesd.pvpmanager.event.PlayerCombatLogEvent;
import me.chancesd.pvpmanager.event.PlayerTagEvent;
import me.chancesd.pvpmanager.event.PlayerUntagEvent;
import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.file.module.PvPManagerFile;
import me.ihqqq.notkillrank.hook.CombatLogNpcData;
import me.ihqqq.notkillrank.hook.PvPManagerHook;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class PvPManagerListener implements Listener {

    public PvPManagerListener() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTagged(PlayerTagEvent event) {
        if (!event.isAttacker() && event.getEnemy() != null) {
            PvPManagerHook.getInstance().recordAttacker(
                    event.getPlayer().getUniqueId(),
                    event.getEnemy().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUntagged(PlayerUntagEvent event) {
        PvPManagerHook.getInstance().clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = getPlayerAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;

        if (victim.hasMetadata("NPC") || attacker.hasMetadata("NPC")) return;

        if (!PvPManagerFile.get().getBoolean("block-attack-if-victim-protected", true)) return;
        if (!Settings.MODULE_PROTECTION) return;

        PlayerData victimData = PluginDataManager.getPlayerDatabase(victim.getUniqueId().toString());
        if (victimData == null) return;

        if (EloManager.getInstance().isNewbie(victimData)) {
            event.setCancelled(true);
            String msg = MessageUtil.getMessage("pvpmanager-protection-block",
                            "<red>⚔ <white>{victim} <gray>đang được bảo vệ người mới — không thể tấn công!")
                    .replace("{victim}", victim.getName());
            MessageUtil.sendMessage(attacker, msg);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onNpcDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!PvPManagerHook.getInstance().isTrackedNpc(zombie.getUniqueId())) return;

        if (event instanceof EntityDamageByEntityEvent byEntity
                && getPlayerAttacker(byEntity) != null) {
            return; // legitimate player hit — let it through
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatLog(PlayerCombatLogEvent event) {
        Player logger = event.getPlayer();
        UUID loggerUUID = logger.getUniqueId();

        UUID attackerUUID = PvPManagerHook.getInstance().popLastAttacker(loggerUUID);

        PlayerData loggerData = PluginDataManager.getPlayerDatabase(loggerUUID.toString());
        if (loggerData == null) return;

        var cfg = PvPManagerFile.get();
        double lossMultiplier  = cfg.getDouble("combat-log.elo-loss-multiplier", 2.0);
        boolean attackerGains  = cfg.getBoolean("combat-log.attacker-gains-elo", true);
        boolean broadcast      = cfg.getBoolean("combat-log.broadcast", true);
        boolean spawnNpc       = cfg.getBoolean("combat-log.spawn-npc", false);
        int npcTimeout         = cfg.getInt("combat-log.npc-timeout-seconds", 30);

        int baseElo = (int) Math.floor(loggerData.getElo() * (Settings.ELO_KILL_PERCENT / 100.0));
        if (baseElo < 1) baseElo = 1;
        int eloLoss = (int) Math.round(baseElo * lossMultiplier);

        int oldLoggerElo  = loggerData.getElo();
        int newLoggerElo  = Math.max(Settings.ELO_MIN, oldLoggerElo - eloLoss);
        int actualLoss    = oldLoggerElo - newLoggerElo;

        loggerData.setElo(newLoggerElo);
        loggerData.setDeaths(loggerData.getDeaths() + 1);
        loggerData.setDeathStreak(loggerData.getDeathStreak() + 1);
        loggerData.setKillStreak(0);
        PluginDataManager.invalidateTopCache();

        if (spawnNpc) {
            boolean spawned = spawnCombatLogNpc(
                    logger, attackerUUID, actualLoss, attackerGains, npcTimeout);
            if (spawned) {
                if (broadcast) {
                    String msg = MessageUtil.getMessage("pvpmanager-combat-log-npc",
                                    "<red>☠ <white>{logger} <gray>thoát game trong combat! "
                                            + "<red>-{elo_loss} elo<gray>. "
                                            + "<yellow>NPC xuất hiện — tiêu diệt trong {seconds}s!")
                            .replace("{logger}", logger.getName())
                            .replace("{elo_loss}", String.valueOf(actualLoss))
                            .replace("{seconds}", String.valueOf(npcTimeout));
                    MessageUtil.sendBroadcast(msg);
                }
            } else {
                applyAttackerReward(attackerUUID, actualLoss, attackerGains, broadcast, logger);
            }
        } else {
            applyAttackerReward(attackerUUID, actualLoss, attackerGains, broadcast, logger);
        }

        final String loggerUuidStr = loggerUUID.toString();
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () ->
                PluginDataManager.savePlayerDatabaseToStorage(loggerUuidStr));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNpcDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        boolean isOurNpc = zombie.hasMetadata("notkillrank-npc");
        CombatLogNpcData npcData = PvPManagerHook.getInstance().removeNpc(zombie.getUniqueId());

        if (npcData == null && !isOurNpc) return; // Not one of our NPCs

        event.getDrops().clear();
        event.setDroppedExp(0);

        if (npcData == null) return;

        PluginDataManager.unexcludeFromTop(zombie.getUniqueId().toString());
        if (npcData.getTaskId() >= 0) {
            Bukkit.getScheduler().cancelTask(npcData.getTaskId());
        }

        Player killer = zombie.getKiller();
        if (killer == null) return; // burned, suffocated, etc. — no reward

        var cfg = PvPManagerFile.get();
        boolean broadcast = cfg.getBoolean("combat-log.broadcast", true);

        PlayerData killerData = PluginDataManager.getOrCreate(killer);
        int eloGain = 0;

        if (npcData.isAttackerGains() && npcData.getPendingEloGain() > 0) {
            eloGain = npcData.getPendingEloGain();
            int oldElo = killerData.getElo();
            int newElo = oldElo + eloGain;
            killerData.setElo(newElo);
            if (newElo > killerData.getPeakElo()) killerData.setPeakElo(newElo);
            RankManager.getInstance().checkRankUp(killer, killerData, oldElo, newElo);
        }

        killerData.setKills(killerData.getKills() + 1);
        killerData.setKillStreak(killerData.getKillStreak() + 1);
        if (killerData.getKillStreak() > killerData.getHighestKillStreak()) {
            killerData.setHighestKillStreak(killerData.getKillStreak());
        }
        killerData.setDeathStreak(0);
        PluginDataManager.invalidateTopCache();

        if (broadcast) {
            broadcastNpcKill(killer, killerData, npcData, eloGain);
        }

        if (eloGain > 0) {
            String notify = MessageUtil.getMessage("pvpmanager-combat-log-notify",
                            "<green>+{elo_gain} elo <gray>— <white>{logger} <gray>đã thoát game trong combat!")
                    .replace("{elo_gain}", String.valueOf(eloGain))
                    .replace("{logger}", npcData.getOriginalPlayerName());
            MessageUtil.sendMessage(killer, notify);
        }

        final String killerUuidStr = killer.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () ->
                PluginDataManager.savePlayerDatabaseToStorage(killerUuidStr));
    }

    private void applyAttackerReward(UUID attackerUUID, int actualLoss,
                                     boolean attackerGains, boolean broadcast, Player logger) {
        Player attacker = (attackerUUID != null) ? Bukkit.getPlayer(attackerUUID) : null;
        PlayerData attackerData = null;
        int eloGain = 0;

        if (attacker != null && attacker.isOnline() && attackerGains && actualLoss > 0) {
            attackerData = PluginDataManager.getOrCreate(attacker);
            eloGain = actualLoss;
            int oldElo = attackerData.getElo();
            int newElo = oldElo + eloGain;
            attackerData.setElo(newElo);
            if (newElo > attackerData.getPeakElo()) attackerData.setPeakElo(newElo);
            attackerData.setKills(attackerData.getKills() + 1);
            attackerData.setKillStreak(attackerData.getKillStreak() + 1);
            if (attackerData.getKillStreak() > attackerData.getHighestKillStreak()) {
                attackerData.setHighestKillStreak(attackerData.getKillStreak());
            }
            attackerData.setDeathStreak(0);
            RankManager.getInstance().checkRankUp(attacker, attackerData, oldElo, newElo);
        }

        if (broadcast) broadcastCombatLog(logger, attacker, attackerData, actualLoss, eloGain, attackerGains);

        if (attacker != null && attacker.isOnline() && eloGain > 0) {
            String notify = MessageUtil.getMessage("pvpmanager-combat-log-notify",
                            "<green>+{elo_gain} elo <gray>— <white>{logger} <gray>đã thoát game trong combat!")
                    .replace("{elo_gain}", String.valueOf(eloGain))
                    .replace("{logger}", logger.getName());
            MessageUtil.sendMessage(attacker, notify);
        }

        if (attackerData != null && attacker != null) {
            final String attackerUuidStr = attacker.getUniqueId().toString();
            Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () ->
                    PluginDataManager.savePlayerDatabaseToStorage(attackerUuidStr));
        }
    }


    private boolean spawnCombatLogNpc(Player logger, UUID trackedAttackerUUID,
                                      int pendingEloGain, boolean attackerGains, int timeoutSeconds) {
        Location loc = logger.getLocation();
        if (loc.getWorld() == null) return false;

        ItemStack helmet     = logger.getInventory().getHelmet();
        ItemStack chestplate = logger.getInventory().getChestplate();
        ItemStack leggings   = logger.getInventory().getLeggings();
        ItemStack boots      = logger.getInventory().getBoots();
        ItemStack mainHand   = logger.getInventory().getItemInMainHand();

        Zombie zombie = loc.getWorld().spawn(loc, Zombie.class);
        zombie.customName(MessageUtil.parse(
                "<red>☠ <white>" + logger.getName() + " <dark_gray>[Combat Log]"));
        zombie.setCustomNameVisible(true);
        zombie.setAI(false);
        zombie.setGlowing(true);
        zombie.setCanPickupItems(false);
        zombie.setSilent(true);
        zombie.setRemoveWhenFarAway(false);
        zombie.setBaby(false);
        zombie.setMetadata("notkillrank-npc", new FixedMetadataValue(NotKillRank.plugin, logger.getUniqueId().toString()));

        var eq = zombie.getEquipment();
        if (eq != null) {
            if (isRealItem(helmet))     eq.setHelmet(helmet.clone());
            if (isRealItem(chestplate)) eq.setChestplate(chestplate.clone());
            if (isRealItem(leggings))   eq.setLeggings(leggings.clone());
            if (isRealItem(boots))      eq.setBoots(boots.clone());
            if (isRealItem(mainHand))   eq.setItemInMainHand(mainHand.clone());
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
        }

        CombatLogNpcData data = new CombatLogNpcData(
                logger.getUniqueId(), logger.getName(),
                trackedAttackerUUID, pendingEloGain, attackerGains);
        String zombieUUIDStr = zombie.getUniqueId().toString();
        PluginDataManager.excludeFromTop(zombieUUIDStr);

        PvPManagerHook.getInstance().registerNpc(zombie.getUniqueId(), data);

        UUID zombieUUID = zombie.getUniqueId();
        int taskId = Bukkit.getScheduler().runTaskLater(NotKillRank.plugin, () -> {
            CombatLogNpcData d = PvPManagerHook.getInstance().removeNpc(zombieUUID);
            PluginDataManager.unexcludeFromTop(zombieUUID.toString());
            if (d == null) return; // already killed — do nothing
            if (!zombie.isDead()) zombie.remove();
            if (PvPManagerFile.get().getBoolean("combat-log.broadcast", true)) {
                String msg = MessageUtil.getMessage("pvpmanager-npc-timeout",
                                "<gray>☠ NPC của <white>{logger} <gray>đã biến mất sau {seconds}s!")
                        .replace("{logger}", d.getOriginalPlayerName())
                        .replace("{seconds}", String.valueOf(timeoutSeconds));
                MessageUtil.sendBroadcast(msg);
            }
        }, timeoutSeconds * 20L).getTaskId();

        data.setTaskId(taskId);
        return true;
    }

    private void broadcastCombatLog(Player logger, Player attacker, PlayerData attackerData,
                                    int actualLoss, int eloGain, boolean attackerGains) {
        if (attacker != null && attacker.isOnline() && attackerGains && eloGain > 0) {
            String rankAttacker = RankManager.getInstance().getRankTag(attackerData.getElo() - eloGain);
            String msg = MessageUtil.getMessage("pvpmanager-combat-log-with-attacker",
                            "<red>☠ <white>{logger} <gray>thoát game trong combat! "
                                    + "<red>-{elo_loss} elo<gray>. "
                                    + "{rank_attacker}<white>{attacker} <gray>nhận <green>+{elo_gain} elo<gray>.")
                    .replace("{logger}", logger.getName())
                    .replace("{elo_loss}", String.valueOf(actualLoss))
                    .replace("{rank_attacker}", rankAttacker)
                    .replace("{attacker}", attacker.getName())
                    .replace("{elo_gain}", String.valueOf(eloGain));
            MessageUtil.sendBroadcast(msg);
        } else {
            String msg = MessageUtil.getMessage("pvpmanager-combat-log",
                            "<red>☠ <white>{logger} <gray>thoát game trong combat! <red>-{elo_loss} elo<gray>.")
                    .replace("{logger}", logger.getName())
                    .replace("{elo_loss}", String.valueOf(actualLoss));
            MessageUtil.sendBroadcast(msg);
        }
    }

    private void broadcastNpcKill(Player killer, PlayerData killerData,
                                  CombatLogNpcData npcData, int eloGain) {
        if (eloGain > 0) {
            String rankKiller = RankManager.getInstance().getRankTag(killerData.getElo() - eloGain);
            String msg = MessageUtil.getMessage("pvpmanager-npc-killed",
                            "<gold>⚔ {rank_killer}<white>{killer} <gray>tiêu diệt NPC của "
                                    + "<white>{logger}<gray>! <green>+{elo_gain} elo<gray>.")
                    .replace("{rank_killer}", rankKiller)
                    .replace("{killer}", killer.getName())
                    .replace("{logger}", npcData.getOriginalPlayerName())
                    .replace("{elo_gain}", String.valueOf(eloGain));
            MessageUtil.sendBroadcast(msg);
        } else {
            String msg = MessageUtil.getMessage("pvpmanager-npc-killed-no-elo",
                            "<gold>⚔ <white>{killer} <gray>tiêu diệt NPC của <white>{logger}<gray>!")
                    .replace("{killer}", killer.getName())
                    .replace("{logger}", npcData.getOriginalPlayerName());
            MessageUtil.sendBroadcast(msg);
        }
    }

    private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    private boolean isRealItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
}
