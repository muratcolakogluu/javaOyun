package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Key;
import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Tile;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Kilitli mahzen: katin sordugu tek soru.
 *
 * <p>Harita bir <em>kap</em>ti -- icinden geciyordun, sana bir sey sormuyordu.
 * Mahzen kilitli bir kapinin arkasinda ve anahtari kattaki bir dusman
 * tasiyor. Buradaki sinavlar uc seyi kovaliyor: mahzen haritayi
 * <b>bozmuyor</b>, kilit gercekten kilit (yandan girilemiyor), ve anahtar
 * gerceketen bir hedef (kim tasidigi belli).</p>
 */
class VaultTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 22;

    /** Oyma isleminin kendisi: haritaya ne yapiyor. */
    @Nested
    class Carving {

        private final FloorBuilder builder =
                new FloorBuilder(List.of(new BspGenerator()), WIDTH, HEIGHT);

        /**
         * Mahzen kayaya oyuluyor, yani yalnizca yeni kare aciyor. Bu sinav
         * kuralin kendisini olcuyor: onceki yurunebilir karelerin hepsi
         * hala yerinde mi.
         */
        @Test
        @DisplayName("Mahzen hicbir kareyi kapatmiyor")
        void carvingOnlyOpensTiles() {
            for (long seed = 0; seed < 120; seed++) {
                Floor before = builder.layout(0, 1, seed);
                Set<Position> wasWalkable = Set.copyOf(before.dungeon().walkablePositions());

                // Ayni tohumla 4. kati kuruyoruz: harita ayni, tek fark
                // mahzenin oyulabilmesi.
                Floor after = builder.layout(0, 4, seed);
                if (after.vault() == null) {
                    continue;
                }

                Set<Position> nowWalkable = Set.copyOf(after.dungeon().walkablePositions());
                for (Position spot : wasWalkable) {
                    assertTrue(nowWalkable.contains(spot),
                            "Onceden yurunebilen kare kapanmis: " + spot);
                }
            }
        }

        /** Merdivene ulasilamayan bir kat oynanamaz; mahzen onu bozmamali. */
        @Test
        @DisplayName("Merdiven hala ulasilabilir")
        void thestairsStayReachable() {
            for (long seed = 0; seed < 120; seed++) {
                Floor floor = builder.layout(0, 4, seed);
                if (floor.vault() == null) {
                    continue;
                }

                assertTrue(floor.dungeon().walkableDistancesFrom(floor.spawn())
                                .containsKey(floor.stairs()),
                        "Mahzen merdiveni kapatmis");
            }
        }

        /**
         * Kilit ancak tek girisi varsa kilit: mahzene yandan girilebilseydi
         * anahtari bulmanin hicbir anlami olmazdi.
         */
        @Test
        @DisplayName("Mahzene yalnizca kapidan girilebiliyor")
        void theonlyWayInIsTheDoor() {
            for (long seed = 0; seed < 120; seed++) {
                Floor floor = builder.layout(0, 4, seed);
                Vault vault = floor.vault();
                if (vault == null) {
                    continue;
                }

                // Kapi kapaliyken mahzenin ici doguldugun yerden
                // ulasilamaz olmali.
                for (Position cell : vault.getInside()) {
                    assertFalse(floor.dungeon().walkableDistancesFrom(floor.spawn())
                                    .containsKey(cell),
                            "Mahzenin ici kapi kapaliyken ulasilabiliyor: " + cell);
                }
            }
        }

        @Test
        @DisplayName("Kapi kilitli ve icerisi yurunebilir")
        void thedoorIsLockedAndTheRoomIsFloor() {
            Vault vault = firstVault();

            assertEquals(Tile.DOOR_LOCKED,
                    tileAt(vault, vault.getDoor()), "Kapi kilitli olmali");
        }

        /** Mahzeni acan oyuncu bos bir oda bulmamali. */
        @Test
        @DisplayName("Mahzenin icinde ganimet var")
        void thevaultIsNotEmpty() {
            for (long seed = 0; seed < 200; seed++) {
                Floor floor = builder.layout(0, 6, seed);
                if (floor.vault() == null) {
                    continue;
                }

                assertFalse(floor.groundItems().isEmpty(), "Mahzen bos");
                for (Item item : floor.groundItems()) {
                    assertTrue(floor.vault().getInside().contains(item.getTile()),
                            "Mahzen ganimeti disarida: " + item.getName());
                }
                return;
            }
            throw new AssertionError("Iki yuz katta mahzen cikmadi");
        }

        /**
         * Mahzen bulunabilir olmali: kayada yer aranirken cok katiysak
         * ozellik oyunda hic gorunmez. Olcum, tasarim karari kadar onemli.
         */
        @Test
        @DisplayName("Mahzen pratikte kurulabiliyor")
        void vaultsActuallyGetCarved() {
            int carved = 0;
            int floors = 300;

            for (long seed = 0; seed < floors; seed++) {
                if (builder.layout(0, 6, seed).vault() != null) {
                    carved++;
                }
            }

            assertTrue(carved > floors / 10,
                    "Mahzen fazla seyrek kuruluyor: " + carved + "/" + floors);
            assertTrue(carved < floors / 2,
                    "Mahzen fazla sik: " + carved + "/" + floors);
        }

        @Test
        @DisplayName("Ilk iki katta ve boss katinda mahzen yok")
        void thevaultKeepsItsSchedule() {
            for (long seed = 0; seed < 150; seed++) {
                assertNull(builder.layout(0, 1, seed).vault());
                assertNull(builder.layout(0, 2, seed).vault());
                assertNull(builder.layout(0, 5, seed).vault());
            }
        }

        /** Kat kurulumu tohumun saf bir fonksiyonu; mahzen de buna dahil. */
        @Test
        @DisplayName("Ayni tohum ayni mahzeni veriyor")
        void theSameSeedGivesTheSameVault() {
            for (long seed = 0; seed < 60; seed++) {
                Vault first = builder.layout(0, 4, seed).vault();
                Vault second = builder.layout(0, 4, seed).vault();

                assertEquals(first == null, second == null);
                if (first != null) {
                    assertEquals(first.getDoor(), second.getDoor());
                    assertEquals(first.getInside(), second.getInside());
                }
            }
        }

        private Vault firstVault() {
            for (long seed = 0; seed < 300; seed++) {
                Floor floor = builder.layout(0, 4, seed);
                if (floor.vault() != null) {
                    vaultFloor = floor;
                    return floor.vault();
                }
            }
            throw new AssertionError("Mahzen cikmadi");
        }

        private Floor vaultFloor;

        private Tile tileAt(Vault vault, Position spot) {
            return vaultFloor.dungeon().getTile(spot.x(), spot.y());
        }
    }

    /** Anahtar: kim tasiyor, ne zaman dusuyor, ne aciyor. */
    @Nested
    class TheKey {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

        /** Mahzenli bir kat cikana kadar iner ve kati yeniden kurar. */
        private void findVaultFloor() {
            player.setTile(game.getStairs());
            assertTrue(game.descend());
            player.setTile(game.getStairs());
            assertTrue(game.descend());

            for (int attempt = 0; attempt < 300 && game.getVault() == null; attempt++) {
                game.regenerateFloor();
            }
            assertNotNull(game.getVault(), "Mahzenli kat cikmadi");
        }

        private Enemy keeper() {
            return game.getEnemies().stream()
                    .filter(Enemy::hasKey)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Anahtari tasiyan yok"));
        }

        @Test
        @DisplayName("Mahzenli katta anahtari biri tasiyor")
        void someoneCarriesTheKey() {
            findVaultFloor();

            assertEquals(1, game.getEnemies().stream().filter(Enemy::hasKey).count(),
                    "Tam bir tasiyici olmali");
        }

        /** Gorunmezse "dogru olani bul" bir hedef degil bir tarama olurdu. */
        @Test
        @DisplayName("Tasiyicinin adinda sifati var")
        void thekeeperWearsItsName() {
            findVaultFloor();

            assertTrue(keeper().getName().startsWith(Text.ENEMY_KEEPER.get()),
                    "Ad anahtarciyi soylemeli: " + keeper().getName());
        }

        @Test
        @DisplayName("Tasiyici olunce anahtar yere dusuyor")
        void thekeyDropsWhereItFell() {
            findVaultFloor();
            Enemy keeper = keeper();
            Position where = keeper.getTile();

            player.setTile(keeper.getTileX() + 1, keeper.getTileY());
            for (int i = 0; i < 900 && keeper.isAlive(); i++) {
                game.playerAttacks();
            }
            assertFalse(keeper.isAlive());

            boolean dropped = game.getGroundItems().stream()
                    .anyMatch(item -> item instanceof Key && item.getTile().equals(where));
            assertTrue(dropped, "Anahtar dustugu yerde olmali");
        }

        @Test
        @DisplayName("Anahtar yoksa kapi acilmiyor")
        void theLockedDoorStaysShut() {
            findVaultFloor();
            Position door = game.getVault().getDoor();
            player.setTile(door.x() + 1, door.y());

            assertFalse(game.openVault(), "Anahtarsiz acilmamali");
            assertEquals(Tile.DOOR_LOCKED, game.getDungeon().getTile(door.x(), door.y()));
        }

        @Test
        @DisplayName("Anahtarla kapi aciliyor ve anahtar harcaniyor")
        void thekeyOpensTheDoorOnce() {
            findVaultFloor();
            Position door = game.getVault().getDoor();
            game.getInventory().add(new Key(0, 0));
            player.setTile(door.x() + 1, door.y());

            assertTrue(game.openVault());

            assertEquals(Tile.FLOOR, game.getDungeon().getTile(door.x(), door.y()),
                    "Kapi zemine donmeli");
            assertFalse(game.hasVaultKey(), "Anahtar harcanmali");
        }

        /** Kapi kalkinca hazine bir adim atmayi beklemeden gorunmeli. */
        @Test
        @DisplayName("Kapi acilinca icerisi goruluyor")
        void openingLightsTheRoom() {
            findVaultFloor();
            Position door = game.getVault().getDoor();
            game.getInventory().add(new Key(0, 0));
            player.setTile(door.x() + 1, door.y());

            assertTrue(game.openVault());

            assertTrue(game.getVault().getInside().stream()
                            .anyMatch(cell -> game.getVision().isRemembered(cell.x(), cell.y())),
                    "Mahzenin ici gorulmus olmali");
        }

        /** F "buradakiyle bir sey yap" tusu; kapi da bir "burada". */
        @Test
        @DisplayName("Kapinin yaninda F mahzeni aciyor")
        void interactingOpensTheVault() {
            findVaultFloor();
            Position door = game.getVault().getDoor();
            game.getInventory().add(new Key(0, 0));

            for (Position spot : List.of(door.offset(1, 0), door.offset(-1, 0),
                    door.offset(0, 1), door.offset(0, -1))) {
                if (!game.getDungeon().isWalkable(spot.x(), spot.y())) {
                    continue;
                }

                player.setTile(spot);
                game.pickUp();
                if (game.isNearWizard() || game.isNearMerchant() || game.isNearShrine()
                        || !game.itemsUnderfoot().isEmpty()) {
                    continue;
                }

                assertTrue(game.interact());
                assertEquals(Tile.FLOOR, game.getDungeon().getTile(door.x(), door.y()));
                return;
            }
            throw new AssertionError("Kapinin yaninda temiz bir kare bulunamadi");
        }

        @Test
        @DisplayName("Mahzenin icine dusman dogmuyor")
        void nothingLurksInsideTheVault() {
            findVaultFloor();

            for (Enemy enemy : game.getEnemies()) {
                assertFalse(game.getVault().getInside().contains(enemy.getTile()),
                        "Kilitli odada dusman var: " + enemy.getName());
            }
        }
    }
}
