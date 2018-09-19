REM This script will create a windows package for tmog.
REM
REM See src/module_build/scripts/setup_windows_package_build.sh for setup details.
REM
REM Results are written to a directory named 'tmog' in the script run directory.
REM All result subdirectories except for 'runtime' should be copied to src/module_build/package_templates/windows.
REM NOTE: You'll need to chmod 755 src/module_build/package_templates/windows/[m-v]* after copying.
REM
REM This script should only need to be run if something changes in the Windows OS packaging process.
REM Normally, code (and even JDK) changes can be handled simply by using jlink to build the runtime subdirectory.
REM See src/module_build/scripts/02_link.sh for details.
REM

set JDK_DIR=.\JDK_VERSION_FROM_SETUP
set CORE_ARGS=-deploy -v -native image -name tmog -outdir . -outfile tmog -title tmog

set TMOG_MODULE=org.janelia.tmog
set TMOG_MAIN=%TMOG_MODULE%/org.janelia.it.ims.tmog.JaneliaTransmogrifier
set MODULE_ARGS=--module-path libs_prod;libs_fixed --add-modules %TMOG_MODULE% --module %TMOG_MAIN%

set BUNDLER_ARGS=-BsignBundle=false -BjvmOptions=-Xms264m -BjvmOptions=-Xmx1024m -BdropinResourcesRoot=.

"%JDK_DIR%\bin\javapackager" %CORE_ARGS% %MODULE_ARGS% %BUNDLER_ARGS%

REM remove runtime directory that gets rebuilt elsewhere by jlink
REM
RMDIR /S "tmog\runtime"

REM remove unneeded dll files
del "tmog\msvcp120.dll"
del "tmog\msvcr120.dll"
