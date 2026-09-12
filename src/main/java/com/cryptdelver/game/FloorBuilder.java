package com.cryptdelver.game;

import com.cryptdelver.entity.Archer;
import com.cryptdelver.entity.Bomb;
import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.EliteTrait;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.EscapePotion;
import com.cryptdelver.entity.FuryPotion;
import com.cryptdelver.entity.Goblin;
import com.cryptdelver.entity.Gold;
import com.cryptdelver.entity.HastePotion;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.LegendWeapon;
import com.cryptdelver.entity.Merchant;
import com.cryptdelver.entity.Orc;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Saman;
import com.cryptdelver.entity.Shrine;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Wizard;
import com.cryptdelver.entity.Zombi;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Tile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/**
 * Bir katın nasıl kurulduğu: harita, merdiven, büyücü, boss, düşmanlar, eşyalar.
 *
 * <p>Bu iş eskiden {@link Game} içindeydi ve orası büyüdükçe "oyunun kuralları"
 * ile "kat nasıl doğar" birbirine karışıyordu. İkisi farklı sorular: kurallar
 * her karede işliyor, kat kurulumu ise kat başına bir kez. Ayrılınca ikisi de
 * tek başına okunabilir hâle geldi.</p>
 *
 * <p>Burası oyunun durumuna <em>dokunmuyor</em>: bir {@link Floor} döndürüyor,
 * onu devralmak {@code Game}'in işi. Yan etkisiz olması, kat kurulumunu oyun
 * döngüsü çalıştırmadan sınamayı da kolaylaştırıyor.</p>
 */
public class FloorBuilder {

    /** Uretici sirasindaki yerleri: 0 odali, 1 magara. */
    private static final int ROOMS = 0;
    private static final int CAVES = 1;

    /** Kaç katta bir boss çıkar. */
    private static final int FLOORS_PER_BOSS = 5;

    /** İlk kattaki düşman sayısı; her kat bir artar. */
    private static final int BASE_ENEMIES_PER_FLOOR = 8;

    /** Ne kadar inersen in, bir kata bundan fazla düşman doğmaz. */
    private static final int MAX_ENEMIES_PER_FLOOR = 20;

    /** Düşmanlar oyuncunun bu kadar yakınına doğmaz (kare). */
    private static final int MIN_SPAWN_DISTANCE = 8;

    /** Yeni türlerin oyuna girdiği katlar. */
    private static final int GOBLIN_MIN_DEPTH = 2;
    private static final int ORC_MIN_DEPTH = 4;

    /**
     * Okçu 3. kattan itibaren.
     *
     * <p>İlk iki kat oyunun temel dövüşünü öğretiyor: yaklaş, vur, geri çekil.
     * Okçu tam o alışkanlığı bozmak için var, o yüzden alışkanlık kurulduktan
     * hemen sonra giriyor — daha geç girseydi oyuncu yirmi kat boyunca tek bir
     * hamleye güvenmeyi öğrenmiş olurdu.</p>
     */
    private static final int ARCHER_MIN_DEPTH = 3;

    /**
     * Elit düşmanlar 3. kattan itibaren, her on düşmandan kabaca birinde.
     *
     * <p>İlk iki kat oyunun temel dövüşünü öğretiyor ve elit tam o dersin
     * istisnası — istisnayı kural öğrenilmeden göstermek öğretmiyor,
     * şaşırtıyor.</p>
     *
     * <p>Oran sabit, derinlikle artmıyor. Artsaydı derin katlar elit
     * sürüsüne dönerdi ve elit "bu farklı" demeyi bırakırdı: bir şeyin özel
     * olması için seyrek kalması gerekiyor.</p>
     */
    private static final int ELITE_MIN_DEPTH = 3;
    private static final double ELITE_CHANCE = 0.10;

    /**
     * Zombi ve şaman bölge sınırlarında giriyor: zombi Sarnıçta, şaman Korlukta.
     *
     * <p>Bölge değiştiğinde yalnızca renk değil karşına çıkan şey de
     * değişiyor; yeni bölgeye inmenin ilk dakikası böylece bir şey
     * öğretiyor.</p>
     */
    private static final int ZOMBI_MIN_DEPTH = 6;
    private static final int SAMAN_MIN_DEPTH = 11;

