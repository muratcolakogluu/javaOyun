package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Şifa iksiri: kullanılınca can doldurur ve tükenir.
 */
public class Potion extends Item {

    private static final int HEAL_AMOUNT = 8;

    public Potion(int tileX, int tileY) {
        super(tileX, tileY, "İksir");
    }

    public int getHealAmount() {
        return HEAL_AMOUNT;
    }

    @Override
    public boolean use(Game game) {
        Player player = game.getPlayer();

        // Dolu canla iksir harcamayı engelle; yanlışlıkla basmak canını yakmasın.
        if (player.getHp() >= player.getMaxHp()) {
            game.getMessageLog().add("Canın zaten dolu.");
            return false;
        }

        int before = player.getHp();
        player.heal(HEAL_AMOUNT);
        game.getMessageLog().add("İksiri içtin: " + (player.getHp() - before) + " can geldi.");
        return true;
    }

    @Override
    public String getSaveKind() {
        return "POTION";
    }

    @Override
    public String getSpriteName() {
        return "potion";
    }
}
