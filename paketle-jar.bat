@echo off
REM ===================================================================
REM  Her yerde calisan jar'lar. Cikti:
REM
REM    paket\CryptDelver-Mac-M-serisi.jar   (M1/M2/M3/M4 Mac)
REM    paket\CryptDelver-Mac-Intel.jar      (2020 oncesi Mac)
REM
REM  Ikisinin de icinde Windows ve Linux kutuphaneleri de var, yani
REM  aslinda ikisi de her yerde calisiyor; fark yalnizca hangi Mac
REM  islemcisine gore hazirlandiklari.
REM
REM  NEDEN IKI DOSYA: Mac'in iki mimarisinin kutuphaneleri ayni dosya
REM  adlarini kullaniyor (libglass.dylib). Tek jar'a ikisi birden
REM  sigmiyor, birlestirme sirasinda biri otekinin uzerine yaziliyor ve
REM  yanlis mimari sessizce icerde kaliyor -- oyun karsi tarafta hic
REM  acilmadan kapaniyor. Tek dosya ugruna "hangi Mac?" diye sormak
REM  yerine ikisini de uretip karsi tarafin kendi bildigi seye (Intel mi
REM  M mi) gore secmesini biraktik.
REM
REM  Bu jar'larin icinde Java YOK, karsi tarafta kurulu olmasi gerekiyor
REM  (21 ve uzeri, adoptium.net). Windows icin paketle.bat daha iyi --
REM  o Java istemiyor. Bu betik Mac ve Linux icin.
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

set SISMAN=target\cryptdelver-0.1.0-SNAPSHOT-oyun.jar

if not exist paket mkdir paket

echo.
echo [1/2] M serisi Mac (Apple Silicon) icin...
call %MVN% -B -Ptumplatformlar -Dmac.mimari=mac-aarch64 -DskipTests package || goto :hata
copy /y "%SISMAN%" paket\CryptDelver-Mac-M-serisi.jar >nul || goto :hata

echo.
echo [2/2] Intel Mac icin...
call %MVN% -B -Ptumplatformlar -Dmac.mimari=mac -DskipTests package || goto :hata
copy /y "%SISMAN%" paket\CryptDelver-Mac-Intel.jar >nul || goto :hata

echo.
echo Bitti:
echo   paket\CryptDelver-Mac-M-serisi.jar
echo   paket\CryptDelver-Mac-Intel.jar
echo.
echo Karsi taraf once Java 21+ kursun (adoptium.net), sonra jar'a cift
echo tiklasin. Acilmazsa Terminal'den:  java -jar DOSYA.jar
goto :son

:hata
echo.
echo HATA: paketleme yarida kaldi.
exit /b 1

:son
endlocal
