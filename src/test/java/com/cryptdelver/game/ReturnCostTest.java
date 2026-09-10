package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.RandomWalkGenerator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Geri donusun bedeli.
 *
 * <p>Merdivenden yukari cikabilmek altina bir anlam kazandirdi ama oyunu
 * kolaylastirdi: temizlenmis katlar bos, geri donus yolu tehlikesiz, biriken
 * altin da sabit fiyatli buyulere dogrudan cevriliyordu. Sonucta tam takim
 * buyulu bir oyuncu sig katlarda hicbir tehlikeyle karsilasmiyordu.</p>
 *
 * <p>Iki fren kondu ve ikisi de burada sinaniyor: buyuler ustundekilerle
 * birlikte pahalaniyor, zindan da her donusu hatirlayip daha cabuk
 * uyaniyor.</p>
 */
class ReturnCostTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 24;

    private Player player;
    private Game game;

    private static List<DungeonGenerator> generators() {
        return List.of(new BspGenerator(), new RandomWalkGenerator());
    }

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        game = new Game(generators(), WIDTH, HEIGHT, player);
    }

    private void goDown() {
        player.setTile(game.getStairs());
        assertTrue(game.descend(), "Inis calismali");
    }

    // ------------------------------------------------------------ fiyatlar

    @Test
    @DisplayName("Ilk buyu taban fiyattan")
    void theFirstEnchantmentCostsItsBasePrice() {
        assertEquals(Enchantment.DIKEN.getCost(), game.enchantPrice(Enchantment.DIKEN));
    }

    /** Ustundeki her buyu bir sonrakine pay bindiriyor; kese dogrudan guce cevrilmiyor. */
    @Test
    @DisplayName("Ustundeki buyu sonrakini pahalandiriyor")
    void carriedEnchantmentsRaiseTheNextPrice() {
        Weapon sword = new Weapon(0, 0, "Test Kilici", 5, "sword", 40);
        sword.enchant(Enchantment.VAMPIRLIK);
        game.getInventory().add(sword);
        player.equip(sword);

        assertTrue(game.enchantPrice(Enchantment.DIKEN) > Enchantment.DIKEN.getCost(),
                "Bir buyu tasirken ikincisi daha pahali");
    }

    /** Silah ve zirh birlikte sayiliyor: pahali olan tam takim kusanmak. */
    @Test
    @DisplayName("Silah ve zirhtaki buyuler birlikte sayiliyor")
    void bothPiecesCountTowardsThePrice() {
        Weapon sword = new Weapon(0, 0, "Test Kilici", 5, "sword", 40);
        sword.enchant(Enchantment.VAMPIRLIK);
        game.getInventory().add(sword);
        player.equip(sword);
        int withOne = game.enchantPrice(Enchantment.DIKEN);

        Armor mail = new Armor(0, 0, "Test Zirhi", 2, "armor_chain", 40);
        mail.enchant(Enchantment.YENILENME);
        game.getInventory().add(mail);
        player.equip(mail);

        assertTrue(game.enchantPrice(Enchantment.DIKEN) > withOne,
                "Zirhtaki buyu de fiyata giriyor");
    }

    @Test
    @DisplayName("Buyusuz oyuncu icin fiyat degismiyor")
    void anUnenchantedPlayerPaysTheListPrice() {
        Weapon sword = new Weapon(0, 0, "Test Kilici", 5, "sword", 40);
        game.getInventory().add(sword);
        player.equip(sword);

        assertEquals(Enchantment.ACELE.getCost(), game.enchantPrice(Enchantment.ACELE));
    }

    // -------------------------------------------------------- zindanin sabri

    @Test
    @DisplayName("Hic donmeyen oyuncu tam sabri goruyor")
    void patienceStartsFull() {
        assertEquals(0, game.getReturns());
        assertEquals(90.0, game.getFloorPatience(), 0.001);
    }

    @Test
    @DisplayName("Her geri donus zindanin sabrini kisaltiyor")
    void everyReturnShortensThePatience() {
        double before = game.getFloorPatience();

        goDown();
        assertTrue(game.ascend());

        assertEquals(1, game.getReturns());
        assertTrue(game.getFloorPatience() < before, "Sabir kisalmali");
    }

    /** Ceza birikip kati oynanamaz hale getirmiyor. */
    @Test
    @DisplayName("Sabir bir tabanin altina inmiyor")
    void patienceNeverFallsBelowTheFloor() {
        for (int i = 0; i < 12; i++) {
            goDown();
            game.ascend();
        }

        assertTrue(game.getFloorPatience() >= 30.0, "Taban sabir korunmali");
    }

    @Test
    @DisplayName("Yeniden baslayinca sayac sifirlaniyor")
    void restartForgetsTheReturns() {
        goDown();
        game.ascend();

        game.restart();

        assertEquals(0, game.getReturns());
        assertEquals(90.0, game.getFloorPatience(), 0.001);
    }
}
