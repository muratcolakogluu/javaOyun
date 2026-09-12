package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Archer;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Orc;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Projectile;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Savusturma: oyuncunun ucuncu fiili.
 *
 * <p>Kizil halkanin tek bir cevabi vardi: geri cekil. Savusturma ayni isareti
 * <b>uc cevabi olan</b> bir soruya ceviriyor -- kac, arkasina dolan, ya da
 * karsila. Buradaki sinavlar pencerenin gercekten bir <em>zamanlama</em>
 * penceresi oldugunu kovaliyor: aciksa hasar yok, kapaliysa vurus iniyor, ve
 * bir pencere yalnizca bir vurusa yetiyor.</p>
 */
class ParryTest {

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

    private void simulate(double seconds) {
        for (int i = 0; i < seconds / FRAME; i++) {
            game.update(FRAME);
        }
    }

    /**
     * Pencereyi acar.
     *
     * <p>Tus yalnizca istek birakiyor; pencereyi acan sey oyun donugusu. Bu
     * yuzden testlerdeki dusmanlar <em>uzakta</em> duruyor: yani basinda
     * dusman olan bir oyuncu pencereyi acar acmaz ayni karede vurus yiyor ve
     * pencere hemen tukeniyor. Vurusu biz elle cagiriyoruz ki "pencere acik
     * miydi" sorusu tek basina olculebilsin.
     */
    private void parry() {
        player.requestParry();
        game.update(FRAME);
        assertTrue(player.isParrying(), "Pencere acilmaliydi");
    }

    @Test
    @DisplayName("Pencere acikken gelen vurus hasar vermiyor")
    void anOpenWindowStopsTheBlow() {
        Skeleton bones = new Skeleton(16, 5);
        game.addEnemy(bones);
        int before = player.getHp();

        parry();
        game.enemyAttacksPlayer(bones);

        assertEquals(before, player.getHp(), "Karsilanan vurus hasar vermemeli");
    }

    @Test
    @DisplayName("Pencere kapaliyken vurus iniyor")
    void aclosedWindowLetsItThrough() {
        Skeleton bones = new Skeleton(11, 5);
        game.addEnemy(bones);
        int before = player.getHp();

        game.enemyAttacksPlayer(bones);

        assertTrue(player.getHp() < before, "Savusturmayan oyuncu hasar almali");
    }

    /**
     * Savusturmak yalnizca vurusu engelleseydi bir "hasar almama" tusu
     * olurdu. Sersemlik o ani bir saldiri firsatina ceviriyor.
     */
    @Test
    @DisplayName("Karsilanan dusman sersemliyor")
    void theParriedEnemyIsStaggered() {
        Skeleton bones = new Skeleton(16, 5);
        game.addEnemy(bones);

        parry();
        game.enemyAttacksPlayer(bones);

        assertTrue(bones.isStaggered());
    }

    @Test
    @DisplayName("Sersemleyen dusman ne yuruyor ne vuruyor")
    void thestaggeredCannotAct() {
        Skeleton bones = new Skeleton(15, 5);
        game.addEnemy(bones);
        bones.stagger(1.0);

        int x = bones.getTileX();
        int before = player.getHp();
        simulate(0.8);

        assertEquals(x, bones.getTileX(), "Sersemken yurumemeli");
        assertEquals(before, player.getHp(), "Sersemken vurmamali");
        assertTrue(bones.isStaggered());
    }

    @Test
    @DisplayName("Sersemlik geciyor")
    void thestaggerWearsOff() {
        Skeleton bones = new Skeleton(15, 5);
        game.addEnemy(bones);
        bones.stagger(0.5);

        simulate(0.8);

        assertFalse(bones.isStaggered());
    }

    /**
     * Yoksa kalabaligin ortasinda tek bir tus butun kalabaligi durdururdu.
     */
    @Test
    @DisplayName("Bir pencere yalnizca bir vurusa yetiyor")
    void onewindowStopsOneBlow() {
        Skeleton first = new Skeleton(16, 5);
        Skeleton second = new Skeleton(4, 5);
        game.addEnemy(first);
        game.addEnemy(second);

        parry();
        game.enemyAttacksPlayer(first);
        int after = player.getHp();

        game.enemyAttacksPlayer(second);

        assertTrue(player.getHp() < after, "Ikinci vurus gecmeli");
        assertFalse(second.isStaggered(), "Ikinci dusman sersemlememeli");
    }

