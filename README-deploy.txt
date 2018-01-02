========================================
Build and Deployment Instructions:
========================================

1. Increment app.version value in build.properties and
   in JaneliaTransmogrifier.java.

2. Rebuild project in IntelliJ IDEA (Build -> Rebuild Project).

3. Run ant target "build-prod-jar".
   -- NOTE: This target calls the signjar ant task and expects a keystore
            to be setup with an appropriate alias and password
            (see build.xml for details).

4. scp dist/JaneliaTransmogrifier__Va.b.c.jar to apps.int.janelia.org:/tmp
   -- NOTE: webstart app is hosted on same VM (vm144) as sage web service,

5. ssh apps.int.janelia.org

   > cd /opt/local/webstart/tmog4
   > su jbossadmin
   > cp /tmp/JaneliaTransmogrifier__Va.b.c.jar old                       # save versioned jar in 'old' sub-directory
   > cp old/JaneliaTransmogrifier__Va.b.c.jar JaneliaTransmogrifier.jar  # copy versioned jar to generically named instance
   > exit
   > rm /tmp/JaneliaTransmogrifier__Va.b.c.jar                           # remove temp copy of jar file
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

/groups/ditp/ditp/tmog/config
/groups/larvalolympiad/larvalolympiad/tmog/config
/groups/simpson/MicroscopeData/tmog/config
/groups/svoboda/wdbp/tmog/config
/groups/zlatic/zlaticlab/tmog/config
/groups/flylight/flylight/tmog/config
/tier2/leet/leetconfocal/tmog/config
/tier2/leet/leetimg/leetlab/tmog/config
/tier2/rubin/data1/rubinlab/tmog/config
