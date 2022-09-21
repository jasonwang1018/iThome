# [Android NDK 程式 (.so檔) 逆向與防逆向](https://cyber.ithome.com.tw/2022/session-page/817)  
09 月 22 日 16:30 - 17:00 
  
Android App 的程式碼保護主要分為 Java 層及 Native 層兩種，其中 Java 層的程式碼保護已經有許多研究，多數開發者已經知道可以透過混淆或加殼來防止 Java 程式碼的逆向工程，然而針對 Native 層 NDK 程式的研究則相對較少。本議程將介紹各種 Android NDK 程式的逆向手法及防範措施，幫助開發者提升 Native 層程式碼安全。  
  
- LOCATION: 臺北南港展覽二館 7F 701E  
- LEVEL: 進階等級  
- SESSION TYPE: 現場演講  
- LANGUAGE: 中文  
- SESSION TOPIC: Mobile Security, Reverse Engineering, Secure Coding  
  
## 總結  
如何增加 NDK 程式的逆向難度
- 將函數宣告為 static 類型，避免暴露函數名稱  
- [JNI 函數使用 RegisterNatives 進行動態註冊](https://developer.android.com/training/articles/perf-jni#native-libraries)  
- [所有字串使用字串加密巨集進行加密](https://github.com/adamyaxley/Obfuscate)  
- [敏感函數自行實作，不呼叫系統函示庫的敏感函數](https://github.com/darvincisec/DetectFrida/tree/master/app/src/main/c)  
- [所有程式碼使用 O-LLVM 進行混淆](https://github.com/darvincisec/o-llvm-binary)  

## 相關工具檔案
[OWASP - MSTG Crackmes](https://github.com/OWASP/owasp-mastg/tree/master/Crackmes)  
[Unicorn Engine](https://github.com/unicorn-engine/unicorn)  
[unidbg](https://github.com/zhkl0228/unidbg)  
[AndroidNativeEmu](https://github.com/AeonLucid/AndroidNativeEmu)  
