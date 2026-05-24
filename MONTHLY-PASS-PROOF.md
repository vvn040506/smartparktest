# 🎫 Bằng chứng Thẻ tháng - Giải pháp

## ❌ Vấn đề hiện tại

Sau khi thanh toán thẻ tháng thành công:
- ✅ Có trang web hiển thị thẻ ACTIVE
- ❌ **KHÔNG CÓ** thẻ vật lý/QR code để xuất trình
- ❌ Nhân viên bãi xe không biết kiểm tra thế nào
- ❌ Khách phải nhớ link trang chi tiết

---

## 💡 Các giải pháp

### **Giải pháp 1: QR Code thẻ tháng (Khuyến nghị)**

#### Cách hoạt động:
1. Sau khi thanh toán → Tạo QR code chứa thông tin thẻ
2. Khách lưu QR code vào điện thoại
3. Khi vào bãi → Quét QR → Hệ thống kiểm tra thẻ hợp lệ

#### Thông tin trong QR:
```json
{
  "type": "monthly_pass",
  "id": 123,
  "plate": "30A12345",
  "code": "MP123456"
}
```

#### Implement:

**1. Thêm dependency tạo QR code**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.2</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.2</version>
</dependency>
```

**2. Tạo QRCodeService**
```java
@Service
public class QRCodeService {
    
    public String generatePassQR(MonthlyPass pass) throws Exception {
        String data = String.format(
            "SMARTPARK_PASS|%d|%s|%s|%s",
            pass.getId(),
            pass.getPaymentCode(),
            pass.getLicensePlate(),
            pass.getEndDate()
        );
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            data, 
            BarcodeFormat.QR_CODE, 
            300, 
            300
        );
        
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        
        return "data:image/png;base64," + 
               Base64.getEncoder().encodeToString(pngData);
    }
}
```

**3. Thêm vào trang chi tiết thẻ**
```html
<!-- monthly-pass-detail.html -->
<div th:if="${pass.status == 'ACTIVE'}" class="card mb-3">
    <div class="card-header">🎫 Thẻ của bạn</div>
    <div class="card-body text-center">
        <div class="qr-wrap">
            <img th:src="${passQR}" alt="QR Thẻ tháng">
        </div>
        <p class="mt-3 text-muted" style="font-size:13px">
            📱 Chụp màn hình hoặc lưu ảnh này<br>
            Xuất trình khi vào bãi xe
        </p>
        <button class="btn btn-primary" onclick="downloadQR()">
            💾 Tải QR Code
        </button>
    </div>
</div>

<script>
function downloadQR() {
    const img = document.querySelector('.qr-wrap img');
    const link = document.createElement('a');
    link.href = img.src;
    link.download = 'the-thang-' + [[${pass.paymentCode}]] + '.png';
    link.click();
}
</script>
```

**4. Controller trả về QR**
```java
@GetMapping("/the-thang/{id}")
public String chiTietTheThang(@PathVariable Long id, Model model) {
    MonthlyPass pass = monthlyPassService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Thẻ tháng", "id", id));
    
    model.addAttribute("pass", pass);
    
    // Nếu thẻ ACTIVE → tạo QR code
    if ("ACTIVE".equals(pass.getStatus())) {
        try {
            String passQR = qrCodeService.generatePassQR(pass);
            model.addAttribute("passQR", passQR);
        } catch (Exception e) {
            // Log error
        }
    }
    
    // ... code cũ cho QR thanh toán
    return "monthly-pass-detail";
}
```

**5. API kiểm tra thẻ (cho nhân viên quét)**
```java
@PostMapping("/api/verify-pass")
public ApiResponse<MonthlyPass> verifyPass(@RequestBody String qrData) {
    // Parse: SMARTPARK_PASS|123|MP123456|30A12345|2026-06-22
    String[] parts = qrData.split("\\|");
    
    if (parts.length < 4 || !"SMARTPARK_PASS".equals(parts[0])) {
        return ApiResponse.error("QR không hợp lệ");
    }
    
    Long id = Long.parseLong(parts[1]);
    MonthlyPass pass = monthlyPassService.findById(id).orElse(null);
    
    if (pass == null) {
        return ApiResponse.error("Thẻ không tồn tại");
    }
    
    if (!"ACTIVE".equals(pass.getStatus())) {
        return ApiResponse.error("Thẻ không còn hiệu lực");
    }
    
    if (pass.getEndDate().isBefore(LocalDate.now())) {
        return ApiResponse.error("Thẻ đã hết hạn");
    }
    
    return ApiResponse.success("Thẻ hợp lệ", pass);
}
```

---

### **Giải pháp 2: Tra cứu theo biển số (Đơn giản hơn)**

#### Cách hoạt động:
1. Nhân viên nhập biển số xe vào hệ thống
2. Hệ thống kiểm tra có thẻ tháng hợp lệ không
3. Cho xe vào nếu có thẻ

#### Implement:

**1. Trang tra cứu cho nhân viên**
```html
<!-- staff-check-pass.html -->
<div class="card">
    <div class="card-header">🔍 Kiểm tra thẻ tháng</div>
    <div class="card-body">
        <form id="checkForm">
            <input type="text" 
                   class="form-control form-control-lg" 
                   placeholder="Nhập biển số xe (VD: 30A12345)"
                   id="plateInput"
                   style="text-transform:uppercase">
            <button type="submit" class="btn btn-primary btn-lg w-100 mt-3">
                Kiểm tra
            </button>
        </form>
        
        <div id="result" class="mt-4"></div>
    </div>
</div>