    /** Derin katlarda düşmanlar kaç katta bir güçlenir. */
    private static final int DEPTHS_PER_HP_BONUS = 2;
    private static final int DEPTHS_PER_ATTACK_BONUS = 3;
    private static final int DEPTHS_PER_DEFENSE_BONUS = 4;

    /** Boss katlarında sıradan düşman sayısı bu oranda azalır. */
    private static final double BOSS_FLOOR_ENEMY_RATIO = 0.6;

    /**
     * Büyücü doğulan yerden en az bu kadar <em>adım</em> uzağa konur.
     *
     * <p>Kuş uçuşu değil yürüme mesafesi: duvarın öbür yanındaki kare yakın
     * görünüp uzak olabiliyordu. İki adım, "indiğin anda görüyorsun ama
     * üstünde belirmiş gibi durmuyor" dengesi.</p>
     */
    private static final int WIZARD_MIN_DISTANCE = 2;

    /** Sıradan bir katta gezgin büyücüye rastlama olasılığı. */
    private static final double WIZARD_WANDER_CHANCE = 0.05;

    /**
     * Büyücü zarını katın tohumundan ayırmak için karıştırılan sayı.
     *
     * <p>Doğrudan tohumu kullansaydık zar, aynı tohumdan üretilen haritayla
     * ilişkili çıkardı; karıştırmak ikisini bağımsızlaştırıyor.</p>
     */
    private static final long WIZARD_ROLL_SALT = 0x5DEECE66DL;

    /**
     * Sıradan bir katta gezgin satıcıya rastlama olasılığı.
     *
     * <p>Büyücünün tam tersi bir takvim: büyücü boss katlarında sabit duruyor,
     * satıcı yalnızca aradaki katlarda çıkıyor. Böylece kese hiçbir zaman çok
     * uzun süre ölü kalmıyor ama ikisi de aynı katta üst üste binmiyor.</p>
     *
     * <p>Üçte bir: dört katlık bir aralıkta satıcıyı hiç görmeme ihtimali
     * kabaca beşte bir. Yani "bir sonrakinde çıkar" diye biriktirmek makul bir
     * bahis, ama garanti değil — o yüzden elindeki altını harcamak da bir
     * karar.</p>
     */
    private static final double MERCHANT_CHANCE = 0.35;

    /** Satıcı zarını hem tohumdan hem büyücünün zarından ayıran sayı. */
    private static final long MERCHANT_ROLL_SALT = 0x7F4A7C15L;

    /** Tezgâh doğulan yerden en az bu kadar adım uzağa kurulur. */
    private static final int MERCHANT_MIN_DISTANCE = 4;

    /**
     * Kader taşının sıklığı ve zarı.
     *
     * <p>Satıcıyla aynı oran ama ayrı zar: aynı sayıyla atsaydık taş ve
     * satıcı ya hep birlikte çıkar ya hep birlikte çıkmazdı.</p>
     */
    private static final double SHRINE_CHANCE = 0.30;
    private static final long SHRINE_ROLL_SALT = 0x2545F491L;

    /** Taş girişten biraz daha uzağa kuruluyor; bulunması da işin parçası. */
    private static final int SHRINE_MIN_DISTANCE = 6;

    /** Kat olaylarının kattan itibaren, sıklığı ve kendi zarı. */
    private static final int EVENT_MIN_DEPTH = 3;
    private static final double EVENT_CHANCE = 0.33;
    private static final long EVENT_ROLL_SALT = 0x9E3779B9L;

    /**
     * Nadir eşyalar için kat başına kaç zar, hangi olasılıkla.
     *
     * <p>İki zar ve her biri %20: kat başına ortalama 0.4 eşya, yirmi katlık
     * bir koşuda kabaca sekiz tane. Bulmak olay olacak kadar seyrek, ama
     * "hiç görmedim" diyecek kadar da değil.</p>
     */
    private static final int RARE_ITEM_ROLLS = 2;
    private static final double RARE_ITEM_CHANCE = 0.20;

    /**
     * Efsanevi kılıcın bir katta çıkma olasılığı: binde bir.
     *
     * <p>Yirmi katlık bir koşuda görme ihtimali yüzde ikinin altında. Kasten
     * böyle: efsanevi olmasının anlamı bu.</p>
     */
    private static final double LEGEND_CHANCE = 0.001;

