@echo off
echo Running dmd...
dmd src/main/d/jni.d src/main/d/shell.d -of=wlplugin.dll -shared -m64 && goto l1
echo Compilation failure!
goto end

:l1

move wlplugin.dll target
if %1%==dmd (goto end)
echo Running maven...
mvn install

:end