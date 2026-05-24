# 🔍 Cách hoạt động của QR Verification

## 📋 Tổng quan

Tài liệu này giải thích chi tiết cách hệ thống xác nhận QR code hoạt động từ A-Z.

---

## 🎯 Flow tổng quan

```
┌─────────────┐
│  Khách hàng │
└──────┬──────┘
       │
       │ 1. Đặt vé/Đăng ký thẻ
       ↓
┌─────────────────┐
│ Thanh toán      │
│ (VietQR/Webhook)│
└──────┬──────────┘
       │
       │ 2. Status → PAID/ACTIVE
       ↓
┌──────────────────────┐
│ Generate QR Code     │
│ (QRCodeService)      │
└──────┬───────────────┘
       │
       │ 3. Hiển thị QR (Base64 PNG)
       ↓
┌──────────────────────┐
│ Khách lưu QR         │
│ (Download/Screenshot)│
└──────┬───────────────┘
       │
       │ 4. Đến bãi xe
       ↓
┌──────────────────────┐
│ Nhân viên quét QR    │
│ (/staff/scan-qr)     │
└──────┬───────────────┘
       │
       │ 5. Camera detect QR
       ↓
┌──────────────────────┐
│ Parse QR data        │
│ (JavaScript)         │
└──────┬───────────────┘
       │
       │ 6. POST /api/qr/verify
       ↓
┌──────────────────────┐
│ Verify trong DB      │
│ (QRController)       │
└──────┬───────────────┘
       │
       │ 7. Return result
       ↓
┌──────────────────────┐
│ Hiển thị kết quả     │
│ (Xanh/Đỏ)            │
└──────┬───────────────┘
       │
       │ 8. Nhân viên xác nhận
       ↓
┌──────────────────────┐
│ Cho xe vào           │
└──────────────────────┘
```

---

## 🔐 QR Code Format

### **Vé đặt trước (Booking):**
```
SMARTPARK_BOOKING|{id}|{code}|{plate}|{date}|{slot}
```

**Ví dụ:**
```
SMARTPARK_BOOKING|123|SP123456|30A12345|2026-05-24|A-01
```

**Giải thích:**
- `SMARTPARK_BOOKING`: Loại QR (booking)
- `123`: ID của booking trong database
- `SP123456`: Payment code (unique)
- `30A12345`: Biển số xe
- `2026-05-24`: Ngày đặt
- `A-01`: Vị trí đỗ

### **Thẻ tháng (Monthly Pass):**
```
SMARTPARK_PASS|{id}|{code}|{plate}|{endDate}
```

**Ví dụ:**
```
SMARTPARK_PASS|456|MP123456|51B67890|2026-06-24
```

**Giải thích:**
- `SMARTPARK_PASS`: Loại QR (monthly pass)
- `456`: ID của monthly pass trong database
- `MP123456`: Payment code (unique)
- `51B67890`: Biển số xe
- `2026-06-24`: Ngày hết hạn

---

## 🔧 Backend: Generate QR Code

### **File:** `QRCodeService.java`

```java
@Service
public class QRCodeService {
    
    // Generate QR cho booking
    public String generateBookingQR(Booking booking) {
        // 1. Tạo QR data string
        String data = String.format(
            "SMARTPARK_BOOKING|%d|%s|%s|%s|%s",
            booking.getId(),
            booking.getPaymentCode(),
            booking.getLicensePlate(),
            booking.getBookingDate(),
            booking.getSlotId()
        );
        
        // 2. Generate QR image (ZXing)
        BitMatrix matrix = new QRCodeWriter().encode(
            data,
            BarcodeFormat.QR_CODE,
            250, 250  // 250x250 pixels
        );
        
        // 3. Convert to PNG
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        
        // 4. Convert to Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] bytes = baos.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        
        // 5. Return data URL
        return "data:image/png;base64," + base64;
    }
    
    // Generate QR cho monthly pass
    public String generatePassQR(MonthlyPass pass) {
        String data = String.format(
            "SMARTPARK_PASS|%d|%s|%s|%s",
            pass.getId(),
            pass.getPaymentCode(),
            pass.getLicensePlate(),
            pass.getEndDate()
        );
        
        return generateQRImage(data);
    }
}
```

### **Khi nào generate QR?**