    private static final int POTIONS_PER_FLOOR = 4;
    private static final int GOLD_PILES_PER_FLOOR = 5;
    private static final int MIN_GOLD = 5;
    private static final int MAX_GOLD = 25;

    private final List<DungeonGenerator> generators;
    private final int width;
    private final int height;
    private final Random random = new Random();

    public FloorBuilder(List<DungeonGenerator> generators, int width, int height) {
        this.generators = List.copyOf(generators);
        this.width = width;
        this.height = height;
    }

    /**
     * Katın tohumundan bağımsız bir zar üretir.
     *
     * <p>Önce {@code new Random(seed ^ SALT)} yazıyordum ve bu <b>yanlıştı</b>.
     * {@code java.util.Random} tohumu yalnızca hafifçe karıştırdığı için
     * birbirine yakın tohumlar birbirine yakın ilk değerler veriyor: sıralı
     * tohumlarla kurulan yüzlerce katta "üçte bir" olması gereken olay her
     * katta çıktı. Oyunda tohumlar {@code nextLong()}'dan geldiği için bu hiç
     * görünmüyordu — sınav sıralı tohum kullanınca ortaya çıktı.</p>
     *
     * <p>Buradaki karıştırma (murmur3'ün bitiricisi) tohumun her bitini
     * bütün bitlere yayıyor. Böylece hem komşu tohumlar ayrışıyor hem de
     * <em>farklı tuzlar birbirinden bağımsız</em> kalıyor: büyücü zarı ile
     * satıcı zarı artık aynı yöne eğilmiyor.</p>
     */
    private static Random diceFor(long seed, long salt) {
        long mixed = seed ^ salt;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return new Random(mixed);
    }

    /**
     * Katta oyuncudan uzak, boş bir kare bulur.
     *
     * <p>Takviye düşmanları buraya doğuyor: oyuncunun tepesinde belirmeleri
     * hem haksız olurdu hem de "zindan seni kuşatıyor" hissini bozardı — tehdit
     * uzaktan gelip yaklaşmalı.</p>
     *
     * @param blocked dolu olduğu bilinen kareler
     * @return uygun kare; bulunamazsa {@code null}
     */
    public Position findSpawnAwayFrom(Dungeon dungeon, Position player, Set<Position> blocked) {
        List<Position> spots = new ArrayList<>(dungeon.walkablePositions());
        Collections.shuffle(spots, random);

        for (Position spot : spots) {
            if (!blocked.contains(spot)
                    && spot.manhattanDistance(player) >= MIN_SPAWN_DISTANCE) {
                return spot;
            }
        }
        return null;
    }

    /** Bu derinlikte boss var mı. */
    public static boolean isBossFloor(int depth) {
        return depth % FLOORS_PER_BOSS == 0;
    }

    /** Kaçıncı boss: 5. katta 1, 10. katta 2... Boss gövdesini bu belirliyor. */
    public static int bossNumber(int depth) {
        return depth / FLOORS_PER_BOSS;
    }

    /**
     * Katın hangi üreticiyle kurulacağı: <b>derinliğin kararı, oyuncunun
     * değil.</b>
     *
     * <p>Önce oyuncu {@code G} ile üreticiyi elle değiştirebiliyordu. Bu bir
     * hata ayıklama kolaylığıydı ve oyunun akışını bozuyordu: zindan bir yol
     * olmaktan çıkıp ayar penceresine dönüyordu. Şimdi sıra sabit ve
     * öngörülebilir:</p>
     *
     * <ul>
     *   <li><b>Boss katları hep odalı.</b> Boss yavaş ama durmak bilmez;
     *       vurup geri çekilerek dövüşmek için alan gerekiyor. Mağara
     *       koridorlarında sıkışıp kalıyordun.</li>
     *   <li><b>Kalan katların şekli bölgenin kendi kimliği.</b> Önce tek/çift
     *       diye değişiyordu ve bu hiçbir şey anlatmıyordu — yalnızca aynı
     *       görüntüde üst üste inmeni engelliyordu. Artık şekil de bölgeyi
     *       anlatıyor: Mahzen örülmüş odalar, Sarnıç oyulmuş mağaralar,
     *       Korluk ikisinin arasında çökmüş bir yer, Kript yine yapılmış
     *       salonlar. Yani bölge değiştiğinde yalnızca renk ve taş değil,
     *       <em>katın şekli</em> de değişiyor.</li>
     * </ul>
     */
    public int generatorForDepth(int depth) {
        if (generators.size() < 2 || isBossFloor(depth)) {
            return 0;
        }

        return switch (FloorTheme.forDepth(depth)) {
            case MAHZEN, KRIPT -> ROOMS;
            case SARNIC -> CAVES;

            // Korluk çökmekte olan bir yer: bazı katları hâlâ oda, bazıları
            // artık mağara. Tek bölgede iki şekil, "burada bir şey yıkılmış"
            // hissini haritanın kendisine yazıyor.
            case KORLUK -> depth % 2 == 0 ? CAVES : ROOMS;
        };
    }

