package com.cryptdelver.game;

/**
 * Oyuncunun gördüğü her metin.
 *
 * <p>Her satır bir metnin iki dildeki hâli, yan yana. Ayrı dosyalarda iki
 * sözlük tutmak yerine böyle yazıldı çünkü <b>çeviriyi unutmak imkânsız
 * oluyor</b>: yeni bir metin eklerken İngilizcesini de aynı satıra yazmak
 * zorundasın, sonradan "hangi anahtarın karşılığı eksik" diye aramak
 * gerekmiyor.</p>
 *
 * <p>Anahtar bir enum olduğu için yazım hatası derleme hatası: elle yazılan
 * {@code "menu.new_game"} gibi bir dizgede hata ancak çalışma zamanında
 * görülürdü.</p>
 *
 * <h2>Neden durağan bir dil alanı</h2>
 * <p>Dil, {@link Settings} içinde yaşıyor ve oradan buraya bildiriliyor. Metin
 * isteyen taraf dili taşımak zorunda değil. Alternatifi dili <em>her</em>
 * imzadan geçirmekti: düşmanın adını soran çizim kodundan, iksirin mesajını
 * yazan {@code use} metoduna kadar her yere bir parametre eklemek. Oyunda
 * oturum başına bir kez değişen bir ayar için bu, kodun tamamını kirletirdi.</p>
 *
 * <p>Biçimlendirme yalnızca argüman verilince yapılıyor: {@code "%50"} gibi
 * içinde yüzde işareti olan metinler argümansız çağrıldığında olduğu gibi
 * kalsın diye.</p>
 */
public enum Text {

    // ------------------------------------------------------------------ menü

    GAME_TITLE("CRYPTDELVER", "CRYPTDELVER"),
    GAME_TAGLINE("Kripte in, ganimeti topla, Kript Lordu'nu geç.",
            "Descend the crypt, take the loot, beat the Crypt Lord."),
    MENU_NEW_GAME("Yeni Oyun", "New Game"),
    MENU_SETTINGS("Ayarlar", "Settings"),
    MENU_HELP("Nasıl Oynanır", "How to Play"),
    MENU_QUIT("Çıkış", "Quit"),
    MENU_HINT("Yön tuşlarıyla seç, Enter ile onayla",
            "Arrow keys to choose, Enter to confirm"),
    SETTINGS_HINT("Yön tuşlarıyla değiştir, ESC ile geri dön",
            "Arrow keys to change, ESC to go back"),

    // ----------------------------------------------------------------- ayarlar

    SETTING_LANGUAGE("Dil", "Language"),
    SETTING_EFFECTS("Efekt sesi", "Effects"),
    SETTING_MUSIC("Müzik", "Music"),
    SETTING_MUTE("Sessiz", "Mute"),
    SETTING_DIFFICULTY("Zorluk", "Difficulty"),
    SETTING_BACK("Geri", "Back"),
    ON("Açık", "On"),
    OFF("Kapalı", "Off"),

    DIFFICULTY_EASY("Kolay", "Easy"),
    DIFFICULTY_NORMAL("Normal", "Normal"),
    DIFFICULTY_HARD("Zor", "Hard"),
    DIFFICULTY_EASY_HINT("Kolay: kat daha tenha, düşmanlar derinlikle yavaş sertleşir.",
            "Easy: floors are emptier and enemies toughen slowly with depth."),
    DIFFICULTY_NORMAL_HINT("Normal: oyunun dengelendiği kademe.",
            "Normal: the setting the game is balanced around."),
    DIFFICULTY_HARD_HINT("Zor: kat kalabalık, düşmanlar derinlikle hızla sertleşir.",
            "Hard: floors are crowded and enemies toughen fast with depth."),

    // ------------------------------------------------------------------ tuşlar

