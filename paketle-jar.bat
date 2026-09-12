@echo off
REM ===================================================================
REM  Her yerde calisan tek dosya: paket\CryptDelver.jar
REM
REM  paketle.bat'tan farki: bu jar'in icinde Java YOK, karsi tarafta
REM  kurulu olmasi gerekiyor (21 ve uzeri). Buna karsilik Windows, Mac ve
REM  Linux'ta ayni dosya calisiyor.
REM
REM  Windows icin paketle.bat daha iyi (Java istemiyor). Bu betik Mac ya
REM  da Linux'taki biri icin.
REM
REM  Calistirma (her platformda):  java -jar CryptDelver.jar
REM
REM  MAC MIMARISI: varsayilan Apple Silicon. Karsi taraf Intel Mac
REM  kullaniyorsa bu betikteki MAC_MIMARI satirini "mac" yap. Ikisi ayni
REM  dosya adlarini kullandigi icin tek jar'a ikisi birden sigmiyor.
REM ===================================================================

setlocal

if "%JAVA_HOME%"=="" set JAVA_HOME=C:\Users\murat\.jdks\openjdk-25.0.2
set MVN="C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd"

set MAC_MIMARI=mac-aarch64
set SISMAN=target\cryptdelver-0.1.0-SNAPSHOT-oyun.jar

echo.
echo [1/2] Windows, Linux ve %MAC_MIMARI% kutuphaneleriyle derleniyor...
call %MVN% -B -Ptumplatformlar -Dmac.mimari=%MAC_MIMARI% -DskipTests package || goto :hata

echo.
echo [2/2] Kopyalaniyor...
if not exist paket mkdir paket
copy /y "%SISMAN%" paket\CryptDelver.jar >nul || goto :hata

echo.
echo Bitti: paket\CryptDelver.jar
echo Karsi taraf once Java 21+ kursun (adoptium.net), sonra:
echo     java -jar CryptDelver.jar
goto :son

:hata
echo.
echo HATA: paketleme yarida kaldi.
exit /b 1

:son
endlocal
