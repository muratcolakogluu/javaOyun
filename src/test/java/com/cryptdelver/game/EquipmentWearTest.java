package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kusanilan parcalarin yipranmasi.
 *
 * <p>Buradaki en onemli kural yipranmanin <em>kullanima</em> bagli olmasi:
 * gecen sure ya da atilan adim degil, vurulan darbe ve yenilen darbe
 * yipratiyor. Testler bu ikisini ayri ayri kovaliyor.</p>
 */
class EquipmentWearTest {

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(11, 9);
        dungeon.fill(Tile.FLOOR);
        for (int i = 0; i < 11; i++) {
            dungeon.setTile(i, 0, Tile.WALL);
            dungeon.setTile(i, 8, Tile.WALL);
        }
        for (int i = 0; i < 9; i++) {
            dungeon.setTile(0, i, Tile.WALL);
            dungeon.setTile(10, i, Tile.WALL);
        }

        player = new Player(4, 4);
        game = new Game(dungeon, player);
    }

    private Weapon sword(int durability) {
        return new Weapon(0, 0, "Test Kilici", 4, "sword", durability);
    }

    private Armor mail(int durability) {
        return new Armor(0, 0, "Test Zirhi", 2, "armor_chain", durability);
    }

    @Test
    @DisplayName("Isabet eden vurus silahi yipratir")
    void landedHitsWearTheWeapon() {
        Weapon weapon = sword(10);
        player.equip(weapon);
        game.addEnemy(new Skeleton(5, 4));

        game.playerAttacks();

        assertEquals(9, weapon.getDurability());
    }

    /**
     * Bosa savurus yipratmasaydi ya da yipratsaydi fark ederdi: yipranma
     * "is gordun" karsiligi, "tusa bastin" karsiligi degil.
     */
    @Test
    @DisplayName("Bosa savurus silahi yipratmaz")
    void missingDoesNotWearTheWeapon() {
        Weapon weapon = sword(10);
        player.equip(weapon);

        game.playerAttacks();

        assertEquals(10, weapon.getDurability(), "Kimseye degmedi");
    }

    @Test
    @DisplayName("Yenilen darbe zirhi yipratir")
    void takingHitsWearsTheArmor() {
        Armor armor = mail(10);
        player.equip(armor);
        Skeleton skeleton = new Skeleton(5, 4);
        game.addEnemy(skeleton);

        game.enemyAttacksPlayer(skeleton);

        assertEquals(9, armor.getDurability());
    }

    @Test
    @DisplayName("Vurmak zirhi, darbe yemek silahi yipratmaz")
    void wearStaysOnItsOwnSide() {
        Weapon weapon = sword(10);
        Armor armor = mail(10);
        player.equip(weapon);
        player.equip(armor);
        Skeleton skeleton = new Skeleton(5, 4);
        game.addEnemy(skeleton);

        game.playerAttacks();
        game.enemyAttacksPlayer(skeleton);

        assertEquals(9, weapon.getDurability(), "Yalnizca vurus silahi yipratmali");
        assertEquals(9, armor.getDurability(), "Yalnizca darbe zirhi yipratmali");
    }

    @Test
    @DisplayName("Dayanikliligi biten parca kirilir ve bonusunu tumden kaybeder")
    void brokenGearGivesNothing() {
        Weapon weapon = sword(1);
        player.equip(weapon);
        game.addEnemy(new Skeleton(5, 4));

        assertEquals(4, weapon.getAttackBonus(), "Saglamken tam bonus");

        game.playerAttacks();

        assertTrue(weapon.isBroken());
        assertEquals(0, weapon.getAttackBonus(), "Kirikken hicbir sey");
    }

    /** Zirh da ayni kurala tabi: kirilinca savunma sifirlaniyor. */
    @Test
    @DisplayName("Kirik zirh hic korumuyor")
    void brokenArmourStopsProtecting() {
        Armor armor = mail(1);
        player.equip(armor);
        Skeleton skeleton = new Skeleton(5, 4);
        game.addEnemy(skeleton);

        assertEquals(2, player.getDefense(), "Saglamken koruyor");

        game.enemyAttacksPlayer(skeleton);

        assertTrue(armor.isBroken());
        assertEquals(0, player.getDefense(), "Kirik zirh zirhsizlikla ayni");
    }

    /**
     * Asil soru bonus degeri degil, dovuste ne oldugu.
     *
     * <p>Bu sinav olmadan "kirilma var mi" sorusuna bakarak yanit
     * veremiyorduk: bonusun dusmesi tek basina hasarin dustugunu
     * kanitlamiyor. Ayni dusmana saglam ve kirik kilicla vurup toplam hasari
     * karsilastiriyoruz.</p>
     *
     * <p>Oran nicin tam sifir degil: bonus gitse de oyuncunun taban vurusu
     * (4) duruyor, yani kirik kilicla yumruk atiyorsun. Olculen fark yariya
     * yakin; zirhli dusmanda daha da acilir, cunku savunma once tabani
     * yiyor.</p>
     */
    @Test
    @DisplayName("Kirik kilic gozle gorulur daha az hasar veriyor")
    void brokenWeaponsHitSofter() {
        int healthy = totalDamageOverSwings(sword(500));

        Weapon broken = sword(1);
        broken.restoreState(0, 0);
        int damaged = totalDamageOverSwings(broken);

        assertTrue(damaged < healthy * 0.6,
                "Kirik kilic belirgin sekilde az vurmali: saglam " + healthy
                        + ", kirik " + damaged);
    }

    /** Verilen silahla dayanikli bir kuklaya 40 kez vurup toplam hasari olcer. */
    private int totalDamageOverSwings(Weapon weapon) {
        Player striker = new Player(4, 4);
        Game arena = new Game(game.getDungeon(), striker);
        striker.equip(weapon);

        Skeleton dummy = new Skeleton(5, 4);
        dummy.strengthen(10_000, 0, 0);
        arena.addEnemy(dummy);

        int before = dummy.getHp();
        for (int i = 0; i < 40; i++) {
            arena.playerAttacks();
        }
        return before - dummy.getHp();
    }

    /**
     * Kirik takimla dovusmek hala mumkun, cunku oyuncunun taban vurusu
     * duruyor. Ceza sert ama oyunu bitiren bir sey degil: bir sonraki
     * buyucuye kadar kotu dovusuyorsun, catisamaz hale gelmiyorsun.
     */
    @Test
    @DisplayName("Kirik kilicla hala vurulabiliyor")
    void aBrokenWeaponStillLetsYouFight() {
        Weapon weapon = sword(1);
        player.equip(weapon);
        Skeleton skeleton = new Skeleton(5, 4);
        game.addEnemy(skeleton);
        game.playerAttacks();

        assertTrue(weapon.isBroken());
        assertTrue(player.getAttackPower() > 0, "Taban vurus duruyor");

        int before = skeleton.getHp();
        game.playerAttacks();

        assertTrue(skeleton.getHp() < before, "Kirik kilicla da hasar veriliyor");
    }

    @Test
    @DisplayName("Kirilan parca daha fazla yipranmaz")
    void brokenGearStopsWearing() {
        Weapon weapon = sword(1);
        player.equip(weapon);
        game.addEnemy(new Skeleton(5, 4));

        game.playerAttacks();
        game.playerAttacks();

        assertEquals(0, weapon.getDurability(), "Eksiye dusmemeli");
    }

    @Test
    @DisplayName("Kirilma mesaj kaydina yazilir")
    void breakingIsAnnounced() {
        Weapon weapon = sword(1);
        player.equip(weapon);
        game.addEnemy(new Skeleton(5, 4));

        game.playerAttacks();

        assertTrue(game.getMessageLog().latest(5).stream().anyMatch(line -> line.contains("kirildi")
                        || line.contains("kırıldı")),
                "Oyuncu neden zayifladigini gorebilmeli");
    }

    @Test
    @DisplayName("Tamir dayanikliligi doldurur")
    void repairRefillsDurability() {
        Weapon weapon = sword(10);
        player.equip(weapon);
        game.addEnemy(new Skeleton(5, 4));
        game.playerAttacks();

        weapon.repair();

        assertEquals(10, weapon.getDurability());
        assertFalse(weapon.isBroken());
    }

    @Test
    @DisplayName("Kirik parca tamir edilince eski gucune doner")
    void repairRestoresTheFullBonus() {
        Weapon weapon = sword(1);
        player.equip(weapon);
        game.addEnemy(new Skeleton(5, 4));
        game.playerAttacks();

        weapon.repair();

        assertEquals(4, weapon.getAttackBonus());
    }
}