    KEYS_TITLE("— TUŞLAR —", "— KEYS —"),
    KEY_MOVE("WASD / oklar", "WASD / arrows"),
    KEY_MOVE_WHAT("hareket", "move"),
    KEY_ATTACK("Boşluk", "Space"),
    KEY_ATTACK_WHAT("vur", "attack"),
    KEY_DASH("Q", "Q"),
    KEY_DASH_WHAT("kacis adimi: baktigin yone sicra", "dash: leap the way you face"),
    KEY_TAKE("F", "F"),
    KEY_TAKE_WHAT("yerdeki ekipmanı al ya da tezgâhtakiyle konuş",
            "pick up gear or talk to whoever keeps a bench"),
    KEY_STAIRS("E", "E"),
    KEY_STAIRS_WHAT("merdivende in ya da çık", "go down or up at stairs"),
    KEY_FORGE("T", "T"),
    KEY_FORGE_WHAT("büyücünün yanında tezgâhı aç", "open the bench beside the wizard"),
    KEY_USE("1-8 / tık", "1-8 / click"),
    KEY_USE_WHAT("çantadaki eşyayı kullan / kuşan", "use or equip a bag item"),
    KEY_DROP("Shift + 1-8 / tık", "Shift + 1-8 / click"),
    KEY_DROP_WHAT("eşyayı yere bırak", "drop the item"),
    KEY_VOLUME("- / + / M", "- / + / M"),
    KEY_VOLUME_WHAT("ses azalt / artır / sustur", "volume down / up / mute"),
    KEY_RESTART("Enter", "Enter"),
    KEY_RESTART_WHAT("ölünce yeniden başla", "restart after death"),
    KEY_PAUSE("ESC", "ESC"),
    KEY_PAUSE_WHAT("devam et", "resume"),
    KEY_AUTOPICK_NOTE("iksir ve altın kendiliğinden alınır",
            "potions and gold are picked up automatically"),

    // -------------------------------------------------------------- bilgi şeridi

    PANEL_CHARACTER("KARAKTER", "CHARACTER"),
    PANEL_BAG("ÇANTA", "BAG"),
    PANEL_COMBAT("SAVAŞ", "COMBAT"),
    PANEL_ITEM("EŞYA", "ITEMS"),
    PANEL_STATUS("DURUM", "STATUS"),
    HUD_ATTACK("Vuruş %d", "Attack %d"),
    HUD_DEFENSE("Zırh %d", "Armor %d"),
    HUD_GOLD("Altın %d", "Gold %d"),
    HUD_DEPTH("Kat %d/%d", "Floor %d/%d"),
    HUD_TIME("Süre %.0fs", "Time %.0fs"),
    HUD_ENEMIES("Düşman %d", "Enemies %d"),
    HUD_DUNGEON_AWAKE("ZİNDAN UYANDI", "THE DUNGEON IS AWAKE"),
    HUD_BAG_HINT("F ekipmanı alır · 1-8 kullanır · Shift+1-8 bırakır",
            "F takes gear · 1-8 uses · Shift+1-8 drops"),
    HUD_ESC_HINT("ESC: durdur, ayarlar ve tuşlar", "ESC: pause, settings and keys"),
    GEAR_SLOT_ARMOR("Z", "A"),
    GEAR_SLOT_WEAPON("S", "W"),
    GEAR_BROKEN("KIRIK", "BROKEN"),

    // ---------------------------------------------------------------- ipuçları

    HINT_TAKE_ONE("F ile %s al", "F to take %s"),
    HINT_TAKE_MANY("F ile %d eşyayı al", "F to take %d items"),
    HINT_OPEN_BENCH("F ile tezgâhı aç", "F to open the bench"),
    HINT_OPEN_SHOP("F ile tezgâha bak", "F to browse the stall"),
    HINT_STAIRS_LOCKED("Merdiveni tutan şeyi önce yen", "Beat what guards the stairs first"),
    HINT_STAIRS_UP("E ile bir üst kata çık", "E to go up one floor"),
    HINT_STAIRS_DOWN("E ile bir alt kata in", "E to go down one floor"),
    HINT_STAIRS_EXIT("E ile kriptten çık", "E to leave the crypt"),
    WIZARD_SIGN("BÜYÜCÜ", "WIZARD"),
    MERCHANT_SIGN("GEZGİN SATICI", "PEDLAR"),

    // ----------------------------------------------------------------- perdeler

    PAUSED("DURAKLATILDI", "PAUSED"),
    SETTINGS_TITLE("— AYARLAR —", "— SETTINGS —"),
    PAUSE_SOUND_HINT("- / +  efekt,  M  sustur,  müzik menüdeki ayarlardan",
            "- / +  effects,  M  mute,  music in the settings menu"),
    GAME_OVER("ÖLDÜN", "YOU DIED"),
    GAME_OVER_LINE("%d. katta düştün    %d altın topladın",
            "You fell on floor %d    you gathered %d gold"),
    GAME_OVER_HINT("Enter ile yeniden başla", "Enter to start again"),
    VICTORY("KURTULDUN", "YOU ESCAPED"),
    VICTORY_LINE("Yirmi kat indin ve geri döndün.", "Twenty floors down and back again."),
    VICTORY_STATS("%d altın  ·  %.0f saniye", "%d gold  ·  %.0f seconds"),

