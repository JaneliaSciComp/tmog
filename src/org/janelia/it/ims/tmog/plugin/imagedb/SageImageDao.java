/*
 * Copyright (c) 2016 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.plugin.ExternalSystemException;
import org.janelia.it.utils.StringUtil;
import org.janelia.it.utils.db.DbConfigException;
import org.janelia.it.utils.db.DbManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class supports management of image data within the SAGE database.
 *
 * @author Eric Trautman
 */
public class SageImageDao
        extends ImageDao {

    public static final String NONE_LAB = "none";
    public static final String NOT_APPLICABLE = "Not_Applicable";
    public static final String UNDEFINED_OBJECTIVE = "undefined";

    /**
     * Constructs a dao using the default manager and configuration.
     *
     * @param  dbConfigurationKey  the key for loading database
     *                             configuration information.
     *
     * @throws ExternalSystemException
     *   if the database configuration information cannot be loaded.
     */
    public SageImageDao(String dbConfigurationKey) throws ExternalSystemException {
        super(dbConfigurationKey);
    }

    /**
     * Verifies that a connection to the database can be established.
     *
     * @throws ExternalSystemException
     *   if a connection to the database can not be established.
     */
    public void checkAvailability() throws ExternalSystemException {
        Connection connection = null;
        try {
            final DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } finally {
            DbManager.closeResources(null, null, connection, LOG);
        }
    }

    /**
     * Writes (inserts or updates) the specified image properties
     * to the image database.
     *
     * @param  image  image to be persisted.
     *
     * @return the specified image with its database id updated.
     *
     * @throws ExternalSystemException
     *   if the save fails.
     */
    public Image saveProperties(Image image) throws ExternalSystemException {
        String relativePath = image.getRelativePath();

        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement select = null;
        PreparedStatement deleteImage = null;

        try {
            final DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);

            String previousRelativePath = image.getPreviousRelativePath();
            if (image.isBeingMoved() &&
                (previousRelativePath != null) &&
                (! relativePath.equals(previousRelativePath))) {
                deleteImage = connection.prepareStatement(SQL_DELETE_IMAGE);
                deleteImage.setString(1, previousRelativePath);

                // HACK: strip off .bz2 in case real file name no longer matches SAGE name
                String previousRelativePathWithoutBz2 = previousRelativePath;
                if (previousRelativePath.endsWith(".bz2")) {
                    previousRelativePathWithoutBz2 =
                            previousRelativePath.substring(0, (previousRelativePath.length() - 4));
                }
                deleteImage.setString(2, previousRelativePathWithoutBz2);

                int rowsUpdated = deleteImage.executeUpdate();
                if (rowsUpdated > 0) {
                    LOG.info("removed " + rowsUpdated +
                             " image row(s) for existing image name " +
                             previousRelativePath +
                             " because it is being renamed to " + relativePath);
                }
            }

            select = connection.prepareStatement(SQL_SELECT_IMAGE_PROPERTY_TYPES);
            select.setString(1, relativePath);
            resultSet = select.executeQuery();

            Integer imageId = null;
            Integer lastImageId = null;
            List<String> existingPropertyNames = new ArrayList<>();
            String propertyName;
            while (resultSet.next()) {
                imageId = resultSet.getInt(1);
                if ((lastImageId != null) && (! lastImageId.equals(imageId))) {
                    throw new ExternalSystemException(
                            "Database contains multiple images with the " +
                            "same relative path: '"  + relativePath + "'.");
                }
                propertyName = resultSet.getString(2);
                if (StringUtil.isDefined(propertyName)) {
                    existingPropertyNames.add(propertyName);
                }
                lastImageId = imageId;
            }

            Integer lineId = null;
            String lineName = image.getLineName();
            if (lineName == null) {

                if (imageId == null) {
                    throw new ExternalSystemException(
                            "Line name must be provided for new images.");
                }

            } else {

                final String defaultLineLabName =
                        deriveLineLabName(image.getLabName(), lineName);
                lineId = getLineId(lineName, defaultLineLabName, connection);
                if (lineId == null) {
                    throw new ExternalSystemException(
                            "Line '" + lineName + "' does not exist.");
                }

            }

            if (imageId == null) {
                image = addImage(image, lineId, connection);
            } else {
                image.setId(imageId);
                updateImage(image, lineId, connection);
                if (existingPropertyNames.size() > 0) {
                    updateImageProperties(image,
                                          connection,
                                          existingPropertyNames);
                }
            }

            connection.commit();

        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to store image properties for '" +
                    relativePath + "'.", e);
        } finally {
            DbManager.closeResources(null, deleteImage, null, LOG);
            DbManager.closeResources(resultSet, select, connection, LOG);
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("successfully persisted image properties to the '" +
                     getDbConfigurationKey() + "' database: " + image);
        }

        return image;
    }

    /**
     * @param  relativePath  the relative path for the desired image.
     *
     * @return the image identifier for the specified path
     *         (or null if the path is not found).
     *
     * @throws ExternalSystemException
     *   if the retrieval fails.
     */
    public Integer getImageId(String relativePath) throws ExternalSystemException {

        Integer imageId = null;
        Connection connection = null;

        try {
            final DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            imageId = getImageId(relativePath, connection);
        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException(
                    "Failed to retrieve image id for '" +
                    relativePath + "'.", e);
        } finally {
            DbManager.closeResources(null, null, connection, LOG);
        }

        LOG.info("getImageId: returning " + imageId + " for " + relativePath);

        return imageId;
    }