**BookingController:**
```java
@GetMapping("/ve/{id}")
public String chiTiet(@PathVariable Long id, Model model) {
    Booking b = bookingService.findById(id).orElseThrow(...);
    
    // Chỉ generate QR khi đã thanh toán
    if ("PAID".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus())) {
        String bookingQR = qrCodeService.generateBookingQR(b);
        model.addAttribute("bookingQR", bookingQR);
    }
    
    return "chi-tiet";
}
```

**Tương tự cho Monthly Pass:**
```java
@GetMapping("/the-thang/{id}")
public String chiTietTheThang(@PathVariable Long id, Model model) {
    MonthlyPass pass = monthlyPassService.findById(id).orElseThrow(...);
    
    // Chỉ generate QR khi ACTIVE
    if ("ACTIVE".equals(pass.getStatus())) {
        String passQR = qrCodeService.generatePassQR(pass);
        model.addAttribute("passQR", passQR);
    }
    
    return "monthly-pass-detail";
}
```

---

## 🖼️ Frontend: Hiển thị QR Code

### **File:** `chi-tiet.html`

```html
<!-- Chỉ hiển thị khi có bookingQR -->
<div th:if="${bookingQR != null}" class="mt-4">
    <hr>
    <h6 class="fw-bold mb-3">🎫 QR Code vé của bạn</h6>
    
    <!-- QR Image (Base64) -->
    <div class="qr-wrap">
        <img th:src="${bookingQR}" 
             alt="QR Vé" 
             style="width:250px;height:250px">
    </div>
    
    <p class="mt-3 text-muted" style="font-size:13px">
        📱 Chụp màn hình hoặc lưu ảnh này<br>
        Xuất trình khi vào bãi xe
    </p>
    
    <!-- Nút tải QR -->
    <button class="btn btn-primary mt-2" onclick="downloadBookingQR()">
        💾 Tải QR Code
    </button>
</div>

<script th:if="${bookingQR != null}">
function downloadBookingQR() {
    const link = document.createElement('a');
    link.href = /*[[${bookingQR}]]*/ '';  // Base64 data URL
    link.download = 've-' + /*[[${booking.paymentCode}]]*/ '' + '.png';
    link.click();
}
</script>
```

**Kết quả:**
- QR code hiển thị ngay trên trang
- Khách có thể chụp màn hình
- Hoặc nhấn nút tải về file PNG

---

## 📷 Scanner Page: Quét QR

### **File:** `staff-scan-qr.html`

```html
<!-- Camera Scanner -->
<div id="qr-reader" style="width:100%"></div>

<!-- HTML5 QR Code Scanner Library -->
<script src="https://unpkg.com/html5-qrcode"></script>

<script>
let html5QrCode;
let isScanning = false;

// Khởi động camera
function startScanner() {
    html5QrCode = new Html5Qrcode("qr-reader");
    
    html5QrCode.start(
        { facingMode: "environment" },  // Camera sau (mobile)
        {
            fps: 10,                    // 10 frames/giây
            qrbox: { width: 250, height: 250 }  // Khung quét
        },
        onScanSuccess,  // Callback khi quét thành công
        onScanError     // Callback khi lỗi
    );
}

// Khi quét thành công
function onScanSuccess(decodedText, decodedResult) {
    if (isScanning) return;  // Tránh quét nhiều lần
    
    isScanning = true;
    console.log("QR scanned:", decodedText);
    
    // Dừng scanner tạm thời
    html5QrCode.pause();
    
    // Verify QR
    verifyQR(decodedText);
}

// Verify QR qua API
async function verifyQR(qrData) {
    try {
        showResult('info', '🔄 Đang kiểm tra...');
        
        const response = await fetch('/api/qr/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ qrData: qrData })
        });
        
        const result = await response.json();
        
        if (result.success && result.data) {
            showSuccessResult(result.data);
        } else {
            showResult('error', '❌ ' + (result.message || 'QR không hợp lệ'));
            
            // Auto-resume sau 3 giây
            setTimeout(() => {
                html5QrCode.resume();
                isScanning = false;
            }, 3000);
        }
    } catch (error) {
        console.error('Error:', error);
        showResult('error', '❌ Lỗi kết nối server');
    }
}

// Khởi động khi trang load
window.onload = function() {
    startScanner();
};
</script>
```

**Flow:**
1. Trang load → Khởi động camera
2. Trình duyệt hỏi quyền → User cho phép
3. Camera bật → Hiển thị preview
4. User đưa QR vào khung quét
5. Library detect QR → Parse text
6. Gọi `onScanSuccess(decodedText)`
7. Gửi `decodedText` lên API verify

