package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Goblin;
import com.cryptdelver.entity.Orc;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Yeni yaratiklarin davranisi ve katlara dagilimi.
 */
class CreatureTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(20, 12);
        dungeon.fill(Tile.FLOOR);
        for (int i = 0; i < 20; i++) {
            dungeon.setTile(i, 0, Tile.WALL);
            dungeon.setTile(i, 11, Tile.WALL);
        }
        for (int i = 0; i < 12; i++) {
            dungeon.setTile(0, i, Tile.WALL);
            dungeon.setTile(19, i, Tile.WALL);
        }

        player = new Player(5, 5);
        game = new Game(dungeon, player);
    }

    private void simulateFrames(int frames) {
        for (int i = 0; i < frames; i++) {
            game.update(FRAME);
        }
    }

    @Test
    @DisplayName("Saglam goblin oyuncuya yaklasir")
    void healthyGoblinApproaches() {
        Goblin goblin = new Goblin(11, 5);
        game.addEnemy(goblin);
        int before = goblin.tileDistanceTo(player);

        simulateFrames(60);

        assertTrue(goblin.tileDistanceTo(player) < before, "Dolu canla kovalamali");
    }

    /**
     * Goblinin oyuna kattigi sey sayi degil karar: yaraliyken kaciyor, yani
     * bitirmek istiyorsan pesinden gitmen gerekiyor.
     */
    @Test
    @DisplayName("Yarali goblin kacar")
    void woundedGoblinFlees() {
        Goblin goblin = new Goblin(7, 5);
        game.addEnemy(goblin);
        goblin.takeDamage(goblin.getMaxHp() - 1);
        int before = goblin.tileDistanceTo(player);

        simulateFrames(60);

        assertTrue(goblin.tileDistanceTo(player) > before,
                "Cani azalinca uzaklasmali, olculen: " + goblin.tileDistanceTo(player));
    }

    @Test
    @DisplayName("Kacan goblin vurmaz")
    void fleeingGoblinDoesNotAttack() {
        Goblin goblin = new Goblin(6, 5);
        game.addEnemy(goblin);
        goblin.takeDamage(goblin.getMaxHp() - 1);

        simulateFrames(60);

        assertEquals(player.getMaxHp(), player.getHp(), "Kacarken saldirmamali");
    }

    @Test
    @DisplayName("Ork iskeletten sert ama daha yavas")
    void orcIsToughAndSlow() {
        Orc orc = new Orc(3, 3);
        Skeleton skeleton = new Skeleton(3, 3);

        assertTrue(orc.getMaxHp() > skeleton.getMaxHp(), "Ork daha dayanikli");
        assertTrue(orc.getAttackPower() > skeleton.getAttackPower(), "Ork daha sert vurur");
        assertTrue(orc.getDefense() > skeleton.getDefense(), "Ork daha zirhli");
        assertTrue(orc.getSpeed() < skeleton.getSpeed(), "Ork daha yavas");
    }

    /**
     * Katlar tur degistirerek zorlasiyor: ilk katta ork/goblin cikmiyor, derin
     * katlarda ise imp yerini sert turlere birakiyor.
     */
    @Test
    @DisplayName("Ilk katta yalnizca erken turler cikar")
    void firstFloorHasOnlyEarlyKinds() {
        Player fresh = new Player(0, 0);
        Game generated = new Game(List.of(new BspGenerator()), 40, 24, fresh);

        Set<String> kinds = generated.getEnemies().stream()
                .map(Enemy::getSaveKind)
                .collect(Collectors.toSet());

        assertFalse(kinds.contains("GOBLIN"), "Goblin 2. kattan once cikmamali");
        assertFalse(kinds.contains("ORC"), "Ork 4. kattan once cikmamali");
        assertTrue(kinds.contains("IMP") || kinds.contains("SKELETON"), "Erken turler olmali");
    }

    @Test
    @DisplayName("Derin katlarda sert turler devreye girer")
    void deeperFloorsBringTougherKinds() {
        Player deep = new Player(0, 0);
        Game generated = new Game(List.of(new BspGenerator()), 40, 24, deep);

        for (int i = 0; i < 6; i++) {
            deep.setTile(generated.getStairs());
            if (!generated.descend()) {
                while (generated.getBoss() != null) {
                    deep.setTile(generated.getBoss().getTileX() + 1, generated.getBoss().getTileY());
                    generated.playerAttacks();
                }
                deep.setTile(generated.getStairs());
                generated.descend();
            }
        }

        // 7. kattayiz: ork ve goblin havuzda, imp'in agirligi tabanda.
        assertTrue(generated.getDepth() >= 7);
        assertTrue(generated.getEnemies().size() > 8, "Derin kat daha kalabalik");
    }

    @Test
    @DisplayName("Inmek azami cani buyutur ama iyilestirmez")
    void descendingRaisesMaxHealthWithoutHealing() {
        Player diver = new Player(0, 0);
        Game generated = new Game(List.of(new BspGenerator()), 40, 24, diver);
        diver.takeDamage(5);

        int hpBefore = diver.getHp();
        int maxBefore = diver.getMaxHp();

        diver.setTile(generated.getStairs());
        generated.descend();

        assertEquals(hpBefore, diver.getHp(), "Mevcut can degismemeli");
        assertTrue(diver.getMaxHp() > maxBefore, "Tavan yukselmeli");
    }
}
