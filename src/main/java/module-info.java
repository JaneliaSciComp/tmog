module org.janelia.tmog {
    requires java.base;
    requires java.desktop;
    requires java.sql;
    requires java.xml.bind;
//    requires java.activation;
//    requires jaxb.runtime;
    requires commons.codec;
    requires commons.digester;
    requires commons.httpclient;
    requires commons.logging;
    requires forms.rt;
    requires gson;
    requires glazedlists.java15;
    requires log4j;
//    requires org.apache.logging.log4j;
    requires swing.worker;
    requires mysql.connector.java;

    opens org.janelia.it.ims.tmog.config;
    opens org.janelia.it.ims.tmog.config.output;
    opens org.janelia.it.ims.tmog.config.preferences;
    opens org.janelia.it.ims.tmog.field;
    opens org.janelia.it.ims.tmog.filefilter;
    opens org.janelia.it.ims.tmog.plugin;
    opens org.janelia.it.ims.tmog.plugin.dataFile;
    opens org.janelia.it.ims.tmog.plugin.imagedb;
}