    /** Sıradaki üreticinin kendisi; ekran adını göstermek için soruyor. */
    public DungeonGenerator generatorAt(int index) {
        return generators.get(clampGeneratorIndex(index));
    }

    /** Kayıttan gelen indisi listenin içinde tutar. */
    public int clampGeneratorIndex(int index) {
        return Math.floorMod(index, generators.size());
    }

    /**
     * Katın sabit döşemesi: harita, doğulan yer, merdiven ve büyücü.
     *
     * <p>Hepsi tohumdan belirlenimci olarak çıkıyor: aynı tohumla iki kez
     * kurulan kat birebir aynı oluyor. Doldurma adımından ayrı durmasının
     * sebebi bu — belirlenimcilik tek başına sınanabiliyor.</p>
     */
    public Floor layout(int generatorIndex, int depth, long seed) {
        Dungeon dungeon = generators.get(generatorIndex).generate(width, height, seed);
        Position spawn = dungeon.findWalkableNear(width / 2, height / 2);

        // Merdiven, doğulan yerden yürüyerek gidilebilen en uzak kareye konur.
        Position stairs = dungeon.findFarthestWalkableFrom(spawn);
        dungeon.setTile(stairs.x(), stairs.y(), Tile.STAIRS_DOWN);

        // Kata indiğin nokta aynı zamanda yukarı çıkan merdiven: geldiğin yer.
        // İlk katta da işaretleniyor, oradan yukarı çıkmayı reddetmek katı
        // kuran tarafın değil oyunun kararı.
        dungeon.setTile(spawn.x(), spawn.y(), Tile.STAIRS_UP);

        Wizard wizard = hasWizard(depth, seed)
                ? placeAt(dungeon, spawn, stairs, WIZARD_MIN_DISTANCE, Set.of(),
                        spot -> new Wizard(spot.x(), spot.y()))
                : null;

        // Satıcı büyücünün karesini ve komşularını dışlıyor: F tuşu "burada ne
        // var" sorusunu tek cevapla yanıtlıyor ve yan yana duran iki tezgâh o
        // cevabı belirsiz yapardı.
        Merchant merchant = hasMerchant(depth, seed)
                ? placeAt(dungeon, spawn, stairs, MERCHANT_MIN_DISTANCE, around(wizard),
                        spot -> Merchant.stocked(spot, seed ^ MERCHANT_ROLL_SALT))
                : null;

        // Taş ikisinden de uzağa kuruluyor: üç tezgâhın da kendi köşesi
        // olmalı, yoksa F tuşunun hangisini açacağı belirsiz kalır.
        Set<Position> busy = new HashSet<>(around(wizard));
        busy.addAll(around(merchant));

        Shrine shrine = hasShrine(depth, seed)
                ? placeAt(dungeon, spawn, stairs, SHRINE_MIN_DISTANCE, busy,
                        spot -> Shrine.seeded(spot, seed ^ SHRINE_ROLL_SALT))
                : null;

        return new Floor(seed, dungeon, spawn, stairs, wizard, merchant, shrine,
                rollEvent(depth, seed), null, List.of(), List.of());
    }

    /** Döşemeyi kurup üstüne boss, düşman ve eşyaları dağıtır. */
    public Floor build(int generatorIndex, int depth, long seed, Difficulty difficulty) {
        return populate(layout(generatorIndex, depth, seed), depth, difficulty);
    }

