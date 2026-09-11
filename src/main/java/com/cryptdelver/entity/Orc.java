package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.ai.AStarPathfinder;

/**
 * Ork savaşçısı: ağır, zırhlı, sert vuran.
 *
 * <p>İskeletten daha dayanıklı ve daha sert; buna karşılık en yavaş sıradan
 * düşman. A* kullandığı için köşeye kaçmak işe yaramıyor, ama hızın sayesinde
 * kaçıp iksir içmeye vaktin oluyor.</p>
 *
 * <p>Savunması 2: erken kademe silahla vurmak gerçekten yavaş ilerliyor, yani
 * ork göründüğünde kılıcını yükseltmiş olman gerekiyor.</p>
 */
public class Orc extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            16,     // can
            6,      // vuruş gücü
            2,      // savunma: zırhlı
            2.2,    // hız (kare/saniye) — en yavaşı
            1.5,    // vuruş arası bekleme
            10);    // fark etme menzili

    public Orc(int tileX, int tileY) {
        super(tileX, tileY, Text.ENEMY_ORC, STATS, new AStarPathfinder());
    }

    @Override
    public String getKind() {
        return "ORC";
    }

    @Override
    public String getSpriteName() {
        return "orc";
    }
}
