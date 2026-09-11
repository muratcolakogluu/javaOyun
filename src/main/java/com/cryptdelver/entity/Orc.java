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
 *
 * <h2>Kalkan kol</h2>
 * <p>Vuruşundan önce yarım saniye hazırlanıyor ve o sırada duruyor. Eskiden
 * yanında durmak saf bir hesaptı: canın yetiyorsa vuruşurdun, yetmiyorsa
 * ölürdün, arada verilecek bir karar yoktu. İşaret o kararı geri veriyor —
 * bir adım geri, vuruş boşa, sonra geri gir. Bedeli de var: baltası artık
 * daha sert iniyor, yani zamanlamayı kaçırmak eskisinden pahalı.</p>
 */
public class Orc extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            16,     // can
            7,      // vuruş gücü — işaretli olduğu için biraz daha sert
            2,      // savunma: zırhlı
            2.2,    // hız (kare/saniye) — en yavaşı
            1.5,    // vuruş arası bekleme
            10,     // fark etme menzili
            0.6);   // hazırlık: ağır gövde, uzun işaret

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
