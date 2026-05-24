# 💰 Mô hình giá SmartPark

## 📊 Bảng giá tổng hợp

### **1. Đỗ thường (Walk-in)**
Tính theo giờ thực tế

| Loại xe | Giá/giờ |
|---------|---------|
| Xe máy 🏍️ | 10.000đ |
| Ô tô 🚗 | 20.000đ |

**Ví dụ:**
- Đỗ 3 giờ xe máy = 10.000đ × 3 = **30.000đ**
- Đỗ 5 giờ ô tô = 20.000đ × 5 = **100.000đ**

---

### **2. Đặt trước (Pre-booking)**
Tính theo **khung 12 giờ** (làm tròn lên)

#### **Công thức:**
```
Số khung 12h = CEILING(Số giờ / 12)
Giá = Số khung × Giá cơ bản
```

#### **Giá cơ bản (1 khung 12h):**
| Loại xe | Giá gốc | Có thẻ tháng (-20%) |
|---------|---------|---------------------|
| Xe máy 🏍️ | 20.000đ | 16.000đ |
| Ô tô 🚗 | 40.000đ | 32.000đ |

#### **Bảng giá theo thời gian:**

**Xe máy:**
| Thời gian | Số khung | Giá gốc | Có thẻ tháng |
|-----------|----------|---------|--------------|
| 1-12h | 1 | 20.000đ | 16.000đ |
| 13-24h | 2 | 40.000đ | 32.000đ |
| 25-36h | 3 | 60.000đ | 48.000đ |
| 37-48h | 4 | 80.000đ | 64.000đ |

**Ô tô:**
| Thời gian | Số khung | Giá gốc | Có thẻ tháng |
|-----------|----------|---------|--------------|
| 1-12h | 1 | 40.000đ | 32.000đ |
| 13-24h | 2 | 80.000đ | 64.000đ |
| 25-36h | 3 | 120.000đ | 96.000đ |
| 37-48h | 4 | 160.000đ | 128.000đ |

---

### **3. Thẻ tháng (Monthly Pass)**
Ra vào tự do trong 30 ngày

| Loại xe | Giá/tháng |
|---------|-----------|
| Xe máy 🏍️ | 200.000đ |
| Ô tô 🚗 | 500.000đ |

**Ưu đãi:**
- ✅ Ra vào không giới hạn
- ✅ Giảm 20% khi đặt trước vị trí

---

## 📝 Ví dụ tính giá

### **Ví dụ 1: Đặt trước 11 giờ (xe máy)**
```
Thời gian: 8h sáng → 19h tối = 11 giờ
Số khung 12h = CEILING(11/12) = 1 khung
Giá = 20.000đ × 1 = 20.000đ

Nếu có thẻ tháng:
Giá = 20.000đ × 0.8 = 16.000đ
```

### **Ví dụ 2: Đặt trước 13 giờ (xe máy)**
```
Thời gian: 8h sáng → 21h tối = 13 giờ
Số khung 12h = CEILING(13/12) = 2 khung
Giá = 20.000đ × 2 = 40.000đ

Nếu có thẻ tháng:
Giá = 40.000đ × 0.8 = 32.000đ
```

### **Ví dụ 3: Đặt trước 24 giờ (ô tô)**
```
Thời gian: 8h sáng hôm nay → 8h sáng hôm sau = 24 giờ
Số khung 12h = CEILING(24/12) = 2 khung
Giá = 40.000đ × 2 = 80.000đ

Nếu có thẻ tháng:
Giá = 80.000đ × 0.8 = 64.000đ
```

### **Ví dụ 4: Đặt trước 25 giờ (xe máy)**
```
Thời gian: 8h sáng hôm nay → 9h sáng hôm sau = 25 giờ
Số khung 12h = CEILING(25/12) = 3 khung
Giá = 20.000đ × 3 = 60.000đ

Nếu có thẻ tháng:
Giá = 60.000đ × 0.8 = 48.000đ
```

---

## 💡 So sánh: Đặt trước vs Thẻ tháng

### **Khi nào nên mua thẻ tháng?**

**Xe máy (200.000đ/tháng):**
- Đặt trước 1 khung (12h) = 20.000đ
- Hòa vốn khi đặt: 200.000 ÷ 20.000 = **10 lần/tháng**
- Nếu đỗ > 10 lần/tháng → **Nên mua thẻ tháng**

