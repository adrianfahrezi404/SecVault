# SecVault P2P 🛡️

**SecVault** adalah Brankas Bukti Digital Terdesentralisasi yang dibangun di atas jaringan *Peer-to-Peer* (P2P) *Distributed Hash Table* (DHT) yang sangat efisien. Dirancang khusus untuk operasi *Red Teaming*, *Bug Bounty*, dan penyimpanan bukti forensik yang aman, SecVault menggunakan *End-to-End Encryption* (E2EE) dan perutean `O(log N)` cerdas untuk mendistribusikan dan menyimpan file digital lintas node secara aman.

Hal yang membuat proyek ini sangat spesial adalah pembangunannya yang **100% menggunakan Java Murni (JDK 8+) tanpa satupun library eksternal (zero-dependency)**.

---

## ✨ Fitur Utama

- **Distributed Hash Table (DHT) Routing** 🕸️
  Mengimplementasikan algoritma *hashing* 5-bit bergaya *Chord* menggunakan *Finger Table*, yang memungkinkan pencarian dan perutean file secepat kilat dengan kompleksitas `O(log N)` melintasi jaringan desentralisasi.
  
- **End-to-End Encryption (E2EE)** 🔒
  File dienkripsi secara *on-the-fly* menggunakan `AES-256/CBC/PKCS5Padding` dan ekstraksi kunci `PBKDF2WithHmacSHA256` persis sebelum meninggalkan komputer pengirim. Node penerima (Successor) hanya akan menerima dan menyimpan *ciphertext* acak yang tidak bisa dibaca.

- **Pure Java Custom REST API** 🌐
  Dilengkapi dengan *server* REST API *multi-threaded* yang dibangun sepenuhnya dari nol menggunakan kelas bawaan `com.sun.net.httpserver.HttpServer` (Tanpa Spring Boot, tanpa Maven, dan tanpa *library parser* JSON pihak ketiga).

- **Neumorphism Desktop UI** 🎨
  Antarmuka "Soft UI" Desktop yang memukau dan modern. Dibangun sepenuhnya menggunakan *override* grafis mentah `Java 2D Graphics` dan *Swing* untuk menyimulasikan *blur* 3D serta bayangan dinamis (Tanpa FlatLaf atau *framework* UI eksternal).

- **Smart Evidence Viewer** 👁️
  Mendekripsi bukti digital secara otomatis ke dalam memori sementara saat file diklik-ganda pada GUI. Setelah jendela *preview* ditutup, sistem akan memanggil perintah `.deleteOnExit()` untuk memusnahkan sisa-sisa file tidak terenkripsi secara permanen, sehingga mencegah kebocoran forensik lokal.

---

## 🏗️ Arsitektur Teknologi

- **Backend & Jaringan**: *Pure Java Sockets* (`java.net.*`), `java.io.*`, *Native Multithreading*.
- **Kriptografi**: `javax.crypto.*`, `java.security.*`.
- **Desktop UI**: `javax.swing.*`, `java.awt.Graphics2D`.

---

## 📂 Struktur Folder
```text
.
├── README.md
├── bin/                       # Folder output kompilasi (.class)
└── src/
    └── secvault/              # Source code Java utama
        ├── ApiServer.java
        ├── CryptoUtils.java
        ├── Main.java
        └── ... (file lainnya)
```

---

## 🚀 Cara Menjalankan Program

1. *Clone* repositori ini ke komputer lokal Anda.
2. Lakukan kompilasi seluruh file Java dari dalam folder *root* proyek:
   ```bash
   javac -d bin src/secvault/*.java
   ```
3. **Konfigurasi Jaringan**: 
   Untuk menjalankan program ini di laptop yang berbeda dalam jaringan internet sungguhan, buka `src/secvault/Main.java` dan petakan ID Node ke alamat IP Publik atau IP VPN (ZeroTier/Tailscale) Anda di dalam variabel `nodeIps`. (Jangan lupa *compile* ulang setelah mengubah IP).
4. Jalankan satu node (misalnya Node 3):
   ```bash
   java -cp bin secvault.Main 3
   ```
5. Buka terminal lain (atau gunakan laptop teman Anda) dan jalankan node *successor*:
   ```bash
   java -cp bin secvault.Main 11
   ```
6. Masukkan **Master Key** rahasia yang sama saat *startup* (pop-up) untuk mengautentikasi brankas dan mulailah mentransfer bukti digital terenkripsi Anda!

---

## 📝 Catatan Akademik / Portofolio
Proyek ini dikembangkan dengan secara ketat mematuhi aturan *zero-dependency* (tanpa *library*), yang mendemonstrasikan pemahaman mendalam tentang jaringan inti (*Socket programming*), matematika perutean (algoritma DHT), transmisi *stream* biner level bit, kriptografi *cipher*, dan manipulasi *rendering* GUI mentah secara presisi.