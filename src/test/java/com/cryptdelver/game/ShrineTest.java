package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Shrine;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Tile;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Kader tasi: bedeli altin olmayan tek tezgah.
 *
 * <p>Oyundaki her karar altinla odeniyordu -- buyucu, satici, hepsi ayni
 * keseyi bosaltiyordu. Tas ikinci bir para birimi aciyor ve o para birimi
 * oyuncunun kendi omru. Buradaki sinavlar takasin gercekten bir <b>bedeli</b>
 * oldugunu ve tasin bir kez verdigini kovaliyor.</p>
 */
class ShrineTest {

    /** Teklifin kendisi: tas ne veriyor, neye gore seciyor. */
    @Nested
    class Offer {

        private Player player;
        private Game game;

        @BeforeEach
        void setUp() {
            Dungeon dungeon = new Dungeon(11, 9);
            dungeon.fill(Tile.FLOOR);
            player = new Player(4, 4);
            game = new Game(dungeon, player);
        }

        private Shrine stone() {
            return new Shrine(5, 4, 0);
        }

        /**
         * Takimi tavana dayanmis bir oyuncuya "sana verecek bir seyim yok"
         * demek, tasi bulmanin hicbir anlami olmadigi anlamina gelirdi.
         */
        @Test
        @DisplayName("Tasin her oyuncuya bir teklifi var")
        void thereIsAlwaysAnOffer() {
            assertNotNull(stone().offerFor(game), "Ciplak elle gelene de bir sey vermeli");
        }

        @Test
        @DisplayName("Eli bos oyuncuya kilic veriyor")
        void theBarehandedGetABlade() {
            Shrine.Boon boon = stone().offerFor(game);

            assertEquals(Shrine.Boon.SILAH, boon, "Kilici olmayana once kilic");
        }

        /**
         * Teklif her soruldugunda yeniden hesaplaniyor: takimini
         * degistirdiysen tas da fikrini degistiriyor.
         */
        @Test
        @DisplayName("Takim degisince teklif de degisiyor")
        void theOfferFollowsYourKit() {
            Shrine stone = stone();
            assertEquals(Shrine.Boon.SILAH, stone.offerFor(game));

            player.equip(LootTable.weaponForTier(1, 0, 0));

            assertFalse(stone.offerFor(game) == Shrine.Boon.SILAH,
                        "Kilicin varken kilic teklifi anlamsiz");
        }

        @Test
        @DisplayName("Her teklifin bir bedeli var")
        void everyBoonCosts() {
            for (Shrine.Boon boon : Shrine.Boon.values()) {
                assertTrue(boon.getCost() > 0, boon + " bedava duruyor");
                assertFalse(boon.getLabel().isBlank(), boon + " ne verdigini soylemiyor");
            }
        }
    }

    /** Takasin kendisi: bedel aliniyor mu, karsiligi veriliyor mu. */
    @Nested
    class Trade {

        private Player player;
        private Game game;
        private Shrine stone;

        @BeforeEach
        void setUp() {
            Dungeon dungeon = new Dungeon(11, 9);
            dungeon.fill(Tile.FLOOR);
            player = new Player(4, 4);
            game = new Game(dungeon, player);
            stone = new Shrine(5, 4, 0);
        }

        /**
         * Bedel canin tavani, o anki canin degil: yalnizca cani dusurseydi
         * bir iksirle geri alinabilir olurdu ve bedel diye bir sey kalmazdi.
         */
        @Test
        @DisplayName("Takas azami candan aliyor")
        void thepriceIsPaidInMaxHealth() {
            int before = player.getMaxHp();
            Shrine.Boon boon = stone.offerFor(game);

            assertNotNull(stone.accept(game));

            assertEquals(before - boon.getCost(), player.getMaxHp(), "Tavan dusmeli");
        }

        @Test
        @DisplayName("Mevcut can yeni tavani asamiyor")
        void currentHealthFollowsTheCeiling() {
            stone.accept(game);

            assertTrue(player.getHp() <= player.getMaxHp());
        }

        /**
         * Tasin isi secenek sunmak degil, tek bir teklifi masaya koymak.
         * Ikinci bir teklif olsaydi reddetmek anlamsizlasirdi.
         */
        @Test
        @DisplayName("Tas bir kez veriyor")
        void thestoneGivesOnce() {
            assertNotNull(stone.accept(game));
            assertTrue(stone.isSpent());

            int after = player.getMaxHp();
            assertNull(stone.accept(game), "Ikinci takas olmamali");
            assertEquals(after, player.getMaxHp(), "Ikinci kez bedel de alinmamali");
            assertNull(stone.offerFor(game));
        }

        @Test
        @DisplayName("Kilic teklifi gercekten kilic veriyor")
        void thebladeOfferArmsYou() {
            assertEquals(Shrine.Boon.SILAH, stone.offerFor(game));

            stone.accept(game);

            assertNotNull(player.getEquippedWeapon(), "Kilic elinde olmali");
        }

        @Test
        @DisplayName("Celik teklifi kilici bir kademe buyutuyor")
        void thesteelOfferGrowsTheBlade() {
            Weapon blade = LootTable.weaponForTier(1, 0, 0);
            game.getInventory().add(blade);
            player.equip(blade);
            int before = blade.getBonus();

            // Sira CELIK'ten baslasin diye tasi oradan kuruyoruz.
            Shrine steel = new Shrine(5, 4, Shrine.Boon.CELIK.ordinal());
            assertEquals(Shrine.Boon.CELIK, steel.offerFor(game));
            steel.accept(game);

            assertTrue(blade.getBonus() > before, "Kilic buyumeli");
        }

