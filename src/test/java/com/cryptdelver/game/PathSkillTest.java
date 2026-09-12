package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Yol yetenekleri: Q tusunun yola gore degismesi.
 *
 * <p>Uc yol vardi ama ucu de yalnizca baslangic takimiyla ayrisiyordu: ilk
 * kattan sonra elindeki kilic neyse oyle oynuyordun ve Q hepsinde ayni seyi
 * yapiyordu. Yani secim ilk bes dakikaya aitti. Buradaki sinavlar yetenegin
 * gercekten yola bagli oldugunu ve ucunun <b>farkli bir sikismayi</b>
 * cozdugunu kovaliyor.</p>
 */
class PathSkillTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(21, 11);
        dungeon.fill(Tile.FLOOR);
        player = new Player(10, 5);
        game = new Game(dungeon, player);
    }

    private void tick() {
        game.update(FRAME);
    }

    /** Yetenekleri birbirinden ayiran sey. */
    @Nested
    class Identity {

        /**
         * Ucu de "kac" ya da ucu de "vur" olsaydi yol secimi bir renk secimi
         * olurdu.
         */
        @Test
        @DisplayName("Uc yolun uc ayri yetenegi var")
        void eachPathHasItsOwn() {
            Set<PathSkill> skills = new HashSet<>();
            for (StartPath path : StartPath.values()) {
                skills.add(path.getSkill());
            }

            assertEquals(StartPath.values().length, skills.size(),
                    "Iki yol ayni yetenegi paylasmamali");
        }

        @Test
        @DisplayName("Her yetenek adini ve ne yaptigini soyluyor")
        void everySkillIntroducesItself() {
            for (PathSkill skill : PathSkill.values()) {
                assertFalse(skill.getLabel().isBlank(), skill + " adsiz");
                assertFalse(skill.getDescription().isBlank(), skill + " aciklamasiz");
                assertTrue(skill.getCooldown() > 0, skill + " beklemesiz");
            }
        }

        /**
         * Kalabaligi durdurmak oyundaki en guclu sey; sik olsa dovusun ritmi
         * diye bir sey kalmazdi.
         */
        @Test
        @DisplayName("Sarsinti en uzun bekleyen yetenek")
        void theShockwaveWaitsLongest() {
            for (PathSkill skill : PathSkill.values()) {
                if (skill != PathSkill.SARSINTI) {
                    assertTrue(PathSkill.SARSINTI.getCooldown() > skill.getCooldown(),
                            "Sarsinti " + skill + "'den uzun beklemeli");
                }
            }
        }

        /** Buyu "yer degistirmek senin icin ucuz" diyor; digerleri yer degistirme degil. */
        @Test
        @DisplayName("Ceviklik yalnizca sicramayi kisaltiyor")
        void agilityOnlyHelpsTheDash() {
            com.cryptdelver.entity.Armor mail =
                    LootTable.armorForTier(1, 0, 0);
            mail.enchant(com.cryptdelver.entity.Enchantment.CEVIKLIK);
            player.equip(mail);

            assertTrue(PathSkill.SICRAMA.cooldownFor(player) < PathSkill.SICRAMA.getCooldown());
            assertEquals(PathSkill.SARSINTI.getCooldown(),
                    PathSkill.SARSINTI.cooldownFor(player), 0.001);
            assertEquals(PathSkill.RUSVET.getCooldown(),
                    PathSkill.RUSVET.cooldownFor(player), 0.001);
        }

        @Test
        @DisplayName("Yol secimi yetenegi de ogretiyor")
        void choosingAPathTeachesItsSkill() {
            Game run = new Game(List.of(new BspGenerator()), 40, 22, new Player(0, 0));

            for (StartPath path : StartPath.values()) {
                run.setStartPath(path);
                run.restart();

                assertEquals(path.getSkill(), run.getPlayer().getSkill(),
                        path + " yetenegini vermemis");
            }
        }
    }

    /** Sarsinti: kusatilmisligi cozen yetenek. */
    @Nested
    class Shockwave {

        @Test
        @DisplayName("Yanindaki herkes sersemliyor")
        void everyoneBesideYouIsStaggered() {
            Skeleton left = new Skeleton(9, 5);
            Skeleton right = new Skeleton(11, 5);
            Imp far = new Imp(15, 5);
            game.addEnemy(left);
            game.addEnemy(right);
            game.addEnemy(far);

            assertTrue(game.shockwave());

            assertTrue(left.isStaggered());
            assertTrue(right.isStaggered());
            assertFalse(far.isStaggered(), "Uzaktaki etkilenmemeli");
        }

        /** Bu bir saldiri degil bir cikis. */
        @Test
        @DisplayName("Sarsinti hasar vermiyor")
        void theShockwaveDoesNoDamage() {
            Skeleton bones = new Skeleton(11, 5);
            game.addEnemy(bones);
            int before = bones.getHp();

            game.shockwave();

            assertEquals(before, bones.getHp());
        }

        /** Bosluga sarsinti yapmak yetenegi elinden almamali. */
        @Test
        @DisplayName("Kimse yoksa bekleme harcanmiyor")
        void anEmptyShockwaveCostsNothing() {
            player.learn(PathSkill.SARSINTI);

            player.requestDash();
            tick();

            assertTrue(player.canDash(), "Bosa giden yetenek bekleme baslatmamali");
        }

        @Test
        @DisplayName("Sarsinti Q ile isliyor")
        void theKeyTriggersIt() {
            player.learn(PathSkill.SARSINTI);
            Skeleton bones = new Skeleton(11, 5);
            game.addEnemy(bones);

            player.requestDash();
            tick();

            assertTrue(bones.isStaggered());
            assertFalse(player.canDash(), "Isleyen yetenek bekleme baslatmali");
        }
    }

    /** Rusvet: dovuse hic girmemeyi satin almak. */
    @Nested
    class Bribe {

        @Test
        @DisplayName("Yakindakiler dagiliyor ve altin dusuyor")
        void goldScattersTheNearby() {
            Skeleton bones = new Skeleton(12, 5);
            game.addEnemy(bones);
            game.addGold(100);

            assertTrue(game.bribe());

            assertTrue(bones.isFrightened());
            assertTrue(game.getGold() < 100, "Kese eksilmeli");
        }

        /** Tuccarin tek sinirli yetenegi bu: kesesi bossa calismiyor. */
        @Test
        @DisplayName("Altin yetmezse rusvet olmuyor")
        void anEmptyPurseBuysNoPeace() {
            Skeleton bones = new Skeleton(12, 5);
            game.addEnemy(bones);

            assertFalse(game.bribe());

            assertFalse(bones.isFrightened());
            assertEquals(0, game.getGold());
        }

        @Test
        @DisplayName("Kimse yakinda yoksa altin harcanmiyor")
        void nobodyNearbyKeepsTheGold() {
            game.addGold(100);
            game.addEnemy(new Imp(20, 5));

            assertFalse(game.bribe());

            assertEquals(100, game.getGold(), "Bosa giden rusvet altin almamali");
        }

        /** Oldurseydi bu bir yetenek degil bir para-silah olurdu. */
        @Test
        @DisplayName("Korkan dusman olmuyor, uzaklasiyor")
        void thefrightenedRunRatherThanDie() {
            Skeleton bones = new Skeleton(12, 5);
            game.addEnemy(bones);
            game.addGold(100);
            int before = bones.getHp();

            game.bribe();
            int distance = bones.tileDistanceTo(player);
            for (int i = 0; i < 60; i++) {
                tick();
            }

            assertTrue(bones.isAlive(), "Rusvet oldurmuyor");
            assertEquals(before, bones.getHp(), "Hasar da vermiyor");
            assertTrue(bones.tileDistanceTo(player) > distance, "Uzaklasmali");
        }

        @Test
        @DisplayName("Korku geciyor, dusman geri geliyor")
        void thefrightWearsOff() {
            Skeleton bones = new Skeleton(12, 5);
            game.addEnemy(bones);
            bones.frighten(0.4);

            for (int i = 0; i < 60; i++) {
                tick();
            }

            assertFalse(bones.isFrightened());
        }

        /** Rusvet bossa da isliyor; bunun ozel bir kurali yok. */
        @Test
        @DisplayName("Korku turun kendi kararini eziyor")
        void frightOverridesTheStance() {
            Enemy archer = new com.cryptdelver.entity.Archer(13, 5);
            game.addEnemy(archer);
            archer.frighten(2.0);
            int distance = archer.tileDistanceTo(player);

            for (int i = 0; i < 60; i++) {
                tick();
            }

            assertTrue(archer.tileDistanceTo(player) > distance,
                    "Hat tutan okcu bile kacmali");
        }
    }
}