    @Test
    @DisplayName("Pencere kisa surede kapaniyor")
    void thewindowClosesQuickly() {
        parry();

        simulate(0.4);

        assertFalse(player.isParrying(), "Ceyrek saniyeden fazla acik kalmamali");
    }

    /** Ok da karsilanabiliyor ama sekiz kare oteden atani sersemletemezsin. */
    @Test
    @DisplayName("Ok savusturuluyor, atan sersemlemiyor")
    void arrowsAreDeflectedWithoutStagger() {
        Archer archer = new Archer(2, 5);
        game.addEnemy(archer);
        int before = player.getHp();

        parry();
        game.projectileHitsPlayer(new Projectile(9, 5, 1, 0, archer, 8));

        assertEquals(before, player.getHp(), "Ok hasar vermemeli");
        assertFalse(archer.isStaggered(), "Uzaktaki atan sersemlemez");
    }

    /**
     * Bekleme kisa ama var: bir karede iki kez basmak iki pencere acmamali.
     */
    @Test
    @DisplayName("Pencereler ust uste binmiyor")
    void windowsDoNotStack() {
        parry();
        simulate(0.4);
        assertFalse(player.isParrying());

        player.requestParry();
        game.update(FRAME);

        assertFalse(player.isParrying(), "Bekleme dolmadan ikinci pencere yok");
    }

    @Test
    @DisplayName("Bekleme dolunca yeniden savusturulabiliyor")
    void theparryComesBack() {
        parry();
        simulate(1.2);

        player.requestParry();
        game.update(FRAME);

        assertTrue(player.isParrying());
    }

    /**
     * Kolunu kaldirmis orkta en temiz hali: hazirlik basladiktan sonra
     * donmuyor, yani vurusun ne zaman inecegini biliyorsun.
     */
    @Test
    @DisplayName("Hazirlanan orkun vurusu karsilanabiliyor")
    void theWindupCanBeMet() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);
        int before = player.getHp();

        for (int i = 0; i < 60 && !orc.isWindingUp(); i++) {
            game.update(FRAME);
        }
        assertTrue(orc.isWindingUp());

        // Kol inmek uzereyken pencereyi aciyoruz.
        simulate(0.45);
        player.requestParry();
        simulate(0.3);

        assertEquals(before, player.getHp(), "Karsilanan balta hasar vermemeli");
        assertTrue(orc.isStaggered(), "Ork sersemlemis olmali");
    }

    @Test
    @DisplayName("Olu oyuncu savusturmuyor")
    void thedeadDoNotParry() {
        player.takeDamage(player.getMaxHp());

        player.requestParry();
        game.update(FRAME);

        assertFalse(player.isParrying());
    }

    @Test
    @DisplayName("Yeniden baslayinca pencere hazir")
    void restartResetsTheParry() {
        parry();
        game.getPlayer().restore();

        assertFalse(player.isParrying(), "Yeni kosuda acik pencere kalmamali");

        player.requestParry();
        game.update(FRAME);
        assertTrue(player.isParrying(), "Bekleme de sifirlanmali");
    }

    /** Sersemlik baslamis hazirligi da iptal ediyor: ayni kol iki kez inmiyor. */
    @Test
    @DisplayName("Sersemlik hazirligi iptal ediyor")
    void thestaggerCancelsTheWindup() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);

        for (int i = 0; i < 60 && !orc.isWindingUp(); i++) {
            game.update(FRAME);
        }
        assertTrue(orc.isWindingUp());

        orc.stagger(1.0);

        assertFalse(orc.isWindingUp());
    }

    @Test
    @DisplayName("Imp de sersemletilebiliyor")
    void anyEnemyCanBeStaggered() {
        Imp imp = new Imp(16, 5);
        game.addEnemy(imp);

        parry();
        game.enemyAttacksPlayer(imp);

        assertTrue(imp.isStaggered(), "Isareti olmayan dusman da karsilanabilir");
    }
}
