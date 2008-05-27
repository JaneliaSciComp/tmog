/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.view.component;

/**
 * Simple utility class to manage the set of text associated with
 * the different states of a task execution button.
 *
 * @author Eric Trautman
 */
public class TaskButtonText {

    private static final String START_TEXT = "Save";
    private static final String START_TOOL_TIP_TEXT =
            "Save specified information.";
    private static final String CANCEL_TEXT =
            "Cancel Task In Progress";
    private static final String CANCEL_TOOL_TIP_TEXT =
            "Cancel the task that is currently running.";
    private static final String CANCELLED_TEXT =
            "Task Cancelled";
    private static final String CANCELLED_TOOL_TIP_TEXT =
            "Waiting for current target processing to complete.";

    private static final String RENAME_START_TEXT = "Copy and Rename";
    private static final String RENAME_START_TOOL_TIP_TEXT =
            "Copy and rename all files using specified information";
    private static final String RENAME_CANCEL_TEXT =
            "Cancel Rename In Progress";
    private static final String RENAME_CANCEL_TOOL_TIP_TEXT =
            "Cancel the renaming process that is currently running";
    private static final String RENAME_CANCELLED_TEXT =
            "Rename Session Cancelled";
    private static final String RENAME_CANCELLED_TOOL_TIP_TEXT =
            "Waiting for current file processing to complete";

    /** The default task button values. */
    public static final TaskButtonText DEFAULT =
            new TaskButtonText(START_TEXT, START_TOOL_TIP_TEXT,
                               CANCEL_TEXT, CANCEL_TOOL_TIP_TEXT,
                               CANCELLED_TEXT, CANCELLED_TOOL_TIP_TEXT);

    /** The rename task button values. */
    public static final TaskButtonText RENAME =
            new TaskButtonText(RENAME_START_TEXT, RENAME_START_TOOL_TIP_TEXT,
                               RENAME_CANCEL_TEXT, RENAME_CANCEL_TOOL_TIP_TEXT,
                               RENAME_CANCELLED_TEXT,
                               RENAME_CANCELLED_TOOL_TIP_TEXT);

    private String startText;
    private String startToolTipText;
    private String cancelText;
    private String cancelToolTipText;
    private String cancelledText;
    private String cancelledToolTipText;

    public TaskButtonText(String startText,
                          String startToolTipText,
                          String cancelText,
                          String cancelToolTipText,
                          String cancelledText,
                          String cancelledToolTipText) {
        this.startText = startText;
        this.startToolTipText = startToolTipText;
        this.cancelText = cancelText;
        this.cancelToolTipText = cancelToolTipText;
        this.cancelledText = cancelledText;
        this.cancelledToolTipText = cancelledToolTipText;
    }

    public String getStartText() {
        return startText;
    }

    public String getStartToolTipText() {
        return startToolTipText;
    }

    public String getCancelText() {
        return cancelText;
    }

    public String getCancelToolTipText() {
        return cancelToolTipText;
    }

    public String getCancelledText() {
        return cancelledText;
    }

    public String getCancelledToolTipText() {
        return cancelledToolTipText;
    }
}