package com.cryptdelver.game;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Combatant;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.Goblin;
import com.cryptdelver.entity.Gold;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Orc;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.persistence.SaveData;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Tile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Oyunun durumu ve kuralları: harita, oyuncu, düşmanlar, eşyalar, hareket ve
 * savaş.
 *
 * <p>Zaman gerçek zamanlı akar ({@link #update(double)} her karede çağrılır),
 * ama hareket ızgaraya kilitlidir: herkes kare kare adım atar, adım ortasında
 * yön değiştiremez. Sıra yoktur — düşmanlar sen dursan da yürür.</p>
 *
 * <p>Arayüzden (JavaFX) tamamen bağımsızdır — ekran yalnızca bu nesneyi okur ve
 * girdiyi buradaki metotlara çevirir, böylece kurallar pencere açmadan test
 * edilebiliyor.</p>
 */
public class Game {

    /** İlk kattaki düşman sayısı; her kat bir artar. */
    private static final int BASE_ENEMIES_PER_FLOOR = 8;

    /** Ne kadar inersen in, bir kata bundan fazla düşman doğmaz. */
    private static final int MAX_ENEMIES_PER_FLOOR = 20;

    /** Düşmanlar oyuncunun bu kadar yakınına doğmaz (kare). */
    private static final int MIN_SPAWN_DISTANCE = 8;

    /** Yeni türlerin oyuna girdiği katlar. */
    private static final int GOBLIN_MIN_DEPTH = 2;
    private static final int ORC_MIN_DEPTH = 4;

    /** Her inişte kazanılan azami can; oyuncunun tek kalıcı büyümesi. */
    private static final int MAX_HP_PER_FLOOR = 2;

    /** Derin katlarda düşmanlar kaç katta bir güçlenir. */
    private static final int DEPTHS_PER_HP_BONUS = 2;
    private static final int DEPTHS_PER_ATTACK_BONUS = 3;
    private static final int DEPTHS_PER_DEFENSE_BONUS = 4;

    /** Kaç katta bir boss çıkar. */
    private static final int FLOORS_PER_BOSS = 5;

    /** Boss katlarında sıradan düşman sayısı bu oranda azalır. */
    private static final double BOSS_FLOOR_ENEMY_RATIO = 0.6;

    private static final int POTIONS_PER_FLOOR = 4;
    private static final int GOLD_PILES_PER_FLOOR = 5;
    private static final int MIN_GOLD = 5;
    private static final int MAX_GOLD = 25;

    private final Player player;
    private final Inventory inventory = new Inventory();
    private final MessageLog messageLog = new MessageLog();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Item> groundItems = new ArrayList<>();
    private final List<DungeonGenerator> generators;
    private final Random random = new Random();
    private final int floorWidth;
    private final int floorHeight;

    private Dungeon dungeon;
    private int generatorIndex;
    private long currentSeed;
    private double elapsedSeconds;
    private int gold;
    private int depth = 1;
    private Position stairs;
    private Boss boss;
    private Position lastPickupTile;

    /** Hazır bir harita ile kurar; testler ve sabit kat senaryoları için. */
    public Game(Dungeon dungeon, Player player) {
        this.dungeon = dungeon;
        this.player = player;
        this.generators = List.of();
        this.floorWidth = dungeon.getWidth();
        this.floorHeight = dungeon.getHeight();
    }

    /** Üreticilerle kurar; ilk kat hemen üretilir, oyuncu, düşmanlar ve eşyalar yerleşir. */
    public Game(List<DungeonGenerator> generators, int floorWidth, int floorHeight, Player player) {
        if (generators.isEmpty()) {
            throw new IllegalArgumentException("En az bir zindan üreticisi gerekli");
        }
        this.generators = List.copyOf(generators);
        this.floorWidth = floorWidth;
        this.floorHeight = floorHeight;
        this.player = player;
        generateFloor(random.nextLong());
        messageLog.add("Zindana indin. Boşluk vurur, 1-8 eşya kullanır.");
    }

    // ---------------------------------------------------------------- durum

    public Dungeon getDungeon() {
        return dungeon;
    }

    public Player getPlayer() {
        return player;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public MessageLog getMessageLog() {
        return messageLog;
    }

    public List<Enemy> getEnemies() {
        return List.copyOf(enemies);
    }

    /** Haritada yerde duran eşyalar. */
    public List<Item> getGroundItems() {
        return List.copyOf(groundItems);
    }

    /** Kesedeki altın. */
    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    /** Oyunun başından beri geçen süre, saniye. */
    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    /** Kaçıncı kattayız; 1 en üst kat. */
    public int getDepth() {
        return depth;
    }

    /** Bu kattaki iniş merdiveninin yeri; merdiven yoksa {@code null}. */
    public Position getStairs() {
        return stairs;
    }

    /** Oyuncu merdivenin üstünde mi; ekran buna göre ipucu gösteriyor. */
    public boolean isPlayerOnStairs() {
        return stairs != null && stairs.equals(player.getTile());
    }

    /** Bu katta boss var mı. */
    public boolean isBossFloor() {
        return depth % FLOORS_PER_BOSS == 0;
    }

    /** Kattaki yaşayan boss; yoksa {@code null}. */
    public Boss getBoss() {
        return boss;
    }

    /** Merdiven boss tarafından tutuluyor mu; öyleyse inilemez. */
    public boolean isStairsLocked() {
        return boss != null && boss.isAlive();
    }

    /**
     * Bir alt kata iner.
     *
     * <p>Can, çanta ve kese korunur — inmek bir ödül değil, risktir: aşağıda
     * daha çok ve daha sert düşman var. Merdivenin üstünde değilsen hiçbir şey
     * olmaz.</p>
     *
     * @return inildiyse {@code true}
     */
    public boolean descend() {
        if (isOver() || !isPlayerOnStairs() || generators.isEmpty()) {
            return false;
        }
        if (isStairsLocked()) {
            messageLog.add(boss.getName() + " merdiveni tutuyor; önce onu geç.");
            return false;
        }

        depth++;
        player.gainMaxHp(MAX_HP_PER_FLOOR);
        generateFloor(random.nextLong());

        messageLog.add(depth + ". kata indin (+" + MAX_HP_PER_FLOOR + " azami can).");
        return true;
    }

    /** O an kullanılan üretici; sabit haritayla kurulduysa {@code null}. */
    public DungeonGenerator getCurrentGenerator() {
        return generators.isEmpty() ? null : generators.get(generatorIndex);
    }

    /**
     * Güncel katın tohumu. Kaydetme sisteminde haritanın tamamı yerine bu tek
     * sayıyı saklamak yetecek.
     */
    public long getCurrentSeed() {
        return currentSeed;
    }

    /** Oyuncu öldüyse oyun biter. */
    public boolean isOver() {
        return !player.isAlive();
    }

    /** Verilen kareyi tutan canlı düşman; yoksa {@code null}. */
    public Enemy enemyAt(int x, int y) {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && enemy.occupies(x, y)) {
                return enemy;
            }
        }
        return null;
    }

    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void removeEnemy(Enemy enemy) {
        enemies.remove(enemy);
    }

    /** Haritaya yere düşmüş bir eşya koyar. */
    public void addGroundItem(Item item) {
        groundItems.add(item);
    }

    // ------------------------------------------------------------ oyun akışı

    /**
     * Oyunu bir kare ilerletir.
     *
     * @param delta son kareden bu yana geçen süre, saniye
     */
    public void update(double delta) {
        player.update(this, delta);

        // Toplama yalnızca yeni bir kareye <em>girildiğinde</em> deneniyor.
        // Her karede denemek iki soruna yol açıyordu: yere bıraktığın eşya
        // anında geri alınıyordu ve çanta doluyken mesaj kaydı akıyordu.
        if (!player.getTile().equals(lastPickupTile)) {
            lastPickupTile = player.getTile();
            pickUpItems();
        }

        // Kopya üzerinde geziyoruz: bir düşman hamlesi sırasında ölüp listeden düşebilir.
        for (Enemy enemy : List.copyOf(enemies)) {
            enemy.update(this, delta);
        }

        if (!isOver()) {
            elapsedSeconds += delta;
        }
    }

    /**
     * Varlığın komşu kareye adımını başlatmayı dener.
     *
     * <p>Hedef kare duvarsa ya da başkası tutuyorsa adım başlamaz. "Tutmak",
     * adım halindeki varlığın hem çıktığı hem girdiği kareyi kapsar; iki
     * varlığın aynı kareye aynı anda girmesi böyle engelleniyor.</p>
     *
     * @return adım başladıysa {@code true}
     */
    public boolean tryStartStep(Entity entity, int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return false;
        }

        int targetX = entity.getTileX() + dx;
        int targetY = entity.getTileY() + dy;

        if (!isTileFree(targetX, targetY, entity)) {
            return false;
        }

        entity.beginStep(targetX, targetY);
        return true;
    }

    /** Kare yürünebilir ve (verilen varlık dışında) boş mu. Eşyalar engel değil. */
    public boolean isTileFree(int x, int y, Entity ignored) {
        if (!dungeon.isWalkable(x, y)) {
            return false;
        }
        if (player != ignored && player.occupies(x, y)) {
            return false;
        }

        for (Enemy enemy : enemies) {
            if (enemy != ignored && enemy.occupies(x, y)) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- eşya

    /**
     * Oyuncunun bastığı karedeki eşyaları toplar.
     *
     * <p>Ayrı bir "al" tuşu yok: üstüne basmak yetiyor. Çanta doluysa eşya
     * yerde kalır — ama altın çantaya girmediği için her hâlükârda alınır.</p>
     */
    private void pickUpItems() {
        for (Item item : List.copyOf(groundItems)) {
            if (!item.getTile().equals(player.getTile())) {
                continue;
            }

            if (item.goesToInventory()) {
                if (!inventory.add(item)) {
                    messageLog.add("Çantan dolu: " + item.getName() + " yerde kaldı.");
                    continue;
                }
                messageLog.add(item.getName() + " aldın.");
            }

            item.onPickup(this);
            groundItems.remove(item);
        }
    }

    /**
     * Çantadaki bir eşyayı kullanır.
     *
     * <p>Ne olacağını eşyanın kendisi biliyor: iksir iyileştirip tükenir, silah
     * kuşanılıp çantada kalır. Burada tür kontrolü yok.</p>
     */
    public void useItem(int slot) {
        if (isOver()) {
            return;
        }

        Item item = inventory.get(slot);
        if (item == null) {
            return;
        }

        if (item.use(this)) {
            inventory.remove(item);
        }
    }

    /**
     * Çantadaki bir eşyayı bulunduğun kareye bırakır.
     *
     * <p>Kuşanılmış bir parçayı bırakmak onu üstünden de çıkarır — bunu eşyanın
     * kendisi hallediyor ({@code Item.onDrop}). Bıraktığın eşya, sen o kareden
     * çıkıp geri dönene kadar tekrar toplanmıyor; yoksa elinden bırakır
     * bırakmaz geri alırdın.</p>
     *
     * @return eşya bırakıldıysa {@code true}
     */
    public boolean dropItem(int slot) {
        if (isOver()) {
            return false;
        }

        Item item = inventory.get(slot);
        if (item == null) {
            return false;
        }

        inventory.remove(item);
        item.setTile(player.getTile());
        item.onDrop(this);
        addGroundItem(item);
        messageLog.add(item.getName() + " yere bıraktın.");

        lastPickupTile = player.getTile();
        return true;
    }

    // ---------------------------------------------------------------- savaş

    /**
     * Oyuncunun vuruşu: yan yana olduğu <em>tüm</em> düşmanlara isabet eder.
     *
     * <p>Yön seçimi yok, etrafa savuruyor. Kalabalığın ortasında kalınca yön
     * tutturmaya çalışmak sinir bozucu olurdu; savurmak hem adil hem
     * anlaşılır.</p>
     */
    public void playerAttacks() {
        List<Enemy> targets = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.tileDistanceTo(player) <= 1) {
                targets.add(enemy);
            }
        }

        if (targets.isEmpty()) {
            messageLog.add("Kılıcın boşluğu kesti.");
            return;
        }

        for (Enemy enemy : targets) {
            int damage = resolveDamage(player, enemy);
            enemy.takeDamage(damage);
            messageLog.add(enemy.getName() + " " + damage + " hasar aldı.");

            if (!enemy.isAlive()) {
                removeEnemy(enemy);
                messageLog.add(enemy.getName() + " yere serildi.");

                // Ganimeti düşman kendi bırakıyor; burada tür kontrolü yok.
                enemy.onDeath(this);
                if (enemy == boss) {
                    boss = null;
                }
            }
        }
    }

    /**
     * Düşmanın oyuncuya vuruşu; {@code Enemy.update} içinden çağrılır.
     * Saldırı kararı düşmanın, hasar hesabı oyunun işi.
     */
    public void enemyAttacksPlayer(Enemy enemy) {
        int damage = resolveDamage(enemy, player);
        player.takeDamage(damage);
        messageLog.add(enemy.getName() + " sana " + damage + " hasar vurdu.");

        if (!player.isAlive()) {
            messageLog.add("Zindanda öldün.");
        }
    }

    /**
     * Tek hasar boru hattı: saldıranın gücü, küçük bir sapma, savunanın zırhı.
     *
     * <p>Oyundaki <em>her</em> vuruş buradan geçiyor — oyuncununki de düşmanınki
     * de. Zırh sistemi geldiğinde formül yalnızca burada değişecek; savunma
     * değerini kimin nereden aldığı ({@code Combatant.getDefense()}) çağıranı
     * ilgilendirmiyor.</p>
     *
     * <p>Sapma olmasa aynı düşman hep aynı hasarı vururdu; alt sınır 1, yani
     * zırh ne kadar kalın olursa olsun vuruşlar tamamen boşa gitmiyor.</p>
     */
    private int resolveDamage(Combatant attacker, Combatant defender) {
        int swing = attacker.getAttackPower() - 1 + random.nextInt(3);
        return Math.max(minimumDamage(attacker), swing - defender.getDefense());
    }

    /**
     * Savunma ne kadar kalın olursa olsun geçen en az hasar.
     *
     * <p>Sabit 1 bırakırsak, zırh ve kalkan birlikte kuşanıldığında derin
     * katlardaki sert düşmanlar da tırmık atan bir imp kadar zararsız oluyordu.
     * Alt sınırı vuruş gücüyle ölçekleyince ağır vuranlar zırhı yine deliyor,
     * zayıf düşmanlar yine 1 vuruyor.</p>
     */
    private int minimumDamage(Combatant attacker) {
        return 1 + attacker.getAttackPower() / 5;
    }

    // ---------------------------------------------------------- kat yönetimi

    /** Aynı üreticiyle yeni bir kat üretir. */
    public void regenerateFloor() {
        if (!generators.isEmpty()) {
            generateFloor(random.nextLong());
        }
    }

    /** Sıradaki üreticiye geçer ve yeni bir kat üretir. */
    public void cycleGenerator() {
        if (!generators.isEmpty()) {
            generatorIndex = (generatorIndex + 1) % generators.size();
            regenerateFloor();
        }
    }

    // ------------------------------------------------------------ kaydetme

    /**
     * O anki durumu kayıt verisine çevirir.
     *
     * <p>Harita yazılmıyor, yalnızca tohumu: üreticiler aynı tohumdan aynı
     * haritayı üretiyor. Ama harita üstündeki her şey — düşmanların canı,
     * yerdeki eşyalar — yazılıyor; yoksa yükleme kat başına altın ve iksir
     * çiftliğine dönerdi.</p>
     */
    public SaveData captureSave() {
        List<Item> carried = inventory.getItems();

        List<SaveData.ItemData> savedInventory = new ArrayList<>();
        for (Item item : carried) {
            savedInventory.add(describe(item));
        }

        List<SaveData.ItemData> savedGround = new ArrayList<>();
        for (Item item : groundItems) {
            savedGround.add(describe(item));
        }

        List<SaveData.EnemyData> savedEnemies = new ArrayList<>();
        for (Enemy enemy : enemies) {
            savedEnemies.add(new SaveData.EnemyData(enemy.getSaveKind(),
                    enemy.getTileX(), enemy.getTileY(),
                    enemy.getHp(), enemy.getMaxHp(), enemy.getAttackPower(), enemy.getDefense()));
        }

        return new SaveData(depth, currentSeed, generatorIndex, gold, elapsedSeconds,
                player.getTileX(), player.getTileY(), player.getHp(), player.getMaxHp(),
                inventory.slotOf(player.getEquippedWeapon()),
                inventory.slotOf(player.getEquippedArmor()),
                savedInventory, savedGround, savedEnemies);
    }

    /**
     * Kaydedilmiş durumu yükler: harita tohumdan yeniden üretilir, üstündeki
     * her şey kayıttan kurulur.
     */
    public void applySave(SaveData data) {
        if (generators.isEmpty()) {
            throw new IllegalStateException("Kayıt yüklemek için zindan üreticisi gerekli");
        }

        depth = data.depth();
        generatorIndex = Math.floorMod(data.generatorIndex(), generators.size());
        gold = data.gold();
        elapsedSeconds = data.elapsedSeconds();

        buildFloor(data.seed());
        inventory.clear();

        restorePlayer(data);

        for (SaveData.ItemData item : data.inventory()) {
            Item restored = createItem(item);
            if (restored != null) {
                inventory.add(restored);
            }
        }
        equipFromSlots(data);

        for (SaveData.ItemData item : data.groundItems()) {
            Item restored = createItem(item);
            if (restored != null) {
                addGroundItem(restored);
            }
        }
        for (SaveData.EnemyData enemy : data.enemies()) {
            addEnemy(createEnemy(enemy));
        }

        lastPickupTile = player.getTile();
        messageLog.add(depth + ". kattaki kayıt yüklendi.");
    }

    private void restorePlayer(SaveData data) {
        player.restore();
        player.setTile(data.playerX(), data.playerY());

        // Derinlikle kazanilan azami can da geri yukleniyor; eski kayitlarda
        // bu alan yok, o zaman taban canla devam ediyoruz.
        int extraMaxHp = data.playerMaxHp() - player.getMaxHp();
        if (extraMaxHp > 0) {
            player.gainMaxHp(extraMaxHp);
        }

        int damage = player.getMaxHp() - data.playerHp();
        if (damage > 0) {
            player.takeDamage(damage);
        }
    }

    private void equipFromSlots(SaveData data) {
        Item weapon = inventory.get(data.equippedWeaponSlot());
        if (weapon instanceof Weapon) {
            player.equip((Weapon) weapon);
        }

        Item armor = inventory.get(data.equippedArmorSlot());
        if (armor instanceof Armor) {
            player.equip((Armor) armor);
        }


    }

    private SaveData.ItemData describe(Item item) {
        return new SaveData.ItemData(item.getSaveKind(), item.getTileX(), item.getTileY(),
                item.getName(), item.getSaveValue(), item.getSpriteName());
    }

    /**
     * Etiketten eşya üretir; kayıt biçimini nesnelere çeviren tek yer.
     *
     * <p>Tanınmayan bir tür kaydı çöpe atmıyor, yalnızca o eşyayı atlıyoruz
     * ({@code null} dönüyor). Eski kayıtlarda oyundan kaldırılmış türler
     * (kalkan, kask) olabilir; onlar yüzünden bütün kayıt okunamaz olmasın.</p>
     */
    private Item createItem(SaveData.ItemData data) {
        return switch (data.kind()) {
            case "POTION" -> new Potion(data.x(), data.y());
            case "GOLD" -> new Gold(data.x(), data.y(), data.value());
            case "WEAPON" -> new Weapon(data.x(), data.y(), data.name(), data.value(), data.spriteName());
            case "ARMOR" -> new Armor(data.x(), data.y(), data.name(), data.value(), data.spriteName());
            default -> null;
        };
    }

    /**
     * Etiketten düşman üretir ve kayıttaki değerlere getirir.
     *
     * <p>Bonusları değil toplam değerleri sakladığımız için, taze düşmanla
     * kayıt arasındaki farkı ekliyoruz. Tür değerlerini sonradan dengelemek
     * eski kayıtları bozmuyor.</p>
     */
    private Enemy createEnemy(SaveData.EnemyData data) {
        Enemy enemy = switch (data.kind()) {
            // "RAT": bu düşman İmp olarak yeniden adlandırılmadan önceki kayıtlar.
            case "IMP", "RAT" -> new Imp(data.x(), data.y());
            case "GOBLIN" -> new Goblin(data.x(), data.y());
            case "ORC" -> new Orc(data.x(), data.y());
            case "SKELETON" -> new Skeleton(data.x(), data.y());
            case "BOSS" -> {
                Boss restored = new Boss(data.x(), data.y());
                boss = restored;
                yield restored;
            }
            default -> throw new IllegalArgumentException("Bilinmeyen düşman türü: " + data.kind());
        };

        enemy.strengthen(
                data.maxHp() - enemy.getMaxHp(),
                data.attack() - enemy.getAttackPower(),
                data.defense() - enemy.getDefense());

        int damage = enemy.getMaxHp() - data.hp();
        if (damage > 0) {
            enemy.takeDamage(damage);
        }
        return enemy;
    }

    /** Ölümden sonra sıfırdan başlar: can, çanta, kese, derinlik ve süre sıfırlanır. */
    public void restart() {
        player.restore();
        inventory.clear();
        gold = 0;
        depth = 1;
        elapsedSeconds = 0;
        enemies.clear();
        groundItems.clear();
        messageLog.clear();
        regenerateFloor();
        messageLog.add("Yeniden zindana indin.");
    }

    private void generateFloor(long seed) {
        Position spawn = buildFloor(seed);

        player.setTile(spawn);
        lastPickupTile = player.getTile();

        populateFloor();
    }

    /**
     * Haritayı tohumdan üretir ve merdiveni yerleştirir; içini doldurmaz.
     *
     * <p>Yeni kat üretirken de kayıt yüklerken de aynı adımlar işlemeli, yoksa
     * kaydedilen katın merdiveni yüklenince başka yere düşerdi.</p>
     *
     * @return oyuncunun doğduğu kare
     */
    private Position buildFloor(long seed) {
        currentSeed = seed;
        dungeon = generators.get(generatorIndex).generate(floorWidth, floorHeight, seed);
        enemies.clear();
        groundItems.clear();
        boss = null;

        Position spawn = dungeon.findWalkableNear(floorWidth / 2, floorHeight / 2);

        // Merdiven, doğulan yerden yürüyerek gidilebilen en uzak kareye konur.
        stairs = dungeon.findFarthestWalkableFrom(spawn);
        dungeon.setTile(stairs.x(), stairs.y(), Tile.STAIRS_DOWN);

        return spawn;
    }

    /**
     * Katı düşman ve eşyalarla doldurur.
     *
     * <p>Uygun kareleri bir kez karıştırıp sırayla dağıtıyoruz; kullanılan
     * kareleri işaretlemek, iki şeyin aynı kareye konmasını kendiliğinden
     * engelliyor.</p>
     */
    private void populateFloor() {
        List<Position> spots = new ArrayList<>(dungeon.walkablePositions());
        Collections.shuffle(spots, random);

        Set<Position> used = new HashSet<>();
        used.add(player.getTile());
        if (stairs != null) {
            // Merdivenin üstü boş kalsın; eşya ya da düşmanla kapanmasın.
            used.add(stairs);
        }

        // Boss merdivenin üstünde doğar: geçmek için onu yenmen gerekiyor.
        if (isBossFloor() && stairs != null) {
            boss = new Boss(stairs.x(), stairs.y());
            applyDepthBonus(boss);
            addEnemy(boss);
            messageLog.add(boss.getName() + " merdiveni tutuyor. Yavaş — vur ve geri çekil.");
        }

        int target = enemyCountForDepth();
        int spawned = 0;
        for (Position spot : spots) {
            if (spawned >= target) {
                break;
            }
            if (used.contains(spot) || spot.manhattanDistance(player.getTile()) < MIN_SPAWN_DISTANCE) {
                continue;
            }

            used.add(spot);
            addEnemy(createEnemyForDepth(spot));
            spawned++;
        }

        placeItems(spots, used);
    }

    /**
     * Her kat bir düşman daha; belli bir sayıdan sonra artmıyor.
     *
     * <p>Boss katlarında sıradan düşman sayısı azaltılıyor: asıl tehdit boss ve
     * çağırdığı yaratıklar olsun, kalabalık boğmasın.</p>
     */
    private int enemyCountForDepth() {
        int count = Math.min(MAX_ENEMIES_PER_FLOOR, BASE_ENEMIES_PER_FLOOR + depth - 1);
        return isBossFloor() ? (int) Math.round(count * BOSS_FLOOR_ENEMY_RATIO) : count;
    }

    /**
     * Derinliğe uygun bir düşman üretir.
     *
     * <p>İki kaldıraç var: aşağı indikçe iskelet oranı artıyor (kalabalık
     * sertleşiyor) ve düşmanlar can/güç bonusu alıyor. Yeni tür yazmadan
     * zorluk eğrisi elde etmenin ucuz yolu bu.</p>
     */
    private Enemy createEnemyForDepth(Position spot) {
        Enemy enemy = rollEnemyKind(spot);
        applyDepthBonus(enemy);
        return enemy;
    }

    /**
     * Derinliğe göre ağırlıklı düşman seçimi.
     *
     * <p>Katlar tür değiştirerek zorlaşıyor, yalnızca sayı büyüterek değil:
     * imp yukarıda kalabalık, aşağı indikçe yerini iskelete ve orka bırakıyor.
     * Goblin 2., ork 4. kattan itibaren giriyor; ağırlıkları derinlikle
     * arttığı için karşına çıkan sürü de yavaş yavaş sertleşiyor.</p>
     */
    private Enemy rollEnemyKind(Position spot) {
        int impWeight = Math.max(1, 7 - depth);
        int skeletonWeight = 2 + depth;
        int goblinWeight = depth >= GOBLIN_MIN_DEPTH ? 3 : 0;
        int orcWeight = depth >= ORC_MIN_DEPTH ? depth - 2 : 0;

        int roll = random.nextInt(impWeight + skeletonWeight + goblinWeight + orcWeight);

        if (roll < impWeight) {
            return new Imp(spot.x(), spot.y());
        }
        roll -= impWeight;

        if (roll < skeletonWeight) {
            return new Skeleton(spot.x(), spot.y());
        }
        roll -= skeletonWeight;

        return roll < goblinWeight ? new Goblin(spot.x(), spot.y()) : new Orc(spot.x(), spot.y());
    }

    /**
     * Derinliğe göre can/güç/savunma bonusu uygular.
     *
     * <p>Savunma en yavaş artan değer: hızlı artsaydı kılıcın kademesi geride
     * kaldığı anda düşmanlar delinmez olurdu. Oyuncunun ekipman kademesi
     * {@link LootTable} ile 3 katta bir yükseliyor, düşman savunması 4 katta
     * bir — yani ilerleme hep oyuncunun lehine, ama fark kapanmıyor.</p>
     */
    private void applyDepthBonus(Enemy enemy) {
        enemy.strengthen(
                (depth - 1) / DEPTHS_PER_HP_BONUS,
                (depth - 1) / DEPTHS_PER_ATTACK_BONUS,
                (depth - 1) / DEPTHS_PER_DEFENSE_BONUS);
    }

    /**
     * Kata iksir, altın ve birer ekipman parçası dağıtır.
     *
     * <p>Ekipmanın kademesi rastgele değil, tamamen derinliğe bağlı
     * ({@link LootTable}): 2. katta bulduğun zırh 4. kattakinden iyi olamaz.</p>
     */
    private void placeItems(List<Position> spots, Set<Position> used) {
        int tier = LootTable.tierForDepth(depth);
        int potions = 0;
        int goldPiles = 0;
        boolean weaponPlaced = false;
        boolean armorPlaced = false;

        for (Position spot : spots) {
            if (used.contains(spot)) {
                continue;
            }

            if (potions < POTIONS_PER_FLOOR) {
                addGroundItem(new Potion(spot.x(), spot.y()));
                potions++;
            } else if (goldPiles < GOLD_PILES_PER_FLOOR) {
                int amount = MIN_GOLD + random.nextInt(MAX_GOLD - MIN_GOLD + 1);
                addGroundItem(new Gold(spot.x(), spot.y(), amount));
                goldPiles++;
            } else if (!weaponPlaced) {
                addGroundItem(LootTable.weaponForTier(tier, spot.x(), spot.y()));
                weaponPlaced = true;
            } else if (!armorPlaced) {
                addGroundItem(LootTable.armorForTier(tier, spot.x(), spot.y()));
                armorPlaced = true;

            } else {
                return;
            }

            used.add(spot);
        }
    }
}
