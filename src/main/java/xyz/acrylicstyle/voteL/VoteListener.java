package xyz.acrylicstyle.voteL;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import util.ICollectionList;
import xyz.acrylicstyle.points.MiningPoints;
import xyz.acrylicstyle.shared.BaseMojangAPI;
import xyz.acrylicstyle.tomeito_api.command.PlayerCommandExecutor;
import xyz.acrylicstyle.tomeito_api.providers.ConfigProvider;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class VoteListener extends JavaPlugin implements Listener {
    public static ConfigProvider config = null;
    public static int votes = 0;
    public static LuckPerms api = null;
    public static util.Collection<UUID, AtomicInteger> playerVotes = new util.Collection<>();

    @Override
    public void onEnable() {
        config = ConfigProvider.getConfig("./plugins/VoteListener/config.yml");
        votes = config.getInt("votes", 0);
        Objects.requireNonNull(Bukkit.getPluginCommand("forcevote")).setExecutor(new PlayerCommandExecutor() {
            @Override
            public void onCommand(Player player, String[] args) {
                if (args.length == 0) {
                    player.sendMessage(ChatColor.RED + "プレイヤーを指定してください。");
                    return;
                }
                VoteListener.this.onVotifier(new VotifierEvent(new Vote("plugin:VoteListener", args[0], "0.0.0.0", Long.toString(System.currentTimeMillis()))));
            }
        });
        Bukkit.getPluginManager().registerEvents(this, this);
        new BukkitRunnable() {
            @Override
            public void run() {
                RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    api = provider.getProvider();
                }
            }
        }.runTaskLater(this, 1);
        config.getConfigSectionValue("players", true)
                .forEach((s, o) -> playerVotes.add(UUID.fromString(s), new AtomicInteger((int) o)));
    }

    @Override
    public void onDisable() {
        config.set("votes", votes);
        config.set("players", playerVotes.map((u, a) -> u.toString(), (u, a) -> a.get()));
        config.save();
    }

    public static List<Material> trashItems = new ArrayList<>();

    static {
        trashItems.add(Material.ROTTEN_FLESH);
        trashItems.add(Material.SPIDER_EYE);
        trashItems.add(Material.POISONOUS_POTATO);
        trashItems.add(Material.SUSPICIOUS_STEW);
        trashItems.add(Material.BEEF);
        trashItems.add(Material.CHICKEN);
        trashItems.add(Material.PORKCHOP);
        trashItems.add(Material.SWEET_BERRIES);
        trashItems.add(Material.SWEET_BERRY_BUSH);
        trashItems.add(Material.RABBIT);
        trashItems.add(Material.BEETROOT_SOUP);
        trashItems.add(Material.MUSHROOM_STEW);
        trashItems.add(Material.TROPICAL_FISH);
        trashItems.add(Material.HONEY_BOTTLE);
        trashItems.add(Material.DRIED_KELP);
        trashItems.add(Material.POTATO);
        trashItems.add(Material.POTATOES);
        trashItems.add(Material.CARROTS);
        trashItems.add(Material.CARROT);
    }

    @EventHandler
    public void onVotifier(VotifierEvent e) {
        UUID uuid = BaseMojangAPI.getUniqueId(e.getVote().getUsername());
        if (uuid == null) return; // if its invalid player id, then skip.
        if (!playerVotes.containsKey(uuid)) playerVotes.add(uuid, new AtomicInteger());
        int v = playerVotes.get(uuid).incrementAndGet();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && !player.hasPermission("group.admin") && !player.hasPermission("group.helper") && api.getGroupManager().getGroup("vote" + v) != null) {
            // if player is null, we cannot guarantee that player doesn't have admin permission
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent set vote" + v);
        }
        votes++;
        boolean diamond = votes % 30 == 0;
        int l = 30 - (votes % 30);
        Bukkit.broadcastMessage(ChatColor.GOLD + e.getVote().getUsername() + ChatColor.GREEN + "さんが投票しました！ありがとうございます！");
        Bukkit.broadcastMessage(ChatColor.GREEN + "現在合計で" + ChatColor.RED + votes + ChatColor.GREEN + "投票です。次のダイヤ配布まであと" + ChatColor.RED + l + ChatColor.GREEN + "投票必要です。");
        Bukkit.broadcastMessage(ChatColor.GREEN + "このプレイヤーは今回で" + ChatColor.RED + v + ChatColor.GREEN + "回目の投票です。");
        if (diamond) Bukkit.broadcastMessage("" + ChatColor.LIGHT_PURPLE + votes + "回目の投票です！ダイヤモンドが配布されました！");
        Material material = Objects.requireNonNull(
                ICollectionList
                        .asList(Material.values())
                        .filter(Material::isEdible)
                        .filter(m -> !trashItems.contains(m))
                        .shuffle()
                        .first()
        );
        ItemStack food = new ItemStack(material, 3);
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.giveExp(50);
            Collection<ItemStack> items = diamond
                    ? p.getInventory().addItem(food, new ItemStack(Material.DIAMOND, 10)).values()
                    : p.getInventory().addItem(food).values();
            if (items.size() != 0) {
                p.sendMessage(ChatColor.GOLD + "インベントリに入りきらなかったアイテムがドロップされました。");
                items.forEach(item -> p.getWorld().dropItemNaturally(p.getLocation(), item));
            }
            MiningPoints.addPoints(p.getUniqueId(), 10);
        });
    }
}
