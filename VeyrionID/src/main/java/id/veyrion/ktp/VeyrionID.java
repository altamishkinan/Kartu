package id.veyrion.ktp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class VeyrionID extends JavaPlugin implements Listener, TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private NamespacedKey ownerKey;
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ownerKey = new NamespacedKey(this, "owner");
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
        var cmd = getCommand("ki");
        if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        getServer().getPluginManager().registerEvents(this, this);
    }

    // ---------- Pesan & kartu ----------

    private Component msg(String body) {
        return MM.deserialize(getConfig().getString("prefix", "") + "<white>" + body);
    }

    /** Isi satu halaman buku kartu (teks gelap agar terbaca di kertas buku). */
    private Component cardPage(UUID id) {
        String p = "players." + id + ".";
        String petugas = data.getString(p + "didaftarkan-oleh");
        TagResolver r = TagResolver.resolver(
                Placeholder.unparsed("nama", data.getString(p + "nama", "-")),
                Placeholder.unparsed("umur", data.getString(p + "umur", "-")),
                Placeholder.unparsed("lahir", data.getString(p + "lahir", "-")),
                Placeholder.unparsed("ras", data.getString(p + "ras", "-")),
                Placeholder.unparsed("terbit", data.getString(p + "terbit", "-")),
                Placeholder.unparsed("petugas", petugas == null ? "" : petugas));
        String t = "<#7a5a10><bold>KARTU IDENTITAS<newline>VEYRION</bold><newline>"
                + "<dark_gray>━━━━━━━━━━━━━━<newline>"
                + "<#7a5a10>Nama: <black><nama><newline>"
                + "<#7a5a10>Umur: <black><umur> tahun<newline>"
                + "<#7a5a10>Lahir: <black><lahir><newline>"
                + "<#7a5a10>Ras: <#5a3d00><bold><ras></bold><newline>"
                + "<#7a5a10>Terbit: <black><terbit>"
                + (petugas == null ? "" : "<newline><#7a5a10>Petugas: <black><petugas>");
        return MM.deserialize(t, r);
    }

    private void showCard(Player viewer, UUID owner) {
        if (!data.contains("players." + owner)) {
            viewer.sendMessage(msg("Kartu ini belum terdaftar."));
            return;
        }
        viewer.openBook(makeCard(owner)); // dibuat ulang tiap dibuka, jadi selalu sesuai data terbaru
    }

    private ItemStack makeCard(UUID owner) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.title(Component.text("Kartu Identitas Veyrion"));
        meta.author(Component.text("Kerajaan Veyrion"));
        meta.addPages(cardPage(owner));
        meta.displayName(MM.deserialize("<gradient:#b8892b:#f1d98a><bold>Kartu Identitas Veyrion</bold></gradient>")
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<#b8892b>Nama <gray>: <white>" + "<nama>",
                Placeholder.unparsed("nama", data.getString("players." + owner + ".nama", "-")))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(MM.deserialize("<#b8892b>Ras  <gray>: <#f1d98a><ras>",
                Placeholder.unparsed("ras", data.getString("players." + owner + ".ras", "-")))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<gray>Klik kanan: buka · Klik kanan pemain: tunjukkan")
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        int cmd = getConfig().getInt("item.custom-model-data", 0);
        if (cmd > 0) meta.setCustomModelData(cmd);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
        return item;
    }

    private void giveCard(Player p, UUID owner) {
        p.getInventory().addItem(makeCard(owner)).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }

    private UUID cardOwner(ItemStack item) {
        if (item == null || (item.getType() != Material.WRITTEN_BOOK && item.getType() != Material.PAPER)
                || !item.hasItemMeta()) return null; // PAPER = kartu lama (v1.0)
        String s = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        try { return s == null ? null : UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    private void save() {
        try { data.save(dataFile); } catch (IOException e) { getLogger().severe("Gagal menyimpan data: " + e.getMessage()); }
    }

    // ---------- Event ----------

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        UUID owner = cardOwner(e.getItem());
        if (owner == null) return;
        e.setCancelled(true);
        showCard(e.getPlayer(), owner);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String key = "players." + p.getUniqueId();
        if (!data.getBoolean(key + ".kartu-tertunda", false)) return;
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) return;
            data.set(key + ".kartu-tertunda", null);
            save();
            giveCard(p, p.getUniqueId());
            p.sendMessage(msg("Kamu telah didaftarkan sebagai penduduk Veyrion. Kartu identitasmu diberikan."));
        }, 40L);
    }

    @EventHandler
    public void onShow(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof Player target)) return;
        UUID owner = cardOwner(e.getPlayer().getInventory().getItemInMainHand());
        if (owner == null) return;
        e.setCancelled(true);
        target.sendMessage(msg("<#f1d98a>" + e.getPlayer().getName() + "</#f1d98a> menunjukkan kartu identitasnya:"));
        showCard(target, owner);
    }

    // ---------- Perintah ----------

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) { s.sendMessage(msg("/ki <daftar|lihat|tunjuk|kartu|hapus|reload>")); return true; }
        boolean admin = s.hasPermission("veyrion.id.admin");
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "daftar" -> register(s, a);
            case "lihat" -> {
                if (!(s instanceof Player p)) { s.sendMessage(msg("Hanya untuk pemain.")); return true; }
                UUID target = p.getUniqueId();
                if (a.length > 1) {
                    if (!admin) { s.sendMessage(msg("Kamu tidak punya izin.")); return true; }
                    Player t = Bukkit.getPlayerExact(a[1]);
                    if (t == null) { s.sendMessage(msg("Pemain tidak ditemukan.")); return true; }
                    target = t.getUniqueId();
                }
                showCard(p, target);
            }
            case "tunjuk" -> {
                if (!(s instanceof Player p)) return true;
                if (!data.contains("players." + p.getUniqueId())) { p.sendMessage(msg("Kamu belum punya kartu.")); return true; }
                double r = getConfig().getDouble("show-radius", 6);
                p.sendMessage(msg("Kartu ditunjukkan ke pemain di sekitarmu."));
                for (Player o : p.getWorld().getPlayers()) {
                    if (o.equals(p) || o.getLocation().distance(p.getLocation()) > r) continue;
                    o.sendMessage(msg("<#f1d98a>" + p.getName() + "</#f1d98a> menunjukkan kartu identitasnya:"));
                    showCard(o, p.getUniqueId());
                }
            }
            case "kartu" -> {
                if (!(s instanceof Player p)) return true;
                if (!data.contains("players." + p.getUniqueId())) { p.sendMessage(msg("Daftar dulu: /ki daftar")); return true; }
                giveCard(p, p.getUniqueId());
                p.sendMessage(msg("Kartu identitasmu diberikan."));
            }
            case "hapus" -> {
                if (!admin) { s.sendMessage(msg("Kamu tidak punya izin.")); return true; }
                if (a.length < 2) { s.sendMessage(msg("/ki hapus <pemain>")); return true; }
                @SuppressWarnings("deprecation") var off = Bukkit.getOfflinePlayer(a[1]);
                data.set("players." + off.getUniqueId(), null);
                save();
                s.sendMessage(msg("Data identitas " + a[1] + " dihapus."));
            }
            case "reload" -> {
                if (!admin) { s.sendMessage(msg("Kamu tidak punya izin.")); return true; }
                reloadConfig();
                data = YamlConfiguration.loadConfiguration(dataFile);
                s.sendMessage(msg("Konfigurasi dimuat ulang."));
            }
            default -> s.sendMessage(msg("Perintah tidak dikenal."));
        }
        return true;
    }

    private void register(CommandSender s, String[] args) {
        // Diri sendiri : daftar <nama> <umur> <tgl> <ras>            (4 argumen)
        // Orang lain   : daftar <pemain> <nama> <umur> <tgl> <ras>   (5 argumen)
        boolean other = args.length >= 6;
        if (args.length < 5) {
            s.sendMessage(msg("/ki daftar <nama> <umur> <DD/MM/YYYY> <ras>"));
            s.sendMessage(msg("<gray>Pakai _ sebagai spasi pada nama. Contoh: /ki daftar Kynaro_Karamazov 17 11/01/2009 Elf"));
            if (s.hasPermission("veyrion.id.register.other")) {
                s.sendMessage(msg("/ki daftar <pemain> <nama> <umur> <DD/MM/YYYY> <ras>"));
                s.sendMessage(msg("<gray>Mendaftarkan orang lain. Contoh: /ki daftar Steve Aria_Vale 20 03/05/2006 Elf"));
            }
            return;
        }
        // a = [daftar, nama, umur, tgl, ras] (shift target jika mendaftarkan orang lain)
        String[] a = other ? java.util.stream.Stream.concat(java.util.stream.Stream.of(args[0]),
                java.util.Arrays.stream(args, 2, args.length)).toArray(String[]::new) : args;
        OfflinePlayer target;
        if (other) {
            if (!s.hasPermission("veyrion.id.register.other")) {
                s.sendMessage(msg("Kamu tidak punya izin mendaftarkan orang lain."));
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(args[1]);
                if (off == null || !off.hasPlayedBefore()) {
                    s.sendMessage(msg("Pemain <#f1d98a>" + args[1] + "</#f1d98a> belum pernah masuk server."));
                    return;
                }
                target = off;
            }
        } else {
            if (!(s instanceof Player p)) { s.sendMessage(msg("Konsol harus menyebut pemain: /ki daftar <pemain> <nama> ...")); return; }
            target = p;
        }
        String key = "players." + target.getUniqueId();
        if (!other && data.contains(key) && !getConfig().getBoolean("allow-reregister", false)
                && !s.hasPermission("veyrion.id.admin")) {
            s.sendMessage(msg("Kamu sudah terdaftar. Hubungi staf untuk mengubah data."));
            return;
        }
        String nama = a[1].replace('_', ' ');
        if (nama.length() < 2 || nama.length() > 32) { s.sendMessage(msg("Nama harus 2–32 karakter.")); return; }
        int umur;
        try { umur = Integer.parseInt(a[2]); } catch (NumberFormatException e) { umur = -1; }
        if (umur < 1 || umur > 9999) { s.sendMessage(msg("Umur harus angka 1–9999.")); return; }
        try { LocalDate.parse(a[3], DATE); } catch (Exception e) { s.sendMessage(msg("Format tanggal lahir: DD/MM/YYYY.")); return; }
        String ras = getConfig().getStringList("races").stream()
                .filter(x -> x.equalsIgnoreCase(a[4])).findFirst().orElse(null);
        if (ras == null) {
            s.sendMessage(msg("Ras tidak valid. Pilihan: " + String.join(", ", getConfig().getStringList("races"))));
            return;
        }
        data.set(key + ".nama", nama);
        data.set(key + ".umur", umur);
        data.set(key + ".lahir", a[3]);
        data.set(key + ".ras", ras);
        data.set(key + ".terbit", LocalDate.now().format(DATE));
        data.set(key + ".didaftarkan-oleh", other ? s.getName() : null);

        Player online = target.getPlayer();
        if (online != null) {
            data.set(key + ".kartu-tertunda", null);
            save();
            giveCard(online, online.getUniqueId());
            online.sendMessage(msg(other
                    ? "<#f1d98a>" + s.getName() + "</#f1d98a> mendaftarkanmu. Kamu resmi tercatat sebagai penduduk Veyrion."
                    : "Kamu resmi tercatat sebagai penduduk Veyrion."));
        } else {
            data.set(key + ".kartu-tertunda", true); // diberikan saat pemain masuk
            save();
        }
        if (other) {
            s.sendMessage(msg("<#f1d98a>" + nama + "</#f1d98a> (" + target.getName() + ") berhasil didaftarkan"
                    + (online == null ? ". Kartu akan diberikan saat pemain masuk." : ". Kartu sudah diberikan.")));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> opts = new ArrayList<>();
        if (a.length == 2 && a[0].equalsIgnoreCase("daftar") && s.hasPermission("veyrion.id.register.other")) {
            Bukkit.getOnlinePlayers().forEach(p -> opts.add(p.getName()));
        } else if (a.length == 1) {
            opts.addAll(List.of("daftar", "lihat", "tunjuk", "kartu"));
            if (s.hasPermission("veyrion.id.admin")) opts.addAll(List.of("hapus", "reload"));
        } else if (a.length == 2 && (a[0].equalsIgnoreCase("hapus") || a[0].equalsIgnoreCase("lihat"))) {
            Bukkit.getOnlinePlayers().forEach(p -> opts.add(p.getName()));
        } else if (a.length == 5 && a[0].equalsIgnoreCase("daftar")) {
            opts.addAll(getConfig().getStringList("races"));
        } else if (a.length == 6 && a[0].equalsIgnoreCase("daftar")) {
            opts.addAll(getConfig().getStringList("races")); // bentuk: daftar <pemain> <nama> <umur> <tgl> <ras>
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        return opts.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
    }
}
