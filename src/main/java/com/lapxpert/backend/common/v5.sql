-- =============================================================================
-- Script tạo Schema cho Dự án E-commerce PostgreSQL
-- Version 5: Dựa trên DBML cuối cùng và các góp ý
-- =============================================================================

-- (Tùy chọn) Xóa các đối tượng cũ nếu tồn tại (Cẩn thận!)
BEGIN;
DROP FUNCTION IF EXISTS trigger_set_timestamp() CASCADE;
DROP TABLE IF EXISTS phieu_giam_gia_nguoi_dung CASCADE;
DROP TABLE IF EXISTS phieu_giam_gia_thanh_toan CASCADE;
DROP TABLE IF EXISTS hoa_don_thanh_toan CASCADE;
DROP TABLE IF EXISTS wishlist_item CASCADE;
DROP TABLE IF EXISTS wishlist CASCADE;
DROP TABLE IF EXISTS danh_gia CASCADE;
DROP TABLE IF EXISTS thanh_toan CASCADE;
DROP TABLE IF EXISTS hoa_don_phieu_giam_gia CASCADE;
DROP TABLE IF EXISTS hoa_don_chi_tiet CASCADE;
DROP TABLE IF EXISTS hoa_don CASCADE;
DROP TABLE IF EXISTS gio_hang_chi_tiet CASCADE;
DROP TABLE IF EXISTS gio_hang CASCADE;
DROP TABLE IF EXISTS san_pham_chi_tiet_dot_giam_gia CASCADE;
DROP TABLE IF EXISTS san_pham_chi_tiet CASCADE;
DROP TABLE IF EXISTS san_pham_danh_muc CASCADE;
DROP TABLE IF EXISTS san_pham CASCADE;
DROP TABLE IF EXISTS thuong_hieu CASCADE; -- Added
DROP TABLE IF EXISTS phieu_giam_gia CASCADE;
DROP TABLE IF EXISTS dot_giam_gia CASCADE;
DROP TABLE IF EXISTS dia_chi CASCADE;
DROP TABLE IF EXISTS nguoi_dung CASCADE;
DROP TABLE IF EXISTS danh_muc CASCADE;
-- Drop attribute tables
DROP TABLE IF EXISTS cpu CASCADE;
DROP TABLE IF EXISTS ram CASCADE;
DROP TABLE IF EXISTS o_cung CASCADE;
DROP TABLE IF EXISTS gpu CASCADE;
DROP TABLE IF EXISTS man_hinh CASCADE;
DROP TABLE IF EXISTS cong_giao_tiep CASCADE;
DROP TABLE IF EXISTS ban_phim CASCADE;
DROP TABLE IF EXISTS ket_noi_mang CASCADE;
DROP TABLE IF EXISTS am_thanh CASCADE;
DROP TABLE IF EXISTS webcam CASCADE;
DROP TABLE IF EXISTS bao_mat CASCADE;
DROP TABLE IF EXISTS he_dieu_hanh CASCADE;
DROP TABLE IF EXISTS pin CASCADE;
DROP TABLE IF EXISTS thiet_ke CASCADE;
-- Drop enums
DROP TYPE IF EXISTS trang_thai_phieu_giam_gia; -- Added
DROP TYPE IF EXISTS phuong_thuc_thanh_toan_enum;
DROP TYPE IF EXISTS trang_thai_giao_dich_enum;
DROP TYPE IF EXISTS trang_thai_giao_hang_enum;
DROP TYPE IF EXISTS loai_phieu_giam_gia_enum;
DROP TYPE IF EXISTS vai_tro_enum;
DROP TYPE IF EXISTS gioi_tinh_enum;
COMMIT;


-- =============================================================================
-- 1. TẠO ENUM TYPES
-- =============================================================================
CREATE TYPE gioi_tinh_enum AS ENUM ('NAM', 'NU');
CREATE TYPE vai_tro_enum AS ENUM ('ADMIN', 'STAFF', 'CUSTOMER', 'GUEST');
CREATE TYPE loai_phieu_giam_gia_enum AS ENUM ('PHAN_TRAM', 'GIA_TRI');
CREATE TYPE trang_thai_giao_hang_enum AS ENUM ('DANG_XU_LY', 'CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DANG_DONG_GOI', 'DANG_GIAO_HANG', 'DA_GIAO_HANG', 'HOAN_THANH', 'DA_HUY', 'YEU_CAU_TRA_HANG', 'DA_TRA_HANG');
CREATE TYPE trang_thai_giao_dich_enum AS ENUM ('DA_THANH_TOAN', 'CHUA_THANH_TOAN', 'DA_HUY', 'THAT_BAI', 'CHO_XU_LY', 'DANG_XU_LY', 'HOAN_TIEN');
CREATE TYPE phuong_thuc_thanh_toan_enum AS ENUM ('TIEN_MAT', 'CHUYEN_KHOAN', 'THE_TIN_DUNG', 'THE_GHI_NO', 'VISA', 'MASTERCARD', 'PAYPAL', 'MOMO', 'ZALO_PAY', 'VNPAY_QR', 'COD');
CREATE TYPE trang_thai_phieu_giam_gia AS ENUM ('CHUA_DIEN_RA', 'DA_DIEN_RA', 'KET_THUC'); -- New Enum

