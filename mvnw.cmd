@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@echo off
setlocal EnableExtensions DisableDelayedExpansion
rem Maven Wrapper 3.3.4: arranque tradicional con Java, sin PowerShell.
set "JAVACMD=java.exe"
if defined JAVA_HOME set "JAVACMD=%JAVA_HOME%\bin\java.exe"
set "JVM_CONFIG_MAVEN_PROPS="
if not exist "%~dp0.mvn\jvm.config" goto jvm_config_ready
setlocal EnableDelayedExpansion
for /F "usebackq delims=" %%a in ("%~dp0.mvn\jvm.config") do set "JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a"
endlocal & set "JVM_CONFIG_MAVEN_PROPS=%JVM_CONFIG_MAVEN_PROPS%"
:jvm_config_ready
"%JAVACMD%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% "%~dp0scripts\PrepararMaven.java" "%~dp0."
if errorlevel 1 exit /b 1
"%JAVACMD%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% %MAVEN_DEBUG_OPTS% "-Dmaven.multiModuleProjectDirectory=%~dp0." -classpath "%~dp0.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %MAVEN_ARGS% %*
exit /b %ERRORLEVEL%
