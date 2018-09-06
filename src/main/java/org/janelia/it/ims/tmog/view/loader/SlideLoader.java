/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view.loader;

import org.janelia.it.ims.tmog.plugin.imagedb.SageImageDao;
import org.janelia.it.utils.BackgroundWorker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads slide and objective information for the selected data set in background thread.
 *
 * @author Eric Trautman
 */
public class SlideLoader extends BackgroundWorker<Void, String> {

    private String family;
    private String dataSet;
    private Map<String, List<String>> slideToObjectivesMap;

    public SlideLoader(String family,
                       String dataSet) {
        this.family = family;
        this.dataSet = dataSet;
        this.slideToObjectivesMap = new LinkedHashMap<>();
    }

    public boolean foundSlides() {
        return slideToObjectivesMap.size() > 0;
    }

    public boolean hasSlide(String slide) {
        return slideToObjectivesMap.containsKey(slide);
    }

    public Set<String> getSlides() {
        return slideToObjectivesMap.keySet();
    }

    public List<String> getObjectiveList(String slide) {
        return slideToObjectivesMap.get(slide);
    }

    @Override
    protected Void executeBackgroundOperation()
            throws Exception {
        final SageImageDao dao = new SageImageDao("sage");
        slideToObjectivesMap = dao.getSlideToObjectiveMapForDataSet(family, dataSet);
        simplifyObjectives();
        return null;
    }

    /**
     * Simplifies objective names, reducing names like 'Plan-Apochromat 20x/0.8 M27' and
     * 'tmog fix 20x objective' to just '20x'.
     */
    private void simplifyObjectives() {

        Map<String, List<String>> simplifiedMap = new LinkedHashMap<>(slideToObjectivesMap.size() * 2);

        TreeSet<String> simplifiedObjectiveSet;
        String simplifiedObjective;
        Matcher m;
        for (String slide : slideToObjectivesMap.keySet()) {
            simplifiedObjectiveSet = new TreeSet<>();
            for (String objective : slideToObjectivesMap.get(slide)) {
                simplifiedObjective = objective;
                m = OBJECTIVE_PATTERN.matcher(objective);
                if (m.matches()) {
                    simplifiedObjective = m.group(1);
                }
                simplifiedObjectiveSet.add(simplifiedObjective);
            }
            simplifiedMap.put(slide, new ArrayList<>(simplifiedObjectiveSet));
        }

        slideToObjectivesMap = simplifiedMap;
    }

    private static final Pattern OBJECTIVE_PATTERN = Pattern.compile(".*(\\d{2}x).*");
}
