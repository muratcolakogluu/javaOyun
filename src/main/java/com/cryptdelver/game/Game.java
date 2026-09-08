package com.cryptdelver.game;

import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Combatant;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.Gold;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Rat;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Weapon;
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

    /** İlk katta iskelet olasılığı; derinlikle artar, kalanı fare. */
    private static final double BASE_SKELETON_CHANCE = 0.3;
    private static final double SKELETON_CHANCE_PER_DEPTH = 0.06;
    private static final double MAX_SKELETON_CHANCE = 0.8;

    /** Derin katlarda düşmanlar kaç katta bir güçlenir. */
    private static final int DEPTHS_PER_HP_BONUS = 2;
    private static final int DEPTHS_PER_ATTACK_BONUS = 3;

    /** Kaç katta bir boss çıkar. */
    private static final int FLOORS_PER_BOSS = 5;

    /** Boss katlarında sıradan düşman sayısı bu oranda azalır. */
    private static final double BOSS_FLOOR_ENEMY_RATIO = 0.6;

    private static final int POTIONS_PER_FLOOR = 4;
    private static final int GOLD_PILES_PER_FLOOR = 5;
    private static final int MIN_GOLD = 5;
    private static final int MAX_GOLD = 25;

    /** Kılıç yerine balta çıkma olasılığı. */
    private static final double AXE_CHANCE = 0.35;

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
        generateFloor(random.nextLong());
        messageLog.add(depth + ". kata indin. Burası daha kalabalık.");
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
        pickUpItems();

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
        return Math.max(1, swing - defender.getDefense());
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
        currentSeed = seed;
        dungeon = generators.get(generatorIndex).generate(floorWidth, floorHeight, seed);
        enemies.clear();
        groundItems.clear();
        boss = null;

        player.setTile(dungeon.findWalkableNear(floorWidth / 2, floorHeight / 2));

        // Merdiven, doğduğun yerden yürüyerek gidilebilen en uzak kareye konur.
        stairs = dungeon.findFarthestWalkableFrom(player.getTile());
        dungeon.setTile(stairs.x(), stairs.y(), Tile.STAIRS_DOWN);

        populateFloor();
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
            boss.strengthen((depth - 1) / DEPTHS_PER_HP_BONUS, (depth - 1) / DEPTHS_PER_ATTACK_BONUS);
            addEnemy(boss);
            messageLog.add(boss.getName() + " merdiveni tutuyor.");
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
        double skeletonChance = Math.min(MAX_SKELETON_CHANCE,
                BASE_SKELETON_CHANCE + SKELETON_CHANCE_PER_DEPTH * (depth - 1));

        Enemy enemy = random.nextDouble() < skeletonChance
                ? new Skeleton(spot.x(), spot.y())
                : new Rat(spot.x(), spot.y());

        enemy.strengthen((depth - 1) / DEPTHS_PER_HP_BONUS, (depth - 1) / DEPTHS_PER_ATTACK_BONUS);
        return enemy;
    }

    private void placeItems(List<Position> spots, Set<Position> used) {
        int potions = 0;
        int goldPiles = 0;
        boolean weaponPlaced = false;

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
                addGroundItem(random.nextDouble() < AXE_CHANCE
                        ? Weapon.battleAxe(spot.x(), spot.y())
                        : Weapon.rustySword(spot.x(), spot.y()));
                weaponPlaced = true;
            } else {
                return;
            }

            used.add(spot);
        }
    }
}