    /**
     * Bu katta büyücü var mı.
     *
     * <p>Boss katlarında her zaman var — orası tezgâhın sabit adresi. Sıradan
     * katlarda ise {@value #WIZARD_WANDER_CHANCE} olasılıkla çıkıyor: beş kat
     * boyunca kırık kılıçla yürümek bazen fazla uzun bir ceza oluyordu ve
     * arada bir gezgin büyücüye rastlamak, katı açmaya değer küçük bir
     * sürpriz.</p>
     *
     * <p>Zar katın <em>tohumundan</em> atılıyor, genel rastgelelikten değil:
     * böylece kat kurulumu tohumun saf bir fonksiyonu kalıyor ve aynı tohumla
     * kurulan iki kat büyücü konusunda da ayrışmıyor.</p>
     */
    private boolean hasWizard(int depth, long seed) {
        return isBossFloor(depth)
                || diceFor(seed, WIZARD_ROLL_SALT).nextDouble() < WIZARD_WANDER_CHANCE;
    }

    /**
     * Bu katta gezgin satıcı var mı.
     *
     * <p>Boss katlarında <em>hiç yok</em>. Orada zaten büyücü duruyor ve iki
     * tezgâhı aynı kata koymak ikisini de sıradanlaştırırdı: boss katı
     * "büyük hazırlık", aradaki katlar "yolda ne bulursan". Ayrımı koruyan şey
     * bu.</p>
     *
     * <p>Zar yine katın tohumundan, ama büyücününkinden farklı bir karıştırma
     * sayısıyla: aynı tohumu aynı şekilde kullansaydık iki zar birbirinin
     * kopyası çıkardı.</p>
     */
    /**
     * Bu katın kendine özgü bir hâli var mı, varsa hangisi.
     *
     * <p>Boss katlarında yok: boss zaten o katın olayı ve üstüne bir de
     * karanlık eklemek, dövüşü okunamaz yapardı. İlk iki katta da yok — orada
     * oyun kendi temel kurallarını öğretiyor ve istisnayı kural
     * öğrenilmeden göstermek öğretmiyor.</p>
     *
     * <p>Üçte bir: daha sık olsa olay "normal" olurdu ve sıradan kat
     * istisnaya dönerdi; daha seyrek olsa indiğinde okuduğun cümle
     * hatırlanmazdı.</p>
     */
    private FloorEvent rollEvent(int depth, long seed) {
        if (depth < EVENT_MIN_DEPTH || isBossFloor(depth)) {
            return null;
        }

        Random dice = diceFor(seed, EVENT_ROLL_SALT);
        if (dice.nextDouble() >= EVENT_CHANCE) {
            return null;
        }
        return FloorEvent.values()[dice.nextInt(FloorEvent.values().length)];
    }

    /**
     * Bu katta kader taşı var mı.
     *
     * <p>Satıcı gibi yalnızca boss dışı katlarda ve aynı sıklıkta. İkisi aynı
     * kata düşebiliyor — biri altın istiyor, diğeri can; yan yana durmaları
     * "neyi neyle ödeyeceksin" sorusunu en keskin hâline getiriyor.</p>
     */
    private boolean hasShrine(int depth, long seed) {
        return !isBossFloor(depth)
                && diceFor(seed, SHRINE_ROLL_SALT).nextDouble() < SHRINE_CHANCE;
    }

    private boolean hasMerchant(int depth, long seed) {
        return !isBossFloor(depth)
                && diceFor(seed, MERCHANT_ROLL_SALT).nextDouble() < MERCHANT_CHANCE;
    }