//    /**
//     * @return distinct data sets associated with images in the specified family.
//     */
//    public Set<String> getDataSetsForFamily(String family)
//            throws ExternalSystemException {
//
//        Set<String> dataSets = new LinkedHashSet<>();
//
//        Connection connection = null;
//        PreparedStatement select = null;
//        ResultSet resultSet = null;
//
//        try {
//            final DbManager dbManager = getDbManager();
//            connection = dbManager.getConnection();
//            select = connection.prepareStatement(SQL_SELECT_FAMILY_DATA_SETS);
//            select.setString(1, family);
//            resultSet = select.executeQuery();
//
//            while (resultSet.next()) {
//                dataSets.add(resultSet.getString(1));
//            }
//        } catch (DbConfigException e) {
//            throw new ExternalSystemException(e.getMessage(), e);
//        } catch (SQLException e) {
//            throw new ExternalSystemException("Failed to retrieve data sets for family '" + family + "'.  \n" +
//                                              e.getMessage(), e);
//        } finally {
//            DbManager.closeResources(resultSet, select, connection, LOG);
//        }
//
//        return dataSets;
//    }

    /**
     * @return  distinct slide and objectives for all images in the specified family and data set.
     */
    public Map<String, List<String>> getSlideToObjectiveMapForDataSet(String family,
                                                                      String dataSet)
            throws ExternalSystemException {

        Map<String, List<String>> slideToObjectivesMap = new LinkedHashMap<>();

        Connection connection = null;
        PreparedStatement select = null;
        ResultSet resultSet = null;

        try {
            final DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            select = connection.prepareStatement(SQL_SELECT_DATA_SET_SLIDES);
            select.setString(1, family);
            select.setString(2, dataSet);
            resultSet = select.executeQuery();

            String slide;
            String objective;
            List<String> objectiveList;
            while (resultSet.next()) {
                slide = resultSet.getString(1);
                objective = resultSet.getString(2);
                objectiveList = slideToObjectivesMap.get(slide);
                if (objectiveList == null) {
                    objectiveList = new ArrayList<>();
                    slideToObjectivesMap.put(slide, objectiveList);
                }
                if (objective == null) {
                    objective = UNDEFINED_OBJECTIVE;
                }
                objectiveList.add(objective);
            }
        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException("Failed to retrieve slide data for data set '" + dataSet + "'.  \n" +
                                              e.getMessage(), e);
        } finally {
            DbManager.closeResources(resultSet, select, connection, LOG);
        }

        return slideToObjectivesMap;
    }

    /**
     * @return image names (relative paths) for all images with the specified family, data set, slide, and objective.
     */
    public List<String> getImageNamesForSlide(String family,
                                              String dataSet,
                                              String slide,
                                              String objective)
            throws ExternalSystemException {

        List<String> imageNames = new ArrayList<>();

        Connection connection = null;
        PreparedStatement select = null;
        ResultSet resultSet = null;

        try {
            final DbManager dbManager = getDbManager();
            connection = dbManager.getConnection();
            select = connection.prepareStatement(SQL_SELECT_SLIDE_IMAGE_NAMES);
            select.setString(1, family);
            select.setString(2, dataSet);
            select.setString(3, slide + "\\_%");
            resultSet = select.executeQuery();

            if (objective == null) {
                objective = UNDEFINED_OBJECTIVE;
            }
            final boolean findUnknownObjectives = UNDEFINED_OBJECTIVE.equals(objective);

            String imageName;
            String imageObjective;
            while (resultSet.next()) {

                // filter images with matching objectives here instead of in sql since we have deal with nulls
                imageName = resultSet.getString(1);
                imageObjective = resultSet.getString(2);
                if (imageObjective == null) {
                    if (findUnknownObjectives) {
                        imageNames.add(imageName);
                    }
                } else if (imageObjective.contains(objective)) {
                    imageNames.add(imageName);
                }
            }

        } catch (DbConfigException e) {
            throw new ExternalSystemException(e.getMessage(), e);
        } catch (SQLException e) {
            throw new ExternalSystemException("Failed to retrieve image names for slide '" + slide +
                                              "' and objective '" + objective + "'.  \n" +
                                              e.getMessage(), e);
        } finally {
            DbManager.closeResources(resultSet, select, connection, LOG);
        }

        return imageNames;
    }

    @Override
    protected String getSelectImageDataSql() {
        return
            "SELECT i.id, i.name, i.capture_date, i.display, i.created_by, p.type, p.value, i.line " +
            "FROM image_vw i LEFT JOIN image_property_vw p " +
            "ON (i.id=p.image_id) WHERE i.family=? AND i.name like ?";
    }

    private Integer getImageId(String relativePath,
                               Connection connection)
            throws SQLException, ExternalSystemException {

        Integer imageId = null;
        PreparedStatement select = null;
        ResultSet resultSet = null;

        try {
            select = connection.prepareStatement(SQL_SELECT_IMAGE_ID);
            select.setString(1, relativePath);
            resultSet = select.executeQuery();
            if (resultSet.next()) {
                imageId = resultSet.getInt(1);
            }
        } finally {
            DbManager.closeResources(resultSet, select, null, LOG);
        }

        return imageId;
    }

    private Image addImage(Image image,
                           Integer lineId,
                           Connection connection)
            throws SQLException, ExternalSystemException {

        String relativePath = image.getRelativePath();

        PreparedStatement insertImage = null;
        try {
            insertImage = connection.prepareStatement(SQL_INSERT_IMAGE);
            insertImage.setString(1, relativePath);
            Date captureDate = image.getCaptureDate();
            if (captureDate != null) {
                insertImage.setDate(2,
                                    new java.sql.Date(captureDate.getTime()));
            } else {
                insertImage.setDate(2, null);
            }
            insertImage.setString(3, image.getFamily());
            insertImage.setBoolean(4, image.isDisplay());
            insertImage.setString(5, image.getSource());
            insertImage.setInt(6, lineId);
            insertImage.setBoolean(7, image.isRepresentative());
            insertImage.setString(8, image.getUrl());
            insertImage.setString(9, image.getPath());
            insertImage.setString(10, image.getCreatedBy());

            int rowsUpdated = insertImage.executeUpdate();
            if (rowsUpdated != 1) {
                throw new ExternalSystemException(
                        "Failed to create image '" + relativePath +
                        "'.  Attempted to create " + rowsUpdated + " rows.");
            }

            Integer imageId = getImageId(relativePath, connection);
            if (imageId == null) {
                throw new ExternalSystemException(
                        "Failed to retrieve id for image '" + relativePath +
                        "'.");
            }
            image.setId(imageId);
            updateImageProperties(image, connection, new ArrayList<String>());

        } finally {
            DbManager.closeResources(null, insertImage, null, LOG);
        }

        return image;
    }

    private void updateImage(Image image,
                             Integer lineId,
                             Connection connection)
            throws SQLException, ExternalSystemException {

        String relativePath = image.getRelativePath();

        PreparedStatement updateImage = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE image SET ");

            Date captureDate = image.getCaptureDate();
            if (captureDate != null) {
                sql.append("capture_date=?, ");
            }

            String family = image.getFamily();
            if (family != null) {
                sql.append("family_id=(" + SQL_SELECT_FAMILY_ID + "), ");
            }

            String url = image.getUrl();
            if (url != null) {
                sql.append("url=?, ");
            }

            String path = image.getPath();
            if (path != null) {
                sql.append("path=?, ");
            }

            String createdBy = image.getCreatedBy();
            if (createdBy != null) {
                sql.append("created_by=?, ");
            }

            // TODO: support update of source name and representative columns
            sql.append("name=?, display=?");
            if (lineId != null) {
                sql.append(", line_id=?");
            }
            sql.append(" WHERE id=?");

            int columnIndex = 1;
            updateImage = connection.prepareStatement(sql.toString());
            if (captureDate != null) {
                updateImage.setDate(columnIndex,
                                    new java.sql.Date(captureDate.getTime()));
                columnIndex++;
            }
            if (family != null) {
                updateImage.setString(columnIndex, image.getFamily());
                columnIndex++;
            }
            if (url != null) {
                updateImage.setString(columnIndex, url);
                columnIndex++;
            }
            if (path != null) {
                updateImage.setString(columnIndex, path);
                columnIndex++;
            }
            if (createdBy != null) {
                updateImage.setString(columnIndex, createdBy);
                columnIndex++;
            }
            updateImage.setString(columnIndex, relativePath);
            columnIndex++;
            updateImage.setBoolean(columnIndex, image.isDisplay());
            columnIndex++;
            if (lineId != null) {
                updateImage.setInt(columnIndex, lineId);
                columnIndex++;
            }
            updateImage.setInt(columnIndex, image.getId());

            int rowsUpdated = updateImage.executeUpdate();
            if (rowsUpdated != 1) {
                throw new ExternalSystemException(
                        "Failed to update image '" + relativePath +
                        "'.  Attempted to update " + rowsUpdated + " rows.");
            }

            LOG.info("updateImage: successfully executed " + sql.toString());

        } finally {
            DbManager.closeResources(null, updateImage, null, LOG);
        }
    }

    private void updateImageProperties(Image image,
                                       Connection connection,
                                       List<String> existingPropertyNames)
            throws SQLException, ExternalSystemException {

        int imageId = image.getId();
        String relativePath = image.getRelativePath();

        Set<String> insertPropertyNames = new LinkedHashSet<>();
        Set<String> updatePropertyNames = new LinkedHashSet<>();

        Map<String, String> properties =
                image.getPropertyTypeToValueMapForSage();
        for (String type : properties.keySet()) {
            if (StringUtil.isDefined(type)) {
                if (existingPropertyNames.contains(type)) {
                    updatePropertyNames.add(type);
                } else {
                    insertPropertyNames.add(type);
                }
            }
        }

        int numberOfPropertiesToAdd = insertPropertyNames.size();
        if (numberOfPropertiesToAdd > 0) {
            PreparedStatement insertProperty = null;

            try {

                insertProperty =
                        connection.prepareStatement(SQL_INSERT_IMAGE_PROPERTY);
                for (String type : insertPropertyNames) {
                    insertProperty.setInt(1, imageId);
                    insertProperty.setString(2, type);
                    insertProperty.setString(3, properties.get(type));
                    insertProperty.addBatch();
                }

                int[] numUpdates = insertProperty.executeBatch();
                validateUpdateCounts("Failed to add image properties for '" +
                                     relativePath + "'.",
                                     numberOfPropertiesToAdd,
                                     numUpdates);

                LOG.info("updateImageProperties: successfully added " +
                         insertPropertyNames);

            } finally {
                DbManager.closeResources(null, insertProperty, null, LOG);
            }
        }

        int numberOfPropertiesToUpdate = updatePropertyNames.size();
        if (numberOfPropertiesToUpdate > 0) {
            PreparedStatement updateProperty = null;

            try {

                updateProperty =
                        connection.prepareStatement(SQL_UPDATE_IMAGE_PROPERTY);
                for (String type : updatePropertyNames) {
                    updateProperty.setString(1, properties.get(type));
                    updateProperty.setInt(2, imageId);
                    updateProperty.setString(3, type);
                    updateProperty.addBatch();
                }

                int[] numUpdates = updateProperty.executeBatch();
                validateUpdateCounts("Failed to update image properties for '" +
                                     relativePath + "'.",
                                     numberOfPropertiesToUpdate,
                                     numUpdates);

                LOG.info("updateImageProperties: successfully updated " +
                         updatePropertyNames);

            } finally {
                DbManager.closeResources(null, updateProperty, null, LOG);
            }
        }

    }

    protected Integer getLineId(String lineName,
                                String defaultLineLabName,
                                Connection connection)
            throws SQLException, ExternalSystemException {

        Integer lineId = null;
        String lab;

        PreparedStatement select = null;
        ResultSet resultSet = null;
        try {
            select = connection.prepareStatement(SQL_SELECT_LINE_ID);
            select.setString(1, lineName);
            resultSet = select.executeQuery();
            int count = 0;

            Map<String, Integer> labToLineNameMap = new LinkedHashMap<>();

            while (resultSet.next()) {
                lineId = resultSet.getInt(1);
                lab = resultSet.getString(2);
                labToLineNameMap.put(lab, lineId);
                count++;
            }

            if (count > 1) {
                if (labToLineNameMap.containsKey(defaultLineLabName)) {
                    lineId = labToLineNameMap.get(defaultLineLabName);
                } else {
                    lab = null;
                    for (String orderedLabName : LINE_LAB_PRECEDENCE) {
                        if (labToLineNameMap.containsKey(orderedLabName)) {
                            lab = orderedLabName;
                            lineId = labToLineNameMap.get(orderedLabName);
                            break;
                        }
                    }
                    if (lab == null) {
                        //noinspection LoopStatementThatDoesntLoop
                        for (String retrievedLabName : labToLineNameMap.keySet()) {
                            lab = retrievedLabName;
                            lineId = labToLineNameMap.get(lab);
                            break;
                        }
                    }
                    LOG.warn("Multiple lines exist with the name '" + lineName +
                             "' but none of them are owned by the '" + defaultLineLabName + "' lab.  " +
                             "Selected the instance owned by the '" + lab + "' lab.");
                }
            }

        } finally {
            DbManager.closeResources(resultSet, select, null, LOG);
        }

        return lineId;
    }

    private void validateUpdateCounts(String failureContext,
                                      int expectedNumberOfUpdates,
                                      int[] numUpdates)
            throws ExternalSystemException {
        if (numUpdates.length != expectedNumberOfUpdates) {
            throw new ExternalSystemException(
                    failureContext + "  Executed " +
                    numUpdates.length + " instead of " +
                    expectedNumberOfUpdates + " statement(s).");
        } else {
            for (int i = 0; i < numUpdates.length; i++) {
                if (numUpdates[i] != 1) {
                    throw new ExternalSystemException(
                            failureContext + "  Attempted to update " +
                            numUpdates[i] + " row(s) for property " +
                            i + ".");
                }
            }
        }
    }

    private String deriveLineLabName(String imageLabName,
                                     String lineName) {
        String lineLabName = imageLabName;
        if (NOT_APPLICABLE.equals(lineName)) {
            lineLabName = NONE_LAB;
        }
        return lineLabName;
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(SageImageDao.class);

    private static final String SQL_SELECT_FAMILY_ID =
            "SELECT id FROM cv_term_vw " +
            "WHERE cv='family' AND cv_term=?";    // 1. family name

    private static final String SQL_SELECT_LAB_ID =
            "SELECT id FROM lab_vw WHERE lab=?";  // 1. lab name

    // usage of getCvTermID function supports light_imagery cv_relationships (e.g. to fly)
    private static final String SQL_SELECT_IMAGE_TYPE_ID =
            "getCvTermID('light_imagery',?,NULL)";  // 1. image type name

    private static final String SQL_INSERT_IMAGE =
            "INSERT INTO image (name, capture_date, family_id, display, " +
            "source_id, line_id, representative, url, path, created_by) " +
            "VALUES (" +
            "?, " +                                // 1. relative path
            "?, " +                                // 2. capture date
            "(" + SQL_SELECT_FAMILY_ID + "), " +   // 3. family
            "?, " +                                // 4. display
            "(" + SQL_SELECT_LAB_ID + "), " +      // 5. source
            "?, " +                                // 6. line id
            "?, " +                                // 7. representative
            "?, " +                                // 8. url
            "?, " +                                // 9. path
            "?)";                                  // 10. created_by

    private static final String SQL_SELECT_IMAGE_ID =
            "SELECT id FROM image WHERE name=?"; // 1. image's relative path

//    private static final String SQL_SELECT_FAMILY_DATA_SETS =
//            "select distinct data_set.value from image_property data_set " +
//            "where data_set.type_id=5766 and data_set.image_id in (select id from image_vw where family=?) " +
//            "order by data_set.value";

    // This query did not perform as well as the one above on dev-db.
    // Keeping it here for reference ...

//    private static final String SQL_SELECT_FAMILY_DATA_SETS_2 =
//            "select distinct data_set.value from image i " +
//            "join image_vw image_view on image_view.id=i.id and image_view.family=? " +
//            "join image_property data_set on data_set.image_id=i.id and data_set.type_id=5766 " +
//            "order by data_set.value";

    private static final String SQL_SELECT_DATA_SET_SLIDES =
            "select distinct substring(slide_code.value, " +
            "                          1, " +
            "                          locate('_', " +
            "                                 slide_code.value, " +
            "                                 locate('_', slide_code.value) + 1) - 1) as slide, " +
            "       objective.value as objective " +
            "from image_property slide_code " +
            "join image_vw image_view on image_view.id=slide_code.image_id and " +
            "                            image_view.family=? " +   // 1. family
            "join image_property data_set on data_set.image_id=slide_code.image_id and data_set.type_id=5766 and " +
            "                                data_set.value=? " +  // 2. data set
            "left join image_property objective on objective.image_id=slide_code.image_id and objective.type_id=33 " +
            "where slide_code.type_id=3654 " +
            "order by slide_code.value, objective.value";

    private static final String SQL_SELECT_SLIDE_IMAGE_NAMES =
            "select i.name, objective.value from image i " +
            "join image_vw image_view on image_view.id=i.id and " +
            "                            image_view.family=? " +           // 1. family
            "join image_property data_set on data_set.image_id=i.id and data_set.type_id=5766 and " +
            "                                data_set.value=? " +          // 2. data set
            "join image_property slide_code on slide_code.image_id=i.id and slide_code.type_id=3654 and " +
            "                                  slide_code.value like ? " + // 3. slide
            "left join image_property objective on objective.image_id=i.id and objective.type_id=33 " +
            "order by slide_code.value, i.capture_date, i.name";

    private static final String SQL_INSERT_IMAGE_PROPERTY =
            "INSERT INTO image_property (image_id, type_id, value) VALUES (" +
            "?, " +                                    // 1. image id
            "(" + SQL_SELECT_IMAGE_TYPE_ID + "), " +   // 2. property type name
            "?)";                                      // 3. property value

    private static final String SQL_UPDATE_IMAGE_PROPERTY =
            "UPDATE image_property SET value=? " +        // 1. property value
            "WHERE image_id=? AND " +                     // 2. image id
            "type_id=(" + SQL_SELECT_IMAGE_TYPE_ID + ")"; // 3. property type name

    /**
     * SQL for retrieving an image's property types.
     *   Parameter 1 is the image's relative path.
     */
    private static final String SQL_SELECT_IMAGE_PROPERTY_TYPES =
            "SELECT i.id, ipv.type FROM image i " +
            "LEFT OUTER JOIN image_property_vw ipv ON i.id=ipv.image_id " +
            "WHERE i.name=?";

    /**
     * SQL for deleting all properties for an image.
     *   Parameter 1 is the image name.
     *   Parameter 2 is the image name without a .bz2 suffix.
     */
    private static final String SQL_DELETE_IMAGE =
            "DELETE FROM image WHERE name in (?,?)";

    /**
     * SQL for retrieving a line id.
     *   Parameter 1 is the line name.
     */
    private static final String SQL_SELECT_LINE_ID =
            "SELECT l.id, c.name FROM line l, cv_term c " +
            "WHERE l.name=? AND c.id=l.lab_id";

    /** Order of precedence for identifying lines associated with multiple labs. */
    private static final String[] LINE_LAB_PRECEDENCE = {
            "fly", "flylight", "rubin", "simpson", "zlatic", "baker", "heiberlein", "riddiford", "olympiad"
    };

}