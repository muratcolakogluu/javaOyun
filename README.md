# CryptDelver

Yirmi kat aşağı in, Kript Lordu'nu geç, geri dön.

Java 21 ve JavaFX ile yazılmış, gerçek zamanlı prosedürel bir zindan kaşifi
(roguelike). Her koşu yeni bir zindan üretiyor; ölünce baştan başlıyorsun.

Java sınıflarını kullanarak kat kat ilerlenen bir oyun tasarlama denemesi
olarak başladı; aşağıdaki "Ne amaçladık" bölümü nereye vardığını anlatıyor.

```bash
mvn javafx:run
```

---

## Ne amaçladık

Oyunun tamamı tek bir soruya cevap arıyor: **her an bir kararın olsun.**

Bu kulağa geldiğinden daha zor. Bir zindan kaşifi yazmak kolay — harita üret,
düşman doğur, can çubuğu koy, bitti. Ama o oyunda oyuncu hiçbir şey *seçmiyor*:
yürüyor, vuruyor, iniyor. Zorluk yalnızca sayıların büyümesinden geliyor ve
onuncu kat, birinci katın daha büyük sayılı hâli oluyor.

Projedeki her özellik, "burada oyuncu ne karar veriyor?" sorusunun bir cevabı.
Karar üretmeyen fikirler — ne kadar havalı olsalar da — eklenmedi ya da
çıkarıldı.

### Kararların geldiği yerler

**Yer.** Izgaraya kilitli ama gerçek zamanlı hareket: adım başladıktan sonra
yön değişmiyor, yani nereye basacağını önceden düşünmen gerekiyor. Kaçış adımı,
arkadan vuruş ve okçunun nişan hattı hep aynı şeyi soruyor — *şu an nerede
durmalıyım?*

**Zaman.** Düşmanların ağır vuranları vurmadan önce hazırlanıyor ve o sırada
duruyor; ekranda gövdeye doğru kapanan kızıl bir halka görüyorsun. Halkanın üç
cevabı var: geri çekil, arkasına dolan, ya da tam anında savuştur. Zindanın
sabrı da bir zamanlayıcı: fazla oyalanırsan takviye geliyor.

**Kaynak.** Altın tek bir yere harcanmıyor — büyücü takımını büyütüyor, gezgin
satıcı yolda hayatta kalmanı satıyor, kader taşı ise bedeli **altın değil,
azami canın** olan bir takas öneriyor. İkinci bir para birimi, koşuları
birbirinden ayıran şey.

**Yön.** Merdivenlerin yarısında aşağıda iki yol sunuluyor ve ne bulacağını
inmeden önce biliyorsun: sakin bir kat mı, kalabalık ama zengin bir kat mı.
Diğer yarısında kararı zindan veriyor — ikisi birlikte yaşıyor.

**Kimlik.** Koşuya üç yoldan biriyle başlıyorsun ve yol yalnızca başlangıç
takımını değil **Q tuşunun ne yaptığını** da belirliyor: Muhafız kalabalığı
savuruyor, Haydut sıçrıyor, Tüccar parayla kurtuluyor. Böylece seçim ilk beş
dakikaya değil yirmi katın tamamına ait oluyor.

### Kasten yapılmayanlar

- **Kayıt yok.** Oyun kısa; ölünce baştan başlıyorsun. Kaydetmek, ölümü bir
  sonuç olmaktan çıkarıp bir rahatsızlığa çevirirdi.
- **Koşular arası ilerleme yok.** Kalıcı yükseltmeler, kaybedilen koşuyu
  "yatırım"a dönüştürüp her koşunun kendi içinde bir hikâye olmasını
  engelliyordu.
- **Sonsuz zindan yok.** Yirminci katın merdiveni aşağı değil dışarı çıkıyor.
  Bir sonu olması, "ne kadar dayanırım" yerine "başarabilir miyim" sorusunu
  veriyor.

---

## Nasıl oynanır

