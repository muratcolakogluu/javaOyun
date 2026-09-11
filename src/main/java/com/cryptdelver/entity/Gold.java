package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.game.Game;

/**
 * Altın yığını: çantaya girmez, toplanır toplanmaz keseye yazılır.
 *
 * <p>Bu yüzden çanta dolu olsa bile altın alınabiliyor — kuralı {@code Game}
 * değil, eşyanın kendisi belirliyor.</p>
 */
public class Gold extends Item {

    private final int amount;

    public Gold(int tileX, int tileY, int amount) {
        super(tileX, tileY, Text.ITEM_GOLD);
        if (amount <= 0) {
            throw new IllegalArgumentException("Altın miktarı pozitif olmalı: " + amount);
        }
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public boolean goesToInventory() {
        return false;
    }

    /** Keseye gidiyor, çantaya değil; yani alması hiçbir şeye mal olmuyor. */
    @Override
    public boolean isAutoPickedUp() {
        return true;
    }

    @Override
    public void onPickup(Game game) {
        game.addGold(amount);
        game.getMessageLog().item(Text.MSG_GOLD_TAKEN.get(amount));
    }

    @Override
    public boolean use(Game game) {
        return false;
    }

    @Override
    public String getKind() {
        return "GOLD";
    }

    @Override
    public String getSpriteName() {
        return "gold";
    }
}