        @Test
        @DisplayName("Kabuk teklifi zirhi bir kademe buyutuyor")
        void theshellOfferGrowsTheArmour() {
            Armor mail = LootTable.armorForTier(1, 0, 0);
            game.getInventory().add(mail);
            player.equip(mail);
            int before = mail.getBonus();

            Shrine shell = new Shrine(5, 4, Shrine.Boon.KABUK.ordinal());
            assertEquals(Shrine.Boon.KABUK, shell.offerFor(game));
            shell.accept(game);

            assertTrue(mail.getBonus() > before, "Zirh buyumeli");
        }

        @Test
        @DisplayName("Kese teklifi altin veriyor")
        void thepurseOfferPays() {
            Shrine purse = new Shrine(5, 4, Shrine.Boon.KESE.ordinal());
            assertEquals(Shrine.Boon.KESE, purse.offerFor(game));

            purse.accept(game);

            assertTrue(game.getGold() > 0, "Kese dolmali");
        }

        /** F "buradakiyle bir sey yap" tusu; tas da bir "burada". */
        @Test
        @DisplayName("Tasin yaninda F takasi yapiyor")
        void touchingTheStoneTrades() {
            Dungeon dungeon = new Dungeon(40, 22);
            dungeon.fill(Tile.FLOOR);
            Player hero = new Player(0, 0);
            Game floors = new Game(List.of(new BspGenerator()), 40, 22, hero);

            for (int attempt = 0; attempt < 60 && floors.getShrine() == null; attempt++) {
                floors.regenerateFloor();
            }
            assertNotNull(floors.getShrine(), "Altmis katta bir tas cikmadi");

            hero.setTile(floors.getShrine().getTileX() + 1, floors.getShrine().getTileY());
            floors.pickUp();
            int before = hero.getMaxHp();

            assertTrue(floors.interact());

            assertTrue(hero.getMaxHp() < before, "Bedel odenmeli");
            assertTrue(floors.getShrine().isSpent());
        }
    }

    /** Katin uzerinde: tasin nerede durdugu. */
    @Nested
    class Placement {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), 40, 22, player);

        private Shrine findShrine() {
            for (int attempt = 0; attempt < 60; attempt++) {
                if (game.getShrine() != null) {
                    return game.getShrine();
                }
                game.regenerateFloor();
            }
            throw new AssertionError("Altmis katta bir tas cikmadi");
        }

        @Test
        @DisplayName("Boss katinda tas yok")
        void bossFloorsHaveNoShrine() {
            for (int i = 0; i < 4; i++) {
                player.setTile(game.getStairs());
                assertTrue(game.descend());
            }
            assertTrue(game.isBossFloor());

            for (int attempt = 0; attempt < 10; attempt++) {
                assertNull(game.getShrine(), "Boss katinda tas olmamali");
                game.regenerateFloor();
            }
        }

        @Test
        @DisplayName("Tasin karesinden gecilemez")
        void theStoneBlocksItsTile() {
            Shrine stone = findShrine();

            assertFalse(game.isTileFree(stone.getTileX(), stone.getTileY(), player));
        }

        @Test
        @DisplayName("Tas merdivenin yolunu kesmiyor")
        void theStoneNeverSealsTheFloor() {
            for (int attempt = 0; attempt < 15; attempt++) {
                Shrine stone = findShrine();

                assertTrue(game.getDungeon()
                                .walkableDistancesFrom(player.getTile(), Set.of(stone.getTile()))
                                .containsKey(game.getStairs()),
                        "Tas kapaliyken de merdivene ulasilabilmeli");

                game.regenerateFloor();
            }
        }

        /** Uc tezgahin da kendi kosesi olmali, yoksa F hangisini acacagi belirsiz kalir. */
        @Test
        @DisplayName("Tas diger tezgahlarin dibine kurulmuyor")
        void theStoneKeepsItsDistance() {
            for (int attempt = 0; attempt < 80; attempt++) {
                game.regenerateFloor();
                if (game.getShrine() == null) {
                    continue;
                }

                Position stone = game.getShrine().getTile();
                if (game.getMerchant() != null) {
                    assertFalse(neighbours(stone, game.getMerchant().getTile()),
                            "Tas ve satici komsu karelerde durmamali");
                }
                if (game.getWizard() != null) {
                    assertFalse(neighbours(stone, game.getWizard().getTile()),
                            "Tas ve buyucu komsu karelerde durmamali");
                }
            }
        }

        private boolean neighbours(Position one, Position other) {
            return Math.abs(one.x() - other.x()) <= 1 && Math.abs(one.y() - other.y()) <= 1;
        }

        /** Kat kurulumu tohumun saf bir fonksiyonu; tas da buna dahil. */
        @Test
        @DisplayName("Ayni tohum ayni tasi veriyor")
        void theSameSeedGivesTheSameStone() {
            FloorBuilder builder = new FloorBuilder(List.of(new BspGenerator()), 40, 22);

            Floor first = builder.layout(0, 4, 777L);
            Floor second = builder.layout(0, 4, 777L);

            assertEquals(first.shrine() == null, second.shrine() == null);
            if (first.shrine() != null) {
                assertEquals(first.shrine().getTile(), second.shrine().getTile());
            }
        }
    }
}