**Ô tô (500.000đ/tháng):**
- Đặt trước 1 khung (12h) = 40.000đ
- Hòa vốn khi đặt: 500.000 ÷ 40.000 = **13 lần/tháng**
- Nếu đỗ > 13 lần/tháng → **Nên mua thẻ tháng**

### **Lợi ích thẻ tháng:**
1. ✅ Ra vào không giới hạn
2. ✅ Không cần đặt trước (trừ khi muốn vị trí cố định)
3. ✅ Giảm 20% khi đặt trước vị trí
4. ✅ Tiết kiệm nếu đỗ thường xuyên

---

## 🎯 Chiến lược giá

### **Mục tiêu:**
1. **Walk-in:** Khách lẻ, giá linh hoạt theo giờ
2. **Pre-booking:** Khách có kế hoạch, giá ưu đãi hơn walk-in
3. **Monthly Pass:** Khách thường xuyên, giá tốt nhất

### **Lợi ích thẻ tháng:**
- Giảm 20% đặt trước → Khuyến khích mua thẻ
- Ví dụ: Đặt 1 khung xe máy
  - Không thẻ: 20.000đ
  - Có thẻ: 16.000đ (tiết kiệm 4.000đ)

### **Tại sao tính theo khung 12h?**
- ✅ Đơn giản, dễ hiểu
- ✅ Khuyến khích đặt dài hạn
- ✅ Tối ưu doanh thu
- ✅ Công bằng cho cả khách và bãi xe

---

## 🔧 Implementation

### **Database fields cần thêm:**
```sql
ALTER TABLE bookings ADD COLUMN start_time TIME;
ALTER TABLE bookings ADD COLUMN end_time TIME;
ALTER TABLE bookings ADD COLUMN duration_hours DECIMAL(5,2);
ALTER TABLE bookings ADD COLUMN blocks_12h INT;
```

### **Logic tính giá:**
```java
public long calculatePreBookingPrice(
    LocalTime startTime, 
    LocalTime endTime, 
    String vehicleType,
    boolean hasMonthlyPass
) {
    // Tính số giờ
    long hours = Duration.between(startTime, endTime).toHours();
    if (hours <= 0) hours += 24; // Qua ngày hôm sau
    
    // Tính số khung 12h (làm tròn lên)
    int blocks = (int) Math.ceil(hours / 12.0);
    if (blocks < 1) blocks = 1;
    
    // Giá cơ bản
    long pricePerBlock = vehicleType.equals("o_to") ? 40_000L : 15_000L;
    long totalPrice = pricePerBlock * blocks;
    
    // Giảm giá nếu có thẻ tháng
    if (hasMonthlyPass) {
        totalPrice = (long)(totalPrice * 0.8);
    }
    
    return totalPrice;
}
```

---

## 📊 Dự đoán doanh thu

### **Giả sử bãi xe có 50 chỗ:**

**Scenario 1: Chủ yếu walk-in**
- 30 xe/ngày × 3 giờ × 5.000đ = 450.000đ/ngày
- Doanh thu tháng: ~13.500.000đ

**Scenario 2: 50% pre-booking**
- 15 walk-in × 3h × 5.000đ = 225.000đ
- 15 pre-booking × 15.000đ = 225.000đ
- Tổng: 450.000đ/ngày = ~13.500.000đ

**Scenario 3: 20 thẻ tháng + pre-booking**
- 20 thẻ tháng × 200.000đ = 4.000.000đ
- 10 pre-booking/ngày × 15.000đ × 30 = 4.500.000đ
- Tổng: ~8.500.000đ/tháng (ổn định)

**Lợi ích thẻ tháng:**
- ✅ Doanh thu ổn định, dự đoán được
- ✅ Khách trung thành
- ✅ Giảm công việc quản lý

---

## 🎯 Tóm tắt

| Loại | Cách tính | Giá xe máy | Giá ô tô | Ưu đãi thẻ tháng |
|------|-----------|------------|----------|------------------|
| **Walk-in** | Theo giờ | 10k/giờ | 20k/giờ | - |
| **Pre-booking** | Theo khung 12h | 20k/khung | 40k/khung | -20% |
| **Monthly Pass** | Cố định | 200k/tháng | 500k/tháng | Unlimited + -20% booking |

**Công thức đặt trước:**
```
Giá = CEILING(Giờ / 12) × Giá_cơ_bản × (Có_thẻ ? 0.8 : 1.0)
```
