package com.cryptdelver.game;

import com.cryptdelver.entity.Archer;
import com.cryptdelver.entity.Bomb;
import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.EscapePotion;
import com.cryptdelver.entity.FuryPotion;
import com.cryptdelver.entity.Goblin;
import com.cryptdelver.entity.Gold;
import com.cryptdelver.entity.HastePotion;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.LegendWeapon;
import com.cryptdelver.entity.Orc;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Saman;
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
     *   <li><b>Diğer katlar sırayla değişiyor:</b> tek katlar odalı, çift
     *       katlar mağara. Aynı görüntüde üst üste inmiyorsun.</li>
     * </ul>
     */
    public int generatorForDepth(int depth) {
        if (generators.size() < 2 || isBossFloor(depth)) {
            return 0;
        }
        return depth % 2 == 0 ? 1 : 0;
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
     * <p>Hepsi tohumdan belirlenimci olarak çıkıyor. Kayıt yüklerken de bu
     * çağrılıyor: aynı tohum aynı döşemeyi verdiği için merdiven ve büyücü
     * kayıt dosyasında saklanmak zorunda değil.</p>
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

        Wizard wizard = hasWizard(depth, seed) ? placeWizard(dungeon, spawn, stairs) : null;

        return new Floor(seed, dungeon, spawn, stairs, wizard, null, List.of(), List.of());
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
     * <p>Zar katın <em>tohumundan</em> atılıyor, genel rastgelelikten değil.
     * Sebebi kayıt: kaydı yüklerken kat tohumdan yeniden kuruluyor, genel
     * rastgelelikten zar atsaydık büyücü kaydettiğinde varken yüklediğinde yok
     * olabilirdi.</p>
     */
    private boolean hasWizard(int depth, long seed) {
        return isBossFloor(depth)
                || new Random(seed ^ WIZARD_ROLL_SALT).nextDouble() < WIZARD_WANDER_CHANCE;
    }

    /**
     * Büyücüyü doğulan yerin yakınına koyar.
     *
     * <p>Merdivenin yanına koymak cazipti ama orada boss duruyor: büyücüyü
     * dövüşün ortasına yerleştirmiş olurduk. Girişin dibinde olması daha doğru
     * — kata inip önce hazırlanıyor, sonra bossa yürüyorsun.</p>
     *
     * <p>Yerleştirme <em>rastgele değil</em>: doğulan yerden yayılan BFS'in
     * sırasında ilk uygun kare seçiliyor, yani en yakını. Merdiven gibi bu da
     * katın sabit döşemesi.</p>
     */
    private Wizard placeWizard(Dungeon dungeon, Position spawn, Position stairs) {
        Map<Position, Integer> distances = dungeon.walkableDistancesFrom(spawn);

        // BFS sırası yakından uzağa; ilk uyan kare en yakın uygun kare oluyor.
        for (Map.Entry<Position, Integer> candidate : distances.entrySet()) {
            Position spot = candidate.getKey();

            if (candidate.getValue() < WIZARD_MIN_DISTANCE || spot.equals(stairs)) {
                continue;
            }
            if (wouldSealTheFloor(dungeon, spot, spawn, stairs)) {
                continue;
            }

            return new Wizard(spot.x(), spot.y());
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

        List<Enemy> enemies = new ArrayList<>();
        Boss boss = spawnBoss(floor, depth, difficulty);
        if (boss != null) {
            enemies.add(boss);
        }

        int target = enemyCountForDepth(depth, difficulty);
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

        return floor.filledWith(boss, enemies, placeItems(spots, used, depth));
    }

    /** Boss merdivenin üstünde doğar: geçmek için onu yenmen gerekiyor. */
    private Boss spawnBoss(Floor floor, int depth, Difficulty difficulty) {
        if (!isBossFloor(depth) || floor.stairs() == null) {
            return null;
        }

        int number = bossNumber(depth);
        Boss boss = new Boss(floor.stairs().x(), floor.stairs().y(), number);
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
    private List<Item> placeItems(List<Position> spots, Set<Position> used, int depth) {
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

            } else if (rareItems < RARE_ITEM_ROLLS) {
                // Nadir eşyalar için tek tek zar atılıyor; tutmayan zar kareyi
                // boş bırakıyor, yani "her katta bir tane" olmuyor.
                rareItems++;
                if (random.nextDouble() < RARE_ITEM_CHANCE) {
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