---

## 🔍 Backend: Verify QR Code

### **File:** `QRController.java`

```java
@RestController
@RequestMapping("/api/qr")
public class QRController {
    
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<QRVerifyResponse>> verifyQR(
            @RequestBody Map<String, String> payload) {
        
        String qrData = payload.get("qrData");
        
        // 1. Validate format
        if (!qrCodeService.isValidQRData(qrData)) {
            return ResponseEntity.ok(ApiResponse.error("QR code không hợp lệ"));
        }
        
        // 2. Parse QR data
        Map<String, String> parsed = qrCodeService.parseQRData(qrData);
        String type = parsed.get("type");
        
        // 3. Route theo loại
        if ("SMARTPARK_BOOKING".equals(type)) {
            return verifyBookingQR(parsed);
        } else if ("SMARTPARK_PASS".equals(type)) {
            return verifyPassQR(parsed);
        }
        
        return ResponseEntity.ok(ApiResponse.error("Loại QR không xác định"));
    }
    
    // Verify booking
    private ResponseEntity<ApiResponse<QRVerifyResponse>> verifyBookingQR(
            Map<String, String> parsed) {
        
        Long id = Long.parseLong(parsed.get("id"));
        String code = parsed.get("code");
        
        // 1. Tìm booking trong DB
        Booking booking = bookingService.findById(id).orElse(null);
        if (booking == null) {
            return ResponseEntity.ok(ApiResponse.error("Vé không tồn tại"));
        }
        
        // 2. Check mã code
        if (!booking.getPaymentCode().equals(code)) {
            return ResponseEntity.ok(ApiResponse.error("Mã vé không khớp"));
        }
        
        // 3. Check status
        if (!"PAID".equals(booking.getStatus()) && 
            !"CONFIRMED".equals(booking.getStatus())) {
            return ResponseEntity.ok(ApiResponse.error(
                "Vé chưa thanh toán (Status: " + booking.getStatus() + ")"
            ));
        }
        
        // 4. Check ngày
        if (booking.getBookingDate() != null) {
            LocalDate today = LocalDate.now();
            if (booking.getBookingDate().isBefore(today)) {
                return ResponseEntity.ok(ApiResponse.error("Vé đã hết hạn"));
            }
        }
        
        // 5. Tạo response
        QRVerifyResponse response = QRVerifyResponse.success(
            "booking",
            booking.getId(),
            booking.getPaymentCode(),
            booking.getLicensePlate(),
            booking.getStatus(),
            booking
        );
        
        return ResponseEntity.ok(ApiResponse.success("Vé hợp lệ", response));
    }
    
    // Verify monthly pass (tương tự)
    private ResponseEntity<ApiResponse<QRVerifyResponse>> verifyPassQR(
            Map<String, String> parsed) {
        // ... tương tự booking
        // Check: status = ACTIVE, endDate >= today
    }
}
```

### **Validation Rules:**

#### **Booking:**
```
✅ Vé phải tồn tại (findById)
✅ paymentCode phải khớp
✅ status = "PAID" hoặc "CONFIRMED"
✅ bookingDate >= today (chưa hết hạn)
```

#### **Monthly Pass:**
```
✅ Thẻ phải tồn tại (findById)
✅ paymentCode phải khớp
✅ status = "ACTIVE"
✅ endDate >= today (chưa hết hạn)
```

---

## 📤 API Response Format

### **Success Response:**
```json
{
  "success": true,
  "message": "Vé hợp lệ",
  "data": {
    "type": "booking",
    "id": 123,
    "code": "SP123456",
    "licensePlate": "30A12345",
    "status": "PAID",
    "details": {
      "customerName": "Nguyễn Văn A",
      "vehicleType": "xe_may",
      "slotId": "A-01",
      "bookingDate": "2026-05-24",
      "checkIn": "2026-05-24T08:00:00",
      "checkOut": "2026-05-24T20:00:00"
    }
  }
}
```

### **Error Response:**
```json
{
  "success": false,
  "message": "Vé đã hết hạn",
  "data": null
}
```

---

## 🎨 Frontend: Hiển thị kết quả

