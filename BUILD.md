# Build and Deployment Instructions

### Install Git, Java, and Gradle

1. Install Git: [https://www.atlassian.com/git/tutorials/install-git](https://www.atlassian.com/git/tutorials/install-git)
2. Install the latest Java version 8 JDK or JRE: [https://adoptopenjdk.net/](https://adoptopenjdk.net/).
3. Install Gradle version 4.8.1 or later: [https://gradle.org/install/](https://gradle.org/install/)


### Retrieve Source Code

```bash
git clone https://github.com/JaneliaSciComp/tmog.git

# switch to jdk8-zlatic branch
cd tmog
git checkout jdk8-zlatic
```


### Build and Test Locally

The application can be built using gradle commands run from the base 
directory where the tmog repository was cloned 
(see [build.gradle](build.gradle) for details).

```bash
gradle build

# creates:
#   build/libs/tmog-<version>.jar
#   build/libs_prod/tmog-<version>-prod.jar
```

### Launch Application

```bash
java -jar build/libs/tmog-<version>.jar [config.xml]
```

