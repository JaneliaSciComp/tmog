# Build and Deployment Instructions

The application can be built and deployed using gradle commands run from the base 
directory of the tmog repository.  The build and test steps are handled through 
traditional gradle java tasks while the link and deploy steps are handled through
shell scripts called from gradle.  The details are in [build.gradle](build.gradle)
and in [src/module_build/scripts](src/module_build/scripts). 

### Build and Test Locally

```bash
gradle build
```

### Build, Test, Link, and Deploy Application

```bash
gradle deployImages
```

### Build and Deploy Configuration
```bash
gradle deployConfig
```