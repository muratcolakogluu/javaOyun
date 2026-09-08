# CryptDelver

*Çalışma adı — istersen sonra değiştiririz.*

## Bir cümlede

Her oynanışta yeniden üretilen (prosedürel) bir zindanda ilerleyen, düşmanlarla
savaşan, eşya toplayan 2D rogue-like bir zindan kaşifi.

## Neden bu proje

- Nesne yönelimli tasarımı gerçek anlamda kullandırıyor (kalıtım, arayüzler,
  polimorfizm — Entity / Combatant / Player / Enemy / Item hiyerarşisi).
- Prosedürel zindan üretimi, veri yapıları ve algoritma bilgisini pratiğe döküyor.
- Düşman yapay zekası için A* pathfinding, portföyde anlatılabilir somut bir
  algoritma örneği oluyor.
- Kaydetme/yükleme (dosya I/O veya JSON serileştirme), dosya işlemleri
  konusunda deneyim katıyor.

## Teknoloji

- **Dil:** Java 21 hedefi (makinede kurulu JDK 25 ile derleniyor)
- **Arayüz / render:** JavaFX 21, tek bir `Canvas` üzerine çizim
- **Build:** Maven
- **IDE:** IntelliJ IDEA

## Oyun modeli

**Gerçek zamanlı, ızgaraya kilitli hareket.** Baştaki plan sıra tabanlıydı ama
oynanışta kötü hissettirdi: düşmanlar ancak sen hareket edince kıpırdıyordu.
Şimdi zaman kendi başına akıyor (`AnimationTimer`, delta zamanlı), ama herkes —
oyuncu da düşmanlar da — kare kare adım atıyor ve adım ortasında yön
değiştiremiyor. Mantık ızgarada tam sayı, çizim iki karenin arasında kesirli.

Sonuç: kare kare hareketin okunaklılığı + aksiyonun canlılığı bir arada.

## MVP özellikleri — tamamlandı

- [x] Tek katlı, prosedürel olarak üretilen bir zindan (BSP + random walk)
- [x] Oyuncu hareketi ve temel çarpışma (collision) sistemi
- [x] 2 düşman tipi (fare, iskelet), vuruş bekleme süreli savaş sistemi
- [x] Can, hasar, ölme/yeniden başlama
- [x] Envanter (iksir, altın, silah — 8 slot, 1-8 tuşlarıyla kullanım)

## Sonraki aşama (stretch goals)

- [x] A* pathfinding ile düşman yapay zekası (iskeletler A*, fareler açgözlü)
- [x] Çok katlı zindan (merdiven, derinlikle artan zorluk)
- [x] Boss savaşları (her 5. katta Kript Lordu, merdiveni tutuyor)
- [ ] **Zırh ve savunma sistemi** — sıradaki iş, aşağıda
- [ ] Kaydetme / yükleme sistemi
- [ ] Farklı silah / büyü türleri (silahlar var, büyü yok)
- [ ] Görüş alanı (FOV) — şu an tüm harita açık
- [ ] Ses

## Sıradaki iş: zırh ve savunma

Katlarda zırh parçaları bulunacak, savunma değeri arttıkça gelen hasar azalacak;
boss ve düşman zorluğu buna denk şekilde ölçeklenecek.

Zemini hazır: her vuruş `Game.resolveDamage(saldıran, savunan)` üzerinden
geçiyor ve savunmayı `Combatant.getDefense()` belirliyor. Şu an herkes için 0.
Yapılacaklar:

1. `Armor extends Item` — kuşanılan zırh, savunma bonusu verir
2. `Player.getDefense()` kuşanılan zırhtan gelsin
3. Düşmanlara tür bazlı savunma (`EnemyStats`'a alan eklenir)
4. Derinlikle ölçekleme: hem oyuncunun bulduğu zırh hem düşman savunması artsın

## Mimari (paket yapısı)

```
com.cryptdelver
├── entity        // Entity → Combatant → Player / Enemy (Rat, Skeleton, Boss)
│                 // Entity → Item → Potion / Gold / Weapon
├── world         // Dungeon, Tile, Room, Position, DungeonGenerator (BSP + RandomWalk)
├── ai            // Pathfinder → GreedyPathfinder / AStarPathfinder
├── game          // Game (kurallar), Inventory, MessageLog
├── ui            // GameScreen (döngü + girdi), GameRenderer, Sprite katmanı
├── persistence   // Kaydetme / yükleme — henüz boş
└── Main.java
```

`game` paketi ilk taslakta yoktu; oyun kuralları ne `world`'e ne `ui`'ya aitti,
ayrı bir yere alındı.

### Sprite'lar

Şu an figürler kodda şekillerle çiziliyor (`ShapeSprites`). `SpriteRegistry`
önce `resources/assets/sprites/<ad>.png` arıyor, bulamazsa şekle düşüyor —
yani hazır bir asset paketi bulunca dosyaları doğru adlarla klasöre atmak
yeterli, kod değişmeyecek.

Beklenen dosya adları: `player`, `rat`, `skeleton`, `boss`, `potion`, `gold`,
`sword`, `axe`.

## Kontroller

| Tuş | İş |
|---|---|
| WASD / oklar | hareket |
| Boşluk | vur (yan karelerdeki tüm düşmanlara) |
| 1-8 | çantadaki eşyayı kullan / kuşan |
| E | merdivende bir alt kata in |
| R | aynı üreticiyle yeni kat (deneme amaçlı) |
| G | zindan üreticisini değiştir (BSP ↔ mağara) |
| Enter | ölünce yeniden başla |

## Yol haritası — durum

1. [x] Proje iskeleti (Maven + JavaFX), boş pencere
2. [x] Tile-map ve oyuncu hareketi
3. [x] Prosedürel zindan üretimi
4. [x] Çarpışma ve savaş sistemi
5. [x] Düşman AI (önce açgözlü, sonra A*)
6. [x] Envanter ve eşyalar
7. [ ] Cilalama: ses, animasyon, menüler

## Test

`mvn test` → 82 test. Oyun mantığı JavaFX'ten tamamen bağımsız olduğu için
oyun döngüsü testlerde elle çevrilebiliyor (`game.update(1/60.0)`); pencere
açmadan hareket, savaş, kat inişi ve boss akışı test ediliyor.
