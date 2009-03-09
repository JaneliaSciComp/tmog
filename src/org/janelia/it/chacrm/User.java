/*
 * Copyright 2009 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */
package org.janelia.it.chacrm;

import org.janelia.it.ims.tmog.plugin.PluginDataRow;

/**
 * Simple class to hold renamer's identity.
 *
 * @author Eric Trautman
 */
public class User {
    private String name;

    public User(String name) {
        if ((name == null) || (name.length() == 0)) {
            this.name = System.getProperty("user.name");
        } else {
            this.name = name;
        }
    }

    public static User getUser(PluginDataRow row) {
        String renamer = null;
        if (row != null) {
            renamer = row.getCoreValue("Renamer");
        }
        return new User(renamer);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "User{" +
               "name='" + name + '\'' +
               '}';
    }
}
