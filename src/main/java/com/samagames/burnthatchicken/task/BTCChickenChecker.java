package com.samagames.burnthatchicken.task;

import java.util.Random;

import net.samagames.tools.Titles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.samagames.burnthatchicken.BTCMap.BTCGameZone;
import com.samagames.burnthatchicken.BTCPlayer;
import com.samagames.burnthatchicken.BTCPlugin;
import com.samagames.burnthatchicken.metadata.ChickenMetadataValue;
import com.samagames.burnthatchicken.util.ChatUtils;
import com.samagames.burnthatchicken.util.ParticlesUtils;

/*
 * This file is part of BurnThatChicken.
 *
 * BurnThatChicken is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BurnThatChicken is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BurnThatChicken.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BTCChickenChecker implements Runnable {
    private BTCPlugin main;
    private static BTCChickenChecker instance;
    private Random random;

    public BTCChickenChecker(BTCPlugin main) {
        this.main = main;
        instance = this;
        random = new Random();
    }

    @Override
    public void run() {
        for (World w : Bukkit.getWorlds())
            for (Entity e : w.getEntities())
                if (!(e.getType() != EntityType.CHICKEN || e.isDead()))
                    tickEntity(e);
    }

    private void tickEntity(Entity e) {
        ChickenMetadataValue meta = ChickenMetadataValue
                .getMetadataValueFromChicken(main, e);
        if (meta == null) {
            e.remove();
            return;
        }

        if (meta.isSpecial()) {

            for (int i = 0; i < 4; i++) {
                double theta = 2 * Math.PI * random.nextDouble();
                double phi = Math.acos(2 * random.nextDouble() - 1);
                double x = e.getLocation().getX() + (1 * Math.sin(phi) * Math.cos(theta));
                double y = e.getLocation().getY() + (1 * Math.sin(phi) * Math.sin(theta));
                double z = e.getLocation().getZ() + (1 * Math.cos(phi));
                ParticlesUtils.sendParticleToPlayers(meta.getSpecialAttribute().getParticle(), (float) x, (float) y, (float) z);
            }

            for (BTCGameZone zone : main.getCurrentMap().getGameZones())
                if (zone.getUniqueId() == meta.getGameZoneId()
                        && !zone.isEnded()
                        && zone.isInChickenEndZone(e.getLocation()))
                    e.remove();
        } else
            checkZones(meta, e);
    }

    private void checkZones(ChickenMetadataValue meta, Entity e)
    {
        for (BTCGameZone zone : main.getCurrentMap().getGameZones()) {
            if (zone.getUniqueId() == meta.getGameZoneId()
                    && !zone.isEnded()
                    && zone.isInChickenEndZone(e.getLocation())) {
                zone.setEnded(true);
                clearChicken(zone);
                BTCPlayer player = main.getPlayerByZone(zone.getUniqueId());
                if (player == null)
                    return;
                player.setSpectator();
                main.addPlayerToRank(player);
                Player p = player.getPlayerIfOnline();
                ChatUtils.broadcastMessage(ChatUtils.getPluginPrefix() + " " + player.getName() + " est éliminé !");
                if (p != null)
                    Titles.sendTitle(p, 0, 100, 0, "", ChatColor.GOLD + "Vous avez perdu !");
                main.checkPlayers();
                player.updateScoreboard();
            }
        }
    }

    public void clearChicken(BTCGameZone btcGameZone) {
        for (World w : Bukkit.getWorlds())
            for (Entity e : w.getEntities()) {
                if (e.getType() != EntityType.CHICKEN)
                    continue;
                ChickenMetadataValue meta = ChickenMetadataValue.getMetadataValueFromChicken(main, e);
                if (meta == null || btcGameZone.getUniqueId() == meta.getGameZoneId())
                    e.remove();
            }
    }

    public static BTCChickenChecker getInstance() {
        return instance;
    }
}
