package com.cryptdelver.game;

/**
 * Koşuların ardında kalan iz: en iyi derece ve toplam sayaçlar.
 *
 * <p>Ölünce her şey siliniyordu ve geriye hiçbir kayıt kalmıyordu. Kaybedilen
 * bir koşunun da bir anlamı olmalı: "15'i geçebildim" diye bir hedef ancak
 * önceki denemeyi hatırlarsan doğuyor.</p>
 *
 * <p>Ayarlardan ayrı bir sınıf, çünkü yaşam döngüsü farklı: ayarlar oyuncunun
 * <em>tercihi</em>, bunlar oyuncunun <em>geçmişi</em>. İkisini aynı yerde
 * tutmak "ayarları sıfırla" gibi bir işlemin rekorları da silmesine yol
 * açardı.</p>
 *
 * <p>Yalnızca en iyiler saklanıyor, koşuların tamamı değil: bir listeyi
 * gezmek yerine tek bir satır göstermek menüde daha okunaklı ve dosya
 * büyümüyor.</p>
 */
public class Records {

    private int deepestFloor;
    private int mostGold;
    private int runs;
    private int wins;

    /** Ulaşılan en derin kat. */
    public int getDeepestFloor() {
        return deepestFloor;
    }

    /** Tek bir koşuda toplanan en çok altın. */
    public int getMostGold() {
        return mostGold;
    }

    /** Kaç koşu bitti: ölerek ya da kazanarak. */
    public int getRuns() {
        return runs;
    }

    /** Kaç kez kriptten çıkıldı. */
    public int getWins() {
        return wins;
    }

    public boolean hasAnyRun() {
        return runs > 0;
    }

    /**
     * Biten bir koşuyu işler.
     *
     * <p>Ölmek de kazanmak da bir koşu: ikisi de sayılıyor, böylece "kaç
     * denemede bir bitirebiliyorum" sorusu yanıtlanabiliyor.</p>
     *
     * @param won kriptten çıkıldı mı
     */
    public void recordRun(int depth, int gold, boolean won) {
        runs++;
        if (won) {
            wins++;
        }

        deepestFloor = Math.max(deepestFloor, depth);
        mostGold = Math.max(mostGold, gold);
    }

    /** Kayıttan okunurken kullanılıyor; oyun içi akışta çağrılmaz. */
    public void restore(int deepestFloor, int mostGold, int runs, int wins) {
        this.deepestFloor = Math.max(0, deepestFloor);
        this.mostGold = Math.max(0, mostGold);
        this.runs = Math.max(0, runs);
        this.wins = Math.max(0, wins);
    }
}
