-- =====================================================================
-- product-service :: V2 - data contoh katalog
--
-- Harga dalam satuan minor (sen / 1/100). Jadi 8500000 = Rp 85.000,00
-- Gambar memakai picsum.photos sebagai placeholder deterministik.
--
-- Teknik: masukkan data sebagai tabel VALUES lalu JOIN ke categories.
-- Lebih aman dan lebih ringkas daripada subquery berulang, dan baru
-- gagal saat migrate kalau ada slug kategori yang salah ketik.
-- =====================================================================

INSERT INTO categories (slug, name, display_order)
VALUES ('kopi-teh', 'Kopi & Teh', 1),
       ('makanan-ringan', 'Makanan Ringan', 2),
       ('batik-fashion', 'Batik & Fashion', 3),
       ('elektronik', 'Elektronik', 4),
       ('perawatan-diri', 'Perawatan Diri', 5),
       ('olahraga', 'Olahraga', 6),
       ('rumah-tangga', 'Rumah Tangga', 7),
       ('kerajinan', 'Kerajinan Tangan', 8);

INSERT INTO products (sku, slug, name, description, price_cents, currency, category_id, image_url, stock_quantity)
SELECT v.sku,
       v.slug,
       v.name,
       v.description,
       v.price_cents,
       'IDR',
       c.id,
       v.image_url,
       v.stock_quantity
