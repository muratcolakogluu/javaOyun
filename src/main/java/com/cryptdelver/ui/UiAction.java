package com.cryptdelver.ui;

import com.cryptdelver.entity.Enchantment;

/**
 * Fareyle tıklanabilen bir arayüz öğesinin ne yaptığı.
 *
 * <p>Çizim katmanı ekrana bir şey koyarken "buraya tıklanırsa şu olur" diye
 * bunu kaydediyor; {@link GameScreen} tıklamayı buna çevirip oyuna
 * iletiyor. Böylece <b>yerleşim tek yerde kalıyor</b>: bir satırın nerede
 * durduğunu yalnızca onu çizen kod biliyor, isabet hesabı için aynı
 * koordinatların ikinci bir kopyası tutulmuyor.</p>
 *
 * <p>Mühürlü arayüz ve kayıtlar: tıklamayı işleyen tarafta {@code switch}
 * bütün durumları kapsamak zorunda kalıyor, yeni bir öğe eklendiğinde derleyici
 * unutulan yeri gösteriyor.</p>
 */
public sealed interface UiAction {

    /** Başlangıç menüsünün ana listesinde bir satır. */
    record Menu(StartMenu.Option option) implements UiAction {
    }

    /**
     * Ayarlar sayfasında bir satır ve tıklamanın hangi yöne götürdüğü.
     *
     * <p>Yön başta yoktu: her tıklama değeri bir <em>ileri</em> alıyordu.
     * Açık/kapalı ve zorluk için sorun değil — sıradaki değere geçiyorsun — ama
     * ses seviyeleri sınırda duruyor, yani %100'e gelince fareyle geri
     * dönmenin hiçbir yolu kalmıyordu. Satırdaki {@code <} ve {@code >}
     * işaretleri artık kendi bölgelerini kaydediyor.</p>
     *
     * @param step {@code +1} ileri, {@code -1} geri
     */
    record Setting(StartMenu.SettingRow row, int step) implements UiAction {
    }

    /** Büyücü tezgâhında tamir ya da yükseltme. */
    record Forge(Bench bench) implements UiAction {
    }

    /** Büyücü tezgâhında bir büyü; hangi parçaya basılacağı da burada. */
    record Enchant(boolean onWeapon, Enchantment enchantment) implements UiAction {
    }

    /**
     * Satıcının tezgâhında bir sıra; tıklamak satın alıyor.
     *
     * <p>Sırayı numarayla tutuyorum, eşyayla değil: tezgâhtan bir parça
     * satılınca kalanlar yukarı kayıyor ve ekran bir sonraki karede yeni
     * bölgeleri kaydediyor. Nesneyi tutsaydım satılmış bir parçaya ait ölü bir
     * tıklama bölgesi kalabilirdi.</p>
     */
    record Buy(int index) implements UiAction {
    }

    /**
     * Başlangıç yolları sayfasında bir satır; tıklamak koşuyu başlatıyor.
     *
     * <p>Menü satırlarından ayrı bir kayıt, çünkü yaptığı iş farklı: menü
     * satırı sayfa değiştiriyor, bu satır oyunu <em>kuruyor</em>.</p>
     */
    record Path(com.cryptdelver.game.StartPath path) implements UiAction {
    }

    /** Çantadaki bir slot; tıklamak kullanıyor, Shift ile yere bırakıyor. */

    record Slot(int index) implements UiAction {
    }

    /** Tezgâhın dört sabit işi. */
    enum Bench {
        REPAIR_WEAPON,
        REPAIR_ARMOR,
        UPGRADE_WEAPON,
        UPGRADE_ARMOR
    }
}
