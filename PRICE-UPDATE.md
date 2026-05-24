# 💰 Cập nhật giá mới

## ✅ Đã cập nhật

### **Giá mới:**

| Loại | Xe máy | Ô tô |
|------|--------|------|
| **Walk-in** (theo giờ) | 10.000đ/giờ | 20.000đ/giờ |
| **Pre-booking** (theo khung 12h) | 20.000đ/khung | 40.000đ/khung |
| **Pre-booking (có thẻ tháng)** | 16.000đ/khung (-20%) | 32.000đ/khung (-20%) |
| **Thẻ tháng** | 200.000đ/tháng | 500.000đ/tháng |

---

## 📊 So sánh giá cũ vs mới

### **Walk-in:**
| Loại | Giá cũ | Giá mới | Tăng |
|------|--------|---------|------|
| Xe máy | 5k/giờ | **10k/giờ** | +100% |
| Ô tô | 15k/giờ | **20k/giờ** | +33% |

### **Pre-booking:**
| Loại | Giá cũ | Giá mới | Tăng |
|------|--------|---------|------|
| Xe máy | 15k/khung | **20k/khung** | +33% |
| Ô tô | 40k/khung | **40k/khung** | Không đổi |

---

## 🧮 Ví dụ tính giá mới

### **Đặt trước 11 giờ (xe máy):**
- Số khung: 1
- Giá gốc: **20.000đ**
- Có thẻ tháng: **16.000đ** (giảm 4.000đ)

### **Đặt trước 13 giờ (xe máy):**
- Số khung: 2
- Giá gốc: **40.000đ**
- Có thẻ tháng: **32.000đ** (giảm 8.000đ)

### **Đặt trước 24 giờ (ô tô):**
- Số khung: 2
- Giá gốc: **80.000đ**
- Có thẻ tháng: **64.000đ** (giảm 16.000đ)

---

## 💡 Phân tích hòa vốn thẻ tháng

### **Xe máy (200.000đ/tháng):**
- Giá đặt trước: 20.000đ/khung
- Hòa vốn: 200.000 ÷ 20.000 = **10 lần/tháng**
- Nếu đỗ > 10 lần/tháng → Nên mua thẻ

### **Ô tô (500.000đ/tháng):**
- Giá đặt trước: 40.000đ/khung
- Hòa vốn: 500.000 ÷ 40.000 = **13 lần/tháng**
- Nếu đỗ > 13 lần/tháng → Nên mua thẻ

---

## 📝 Files đã cập nhật

✅ `PricingService.java` - Constants giá mới  
✅ `PRICING-MODEL.md` - Bảng giá chi tiết  
✅ `IMPLEMENTATION-LOG.md` - Ví dụ mới  
✅ `PHASE-1-SUMMARY.md` - Tóm tắt giá  

---

## 🚀 Không cần làm gì thêm

Code đã tự động sử dụng giá mới từ constants trong `PricingService.java`.

Khi chạy lại app, giá sẽ được áp dụng ngay lập tức.

---

## ✅ Tóm tắt

**Giá tăng:**
- Walk-in xe máy: 5k → **10k** (+100%)
- Walk-in ô tô: 15k → **20k** (+33%)
- Pre-booking xe máy: 15k → **20k** (+33%)
- Pre-booking ô tô: **40k** (không đổi)

**Thẻ tháng:** Không đổi (200k xe máy, 500k ô tô)

**Ưu đãi:** Vẫn giảm 20% khi có thẻ tháng