FROM (VALUES
    ('KOP-001', 'kopi-gayo-arabica-250g', 'Kopi Gayo Arabica 250g',
     'Kopi single origin dari dataran tinggi Gayo, Aceh. Profil rasa: cokelat, jeruk manis, keasaman rendah. Digiling sesuai pesanan.',
     CAST(8500000 AS BIGINT), 'kopi-teh', 'https://picsum.photos/seed/kopi-gayo/600/600', 48),

    ('KOP-002', 'kopi-toraja-sapan-200g', 'Kopi Toraja Sapan 200g',
     'Kopi Toraja dengan body tebal dan aftertaste panjang. Cocok untuk espresso maupun tubruk.',
     CAST(9500000 AS BIGINT), 'kopi-teh', 'https://picsum.photos/seed/kopi-toraja/600/600', 32),

    ('TEH-001', 'teh-hijau-jasmine-premium', 'Teh Hijau Jasmine Premium 100g',
     'Teh hijau dengan bunga melati asli, dipetik pada musim kemarau. Aroma kuat, tidak pahit.',
     CAST(6200000 AS BIGINT), 'kopi-teh', 'https://picsum.photos/seed/teh-jasmine/600/600', 60),

    ('MKN-001', 'keripik-pisang-cokelat-200g', 'Keripik Pisang Cokelat 200g',
     'Pisang kepok pilihan diiris tipis, digoreng renyah, dilapisi cokelat compound.',
     CAST(3200000 AS BIGINT), 'makanan-ringan', 'https://picsum.photos/seed/keripik-pisang/600/600', 120),

    ('MKN-002', 'kacang-mede-madura-150g', 'Kacang Mede Madura 150g',
     'Kacang mede panggang tanpa kulit, gurih dan renyah. Tanpa pengawet.',
     CAST(5800000 AS BIGINT), 'makanan-ringan', 'https://picsum.photos/seed/kacang-mede/600/600', 75),

    ('BTK-001', 'kemeja-batik-tulis-sogan', 'Kemeja Batik Tulis Sogan',
     'Batik tulis tangan dengan pewarna alami sogan. Bahan katun primisima, adem dan tidak mudah kusut.',
     CAST(34500000 AS BIGINT), 'batik-fashion', 'https://picsum.photos/seed/batik-sogan/600/600', 18),

    ('BTK-002', 'selendang-batik-parang', 'Selendang Batik Parang',
     'Selendang batik cap motif parang, panjang 200 cm. Bisa dipakai formal maupun kasual.',
     CAST(18500000 AS BIGINT), 'batik-fashion', 'https://picsum.photos/seed/batik-parang/600/600', 25),

    ('ELK-001', 'tws-shopflow-buds-pro', 'TWS ShopFlow Buds Pro',
     'Earbuds dengan active noise cancelling 35dB, baterai 8 jam + 24 jam dari case, Bluetooth 5.3, IPX5.',
     CAST(74900000 AS BIGINT), 'elektronik', 'https://picsum.photos/seed/tws-buds/600/600', 40),

    ('ELK-002', 'powerbank-20000mah-pd', 'Power Bank 20.000mAh PD 30W',
     'Power bank dengan Power Delivery 30W, bisa mengisi laptop ringan. Dua port USB-A dan satu USB-C.',
     CAST(38500000 AS BIGINT), 'elektronik', 'https://picsum.photos/seed/powerbank/600/600', 55),

    ('ELK-003', 'keyboard-mekanik-65', 'Keyboard Mekanik 65% Hot-Swap',
     'Keyboard mekanik layout 65%, switch hot-swap, keycap PBT double-shot, koneksi USB-C dan Bluetooth.',
     CAST(92500000 AS BIGINT), 'elektronik', 'https://picsum.photos/seed/keyboard-65/600/600', 22),

    ('PRW-001', 'sabun-madu-hutan-100g', 'Sabun Madu Hutan 100g',
     'Sabun cold process dengan madu hutan dan minyak kelapa. Cocok untuk kulit sensitif.',
     CAST(4500000 AS BIGINT), 'perawatan-diri', 'https://picsum.photos/seed/sabun-madu/600/600', 90),

    ('PRW-002', 'serum-vitamin-c-20ml', 'Serum Vitamin C 20ml',
     'Serum dengan 10% ascorbic acid stabil dan ferulic acid. Membantu mencerahkan dan meratakan warna kulit.',
     CAST(13500000 AS BIGINT), 'perawatan-diri', 'https://picsum.photos/seed/serum-vitc/600/600', 64),

    ('OLH-001', 'matras-yoga-tpe-6mm', 'Matras Yoga TPE 6mm',
     'Matras yoga dua sisi, anti-slip, bebas lateks. Tebal 6mm, nyaman untuk sendi.',
     CAST(21500000 AS BIGINT), 'olahraga', 'https://picsum.photos/seed/matras-yoga/600/600', 38),

    ('OLH-002', 'sepatu-lari-road-runner', 'Sepatu Lari Road Runner',
     'Midsole EVA responsif dengan drop 8mm. Upper mesh breathable, berat 245g (size 42).',
     CAST(68900000 AS BIGINT), 'olahraga', 'https://picsum.photos/seed/sepatu-lari/600/600', 30),

    ('RMH-001', 'tumbler-stainless-600ml', 'Tumbler Stainless 600ml',
     'Tumbler dinding ganda, menahan suhu panas 12 jam dan dingin 18 jam. Bebas BPA.',
     CAST(16500000 AS BIGINT), 'rumah-tangga', 'https://picsum.photos/seed/tumbler/600/600', 110),

    ('RMH-002', 'lilin-aromaterapi-sandalwood', 'Lilin Aromaterapi Sandalwood',
     'Lilin soy wax dengan minyak esensial sandalwood, waktu bakar sekitar 40 jam.',
     CAST(12500000 AS BIGINT), 'rumah-tangga', 'https://picsum.photos/seed/lilin-aroma/600/600', 44),

    ('KRJ-001', 'anyaman-bambu-serbaguna', 'Keranjang Anyaman Bambu Serbaguna',
     'Anyaman bambu tangan dari pengrajin Tasikmalaya. Kuat untuk penyimpanan atau dekorasi.',
     CAST(9800000 AS BIGINT), 'kerajinan', 'https://picsum.photos/seed/anyaman-bambu/600/600', 27),

    ('KRJ-002', 'wayang-kulit-miniatur', 'Wayang Kulit Miniatur Dekoratif',
     'Miniatur wayang kulit dengan pewarna alami, dipasang pada rangka kayu. Cocok untuk hiasan dinding.',
     CAST(27500000 AS BIGINT), 'kerajinan', 'https://picsum.photos/seed/wayang-mini/600/600', 12)
) AS v (sku, slug, name, description, price_cents, category_slug, image_url, stock_quantity)
JOIN categories c ON c.slug = v.category_slug;

-- Satu produk sengaja dibuat habis stok untuk menguji tampilan "stok habis" di UI.
UPDATE products SET stock_quantity = 0 WHERE sku = 'KRJ-002';
