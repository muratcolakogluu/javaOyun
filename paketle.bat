@echo off
REM ===================================================================
REM  CryptDelver'i paylasilabilir tek klasore paketler.
REM
REM  Cikti: paket\CryptDelver.zip
REM  Karsi taraf zip'i acip CryptDelver.exe'ye cift tikliyor. Java kurmasi
REM  gerekmiyor -- Java'nin kendisi de paketin icinde.
REM
REM  JDK ve Maven PATH'te yoksa asagidaki yedek yollar kullaniliyor;
REM  JAVA_HOME ve MVN vererek onlari ezebilirsin.
REM ===================================================================

setlocal

REM JDK ve Maven: once PATH'e, yoksa bu makinedeki tam yollara bakiyor.
REM Baska bir makinede calistiracaksan JAVA_HOME ve MVN'yi kendi yollarinla
REM ver, betige dokunmana gerek yok:
REM     set JAVA_HOME=C:\yol\jdk & set MVN=mvn & paketle.bat
if "%JAVA_HOME%"=="" if exist "%LOCALAPPDATA%\..\..\.jdks" set JAVA_HOME=%USERPROFILE%\.jdks\openjdk-25.0.2
if "%JAVA_HOME%"=="" (
    echo HATA: JAVA_HOME ayarli degil. JDK 21+ kurup JAVA_HOME'u ver.
    exit /b 1
)

if "%MVN%"=="" where /q mvn.cmd && set MVN=mvn.cmd
if "%MVN%"=="" set MVN="%PROGRAMFILES%\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd"

set SURUM=0.1.0
set SISMAN=target\cryptdelver-0.1.0-SNAPSHOT-oyun.jar

REM Pakete konan Java'nin modul listesi. Liste vermemek 134 MB, bu liste
REM 89 MB. Daraltmak sesi sessizce bozabilirdi -- oyun yine acilir, sesin
REM gitmis oldugunu ancak oynayan fark ederdi. O yuzden ayni liste altinda
REM ses dosyalarinin yuklenip calabildigi scratchpad'deki SoundProbe ile
REM olculdu. Listeye dokunuyorsan o olcumu tekrarla.
set MODULLER=java.base,java.desktop,java.logging,java.xml,jdk.unsupported,java.scripting,java.management

echo.
echo [1/3] Oyun derleniyor ve sisman jar uretiliyor...
call %MVN% -B -DskipTests package || goto :hata

echo.
echo [2/3] Java calisma zamaniyla birlikte paketleniyor...
if not exist paket mkdir paket
if exist paket\girdi rmdir /s /q paket\girdi
if exist paket\dagitim rmdir /s /q paket\dagitim
mkdir paket\girdi
copy /y "%SISMAN%" paket\girdi\cryptdelver.jar >nul || goto :hata

"%JAVA_HOME%\bin\jpackage" ^
    --type app-image ^
    --name CryptDelver ^
    --app-version %SURUM% ^
    --vendor CryptDelver ^
    --input paket\girdi ^
    --main-jar cryptdelver.jar ^
    --main-class com.cryptdelver.Launcher ^
    --icon dagitim\cryptdelver.ico ^
    --dest paket\dagitim ^
    --add-modules %MODULLER% ^
    --java-options "--enable-native-access=ALL-UNNAMED" || goto :hata

echo.
echo [3/3] Zip hazirlaniyor...
if exist paket\CryptDelver.zip del /q paket\CryptDelver.zip
powershell -NoProfile -Command ^
    "Compress-Archive -Path 'paket\dagitim\CryptDelver' -DestinationPath 'paket\CryptDelver.zip' -Force" || goto :hata

echo.
echo Bitti: paket\CryptDelver.zip
echo Karsi taraf zip'i acip CryptDelver\CryptDelver.exe'ye cift tiklasin.
goto :son

:hata
echo.
echo HATA: paketleme yarida kaldi.
exit /b 1

:son
endlocal
