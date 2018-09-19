# Build and Deployment Instructions

The application can be built and deployed using gradle commands run from the base 
directory of the tmog repository.  The build and test steps are handled through 
traditional gradle java tasks while the link, package, and deploy steps are handled 
through shell scripts called from gradle.  The details are in [build.gradle](build.gradle)
and in [src/module_build/scripts](src/module_build/scripts). 

### Build and Test Locally

```bash
gradle build
```

### Link, Package, and Deploy Application

Each deployment location has the following directory structure:
```bash
# ------------------------------------------------------------------------------------
# A deploy directory with platform specific packages:
#
#   tag = "current" or deployment timestamp
#
#   "current" directories are copies of latest timestamp directory 
#   instead of symbolic links because of default Windows behavior 
#   (see http://www.virtualizetheworld.com/2014/07/the-symbolic-link-cannot-be-followed.html)

deploy/config/<tag>/*.xml           # specific configuration files with output targets on this filesystem

deploy/app/<tag>/mac/tmog.app       # common mac package from build
deploy/app/<tag>/windows/tmog.exe   # common windows package from build

# ------------------------------------------------------------------------------------
# A run directory with wrappers/shortcuts for launching specific tmog configurations:

run/mac/tmog_flylight_flip.app      # mac automator app wrapper with document.wflow that references 
                                    # current deploy tmog.app and flip config file

run/windows/tmog_flylight_flip.lnk  # windows shortcut that references 
                                    # current deploy tmog.exe and flip config file
```

Current deployment locations are:
* /groups/flyfuncconn/flyfuncconn/tmog
* /groups/flylight/flylight/tmog
* /groups/projtechres/projtechres/tmog

Legacy (to be migrated from java web start) deployment locations are: 
* /groups/leet/leetimg/leetlab/tmog (leetimg group)
* /groups/magee/mageelab/tmog
* /groups/rubin/data1/rubinlab/tmog
* /groups/svoboda/wdbp/tmog (wdbp group)
* /groups/zlatic/zlaticlab/tmog
* /nrs/zlatic/zlaticlab/tmog

#### Code Change Deployment
```bash
# deploys to deploy/app for each location

gradle deployPackages
```


#### Configuration Change Deployment
```bash
# deploys to deploy/config for each location

gradle deployConfig
```

#### Setup Windows Package Build
```bash
# set up a windows_package_build directory that can be be easily copied to a Windows VM
# to facilitate running the javapackager tool

gradle setupWindowsPackageBuild
```