    RECORDS_TITLE("— GEÇMİŞ —", "— HISTORY —"),
    RECORDS_DEEPEST("En derin kat %d", "Deepest floor %d"),
    RECORDS_GOLD("En çok altın %d", "Most gold %d"),
    RECORDS_RUNS("%d koşu, %d kez kurtuldun", "%d runs, escaped %d times"),
    RECORDS_NO_WIN("%d koşu, henüz kurtulamadın", "%d runs, no escape yet"),

    // ------------------------------------------------------------------ tezgâh

    FORGE_TITLE("BÜYÜCÜ", "WIZARD"),
    FORGE_PURSE("Kesende %d altın var.", "You carry %d gold."),
    FORGE_REPAIR("%s tamir et", "Repair %s"),
    FORGE_UPGRADE("%s yükselt", "Upgrade %s"),
    FORGE_WEAPON("Silahı", "weapon"),
    FORGE_ARMOR("Zırhı", "armor"),
    FORGE_TO_WEAPON("Kılıca", "Weapon"),
    FORGE_TO_ARMOR("Zırha", "Armor"),
    FORGE_ENCHANTS_TITLE("— BÜYÜLER (her parçada bir tane durur) —",
            "— ENCHANTMENTS (one per piece) —"),
    FORGE_CAP_NOTE("Yükseltme tavanı, bu katta bossun bırakacağı parça kadar.",
            "The upgrade cap matches what this floor's boss would drop."),
    FORGE_LEAVE("ESC ile tezgâhtan ayrıl", "ESC to leave the bench"),
    FORGE_NO_GEAR("kuşanılmış parça yok", "nothing equipped"),
    FORGE_ACTIVE("takılı — %s", "active — %s"),
    FORGE_COST("%d altın", "%d gold"),
    FORGE_NONE("—", "—"),
    FORGE_SOUND("saglam", "sound"),

    // ------------------------------------------------------------ satıcı tezgâhı

    SHOP_PURSE("Kesende %d altın var.", "You carry %d gold."),
    SHOP_SOLD("satıldı", "sold"),
    SHOP_EMPTY("Tezgâh boşaldı. Bir sonraki satıcıya kadar bu kadar.",
            "The stall is bare. That's it until the next pedlar."),
    SHOP_NOTE("Satıcı boss katlarında çıkmaz; oradaki tezgâh büyücünün.",
            "Pedlars keep off boss floors; that bench belongs to the wizard."),
    SHOP_LEAVE("ESC ile tezgâhtan ayrıl", "ESC to leave the stall"),
    GEAR_WEAPON("Silah", "Weapon"),
    GEAR_ARMOR("Zırh", "Armor"),
    GEAR_YOUR_WEAPON("Kılıcın", "Your blade"),
    GEAR_YOUR_ARMOR("Zırhın", "Your armor"),

    // ------------------------------------------------------------------ bölgeler

    REGION_MAHZEN("Mahzen", "The Vault"),
    REGION_SARNIC("Sarnıç", "The Cistern"),
    REGION_KORLUK("Korluk", "The Embers"),
    REGION_KRIPT("Kript", "The Crypt"),

    // -------------------------------------------------------------------- büyüler

    ENCHANT_VAMPIRISM("Vampirlik", "Vampirism"),
    ENCHANT_VAMPIRISM_INFO("her öldürmede 2 can", "2 health per kill"),
    ENCHANT_LIGHTNING("Yıldırım", "Lightning"),
    ENCHANT_LIGHTNING_INFO("vuruş bir kare uzağa daha erişir", "your swing reaches one tile further"),
    ENCHANT_HASTE("Acele", "Haste"),
    ENCHANT_HASTE_INFO("daha hızlı savuruyorsun", "you swing faster"),
    ENCHANT_THORNS("Diken", "Thorns"),
    ENCHANT_THORNS_INFO("sana vurana 1 hasar", "1 damage to whoever hits you"),
    ENCHANT_REGEN("Yenilenme", "Regeneration"),
    ENCHANT_REGEN_INFO("birkaç saniyede bir 1 can", "1 health every few seconds"),
    ENCHANT_AGILITY("Çeviklik", "Agility"),
    ENCHANT_AGILITY_INFO("daha hızlı yürüyorsun", "you walk faster"),
    ENCHANT_STURDY("Sağlamlık", "Sturdiness"),
    ENCHANT_STURDY_INFO("yıpranma yarı hızda", "wear at half speed"),

