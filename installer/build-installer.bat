@echo off
rem Gera o instalador Windows (Setup .exe). Rode na raiz ou em installer\.
rem Requisitos: JDK 17+ (jpackage), Maven e WiX Toolset 3.x no PATH.
rem Sem WiX: troque TIPO=exe por TIPO=app-image (gera uma pasta portátil).
setlocal
cd /d "%~dp0\.."

set TIPO=exe
set NOME=ESC Contabil
set VERSAO=0.1.0
set JAR=controle-recebimentos-%VERSAO%.jar

call mvn --batch-mode clean verify || goto :erro

if exist installer\input rmdir /s /q installer\input
if exist installer\output rmdir /s /q installer\output
mkdir installer\input
copy "target\%JAR%" installer\input\ >nul || goto :erro

jpackage ^
 --type %TIPO% ^
 --name "%NOME%" ^
 --app-version %VERSAO% ^
 --vendor "ESC" ^
 --input installer\input ^
 --main-jar %JAR% ^
 --dest installer\output ^
 --win-console ^
 --win-shortcut --win-menu --win-dir-chooser ^
 --java-options "-Dspring.profiles.active=mysql-local" ^
 --java-options "-Djava.awt.headless=false" ^
 --java-options "-Dfile.encoding=UTF-8" || goto :erro

echo.
echo Pronto. Arquivo em installer\output
exit /b 0

:erro
echo.
echo ERRO ao gerar o instalador.
exit /b 1
