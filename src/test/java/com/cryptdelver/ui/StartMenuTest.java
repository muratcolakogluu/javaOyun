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

    @Test
    @DisplayName("Kayit yoksa devam satiri hic gorunmuyor")
    void continueIsHiddenWithoutASave() {
        StartMenu menu = new StartMenu(false);

        assertFalse(menu.getOptions().contains(StartMenu.Option.CONTINUE),
                "Secilemeyen soluk satir gostermektense hic gostermemek daha net");
        assertEquals(StartMenu.Option.NEW_GAME, menu.getSelected(), "Yeni oyun basta secili");
    }

    @Test
    @DisplayName("Kayit varsa devam satiri geliyor")
    void continueAppearsWithASave() {
        StartMenu menu = new StartMenu(true);

        assertTrue(menu.getOptions().contains(StartMenu.Option.CONTINUE));
        assertEquals(3, menu.getOptions().size());
    }

    @Test
    @DisplayName("Secim asagi ve yukari geziyor")
    void selectionMoves() {
        StartMenu menu = new StartMenu(true);

        menu.moveDown();
        assertEquals(StartMenu.Option.CONTINUE, menu.getSelected());

        menu.moveDown();
        assertEquals(StartMenu.Option.QUIT, menu.getSelected());

        menu.moveUp();
        assertEquals(StartMenu.Option.CONTINUE, menu.getSelected());
    }

    /** Listenin ucunda takilip kalmak yerine basa sariyor. */
    @Test
    @DisplayName("Secim listenin ucundan basa sariyor")
    void selectionWrapsAround() {
        StartMenu menu = new StartMenu(false);

        menu.moveUp();
        assertEquals(StartMenu.Option.QUIT, menu.getSelected(), "Yukaridan sona sarmali");

        menu.moveDown();
        assertEquals(StartMenu.Option.NEW_GAME, menu.getSelected(), "Sondan basa sarmali");
    }

    @Test
    @DisplayName("Menu kapaninca bir daha acilmiyor")
    void closingIsFinal() {
        StartMenu menu = new StartMenu(false);
        assertTrue(menu.isOpen(), "Oyun menuyle basliyor");

        menu.close();

        assertFalse(menu.isOpen());
    }
}
