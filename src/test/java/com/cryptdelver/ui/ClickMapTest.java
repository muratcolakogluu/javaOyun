package com.cryptdelver.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tiklanabilir bolgeler.
 *
 * <p>Cizim katmani bir satiri cizerken ayni anda buraya kaydediyor, yani
 * yerlesim koordinatlari tek yerde kaliyor. Bu testler kaydin ve isabet
 * hesabinin dogru calistigini JavaFX penceresi acmadan sinaviyor.</p>
 */
class ClickMapTest {

    private ClickMap clicks;

    @BeforeEach
    void setUp() {
        clicks = new ClickMap();
    }

    @Test
    @DisplayName("Bolgenin icindeki nokta o eylemi veriyor")
    void hitFindsTheRegion() {
        clicks.add(new UiAction.Slot(3), 10, 20, 30, 30);

        assertEquals(new UiAction.Slot(3), clicks.hit(15, 25));
        assertNull(clicks.hit(5, 25), "Solunda bir sey yok");
        assertNull(clicks.hit(15, 60), "Altinda bir sey yok");
    }

    /** Sag ve alt kenar disarida: yan yana duran iki slot birbirine karismasin. */
    @Test
    @DisplayName("Bolgenin sag ve alt kenari disarida")
    void theFarEdgesAreExclusive() {
        clicks.add(new UiAction.Slot(0), 0, 0, 10, 10);

        assertEquals(new UiAction.Slot(0), clicks.hit(0, 0), "Sol ust kose iceride");
        assertNull(clicks.hit(10, 5), "Sag kenar disarida");
        assertNull(clicks.hit(5, 10), "Alt kenar disarida");
    }

    /**
     * Ust uste binen bolgelerde sonra cizilen kazaniyor: ekranda ustte duran
     * neyse tiklama ona gidiyor.
     */
    @Test
    @DisplayName("Ust uste binen bolgelerde sonra eklenen kazaniyor")
    void laterRegionsWin() {
        clicks.add(new UiAction.Slot(0), 0, 0, 100, 100);
        clicks.add(new UiAction.Menu(StartMenu.Option.QUIT), 20, 20, 20, 20);

        assertEquals(new UiAction.Menu(StartMenu.Option.QUIT), clicks.hit(25, 25));
        assertEquals(new UiAction.Slot(0), clicks.hit(80, 80), "Ustunde bir sey olmayan yer");
    }

    /**
     * Ayar satirinin yon isaretleri tam da bu kurala dayaniyor: satirin
     * tamami "bir ileri" olarak kayitli, ustune iki kucuk ok bolgesi biniyor.
     * Ok geri aliyor cunku sonra kaydediliyor.
     *
     * <p>Bu olmadan fareyle ses seviyesi yalnizca artiyordu: %100'e gelince
     * geri donmenin hicbir yolu kalmiyordu.</p>
     */
    @Test
    @DisplayName("Ayar satirindaki geri oku satirin kendisini yeniyor")
    void theBackArrowBeatsTheRowBeneathIt() {
        clicks.add(new UiAction.Setting(StartMenu.SettingRow.VOLUME, 1), 0, 0, 400, 30);
        clicks.add(new UiAction.Setting(StartMenu.SettingRow.VOLUME, -1), 300, 0, 30, 30);

        assertEquals(new UiAction.Setting(StartMenu.SettingRow.VOLUME, -1), clicks.hit(310, 15),
                "Okun ustu geri almali");
        assertEquals(new UiAction.Setting(StartMenu.SettingRow.VOLUME, 1), clicks.hit(100, 15),
                "Satirin govdesi ileri almali");
    }

    @Test
    @DisplayName("Yeni kare oncekinin bolgelerini siliyor")
    void clearForgetsEverything() {
        clicks.add(new UiAction.Slot(1), 0, 0, 50, 50);

        clicks.clear();

        assertNull(clicks.hit(10, 10), "Ekranda olmayan sey tiklanamaz");
    }

    /**
     * Kayit ve ustune gelme tek cagriyla yapiliyor: cizen kod donen degere
     * bakip satiri vurgulu ciziyor.
     */
    @Test
    @DisplayName("Ekleme, farenin ustunde olup olmadigini da soyluyor")
    void addReportsHover() {
        clicks.setMouse(15, 15);

        assertTrue(clicks.add(new UiAction.Slot(0), 10, 10, 20, 20), "Fare bu bolgenin ustunde");
        assertFalse(clicks.add(new UiAction.Slot(1), 40, 10, 20, 20), "Bu bolgenin ustunde degil");
    }

    @Test
    @DisplayName("Fare disari cikinca hicbir seyin ustunde degil")
    void leavingTheCanvasClearsHover() {
        clicks.setMouse(15, 15);
        clicks.add(new UiAction.Slot(0), 10, 10, 20, 20);
        assertEquals(new UiAction.Slot(0), clicks.hovered());

        clicks.clearMouse();

        assertNull(clicks.hovered());
    }

    @Test
    @DisplayName("Fare hic kipirdamadiysa da bir sey secili degil")
    void withoutAMouseNothingIsHovered() {
        clicks.add(new UiAction.Slot(0), 0, 0, 100, 100);

        assertNull(clicks.hovered(), "Baslangicta imlec tuvalin disinda sayiliyor");
    }
}
