/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view.loader;

import org.janelia.it.ims.tmog.config.ConfigurationException;
import org.janelia.it.ims.tmog.plugin.imagedb.SageImageDao;
import org.janelia.it.ims.tmog.plugin.imagedb.SageImageDataDefaultValue;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.utils.BackgroundWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Retrieves list of slide image names (relative paths) and
 * then caches meta data for each image in background thread.
 *
 * @author Eric Trautman
 */
public class SlideImageDataLoader
        extends BackgroundWorker<Void, String> {

    private String family;
    private String dataSet;
    private String slide;
    private String objective;

    private SageImageDataDefaultValue imageDataDefaultValue;

    private List<FileTarget> imagePaths;

    public SlideImageDataLoader(String family,
                                String dataSet,
                                String slide,
                                String objective) throws IllegalStateException {

        this.family = family;
        this.dataSet = dataSet;
        this.slide = slide;
        this.objective = objective;

        this.imageDataDefaultValue = new SageImageDataDefaultValue();
        Map<String, String> properties = new HashMap<>();
        properties.put("family", family);
        properties.put("image_property", "cache_me");
        properties.put("baseNameSearch", "true");
        try {
            imageDataDefaultValue.init(properties);
        } catch (ConfigurationException e) {
            throw new IllegalStateException("failed to construct cache accessor for image data", e);
        }

        this.imagePaths = new ArrayList<>();
    }

    public String getDataSet() {
        return dataSet;
    }

    public String getObjective() {
        return objective;
    }

    public String getSlide() {
        return slide;
    }

    public List<FileTarget> getImagePaths() {
        return imagePaths;
    }

    @Override
    protected Void executeBackgroundOperation()
            throws Exception {

        final SageImageDao dao = new SageImageDao("sage");
        final List<String> relativePaths = dao.getImageNamesForSlide(family, dataSet, slide, objective);

        FileTarget target;
        for (String relativePath : relativePaths) {

            target = new FileTarget(new File(relativePath));

            if (isCancelled()) {
                break;
            }

            updateStatus("loading data for " + relativePath);
            cacheImageData(target);
            imagePaths.add(target);
        }

        return null;
    }

    private void updateStatus(String message) {
        if (! isCancelled()) {
            List<String> messages = new ArrayList<>();
            messages.add(message);
            process(messages);
        }
    }

    // Hack to update shared image data cache by requesting it here.
    // This pushes long running data retrieval onto this loader's background thread and
    // works around shortcoming of standard default data operations running on EDT.
    private void cacheImageData(FileTarget target) {
        imageDataDefaultValue.getValue(target);
    }

}