    // --------------------------------------------------------------------- eşya

    ITEM_POTION("İksir", "Potion"),
    ITEM_POTION_INFO("İçince %d can", "%d health when drunk"),
    ITEM_BOMB("Bomba", "Bomb"),
    ITEM_BOMB_INFO("%d kare içindeki herkese %2$d hasar",
            "%2$d damage to everyone within %1$d tiles"),
    ITEM_HASTE("Hız İksiri", "Haste Potion"),
    ITEM_HASTE_INFO("%d saniye daha hızlı yürürsün", "walk faster for %d seconds"),
    ITEM_FURY("Öfke İksiri", "Fury Potion"),
    ITEM_FURY_INFO("%d saniye +%2$d vuruş", "+%2$d attack for %1$d seconds"),
    ITEM_ESCAPE("Kaçış İksiri", "Escape Potion"),
    ITEM_ESCAPE_INFO("Seni merdivenin başına ışınlar", "Teleports you to the stairs"),
    ITEM_GOLD("Altın", "Gold"),
    ITEM_ARROW("Ok", "Arrow"),

    WEAPON_RUSTY("Paslı Kılıç", "Rusty Sword"),
    WEAPON_STEEL("Çelik Kılıç", "Steel Sword"),
    WEAPON_AXE("Savaş Baltası", "Battle Axe"),
    WEAPON_CRYPT("Kript Kılıcı", "Crypt Blade"),
    WEAPON_LEGEND("Yıldızkıran", "Starbreaker"),
    ARMOR_LEATHER("Deri Zırh", "Leather Armor"),
    ARMOR_CHAIN("Zincir Zırh", "Chain Mail"),
    ARMOR_PLATE("Plaka Zırh", "Plate Armor"),
    ARMOR_CRYPT("Kript Plakası", "Crypt Plate"),
    GEAR_WEAPON_INFO("+%d vuruş  ·  %d/%d", "+%d attack  ·  %d/%d"),
    GEAR_ARMOR_INFO("+%d savunma  ·  %d/%d", "+%d armor  ·  %d/%d"),

    // ------------------------------------------------------------------ canlılar

    PLAYER_NAME("Kâşif", "Delver"),
    ENEMY_IMP("İmp", "Imp"),
    ENEMY_GOBLIN("Goblin", "Goblin"),
    ENEMY_SKELETON("İskelet", "Skeleton"),
    ENEMY_ORC("Ork Savaşçısı", "Orc Warrior"),
    ENEMY_ZOMBI("Zombi", "Zombie"),
    ENEMY_SAMAN("Şahin Şaman", "Hawk Shaman"),
    ENEMY_ARCHER("Okçu", "Archer"),
    BOSS_BEKCI("Mahzen Bekçisi", "Vault Warden"),
    BOSS_BOGUCU("Sarnıç Boğucusu", "Cistern Choker"),
    BOSS_SEYTAN("Kor Şeytanı", "Ember Devil"),
    BOSS_LORT("Kript Lordu", "Crypt Lord"),
    WIZARD_NAME("Büyücü", "Wizard"),
    MERCHANT_NAME("Gezgin Satıcı", "Pedlar"),

    // ----------------------------------------------------------------- büyücünün ağzı

    WIZARD_BROKEN("Kırılmış o! Bırak da onarayım.", "That's broken! Let me mend it."),
    WIZARD_WORN("Şu takımının hâline baksana. Tamir ister.",
            "Look at the state of your kit. It wants repair."),
    WIZARD_EMPTY("Elin boş gezme. Bir şey getir, üstünde çalışayım.",
            "Don't wander empty-handed. Bring me something to work on."),
    WIZARD_UPGRADE("Altınını sayma, çeliğini büyütelim.",
            "Never mind counting gold — let's grow that steel."),
    WIZARD_BOSS("Aşağıdakine böyle gitme, bir düşün.",
            "Don't go to the thing below like that. Think."),
    WIZARD_IDLE("Ocak yanıyor, büyüler hazır.", "The forge is lit, the spells are ready."),

    // ----------------------------------------------------------- satıcının ağzı

