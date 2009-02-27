========================================
Build and Deployment Instructions:
========================================

1. Increment app.version value in build.properties.

2. Rebuild project in IntelliJ IDEA (Build -> Rebuild Project).

3. Run ant target "build-prod-jar".
   -- NOTE: This target calls the signjar ant task and expects a keystore
            to be setup with an appropriate alias and password
            (see build.xml for details).

4. scp dist/JaneliaTransmogrifier__V2.1.6.jar to wiki:/tmp
   -- NOTE: webstart app is hosted on wiki server,
            replace jar version number with current version

5. ssh wiki

   > cd /usr/local/jboss/jbossweb-1.0.0.GA/server/wiki/deploy-apps/webstart.war/tmog
   > su jboss
   > cp /tmp/JaneliaTransmogrifier__V2.1.6.jar . # replace jar version number with current version
   > rm JaneliaTransmogrifier__V2.1.4.jar        # remove n-2 version of jar file (keep n-1 around just in case)
   > vi transmogrifier.jnlp
     # update jar version number:
     #   <jar href="JaneliaTransmogrifier.jar" version="2.1.6" main="true" download="eager"/>

   > exit
   > rm /tmp/JaneliaTransmogrifier__V2.1.6.jar # remove temp copy of jar file
   > exit

6. Test launch of app from lab share and make sure latest version is loaded.

========================================
JNLP and Configuration File Deployment:
========================================

The following files:

  transmogrifier.jnlp
  transmogrifier_config.xsd
  transmogrifier_config_<lab>.xml

are deployed for the following lab groups:

  baker    /groups/baker/bakerlab/tmog
  leet     /groups/leet/leetlab/tmog
  rubin    /groups/rubin/data1/rubinlab/renamer
  simpson  /groups/simpson/MicroscopeData/renamer