    /**
     * Bir varlığın karesi ve çevresindeki sekiz kare.
     *
     * <p>"Aynı kareye konmasın" yetmiyordu: komşu karede duran iki tezgâh da
     * tek bir F tuşunun neyi açacağını belirsiz bırakıyor. Varlık yoksa küme
     * boş — o zaman dışlanacak bir şey de yok.</p>
     */
    private static Set<Position> around(Entity entity) {
        if (entity == null) {
            return Set.of();
        }

        Set<Position> ring = new HashSet<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                ring.add(entity.getTile().offset(dx, dy));
            }
        }
        return ring;
    }

    /**
     * Dövüşmeyen bir varlığı doğulan yerin yakınına koyar.
     *
     * <p>Merdivenin yanına koymak cazipti ama orada boss duruyor: tezgâhı
     * dövüşün ortasına yerleştirmiş olurduk. Girişin dibinde olması daha doğru
     * — kata inip önce hazırlanıyor, sonra bossa yürüyorsun.</p>
     *
     * <p>Yerleştirme <em>rastgele değil</em>: doğulan yerden yayılan BFS'in
     * sırasında ilk uygun kare seçiliyor, yani en yakını. Merdiven gibi bu da
     * katın sabit döşemesi.</p>
     *
     * <p>Hem büyücü hem satıcı buradan geçiyor. İkisi aynı kuralı istiyor
     * (girişe yakın, merdivenin üstünde değil, katı ikiye bölmeyen bir kare)
     * ve farkları yalnızca kaç adım uzakta durdukları — o yüzden kuralı iki
     * kere yazmak yerine tek yerde tutuyorum.</p>
     *
     * @param minDistance doğulan yerden en az kaç adım uzağa
     * @param taken kullanılmaması gereken kareler
     * @param create seçilen kareden varlığı üreten işlev
     */
    private <T> T placeAt(Dungeon dungeon, Position spawn, Position stairs, int minDistance,
                          Set<Position> taken, Function<Position, T> create) {
        Map<Position, Integer> distances = dungeon.walkableDistancesFrom(spawn);

        // BFS sırası yakından uzağa; ilk uyan kare en yakın uygun kare oluyor.
        for (Map.Entry<Position, Integer> candidate : distances.entrySet()) {
            Position spot = candidate.getKey();

            if (candidate.getValue() < minDistance || spot.equals(stairs)
                    || taken.contains(spot)) {
                continue;
            }
            if (wouldSealTheFloor(dungeon, spot, spawn, stairs)) {
                continue;
            }

            return create.apply(spot);
        }

        return null;
    }

    /**
     * Büyücü bu kareye konursa merdiven ulaşılmaz kalır mı.
     *
     * <p>Büyücünün karesinden geçilemiyor. Tek karelik bir koridora denk
     * gelirse katı ikiye bölüp merdiveni kapatabilirdi — üstelik en yakın kareyi
     * seçtiğimiz için tam da doğulan odanın çıkışına oturma ihtimali yüksek.
     * Tahmin yürütmek yerine doğrudan soruyoruz: o kare kapalıyken merdivene
     * hâlâ yürünebiliyor mu.</p>
     */
    private boolean wouldSealTheFloor(Dungeon dungeon, Position spot, Position spawn,
                                      Position stairs) {
        if (stairs == null) {
            return false;
        }
        return !dungeon.walkableDistancesFrom(spawn, Set.of(spot)).containsKey(stairs);
    }

    /**
     * Katı boss, düşman ve eşyalarla doldurur.
     *
     * <p>Uygun kareleri bir kez karıştırıp sırayla dağıtıyoruz; kullanılan
     * kareleri işaretlemek, iki şeyin aynı kareye konmasını kendiliğinden
     * engelliyor.</p>
     */
    private Floor populate(Floor floor, int depth, Difficulty difficulty) {
        List<Position> spots = new ArrayList<>(floor.dungeon().walkablePositions());
        Collections.shuffle(spots, random);

        Set<Position> used = new HashSet<>();
        used.add(floor.spawn());
        if (floor.stairs() != null) {
            // Merdivenin üstü boş kalsın; eşya ya da düşmanla kapanmasın.
            used.add(floor.stairs());
        }
        if (floor.wizard() != null) {
            used.add(floor.wizard().getTile());
        }
        if (floor.merchant() != null) {
            used.add(floor.merchant().getTile());
        }
        if (floor.shrine() != null) {
            used.add(floor.shrine().getTile());
        }

        List<Enemy> enemies = new ArrayList<>();
        Boss boss = spawnBoss(floor, depth, difficulty);
        if (boss != null) {
            enemies.add(boss);
        }

        int target = enemyCountForDepth(depth, difficulty);
        if (floor.event() != null) {
            target = floor.event().scaleCrowd(target);
        }
        for (Position spot : spots) {
            if (enemies.size() - (boss == null ? 0 : 1) >= target) {
                break;
            }
            if (used.contains(spot) || spot.manhattanDistance(floor.spawn()) < MIN_SPAWN_DISTANCE) {
                continue;
            }

            used.add(spot);
            enemies.add(createEnemyForDepth(spot, depth, difficulty));
        }

        return floor.filledWith(boss, enemies,
                placeItems(spots, used, depth, extraItems(floor.event())));
    }

    /** Boss merdivenin üstünde doğar: geçmek için onu yenmen gerekiyor. */
    private Boss spawnBoss(Floor floor, int depth, Difficulty difficulty) {
        if (!isBossFloor(depth) || floor.stairs() == null) {
            return null;
        }

        int number = bossNumber(depth);
        Boss boss = Boss.forNumber(floor.stairs().x(), floor.stairs().y(), number);
        applyDepthBonus(boss, depth, difficulty);
        boss.scaleTo(number);
        return boss;
    }

    /**
     * Her kat bir düşman daha; belli bir sayıdan sonra artmıyor.
     *
     * <p>Boss katlarında sıradan düşman sayısı azaltılıyor: asıl tehdit boss ve
     * çağırdığı yaratıklar olsun, kalabalık boğmasın.</p>
     */
    private int enemyCountForDepth(int depth, Difficulty difficulty) {
        int count = Math.min(MAX_ENEMIES_PER_FLOOR, BASE_ENEMIES_PER_FLOOR + depth - 1);
        if (isBossFloor(depth)) {
            count = (int) Math.round(count * BOSS_FLOOR_ENEMY_RATIO);
        }
        return difficulty.scaleCrowd(count);
    }

    /**
     * Derinliğe uygun bir düşman üretir.
     *
     * <p>İki kaldıraç var: aşağı indikçe iskelet oranı artıyor (kalabalık
     * sertleşiyor) ve düşmanlar can/güç bonusu alıyor. Yeni tür yazmadan
     * zorluk eğrisi elde etmenin ucuz yolu bu.</p>
     */
    public Enemy createEnemyForDepth(Position spot, int depth, Difficulty difficulty) {
        Enemy enemy = rollEnemyKind(spot, depth);
        applyDepthBonus(enemy, depth, difficulty);

        // Elitlik en sona: derinlik bonusu sıradan değerlerin üstüne biniyor,
        // elit sıfatı da onun üstüne. Sıra ters olsaydı "zırhlı" bir düşman
        // derin katlarda sıradan bir düşmandan ayırt edilemez hâle gelirdi.
        if (depth >= ELITE_MIN_DEPTH && random.nextDouble() < ELITE_CHANCE) {
            EliteTrait[] traits = EliteTrait.values();
            enemy.makeElite(traits[random.nextInt(traits.length)]);
        }
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
    private Enemy rollEnemyKind(Position spot, int depth) {
        int impWeight = Math.max(1, 7 - depth);
        int skeletonWeight = 2 + depth;
        int goblinWeight = depth >= GOBLIN_MIN_DEPTH ? 3 : 0;
        int orcWeight = depth >= ORC_MIN_DEPTH ? depth - 2 : 0;
        int zombiWeight = depth >= ZOMBI_MIN_DEPTH ? depth - 3 : 0;
        int samanWeight = depth >= SAMAN_MIN_DEPTH ? depth - 8 : 0;

        // Okçunun ağırlığı derinlikle artmıyor, sabit kalıyor: tehdidi
        // sayıdan değil türünden geliyor. Kalabalık okçu, kaçacak yer
        // bırakmayan bir kat demek olurdu.
        int archerWeight = depth >= ARCHER_MIN_DEPTH ? 3 : 0;

        int roll = random.nextInt(impWeight + skeletonWeight + goblinWeight + orcWeight
                + zombiWeight + samanWeight + archerWeight);

        if (roll < impWeight) {
            return new Imp(spot.x(), spot.y());
        }
        roll -= impWeight;

        if (roll < skeletonWeight) {
            return new Skeleton(spot.x(), spot.y());
        }
        roll -= skeletonWeight;

        if (roll < goblinWeight) {
            return new Goblin(spot.x(), spot.y());
        }
        roll -= goblinWeight;

        if (roll < orcWeight) {
            return new Orc(spot.x(), spot.y());
        }
        roll -= orcWeight;

        if (roll < archerWeight) {
            return new Archer(spot.x(), spot.y());
        }
        roll -= archerWeight;

        return roll < zombiWeight ? new Zombi(spot.x(), spot.y()) : new Saman(spot.x(), spot.y());
    }

    /**
     * Derinliğe göre can/güç/savunma bonusu uygular.
     *
     * <p>Savunma en yavaş artan değer: hızlı artsaydı kılıcın kademesi geride
     * kaldığı anda düşmanlar delinmez olurdu. Oyuncunun ekipman kademesi
     * {@link LootTable} ile 3 katta bir yükseliyor, düşman savunması 4 katta
     * bir — yani ilerleme hep oyuncunun lehine, ama fark kapanmıyor.</p>
     */
    private void applyDepthBonus(Enemy enemy, int depth, Difficulty difficulty) {
        enemy.strengthen(
                difficulty.scaleDepthBonus((depth - 1) / DEPTHS_PER_HP_BONUS),
                difficulty.scaleDepthBonus((depth - 1) / DEPTHS_PER_ATTACK_BONUS),
                difficulty.scaleDepthBonus((depth - 1) / DEPTHS_PER_DEFENSE_BONUS));
    }

    /**
     * Kata iksir, altın ve birer ekipman parçası dağıtır.
     *
     * <p>Ekipmanın kademesi rastgele değil, tamamen derinliğe bağlı
     * ({@link LootTable}): 2. katta bulduğun zırh 4. kattakinden iyi olamaz.</p>
     */
    /** Kat olayının fazladan koyduğu nadir eşya sayısı; olay yoksa sıfır. */
    private static int extraItems(FloorEvent event) {
        return event == null ? 0 : event.getExtraItems();
    }

    private List<Item> placeItems(List<Position> spots, Set<Position> used, int depth,
                                  int extraRolls) {
        List<Item> items = new ArrayList<>();
        int tier = LootTable.tierForDepth(depth);
        int potions = 0;
        int goldPiles = 0;
        boolean weaponPlaced = false;
        boolean armorPlaced = false;
        int rareItems = 0;
        boolean legendRolled = false;

        for (Position spot : spots) {
            if (used.contains(spot)) {
                continue;
            }

            if (potions < POTIONS_PER_FLOOR) {
                items.add(new Potion(spot.x(), spot.y()));
                potions++;
            } else if (goldPiles < GOLD_PILES_PER_FLOOR) {
                int amount = MIN_GOLD + random.nextInt(MAX_GOLD - MIN_GOLD + 1);
                items.add(new Gold(spot.x(), spot.y(), amount));
                goldPiles++;
            } else if (!weaponPlaced) {
                items.add(LootTable.weaponForTier(tier, spot.x(), spot.y()));
                weaponPlaced = true;
            } else if (!armorPlaced) {
                items.add(LootTable.armorForTier(tier, spot.x(), spot.y()));
                armorPlaced = true;

            } else if (!legendRolled) {
                // Efsanevi kılıç kendi zarını atıyor; nadir eşyalarla aynı
                // havuzda olsaydı biri diğerinin şansını yerdi.
                legendRolled = true;
                if (random.nextDouble() < LEGEND_CHANCE) {
                    items.add(new LegendWeapon(spot.x(), spot.y()));
                } else {
                    continue;
                }

            } else if (rareItems < RARE_ITEM_ROLLS + extraRolls) {
                // Nadir eşyalar için tek tek zar atılıyor; tutmayan zar kareyi
                // boş bırakıyor, yani "her katta bir tane" olmuyor.
                //
                // Kat olayının fazlaları ise zar atmıyor: "zengin kat" ancak
                // gerçekten zenginse bir şey söylüyor. Zara bağlasaydım üç
                // fazladan zar ortalama yarım eşya ederdi ve okuduğun cümle
                // yalan olurdu.
                rareItems++;
                if (rareItems > RARE_ITEM_ROLLS || random.nextDouble() < RARE_ITEM_CHANCE) {
                    items.add(rollRareItem(spot));
                } else {
                    continue;
                }

            } else {
                return items;
            }

            used.add(spot);
        }
        return items;
    }

    /**
     * Nadir eşyalardan birini seçer.
     *
     * <p>Dördü de tüketilen ve dördü de farklı bir soruna cevap: bomba
     * kalabalığa, öfke tek hedefe, hız sıkışmaya, kaçış uzaklığa. Eşit
     * olasılık veriyorum — birini diğerinden nadir yapmak, oyuncunun hangisini
     * saklayacağına dair kararını zarla almak olurdu.</p>
     */
    private Item rollRareItem(Position spot) {
        return switch (random.nextInt(4)) {
            case 0 -> new Bomb(spot.x(), spot.y());
            case 1 -> new HastePotion(spot.x(), spot.y());
            case 2 -> new FuryPotion(spot.x(), spot.y());
            default -> new EscapePotion(spot.x(), spot.y());
        };
    }
}
