package com.samagames.burnthatchicken;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import net.samagames.api.games.Game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.samagames.burnthatchicken.BTCMap.BTCGameZone;
import com.samagames.burnthatchicken.task.BTCChickenChecker;
import com.samagames.burnthatchicken.util.BTCInventories;
import com.samagames.burnthatchicken.util.ChatUtils;
import com.samagames.burnthatchicken.util.GameState;

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
public class BTCGame extends Game<BTCPlayer> {
    private GameState gamestate;
    private BTCPlugin main;

    public BTCGame(BTCPlugin plugin) {
        super("burnthatchicken", "BurnThatChicken", "", BTCPlayer.class);
        gamestate = GameState.INITIALIZING;
        main = plugin;
    }

    public GameState getGameState() {
        return gamestate;
    }

    public void setGameState(GameState gs) {
        gamestate = gs;
    }

    @Override
    public void startGame() {
        super.startGame();
        Bukkit.getScheduler()
                .scheduleSyncDelayedTask(main,
                        () -> ChatUtils.broadcastTitle(" ", ChatColor.GOLD + "Tuez tous les poulets avant qu'ils ne tombent !", 0, 40, 0)
                        , 20);
        selectPlayers();
        main.getGame().setGameState(GameState.IN_GAME);
        this.main.getServer().getScheduler().runTaskTimerAsynchronously(this.main, new Runnable()
        {
            private int time = 0;

            @Override
            public void run()
            {
                this.time++;
                for (BTCPlayer arena : gamePlayers.values())
                    arena.setScoreboardTime(this.formatTime(time));
            }

            public String formatTime(int time)
            {
                int mins = time / 60;
                int secs = time - mins * 60;

                String secsSTR = (secs < 10) ? "0" + Integer.toString(secs) : Integer.toString(secs);

                return mins + ":" + secsSTR;
            }
        }, 0L, 20L);
    }

    private void selectPlayers() {
        Random random = new Random();
        List<BTCPlayer> list = new ArrayList<>();
        list.addAll(getInGamePlayers().values());
        for (BTCPlayer player : list) {
            try {
                Player p = player.getPlayerIfOnline();
                if (p == null)
                    continue;
                int n;
                BTCGameZone zone;
                do {
                    n = Math.abs(random.nextInt() % gameManager.getGameProperties().getMaxSlots());
                    zone = main.getCurrentMap().getGameZones().get(n);
                } while (zone.getPlayer() != null);
                player.setZone(zone);
                zone.setPlayer(player);
                p.teleport(zone.getPlayerSpawn());
                if (!main.getCurrentMap().canPlayersMove())
                    p.setWalkSpeed(0);
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128));
                BTCInventories.giveGameInventory(p);
                player.setScoreboard();
            } catch (Exception e) {
                main.getServer().getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
        for (BTCGameZone zone : main.getCurrentMap().getGameZones())
            zone.setEnded(zone.getPlayer() == null);
    }

    @Override
    public void handleLogout(Player player) {
        super.handleLogout(player);
        if (main.getGame().getGameState() == GameState.FINISHED)
            return;
        if (main.getGame().getGameState() == GameState.IN_GAME) {
            BTCPlayer btc = main.getGame().getPlayer(player.getUniqueId());
            if (btc == null)
                return;

            main.addPlayerToRank(btc);
            if (btc.getZone() != null) {
                btc.getZone().setEnded(true);
                BTCChickenChecker.getInstance().clearChicken(btc.getZone());
            }
            main.checkPlayers();
        }
    }
}
