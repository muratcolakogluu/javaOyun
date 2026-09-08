package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Her karede kendini güncelleyen varlıklar.
 *
 * <p>Oyun artık gerçek zamanlı: sıra diye bir şey yok, herkes geçen süreye
 * ({@code delta}) göre hareket eder. Bu sayede hızlar birbirinden bağımsız
 * olabiliyor — fare saniyede 4 kare giderken iskelet 2.4 kare gidiyor ve bu,
 * kare hızından (FPS) bağımsız çalışıyor.</p>
 *
 * <p>Eşyalar gibi durağan varlıklar bu arayüzü uygulamaz.</p>
 */
public interface Actor {

    /**
     * Varlığı bir kare ilerletir.
     *
     * @param game oyun durumu; hareket ve saldırı bunun üzerinden yapılır
     * @param delta son kareden bu yana geçen süre, saniye cinsinden
     */
    void update(Game game, double delta);
}