-- =============================================================================
-- 2. TẠO BẢNG THUỘC TÍNH VÀ THƯƠNG HIỆU
-- =============================================================================
CREATE TABLE cpu ( id BIGSERIAL PRIMARY KEY, mo_ta_cpu VARCHAR(255) UNIQUE NOT NULL );
COMMENT ON TABLE cpu IS 'CPU types (e.g., Intel Core i5-1340P)';
CREATE TABLE ram ( id BIGSERIAL PRIMARY KEY, mo_ta_ram VARCHAR(100) UNIQUE NOT NULL );
COMMENT ON TABLE ram IS 'RAM configurations (e.g., 16GB DDR5 5600MHz)';
CREATE TABLE o_cung ( id BIGSERIAL PRIMARY KEY, mo_ta_o_cung VARCHAR(150) UNIQUE NOT NULL );
COMMENT ON TABLE o_cung IS 'Storage configurations (e.g., 512GB NVMe Gen4 SSD)';
CREATE TABLE gpu ( id BIGSERIAL PRIMARY KEY, mo_ta_gpu VARCHAR(255) UNIQUE NOT NULL );
COMMENT ON TABLE gpu IS 'GPU types (e.g., NVIDIA GeForce RTX 4060)';
CREATE TABLE man_hinh ( id BIGSERIAL PRIMARY KEY, mo_ta_man_hinh VARCHAR(300) UNIQUE NOT NULL );
COMMENT ON TABLE man_hinh IS 'Screen configurations (e.g., 15.6 inch FHD IPS 144Hz)';
CREATE TABLE cong_giao_tiep ( id BIGSERIAL PRIMARY KEY, mo_ta_cong VARCHAR(512) UNIQUE NOT NULL );
COMMENT ON TABLE cong_giao_tiep IS 'Interface ports (e.g., USB, HDMI, RJ45)';
CREATE TABLE ban_phim ( id BIGSERIAL PRIMARY KEY, mo_ta_ban_phim VARCHAR(200) UNIQUE NOT NULL );
COMMENT ON TABLE ban_phim IS 'Keyboard types (e.g., RGB Backlit)';
CREATE TABLE ket_noi_mang ( id BIGSERIAL PRIMARY KEY, mo_ta_ket_noi VARCHAR(200) UNIQUE NOT NULL );
COMMENT ON TABLE ket_noi_mang IS 'Network connectivity (e.g., Wi-Fi 6E)';
CREATE TABLE am_thanh ( id BIGSERIAL PRIMARY KEY, mo_ta_am_thanh VARCHAR(200) UNIQUE NOT NULL );
COMMENT ON TABLE am_thanh IS 'Audio descriptions (e.g., Dolby Atmos)';
CREATE TABLE webcam ( id BIGSERIAL PRIMARY KEY, mo_ta_wc VARCHAR(200) UNIQUE NOT NULL );
COMMENT ON TABLE webcam IS 'Webcam descriptions (e.g., FHD Webcam)';
CREATE TABLE bao_mat ( id BIGSERIAL PRIMARY KEY, mo_ta_bao_mat VARCHAR(200) UNIQUE NOT NULL );
COMMENT ON TABLE bao_mat IS 'Security features (e.g., Fingerprint Reader)';
CREATE TABLE he_dieu_hanh ( id BIGSERIAL PRIMARY KEY, mo_ta_he_dieu_hanh VARCHAR(100) UNIQUE NOT NULL );
COMMENT ON TABLE he_dieu_hanh IS 'Operating Systems (e.g., Windows 11 Home)';
CREATE TABLE pin ( id BIGSERIAL PRIMARY KEY, mo_ta_pin VARCHAR(150) UNIQUE NOT NULL );
COMMENT ON TABLE pin IS 'Battery configurations (e.g., 4-cell 75Wh)';
CREATE TABLE thiet_ke ( id BIGSERIAL PRIMARY KEY, mo_ta_thiet_ke VARCHAR(300) UNIQUE NOT NULL );
COMMENT ON TABLE thiet_ke IS 'Design details (e.g., weight, dimensions, materials)';
CREATE TABLE thuong_hieu ( id BIGSERIAL PRIMARY KEY, mo_ta_thuong_hieu VARCHAR(300) UNIQUE NOT NULL ); -- Renamed from 'ten_thuong_hieu' for consistency
COMMENT ON TABLE thuong_hieu IS 'Product Brands (e.g., Dell, Apple, Logitech)';


