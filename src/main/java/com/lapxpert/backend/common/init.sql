-- =====================================================
-- DROP EXISTING TABLES (theo thứ tự phụ thuộc)
-- =====================================================
DROP TABLE IF EXISTS hoa_don_phieu_giam_gia CASCADE;
DROP TABLE IF EXISTS danh_gia CASCADE;
DROP TABLE IF EXISTS thanh_toan CASCADE;
DROP TABLE IF EXISTS hoa_don_chi_tiet CASCADE;
DROP TABLE IF EXISTS hoa_don CASCADE;
DROP TABLE IF EXISTS gio_hang_chi_tiet CASCADE;
DROP TABLE IF EXISTS gio_hang CASCADE;
DROP TABLE IF EXISTS san_pham_chi_tiet_dot_giam_gia CASCADE;
DROP TABLE IF EXISTS san_pham_chi_tiet CASCADE;
DROP TABLE IF EXISTS san_pham_danh_muc CASCADE;
DROP TABLE IF EXISTS san_pham CASCADE;
DROP TABLE IF EXISTS dot_giam_gia CASCADE;
DROP TABLE IF EXISTS phieu_giam_gia CASCADE;
DROP TABLE IF EXISTS dia_chi CASCADE;
DROP TABLE IF EXISTS wishlist_item CASCADE;
DROP TABLE IF EXISTS wishlist CASCADE;
DROP TABLE IF EXISTS danh_muc CASCADE;
DROP TABLE IF EXISTS nguoi_dung CASCADE;

-- =====================================================
-- ENUMS
-- =====================================================
DROP TYPE IF EXISTS gioi_tinh_enum CASCADE;
CREATE TYPE gioi_tinh_enum AS ENUM ('NAM','NU');

DROP TYPE IF EXISTS vai_tro_enum CASCADE;
CREATE TYPE vai_tro_enum AS ENUM ('ADMIN','STAFF','CUSTOMER','GUEST');

DROP TYPE IF EXISTS loai_phieu_giam_gia_enum CASCADE;
CREATE TYPE loai_phieu_giam_gia_enum AS ENUM ('PERCENTAGE','FIXED_AMOUNT');

DROP TYPE IF EXISTS trang_thai_giao_hang_enum CASCADE;
CREATE TYPE trang_thai_giao_hang_enum AS ENUM (
  'DANG_XU_LY',
  'CHO_XAC_NHAN',
  'DA_XAC_NHAN',
  'DANG_DONG_GOI',
  'DANG_GIAO_HANG',
  'DA_GIAO_HANG',
  'HOAN_THANH',
  'DA_HUY',
  'YEU_CAU_TRA_HANG',
  'DA_TRA_HANG'
);

DROP TYPE IF EXISTS trang_thai_giao_dich_enum CASCADE;
CREATE TYPE trang_thai_giao_dich_enum AS ENUM (
  'DA_THANH_TOAN',
  'CHUA_THANH_TOAN',
  'DA_HUY',
  'THAT_BAI',
  'CHO_XU_LY',
  'DANG_XU_LY',
  'HOAN_TIEN'
);

DROP TYPE IF EXISTS phuong_thuc_thanh_toan_enum CASCADE;
CREATE TYPE phuong_thuc_thanh_toan_enum AS ENUM (
  'TIEN_MAT',
  'CHUYEN_KHOAN',
  'THE_TIN_DUNG',
  'THE_GHI_NO',
  'VISA',
  'MASTERCARD',
  'PAYPAL',
  'MOMO',
  'ZALO_PAY',
  'VNPAY_QR',
  'COD'
);

-- =====================================================
-- TABLES
-- =====================================================