| Tuş | İş |
| --- | --- |
| `WASD` / oklar | hareket |
| `Boşluk` | vur |
| `Q` | yol yeteneğin (sarsıntı / kaçış adımı / rüşvet) |
| `R` | savuştur — inen vuruşu tam anında karşıla |
| `F` | yerdekini al, ya da tezgâhla konuş / kapıyı aç |
| `E` | merdivende in ya da çık |
| `T` | büyücünün yanında tezgâhı aç |
| `1`–`8` | çantadaki eşyayı kullan (`Shift` ile yere bırak) |
| `ESC` | duraklat — ayarlar ve tuş listesi burada |

Ekrandaki işaretlerin anlamı:

- **Kızıl halka kapanıyor** → ağır bir vuruş geliyor. Çekil, dolan ya da savuştur.
- **Altın üçgen** → düşmanın sırtı dönük. Vuruş iki katı.
- **Altın yıldızlar** → sersemlemiş. Vurmak için pencere.
- **Altın anahtar** → anahtarı bu taşıyor. Kilitli mahzen onunla açılıyor.
- **Renkli halka (mavi/yeşil/kızıl)** → elit düşman: zırhlı, çevik ya da kanlı.

İksirler ve altın kendiliğinden alınıyor; ekipman `F` bekliyor — çünkü çantada
yer kaplıyor ve neyin eline geçtiğine sen karar vermelisin.

---

## Zindanın yapısı

Yirmi kat, dört bölge. Her bölgenin kendi taşı, kendi zemin çeşitleri, kendi
şekli (örülmüş odalar ya da oyulmuş mağaralar) ve kendi müziği var.

| Kat | Bölge | Boss |
| --- | --- | --- |
| 1–5 | Mahzen | Mahzen Bekçisi |
| 6–10 | Sarnıç | Sarnıç Boğucusu |
| 11–15 | Korluk | Kor Şeytanı |
| 16–20 | Kript | Kript Lordu |

Her beşinci katta boss merdivenin üstünde duruyor — geçmek için onu yenmen
gerekiyor. Dördü de farklı bir soru soruyor: Boğucu dört yöne salvo atıyor
(cevabı köşeye kaymak), Şeytan canı yarılanınca öfkeleniyor, Lort gölgeden
çıkıp yanında beliriyor.

Katlarda ayrıca: gezgin satıcı, kader taşı, kilitli mahzen ve kat olayları
(karanlık / sürü / sessiz / zengin) bulunuyor — hepsi katın tohumundan
belirleniyor, yani aynı tohum aynı katı veriyor.

---

## Kodun şekli

85 kaynak dosyası, 53 test dosyası, 557 test. Paketler birbirini yalnızca tek
yönde tanıyor:

```
world   ← harita, kare türleri, görüş, zindan üreticileri
ai      ← yol bulma (açgözlü ve A*)
entity  ← oyuncu, düşmanlar, eşyalar, NPC'ler
game    ← kurallar: kat kurulumu, dövüş, ekonomi, ayarlar, metinler
ui      ← JavaFX: çizim, girdi, ses
```

`game` paketi `world` ve `entity`'yi tanıyor; `ui` hepsini tanıyor; ama
`world` hiçbirini tanımıyor. Bu yüzden **oyunun kuralları JavaFX olmadan
sınanabiliyor**: testler oyun döngüsünü elle çeviriyor (`game.update(1/60.0)`)
ve pencere açmıyor.

### Tekrarlayan birkaç karar

**Polimorfizm, `instanceof` değil.** Bir eşyanın ne yaptığını eşyanın kendisi
biliyor (`use`, `getKind`, `isAutoPickedUp`), bir düşmanın ne kadar hızlı
olduğunu düşman biliyor. Yeni bir tür eklemek, çağıran kodu değiştirmemek
demek.

