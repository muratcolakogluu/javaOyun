package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.game.Game;

/**
 * Şifa iksiri: kullanılınca can doldurur ve tükenir.
 */
public class Potion extends Item {

    private static final int HEAL_AMOUNT = 8;

    public Potion(int tileX, int tileY) {
        super(tileX, tileY, Text.ITEM_POTION);
    }

    public int getHealAmount() {
        return HEAL_AMOUNT;
    }

    @Override
    public String getDescription() {
        return Text.ITEM_POTION_INFO.get(HEAL_AMOUNT);
    }

    @Override
    public boolean use(Game game) {
        Player player = game.getPlayer();

        // Dolu canla iksir harcamayı engelle; yanlışlıkla basmak canını yakmasın.
        if (player.getHp() >= player.getMaxHp()) {
            game.getMessageLog().add(Text.MSG_POTION_WASTED.get());
            return false;
        }

        int before = player.getHp();
        player.heal(HEAL_AMOUNT);
        game.getMessageLog().add(Text.MSG_POTION_DRUNK.get(player.getHp() - before));
        return true;
    }

    /** İksirler aynı slotta yığılır; birkaç tane taşımak çantayı tıkamasın. */
    /** Tek slotta yigildigi icin cantani sikistirmaz: uzerine basmak yeter. */
    @Override
    public boolean isAutoPickedUp() {
        return true;
    }

    @Override
    public boolean isStackable() {
        return true;
    }

    @Override
    public String getKind() {
        return "POTION";
    }

    @Override
    public String getSpriteName() {
        return "potion";
    }
}
