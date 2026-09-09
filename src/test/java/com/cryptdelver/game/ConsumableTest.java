package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Bomb;
import com.cryptdelver.entity.EscapePotion;
import com.cryptdelver.entity.FuryPotion;
import com.cryptdelver.entity.HastePotion;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Nadir tuketilen esyalar.
 *
 * <p>Dordu de farkli bir soruna cevap: bomba kalabaliga, ofke tek hedefe, hiz
 * sikismaya, kacis uzakliga. Testler her birinin gercekten o isi yaptigini
 * kovaliyor.</p>
 */
class ConsumableTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(21, 15);
        dungeon.fill(Tile.FLOOR);
        for (int x = 0; x < 21; x++) {
            dungeon.setTile(x, 0, Tile.WALL);
            dungeon.setTile(x, 14, Tile.WALL);
        }
        for (int y = 0; y < 15; y++) {
            dungeon.setTile(0, y, Tile.WALL);
            dungeon.setTile(20, y, Tile.WALL);
        }

        player = new Player(10, 7);
        game = new Game(dungeon, player);
    }

    // ------------------------------------------------------------------ bomba

    @Test
    @DisplayName("Bomba cevredeki herkesi ayni anda vuruyor")
    void theBombHitsEveryoneNearby() {
        Skeleton close = new Skeleton(11, 7);
        Skeleton alsoClose = new Skeleton(10, 9);
        game.addEnemy(close);
        game.addEnemy(alsoClose);

        new Bomb(0, 0).use(game);

        assertTrue(game.getEnemies().isEmpty(), "Iki iskelet de patlamayi kaldirmamali");
    }

    @Test
    @DisplayName("Menzil disindaki dusman patlamadan etkilenmiyor")
    void theBlastHasAnEdge() {
        Skeleton far = new Skeleton(10 + Bomb.BLAST_RADIUS + 1, 7);
        game.addEnemy(far);
        int before = far.getHp();

        new Bomb(0, 0).use(game);

        assertEquals(before, far.getHp(), "Menzilin disi guvenli");
    }

    /** Patlama bir vurus degil: kalin zirh de durduramiyor. */
    @Test
    @DisplayName("Patlama zirhtan etkilenmiyor")
    void theBlastIgnoresArmour() {
        Skeleton armoured = new Skeleton(11, 7);
        armoured.strengthen(100, 0, 50);
        game.addEnemy(armoured);
        int before = armoured.getHp();

        new Bomb(0, 0).use(game);

        assertEquals(before - Bomb.BLAST_DAMAGE, armoured.getHp(), "Tam hasar gecmeli");
    }

    @Test
    @DisplayName("Bomba oyuncuya zarar vermiyor")
    void theBombSparesThePlayer() {
        game.addEnemy(new Skeleton(11, 7));
        int before = player.getHp();

        new Bomb(0, 0).use(game);

        assertEquals(before, player.getHp());
    }

    @Test
    @DisplayName("Bomba kullanilinca tukeniyor")
    void theBombIsConsumed() {
        assertTrue(new Bomb(0, 0).use(game), "Kullanilan bomba cantadan dusmeli");
    }

    // ----------------------------------------------------------------- iksirler

    @Test
    @DisplayName("Hiz iksiri bir sure hizlandiriyor")
    void hasteSpeedsYouUp() {
        double normal = player.getSpeed();

        new HastePotion(0, 0).use(game);

        assertTrue(player.isHasted());
        assertTrue(player.getSpeed() > normal, "Hiz artmali");
    }

    @Test
    @DisplayName("Hiz iksirinin etkisi suresi dolunca bitiyor")
    void hasteWearsOff() {
        double normal = player.getSpeed();
        new HastePotion(0, 0).use(game);

        for (int i = 0; i < (int) ((HastePotion.DURATION + 1) * 60); i++) {
            game.update(FRAME);
        }

        assertFalse(player.isHasted());
        assertEquals(normal, player.getSpeed(), 1e-9, "Eski hiza donmeli");
    }

    /**
     * Ikinci iksir sureyi yeniliyor, uzerine eklemiyor: bes iksiri arka arkaya
     * icip yarim dakika ucmak yok.
     */
    @Test
    @DisplayName("Ikinci iksir sureyi yeniliyor, biriktirmiyor")
    void drinkingTwiceRefreshesTheTimer() {
        new HastePotion(0, 0).use(game);
        for (int i = 0; i < 60 * 3; i++) {
            game.update(FRAME);
        }

        new HastePotion(0, 0).use(game);

        assertEquals(HastePotion.DURATION, player.getHasteRemaining(), 0.05,
                "Sure bastan kurulmali, ustune eklenmemeli");
    }

    @Test
    @DisplayName("Ofke iksiri vurus gucunu artiriyor")
    void furyRaisesTheAttack() {
        int normal = player.getAttackPower();

        new FuryPotion(0, 0).use(game);

        assertTrue(player.isFurious());
        assertEquals(normal + FuryPotion.ATTACK_BONUS, player.getAttackPower());
    }

    @Test
    @DisplayName("Ofke iksirinin etkisi suresi dolunca bitiyor")
    void furyWearsOff() {
        int normal = player.getAttackPower();
        new FuryPotion(0, 0).use(game);

        for (int i = 0; i < (int) ((FuryPotion.DURATION + 1) * 60); i++) {
            game.update(FRAME);
        }

        assertEquals(normal, player.getAttackPower());
    }

    /** Olup yeniden baslayinca gecici etkiler de gidiyor. */
    @Test
    @DisplayName("Yeniden baslayinca gecici etkiler siliniyor")
    void restartClearsTheEffects() {
        new HastePotion(0, 0).use(game);
        new FuryPotion(0, 0).use(game);

        player.restore();

        assertFalse(player.isHasted());
        assertFalse(player.isFurious());
    }

    // ------------------------------------------------------------------ kacis

    @Test
    @DisplayName("Kacis iksiri merdivenin basina isinliyor")
    void escapeTakesYouToTheStairs() {
        Player diver = new Player(0, 0);
        Game generated = new Game(List.of(new BspGenerator()), 40, 22, diver);
        assertFalse(generated.isPlayerOnStairs(), "Basta merdivende degiliz");

        assertTrue(new EscapePotion(0, 0).use(generated));

        assertTrue(generated.isPlayerOnStairs(), "Merdivenin basinda olmali");
    }

    /** Merdiven yoksa iksir bosa gitmiyor: cantada kaliyor. */
    @Test
    @DisplayName("Merdiven yoksa kacis iksiri harcanmiyor")
    void escapeIsNotWastedWithoutStairs() {
        assertFalse(new EscapePotion(0, 0).use(game),
                "Sabit haritada merdiven yok; esya tukenmemeli");
    }

    // ------------------------------------------------------------------ dagilim

    /**
     * Nadir esyalar seyrek ama gorunur olmali: cok sayida kat uretip "hem
     * cikiyor hem her katta cikmiyor" ikilisini birlikte ariyoruz.
     */
    @Test
    @DisplayName("Nadir esyalar arada bir yerde duruyor")
    void rareItemsShowUpNowAndThen() {
        Player diver = new Player(0, 0);
        Game generated = new Game(List.of(new BspGenerator()), 40, 22, diver);

        int floors = 200;
        int withRare = 0;
        for (int i = 0; i < floors; i++) {
            generated.regenerateFloor();
            boolean found = generated.getGroundItems().stream()
                    .anyMatch(item -> item instanceof Bomb || item instanceof HastePotion
                            || item instanceof FuryPotion || item instanceof EscapePotion);
            if (found) {
                withRare++;
            }
        }

        assertTrue(withRare > 0, "Hic cikmiyorsa zar isletmiyor demektir");
        assertTrue(withRare < floors / 2,
                "Her katta cikiyorsa nadir olma amaci kalmaz: " + withRare + "/" + floors);
    }
}