    MERCHANT_IDLE("Aşağısı uzun yol. Yanına bir şey al.",
            "It's a long way down. Take something with you."),
    MERCHANT_BROKE("Kese boşsa bakmanın zararı yok, alamazsın ama.",
            "Empty purse? Look all you like, you'll buy nothing."),
    MERCHANT_SOLD_OUT("Tezgâh bitti. Aşağıda yine karşılaşırız.",
            "Stall's empty. We'll meet again further down."),

    // -------------------------------------------------------------------- dövüş

    MSG_ENEMY_HURT("%s %d hasar aldı.", "%s took %d damage."),
    MSG_ENEMY_DOWN("%s yere serildi.", "%s went down."),
    MSG_PLAYER_HURT("%s sana %d hasar vurdu.", "%s hit you for %d."),
    MSG_ARROW_HURT("%s oku sana %d hasar vurdu.", "%s's arrow hit you for %d."),
    MSG_SWING_MISS("Kılıcın boşluğu kesti.", "Your blade cuts empty air."),
    MSG_VAMPIRISM("Vampirlik %d can emdi.", "Vampirism drained %d health."),
    MSG_THORNS("Diken %s üstünde %d hasar açtı.", "Thorns opened %d damage on %s."),
    MSG_BOSS_SUMMON("%s %d yaratık çağırdı!", "%s summoned %d creatures!"),
    MSG_BOSS_WINDUP("%s şişiyor — köşeye kay!", "%s is swelling — slip to a corner!"),
    MSG_BOSS_ENRAGE("%s öfkelendi! Artık kaçamazsın.",
            "%s is enraged! No more running."),
    MSG_BOSS_BLINK("%s gölgeden çıktı — yanındasın!",
            "%s stepped out of the shadow — it's on you!"),
    MSG_BOSS_FELL("%s düştü! %s bıraktı.", "%s has fallen! It dropped %s."),
    MSG_BOSS_REWARD("Gücü sana geçti: +%d azami can.",
            "Its power passed to you: +%d max health."),
    MSG_BOSS_GUARDS("%s merdiveni tutuyor; önce onu geç.",
            "%s guards the stairs; get past it first."),
    MSG_BOSS_ANNOUNCE("%s merdiveni tutuyor. Yavaş — vur ve geri çekil.",
            "%s guards the stairs. Slowly — strike and step back."),
    MSG_DIED("Zindanda öldün.", "You died in the dungeon."),

    // --------------------------------------------------------------------- eşya

    MSG_TOOK("%s aldın.", "You took %s."),
    MSG_DROPPED("%s yere bıraktın.", "You dropped %s."),
    MSG_DISCARDED("%s yere bırakıldı.", "%s was left on the ground."),
    MSG_BAG_FULL("Çantan dolu: %s yerde kaldı.", "Your bag is full: %s stayed on the ground."),
    MSG_NOTHING_HERE("Ayağının altında bir şey yok.", "There's nothing at your feet."),
    MSG_GOLD_TAKEN("%d altın topladın.", "You gathered %d gold."),
    MSG_EQUIPPED_WEAPON("%s kuşandın (+%d vuruş).", "You wield %s (+%d attack)."),
    MSG_EQUIPPED_ARMOR("%s kuşandın (+%d savunma).", "You wear %s (+%d armor)."),
    MSG_ALREADY_WIELDED("%s zaten elinde.", "%s is already in your hand."),
    MSG_ALREADY_WORN("%s zaten üstünde.", "%s is already on you."),
    MSG_GEAR_BROKE("%s kırıldı! Büyücüye uğrayana kadar hiçbir işe yaramaz.",
            "%s broke! It's useless until you see the wizard."),

    // ------------------------------------------------------------------ iksirler

    MSG_POTION_DRUNK("İksiri içtin: %d can geldi.", "You drink the potion: %d health back."),
    MSG_POTION_WASTED("Canın zaten dolu.", "Your health is already full."),
    MSG_HASTE("Hız iksiri: ayakların hafifledi.", "Haste potion: your feet are light."),
    MSG_FURY("Öfke iksiri: vuruşun sertleşti (+%d).", "Fury potion: your swing hardens (+%d)."),
    MSG_ESCAPE("Kaçış iksiri: merdivenin başındasın.",
            "Escape potion: you're at the stairs."),
    MSG_NO_STAIRS("Bu katta merdiven yok.", "There are no stairs on this floor."),
    MSG_BOMB_EMPTY("Bomba boşluğa patladı.", "The bomb blew up in empty air."),
    MSG_BOMB_HIT("Bomba patladı: %d düşman vuruldu.", "The bomb went off: %d enemies hit."),