-- 1. Table: nguoi_dung (Users)
CREATE TABLE nguoi_dung
(
    id            BIGSERIAL PRIMARY KEY,
    ma_nguoi_dung VARCHAR(50) UNIQUE,
    avatar        VARCHAR(512),
    ho_ten        VARCHAR(255) NOT NULL,
    gioi_tinh     gioi_tinh_enum,
    ngay_sinh     DATE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    so_dien_thoai VARCHAR(20) UNIQUE,
    mat_khau      VARCHAR(255) NOT NULL,
    vai_tro       vai_tro_enum NOT NULL DEFAULT 'CUSTOMER',
    trang_thai    BOOLEAN      NOT NULL DEFAULT TRUE,
    ngay_tao      TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_nguoi_dung_email ON nguoi_dung (email);
CREATE INDEX idx_nguoi_dung_so_dien_thoai ON nguoi_dung (so_dien_thoai);
CREATE INDEX idx_nguoi_dung_vai_tro ON nguoi_dung (vai_tro);

-- 2. Table: danh_muc (Product Categories)
CREATE TABLE danh_muc
(
    id            BIGSERIAL PRIMARY KEY,
    ma_danh_muc   VARCHAR(50)  NOT NULL UNIQUE,
    ten_danh_muc  VARCHAR(255) NOT NULL,
    ngay_tao      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_danh_muc_ten ON danh_muc (ten_danh_muc);

-- 3. Table: dia_chi (User Addresses)
CREATE TABLE dia_chi
(
    id                       BIGSERIAL PRIMARY KEY,
    nguoi_dung_id            BIGINT       NOT NULL,
    duong                    VARCHAR(255) NOT NULL,
    phuong_xa                VARCHAR(100) NOT NULL,
    quan_huyen               VARCHAR(100) NOT NULL,
    tinh_thanh               VARCHAR(100) NOT NULL,
    quoc_gia                 VARCHAR(100)          DEFAULT 'Việt Nam',
    loai_dia_chi             VARCHAR(50),
    la_mac_dinh              BOOLEAN      NOT NULL DEFAULT FALSE,
    ngay_tao                 TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat            TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE dia_chi
    ADD CONSTRAINT fk_dia_chi_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung (id);
-- Partial unique index: chỉ cho phép 1 địa chỉ mặc định per user
CREATE UNIQUE INDEX idx_dia_chi_nguoi_dung_mac_dinh ON dia_chi (nguoi_dung_id) WHERE la_mac_dinh = TRUE;

-- 4. Table: san_pham (Products - SPU)
CREATE TABLE san_pham
(
    id            BIGSERIAL PRIMARY KEY,
    ma_san_pham   VARCHAR(100) NOT NULL UNIQUE,
    ten_san_pham  VARCHAR(255) NOT NULL,
    thuong_hieu   VARCHAR(100),
    mo_ta         TEXT,
    hinh_anh      JSONB,
    ngay_ra_mat   DATE,
    trang_thai    BOOLEAN      NOT NULL DEFAULT TRUE,
    ngay_tao      TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ           DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_san_pham_ten ON san_pham (ten_san_pham);
CREATE INDEX idx_san_pham_thuong_hieu ON san_pham (thuong_hieu);
CREATE INDEX idx_san_pham_trang_thai ON san_pham (trang_thai);

-- 5. Table: san_pham_danh_muc (Product-Category Mapping)
CREATE TABLE san_pham_danh_muc
(
    san_pham_id BIGINT NOT NULL,
    danh_muc_id BIGINT NOT NULL,
    PRIMARY KEY (san_pham_id, danh_muc_id)
);
CREATE INDEX idx_san_pham_danh_muc_danh_muc ON san_pham_danh_muc (danh_muc_id);
ALTER TABLE san_pham_danh_muc
    ADD CONSTRAINT fk_san_pham_danh_muc_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham (id)
        ON DELETE CASCADE;
ALTER TABLE san_pham_danh_muc
    ADD CONSTRAINT fk_san_pham_danh_muc_danh_muc FOREIGN KEY (danh_muc_id) REFERENCES danh_muc (id)
        ON DELETE CASCADE;

-- 6. Table: dot_giam_gia (Discount Campaigns)
CREATE TABLE dot_giam_gia
(
    id               BIGSERIAL PRIMARY KEY,
    ma_dot_giam_gia  VARCHAR(50)   NOT NULL UNIQUE,
    ten_dot_giam_gia VARCHAR(255)  NOT NULL,
    phan_tram_giam   DECIMAL(5, 2) NOT NULL,
    ngay_bat_dau     TIMESTAMPTZ   NOT NULL,
    ngay_ket_thuc    TIMESTAMPTZ   NOT NULL,
    trang_thai       BOOLEAN       NOT NULL DEFAULT TRUE,
    ngay_tao         TIMESTAMPTZ            DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat    TIMESTAMPTZ            DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dot_giam_gia_dates CHECK (ngay_ket_thuc > ngay_bat_dau)
);
CREATE INDEX idx_dot_giam_gia_ngay ON dot_giam_gia (ngay_bat_dau, ngay_ket_thuc);
CREATE INDEX idx_dot_giam_gia_trang_thai ON dot_giam_gia (trang_thai);

-- 7. Table: phieu_giam_gia (Vouchers / Coupon Codes)
CREATE TABLE phieu_giam_gia
(
    id                         BIGSERIAL PRIMARY KEY,
    ma_phieu_giam_gia          VARCHAR(50)              NOT NULL UNIQUE,
    loai_phieu_giam_gia        loai_phieu_giam_gia_enum NOT NULL,
    gia_tri_giam               DECIMAL(15, 2)           NOT NULL,
    gia_tri_don_hang_toi_thieu DECIMAL(15, 2)                    DEFAULT 0,
    ngay_bat_dau               TIMESTAMPTZ              NOT NULL,
    ngay_ket_thuc              TIMESTAMPTZ              NOT NULL,
    mo_ta                      TEXT,
    so_luong_ban_dau           INT,
    so_luong_da_dung           INT                               DEFAULT 0,
    trang_thai                 BOOLEAN                  NOT NULL DEFAULT TRUE,
    ngay_tao                   TIMESTAMPTZ                       DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat              TIMESTAMPTZ                       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_phieu_giam_gia_dates CHECK (ngay_ket_thuc > ngay_bat_dau)
);
CREATE INDEX idx_phieu_giam_gia_ngay ON phieu_giam_gia (ngay_bat_dau, ngay_ket_thuc);
CREATE INDEX idx_phieu_giam_gia_trang_thai ON phieu_giam_gia (trang_thai);

-- 8. Table: san_pham_chi_tiet (Product Variants - SKUs)
CREATE TABLE san_pham_chi_tiet
(
    id               BIGSERIAL PRIMARY KEY,
    san_pham_id      BIGINT         NOT NULL,
    sku              VARCHAR(100)   NOT NULL UNIQUE,
    mau_sac          VARCHAR(50),
    so_luong_ton_kho INT            NOT NULL DEFAULT 0,
    gia_ban          DECIMAL(15, 2) NOT NULL,
    gia_khuyen_mai   DECIMAL(15, 2),
    thuoc_tinh       JSONB,
    hinh_anh         JSONB,
    trang_thai       BOOLEAN        NOT NULL DEFAULT TRUE,
    ngay_tao         TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat    TIMESTAMPTZ             DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_san_pham_chi_tiet_san_pham_id ON san_pham_chi_tiet (san_pham_id);
CREATE INDEX idx_san_pham_chi_tiet_sku ON san_pham_chi_tiet (sku);
CREATE INDEX idx_san_pham_chi_tiet_trang_thai ON san_pham_chi_tiet (trang_thai);
ALTER TABLE san_pham_chi_tiet
    ADD CONSTRAINT fk_san_pham_chi_tiet_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham (id)
        ON DELETE CASCADE;

-- 9. Table: san_pham_chi_tiet_dot_giam_gia (SKU - Discount Campaign Mapping)
CREATE TABLE san_pham_chi_tiet_dot_giam_gia
(
    san_pham_chi_tiet_id BIGINT NOT NULL,
    dot_giam_gia_id      BIGINT NOT NULL,
    PRIMARY KEY (san_pham_chi_tiet_id, dot_giam_gia_id)
);
CREATE INDEX idx_spctdg_dot_giam_gia_id ON san_pham_chi_tiet_dot_giam_gia (dot_giam_gia_id);
ALTER TABLE san_pham_chi_tiet_dot_giam_gia
    ADD CONSTRAINT fk_spctdg_san_pham_chi_tiet FOREIGN KEY (san_pham_chi_tiet_id) REFERENCES san_pham_chi_tiet (id)
        ON DELETE CASCADE;
ALTER TABLE san_pham_chi_tiet_dot_giam_gia
    ADD CONSTRAINT fk_spctdg_dot_giam_gia FOREIGN KEY (dot_giam_gia_id) REFERENCES dot_giam_gia (id)
        ON DELETE CASCADE;

-- 10. Table: gio_hang (Shopping Cart)
CREATE TABLE gio_hang
(
    id            BIGSERIAL PRIMARY KEY,
    nguoi_dung_id BIGINT  NOT NULL UNIQUE,
    trang_thai    BOOLEAN NOT NULL DEFAULT TRUE,
    ngay_tao      TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ      DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE gio_hang
    ADD CONSTRAINT fk_gio_hang_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung (id);

-- 11. Table: gio_hang_chi_tiet (Cart Items)
CREATE TABLE gio_hang_chi_tiet
(
    id                   BIGSERIAL PRIMARY KEY,
    gio_hang_id          BIGINT NOT NULL,
    san_pham_chi_tiet_id BIGINT NOT NULL,
    so_luong             INT    NOT NULL CHECK (so_luong > 0),
    ngay_them            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_gio_hang_chi_tiet ON gio_hang_chi_tiet (gio_hang_id, san_pham_chi_tiet_id);
CREATE INDEX idx_gio_hang_chi_tiet_gio_hang_id ON gio_hang_chi_tiet (gio_hang_id);
CREATE INDEX idx_gio_hang_chi_tiet_spct_id ON gio_hang_chi_tiet (san_pham_chi_tiet_id);
ALTER TABLE gio_hang_chi_tiet
    ADD CONSTRAINT fk_gio_hang_chi_tiet_gio_hang FOREIGN KEY (gio_hang_id) REFERENCES gio_hang (id)
        ON DELETE CASCADE;
ALTER TABLE gio_hang_chi_tiet
    ADD CONSTRAINT fk_gio_hang_chi_tiet_spct FOREIGN KEY (san_pham_chi_tiet_id) REFERENCES san_pham_chi_tiet (id)
        ON DELETE CASCADE;

-- 12. Table: hoa_don (Orders)
CREATE TABLE hoa_don
(
    id                              BIGSERIAL PRIMARY KEY,
    ma_hoa_don                      VARCHAR(50)               NOT NULL UNIQUE,
    khach_hang_id                   BIGINT                    NOT NULL,
    nhan_vien_id                    BIGINT,
    dia_chi_giao_hang_day_du        TEXT                      NOT NULL,
    dia_chi_giao_hang_ho_ten        VARCHAR(255),
    dia_chi_giao_hang_so_dien_thoai VARCHAR(20),
    dia_chi_giao_hang_duong         VARCHAR(255),
    dia_chi_giao_hang_phuong_xa     VARCHAR(100),
    dia_chi_giao_hang_quan_huyen    VARCHAR(100),
    dia_chi_giao_hang_tinh_thanh    VARCHAR(100),
    gia_tri_san_pham                DECIMAL(15, 2)            NOT NULL DEFAULT 0,
    gia_tri_giam_gia_voucher        DECIMAL(15, 2)                     DEFAULT 0,
    gia_tri_giam_gia_dot_giam       DECIMAL(15, 2)                     DEFAULT 0,
    phi_van_chuyen                  DECIMAL(15, 2)                     DEFAULT 0,
    tong_thanh_toan                 DECIMAL(15, 2)            NOT NULL DEFAULT 0,
    trang_thai_giao_hang            trang_thai_giao_hang_enum NOT NULL DEFAULT 'DANG_XU_LY',
    ma_van_don                      VARCHAR(100),
    ghi_chu_khach_hang              TEXT,
    ghi_chu_cua_hang                TEXT,
    ngay_tao                        TIMESTAMPTZ                        DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat                   TIMESTAMPTZ                        DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_hoa_don_khach_hang_id ON hoa_don (khach_hang_id);
CREATE INDEX idx_hoa_don_nhan_vien_id ON hoa_don (nhan_vien_id);
CREATE INDEX idx_hoa_don_trang_thai_giao_hang ON hoa_don (trang_thai_giao_hang);
CREATE INDEX idx_hoa_don_ngay_tao ON hoa_don (ngay_tao);
ALTER TABLE hoa_don
    ADD CONSTRAINT fk_hoa_don_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES nguoi_dung (id);
ALTER TABLE hoa_don
    ADD CONSTRAINT fk_hoa_don_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nguoi_dung (id);

-- 13. Table: hoa_don_chi_tiet (Order Line Items)
CREATE TABLE hoa_don_chi_tiet
(
    id                    BIGSERIAL PRIMARY KEY,
    hoa_don_id            BIGINT         NOT NULL,
    san_pham_chi_tiet_id  BIGINT         NOT NULL,
    so_luong              INT            NOT NULL CHECK (so_luong > 0),
    gia_goc               DECIMAL(15, 2) NOT NULL,
    gia_ban               DECIMAL(15, 2) NOT NULL,
    thanh_tien            DECIMAL(15, 2) NOT NULL,
    ten_san_pham_snapshot VARCHAR(255),
    sku_snapshot          VARCHAR(100),
    hinh_anh_snapshot     VARCHAR(512)
);
CREATE INDEX idx_hoa_don_chi_tiet_hoa_don_id ON hoa_don_chi_tiet (hoa_don_id);
CREATE INDEX idx_hoa_don_chi_tiet_spct_id ON hoa_don_chi_tiet (san_pham_chi_tiet_id);
ALTER TABLE hoa_don_chi_tiet
    ADD CONSTRAINT fk_hoa_don_chi_tiet_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES hoa_don (id)
        ON DELETE CASCADE;
ALTER TABLE hoa_don_chi_tiet
    ADD CONSTRAINT fk_hoa_don_chi_tiet_spct FOREIGN KEY (san_pham_chi_tiet_id) REFERENCES san_pham_chi_tiet (id)
        ON DELETE CASCADE;

-- 14. Table: hoa_don_phieu_giam_gia (Order - Voucher Mapping)
CREATE TABLE hoa_don_phieu_giam_gia
(
    hoa_don_id        BIGINT         NOT NULL,
    phieu_giam_gia_id BIGINT         NOT NULL,
    gia_tri_da_giam   DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (hoa_don_id, phieu_giam_gia_id)
);
CREATE INDEX idx_hoa_don_phieu_giam_gia_phieu_giam_gia_id ON hoa_don_phieu_giam_gia (phieu_giam_gia_id);
ALTER TABLE hoa_don_phieu_giam_gia
    ADD CONSTRAINT fk_hdpg_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES hoa_don (id)
        ON DELETE CASCADE;
ALTER TABLE hoa_don_phieu_giam_gia
    ADD CONSTRAINT fk_hdpg_phieu_giam_gia FOREIGN KEY (phieu_giam_gia_id) REFERENCES phieu_giam_gia (id)
        ON DELETE CASCADE;

-- 15. Table: thanh_toan (Payment Transactions)
CREATE TABLE thanh_toan
(
    id                     BIGSERIAL PRIMARY KEY,
    hoa_don_id             BIGINT                      NOT NULL,
    nguoi_dung_id          BIGINT                      NOT NULL,
    trang_thai_giao_dich   trang_thai_giao_dich_enum   NOT NULL DEFAULT 'CHO_XU_LY',
    phuong_thuc_thanh_toan phuong_thuc_thanh_toan_enum NOT NULL,
    ma_giao_dich           VARCHAR(255) UNIQUE,
    gia_tri                DECIMAL(15, 2)              NOT NULL,
    thoi_gian_thanh_toan   TIMESTAMPTZ,
    ghi_chu                TEXT,
    ngay_tao               TIMESTAMPTZ                          DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_thanh_toan_hoa_don_id ON thanh_toan (hoa_don_id);
CREATE INDEX idx_thanh_toan_nguoi_dung_id ON thanh_toan (nguoi_dung_id);
CREATE INDEX idx_thanh_toan_trang_thai ON thanh_toan (trang_thai_giao_dich);
CREATE INDEX idx_thanh_toan_phuong_thuc ON thanh_toan (phuong_thuc_thanh_toan);
CREATE INDEX idx_thanh_toan_ma_giao_dich ON thanh_toan (ma_giao_dich);
ALTER TABLE thanh_toan
    ADD CONSTRAINT fk_thanh_toan_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES hoa_don (id)
        ON DELETE CASCADE;
ALTER TABLE thanh_toan
    ADD CONSTRAINT fk_thanh_toan_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung (id);

-- 16. Table: danh_gia (Product Reviews)
CREATE TABLE danh_gia
(
    id                   BIGSERIAL PRIMARY KEY,
    san_pham_chi_tiet_id BIGINT      NOT NULL,
    nguoi_dung_id        BIGINT      NOT NULL,
    hoa_don_chi_tiet_id  BIGINT UNIQUE,
    so_sao               INT         NOT NULL,
    noi_dung             TEXT,
    hinh_anh             JSONB,
    trang_thai           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ngay_tao             TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat        TIMESTAMPTZ          DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_danh_gia_user_product ON danh_gia (san_pham_chi_tiet_id, nguoi_dung_id);
CREATE INDEX idx_danh_gia_spct_id ON danh_gia (san_pham_chi_tiet_id);
CREATE INDEX idx_danh_gia_nguoi_dung_id ON danh_gia (nguoi_dung_id);
CREATE INDEX idx_danh_gia_trang_thai ON danh_gia (trang_thai);
CREATE INDEX idx_danh_gia_so_sao ON danh_gia (so_sao);
ALTER TABLE danh_gia
    ADD CONSTRAINT fk_danh_gia_spct FOREIGN KEY (san_pham_chi_tiet_id) REFERENCES san_pham_chi_tiet (id)
        ON DELETE CASCADE;
ALTER TABLE danh_gia
    ADD CONSTRAINT fk_danh_gia_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung (id);

-- 17. Table: wishlist (User Wishlist)
CREATE TABLE wishlist
(
    id            BIGSERIAL PRIMARY KEY,
    nguoi_dung_id BIGINT NOT NULL UNIQUE,
    ngay_tao      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE wishlist
    ADD CONSTRAINT fk_wishlist_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung (id);

-- 18. Table: wishlist_item (Wishlist Items)
CREATE TABLE wishlist_item
(
    id          BIGSERIAL PRIMARY KEY,
    wishlist_id BIGINT NOT NULL,
    san_pham_id BIGINT NOT NULL,
    ngay_them   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_wishlist_item ON wishlist_item (wishlist_id, san_pham_id);
CREATE INDEX idx_wishlist_item_wishlist_id ON wishlist_item (wishlist_id);
CREATE INDEX idx_wishlist_item_san_pham_id ON wishlist_item (san_pham_id);
ALTER TABLE wishlist_item
    ADD CONSTRAINT fk_wishlist_item_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlist (id)
        ON DELETE CASCADE;
ALTER TABLE wishlist_item
    ADD CONSTRAINT fk_wishlist_item_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham (id);

-- =====================================================
-- INSERT SAMPLE DATA
-- =====================================================

-- Insert sample user
INSERT INTO nguoi_dung (ma_nguoi_dung, avatar, ho_ten, gioi_tinh, ngay_sinh, email, so_dien_thoai, mat_khau, vai_tro,
                        trang_thai)
VALUES ('CUS001', 'avatar1.jpg', 'Nguyen Van A', 'NAM', '1990-01-01', 'nguyenvana@example.com', '0123456789',
        'hashedpassword', 'CUSTOMER', TRUE);

-- Insert sample category
INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc)
VALUES ('DM001', 'Laptop');

-- Insert sample address for user (nguoi_dung id = 1)
INSERT INTO dia_chi (nguoi_dung_id, ho_ten_nguoi_nhan, so_dien_thoai_nguoi_nhan, duong, phuong_xa, quan_huyen,
                     tinh_thanh, la_mac_dinh)
VALUES (1, 'Nguyen Van A', '0123456789', '12A Nguyen Trai', 'Ben Thanh', '1', 'HCM', TRUE);

-- Insert sample product (SPU)
INSERT INTO san_pham (ma_san_pham, ten_san_pham, thuong_hieu, mo_ta, hinh_anh, ngay_ra_mat, trang_thai)
VALUES ('SP001', 'Laptop Lenovo Ideapad 3', 'Lenovo', 'Laptop hiệu năng tốt cho công việc',
        '["image1.jpg", "image2.jpg"]', '2025-03-01', TRUE);

-- Insert product-category mapping (assuming san_pham id = 1, danh_muc id = 1)
INSERT INTO san_pham_danh_muc (san_pham_id, danh_muc_id)
VALUES (1, 1);

-- Insert sample discount campaign
INSERT INTO dot_giam_gia (ma_dot_giam_gia, ten_dot_giam_gia, phan_tram_giam, ngay_bat_dau, ngay_ket_thuc, trang_thai)
VALUES ('DG001', 'Mua sắm mùa hè', 15.00, '2025-06-01', '2025-06-30', TRUE);

-- Insert sample voucher
INSERT INTO phieu_giam_gia (ma_phieu_giam_gia, loai_phieu_giam_gia, gia_tri_giam, gia_tri_don_hang_toi_thieu,
                            ngay_bat_dau, ngay_ket_thuc, mo_ta, so_luong_ban_dau)
VALUES ('PGG001', 'PERCENTAGE', 10.00, 1000000.00, '2025-07-01', '2025-07-31', 'Voucher giảm 10%', 100);

-- Insert sample product detail / SKU
INSERT INTO san_pham_chi_tiet (san_pham_id, sku, mau_sac, so_luong_ton_kho, gia_ban, thuoc_tinh, hinh_anh, trang_thai)
VALUES (1, 'SKU001', 'Blue', 50, 15000000.00, '{"cpu": "Intel i5", "ram": "8GB"}', '["spct1.jpg", "spct2.jpg"]', TRUE);

-- Insert SKU - Discount mapping (assuming san_pham_chi_tiet id = 1, dot_giam_gia id = 1)
INSERT INTO san_pham_chi_tiet_dot_giam_gia (san_pham_chi_tiet_id, dot_giam_gia_id)
VALUES (1, 1);

-- Insert sample shopping cart for user id 1
INSERT INTO gio_hang (nguoi_dung_id, trang_thai)
VALUES (1, TRUE);

-- Insert sample cart item (assuming gio_hang id = 1, san_pham_chi_tiet id = 1)
INSERT INTO gio_hang_chi_tiet (gio_hang_id, san_pham_chi_tiet_id, so_luong)
VALUES (1, 1, 2);

-- Insert sample order
INSERT INTO hoa_don (ma_hoa_don, khach_hang_id, dia_chi_giao_hang_day_du, gia_tri_san_pham, tong_thanh_toan,
                     trang_thai_giao_hang)
VALUES ('HD001', 1, '12A Nguyen Trai, HCM', 15000000.00, 14000000.00, 'DANG_XU_LY');

-- Insert sample order line item (assuming hoa_don id = 1, san_pham_chi_tiet id = 1)
INSERT INTO hoa_don_chi_tiet (hoa_don_id, san_pham_chi_tiet_id, so_luong, gia_goc, gia_ban, thanh_tien,
                              ten_san_pham_snapshot, sku_snapshot)
VALUES (1, 1, 1, 15000000.00, 14000000.00, 14000000.00, 'Laptop Lenovo Ideapad 3', 'SKU001');

-- Insert sample order-voucher mapping (assuming hoa_don id = 1, phieu_giam_gia id = 1)
INSERT INTO hoa_don_phieu_giam_gia (hoa_don_id, phieu_giam_gia_id, gia_tri_da_giam)
VALUES (1, 1, 1000000.00);

-- Insert sample payment transaction
INSERT INTO thanh_toan (hoa_don_id, nguoi_dung_id, trang_thai_giao_dich, phuong_thuc_thanh_toan, ma_giao_dich, gia_tri)
VALUES (1, 1, 'DA_THANH_TOAN', 'VISA', 'TXN001', 14000000.00);

-- Insert sample product review
INSERT INTO danh_gia (san_pham_chi_tiet_id, nguoi_dung_id, so_sao, noi_dung, trang_thai)
VALUES (1, 1, 5, 'Sản phẩm tuyệt vời!', 'APPROVED');

-- Insert sample wishlist for user id 1
INSERT INTO wishlist (nguoi_dung_id)
VALUES (1);

-- Insert sample wishlist item (assuming wishlist id = 1, san_pham id = 1)
INSERT INTO wishlist_item (wishlist_id, san_pham_id)
VALUES (1, 1);
-- Insert sample order - payment mapping (assuming hoa_don id = 1, thanh_toan id = 1)