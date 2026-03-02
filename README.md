# 🔗 mLinker

**Minecraft ↔ Discord Hesap Eşleştirme ve Rol Senkronizasyon Eklentisi**

[![Developer](https://img.shields.io/badge/Geliştirici-Musbabaff-blue?style=flat-square)](https://github.com/musbabaff)
[![GitHub](https://img.shields.io/badge/GitHub-musbabaff-black?logo=github&style=flat-square)](https://github.com/musbabaff)
[![Team](https://img.shields.io/badge/Takım-BlockStock-orange?style=flat-square)](#)
[![Dependency](https://img.shields.io/badge/Bağımlılık-LuckPerms-lightgrey?style=flat-square)](https://luckperms.net/)

**mLinker**, Minecraft sunucunuzdaki oyuncuların Discord hesaplarını oyun içi hesaplarıyla eşleştirmelerini sağlayan gelişmiş bir köprü eklentisidir. **LuckPerms** entegrasyonu sayesinde, oyuncuların oyun içi yetki grupları (VIP, Rehber, vs.) anında Discord rollerine yansıtılır.

---

## ✨ Özellikler

* **Kusursuz Eşleştirme:** Oyun içinden alınan basit bir kodla Discord üzerinden hızlı hesap doğrulama.
* **LuckPerms Entegrasyonu:** Minecraft'ta bir oyuncunun grubu değiştiğinde (örneğin VIP satın aldığında), Discord sunucusundaki rolü anında güncellenir.
* **Çift Yönlü Senkronizasyon:** İsteğe bağlı olarak Discord'da rolü alınan kişinin oyun içi yetkilerini düzenleyebilme altyapısı.
* **Tamamen Özelleştirilebilir:** `config.yml` üzerinden tüm mesajları, dil seçeneklerini ve rol eşleştirmelerini kendi sunucunuza göre ayarlayabilirsiniz.
* **Performans Dostu:** Sunucunuzu yormayacak şekilde asenkron veritabanı (SQLite/MySQL) işlemleri.

---

## 🛠️ Kurulum

1. `mLinker.jar` dosyasını indirin.
2. Sunucunuzun `plugins` klasörünün içine atın.
3. Sunucuyu yeniden başlatın (veya plugini aktif edin).
4. `plugins/mLinker/config.yml` dosyasını açın.
5. Discord Bot Token'ınızı ve sunucu ID'nizi ilgili yerlere yapıştırın.
6. LuckPerms gruplarınız ile Discord rol ID'lerinizi eşleştirin.
7. Oyun içinden `/mreload` komutunu kullanarak yapılandırmayı yenileyin. **İşlem tamam!**

---

## 💻 Komutlar ve Yetkiler

| Komut | Açıklama | Yetki (Permission) |
| :--- | :--- | :--- |
| `/link` veya `/hesapbagla` | Discord hesabı eşleştirmek için gereken kodu verir. | *Varsayılan* |
| `/unlink` | Eşleştirilmiş hesabı ayırır. | *Varsayılan* |
| `/mreload` | Eklenti yapılandırmasını yeniden yükler. | `mlinker.admin` |
| `/minfo <oyuncu>` | Oyuncunun bağlı olduğu Discord hesabını gösterir. | `mlinker.admin` |

---

## ⚙️ Gereksinimler

* Java 17 veya üzeri
* Spigot / Paper (1.16.x - 1.20.x arası tavsiye edilir)
* [LuckPerms](https://luckperms.net/) eklentisi

---

## 📞 İletişim & Destek

Bir hata (bug) bulduysanız veya yeni bir özellik öneriniz varsa, benimle Discord üzerinden iletişime geçebilir veya GitHub üzerinden Issue açabilirsiniz.

**Geliştirici:** musbabaff | BlockStock.art  
**Discord:** `musbabaff`  
**GitHub:** [musbabaff](https://github.com/musbabaff)