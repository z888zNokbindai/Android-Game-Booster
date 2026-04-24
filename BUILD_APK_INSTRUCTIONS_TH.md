# วิธี Build APK ผ่าน GitHub Actions

ใช้ไฟล์นี้เมื่อเครื่อง local ไม่มี Android SDK หรือ build ไม่ผ่านบนเครื่องตัวเอง

## วิธีใช้

1. Fork หรือ clone repo `Mathias-Boulay/Android-Game-Booster`
2. เอา patch Android 15 ไปทับไฟล์ใน repo ก่อน
3. แตกไฟล์ zip นี้ แล้ว copy โฟลเดอร์ `.github/workflows/build-apk.yml` ไปไว้ใน root repo
4. Commit + push ขึ้น GitHub
5. เข้าแท็บ `Actions`
6. เลือก workflow `Build Android 15 APK`
7. กด `Run workflow`
8. เมื่อ build เสร็จ ดาวน์โหลด APK จาก `Artifacts`

APK ที่ได้จะเป็น debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

ติดตั้งด้วย ADB:

```bash
adb install -r app-debug.apk
adb shell pm grant com.spse.gameresolutionchanger android.permission.WRITE_SECURE_SETTINGS
```

> หมายเหตุ: ถ้าใช้ non-root mode ยังต้อง grant WRITE_SECURE_SETTINGS ผ่าน ADB อยู่ดี
