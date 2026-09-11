package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Baslangic yollari: kosunun ilk karedeki kimligi.
 *
 * <p>Her kosu birebir ayni basliyordu -- ciplak elle, 1. katta -- ve ilk
 * gercek karar ucuncu kata kadar gelmiyordu. Zindan her seferinde farkli
 * doguyordu ama oyuncunun kendisi degil. Buradaki sinavlar uc yolun gercekten
 * <b>farkli</b> oldugunu ve secimin kosu boyunca durdugunu kovaliyor.</p>
 */
class StartPathTest {

    private Game newGame() {
        return new Game(List.of(new BspGenerator()), 40, 22, new Player(0, 0));
    }

    private Game startedAs(StartPath path) {
        Game game = newGame();
        game.setStartPath(path);
        game.restart();
        return game;
    }

    @Test
    @DisplayName("Muhafiz zirhla ve fazladan canla iniyor")
    void theWardenWearsArmour() {
        Game game = startedAs(StartPath.MUHAFIZ);

        assertNotNull(game.getPlayer().getEquippedArmor(), "Zirh kusanilmis olmali");
        assertNull(game.getPlayer().getEquippedWeapon(), "Muhafizin kilici yok");
        assertTrue(game.getPlayer().getMaxHp() > new Player(0, 0).getMaxHp(),
                "Fazladan can taban degerin uzerine cikmali");
    }

    @Test
    @DisplayName("Haydut aceleyle buyulu kilicla iniyor")
    void theCutpurseCarriesAHastedBlade() {
        Game game = startedAs(StartPath.HAYDUT);

        assertNotNull(game.getPlayer().getEquippedWeapon());
        assertNull(game.getPlayer().getEquippedArmor(), "Haydutun zirhi yok");
        assertTrue(game.getPlayer().getEquippedWeapon().hasEnchantment(Enchantment.ACELE),
                "Kilic aceleyle buyulu olmali");
    }

    /**
     * Tuccarin zorlugu burada: ilk katlari ciplak elle geciyor. Kesesi ise
     * dolu, yani ilk buyucude ya da saticida digerlerinin onune geciyor.
     */
    @Test
    @DisplayName("Tuccar takimsiz ama keseli iniyor")
    void theTraderStartsRich() {
        Game game = startedAs(StartPath.TUCCAR);

        assertNull(game.getPlayer().getEquippedWeapon());
        assertNull(game.getPlayer().getEquippedArmor());
        assertTrue(game.getGold() >= 100, "Kese dolu olmali, bulundu: " + game.getGold());
        assertFalse(game.getInventory().isEmpty(), "Yaninda bir iksir olmali");
    }

    /**
     * Baslangic sermayesi "bu kosuda topladigin altin" degil. Defter
     * donatmadan sonra aciliyor, yoksa olum ekrani daha ilk katta 150 altin
     * toplamis gibi gosterirdi.
     */
    @Test
    @DisplayName("Tuccarin kesesi toplanan altina yazilmiyor")
    void thestartingPurseIsNotGathered() {
        Game game = startedAs(StartPath.TUCCAR);

        assertEquals(0, game.getRunLog().getGoldFound(),
                "Sermaye toplanan altin degil");
        assertTrue(game.getGold() > 0, "Ama kesede duruyor");
    }

    /** Ucu de ayni seyi verseydi secim bir sayfa suslemesi olurdu. */
    @Test
    @DisplayName("Uc yol gercekten farkli basliyor")
    void thethreePathsDiffer() {
        Set<String> fingerprints = new HashSet<>();

        for (StartPath path : StartPath.values()) {
            Game game = startedAs(path);
            fingerprints.add(game.getPlayer().getEquippedWeapon() + "|"
                    + game.getPlayer().getEquippedArmor() + "|"
                    + game.getGold() + "|" + game.getPlayer().getMaxHp());
        }

        assertEquals(StartPath.values().length, fingerprints.size());
    }

    /**
     * Olum secimi sifirlamiyor: "bir daha deneyeyim" demenin en dogal aninda
     * oyuncuyu menuye geri yollamak olurdu.
     */
    @Test
    @DisplayName("Olunce ayni yolla yeniden basliyorsun")
    void deathKeepsThePath() {
        Game game = startedAs(StartPath.HAYDUT);
        game.getPlayer().takeDamage(game.getPlayer().getMaxHp());
        assertTrue(game.isOver());

        game.restart();

        assertEquals(StartPath.HAYDUT, game.getStartPath());
        assertNotNull(game.getPlayer().getEquippedWeapon(), "Kilic yine elinde olmali");
    }

    /** Yeniden baslamak onceki kosunun takimini tasimamali. */
    @Test
    @DisplayName("Yeniden baslayinca canta yalnizca yeni yolun verdigi")
    void restartingClearsTheOldKit() {
        Game game = startedAs(StartPath.MUHAFIZ);
        game.getInventory().add(new com.cryptdelver.entity.Bomb(0, 0));
        int carried = game.getInventory().totalItems();

        game.setStartPath(StartPath.HAYDUT);
        game.restart();

        assertTrue(game.getInventory().totalItems() < carried,
                "Eski kosunun esyalari kalmamali");
        assertNull(game.getPlayer().getEquippedArmor(), "Muhafizin zirhi da gitmeli");
    }

    /** Kusanilan parca cantada duruyor; oyundaki her ekipman oyle. */
    @Test
    @DisplayName("Baslangic parcasi cantada da duruyor")
    void theStartingPieceSitsInTheBag() {
        Game game = startedAs(StartPath.MUHAFIZ);

        Item armour = game.getPlayer().getEquippedArmor();
        assertTrue(game.getInventory().getItems().contains(armour),
                "Kusanilan parca cantadan cikmis olmamali");
    }

    @Test
    @DisplayName("Her yolun adi ve aciklamasi var")
    void everyPathIntroducesItself() {
        for (StartPath path : StartPath.values()) {
            assertFalse(path.getLabel().isBlank(), path + " adsiz");
            assertFalse(path.getDescription().isBlank(), path + " aciklamasiz");
        }
    }
}
