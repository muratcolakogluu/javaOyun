package com.cryptdelver.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Kayıt dosyasını okur ve yazar.
 *
 * <p>Biçim, satır başına bir kayıt ve alanları {@code |} ile ayrılmış düz
 * metin. JSON kütüphanesi eklemek yerine bunu seçtim: dosya elle açılıp
 * okunabiliyor (hata ayıklarken çok işe yarıyor), projeye bağımlılık
 * girmiyor ve dosya I/O'nun kendisi görünür kalıyor.</p>
 *
 * <pre>
 * version|1
 * depth|3
 * seed|-482915...
 * player|12|7|14
 * inv|WEAPON|0|0|Çelik Kılıç|4|sword_steel
 * enemy|SKELETON|20|11|7|10|4|1
 * seen|0000111100...
 * floor|2|918273...|1|41.5|true
 * fseen|0011111000...
 * fenemy|IMP|9|3|4|5|2|0
 * </pre>
 *
 * <p>Sürüm satırı ilk satırda: biçim ileride değişirse eski kaydı sessizce
 * yanlış okumak yerine reddedebilelim diye.</p>
 *
 * <p>{@code floor} satırı bir <em>blok</em> açıyor: ondan sonra gelen
 * {@code fseen}, {@code fenemy} ve {@code fground} satırları o kata ait. Tek
 * bir düz metin biçiminde iç içe yapı kurmanın en ucuz yolu bu; alternatifi
 * her satıra derinliği tekrar yazmaktı, o da dosyayı hem şişirir hem tutarsız
 * derinlik yazma ihtimali açardı.</p>
 */
public class SaveFile {

    /** Yazılan dosya sürümü; geri dönüş sayacı eklenince 9 oldu. */
    private static final int VERSION = 9;

    private static final String SEPARATOR = "|";
    private static final String SPLIT_PATTERN = "\\|";

    private final Path path;

    /** Varsayılan konum: kullanıcının ev dizininde {@code .cryptdelver/save.txt}. */
    public SaveFile() {
        this(Path.of(System.getProperty("user.home"), ".cryptdelver", "save.txt"));
    }

