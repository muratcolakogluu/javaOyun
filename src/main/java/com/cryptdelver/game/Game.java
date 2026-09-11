package com.cryptdelver.game;

import com.cryptdelver.entity.Archer;
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
import com.cryptdelver.entity.Projectile;
import com.cryptdelver.entity.Saman;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.entity.Merchant;
import com.cryptdelver.entity.Wizard;
import com.cryptdelver.entity.Zombi;
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

    /**
     * Her geri dönüşün zindanın sabrından götürdüğü saniye.
     *
     * <p>Merdivenden yukarı çıkmak bedava bir hamleydi: temizlenmiş katlar
     * boş, geri dönüş yolu tehlikesiz, yani "aşağıda altın topla, yukarıdaki
     * büyücüye dön" hiçbir risk taşımıyordu. Oyunun kolaylaşması bunun
     * sonucuydu.</p>
     *
     * <p>Artık zindan her dönüşü hatırlıyor ve <em>bütün</em> katlarda daha
     * çabuk uyanıyor. İlk dönüş neredeyse bedava (90 → 75 saniye), dördüncüsü
     * seni her katta yarım dakikada takviyelerle karşılıyor. Yani geri dönmek
     * hâlâ mümkün ve hâlâ doğru bir hamle olabilir — ama artık bir karar.</p>
     */
    private static final double PATIENCE_LOSS_PER_RETURN = 15.0;

    /** Zindanın sabrı bunun altına inmiyor; geri dönüş cezası da olsa nefes payı kalıyor. */
    private static final double MIN_FLOOR_PATIENCE = 30.0;

    /** Takviyeler kattaki düşman sayısını bu sınırın üstüne çıkarmıyor. */
    private static final int REINFORCE_LIMIT = 24;

    private final Player player;
    private final Inventory inventory = new Inventory();
    private final MessageLog messageLog = new MessageLog();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Item> groundItems = new ArrayList<>();

    /**
     * Havada uçan oklar.
     *
     * <p>Kayda yazılmıyor ve kat değişince siliniyor: uçan bir ok kalıcı bir
     * durum değil, o anki dövüşün bir parçası. Kaydedip yükleyince havada
     * asılı kalmış bir okla karşılaşmak tuhaf olurdu.</p>
     */
    private final List<Projectile> projectiles = new ArrayList<>();
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
    private Merchant merchant;

    /**
     * Kendiliğinden toplamanın en son denendiği kare.
     *
     * <p>Deneme kare değişince yapılıyor: aynı karede her çerçevede denemek,
     * yere bıraktığın iksiri anında geri alırdı.</p>
     */
    private Position lastPickupTile;
    private boolean paused;
    private boolean forgeOpen;

    /**
     * Satıcının tezgâhı açık mı.
     *
     * <p>Büyücü ekranından ayrı bir bayrak: ikisi aynı anda açılamaz ama aynı
     * şey de değiller. Tek bir "ekran açık" bayrağı tutsaydım hangi ekranın
     * açık olduğunu çizim tarafında yeniden çıkarmak gerekirdi.</p>
     */
    private boolean shopOpen;
    private boolean won;
    private double regenTimer;
    private Vision vision;
    private double floorSeconds;
    private double reinforceTimer;
    private boolean dungeonAwake;
    private Position upStairs;

    /** Kaç kez yukarı çıkıldı; zindanın sabrını bu kısaltıyor. */
    private int returns;

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
        messageLog.add(Text.MSG_WELCOME.get());
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
            messageLog.add(Text.MSG_BOSS_GUARDS.get(boss.getName()));
            return false;
        }

        // Son katın merdiveni aşağı değil dışarı çıkıyor: zindanın bir sonu
        // olması, "sonsuza kadar in" hissinden çok daha iyi bir hedef veriyor.
        if (depth >= FloorTheme.MAX_DEPTH) {
            won = true;
            sounds.play(SoundEffect.STAIRS);
            sounds.stopAmbience();
            messageLog.addImportant(Text.MSG_ESCAPED.get());
            return true;
        }

        travelTo(depth + 1);
        messageLog.add(Text.MSG_DESCENDED.get(depth, getTheme().getLabel()));
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
     * <p>Bedava değil: geri dönmek zaman alıyor, zindanın sabrı kat başına
     * <em>hatırlanıyor</em> — uyanmış bir kata geri dönersen takviyeler kaldığı
     * yerden devam ediyor — ve her dönüş zindanı bütün katlarda biraz daha
     * sabırsız yapıyor.</p>
     *
     * @return çıkıldıysa {@code true}
     */
    public boolean ascend() {
        if (isOver() || won || !isPlayerOnUpStairs() || floors == null || depth <= 1) {
            return false;
        }

        returns++;
        travelTo(depth - 1);
        messageLog.add(Text.MSG_ASCENDED.get(depth, getTheme().getLabel()));
        messageLog.addImportant(Text.MSG_DUNGEON_ANGRIER.get());
        return true;
    }

    /**
     * Zindanın bu andaki sabrı, saniye.
     *
     * <p>Her geri dönüşle kısalıyor ama bir tabanın altına inmiyor: cezanın
     * birikip katı oynanamaz hâle getirmesi, dengelemek istediğimiz şeyden
     * daha kötü olurdu.</p>
     */
    public double getFloorPatience() {
        return Math.max(MIN_FLOOR_PATIENCE, FLOOR_PATIENCE - returns * PATIENCE_LOSS_PER_RETURN);
    }

    /** Kaç kez yukarı çıkıldı. */
    public int getReturns() {
        return returns;
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
        refreshVision();
    }

    /** O anki katı, bırakıldığı hâliyle bir değere çevirir. */
    private Floor snapshot() {
        return new Floor(currentSeed, dungeon, upStairs, stairs, wizard, merchant, boss,
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
        merchant = floor.merchant();
        boss = floor.boss();

        enemies.clear();
        enemies.addAll(floor.enemies());
        projectiles.clear();
        groundItems.clear();
        groundItems.addAll(floor.groundItems());

        vision = known.vision();
        floorSeconds = known.floorSeconds();
        dungeonAwake = known.awake();
        reinforceTimer = 0;

        refreshAmbience();
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
     * Ses olaylarını dinleyecek tarafı takar.
     *
     * <p>Varsayılan sessiz dinleyici; testler ve pencere açılmadan çalışan
     * senaryolar ses kütüphanesine hiç dokunmuyor.</p>
     */
    public void setSoundListener(SoundListener listener) {
        this.sounds = listener == null ? SoundListener.SILENT : listener;

        // Dinleyici oyun kurulduktan sonra takılıyor, yani ilk katın sesi
        // burada başlatılmazsa oyuncu ikinci kata inene kadar sessizlik duyar.
        refreshAmbience();
    }

    /**
     * Altta dönmesi gereken sesi çalana bildirir.
     *
     * <p>Kat değişiminin iki ayrı yolu var (yeni kat üretimi ve geri dönüş) ve
     * ikisinin de sonunda ses doğru olmalı. Her birine tek tek çağrı koymak
     * yerine "şu an ne çalmalı" sorusunu tek yerde yanıtlıyoruz; çalan taraf
     * da aynı ses ikinci kez istenirse onu baştan başlatmıyor.</p>
     */
    private void refreshAmbience() {
        if (isOver() || won) {
            sounds.stopAmbience();
            return;
        }

        sounds.playAmbience(FloorBuilder.isBossFloor(depth)
                ? Ambience.BOSS
                : Ambience.forTheme(getTheme()));
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
        // Bir tezgâh açıksa ESC önce onu kapatıyor: tek "geri" tuşu.
        if (forgeOpen || shopOpen) {
            forgeOpen = false;
            shopOpen = false;
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
        return paused || forgeOpen || shopOpen || won;
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
            messageLog.add(Text.MSG_NO_WIZARD.get());
            return;
        }

        forgeOpen = true;
    }

    // -------------------------------------------------------------- satıcı

    /** Bu kattaki gezgin satıcı; yoksa {@code null}. */
    public Merchant getMerchant() {
        return merchant;
    }

    /** Satıcının tezgâhı açık mı. */
    public boolean isShopOpen() {
        return shopOpen;
    }

    /** Oyuncu satıcıyla konuşacak kadar yakın mı. */
    public boolean isNearMerchant() {
        return merchant != null && merchant.tileDistanceTo(player) <= 1;
    }

    /**
     * Satıcının tezgâhını açıp kapatır.
     *
     * <p>Büyücü tezgâhıyla aynı kural: yanına gitmek işin parçası. Açılırken
     * diğer ekranın kapanması gerekmiyor, çünkü ikisi bir arada
     * <em>duramıyor</em> — satıcı büyücünün komşu karelerine hiç konmuyor.</p>
     */
    public void toggleShop() {
        if (shopOpen) {
            shopOpen = false;
            return;
        }

        if (isOver() || paused) {
            return;
        }

        if (!isNearMerchant()) {
            messageLog.add(Text.MSG_NO_MERCHANT.get());
            return;
        }

        shopOpen = true;
    }

    /**
     * Tezgâhtaki bir parçayı satın alır.
     *
     * <p>Sıra önemli: <b>önce yer, sonra altın.</b> Tersi olsaydı çantası dolu
     * bir oyuncu parasını ödeyip eline hiçbir şey geçmediğini görebilirdi —
     * geri alınamayan bir kayıp, üstelik oyuncunun hatası bile değil.</p>
     *
     * <p>Satılan parça tezgâhtan düşüyor ve geri gelmiyor: aynı satıcıdan
     * sınırsız iksir almak, altını bir karar olmaktan çıkarırdı.</p>
     *
     * @param index tezgâhtaki sıra, 0'dan başlayarak
     * @return alışveriş olduysa {@code true}
     */
    public boolean buy(int index) {
        if (!shopOpen || merchant == null) {
            messageLog.add(Text.MSG_GO_TO_MERCHANT.get());
            return false;
        }

        Merchant.Offer offer = merchant.offerAt(index);
        if (offer == null) {
            return false;
        }

        if (!inventory.hasRoomFor(offer.item())) {
            messageLog.item(Text.MSG_BAG_FULL_SHOP.get());
            return false;
        }
        if (!spendGold(offer.price())) {
            return false;
        }

        merchant.take(offer);
        inventory.add(offer.item());
        sounds.play(SoundEffect.PICKUP);
        messageLog.importantItem(Text.MSG_BOUGHT.get(offer.item().getName(), offer.price()));
        return true;
    }

    // -------------------------------------------------------- büyücü tezgâhı

    public void repairWeapon() {

        repair(player.getEquippedWeapon(), Text.GEAR_WEAPON.get());
    }

    public void repairArmor() {
        repair(player.getEquippedArmor(), Text.GEAR_ARMOR.get());
    }

    public void upgradeWeapon() {
        upgrade(player.getEquippedWeapon(), Text.GEAR_WEAPON.get());
    }

    public void upgradeArmor() {
        upgrade(player.getEquippedArmor(), Text.GEAR_ARMOR.get());
    }

    /**
     * Parçayı tam dayanıklılığa getirir.
     *
     * <p>Kısmi tamir yok: "40 altınlık tamir" gibi bir seçenek hem ekranı hem
     * kararı gereksiz karmaşıklaştırırdı. Fiyat zaten eksik kadar.</p>
     */
    private void repair(Equipment item, String label) {
        if (!requireForge() || item == null) {
            messageLog.add(Text.MSG_NOTHING_EQUIPPED.get(label));
            return;
        }

        if (!item.needsRepair()) {
            messageLog.add(Text.MSG_ALREADY_WHOLE.get(item.getDisplayName()));
            return;
        }

        int cost = Forge.repairCost(item);
        if (!spendGold(cost)) {
            return;
        }

        item.repair();
        messageLog.add(Text.MSG_REPAIRED.get(item.getDisplayName(), cost));
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
            messageLog.add(Text.MSG_NOTHING_EQUIPPED.get(label));
            return;
        }

        if (!item.canUpgrade(depth)) {
            messageLog.add(Text.MSG_UPGRADE_CAP.get(item.getDisplayName()));
            return;
        }

        int cost = Forge.upgradeCost(item);
        if (!spendGold(cost)) {
            return;
        }

        item.upgrade();
        messageLog.add(Text.MSG_UPGRADED.get(item.getDisplayName(), cost));
        sounds.play(SoundEffect.EQUIP);
    }

    public void enchantWeapon(Enchantment enchantment) {
        enchant(player.getEquippedWeapon(), enchantment, Text.GEAR_WEAPON.get());
    }

    public void enchantArmor(Enchantment enchantment) {
        enchant(player.getEquippedArmor(), enchantment, Text.GEAR_ARMOR.get());
    }

    /**
     * Bir büyünün sana kaça patlayacağı.
     *
     * <p>Üstünde taşıdığın büyü sayısına bağlı, o yüzden fiyatı oyunun durumunu
     * bilen taraf soruyor; tezgâh ekranı da aynı yerden okuyor, yoksa yazan
     * fiyatla kesilen para ayrı düşerdi.</p>
     */
    public int enchantPrice(Enchantment enchantment) {
        return Forge.enchantCost(enchantment, carriedEnchantments());
    }

    /**
     * Silahında ve zırhında hâlihazırda duran büyü sayısı.
     *
     * <p>İkisi birlikte sayılıyor: pahalı olması gereken şey tek bir parçayı
     * büyülemek değil, tam takım büyülü dolaşmak.</p>
     */
    private int carriedEnchantments() {
        int carried = 0;
        if (player.getEquippedWeapon() != null) {
            carried += player.getEquippedWeapon().getEnchantments().size();
        }
        if (player.getEquippedArmor() != null) {
            carried += player.getEquippedArmor().getEnchantments().size();
        }
        return carried;
    }

    /**
     * Parçaya büyü basar.
     *
     * <p>Bir parçada bir büyü duruyor: yenisi eskisinin yerine geçiyor ve tam
     * fiyat ödeniyor. Yani fikir değiştirmek serbest ama bedava değil.</p>
     */
    private void enchant(Equipment item, Enchantment enchantment, String label) {
        if (!requireForge() || item == null) {
            messageLog.add(Text.MSG_NOTHING_EQUIPPED.get(label));
            return;
        }

        if (!item.accepts(enchantment)) {
            messageLog.add(Text.MSG_ENCHANT_REFUSED.get(label));
            return;
        }

        if (item.hasEnchantment(enchantment)) {
            messageLog.add(Text.MSG_ENCHANT_ALREADY.get(item.getDisplayName(), enchantment.getLabel()));
            return;
        }

        int cost = enchantPrice(enchantment);
        if (!spendGold(cost)) {
            return;
        }

        Enchantment replaced = item.enchant(enchantment);
        sounds.play(SoundEffect.EQUIP);

        if (replaced == null) {
            messageLog.add(Text.MSG_ENCHANTED.get(item.getDisplayName(), enchantment.getLabel(), cost));
        } else {
            messageLog.add(Text.MSG_ENCHANT_REPLACED.get(replaced.getLabel(), enchantment.getLabel(), cost));
        }
    }

    private boolean requireForge() {
        if (forgeOpen) {
            return true;
        }
        messageLog.add(Text.MSG_GO_TO_WIZARD.get());
        return false;
    }

    /** Yetiyorsa keseden düşer; yetmiyorsa uyarır ve hiçbir şey yapmaz. */
    private boolean spendGold(int cost) {
        if (gold < cost) {
            messageLog.add(Text.MSG_NOT_ENOUGH_GOLD.get(cost, gold));
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

    /**
     * Oyuncu kacis adimi atti: ses ve iz.
     *
     * <p>Sicramanin duyulmasi onemli -- bekleme suresi bir kaynak ve harcandigi
     * an geri bildirim almalisin.</p>
     */
    public void onPlayerDashed() {
        sounds.play(SoundEffect.SWING);
    }

    /** Havadaki oklar; ekran bunları çiziyor. */
    public List<Projectile> getProjectiles() {
        return List.copyOf(projectiles);
    }

    /** Bir ok fırlatır; okçu bunu çağırıyor. */
    public void addProjectile(Projectile arrow) {
        projectiles.add(arrow);
        sounds.play(SoundEffect.SWING);
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

        // İksir ve altın kendiliğinden alınıyor; ekipman F bekliyor.
        if (!player.getTile().equals(lastPickupTile)) {
            lastPickupTile = player.getTile();
            pickUpAutomatically();
        }

        // Kopya üzerinde geziyoruz: bir düşman hamlesi sırasında ölüp listeden düşebilir.
        for (Enemy enemy : List.copyOf(enemies)) {
            enemy.update(this, delta);
        }

        updateProjectiles(delta);

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
        if (floorSeconds < getFloorPatience()) {
            return;
        }

        // İlk uyarı bir kez: sonrası zaten karşına çıkacak.
        if (!dungeonAwake) {
            dungeonAwake = true;
            messageLog.addImportant(Text.MSG_DUNGEON_AWAKE.get());
        }

        reinforceTimer += delta;
        if (reinforceTimer < REINFORCE_INTERVAL) {
            return;
        }
        reinforceTimer = 0;
        sendReinforcement();
    }

    /**
     * Okları uçurur ve düşenleri temizler.
     *
     * <p>Düşmanlardan <em>sonra</em> çalışıyor: aynı karede atılan ok, atıldığı
     * kare içinde bir kare yol alsın. Önce çalışsaydı ok bir çerçeve boyunca
     * okçunun üstünde durur, atış anı takılıyormuş gibi görünürdü.</p>
     */
    private void updateProjectiles(double delta) {
        for (Projectile arrow : List.copyOf(projectiles)) {
            arrow.update(this, delta);
        }
        projectiles.removeIf(Projectile::isSpent);
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
        if (merchant != null) {
            taken.add(merchant.getTile());
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
        if (merchant != null && merchant != ignored && merchant.occupies(x, y)) {
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
     * F tuşunun yaptığı iş: ayağının altındakini al, yoksa yanındaki büyücüyle
     * konuş.
     *
     * <p>Tezgâhı T'ye taşıyıp F'yi toplamaya vermek yanlıştı: F yıllardır
     * "buradaki şeyle bir şey yap" tuşu ve büyücünün yanında ona basmak hiçbir
     * şey yapmıyordu. İki iş de aynı soruya cevap veriyor aslında —
     * <em>burada ne var</em>. Sıra da bundan çıkıyor: ayağının altındaki, bir
     * kare ötesindekinden önce gelir.</p>
     *
     * <p>T yerinde duruyor: ayağının dibinde bir parça varken büyücüye
     * ulaşmanın bir yolu kalsın diye.</p>
     *
     * @return bir şey olduysa {@code true}
     */
    public boolean interact() {
        if (isFrozen()) {
            return false;
        }

        if (!itemsUnderfoot().isEmpty()) {
            return pickUp();
        }

        if (isNearWizard()) {
            toggleForge();
            return true;
        }

        if (isNearMerchant()) {
            toggleShop();
            return true;
        }

        messageLog.item(Text.MSG_NOTHING_HERE.get());
        return false;
    }

    /**
     * Ayağının altındaki <em>her şeyi</em> toplar; F tuşunun yaptığı iş.
     *
     * <p>Ekipman kendiliğinden alınmıyor çünkü çantada yer kaplıyor: kaçarken
     * üstünden geçtiğin kötü zırhın slot doldurması ya da yere bıraktığın
     * kılıcın anında geri gelmesi, oyuncunun eline ne geçtiğine karar
     * verememesi demekti. Tuş, o kararı geri veriyor.</p>
     *
     * @return elini bir şey doldurduysa {@code true}
     */
    public boolean pickUp() {
        if (isFrozen()) {
            return false;
        }

        if (itemsUnderfoot().isEmpty()) {
            messageLog.item(Text.MSG_NOTHING_HERE.get());
            return false;
        }

        return pickUpItemsExcept(null, false);
    }

    /**
     * Yeni girilen karedeki iksirleri ve altını kendiliğinden toplar.
     *
     * <p>Yalnızca kareye <em>girildiğinde</em> deneniyor: her karede denemek,
     * yere bıraktığın iksiri anında geri alırdı. Neyin buraya girdiğini eşya
     * kendisi söylüyor ({@link Item#isAutoPickedUp()}).</p>
     */
    private void pickUpAutomatically() {
        pickUpItemsExcept(null, true);
    }

    /** Oyuncunun bastığı karedeki eşyalar; ekran "F: al" ipucunu buna göre gösteriyor. */
    public List<Item> itemsUnderfoot() {
        List<Item> here = new ArrayList<>();
        for (Item item : groundItems) {
            if (item.getTile().equals(player.getTile())) {
                here.add(item);
            }
        }
        return here;
    }

    /**
     * Ayağının altındakileri toplar; verilen parçayı atlar.
     *
     * @param skipped  az önce yere bırakılan parça; {@code null} olabilir
     * @param onlyAuto yalnızca kendiliğinden alınanlar mı toplanacak
     * @return en az bir eşya alındıysa {@code true}
     */
    private boolean pickUpItemsExcept(Item skipped, boolean onlyAuto) {
        boolean took = false;

        for (Item item : List.copyOf(groundItems)) {
            if (item == skipped || !item.getTile().equals(player.getTile())) {
                continue;
            }
            if (onlyAuto && !item.isAutoPickedUp()) {
                continue;
            }

            if (item.goesToInventory()) {
                if (!inventory.add(item)) {
                    messageLog.importantItem(Text.MSG_BAG_FULL.get(item.getName()));
                    continue;
                }
                messageLog.item(Text.MSG_TOOK.get(item.getName()));
                sounds.play(SoundEffect.PICKUP);
            }

            item.onPickup(this);
            groundItems.remove(item);
            took = true;
        }
        return took;
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
        messageLog.item(Text.MSG_DROPPED.get(item.getName()));

        // Bırakmak kendiliğinden toplamayı tetiklemesin: bıraktığın iksir
        // anında geri gelirdi.
        lastPickupTile = player.getTile();

        // Bıraktığın anda ayağının altındakini alıyorsun: çanta doluyken
        // takas etmek için kareden çıkıp geri gelmek gerekiyordu. Yeni
        // bıraktığın parça hariç, yoksa onu geri toplardın.
        pickUpItemsExcept(item, false);
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
                ? Text.MSG_BOMB_EMPTY.get()
                : Text.MSG_BOMB_HIT.get(hit));
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
            messageLog.add(Text.MSG_NO_STAIRS.get());
            return false;
        }

        player.setTile(stairs);
        refreshVision();
        sounds.play(SoundEffect.STAIRS);
        messageLog.add(Text.MSG_ESCAPE.get());
        return true;
    }

    /**
     * Yerini yenisi alan parçayı çantadan çıkarıp ayağının dibine bırakır.
     *
     * <p>Daha iyi bir zırh bulunca eskisi çantada duruyordu ve slotlar birkaç
     * katta doluyordu — oysa geri dönüp kötü zırhı giymek diye bir şey yok.
     * Yere bırakmak hem çantayı açıyor hem de fikrini değiştirirsen parça hâlâ
     * orada duruyor.</p>
     */
    public void discardToGround(Item item) {
        if (item == null || !inventory.remove(item)) {
            return;
        }

        item.setTile(player.getTile());
        addGroundItem(item);
        lastPickupTile = player.getTile();
        messageLog.item(Text.MSG_DISCARDED.get(item.getName()));
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
            messageLog.combat(Text.MSG_SWING_MISS.get());
            return;
        }

        sounds.play(SoundEffect.HIT);
        wearGear(player.getEquippedWeapon(), Text.GEAR_YOUR_WEAPON.get());

        for (Enemy enemy : targets) {
            int damage = resolveDamage(player, enemy);
            enemy.takeDamage(damage);
            messageLog.combat(Text.MSG_ENEMY_HURT.get(enemy.getName(), damage));

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
        messageLog.combat(Text.MSG_ENEMY_DOWN.get(enemy.getName()));
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
        messageLog.combat(Text.MSG_VAMPIRISM.get(VAMPIRISM_HEAL));
    }

    /**
     * Düşmanın oyuncuya vuruşu; {@code Enemy.update} içinden çağrılır.
     * Saldırı kararı düşmanın, hasar hesabı oyunun işi.
     */
    public void enemyAttacksPlayer(Enemy enemy) {
        int damage = resolveDamage(enemy, player);
        player.takeDamage(damage);
        messageLog.combat(Text.MSG_PLAYER_HURT.get(enemy.getName(), damage));
        sounds.play(SoundEffect.HURT);
        wearGear(player.getEquippedArmor(), Text.GEAR_YOUR_ARMOR.get());
        reflectThorns(enemy);
        announceDeathIfFallen();
    }

    /**
     * Ok oyuncuya isabet etti.
     *
     * <p>Hasar aynı boru hattından geçiyor: zırh yine sayılıyor, zırh yine
     * yıpranıyor. Tek fark Dikenin işlememesi — diken <em>sana dokunanı</em>
     * yakıyor, sekiz kare öteden ok atanı değil.</p>
     */
    public void projectileHitsPlayer(Projectile arrow) {
        Enemy shooter = arrow.getShooter();
        int damage = resolveDamage(shooter, player);

        player.takeDamage(damage);
        player.triggerHitFlash();
        messageLog.combat(Text.MSG_ARROW_HURT.get(shooter.getName(), damage));
        sounds.play(SoundEffect.HURT);
        wearGear(player.getEquippedArmor(), Text.GEAR_YOUR_ARMOR.get());
        announceDeathIfFallen();
    }

    private void announceDeathIfFallen() {
        if (player.isAlive()) {
            return;
        }

        messageLog.addImportant(Text.MSG_DIED.get());
        sounds.play(SoundEffect.DEATH);

        // Zemin sesi susuyor: sessizlik, ölümü ekrandaki yazıdan daha net
        // anlatıyor.
        sounds.stopAmbience();
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
        messageLog.combat(Text.MSG_THORNS.get(enemy.getName(), THORNS_DAMAGE));

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
            messageLog.addImportant(Text.MSG_GEAR_BROKE.get(label));
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

    /** Ölümden sonra sıfırdan başlar: can, çanta, kese, derinlik ve süre sıfırlanır. */
    public void restart() {
        player.restore();
        inventory.clear();
        gold = 0;
        depth = 1;
        won = false;
        visited.clear();
        returns = 0;
        elapsedSeconds = 0;
        enemies.clear();
        groundItems.clear();
        projectiles.clear();
        messageLog.clear();
        regenerateFloor();
        messageLog.add(Text.MSG_RESTARTED.get());
    }

    /**
     * Yeni bir kat üretip devralır.
     *
     * <p>Katı {@link FloorBuilder} kuruyor; burası yalnızca sonucu sahipleniyor
     * ve oyuncuyu yerleştiriyor. Üretici seçimi katın kendi kuralı.</p>
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
        merchant = floor.merchant();
        boss = floor.boss();

        enemies.clear();
        enemies.addAll(floor.enemies());
        projectiles.clear();
        groundItems.clear();
        groundItems.addAll(floor.groundItems());

        upStairs = floor.spawn();
        player.setTile(floor.spawn());

        // Yeni kat baştan karanlık: bir önceki katın hatırladıkları buraya
        // taşınmamalı.
        vision = new Vision(dungeon.getWidth(), dungeon.getHeight());
        refreshVision();

        // Zindanın sabrı kat başına yeniliyor: inmek gerçekten rahatlatıyor.
        floorSeconds = 0;
        reinforceTimer = 0;
        dungeonAwake = false;

        refreshAmbience();
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

        messageLog.addImportant(Text.MSG_BOSS_ANNOUNCE.get(boss.getName()));
        sounds.play(SoundEffect.BOSS);
    }
}