**Kat kurulumu tohumun saf bir fonksiyonu.** Aynı tohum aynı katı veriyor:
harita, büyücü, satıcı, taş, mahzen, olay. Bu yüzden belirlenimcilik tek başına
sınanabiliyor. Zarlar `java.util.Random`'a doğrudan tohum verilerek atılmıyor —
komşu tohumlar komşu sonuçlar verdiği için tohum önce karıştırılıyor (bu bir
hata olarak ortaya çıktı, `FloorBuilder.diceFor` javadoc'unda yazılı).

**Ölçmek, tahmin etmekten iyi.** Müzik duyulmuyordu ve sebebi "sesi kıs"
değildi: bütün enerji 40–90 Hz'de duruyordu ve dizüstü hoparlör orada hiçbir
şey üretmiyor. Bunu anlamak için oktav bandı analizörü yazıldı. Aynı yaklaşım
arayüzde de var: şerit yerleşimi ekran görüntüsü alınıp *bakılarak* ayarlandı,
mahzenin kayada yer bulma oranı 2000 katta ölçüldü (%26).

**Yorumlar "ne" değil "neden" anlatıyor.** Kod ne yaptığını söylüyor; javadoc
hangi alternatifin denendiğini ve neden bırakıldığını söylüyor. Bu dosyalardaki
her "önce şöyleydi" cümlesi gerçekten yaşanmış bir adım.

---

## Paketleme

| Kime | Komut | Çıktı |
| --- | --- | --- |
| Windows | `paketle.bat` | `paket\CryptDelver.zip` — Java gerekmiyor |
| Mac / Linux | `paketle-jar.bat` | iki jar — karşı tarafta Java 21+ gerekiyor |

Windows paketinin içinde bir Java çalışma zamanı var: karşı taraf zip'i açıp
`CryptDelver.exe`'ye çift tıklıyor. Mac için iki ayrı jar üretiliyor (Intel ve
Apple Silicon) çünkü iki mimarinin JavaFX kütüphaneleri aynı dosya adlarını
kullanıyor ve tek jar'a ikisi birden sığmıyor.

`jpackage` çapraz derleme yapmadığı için gerçek bir Mac uygulaması ancak bir
Mac'te üretilebiliyor; `.github/workflows/paket.yml` bunu GitHub'ın koşucularında
yapıyor.

---

## Varlıklar

Sprite'lar [0x72 DungeonTileset II](https://0x72.itch.io/dungeontileset-ii)
paketinden (CC-0). Hangi dosyanın nereden geldiği ve neden o seçimin yapıldığı
`src/main/resources/assets/sprites/KAYNAK.txt` içinde yazılı.

Ses efektleri ve müzik sentezlenerek üretildi — hazır dosya kullanılmadı.
Üretim araçları ve ölçümler `src/main/resources/assets/sound/KAYNAK.txt`
içinde.

---

## Lisans

Bu oyunun kodu **GNU GPL-3.0** ile lisanslı; tam metin [`LICENSE`](LICENSE)
dosyasında. Kısaca: kullanabilir, değiştirebilir, dağıtabilirsin — ama
dağıttığın sürümü de aynı lisansla ve kaynak koduyla birlikte vermen
gerekiyor. Kod kapalı bir üründe kullanılamaz.

Kodun dışındaki iki şeyin lisansı ayrı:

- **Sprite'lar** 0x72'nin paketinden ve CC-0 (kamu malı), yani onların yeniden
  dağıtımı bir şart getirmiyor.
- **Windows paketi** (`paket\CryptDelver.zip`) içinde bir OpenJDK çalışma
  zamanı taşıyor; o da GPLv2 + Classpath Exception ile lisanslı. Zip'i
  dağıtmak bu yüzden sorun değil, ama dağıttığını bilmek iyi.

Kaynak dosyalarının başına tek tek lisans başlığı koymadım: 85 dosyaya
tekrarlanan bir blok, okunurluktan aldığı kadarını hukuken geri vermiyor.
`LICENSE` dosyası ve buradaki beyan lisansı belirlemek için yeterli.
