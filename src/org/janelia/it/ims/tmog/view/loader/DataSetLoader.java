/*
 * Copyright (c) 2016 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view.loader;

import org.janelia.it.ims.tmog.field.HttpValidValueModel;
import org.janelia.it.utils.BackgroundWorker;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Loads list of data sets in background thread.
 *
 * @author Eric Trautman
 */
public class DataSetLoader
        extends BackgroundWorker<Void, String> {

    private Set<String> dataSetNames;

    public DataSetLoader() {
        this.dataSetNames = new LinkedHashSet<>();
    }

    public Set<String> getDataSetNames() {
        return dataSetNames;
    }

    public boolean hasDataSet(String dataSetName) {
        return dataSetNames.contains(dataSetName);
    }

    @Override
    protected Void executeBackgroundOperation()
            throws Exception {

        dataSetNames.clear();

        // For now, use data sets loaded and cached at tmog startup.
        // Pulling from SAGE and filtering by family is possible (see below), but much slower.

        final HttpValidValueModel model = new HttpValidValueModel();

        model.setGlobalValueFilter("/^((?!leet).)*$/");
        model.setValueCreationPath("*/dataSet");
        model.setRelativeActualValuePath("dataSetIdentifier");
        model.setRelativeValueDisplayNamePath("dataSetIdentifier");
        model.setServiceUrl("http://jacs-data.int.janelia.org:8180/rest-v1/data/dataSet/sage?sageSync=true");

        model.retrieveAndSetValidValues();

        if (! isCancelled()) {
            for (int i = 0; i < model.getSize(); i++) {
                dataSetNames.add(model.getElementAt(i).getValue());
            }
        }

        return null;
    }

//    @Override
//    protected Void executeBackgroundOperation()
//            throws Exception {
//
//        final SageImageDao dao = new SageImageDao("sage");
//        dataSetNames = dao.getDataSetsForFamily(family);
//        return null;
//    }

}
