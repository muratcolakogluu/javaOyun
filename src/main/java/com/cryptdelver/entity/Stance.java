package com.cryptdelver.entity;

/**
 * Bir düşmanın oyuncu karşısındaki duruşu: yaklaşır mı, durur mu, kaçar mı.
 *
 * <p>Önceden yalnızca "kaçıyor mu" diye tek bir soru vardı ve cevabı iki
 * seçenekliydi: ya üstüne gel ya kaç. Bu, oyundaki <em>bütün</em> düşmanların
 * aynı dövüşü vermesinin sebebiydi — hepsi sana doğru yürüyordu, farkları
 * yalnızca ne kadar hızlı yürüdükleriydi.</p>
 *
 * <p>Üçüncü seçenek, {@link #HOLD}, mesafeyi bir karar hâline getiriyor:
 * duran bir düşman senin ona gitmeni bekliyor. Okçunun bütün tehdidi bu —
 * yaklaşmak zorundasın ve yaklaşırken vuruluyorsun.</p>
 */
public enum Stance {

    /** Üstüne yürür; oyundaki çoğu düşmanın yaptığı. */
    CHASE,

    /** Olduğu yerde durur. Menzilli düşmanlar mesafeyi böyle koruyor. */
    HOLD,

    /** Uzaklaşan bir kare arar; ne vurur ne kovalar. */
    FLEE
}
