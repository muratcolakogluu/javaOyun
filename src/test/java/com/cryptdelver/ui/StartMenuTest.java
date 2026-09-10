package com.cryptdelver.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Baslangic menusu.
 *
 * <p>Menu yalnizca secimi tutuyor, secilen seyin ne yaptigini bilmiyor. Bu
 * ayrim sayesinde JavaFX penceresi acmadan sinanabiliyor.</p>
 */
class StartMenuTest {

    /** Kaydetme kalkinca liste dort satira indi: yeni oyun, ayarlar, yardim, cikis. */
    @Test
    @DisplayName("Menu dort satir ve yeni oyun basta secili")
    void theMenuOpensOnNewGame() {
        StartMenu menu = new StartMenu();

        assertEquals(4, menu.getOptions().size(), "Yeni oyun, ayarlar, yardim, cikis");
        assertEquals(StartMenu.Option.NEW_GAME, menu.getSelected(), "Yeni oyun basta secili");
    }

    @Test
    @DisplayName("Secim asagi ve yukari geziyor")
    void selectionMoves() {
        StartMenu menu = new StartMenu();

        menu.moveDown();
        assertEquals(StartMenu.Option.SETTINGS, menu.getSelected());

        menu.moveDown();
        assertEquals(StartMenu.Option.HELP, menu.getSelected());

        menu.moveUp();
        assertEquals(StartMenu.Option.SETTINGS, menu.getSelected());
    }

    /**
     * Ana sayfadaki secim, alt sayfada gezinirken kaybolmuyor: ayarlardan
     * donunce yine "Ayarlar" satirinda duruyorsun.
     */
    @Test
    @DisplayName("Alt sayfada gezinmek ana sayfanin secimini bozmuyor")
    void panesKeepTheirOwnSelection() {
        StartMenu menu = new StartMenu();
        menu.moveDown();
        StartMenu.Option before = menu.getSelected();

        menu.openPane(StartMenu.Pane.SETTINGS);
        menu.moveDown();
        menu.moveDown();
        menu.back();

        assertEquals(StartMenu.Pane.MAIN, menu.getPane());
        assertEquals(before, menu.getSelected());
    }

    @Test
    @DisplayName("Ayarlar sayfasinda satirlar arasinda geziliyor")
    void settingRowsAreNavigable() {
        StartMenu menu = new StartMenu();
        menu.openPane(StartMenu.Pane.SETTINGS);

        assertEquals(StartMenu.SettingRow.VOLUME, menu.getSelectedSetting());

        menu.moveDown();
        assertEquals(StartMenu.SettingRow.MUSIC, menu.getSelectedSetting());

        menu.moveUp();
        menu.moveUp();
        assertEquals(StartMenu.SettingRow.BACK, menu.getSelectedSetting(), "Basta yukari sona sarmali");
    }

    /**
     * ESC ana sayfada pencereyi kapatmiyor: yanlislikla basinca oyundan
     * atilmak, kazanilabilecek en ucuz sinir bozuklugu olurdu. Cikis listede.
     */
    @Test
    @DisplayName("Ana sayfada geri bir sey yapmiyor")
    void backOnTheMainPaneIsHarmless() {
        StartMenu menu = new StartMenu();

        menu.back();

        assertEquals(StartMenu.Pane.MAIN, menu.getPane());
        assertTrue(menu.isOpen(), "Menu acik kalmali");
    }

    /** Listenin ucunda takilip kalmak yerine basa sariyor. */
    @Test
    @DisplayName("Secim listenin ucundan basa sariyor")
    void selectionWrapsAround() {
        StartMenu menu = new StartMenu();

        menu.moveUp();
        assertEquals(StartMenu.Option.QUIT, menu.getSelected(), "Yukaridan sona sarmali");

        menu.moveDown();
        assertEquals(StartMenu.Option.NEW_GAME, menu.getSelected(), "Sondan basa sarmali");
    }

    @Test
    @DisplayName("Menu kapaninca bir daha acilmiyor")
    void closingIsFinal() {
        StartMenu menu = new StartMenu();
        assertTrue(menu.isOpen(), "Oyun menuyle basliyor");

        menu.close();

        assertFalse(menu.isOpen());
    }
}