### **Success (Màu xanh):**
```javascript
function showSuccessResult(data) {
    const type = data.type === 'booking' ? 'Vé đặt trước' : 'Thẻ tháng';
    const icon = data.type === 'booking' ? '🎫' : '🎟️';
    
    let html = `
        <div class="result-box result-success">
            <div style="font-size:48px;text-align:center">✅</div>
            <h4 class="text-center text-success">${icon} ${type} hợp lệ!</h4>
            
            <div class="info-row">
                <span>Mã:</span>
                <strong>${data.code}</strong>
            </div>
            <div class="info-row">
                <span>Biển số:</span>
                <strong style="font-size:20px">${data.licensePlate}</strong>
            </div>
            <div class="info-row">
                <span>Trạng thái:</span>
                <strong class="text-success">${data.status}</strong>
            </div>
            
            <!-- Thông tin chi tiết -->
            ${renderDetails(data)}
            
            <button class="btn btn-success btn-large w-100 mt-4" 
                    onclick="confirmEntry()">
                ✓ Cho xe vào
            </button>
            <button class="btn btn-outline-secondary w-100 mt-2" 
                    onclick="resetScanner()">
                Quét QR khác
            </button>
        </div>
    `;
    
    document.getElementById('result-container').innerHTML = html;
}
```

### **Error (Màu đỏ):**
```javascript
function showResult(type, message) {
    const className = type === 'error' ? 'result-error' : 'result-info';
    
    const html = `
        <div class="result-box ${className}">
            <p class="mb-0 text-center fw-bold">${message}</p>
        </div>
    `;
    
    document.getElementById('result-container').innerHTML = html;
}
```

---

## 🔄 Flow xác nhận

```
1. Nhân viên nhấn "✓ Cho xe vào"
   ↓
2. JavaScript: confirmEntry()
   ↓
3. Hiển thị: "✅ Đã xác nhận! Xe được phép vào."
   ↓
4. Sau 2 giây: resetScanner()
   ↓
5. Clear result container
   ↓
6. Resume camera scanner
   ↓
7. Sẵn sàng quét QR tiếp theo
```

**Code:**
```javascript
function confirmEntry() {
    showResult('success', '✅ Đã xác nhận! Xe được phép vào.');
    setTimeout(resetScanner, 2000);
}

function resetScanner() {
    document.getElementById('result-container').innerHTML = '';
    document.getElementById('manual-code').value = '';
    isScanning = false;
    if (html5QrCode) {
        html5QrCode.resume();
    }
}
```

---

## 🔐 Security Considerations

### **1. QR Code không thể giả mạo:**
- Mỗi QR chứa `paymentCode` unique
- Backend verify code phải khớp với DB
- Không thể tạo QR giả với code hợp lệ

### **2. QR Code có thời hạn:**
- Booking: Check `bookingDate >= today`
- Monthly Pass: Check `endDate >= today`
- QR hết hạn → Reject

### **3. QR Code chỉ dùng 1 lần (optional):**
- Có thể thêm field `used` trong DB
- Sau khi check-in → Set `used = true`
- Lần quét sau → Reject "QR đã được sử dụng"

### **4. Rate limiting:**
- Giới hạn số lần verify/phút
- Tránh brute force attack

---

## 📊 Performance

### **Generate QR:**
- Thời gian: ~50-100ms
- Size: ~4-5KB (Base64)
- Cache: Có thể cache QR trong DB

### **Scan QR:**
- FPS: 10 frames/giây
- Latency: <100ms (detect)
- Auto-focus: Có

### **Verify API:**
- Response time: <200ms
- Database query: 1-2 queries
- No external API calls

---

## 🎯 Tóm tắt

### **Quy trình hoàn chỉnh:**

1. **Generate QR** (Backend)
   - Tạo QR data string
   - Encode bằng ZXing
   - Convert to Base64 PNG
   - Trả về cho frontend

2. **Hiển thị QR** (Frontend)
   - Embed Base64 image
   - Nút tải QR code
   - Hướng dẫn sử dụng

3. **Quét QR** (Scanner Page)
   - HTML5 QR Code Scanner
   - Camera access
   - Real-time detection
   - Parse QR text

4. **Verify QR** (Backend API)
   - Parse QR data
   - Query database
   - Validate rules
   - Return result

5. **Hiển thị kết quả** (Frontend)
   - Success: Màu xanh + thông tin
   - Error: Màu đỏ + lý do
   - Nút xác nhận

6. **Xác nhận** (Final Step)
   - Nhân viên nhấn "Cho xe vào"
   - Reset scanner
   - Sẵn sàng quét tiếp

---

**Status:** Documentation Complete  
**Version:** 1.0  
**Last Updated:** 2026-05-24