    public SaveFile(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public boolean exists() {
        return Files.isRegularFile(path);
    }

    /** Kaydı diske yazar; klasör yoksa oluşturur. */
    public void write(SaveData data) throws IOException {
        List<String> lines = new ArrayList<>();

        lines.add(line("version", String.valueOf(VERSION)));
        lines.add(line("depth", String.valueOf(data.depth())));
        lines.add(line("seed", String.valueOf(data.seed())));
        lines.add(line("generator", String.valueOf(data.generatorIndex())));
        lines.add(line("gold", String.valueOf(data.gold())));
        lines.add(line("elapsed", String.valueOf(data.elapsedSeconds())));
        lines.add(line("player", String.valueOf(data.playerX()), String.valueOf(data.playerY()),
                String.valueOf(data.playerHp()), String.valueOf(data.playerMaxHp())));
        lines.add(line("equipped", String.valueOf(data.equippedWeaponSlot()),
                String.valueOf(data.equippedArmorSlot())));
        lines.add(line("returns", String.valueOf(data.returns())));

        for (SaveData.ItemData item : data.inventory()) {
            lines.add(itemLine("inv", item));
        }
        for (SaveData.ItemData item : data.groundItems()) {
            lines.add(itemLine("ground", item));
        }
        for (SaveData.EnemyData enemy : data.enemies()) {
            lines.add(enemyLine("enemy", enemy));
        }
        lines.add(line("seen", data.visionMask()));

        for (SaveData.FloorData floor : data.visitedFloors()) {
            lines.add(line("floor", String.valueOf(floor.depth()), String.valueOf(floor.seed()),
                    String.valueOf(floor.generatorIndex()), String.valueOf(floor.floorSeconds()),
                    String.valueOf(floor.awake())));
            lines.add(line("fseen", floor.visionMask()));

            for (SaveData.ItemData item : floor.groundItems()) {
                lines.add(itemLine("fground", item));
            }
            for (SaveData.EnemyData enemy : floor.enemies()) {
                lines.add(enemyLine("fenemy", enemy));
            }
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    /**
     * Kaydı okur.
     *
     * @return dosya yoksa boş
     * @throws IOException dosya okunamazsa ya da biçim bozuksa
     */
    public Optional<SaveData> read() throws IOException {
        if (!exists()) {
            return Optional.empty();
        }

        int depth = 1;
        long seed = 0;
        int generatorIndex = 0;
        int gold = 0;
        double elapsed = 0;
        int playerX = 0;
        int playerY = 0;
        int playerHp = 1;
        int playerMaxHp = 0;
        int weaponSlot = SaveData.NO_SLOT;
        int armorSlot = SaveData.NO_SLOT;
        int returns = 0;
        String visionMask = "";
        List<SaveData.ItemData> inventory = new ArrayList<>();
        List<SaveData.ItemData> ground = new ArrayList<>();
        List<SaveData.EnemyData> enemies = new ArrayList<>();
        List<FloorBlock> floors = new ArrayList<>();

        try {
            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (rawLine.isBlank()) {
                    continue;
                }

                String[] parts = rawLine.split(SPLIT_PATTERN, -1);
                switch (parts[0]) {
                    case "version" -> requireSupportedVersion(parts[1]);
                    case "depth" -> depth = Integer.parseInt(parts[1]);
                    case "seed" -> seed = Long.parseLong(parts[1]);
                    case "generator" -> generatorIndex = Integer.parseInt(parts[1]);
                    case "gold" -> gold = Integer.parseInt(parts[1]);
                    case "elapsed" -> elapsed = Double.parseDouble(parts[1]);
                    case "player" -> {
                        playerX = Integer.parseInt(parts[1]);
                        playerY = Integer.parseInt(parts[2]);
                        playerHp = Integer.parseInt(parts[3]);
                        // Azami can sürüm 5 ile geldi; eski kayıtlarda yok.
                        playerMaxHp = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
                    }
                    case "equipped" -> {
                        weaponSlot = Integer.parseInt(parts[1]);
                        armorSlot = Integer.parseInt(parts[2]);
                        // Sürüm 2 ve 3 kalkan/kask slotu da yazıyordu; ikisi de
                        // oyundan kalktı, o alanlar varsa yok sayılıyor.
                    }
                    case "inv" -> inventory.add(parseItem(parts));
                    case "ground" -> ground.add(parseItem(parts));
                    case "enemy" -> enemies.add(parseEnemy(parts));
                    case "returns" -> returns = Integer.parseInt(parts[1]);
                    case "seen" -> visionMask = parts[1];
                    case "floor" -> floors.add(new FloorBlock(
                            Integer.parseInt(parts[1]),
                            Long.parseLong(parts[2]),
                            Integer.parseInt(parts[3]),
                            Double.parseDouble(parts[4]),
                            Boolean.parseBoolean(parts[5])));
                    case "fseen" -> openFloor(floors).visionMask = parts[1];
                    case "fground" -> openFloor(floors).groundItems.add(parseItem(parts));
                    case "fenemy" -> openFloor(floors).enemies.add(parseEnemy(parts));
                    default -> throw new IOException("Tanınmayan kayıt satırı: " + parts[0]);
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IOException("Kayıt dosyası bozuk: " + path, e);
        }

        List<SaveData.FloorData> visitedFloors = new ArrayList<>();
        for (FloorBlock block : floors) {
            visitedFloors.add(block.toData());
        }

        return Optional.of(new SaveData(depth, seed, generatorIndex, gold, elapsed,
                playerX, playerY, playerHp, playerMaxHp, weaponSlot, armorSlot,
                returns, visionMask, inventory, ground, enemies, visitedFloors));
    }

    /**
     * Açık kat bloğu: {@code fseen}, {@code fground} ve {@code fenemy} satırları
     * bir üstlerindeki {@code floor} satırına ait.
     *
     * <p>Kendi yazdığımız dosyada bu satırlar {@code floor} olmadan gelemez ama
     * elle düzenlenmiş bir dosya bozuk olabilir; o zaman sessizce yanlış okumak
     * yerine "bozuk" diyoruz — bütün okuyucunun yaklaşımı bu.</p>
     */
    private FloorBlock openFloor(List<FloorBlock> floors) throws IOException {
        if (floors.isEmpty()) {
            throw new IOException("Kat satırı olmadan kat içeriği: " + path);
        }
        return floors.get(floors.size() - 1);
    }

    /** Okurken doldurulan kat; okuma bitince {@link SaveData.FloorData} oluyor. */
    private static final class FloorBlock {

        private final int depth;
        private final long seed;
        private final int generatorIndex;
        private final double floorSeconds;
        private final boolean awake;
        private final List<SaveData.ItemData> groundItems = new ArrayList<>();
        private final List<SaveData.EnemyData> enemies = new ArrayList<>();
        private String visionMask = "";

        private FloorBlock(int depth, long seed, int generatorIndex, double floorSeconds,
                           boolean awake) {
            this.depth = depth;
            this.seed = seed;
            this.generatorIndex = generatorIndex;
            this.floorSeconds = floorSeconds;
            this.awake = awake;
        }

        private SaveData.FloorData toData() {
            return new SaveData.FloorData(depth, seed, generatorIndex, floorSeconds, awake,
                    visionMask, groundItems, enemies);
        }
    }

    /** Kaydı siler; dosya yoksa sessizce geçer. */
    public void delete() throws IOException {
        Files.deleteIfExists(path);
    }

    /**
     * Sürümü doğrular.
     *
     * <p>Sürüm 1 kayıtları da okunuyor: aradaki tek fark kalkan slotu ve
     * okuyucu o alan yokken varsayılanı kullanıyor. Bilinmeyen bir sürümü ise
     * sessizce yanlış okumaktansa reddediyoruz.</p>
     */
    private void requireSupportedVersion(String value) throws IOException {
        int version = Integer.parseInt(value);
        if (version < 1 || version > VERSION) {
            throw new IOException("Kayıt sürümü desteklenmiyor: " + version + " (en fazla " + VERSION + ")");
        }
    }

    /**
     * Eşya satırını okur.
     *
     * <p>Dayanıklılık ve yükseltme sürüm 6, büyü sürüm 7 ile geldi; daha eski
     * satırlarda bu alanlar yok ve parça büyüsüz, sağlam sayılıyor. Alan
     * eklemeye devam edebilmemizin nedeni satır sonundaki eksik alanları
     * varsayılanla doldurmamız — eski kayıtlar bu yüzden hâlâ açılıyor.</p>
     */
    private SaveData.ItemData parseItem(String[] parts) {
        return new SaveData.ItemData(
                parts[1],
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                parts[4],
                Integer.parseInt(parts[5]),
                parts[6],
                parts.length > 7 ? Integer.parseInt(parts[7]) : SaveData.UNKNOWN_DURABILITY,
                parts.length > 8 ? Integer.parseInt(parts[8]) : 0,
                parts.length > 9 ? parts[9] : "");
    }

    private SaveData.EnemyData parseEnemy(String[] parts) {
        return new SaveData.EnemyData(
                parts[1],
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                Integer.parseInt(parts[5]),
                Integer.parseInt(parts[6]),
                Integer.parseInt(parts[7]));
    }

    private String enemyLine(String tag, SaveData.EnemyData enemy) {
        return line(tag, enemy.kind(), String.valueOf(enemy.x()), String.valueOf(enemy.y()),
                String.valueOf(enemy.hp()), String.valueOf(enemy.maxHp()),
                String.valueOf(enemy.attack()), String.valueOf(enemy.defense()));
    }

    private String itemLine(String tag, SaveData.ItemData item) {
        return line(tag, item.kind(), String.valueOf(item.x()), String.valueOf(item.y()),
                item.name(), String.valueOf(item.value()), item.spriteName(),
                String.valueOf(item.durability()), String.valueOf(item.upgradeLevel()),
                item.enchantment());
    }

    private String line(String tag, String... fields) {
        return tag + SEPARATOR + String.join(SEPARATOR, fields);
    }
}