-- =============================================================================
-- 3. TẠO CÁC BẢNG CORE
-- =============================================================================
CREATE TABLE danh_muc (
    id BIGSERIAL PRIMARY KEY,
    ma_danh_muc VARCHAR(50) UNIQUE NOT NULL,
    ten_danh_muc VARCHAR(255) NOT NULL,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE danh_muc IS 'Product Categories';

CREATE TABLE nguoi_dung (
    id BIGSERIAL PRIMARY KEY,
    ma_nguoi_dung VARCHAR(50) UNIQUE,
    avatar VARCHAR(512),
    ho_ten VARCHAR(255) NOT NULL,
    gioi_tinh gioi_tinh_enum,
    ngay_sinh DATE,
    cccd VARCHAR(12) UNIQUE, -- Changed from string, added UNIQUE, allow NULL
    email VARCHAR(255) UNIQUE NOT NULL,
    so_dien_thoai VARCHAR(20) UNIQUE,
    mat_khau VARCHAR(255) NOT NULL,
    vai_tro vai_tro_enum NOT NULL DEFAULT 'CUSTOMER',
    trang_thai BOOLEAN DEFAULT TRUE NOT NULL,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE nguoi_dung IS 'Users (Admin, Staff, Customer)';
COMMENT ON COLUMN nguoi_dung.cccd IS 'Citizen Identity Card number (consider encryption/security)';

CREATE TABLE dia_chi (
    id BIGSERIAL PRIMARY KEY,
    nguoi_dung_id BIGINT NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    ho_ten_nguoi_nhan VARCHAR(255), -- Added back
    so_dien_thoai_nguoi_nhan VARCHAR(20), -- Added back
    duong VARCHAR(255) NOT NULL,
    phuong_xa VARCHAR(100) NOT NULL,
    quan_huyen VARCHAR(100) NOT NULL,
    tinh_thanh VARCHAR(100) NOT NULL,
    quoc_gia VARCHAR(100) DEFAULT 'Việt Nam',
    loai_dia_chi VARCHAR(50),
    la_mac_dinh BOOLEAN DEFAULT FALSE NOT NULL,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE dia_chi IS 'User Addresses';
COMMENT ON COLUMN dia_chi.ho_ten_nguoi_nhan IS 'Recipient name (can be different from user)';
COMMENT ON COLUMN dia_chi.so_dien_thoai_nguoi_nhan IS 'Recipient phone number';


CREATE TABLE san_pham (
    id BIGSERIAL PRIMARY KEY,
    ma_san_pham VARCHAR(100) UNIQUE NOT NULL,
    ten_san_pham VARCHAR(255) NOT NULL,
    thuong_hieu_id BIGINT REFERENCES thuong_hieu(id) ON DELETE SET NULL, -- Added FK
    mo_ta TEXT,
    hinh_anh JSONB,
    ngay_ra_mat DATE,
    trang_thai BOOLEAN DEFAULT TRUE NOT NULL,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE san_pham IS 'Products (SPU)';

CREATE TABLE san_pham_danh_muc (
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id) ON DELETE CASCADE,
    danh_muc_id BIGINT NOT NULL REFERENCES danh_muc(id) ON DELETE CASCADE,
    PRIMARY KEY (san_pham_id, danh_muc_id)
);
COMMENT ON TABLE san_pham_danh_muc IS 'Mapping between Products and Categories';

CREATE TABLE dot_giam_gia (
    id BIGSERIAL PRIMARY KEY,
    ma_dot_giam_gia VARCHAR(50) UNIQUE NOT NULL,
    ten_dot_giam_gia VARCHAR(255) NOT NULL,
    phan_tram_giam NUMERIC(5, 2) NOT NULL CHECK (phan_tram_giam >= 0 AND phan_tram_giam <= 100),
    ngay_bat_dau TIMESTAMPTZ NOT NULL,
    ngay_ket_thuc TIMESTAMPTZ NOT NULL,
    trang_thai BOOLEAN DEFAULT TRUE NOT NULL, -- Represents admin activation, not time-based status
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_ngay_dot_giam_gia CHECK (ngay_ket_thuc > ngay_bat_dau)
);
COMMENT ON TABLE dot_giam_gia IS 'Discount Campaigns';
COMMENT ON COLUMN dot_giam_gia.trang_thai IS 'Admin activation status (active/inactive)';


CREATE TABLE phieu_giam_gia (
    id BIGSERIAL PRIMARY KEY,
    ma_phieu_giam_gia VARCHAR(50) UNIQUE NOT NULL,
    loai_phieu_giam_gia loai_phieu_giam_gia_enum NOT NULL,
    gia_tri_giam NUMERIC(15, 2) NOT NULL CHECK (gia_tri_giam >= 0),
    gia_tri_don_hang_toi_thieu NUMERIC(15, 2) DEFAULT 0 CHECK (gia_tri_don_hang_toi_thieu >= 0),
    ngay_bat_dau TIMESTAMPTZ NOT NULL,
    ngay_ket_thuc TIMESTAMPTZ NOT NULL,
    mo_ta TEXT,
    phieu_rieng_tu boolean DEFAULT FALSE,
    so_luong_ban_dau INT DEFAULT 0 NOT NULL,
    so_luong_da_dung INT DEFAULT 0 NOT NULL,
    trang_thai trang_thai_phieu_giam_gia DEFAULT 'CHUA_DIEN_RA' NOT NULL, -- Use new Enum
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_ngay_phieu_giam_gia CHECK (ngay_ket_thuc > ngay_bat_dau),
    CONSTRAINT check_so_luong_phieu_giam_gia CHECK (so_luong_da_dung <= so_luong_ban_dau) -- Add if so_luong_ban_dau is always NOT NULL
);
COMMENT ON TABLE phieu_giam_gia IS 'Vouchers / Coupon Codes';
COMMENT ON COLUMN phieu_giam_gia.trang_thai IS 'Time-based status (CHUA_DIEN_RA, DA_DIEN_RA, KET_THUC) - consider auto-updating mechanism';


CREATE TABLE san_pham_chi_tiet (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id) ON DELETE CASCADE,
    sku VARCHAR(100) UNIQUE NOT NULL,
    mau_sac VARCHAR(50),
    so_luong_ton_kho INT NOT NULL DEFAULT 0 CHECK (so_luong_ton_kho >= 0),
    gia_ban NUMERIC(15, 2) NOT NULL CHECK (gia_ban >= 0),
    gia_khuyen_mai NUMERIC(15, 2),
    hinh_anh JSONB,
    trang_thai BOOLEAN DEFAULT TRUE NOT NULL,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    cpu_id BIGINT REFERENCES cpu(id) ON DELETE SET NULL,
    ram_id BIGINT REFERENCES ram(id) ON DELETE SET NULL,
    o_cung_id BIGINT REFERENCES o_cung(id) ON DELETE SET NULL,
    gpu_id BIGINT REFERENCES gpu(id) ON DELETE SET NULL,
    man_hinh_id BIGINT REFERENCES man_hinh(id) ON DELETE SET NULL,
    cong_giao_tiep_id BIGINT REFERENCES cong_giao_tiep(id) ON DELETE SET NULL,
    ban_phim_id BIGINT REFERENCES ban_phim(id) ON DELETE SET NULL,
    ket_noi_mang_id BIGINT REFERENCES ket_noi_mang(id) ON DELETE SET NULL,
    am_thanh_id BIGINT REFERENCES am_thanh(id) ON DELETE SET NULL,
    webcam_id BIGINT REFERENCES webcam(id) ON DELETE SET NULL,
    bao_mat_id BIGINT REFERENCES bao_mat(id) ON DELETE SET NULL,
    he_dieu_hanh_id BIGINT REFERENCES he_dieu_hanh(id) ON DELETE SET NULL,
    pin_id BIGINT REFERENCES pin(id) ON DELETE SET NULL,
    thiet_ke_id BIGINT REFERENCES thiet_ke(id) ON DELETE SET NULL
);
COMMENT ON TABLE san_pham_chi_tiet IS 'Product Variants (SKU) with normalized attributes';

CREATE TABLE san_pham_chi_tiet_dot_giam_gia (
    san_pham_chi_tiet_id BIGINT NOT NULL REFERENCES san_pham_chi_tiet(id) ON DELETE CASCADE,
    dot_giam_gia_id BIGINT NOT NULL REFERENCES dot_giam_gia(id) ON DELETE CASCADE,
    PRIMARY KEY (san_pham_chi_tiet_id, dot_giam_gia_id)
);
COMMENT ON TABLE san_pham_chi_tiet_dot_giam_gia IS 'Mapping between SKUs and Discount Campaigns';

CREATE TABLE gio_hang (
    id BIGSERIAL PRIMARY KEY,
    nguoi_dung_id BIGINT UNIQUE NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    trang_thai BOOLEAN DEFAULT TRUE NOT NULL,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE gio_hang IS 'Shopping Cart';

CREATE TABLE gio_hang_chi_tiet (
    id BIGSERIAL PRIMARY KEY,
    gio_hang_id BIGINT NOT NULL REFERENCES gio_hang(id) ON DELETE CASCADE,
    san_pham_chi_tiet_id BIGINT NOT NULL REFERENCES san_pham_chi_tiet(id) ON DELETE CASCADE, -- Should this be RESTRICT or SET NULL on delete?
    so_luong INT NOT NULL CHECK (so_luong > 0),
    ngay_them TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (gio_hang_id, san_pham_chi_tiet_id)
);
COMMENT ON TABLE gio_hang_chi_tiet IS 'Cart Items';

CREATE TABLE hoa_don (
    id BIGSERIAL PRIMARY KEY,
    ma_hoa_don VARCHAR(50) UNIQUE NOT NULL,
    khach_hang_id BIGINT NOT NULL REFERENCES nguoi_dung(id) ON DELETE RESTRICT,
    nhan_vien_id BIGINT REFERENCES nguoi_dung(id) ON DELETE SET NULL,
    -- dia_chi_giao_hang_day_du TEXT, -- Consider adding this snapshot back
    dia_chi_giao_hang_ho_ten VARCHAR(255),
    dia_chi_giao_hang_so_dien_thoai VARCHAR(20),
    dia_chi_giao_hang_duong VARCHAR(255),
    dia_chi_giao_hang_phuong_xa VARCHAR(100),
    dia_chi_giao_hang_quan_huyen VARCHAR(100),
    dia_chi_giao_hang_tinh_thanh VARCHAR(100),
    gia_tri_san_pham NUMERIC(15, 2) NOT NULL DEFAULT 0,
    gia_tri_giam_gia_voucher NUMERIC(15, 2) DEFAULT 0,
    gia_tri_giam_gia_dot_giam NUMERIC(15, 2) DEFAULT 0,
    phi_van_chuyen NUMERIC(15, 2) DEFAULT 0,
    tong_thanh_toan NUMERIC(15, 2) NOT NULL DEFAULT 0,
    trang_thai_giao_hang trang_thai_giao_hang_enum NOT NULL DEFAULT 'DANG_XU_LY',
    -- trang_thai_thanh_toan ENUM(...) -- Consider adding a payment status here (e.g., PENDING, PARTIAL, PAID, REFUNDED)
    ma_van_don VARCHAR(100),
    ghi_chu_khach_hang TEXT,
    ghi_chu_cua_hang TEXT,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE hoa_don IS 'Customer Orders';

CREATE TABLE hoa_don_chi_tiet (
    id BIGSERIAL PRIMARY KEY,
    hoa_don_id BIGINT NOT NULL REFERENCES hoa_don(id) ON DELETE CASCADE,
    san_pham_chi_tiet_id BIGINT NOT NULL REFERENCES san_pham_chi_tiet(id) ON DELETE RESTRICT,
    so_luong INT NOT NULL CHECK (so_luong > 0),
    gia_goc NUMERIC(15, 2) NOT NULL,
    gia_ban NUMERIC(15, 2) NOT NULL,
    thanh_tien NUMERIC(15, 2) NOT NULL,
    ten_san_pham_snapshot VARCHAR(255),
    sku_snapshot VARCHAR(100),
    hinh_anh_snapshot VARCHAR(512)
);
COMMENT ON TABLE hoa_don_chi_tiet IS 'Order Line Items (Snapshot of product details at time of order)';

CREATE TABLE hoa_don_phieu_giam_gia (
    hoa_don_id BIGINT NOT NULL REFERENCES hoa_don(id) ON DELETE CASCADE,
    phieu_giam_gia_id BIGINT NOT NULL REFERENCES phieu_giam_gia(id) ON DELETE RESTRICT, -- Prevent deleting used voucher
    gia_tri_da_giam NUMERIC(15, 2) NOT NULL CHECK (gia_tri_da_giam >= 0),
    PRIMARY KEY (hoa_don_id, phieu_giam_gia_id)
);
COMMENT ON TABLE hoa_don_phieu_giam_gia IS 'Mapping vouchers applied to orders and the amount discounted';

CREATE TABLE thanh_toan (
    id BIGSERIAL PRIMARY KEY,
    nguoi_dung_id BIGINT NOT NULL REFERENCES nguoi_dung(id) ON DELETE RESTRICT, -- Added back FK to user
    trang_thai_giao_dich trang_thai_giao_dich_enum NOT NULL DEFAULT 'CHO_XU_LY',
    phuong_thuc_thanh_toan phuong_thuc_thanh_toan_enum NOT NULL,
    ma_giao_dich VARCHAR(255) UNIQUE,
    gia_tri NUMERIC(15, 2) NOT NULL CHECK(gia_tri >= 0),
    thoi_gian_thanh_toan TIMESTAMPTZ,
    ghi_chu TEXT,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE thanh_toan IS 'User payment transactions, linked via joining tables';

CREATE TABLE danh_gia (
    id BIGSERIAL PRIMARY KEY,
    san_pham_chi_tiet_id BIGINT NOT NULL REFERENCES san_pham_chi_tiet(id) ON DELETE CASCADE,
    nguoi_dung_id BIGINT NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    hoa_don_chi_tiet_id BIGINT UNIQUE REFERENCES hoa_don_chi_tiet(id) ON DELETE SET NULL,
    so_sao INT NOT NULL CHECK (so_sao >= 1 AND so_sao <= 5),
    noi_dung TEXT,
    hinh_anh JSONB,
    trang_thai VARCHAR(20) DEFAULT 'DANG_CHO' NOT NULL CHECK (trang_thai IN ('DANG_CHO', 'DUYET', 'TU_CHOI')), -- Added CHECK constraint
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (san_pham_chi_tiet_id, nguoi_dung_id)
);
COMMENT ON TABLE danh_gia IS 'Product Reviews';

CREATE TABLE wishlist (
    id BIGSERIAL PRIMARY KEY,
    nguoi_dung_id BIGINT UNIQUE NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    ngay_tao TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE wishlist IS 'User Wishlist';

CREATE TABLE wishlist_item (
    -- id BIGSERIAL PRIMARY KEY, -- Option 1: Use surrogate key
    wishlist_id BIGINT NOT NULL REFERENCES wishlist(id) ON DELETE CASCADE,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id) ON DELETE CASCADE,
    ngay_them TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (wishlist_id, san_pham_id) -- Option 2: Use composite key (chosen)
    -- UNIQUE (wishlist_id, san_pham_id) -- Needed if using Option 1
);
COMMENT ON TABLE wishlist_item IS 'Wishlist Items (linking Wishlist to Product SPU)';


-- =============================================================================
-- 4. TẠO BẢNG TRUNG GIAN (Joining Tables)
-- =============================================================================

CREATE TABLE hoa_don_thanh_toan (
    hoa_don_id BIGINT NOT NULL REFERENCES hoa_don(id) ON DELETE CASCADE,
    thanh_toan_id BIGINT NOT NULL REFERENCES thanh_toan(id) ON DELETE CASCADE,
    so_tien_ap_dung NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (so_tien_ap_dung >= 0),
    PRIMARY KEY (hoa_don_id, thanh_toan_id)
);
COMMENT ON TABLE hoa_don_thanh_toan IS 'Many-to-Many mapping between Orders and Payments.';
COMMENT ON COLUMN hoa_don_thanh_toan.so_tien_ap_dung IS 'Amount of this payment applied to this specific order.';

CREATE TABLE phieu_giam_gia_nguoi_dung (
    phieu_giam_gia_id BIGINT NOT NULL REFERENCES phieu_giam_gia(id) ON DELETE CASCADE,
    nguoi_dung_id BIGINT NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    ngay_nhan TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, -- Example: Add timestamp when user got the voucher
    da_su_dung BOOLEAN DEFAULT FALSE NOT NULL, -- Example: Track if this specific instance used by user
    PRIMARY KEY (phieu_giam_gia_id, nguoi_dung_id) -- Composite primary key
);
COMMENT ON TABLE phieu_giam_gia_nguoi_dung IS 'Mapping which user owns/saved which voucher and its usage status for that user.';


-- =============================================================================
-- 5. TẠO INDEXES
-- =============================================================================
-- Indexes cho bảng core và thuộc tính
CREATE INDEX idx_danh_muc_ten ON danh_muc(ten_danh_muc);
CREATE INDEX idx_nguoi_dung_email ON nguoi_dung(email);
CREATE INDEX idx_nguoi_dung_so_dien_thoai ON nguoi_dung(so_dien_thoai);
CREATE INDEX idx_nguoi_dung_vai_tro ON nguoi_dung(vai_tro);
CREATE INDEX idx_dia_chi_nguoi_dung_id ON dia_chi(nguoi_dung_id);
CREATE UNIQUE INDEX idx_dia_chi_nguoi_dung_mac_dinh ON dia_chi(nguoi_dung_id) WHERE la_mac_dinh IS TRUE;
CREATE INDEX idx_san_pham_ten ON san_pham(ten_san_pham);
CREATE INDEX idx_san_pham_thuong_hieu_id ON san_pham(thuong_hieu_id); -- Index FK
CREATE INDEX idx_san_pham_trang_thai ON san_pham(trang_thai);
CREATE INDEX idx_dot_giam_gia_ngay ON dot_giam_gia(ngay_bat_dau, ngay_ket_thuc);
CREATE INDEX idx_dot_giam_gia_trang_thai ON dot_giam_gia(trang_thai);
CREATE INDEX idx_phieu_giam_gia_ngay ON phieu_giam_gia(ngay_bat_dau, ngay_ket_thuc);
CREATE INDEX idx_phieu_giam_gia_trang_thai ON phieu_giam_gia(trang_thai);
CREATE INDEX idx_spct_san_pham_id ON san_pham_chi_tiet(san_pham_id);
CREATE INDEX idx_spct_sku ON san_pham_chi_tiet(sku);
CREATE INDEX idx_spct_trang_thai ON san_pham_chi_tiet(trang_thai);
-- Consider adding indexes for frequently filtered attribute FKs if needed, e.g.:
-- CREATE INDEX idx_spct_cpu_id ON san_pham_chi_tiet(cpu_id);
-- CREATE INDEX idx_spct_ram_id ON san_pham_chi_tiet(ram_id);
CREATE INDEX idx_gio_hang_chi_tiet_gio_hang_id ON gio_hang_chi_tiet(gio_hang_id);
CREATE INDEX idx_gio_hang_chi_tiet_san_pham_chi_tiet_id ON gio_hang_chi_tiet(san_pham_chi_tiet_id);
CREATE INDEX idx_hoa_don_khach_hang_id ON hoa_don(khach_hang_id);
CREATE INDEX idx_hoa_don_nhan_vien_id ON hoa_don(nhan_vien_id);
CREATE INDEX idx_hoa_don_trang_thai_giao_hang ON hoa_don(trang_thai_giao_hang);
CREATE INDEX idx_hoa_don_ngay_tao ON hoa_don(ngay_tao);
CREATE INDEX idx_hoa_don_chi_tiet_hoa_don_id ON hoa_don_chi_tiet(hoa_don_id);
CREATE INDEX idx_hoa_don_chi_tiet_san_pham_chi_tiet_id ON hoa_don_chi_tiet(san_pham_chi_tiet_id);
CREATE INDEX idx_thanh_toan_nguoi_dung_id ON thanh_toan(nguoi_dung_id); -- Added index for FK
CREATE INDEX idx_thanh_toan_trang_thai ON thanh_toan(trang_thai_giao_dich);
CREATE INDEX idx_thanh_toan_phuong_thuc ON thanh_toan(phuong_thuc_thanh_toan);
CREATE INDEX idx_thanh_toan_ma_giao_dich ON thanh_toan(ma_giao_dich);
CREATE INDEX idx_thanh_toan_thoi_gian ON thanh_toan(thoi_gian_thanh_toan);
CREATE INDEX idx_danh_gia_spct_id ON danh_gia(san_pham_chi_tiet_id);
CREATE INDEX idx_danh_gia_nguoi_dung_id ON danh_gia(nguoi_dung_id);
CREATE INDEX idx_danh_gia_trang_thai ON danh_gia(trang_thai);
CREATE INDEX idx_danh_gia_so_sao ON danh_gia(so_sao);
CREATE INDEX idx_wishlist_item_wishlist_id ON wishlist_item(wishlist_id);
CREATE INDEX idx_wishlist_item_san_pham_id ON wishlist_item(san_pham_id);

-- Indexes cho bảng trung gian
CREATE INDEX idx_spdm_danh_muc_id ON san_pham_danh_muc(danh_muc_id);
CREATE INDEX idx_spct_dgg_dot_giam_gia_id ON san_pham_chi_tiet_dot_giam_gia(dot_giam_gia_id);
CREATE INDEX idx_hdpg_phieu_giam_gia_id ON hoa_don_phieu_giam_gia(phieu_giam_gia_id);
CREATE INDEX idx_hdtt_thanh_toan_id ON hoa_don_thanh_toan(thanh_toan_id);
CREATE INDEX idx_hdtt_hoa_don_id ON hoa_don_thanh_toan(hoa_don_id);
CREATE INDEX idx_pggnd_nguoi_dung_id ON phieu_giam_gia_nguoi_dung(nguoi_dung_id);
CREATE INDEX idx_pggnd_phieu_giam_gia_id ON phieu_giam_gia_nguoi_dung(phieu_giam_gia_id);

-- =============================================================================
-- 6. FUNCTION VÀ TRIGGERS TỰ ĐỘNG CẬP NHẬT ngay_cap_nhat
-- =============================================================================

-- CREATE OR REPLACE FUNCTION trigger_set_timestamp()
-- RETURNS TRIGGER AS $$
-- BEGIN
--   IF TG_OP = 'UPDATE' THEN
--      -- Check if 'ngay_cap_nhat' column exists in the triggering table
--      IF EXISTS (SELECT 1 FROM information_schema.columns
--                 WHERE table_schema = TG_TABLE_SCHEMA
--                   AND table_name = TG_TABLE_NAME
--                   AND column_name = 'ngay_cap_nhat')
--      THEN
--         -- Use qualified name for the column to avoid ambiguity
--         EXECUTE format('UPDATE %I.%I SET ngay_cap_nhat = NOW() WHERE ctid = $1', TG_TABLE_SCHEMA, TG_TABLE_NAME) USING NEW.ctid;
--         -- Directly modifying NEW here might not work as expected in all cases,
--         -- but for this simple assignment it's often sufficient.
--         -- A more robust way might involve re-fetching or ensuring the calling code handles it.
--         -- For simplicity, we'll try direct assignment first.
--         NEW.ngay_cap_nhat = NOW();
--      END IF;
--   END IF;
--   RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- -- Drop existing triggers before creating new ones to avoid errors on re-run
-- DO $$ BEGIN
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_danh_muc') THEN
--         DROP TRIGGER set_timestamp_danh_muc ON danh_muc;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_nguoi_dung') THEN
--         DROP TRIGGER set_timestamp_nguoi_dung ON nguoi_dung;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_dia_chi') THEN
--         DROP TRIGGER set_timestamp_dia_chi ON dia_chi;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_san_pham') THEN
--         DROP TRIGGER set_timestamp_san_pham ON san_pham;
--     END IF;
--      IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_thuong_hieu') THEN
--         DROP TRIGGER set_timestamp_thuong_hieu ON thuong_hieu; -- Trigger for Brand if it had ngay_cap_nhat
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_dot_giam_gia') THEN
--         DROP TRIGGER set_timestamp_dot_giam_gia ON dot_giam_gia;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_phieu_giam_gia') THEN
--         DROP TRIGGER set_timestamp_phieu_giam_gia ON phieu_giam_gia;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_san_pham_chi_tiet') THEN
--         DROP TRIGGER set_timestamp_san_pham_chi_tiet ON san_pham_chi_tiet;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_gio_hang') THEN
--         DROP TRIGGER set_timestamp_gio_hang ON gio_hang;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_hoa_don') THEN
--         DROP TRIGGER set_timestamp_hoa_don ON hoa_don;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_danh_gia') THEN
--         DROP TRIGGER set_timestamp_danh_gia ON danh_gia;
--     END IF;
--     IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_wishlist') THEN
--         DROP TRIGGER set_timestamp_wishlist ON wishlist;
--     END IF;
-- END $$;
--
--
-- -- Áp dụng trigger cho các bảng có cột ngay_cap_nhat
-- CREATE TRIGGER set_timestamp_danh_muc BEFORE UPDATE ON danh_muc FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_nguoi_dung BEFORE UPDATE ON nguoi_dung FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_dia_chi BEFORE UPDATE ON dia_chi FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_san_pham BEFORE UPDATE ON san_pham FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- -- CREATE TRIGGER set_timestamp_thuong_hieu BEFORE UPDATE ON thuong_hieu FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp(); -- Add if thuong_hieu has ngay_cap_nhat
-- CREATE TRIGGER set_timestamp_dot_giam_gia BEFORE UPDATE ON dot_giam_gia FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_phieu_giam_gia BEFORE UPDATE ON phieu_giam_gia FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_san_pham_chi_tiet BEFORE UPDATE ON san_pham_chi_tiet FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_gio_hang BEFORE UPDATE ON gio_hang FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_hoa_don BEFORE UPDATE ON hoa_don FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_danh_gia BEFORE UPDATE ON danh_gia FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
-- CREATE TRIGGER set_timestamp_wishlist BEFORE UPDATE ON wishlist FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- =============================================================================
-- 7. INSERT DỮ LIỆU MẪU
-- =============================================================================

-- Insert sample user
INSERT INTO nguoi_dung (ma_nguoi_dung, avatar, ho_ten, gioi_tinh, ngay_sinh, cccd, email, so_dien_thoai, mat_khau, vai_tro, trang_thai)
VALUES
    ('CUS001', 'avatar1.jpg', 'Nguyen Van A', 'NAM', '1990-01-01', '123456789012', 'nguyenvana@example.com', '0123456789', 'hashedpassword', 'CUSTOMER', TRUE);

-- Insert sample category
INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc)
VALUES ('DM001', 'Laptop');

-- Insert sample address for user (assuming nguoi_dung id = 1)
INSERT INTO dia_chi (nguoi_dung_id, ho_ten_nguoi_nhan, so_dien_thoai_nguoi_nhan, duong, phuong_xa, quan_huyen, tinh_thanh, la_mac_dinh)
VALUES (1, 'Nguyen Van A', '0123456789', '12A Nguyen Trai', 'Ben Thanh', '1', 'HCM', TRUE);

-- INSERT DATA MẪU CHO BẢNG CPU
INSERT INTO cpu (mo_ta_cpu) VALUES
('Intel Core i5-1340P'),
('AMD Ryzen 5 5600H'),
('Intel Core i7-12700H');

-- INSERT DATA MẪU CHO BẢNG RAM
INSERT INTO ram (mo_ta_ram) VALUES
('16GB DDR5 5600MHz'),
('8GB DDR4 2666MHz'),
('32GB DDR5 6000MHz');

-- INSERT DATA MẪU CHO BẢNG O_CUNG
INSERT INTO o_cung (mo_ta_o_cung) VALUES
('512GB NVMe Gen4 SSD'),
('1TB SATA HDD'),
('256GB NVMe Gen3 SSD');

-- INSERT DATA MẪU CHO BẢNG GPU
INSERT INTO gpu (mo_ta_gpu) VALUES
('NVIDIA GeForce RTX 4060'),
('AMD Radeon RX 6800M'),
('NVIDIA GeForce GTX 1650');

-- INSERT DATA MẪU CHO BẢNG MAN_HINH
INSERT INTO man_hinh (mo_ta_man_hinh) VALUES
('15.6 inch FHD IPS 144Hz'),
('14 inch QHD OLED'),
('17.3 inch 4K UHD');

-- INSERT DATA MẪU CHO BẢNG CONG_GIAO_TIEP
INSERT INTO cong_giao_tiep (mo_ta_cong) VALUES
('2x USB 3.2, 1x HDMI, 1x Ethernet'),
('1x USB-C, 2x USB-A, 1x DisplayPort'),
('3x USB-A, 1x USB-C, 1x SD Card Reader');

-- INSERT DATA MẪU CHO BẢNG BAN_PHIM
INSERT INTO ban_phim (mo_ta_ban_phim) VALUES
('RGB Backlit Keyboard'),
('Chiclet Keyboard'),
('Mechanical Keyboard');

-- INSERT DATA MẪU CHO BẢNG KET_NOI_MANG
INSERT INTO ket_noi_mang (mo_ta_ket_noi) VALUES
('Wi-Fi 6E'),
('Gigabit Ethernet'),
('Wi-Fi 5');

-- INSERT DATA MẪU CHO BẢNG AM_THANH
INSERT INTO am_thanh (mo_ta_am_thanh) VALUES
('Dolby Atmos'),
('Stereo Speakers'),
('Hi-Res Audio');

-- INSERT DATA MẪU CHO BẢNG WEBCAM
INSERT INTO webcam (mo_ta_wc) VALUES
('1080p HD Webcam'),
('720p HD Webcam'),
('4K Ultra HD Webcam');

-- INSERT DATA MẪU CHO BẢNG BAO_MAT
INSERT INTO bao_mat (mo_ta_bao_mat) VALUES
('Fingerprint Reader'),
('IR Camera for Face Unlock'),
('TPM 2.0 Module');

-- INSERT DATA MẪU CHO BẢNG HE_DIEU_HANH
INSERT INTO he_dieu_hanh (mo_ta_he_dieu_hanh) VALUES
('Windows 11 Home'),
('Ubuntu 22.04 LTS'),
('macOS Monterey');

-- INSERT DATA MẪU CHO BẢNG PIN
INSERT INTO pin (mo_ta_pin) VALUES
('4-cell 75Wh'),
('3-cell 45Wh'),
('6-cell 99Wh');

-- INSERT DATA MẪU CHO BẢNG THIET_KE
INSERT INTO thiet_ke (mo_ta_thiet_ke) VALUES
('Slim and light design, 1.2kg, aluminum body'),
('Robust design with cooling system, 2.5kg, plastic body'),
('Premium design, 1.8kg, magnesium alloy body');

-- INSERT DATA MẪU CHO BẢNG THUONG_HIEU
INSERT INTO thuong_hieu (mo_ta_thuong_hieu) VALUES
('Dell'),
('Apple'),
('Logitech');

-- Insert sample product (SPU)
INSERT INTO san_pham (ma_san_pham, ten_san_pham, thuong_hieu_id, mo_ta, hinh_anh, ngay_ra_mat, trang_thai)
VALUES ('SP001', 'Laptop Lenovo Ideapad 3', 1, 'Laptop hiệu năng tốt cho công việc', '["image1.jpg", "image2.jpg"]', '2025-03-01', TRUE);

-- Insert product-category mapping (san_pham id = 1, danh_muc id = 1)
INSERT INTO san_pham_danh_muc (san_pham_id, danh_muc_id)
VALUES (1, 1);

-- Insert sample discount campaign
INSERT INTO dot_giam_gia (ma_dot_giam_gia, ten_dot_giam_gia, phan_tram_giam, ngay_bat_dau, ngay_ket_thuc, trang_thai)
VALUES ('DG001', 'Mua sắm mùa hè', 15.00, '2025-06-01T00:00:00Z', '2025-06-30T00:00:00Z', TRUE);

-- Insert sample voucher
INSERT INTO phieu_giam_gia (ma_phieu_giam_gia, loai_phieu_giam_gia, gia_tri_giam, gia_tri_don_hang_toi_thieu, ngay_bat_dau, ngay_ket_thuc, mo_ta, so_luong_ban_dau)
VALUES ('PGG001', 'PHAN_TRAM', 10.00, 1000000.00, '2025-07-01T00:00:00Z', '2025-07-31T00:00:00Z', 'Voucher giảm 10%', 100);

-- Insert sample product detail / SKU (san_pham id = 1)
INSERT INTO san_pham_chi_tiet (san_pham_id, sku, mau_sac, so_luong_ton_kho, gia_ban, hinh_anh, trang_thai, cpu_id, ram_id, o_cung_id)
VALUES (1, 'SKU001', 'Blue', 50, 15000000.00, '["spct1.jpg", "spct2.jpg"]', TRUE, 1, 1, 1);
-- (Giả sử bạn đã insert dữ liệu mẫu cho cpu, ram, o_cung tương ứng với id = 1)

-- Insert SKU - Discount mapping (san_pham_chi_tiet id = 1, dot_giam_gia id = 1)
INSERT INTO san_pham_chi_tiet_dot_giam_gia (san_pham_chi_tiet_id, dot_giam_gia_id)
VALUES (1, 1);

-- Insert sample shopping cart for user id = 1
INSERT INTO gio_hang (nguoi_dung_id, trang_thai)
VALUES (1, TRUE);

-- Insert sample cart item (gio_hang id = 1, san_pham_chi_tiet id = 1)
INSERT INTO gio_hang_chi_tiet (gio_hang_id, san_pham_chi_tiet_id, so_luong)
VALUES (1, 1, 2);

-- Insert sample order
INSERT INTO hoa_don (ma_hoa_don, khach_hang_id, dia_chi_giao_hang_duong, dia_chi_giao_hang_phuong_xa, dia_chi_giao_hang_quan_huyen, dia_chi_giao_hang_tinh_thanh, gia_tri_san_pham, tong_thanh_toan, trang_thai_giao_hang)
VALUES ('HD001', 1, '12A Nguyen Trai', 'Ben Thanh', '1', 'HCM', 15000000.00, 14000000.00, 'DANG_XU_LY');

-- Insert sample order line item (hoa_don id = 1, san_pham_chi_tiet id = 1)
INSERT INTO hoa_don_chi_tiet (hoa_don_id, san_pham_chi_tiet_id, so_luong, gia_goc, gia_ban, thanh_tien, ten_san_pham_snapshot, sku_snapshot)
VALUES (1, 1, 1, 15000000.00, 14000000.00, 14000000.00, 'Laptop Lenovo Ideapad 3', 'SKU001');

-- Insert sample order-voucher mapping (hoa_don id = 1, phieu_giam_gia id = 1)
INSERT INTO hoa_don_phieu_giam_gia (hoa_don_id, phieu_giam_gia_id, gia_tri_da_giam)
VALUES (1, 1, 1000000.00);

-- Insert sample payment transaction (hoa_don id = 1, nguoi_dung id = 1)
INSERT INTO thanh_toan (nguoi_dung_id, trang_thai_giao_dich, phuong_thuc_thanh_toan, ma_giao_dich, gia_tri)
VALUES (1, 'DA_THANH_TOAN', 'VISA', 'TXN001', 14000000.00);

-- Insert sample product review (san_pham_chi_tiet id = 1, nguoi_dung id = 1)
INSERT INTO danh_gia (san_pham_chi_tiet_id, nguoi_dung_id, so_sao, noi_dung, trang_thai)
VALUES (1, 1, 5, 'Sản phẩm tuyệt vời!', 'DUYET');

-- Insert sample wishlist for user id = 1
INSERT INTO wishlist (nguoi_dung_id)
VALUES (1);

-- Insert sample wishlist item (wishlist id = 1, san_pham id = 1)
INSERT INTO wishlist_item (wishlist_id, san_pham_id)
VALUES (1, 1);

-- Insert sample mapping voucher - user (phieu_giam_gia id = 1, nguoi_dung id = 1)
INSERT INTO phieu_giam_gia_nguoi_dung (phieu_giam_gia_id, nguoi_dung_id)
VALUES (1, 1);

-- =============================================================================
-- KẾT THÚC SCRIPT TẠO BẢNG V5 & INSERT SAMPLE DATA
-- =============================================================================
