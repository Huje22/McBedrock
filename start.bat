@echo off
REG QUERY HKCU\Console /v VirtualTerminalLevel >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    REG ADD HKCU\Console /v VirtualTerminalLevel /t REG_DWORD /d 1 /f
    echo Dodano wpis do rejestru aby dodac wsparcje dla ANSI.
)

:: BatchGotAdmin (Run as Admin code starts)
REM --> Check for permissions
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
REM --> If error flag set, we do not have admin.
if '%errorlevel%' NEQ '0' (
echo Proszenie o Administratora....
goto UACPrompt
) else ( goto gotAdmin )
:UACPrompt
echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
echo UAC.ShellExecute "%~s0", "", "", "runas", 1 >> "%temp%\getadmin.vbs"
"%temp%\getadmin.vbs"
exit /B
:gotAdmin
if exist "%temp%\getadmin.vbs" ( del "%temp%\getadmin.vbs" )
pushd "%CD%"
CD /D "%~dp0"
:: BatchGotAdmin (Run as Admin code ends)
:: Your codes should start from the following line
java -jar .\FreeMinecraftBedrock-1.2.jar
pause