<script>
document.getElementById('checkForm').onsubmit = async (e) => {
    e.preventDefault();
    const plate = document.getElementById('plateInput').value.trim();
    
    const res = await fetch(`/api/monthly-passes/check/${plate}`);
    const data = await res.json();
    
    const resultDiv = document.getElementById('result');
    
    if (data.success && data.data) {
        const pass = data.data;
        resultDiv.innerHTML = `
            <div class="alert alert-success">
                <h5>✅ Thẻ hợp lệ</h5>
                <p><strong>Chủ xe:</strong> ${pass.ownerName}</p>
                <p><strong>Loại xe:</strong> ${pass.vehicleType}</p>
                <p><strong>Hiệu lực đến:</strong> ${pass.endDate}</p>
            </div>
        `;
    } else {
        resultDiv.innerHTML = `
            <div class="alert alert-danger">
                ❌ Không tìm thấy thẻ tháng hợp lệ
            </div>
        `;
    }
};
</script>
```

**2. API đã có sẵn**
```java
// Đã có trong ApiController.java
@GetMapping("/api/monthly-passes/check/{plate}")
public ApiResponse<MonthlyPass> checkActivePass(@PathVariable String plate) {
    return monthlyPassService.findActivePass(plate)
            .map(p -> ApiResponse.success("Thẻ hợp lệ", p))
            .orElse(ApiResponse.error("Không có thẻ tháng hợp lệ"));
}
```

---

### **Giải pháp 3: Gửi email thẻ tháng**

#### Sau khi kích hoạt thành công:
```java
@Service
public class MonthlyPassServiceImpl {
    
    @Autowired private EmailService emailService;
    @Autowired private QRCodeService qrCodeService;
    
    public boolean processPayment(String content, long amount, String bankRef) {
        // ... code xử lý thanh toán
        
        if (pass != null && "ACTIVE".equals(pass.getStatus())) {
            // Gửi email xác nhận + QR code
            sendPassConfirmationEmail(pass);
        }
    }
    
    private void sendPassConfirmationEmail(MonthlyPass pass) {
        if (pass.getEmail() == null) return;
        
        try {
            String qrBase64 = qrCodeService.generatePassQR(pass);
            String detailUrl = "https://your-domain.com/the-thang/" + pass.getId();
            
            String emailBody = String.format("""
                <h2>🎉 Thẻ tháng đã được kích hoạt!</h2>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Thẻ tháng của bạn đã được kích hoạt thành công:</p>
                <ul>
                    <li>Biển số: <strong>%s</strong></li>
                    <li>Hiệu lực: %s → %s</li>
                    <li>Mã thẻ: <strong>%s</strong></li>
                </ul>
                <p>QR Code thẻ của bạn:</p>
                <img src="%s" alt="QR Thẻ tháng" style="width:200px">
                <p><a href="%s">Xem chi tiết thẻ</a></p>
                """,
                pass.getOwnerName(),
                pass.getLicensePlate(),
                pass.getStartDate(),
                pass.getEndDate(),
                pass.getPaymentCode(),
                qrBase64,
                detailUrl
            );
            
            emailService.sendHtml(
                pass.getEmail(),
                "Thẻ tháng SmartPark đã kích hoạt",
                emailBody
            );
        } catch (Exception e) {
            // Log error
        }
    }
}
```

---

### **Giải pháp 4: SMS thông báo (Nâng cao)**

Gửi SMS chứa link thẻ tháng:
```
SmartPark: The thang cua ban da kich hoat!
Bien so: 30A12345
Hieu luc den: 22/06/2026
Xem chi tiet: https://short.link/abc123
```

---

## 🎯 So sánh giải pháp

| Giải pháp | Ưu điểm | Nhược điểm | Độ khó |
|-----------|---------|------------|--------|
| **QR Code** | ✅ Nhanh, tự động<br>✅ Khó giả mạo<br>✅ Chuyên nghiệp | ❌ Cần app quét | ⭐⭐⭐ |
| **Tra biển số** | ✅ Đơn giản<br>✅ Không cần QR | ❌ Chậm hơn<br>❌ Nhân viên phải nhập tay | ⭐ |
| **Email** | ✅ Khách có bằng chứng<br>✅ Tự động | ❌ Phụ thuộc email | ⭐⭐ |
| **SMS** | ✅ Nhanh nhất<br>✅ Không cần internet | ❌ Tốn phí SMS | ⭐⭐⭐⭐ |

---

## 💡 Khuyến nghị: Kết hợp 2 + 3

### **Cho khách hàng:**
- ✅ Gửi email có QR code + link chi tiết
- ✅ Trang web hiển thị QR code để tải về
- ✅ Có thể chụp màn hình lưu lại

### **Cho nhân viên:**
- ✅ Trang tra cứu nhanh theo biển số
- ✅ (Tùy chọn) App quét QR code

### **Flow hoàn chỉnh:**
1. Khách đăng ký thẻ → Thanh toán
2. Hệ thống kích hoạt → Gửi email có QR
3. Khách lưu QR hoặc nhớ link
4. Khi vào bãi:
   - **Cách 1:** Xuất trình QR → Nhân viên quét
   - **Cách 2:** Nhân viên nhập biển số → Hệ thống check

---

## 📝 Checklist implement

- [ ] Thêm dependency ZXing (tạo QR)
- [ ] Tạo QRCodeService
- [ ] Sửa trang chi tiết thẻ: Hiển thị QR khi ACTIVE
- [ ] Thêm nút "Tải QR Code"
- [ ] Gửi email xác nhận có QR
- [ ] Tạo trang tra cứu cho nhân viên
- [ ] (Tùy chọn) Tạo app quét QR

Bạn muốn implement giải pháp nào?
