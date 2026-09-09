package com.cryptdelver.game;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Bomb;
import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Combatant;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.Equipment;
import com.cryptdelver.entity.EscapePotion;
import com.cryptdelver.entity.FuryPotion;
import com.cryptdelver.entity.Goblin;
import com.cryptdelver.entity.Gold;
import com.cryptdelver.entity.HastePotion;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.LegendWeapon;
import com.cryptdelver.entity.Orc;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Saman;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.entity.Wizard;
import com.cryptdelver.entity.Zombi;
import com.cryptdelver.persistence.SaveData;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Vision;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /** Bırakıldığı hâliyle saklanan bir kat. */
    private record VisitedFloor(Floor floor, Vision vision, double floorSeconds, boolean awake) {
    }

    /** Vampirlik büyüsünün öldürme başına verdiği can. */
    private static final int VAMPIRISM_HEAL = 2;

    /** Diken büyüsünün vurana yansıttığı hasar. */
    private static final int THORNS_DAMAGE = 1;

    /** Yenilenme büyüsü: kaç saniyede bir kaç can. */
    private static final double REGEN_INTERVAL = 5.0;
    private static final int REGEN_AMOUNT = 1;

    /**
     * Zindanın sabrı: katta bu kadar saniye kaldıktan sonra takviye gelmeye
     * başlıyor.
     *
     * <p>Bu olmadan <b>katı tamamen temizlemek her zaman en doğru hamleydi</b>:
     * düşmanlar yeniden doğmuyor, ganimet sınırlı, yani kalmanın hiçbir riski
     * yoktu. Bu da kurduğumuz bütün ekonomiyi zayıflatıyordu — yıpranma, altın,
     * iksir saklamak, hepsi "zaten hepsini alırım" diye çözülüyordu.</p>
     *
     * <p>Doksan saniye, katı gezip toplamaya yeten ama köşede bekleyip iksir
     * doldurmaya yetmeyen bir süre. Sonrasında her yirmi saniyede bir düşman
     * ekleniyor: baskı yavaş ama artan, yani "yeter, merdivene gidiyorum"
     * gerçek bir karar oluyor.</p>
     */
    private static final double FLOOR_PATIENCE = 90.0;
    private static final double REINFORCE_INTERVAL = 20.0;

    /** Takviyeler kattaki düşman sayısını bu sınırın üstüne çıkarmıyor. */
    private static final int REINFORCE_LIMIT = 24;

    private final Player player;
    private final Inventory inventory = new Inventory();
    private final MessageLog messageLog = new MessageLog();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Item> groundItems = new ArrayList<>();
    private final Random random = new Random();

    /**
     * Katları kuran taraf; sabit haritayla kurulmuş oyunlarda {@code null}.
     *
     * <p>Kat kurulumu buraya taşındı çünkü "oyunun kuralları" ile "kat nasıl
     * doğar" farklı sorular: kurallar her karede işliyor, kat kurulumu kat
     * başına bir kez. İkisi aynı sınıftayken hangi alanın hangi işe ait olduğu
     * okunmuyordu.</p>
     */
    private final FloorBuilder floors;

    private Dungeon dungeon;
    private int generatorIndex;
    private long currentSeed;
    private double elapsedSeconds;
    private int gold;
    private int depth = 1;
    private Position stairs;
    private Boss boss;
    private Wizard wizard;
    private Position lastPickupTile;
    private boolean paused;
    private boolean forgeOpen;
    private boolean won;
    private double regenTimer;
    private Vision vision;
    private double floorSeconds;
    private double reinforceTimer;
    private boolean dungeonAwake;
    private Position upStairs;

    /**
     * Gezilmiş ama şu anda yüklü olmayan katlar: derinlik -> kat, o katın görüşü
     * ve zindanın sabrı.
     *
     * <p>Bulunduğun kat burada <em>değil</em>, oyunun kendi alanlarında duruyor;
     * inip çıkarken biri diskten diğerine geçiyor. Böylece "hangisi doğru"
     * sorusu hiç doğmuyor.</p>
     *
     * <p>Bu hafıza kayda da yazılıyor. Bir süre yazmıyorduk — dosyayı büyütmemek
     * için — ama o zaman kaydedip yükleyen oyuncu geri döndüğü katı yepyeni
     * ganimetle buluyordu. Harita yerine tohum yazdığımız için kat başına maliyet
     * birkaç yüz bayt; kuralı delen bir taviz için fazla ucuz bir bedel.</p>
     */
    private final Map<Integer, VisitedFloor> visited = new HashMap<>();
    private SoundListener sounds = SoundListener.SILENT;
    private final Settings settings = new Settings();

    /** Hazır bir harita ile kurar; testler ve sabit kat senaryoları için. */
    public Game(Dungeon dungeon, Player player) {
        this.dungeon = dungeon;
        this.player = player;
        this.floors = null;
        this.vision = new Vision(dungeon.getWidth(), dungeon.getHeight());
        refreshVision();
    }

    /** Üreticilerle kurar; ilk kat hemen üretilir, oyuncu, düşmanlar ve eşyalar yerleşir. */
    public Game(List<DungeonGenerator> generators, int floorWidth, int floorHeight, Player player) {
        if (generators.isEmpty()) {
            throw new IllegalArgumentException("En az bir zindan üreticisi gerekli");
        }
        this.floors = new FloorBuilder(generators, floorWidth, floorHeight);
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
        return FloorBuilder.isBossFloor(depth);
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
        if (isOver() || !isPlayerOnStairs() || floors == null) {
            return false;
        }
        if (isStairsLocked()) {
            messageLog.add(boss.getName() + " merdiveni tutuyor; önce onu geç.");
            return false;
        }

        // Son katın merdiveni aşağı değil dışarı çıkıyor: zindanın bir sonu
        // olması, "sonsuza kadar in" hissinden çok daha iyi bir hedef veriyor.
        if (depth >= FloorTheme.MAX_DEPTH) {
            won = true;
            sounds.play(SoundEffect.STAIRS);
            messageLog.addImportant("Kriptten çıktın. Zindan arkanda kaldı.");
            return true;
        }

        travelTo(depth + 1);
        messageLog.add(depth + ". kata indin (" + getTheme().getLabel() + ").");
        return true;
    }

    /** Oyuncu yukarı çıkan merdivenin üstünde mi. */
    public boolean isPlayerOnUpStairs() {
        return upStairs != null && upStairs.equals(player.getTile());
    }

    /**
     * Bir üst kata çıkar.
     *
     * <p>Geri dönebilmek altına bir anlam kazandırdı: kesende para birikince
     * yukarıdaki büyücüye dönüp takımına büyü bastırabiliyorsun. Öncesinde
     * altın yalnızca bulunduğun katta büyücü varsa işe yarıyordu, yani çoğu
     * kat boyunca ölü bir kaynaktı.</p>
     *
     * <p>Bedava değil: geri dönmek zaman alıyor ve zindanın sabrı kat başına
     * <em>hatırlanıyor</em> — uyanmış bir kata geri dönersen takviyeler
     * kaldığı yerden devam ediyor.</p>
     *
     * @return çıkıldıysa {@code true}
     */
    public boolean ascend() {
        if (isOver() || won || !isPlayerOnUpStairs() || floors == null || depth <= 1) {
            return false;
        }

        travelTo(depth - 1);
        messageLog.add(depth + ". kata çıktın (" + getTheme().getLabel() + ").");
        return true;
    }

    /**
     * Verilen kata gider.
     *
     * <p>Ayrıldığın kat <em>hatırlanıyor</em>: düşmanları, eşyaları, keşfettiğin
     * yerler ve zindanın sabrı olduğu gibi duruyor. Katı her seferinde yeniden
     * üretmek çok daha kolay olurdu ama sonsuz altın demek olurdu — çık, in,
     * kat yepyeni ganimetle karşına gelsin.</p>
     */
    private void travelTo(int target) {
        visited.put(depth, new VisitedFloor(snapshot(), vision, floorSeconds, dungeonAwake));

        boolean goingDown = target > depth;
        depth = target;
        sounds.play(SoundEffect.STAIRS);

        VisitedFloor known = visited.remove(depth);
        if (known == null) {
            generateFloor(random.nextLong());
            announceBoss();
        } else {
            resume(known);
        }

        // İnerken geldiğin yere, çıkarken indiğin merdivene varıyorsun.
        player.setTile(goingDown ? upStairs : stairs);
        lastPickupTile = player.getTile();
        refreshVision();
    }

    /** O anki katı, bırakıldığı hâliyle bir değere çevirir. */
    private Floor snapshot() {
        return new Floor(currentSeed, dungeon, upStairs, stairs, wizard, boss,
                enemies, groundItems);
    }

    /** Daha önce gezilmiş bir katı bırakıldığı hâliyle geri yükler. */
    private void resume(VisitedFloor known) {
        Floor floor = known.floor();

        currentSeed = floor.seed();
        generatorIndex = floors.generatorForDepth(depth);
        dungeon = floor.dungeon();
        upStairs = floor.spawn();
        stairs = floor.stairs();
        wizard = floor.wizard();
        boss = floor.boss();

        enemies.clear();
        enemies.addAll(floor.enemies());
        groundItems.clear();
        groundItems.addAll(floor.groundItems());

        vision = known.vision();
        floorSeconds = known.floorSeconds();
        dungeonAwake = known.awake();
        reinforceTimer = 0;
    }

    /**
     * Oyuncunun bu kattan ne gördüğü ve neyi hatırladığı.
     *
     * <p>Görüş oyunun durumunun parçası, çizimin değil: hangi düşmanın
     * göründüğü bir kural sorusu ve testten sorulabilmesi gerekiyor.</p>
     */
    public Vision getVision() {
        return vision;
    }

    /** Bu katın bölgesi; görüntüsünü ve adını buradan alıyor. */
    public FloorTheme getTheme() {
        return FloorTheme.forDepth(depth);
    }

    /**
     * Bu kat mağara mı: oyulmuş, dar ve dolambaçlı.
     *
     * <p>Hangi üreticinin kullanıldığını sormak yerine burada tek bir soru
     * var; çizim katmanının üretici listesini tanıması gerekmiyor.</p>
     */
    public boolean isCaveFloor() {
        return generatorIndex == 1;
    }

    /** Oyun kazanıldı mı: son katın bossu geçilip dışarı çıkıldı mı. */
    public boolean isWon() {
        return won;
    }

    /** Son kattayız; merdiven aşağı değil dışarı çıkıyor. */
    public boolean isFinalFloor() {
        return depth >= FloorTheme.MAX_DEPTH;
    }

    /** O an kullanılan üretici; sabit haritayla kurulduysa {@code null}. */
    public DungeonGenerator getCurrentGenerator() {
        return floors == null ? null : floors.generatorAt(generatorIndex);
    }

    /**
     * Güncel katın tohumu. Kaydetme sisteminde haritanın tamamı yerine bu tek
     * sayıyı saklamak yetecek.
     */
    public long getCurrentSeed() {
        return currentSeed;
    }

    /**
     * Ses olaylarını dinleyecek tarafı takar.
     *
     * <p>Varsayılan sessiz dinleyici; testler ve pencere açılmadan çalışan
     * senaryolar ses kütüphanesine hiç dokunmuyor.</p>
     */
    public void setSoundListener(SoundListener listener) {
        this.sounds = listener == null ? SoundListener.SILENT : listener;
    }

    /**
     * Oyuncunun tercihleri (ses seviyesi, sessize alma).
     *
     * <p>Oyun durumundan ayri yasiyor: yeniden baslamak ayarlari
     * sifirlamiyor.</p>
     */
    public Settings getSettings() {
        return settings;
    }

    /** Oyun duraklatıldı mı. */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Duraklatmayı açıp kapatır.
     *
     * <p>Duraklatma oyun durumunun parçası, ekranın değil: hem çizim katmanı
     * bunu okuyup perdeyi gösteriyor hem de {@link #update(double)} hiçbir şey
     * ilerletmeden dönüyor. Oyun bittiyse duraklatmanın anlamı yok.</p>
     */
    public void togglePause() {
        // Büyücü ekranı açıksa ESC önce onu kapatıyor: tek "geri" tuşu.
        if (forgeOpen) {
            forgeOpen = false;
            return;
        }

        if (!isOver()) {
            paused = !paused;
        }
    }

    /**
     * Zaman akıyor mu.
     *
     * <p>Duraklatma ve büyücü ekranı aynı şeyi istiyor: dünya dursun. İkisini
     * tek soruda topladım, yoksa {@link #update(double)} her yeni ekranla
     * birlikte bir koşul daha biriktirirdi.</p>
     */
    public boolean isFrozen() {
        return paused || forgeOpen || won;
    }

    // -------------------------------------------------------------- büyücü

    /** Bu kattaki büyücü; yoksa {@code null}. */
    public Wizard getWizard() {
        return wizard;
    }

    /** Büyücü ekranı açık mı. */
    public boolean isForgeOpen() {
        return forgeOpen;
    }

    /**
     * Oyuncu büyücüyle konuşacak kadar yakın mı.
     *
     * <p>Komşu kare yetiyor, üstüne basmak gerekmiyor — büyücü kendi karesini
     * tuttuğu için zaten üstüne basılamaz.</p>
     */
    public boolean isNearWizard() {
        return wizard != null && wizard.tileDistanceTo(player) <= 1;
    }

    /**
     * Büyücü ekranını açıp kapatır.
     *
     * <p>Uzaktan açılmıyor: büyücü boss katlarının tek sabit noktası, oraya
     * gitmek işin bir parçası.</p>
     */
    public void toggleForge() {
        if (forgeOpen) {
            forgeOpen = false;
            return;
        }

        if (isOver() || paused) {
            return;
        }

        if (!isNearWizard()) {
            messageLog.add("Yakında büyücü yok. Büyücüler boss katlarında.");
            return;
        }

        forgeOpen = true;
    }

    public void repairWeapon() {
        repair(player.getEquippedWeapon(), "Silah");
    }

    public void repairArmor() {
        repair(player.getEquippedArmor(), "Zırh");
    }

    public void upgradeWeapon() {
        upgrade(player.getEquippedWeapon(), "Silah");
    }

    public void upgradeArmor() {
        upgrade(player.getEquippedArmor(), "Zırh");
    }

    /**
     * Parçayı tam dayanıklılığa getirir.
     *
     * <p>Kısmi tamir yok: "40 altınlık tamir" gibi bir seçenek hem ekranı hem
     * kararı gereksiz karmaşıklaştırırdı. Fiyat zaten eksik kadar.</p>
     */
    private void repair(Equipment item, String label) {
        if (!requireForge() || item == null) {
            messageLog.add(label + " kuşanmadın.");
            return;
        }

        if (!item.needsRepair()) {
            messageLog.add(item.getDisplayName() + " zaten sapasağlam.");
            return;
        }

        int cost = Forge.repairCost(item);
        if (!spendGold(cost)) {
            return;
        }

        item.repair();
        messageLog.add(item.getDisplayName() + " tamir edildi (-" + cost + " altın).");
        sounds.play(SoundEffect.EQUIP);
    }

    /**
     * Parçayı bir kademe yükseltir.
     *
     * <p>Tavan {@link Equipment#upgradeCeiling(int)}: bu katta bossun bırakacağı
     * parçanın seviyesi. Yükseltme seni ayakta tutuyor ama sıçramayı hâlâ boss
     * yaptırıyor — altın biriktirerek katları atlamak yok.</p>
     */
    private void upgrade(Equipment item, String label) {
        if (!requireForge() || item == null) {
            messageLog.add(label + " kuşanmadın.");
            return;
        }

        if (!item.canUpgrade(depth)) {
            messageLog.add(item.getDisplayName() + " bu katta daha ileri gitmiyor; bossu geç.");
            return;
        }

        int cost = Forge.upgradeCost(item);
        if (!spendGold(cost)) {
            return;
        }

        item.upgrade();
        messageLog.add(item.getDisplayName() + " yükseltildi (-" + cost + " altın).");
        sounds.play(SoundEffect.EQUIP);
    }

    public void enchantWeapon(Enchantment enchantment) {
        enchant(player.getEquippedWeapon(), enchantment, "Silah");
    }

    public void enchantArmor(Enchantment enchantment) {
        enchant(player.getEquippedArmor(), enchantment, "Zırh");
    }

    /**
     * Parçaya büyü basar.
     *
     * <p>Bir parçada bir büyü duruyor: yenisi eskisinin yerine geçiyor ve tam
     * fiyat ödeniyor. Yani fikir değiştirmek serbest ama bedava değil.</p>
     */
    private void enchant(Equipment item, Enchantment enchantment, String label) {
        if (!requireForge() || item == null) {
            messageLog.add(label + " kuşanmadın.");
            return;
        }

        if (!item.accepts(enchantment)) {
            messageLog.add(label.toLowerCase() + " bu büyüyü taşımaz.");
            return;
        }

        if (item.hasEnchantment(enchantment)) {
            messageLog.add(item.getDisplayName() + " zaten " + enchantment.getLabel()
                    + " taşıyor.");
            return;
        }

        if (!spendGold(enchantment.getCost())) {
            return;
        }

        Enchantment replaced = item.enchant(enchantment);
        sounds.play(SoundEffect.EQUIP);

        if (replaced == null) {
            messageLog.add(item.getDisplayName() + " artık " + enchantment.getLabel()
                    + " taşıyor (-" + enchantment.getCost() + " altın).");
        } else {
            messageLog.add(replaced.getLabel() + " silindi, yerine " + enchantment.getLabel()
                    + " basıldı (-" + enchantment.getCost() + " altın).");
        }
    }

    private boolean requireForge() {
        if (forgeOpen) {
            return true;
        }
        messageLog.add("Önce büyücüye git.");
        return false;
    }

    /** Yetiyorsa keseden düşer; yetmiyorsa uyarır ve hiçbir şey yapmaz. */
    private boolean spendGold(int cost) {
        if (gold < cost) {
            messageLog.add("Altın yetmiyor: " + cost + " gerekiyor, " + gold + " var.");
            return false;
        }

        gold -= cost;
        return true;
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
        if (isFrozen()) {
            return;
        }

        player.update(this, delta);
        refreshVision();

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
            regenerate(delta);
            stirTheDungeon(delta);
        }
    }

    /**
     * Katta oyalanınca zindan takviye göndermeye başlar.
     *
     * <p>Sayaç kat başına sıfırlanıyor, yani baskı "bu katta ne kadar
     * kaldın"la ilgili — toplam oyun süresiyle değil. Aşağı inmek sayacı
     * sıfırlıyor, yani ilerlemek gerçekten rahatlatıyor.</p>
     */
    private void stirTheDungeon(double delta) {
        if (floors == null || won) {
            return;
        }

        floorSeconds += delta;
        if (floorSeconds < FLOOR_PATIENCE) {
            return;
        }

        // İlk uyarı bir kez: sonrası zaten karşına çıkacak.
        if (!dungeonAwake) {
            dungeonAwake = true;
            messageLog.addImportant("Zindan seni fark etti. Oyalanma.");
        }

        reinforceTimer += delta;
        if (reinforceTimer < REINFORCE_INTERVAL) {
            return;
        }
        reinforceTimer = 0;
        sendReinforcement();
    }

    /** Oyuncudan uzakta yeni bir düşman doğurur. */
    private void sendReinforcement() {
        if (enemies.size() >= REINFORCE_LIMIT) {
            return;
        }

        Set<Position> taken = new HashSet<>();
        taken.add(player.getTile());
        for (Enemy enemy : enemies) {
            taken.add(enemy.getTile());
        }
        if (wizard != null) {
            taken.add(wizard.getTile());
        }

        Position spot = floors.findSpawnAwayFrom(dungeon, player.getTile(), taken);
        if (spot == null) {
            return;
        }

        enemies.add(floors.createEnemyForDepth(spot, depth, settings.getDifficulty()));
    }

    /** Zindan uyandı mı; ekran bunu uyarı olarak gösteriyor. */
    public boolean isDungeonAwake() {
        return dungeonAwake;
    }

    /** Bu katta geçen süre, saniye. */
    public double getFloorSeconds() {
        return floorSeconds;
    }

    /**
     * Yenilenme büyüsü: zırh taşıyorsa belli aralıklarla 1 can.
     *
     * <p>Sayaç oyunun içinde, zırhın içinde değil: büyü zırhın <em>özelliği</em>
     * ama akan zaman oyunun işi. Zırhı çıkarınca sayaç sıfırlanıyor, yani
     * "zırhı tak-çıkar yaparak can biriktirmek" diye bir şey yok.</p>
     */
    private void regenerate(double delta) {
        if (!player.hasArmorEnchantment(Enchantment.YENILENME)) {
            regenTimer = 0;
            return;
        }

        regenTimer += delta;
        if (regenTimer < REGEN_INTERVAL) {
            return;
        }

        regenTimer -= REGEN_INTERVAL;
        if (player.getHp() < player.getMaxHp()) {
            player.heal(REGEN_AMOUNT);
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
        // Büyücü dövüşmez ama karesini tutar; üstünden geçilmiyor.
        if (wizard != null && wizard != ignored && wizard.occupies(x, y)) {
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
        pickUpItemsExcept(null);
    }

    /**
     * Ayağının altındakileri toplar; verilen parçayı atlar.
     *
     * @param skipped az önce yere bırakılan parça; {@code null} olabilir
     */
    private void pickUpItemsExcept(Item skipped) {
        for (Item item : List.copyOf(groundItems)) {
            if (item == skipped || !item.getTile().equals(player.getTile())) {
                continue;
            }

            if (item.goesToInventory()) {
                if (!inventory.add(item)) {
                    messageLog.add("Çantan dolu: " + item.getName() + " yerde kaldı.");
                    continue;
                }
                messageLog.add(item.getName() + " aldın.");
                sounds.play(SoundEffect.PICKUP);
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

        boolean consumed = item.use(this);
        sounds.play(consumed ? SoundEffect.POTION : SoundEffect.EQUIP);

        if (consumed) {
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

        // Bıraktığın anda ayağının altındakini alıyorsun: çanta doluyken
        // takas etmek için kareden çıkıp geri gelmek gerekiyordu. Yeni
        // bıraktığın parça hariç, yoksa onu geri toplardın.
        pickUpItemsExcept(item);
        return true;
    }

    /**
     * Oyuncunun çevresindeki düşmanları patlatır; bomba bunu çağırıyor.
     *
     * <p>Hasar zırhtan geçmiyor: bu bir vuruş değil patlama. Böylece derin
     * katlardaki kalın zırhlı düşmanlara karşı da işe yarıyor ve bomba
     * "saklamaya değer" bir eşya olarak kalıyor.</p>
     *
     * <p>Kopya üzerinde geziliyor: patlama öldürdükçe liste değişiyor.</p>
     */
    public void detonate(int radius, int damage) {
        int hit = 0;

        for (Enemy enemy : List.copyOf(enemies)) {
            if (enemy.tileDistanceTo(player) > radius) {
                continue;
            }

            enemy.takeDamage(damage);
            hit++;
            if (!enemy.isAlive()) {
                buryEnemy(enemy);
            }
        }

        sounds.play(SoundEffect.KILL);
        messageLog.add(hit == 0
                ? "Bomba boşluğa patladı."
                : "Bomba patladı: " + hit + " düşman vuruldu.");
    }

    /**
     * Oyuncuyu merdivenin başına ışınlar; kaçış iksiri bunu çağırıyor.
     *
     * <p>Merdivenin bossla tutulu olması engel değil — iksir seni oraya
     * bırakıyor, gerisi sana kalıyor. "Her durumda güvenli" bir çıkış
     * olsaydı boss katlarının anlamı kalmazdı.</p>
     *
     * @return ışınlandıysa {@code true}; merdiven yoksa eşya harcanmıyor
     */
    public boolean teleportToStairs() {
        if (stairs == null) {
            messageLog.add("Bu katta merdiven yok.");
            return false;
        }

        player.setTile(stairs);
        lastPickupTile = player.getTile();
        refreshVision();
        sounds.play(SoundEffect.STAIRS);
        messageLog.add("Kaçış iksiri: merdivenin başındasın.");
        return true;
    }

    /**
     * Yerini yenisi alan parçayı çantadan çıkarıp ayağının dibine bırakır.
     *
     * <p>Daha iyi bir zırh bulunca eskisi çantada duruyordu ve slotlar birkaç
     * katta doluyordu — oysa geri dönüp kötü zırhı giymek diye bir şey yok.
     * Yere bırakmak hem çantayı açıyor hem de fikrini değiştirirsen parça hâlâ
     * orada duruyor.</p>
     *
     * <p>Bırakılan kare {@code lastPickupTile} olarak işaretleniyor: yoksa aynı
     * karede durduğun için parçayı anında geri toplardın.</p>
     */
    public void discardToGround(Item item) {
        if (item == null || !inventory.remove(item)) {
            return;
        }

        item.setTile(player.getTile());
        addGroundItem(item);
        lastPickupTile = player.getTile();
        messageLog.add(item.getName() + " yere bırakıldı.");
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
        int reach = player.getAttackRange();
        for (Enemy enemy : enemies) {
            if (enemy.tileDistanceTo(player) <= reach) {
                targets.add(enemy);
            }
        }

        sounds.play(SoundEffect.SWING);

        if (targets.isEmpty()) {
            messageLog.add("Kılıcın boşluğu kesti.");
            return;
        }

        sounds.play(SoundEffect.HIT);
        wearGear(player.getEquippedWeapon(), "Kılıcın");

        for (Enemy enemy : targets) {
            int damage = resolveDamage(player, enemy);
            enemy.takeDamage(damage);
            messageLog.add(enemy.getName() + " " + damage + " hasar aldı.");

            if (!enemy.isAlive()) {
                buryEnemy(enemy);
                drainLife();
            }
        }
    }

    /**
     * Ölen düşmanı listeden çıkarır, ganimetini bıraktırır ve duyurur.
     *
     * <p>Ölüm iki yerden geliyor: oyuncunun vuruşu ve Diken büyüsünün
     * yansıttığı hasar. İkisinin de aynı işleri yapması gerekiyordu, o yüzden
     * tek yerde toplandı.</p>
     */
    private void buryEnemy(Enemy enemy) {
        removeEnemy(enemy);
        messageLog.add(enemy.getName() + " yere serildi.");
        sounds.play(SoundEffect.KILL);

        // Ganimeti düşman kendi bırakıyor; burada tür kontrolü yok.
        enemy.onDeath(this);
        if (enemy == boss) {
            boss = null;
        }
    }

    /** Vampirlik büyüsü: öldürülen her düşman biraz can veriyor. */
    private void drainLife() {
        Weapon weapon = player.getEquippedWeapon();
        if (weapon == null || !weapon.hasEnchantment(Enchantment.VAMPIRLIK)) {
            return;
        }

        player.heal(VAMPIRISM_HEAL);
        messageLog.add("Vampirlik " + VAMPIRISM_HEAL + " can emdi.");
    }

    /**
     * Düşmanın oyuncuya vuruşu; {@code Enemy.update} içinden çağrılır.
     * Saldırı kararı düşmanın, hasar hesabı oyunun işi.
     */
    public void enemyAttacksPlayer(Enemy enemy) {
        int damage = resolveDamage(enemy, player);
        player.takeDamage(damage);
        messageLog.add(enemy.getName() + " sana " + damage + " hasar vurdu.");
        sounds.play(SoundEffect.HURT);
        wearGear(player.getEquippedArmor(), "Zırhın");
        reflectThorns(enemy);

        if (!player.isAlive()) {
            messageLog.addImportant("Zindanda öldün.");
            sounds.play(SoundEffect.DEATH);
        }
    }

    /**
     * Diken büyüsü: sana vurana hasar yansıtır.
     *
     * <p>Yansıyan hasar düşmanı öldürebiliyor — kalabalığın ortasında hiç
     * vurmadan da kayıp verdirmenin yolu bu.</p>
     */
    private void reflectThorns(Enemy enemy) {
        Armor armor = player.getEquippedArmor();
        if (armor == null || !armor.hasEnchantment(Enchantment.DIKEN) || !enemy.isAlive()) {
            return;
        }

        enemy.takeDamage(THORNS_DAMAGE);
        messageLog.add("Diken " + enemy.getName() + " üstünde " + THORNS_DAMAGE + " hasar açtı.");

        if (!enemy.isAlive()) {
            buryEnemy(enemy);
        }
    }

    /**
     * Kuşanılan parçayı bir puan aşındırır.
     *
     * <p>Silah isabet ettikçe, zırh darbe yedikçe yıpranıyor: ikisi de
     * <em>kullanıma</em> bağlı, geçen süreye değil. Böylece kaçarak oynayan
     * oyuncu zırhını, uzaktan bekleyen oyuncu kılıcını yormuyor — yıpranma
     * yaptığın şeyin bedeli oluyor.</p>
     *
     * <p>Kırılma anını mesajla duyuruyoruz; sessizce yarı güce düşmek oyuncuya
     * "bir şeyler ters gidiyor ama neden bilmiyorum" hissi verirdi.</p>
     */
    private void wearGear(Equipment item, String label) {
        if (item == null) {
            return;
        }

        if (item.wear()) {
            messageLog.addImportant(label + " kırıldı! Büyücüye uğrayana kadar hiçbir işe yaramaz.");
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

    /** Aynı derinlikte yeni bir kat üretir. */
    public void regenerateFloor() {
        if (floors != null) {
            generateFloor(random.nextLong());
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

        return new SaveData(depth, currentSeed, generatorIndex, gold, elapsedSeconds,
                player.getTileX(), player.getTileY(), player.getHp(), player.getMaxHp(),
                inventory.slotOf(player.getEquippedWeapon()),
                inventory.slotOf(player.getEquippedArmor()),
                vision.exportRemembered(),
                savedInventory, savedGround, describeAll(enemies),
                captureVisitedFloors());
    }

    /**
     * Gezilmiş katları kayıt verisine çevirir.
     *
     * <p>Her katın haritası yine yazılmıyor, tohumu yazılıyor: yüklerken
     * {@link FloorBuilder#layout} aynı döşemeyi, aynı merdiveni ve aynı
     * büyücüyü veriyor. Üstüne düşmanlar, yerdeki eşyalar, keşfettiğin kareler
     * ve zindanın o kattaki sabrı ekleniyor — yani geri döndüğünde kat, kaydı
     * almadan önce bıraktığın hâlde.</p>
     */
    private List<SaveData.FloorData> captureVisitedFloors() {
        List<SaveData.FloorData> saved = new ArrayList<>();

        for (Map.Entry<Integer, VisitedFloor> entry : visited.entrySet()) {
            int floorDepth = entry.getKey();
            VisitedFloor known = entry.getValue();
            Floor floor = known.floor();

            List<SaveData.ItemData> items = new ArrayList<>();
            for (Item item : floor.groundItems()) {
                items.add(describe(item));
            }

            saved.add(new SaveData.FloorData(floorDepth, floor.seed(),
                    floors.generatorForDepth(floorDepth), known.floorSeconds(), known.awake(),
                    known.vision().exportRemembered(), items, describeAll(floor.enemies())));
        }
        return saved;
    }

    private List<SaveData.EnemyData> describeAll(List<Enemy> toDescribe) {
        List<SaveData.EnemyData> described = new ArrayList<>();
        for (Enemy enemy : toDescribe) {
            described.add(new SaveData.EnemyData(enemy.getSaveKind(),
                    enemy.getTileX(), enemy.getTileY(),
                    enemy.getHp(), enemy.getMaxHp(), enemy.getAttackPower(), enemy.getDefense()));
        }
        return described;
    }

    /**
     * Kaydedilmiş durumu yükler: harita tohumdan yeniden üretilir, üstündeki
     * her şey kayıttan kurulur.
     */
    public void applySave(SaveData data) {
        if (floors == null) {
            throw new IllegalStateException("Kayıt yüklemek için zindan üreticisi gerekli");
        }

        depth = data.depth();
        generatorIndex = floors.clampGeneratorIndex(data.generatorIndex());
        gold = data.gold();
        elapsedSeconds = data.elapsedSeconds();

        // Yalnızca döşeme kuruluyor: düşmanlar ve eşyalar kayıttan geliyor.
        // Aynı tohum aynı haritayı, merdiveni ve büyücüyü verdiği için bunlar
        // kayıt dosyasında saklanmak zorunda değil.
        adopt(floors.layout(generatorIndex, depth, data.seed()));

        restoreVisited(data);
        inventory.clear();

        restorePlayer(data);

        for (SaveData.ItemData item : data.inventory()) {
            Item restored = createItem(item);
            if (restored != null) {
                inventory.add(restored);
            }
        }
        equipFromSlots(data);

        for (Item item : createItems(data.groundItems())) {
            addGroundItem(item);
        }

        List<Enemy> restoredEnemies = createEnemies(data.enemies(), depth);
        for (Enemy enemy : restoredEnemies) {
            addEnemy(enemy);
        }
        boss = bossAmong(restoredEnemies);

        // Keşif de kaydın parçası: yükleyen oyuncu gezdiği koridorları yeniden
        // bulmak zorunda kalmıyor, ama görmediği yerler hâlâ karanlık.
        vision.importRemembered(data.visionMask());

        lastPickupTile = player.getTile();
        messageLog.add(depth + ". kattaki kayıt yüklendi.");
    }

    /**
     * Kayıttaki gezilmiş katları hafızaya kurar.
     *
     * <p>Her kat için döşeme tohumdan yeniden üretiliyor, üstüne kayıttaki
     * düşmanlar ve eşyalar konuyor. Bulunduğun kat atlanıyor: o zaten kaydın
     * gövdesinden yüklendi ve şu anda oyunun alanlarında duruyor.</p>
     */
    private void restoreVisited(SaveData data) {
        visited.clear();

        for (SaveData.FloorData saved : data.visitedFloors()) {
            if (saved.depth() == depth) {
                continue;
            }

            int index = floors.clampGeneratorIndex(saved.generatorIndex());
            Floor layout = floors.layout(index, saved.depth(), saved.seed());
            List<Enemy> floorEnemies = createEnemies(saved.enemies(), saved.depth());
            Floor restored = layout.filledWith(bossAmong(floorEnemies), floorEnemies,
                    createItems(saved.groundItems()));

            Vision seen = new Vision(layout.dungeon().getWidth(), layout.dungeon().getHeight());
            seen.importRemembered(saved.visionMask());

            visited.put(saved.depth(),
                    new VisitedFloor(restored, seen, saved.floorSeconds(), saved.awake()));
        }
    }

    /** Tanınmayan türleri atlayarak eşya listesini kurar. */
    private List<Item> createItems(List<SaveData.ItemData> saved) {
        List<Item> restored = new ArrayList<>();
        for (SaveData.ItemData item : saved) {
            Item created = createItem(item);
            if (created != null) {
                restored.add(created);
            }
        }
        return restored;
    }

    private List<Enemy> createEnemies(List<SaveData.EnemyData> saved, int floorDepth) {
        List<Enemy> restored = new ArrayList<>();
        for (SaveData.EnemyData enemy : saved) {
            restored.add(createEnemy(enemy, floorDepth));
        }
        return restored;
    }

    /**
     * Listedeki boss; yoksa {@code null}.
     *
     * <p>Boss ayrıca tutuluyor çünkü can barı ve merdiven kilidi onu adıyla
     * soruyor. Kayıtta ayrı bir alan açmak yerine düşmanların arasından
     * buluyoruz: iki yerde saklanan bir bilgi er geç çelişir.</p>
     */
    private Boss bossAmong(List<Enemy> candidates) {
        for (Enemy enemy : candidates) {
            if (enemy instanceof Boss found) {
                return found;
            }
        }
        return null;
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
                item.getName(), item.getSaveValue(), item.getSpriteName(),
                item.getSaveDurability(), item.getSaveUpgradeLevel(), item.getSaveEnchantment());
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
            case "BOMB" -> new Bomb(data.x(), data.y());
            case "HASTE" -> new HastePotion(data.x(), data.y());
            case "FURY" -> new FuryPotion(data.x(), data.y());
            case "ESCAPE" -> new EscapePotion(data.x(), data.y());
            case "GOLD" -> new Gold(data.x(), data.y(), data.value());
            case "LEGEND" -> restoreGear(new LegendWeapon(data.x(), data.y()), data);
            case "WEAPON" -> restoreGear(
                    new Weapon(data.x(), data.y(), data.name(), data.value(), data.spriteName()), data);
            case "ARMOR" -> restoreGear(
                    new Armor(data.x(), data.y(), data.name(), data.value(), data.spriteName()), data);
            default -> null;
        };
    }

    /**
     * Taze üretilmiş parçayı kayıttaki yıpranma ve yükseltme durumuna getirir.
     *
     * <p>Dayanıklılık bilinmiyorsa (sürüm 6 öncesi kayıt) parçaya
     * dokunmuyoruz: yeni üretildiği için zaten dolu.</p>
     */
    private Equipment restoreGear(Equipment gear, SaveData.ItemData data) {
        if (data.durability() != SaveData.UNKNOWN_DURABILITY) {
            gear.restoreState(data.durability(), data.upgradeLevel());
        } else if (data.upgradeLevel() > 0) {
            gear.restoreState(gear.getMaxDurability(), data.upgradeLevel());
        }

        restoreEnchantments(gear, data.enchantment());
        return gear;
    }

    /**
     * Kayıttaki büyü etiketlerini çözüp parçaya basar.
     *
     * <p>Alan virgülle ayrılmış: efsanevi kılıç iki büyü taşıyabildiği için
     * tek etiket yetmiyordu. Ayırıcı olarak virgül seçildi çünkü kayıt
     * dosyasının alan ayırıcısı {@code |} ve iç içe geçmemeleri gerekiyor.</p>
     *
     * <p>Tanımadığımız bir etiket kaydı bozmuyor, yalnızca o büyü atlanıyor:
     * ileride bir büyü oyundan kalkarsa eski kayıtlar hâlâ okunabilsin diye —
     * kalkan ve kaskta izlediğimiz yolun aynısı.</p>
     */
    private void restoreEnchantments(Equipment gear, String labels) {
        if (labels == null || labels.isBlank()) {
            return;
        }

        for (String label : labels.split(",")) {
            for (Enchantment candidate : gear.availableEnchantments()) {
                if (candidate.name().equals(label.trim())) {
                    gear.enchant(candidate);
                    break;
                }
            }
        }
    }

    /**
     * Etiketten düşman üretir ve kayıttaki değerlere getirir.
     *
     * <p>Bonusları değil toplam değerleri sakladığımız için, taze düşmanla
     * kayıt arasındaki farkı ekliyoruz. Tür değerlerini sonradan dengelemek
     * eski kayıtları bozmuyor.</p>
     *
     * @param floorDepth düşmanın <em>hangi kata</em> ait olduğu; bulunduğun kat
     *        olmak zorunda değil, gezilmiş katlar da buradan kuruluyor
     */
    private Enemy createEnemy(SaveData.EnemyData data, int floorDepth) {
        Enemy enemy = switch (data.kind()) {
            // "RAT": bu düşman İmp olarak yeniden adlandırılmadan önceki kayıtlar.
            case "IMP", "RAT" -> new Imp(data.x(), data.y());
            case "GOBLIN" -> new Goblin(data.x(), data.y());
            case "ZOMBI" -> new Zombi(data.x(), data.y());
            case "SAMAN" -> new Saman(data.x(), data.y());
            case "ORC" -> new Orc(data.x(), data.y());
            case "SKELETON" -> new Skeleton(data.x(), data.y());
            // Bossun gövdesi ve adı kaçıncı boss olduğuna bağlı; kayıtta ayrı
            // bir alan tutmak yerine derinlikten çıkarıyoruz.
            case "BOSS" -> new Boss(data.x(), data.y(), FloorBuilder.bossNumber(floorDepth));
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
        won = false;
        visited.clear();
        elapsedSeconds = 0;
        enemies.clear();
        groundItems.clear();
        messageLog.clear();
        regenerateFloor();
        messageLog.add("Yeniden zindana indin.");
    }

    /**
     * Yeni bir kat üretip devralır.
     *
     * <p>Katı {@link FloorBuilder} kuruyor; burası yalnızca sonucu sahipleniyor
     * ve oyuncuyu yerleştiriyor. Üretici seçimi katın kendi kuralı, ama kayıt
     * yüklerken kayıttan geldiği için alan burada tutuluyor.</p>
     */
    private void generateFloor(long seed) {
        generatorIndex = floors.generatorForDepth(depth);

        adopt(floors.build(generatorIndex, depth, seed, settings.getDifficulty()));
        announceBoss();
    }

    /**
     * Kurulmuş katı oyunun durumuna geçirir.
     *
     * <p>Kat bir <em>değer</em> olarak geliyor, yani kurulum sırasında oyunun
     * hiçbir alanı değişmiyor; devralma tek bir yerde ve gözle görülür.</p>
     */
    private void adopt(Floor floor) {
        currentSeed = floor.seed();
        dungeon = floor.dungeon();
        stairs = floor.stairs();
        wizard = floor.wizard();
        boss = floor.boss();

        enemies.clear();
        enemies.addAll(floor.enemies());
        groundItems.clear();
        groundItems.addAll(floor.groundItems());

        upStairs = floor.spawn();
        player.setTile(floor.spawn());
        lastPickupTile = player.getTile();

        // Yeni kat baştan karanlık: bir önceki katın hatırladıkları buraya
        // taşınmamalı.
        vision = new Vision(dungeon.getWidth(), dungeon.getHeight());
        refreshVision();

        // Zindanın sabrı kat başına yeniliyor: inmek gerçekten rahatlatıyor.
        floorSeconds = 0;
        reinforceTimer = 0;
        dungeonAwake = false;
    }

    /**
     * Görüşü oyuncunun bulunduğu kareye göre tazeler.
     *
     * <p>{@link Vision} aynı kareden ikinci kez çağrıldığında hiçbir şey
     * yapmıyor, o yüzden bunu her karede çağırmak sorun değil.</p>
     */
    private void refreshVision() {
        vision.update(dungeon, player.getTileX(), player.getTileY());
    }

    /** Boss katına inince uyarı; sesle birlikte. */
    private void announceBoss() {
        if (boss == null) {
            return;
        }

        messageLog.addImportant(boss.getName() + " merdiveni tutuyor. Yavaş — vur ve geri çekil.");
        sounds.play(SoundEffect.BOSS);
    }
}