    // ------------------------------------------------------------------ büyücü işi

    MSG_NO_WIZARD("Yakında büyücü yok. Büyücüler boss katlarında.",
            "No wizard nearby. Wizards keep to boss floors."),
    MSG_GO_TO_WIZARD("Önce büyücüye git.", "Go to the wizard first."),
    MSG_NO_MERCHANT("Yakında satıcı yok. Satıcılar boss aralarında dolaşır.",
            "No pedlar nearby. They wander the floors between bosses."),
    MSG_GO_TO_MERCHANT("Önce satıcıya git.", "Go to the pedlar first."),
    MSG_BOUGHT("%s satın aldın (-%d altın).", "You bought %s (-%d gold)."),
    MSG_BAG_FULL_SHOP("Çantan dolu; önce yer aç.", "Your bag is full; make room first."),
    MSG_NOT_ENOUGH_GOLD("Altın yetmiyor: %d gerekiyor, %d var.",
            "Not enough gold: %d needed, you have %d."),
    MSG_NOTHING_EQUIPPED("%s kuşanmadın.", "You have no %s equipped."),
    MSG_ALREADY_WHOLE("%s zaten sapasağlam.", "%s is perfectly sound already."),
    MSG_REPAIRED("%s tamir edildi (-%d altın).", "%s repaired (-%d gold)."),
    MSG_UPGRADE_CAP("%s bu katta daha ileri gitmiyor; bossu geç.",
            "%s goes no further on this floor; beat the boss."),
    MSG_UPGRADED("%s yükseltildi (-%d altın).", "%s upgraded (-%d gold)."),
    MSG_ENCHANT_REFUSED("%s bu büyüyü taşımaz.", "%s won't carry that enchantment."),
    MSG_ENCHANT_ALREADY("%s zaten %s taşıyor.", "%s already carries %s."),
    MSG_ENCHANTED("%s artık %s taşıyor (-%d altın).",
            "%s now carries %s (-%d gold)."),
    MSG_ENCHANT_REPLACED("%s silindi, yerine %s basıldı (-%d altın).",
            "%s was erased and %s took its place (-%d gold)."),

    // ------------------------------------------------------------------- zindan

    MSG_WELCOME("Zindana indin. Boşluk vurur, Q sıçrar, F ekipman alır.",
            "You descend. Space strikes, Q dashes, F takes gear."),
    MSG_DESCENDED("%d. kata indin (%s).", "You went down to floor %d (%s)."),
    MSG_ASCENDED("%d. kata çıktın (%s).", "You climbed up to floor %d (%s)."),
    MSG_ESCAPED("Kriptten çıktın. Zindan arkanda kaldı.",
            "You left the crypt. The dungeon is behind you."),
    MSG_RESTARTED("Yeniden zindana indin.", "You descend into the dungeon again."),
    MSG_DUNGEON_AWAKE("Zindan seni fark etti. Oyalanma.",
            "The dungeon has noticed you. Don't linger."),
    MSG_DUNGEON_ANGRIER("Zindan geri döndüğünü gördü; artık daha çabuk uyanıyor.",
            "The dungeon saw you turn back; it wakes faster now."),
    MSG_LEGEND_START("Yıldızkıran elinde. İki büyü yuvası var.",
            "Starbreaker is in your hand. It has two enchantment slots."),

    // --------------------------------------------------------------------- ses

    MSG_VOLUME("Ses: %%%d", "Volume: %d%%"),
    MSG_MUTED("Ses kapatıldı.", "Sound off."),
    MSG_UNMUTED("Ses açıldı.", "Sound on.");

    private static Language language = Language.TURKCE;

    private final String turkish;
    private final String english;

    Text(String turkish, String english) {
        this.turkish = turkish;
        this.english = english;
    }

    /** Bundan sonra istenen bütün metinler bu dilde gelir. */
    public static void use(Language chosen) {
        language = chosen == null ? Language.TURKCE : chosen;
    }

    public static Language current() {
        return language;
    }

    /**
     * Metnin o anki dildeki hâli.
     *
     * <p>Argüman verilmezse biçimlendirme <em>hiç</em> çalışmıyor: içinde
     * yüzde işareti geçen metinlerin ({@code "%50"}) kazara biçim dizgesi
     * sayılmasını engelliyor.</p>
     */
    public String get(Object... args) {
        String pattern = language == Language.ENGLISH ? english : turkish;
        return args.length == 0 ? pattern : String.format(pattern, args);
    }
}
