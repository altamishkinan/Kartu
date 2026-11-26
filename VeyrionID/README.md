# VeyrionID

Plugin Paper 1.21.1 untuk Kartu Identitas Veyrion.

## Build
    mvn package
Hasil: `target/VeyrionID-1.1.0.jar` → taruh di folder `plugins/`.
(Butuh Java 21. Dibuat untuk Paper 1.21.1; versi 1.21.x lain umumnya tetap jalan.)

## Perintah
| Perintah | Fungsi |
|---|---|
| `/ki daftar <nama> <umur> <DD/MM/YYYY> <ras>` | Daftar & terima kartu (pakai `_` untuk spasi) |
| `/ki daftar <pemain> <nama> <umur> <DD/MM/YYYY> <ras>` | Daftarkan pemain lain (izin `veyrion.id.register.other`). Jika pemain offline, kartu diberikan saat ia masuk |
| `/ki lihat [pemain]` | Lihat kartu (pemain lain: admin) |
| `/ki tunjuk` | Tunjukkan kartu ke pemain terdekat |
| `/ki kartu` | Ambil ulang item kartu |
| `/ki hapus <pemain>` | Hapus data (admin) |
| `/ki reload` | Muat ulang config (admin) |

Alias: `/identitas`, `/ktp`. Izin: `veyrion.id.use` (default semua), `veyrion.id.admin` (OP), `veyrion.id.register.other` (OP).

## Interaksi
Kartu berupa **buku tertulis** (bukan kertas lagi). Klik kanan kartu → buku terbuka menampilkan data. Kartu lama berbahan kertas tetap berfungsi, ambil yang baru dengan `/ki kartu`. Klik kanan pemain sambil memegang kartu → tunjukkan ke pemain itu.

## Config
`config.yml`: daftar ras, izin daftar ulang, radius `tunjuk`, dan `custom-model-data` untuk tekstur kartu (resource pack, mis. memakai logo Veyrion).
