/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.projects.dinsic.wikidemarches.extensions.batchimports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.batchimport.BatchImport;
import org.xwiki.batchimport.BatchImportConfiguration;
import org.xwiki.batchimport.RowDataPostprocessor;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.text.StringUtils;

// TODO: consider firing an error that blocks the import as soon as an unexpected value is found, in order to avoid
// importing invalid data
@Component("demarche")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DemarcheRowDataPostprocessor implements RowDataPostprocessor
{
    @Inject
    private Logger logger;

    @Inject
    protected BatchImport batchImport;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    public static String DEMARCHE_PROPERTY_VOLUMETRIE = "volumetrie";

    public static String DEMARCHE_PROPERTY_VOLUMETRIE_DEMATERIALISATION = "volumetrieDemat";

    public static String DEMARCHE_PROPERTY_FRANCE_CONNECT = "franceConnect";

    public static String DEMARCHE_PROPERTY_ADAPTE_MOBILE = "adapteMobile";

    public static String DEMARCHE_PROPERTY_ACCOMPAGNEMENT = "accompagnement";

    public static String DEMARCHE_PROPERTY_MOYENS_DE_CONTACT = "moyensDeContact";

    public static String DEMARCHE_PROPERTY_URL = "urlDemarche";

    public static String DEMARCHE_PROPERTY_UPTIME = "urlAvailability";

    public static String DEMARCHE_PROPERTY_RGAA_STATEMENT = "accessibilityStatement";

    public static String DEMARCHE_PROPERTY_RGAA_COMPLIANCE_LEVEL = "rgaaCompliancyLevel";

    public static String DEMARCHE_PROPERTY_DLNUF = "ditesLeNousUneFois";

    public static String DEMARCHE_PROPERTY_AVIS_INTEGRATION = "avisIntegration";

    public static String DEMARCHE_PROPERTY_AVIS_EXEMPTION = "avisExemption";

    public static String DEMARCHE_PROPERTY_EDI_ONLY = "ediOnly";

    public static SimpleDateFormat FORMATTER_DATE_MISE_EN_LIGNE_INPUT = new SimpleDateFormat("MMM-yy");

    public static SimpleDateFormat FORMATTER_DATE_MISE_EN_LIGNE_OUTPUT = new SimpleDateFormat("MM/yyyy");

    public static SimpleDateFormat FORMATTER_DATE_RGAA = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

    /**
     * {@inheritDoc}
     *
     * @see RowDataPostprocessor#postProcessRow(Map, List, int, Map, List, BatchImportConfiguration)
     */
    @Override
    public void postProcessRow(Map<String, String> data, List<String> row, int rowIndex, Map<String, String> mapping,
        List<String> headers, BatchImportConfiguration config)
    {
        logger.debug("Post processing demarche data: " + data);

        trimAllValues(data);

        // no more processing for the statutDemat, it is handled properly by the list preprocessor

        // no more processing of the opening date, we'll just import it as pure text, the date itself is to be done in a
        // next version (if still needed)

        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_VOLUMETRIE))
            || StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_VOLUMETRIE_DEMATERIALISATION))) {
            processVolumetrieNumbers(data);
        }

        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_FRANCE_CONNECT))) {
            // only cleanup dash, the rest is handled by the list preprocessor
            cleanupDash(data, DEMARCHE_PROPERTY_FRANCE_CONNECT);
        }

        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_ADAPTE_MOBILE))) {
            // only cleanup dash, the rest is handled by the list preprocessor
            cleanupDash(data, DEMARCHE_PROPERTY_ADAPTE_MOBILE);
        }

        processSupportDeQualite(data, mapping);

        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_UPTIME))) {
            cleanupPercentage(data, DEMARCHE_PROPERTY_UPTIME);
        }

        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_RGAA_COMPLIANCE_LEVEL))) {
            cleanupPercentage(data, DEMARCHE_PROPERTY_RGAA_COMPLIANCE_LEVEL);
        }

        processRGAAStatement(data, mapping);

        processDLNUF(data, mapping);

        processUrl(data, mapping);

        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_AVIS_INTEGRATION))) {
            processFlagForProperty(data, DEMARCHE_PROPERTY_AVIS_INTEGRATION, "<100votes", "1");
        }

        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_AVIS_EXEMPTION))) {
            processFlagForProperty(data, DEMARCHE_PROPERTY_AVIS_EXEMPTION, "n/a", "1");
        }

        processEdiOnlyFlag(data, mapping);

        logger.debug("New data after processing: " + data);
    }

    /**
     * Trim all values, since some input files sometimes contain values with surrounding spaces (e.g. directions)
     *
     * @param data the data read from the file and mapped, ready to be imported
     */
    protected void trimAllValues(Map<String, String> data)
    {
        logger.debug("Trimming all values from the data row");
        Set<Map.Entry<String, String>> entries = data.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (StringUtils.isNotEmpty(entry.getValue())) {
                entry.setValue(entry.getValue().trim());
            }
        }
    }

    protected String getRowDataByHeader(List<String> row, String header, List<String> headers)
    {
        int index = headers.indexOf(header);
        if (index >= 0 && index < row.size()) {
            return row.get(index);
        } else {
            return null;
        }
    }

    protected String maybeAddLabel(String value, String label)
    {
        if (StringUtils.isNotEmpty(value)) {
            return label.trim() + " : " + value;
        }
        return value;
    }

    /**
     * Cleans up the values for the number columns, "volumetrie" and "volumetrie" demat, by replacing some text values
     * with empty values.
     *
     * @param data the data read from the file and mapped, ready to be imported
     */
    protected void processVolumetrieNumbers(Map<String, String> data)
    {
        logger.debug("Processing volumetrie numbers...");
        String[] propertyNames =
            new String[] {DEMARCHE_PROPERTY_VOLUMETRIE, DEMARCHE_PROPERTY_VOLUMETRIE_DEMATERIALISATION};
        // The logic below only handles specific values of the data, so we don't check if the properties are mapped or
        // not.
        for (String propertyName : propertyNames) {
            String value = data.get(propertyName);
            if ("n/c".equalsIgnoreCase(value) || "na".equalsIgnoreCase(value) || "n/a".equalsIgnoreCase(value)) {
                logger.debug("Skipping volumetrie value " + value + " for property " + propertyName);
                data.put(propertyName, "");
            }
        }
    }

    /**
     * Cleans up the percentages from the value mapped for the given property
     *
     * @param data the data read from the file and mapped, ready to be imported
     * @param propertyName the property for which to perform the cleanup
     */
    protected void cleanupPercentage(Map<String, String> data, String propertyName)
    {
        logger.debug("Cleaning percentage from " + propertyName);
        String value = data.get(propertyName);
        value = value.replace("%", "");
        data.put(propertyName, value);
    }

    /**
     * Transforms dashes in the data into empty value, for the given property.
     *
     * @param data the data read from the file and mapped, ready to be imported
     * @param propertyName the property to perform the cleanup for
     */
    protected void cleanupDash(Map<String, String> data, String propertyName)
    {
        logger.debug("Cleaning dash from " + propertyName);
        if (isDash(data.get(propertyName))) {
            logger.debug("Found and cleaned up dash");
            data.put(propertyName, "");
        }
    }

    /**
     * Checks if this value is equal to dash, including all variations of dash possible (shorter dash, longer, etc.)
     *
     * @param value the value to check
     * @return true if the value is a dash, false otherwise
     */
    protected boolean isDash(String value)
    {
        return ("–".equals(value) || "-".equals(value));
    }

    /**
     * Replace common static list values by their corresponding key in the démarche property.
     *
     * @param value the value to normalize
     * @return the normalized value
     */
    protected String normalizeStaticListValue(String value)
    {
        if (StringUtils.isNotEmpty(value)) {
            // actual values replacement
            value = value.replaceAll("(?i)^en attente$", "nr");
            value = value.replaceAll("(?i)^n/a$", "na");
            // and also empty the value if the value is -
            if (isDash(value)) {
                value = "nr";
            }
        }
        return value;
    }

    /**
     * Infers the values of "accompagnement" and "moyens de contact", only if they are mapped and mapped to the same
     * header, using the following rule:
     * <ul>
     * <li>support de qualité = oui -&gt; accompagnement = oui and moyens de contact = oui</li>
     * <li>support de qualité = partiel -&gt; accompagnement = oui and moyens de contact = non</li>
     * <li>support de qualité = non -&gt; accompagnement = non and moyens de contact = non</li>
     * <li>support de qualité = na -&gt; accompagnement = na and moyens de contact = na</li>
     * <li>support de qualité = nr -&gt; accompagnement = nr and moyens de contact = nr</li>
     * <li>summary: "accompagnement" and "moyens de contact" have the same value as "support de qualité" except when
     * value is "partiel"</li>
     * </ul>
     *
     * @param data the data read from the file and mapped, ready to be imported
     * @param mapping the current mapping,to be able to check that the fields are mapped and intervene only if mapped
     */
    protected void processSupportDeQualite(Map<String, String> data, Map<String, String> mapping)
    {
        // Check that the 2 properties are mapped, and are mapped to the same column.
        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_ACCOMPAGNEMENT))
            && StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_MOYENS_DE_CONTACT))
            && mapping.get(DEMARCHE_PROPERTY_ACCOMPAGNEMENT).equals(mapping.get(DEMARCHE_PROPERTY_MOYENS_DE_CONTACT))) {
            logger.debug("Support de qualité: properties " + DEMARCHE_PROPERTY_ACCOMPAGNEMENT + " and "
                + DEMARCHE_PROPERTY_MOYENS_DE_CONTACT + " are both mapped to the same header, processing the value...");
            // if so, get the value as fetched in the data and work it
            String value = data.get(DEMARCHE_PROPERTY_ACCOMPAGNEMENT);
            String transformationLog = "Support de qualité value " + value + " becomes ";
            value = normalizeStaticListValue(value);
            // Initialize values to "nr" (for "non renseigné")
            String accompagnement = "nr";
            String moyensDeContact = "nr";
            if (StringUtils.isNotEmpty(value)) {
                if (value.equalsIgnoreCase("partiel")) {
                    accompagnement = "oui";
                    moyensDeContact = "non";
                } else {
                    accompagnement = value;
                    moyensDeContact = value;
                }
            }

            transformationLog += DEMARCHE_PROPERTY_ACCOMPAGNEMENT + "=" + accompagnement + "; "
                + DEMARCHE_PROPERTY_MOYENS_DE_CONTACT + "=" + moyensDeContact;
            logger.debug(transformationLog);
            data.put(DEMARCHE_PROPERTY_ACCOMPAGNEMENT, accompagnement);
            data.put(DEMARCHE_PROPERTY_MOYENS_DE_CONTACT, moyensDeContact);
        }
    }

    /**
     * Processes the demarche URL: cleans up all special values that can appear in the URL, if the URL is mapped to keep
     * only valid values.
     *
     * @param data the data read from the file and mapped, ready to be imported
     * @param mapping the current mapping, to be able to verify that the URL is mapped and only intervene if there is a
     *            mapping for the URL
     */
    protected void processUrl(Map<String, String> data, Map<String, String> mapping)
    {
        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_URL))) {
            String value = data.get(DEMARCHE_PROPERTY_URL);
            logger.debug("Processing url...");
            if ("EDI".equals(value) || "En attente".equals(value)) {
                logger.debug("Skipping url value " + value);
                data.put(DEMARCHE_PROPERTY_URL, "");
            }
        }
    }

    /**
     * Processes the accessibility statement value, if mapped, from a date value (expects to be mapped on a date value),
     * as follows: <br>
     * - if there is a value, parse it as a date. If it's earlier than 3 years, store "oui" in the field <br>
     * - if there is no value or it's no earlier than 3 years, <strong>look at the vlue of the field where the URL is
     * mapped</strong>, and check the following: <br>
     * - if the URL is set to EDI, then store "na" in the field (n/a) - if the URL is set to En attente, then "store" nr
     * in the field (En attente) - in all other cases, store "non"
     *
     * @param data the data read from the file and mapped, ready to be imported
     * @param mapping the current mapping, to be able to verify that the accessibility statement is mapped and only
     *            intervene if there is a mapping
     */
    protected void processRGAAStatement(Map<String, String> data, Map<String, String> mapping)
    {
        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_RGAA_STATEMENT))) {
            logger.debug("Processing accessibility statement");
            String valueToSet = "non";
            String dateValue = data.get(DEMARCHE_PROPERTY_RGAA_STATEMENT);
            // if the value is set, check how old it is
            if (StringUtils.isNotEmpty(dateValue)) {
                try {
                    Date statementDate = FORMATTER_DATE_RGAA.parse(dateValue);
                    // add an extra day in the 3 years to cover for the probability that one of these 3 years is a leap
                    // year, which is quite high (3/4)
                    long threeYearsInMillis = (long) (3 * 365 + 1) * 24 * 60 * 60 * 1000;
                    // we evaluate the age of the declaration on the date of the import, to correspond to what AirTable
                    // seems to display
                    long todayInMillis = new Date().getTime();
                    if (todayInMillis - statementDate.getTime() <= threeYearsInMillis) {
                        logger.debug("Processing accessibility statement: Date found and parsed " + statementDate
                            + " and is within the 3 years");
                        valueToSet = "oui";
                    }
                } catch (ParseException e) {
                    logger.warn("Processing accessibility statement: Could not parse value " + dateValue
                        + " as date for field " + DEMARCHE_PROPERTY_RGAA_STATEMENT + ", assuming empty value");
                }
            }
            if ("non".equals(valueToSet)) {
                // value does not exist, or could not be parsed to a date that is within 3 years from now, check if this
                // is a special case
                logger.debug("Processing accessibility statement: Date within 3 years not found, looking at URL");
                // FIXME: this function only works if the URL is also mapped, but we'll make sure it's always mapped
                String urlValue = data.get(DEMARCHE_PROPERTY_URL);
                if ("EDI".equals(urlValue)) {
                    logger.debug("Processing accessibility statement: URL is 'EDI', setting n/a");
                    valueToSet = "na";
                }
                if (("En attente").equals(urlValue)) {
                    logger.debug("Processing accessibility statement: URL is 'En attente', setting n/r (En attente)");
                    valueToSet = "nr";
                }
            }
            logger.debug("Processing accessibility statement: setting final value " + valueToSet);
            data.put(DEMARCHE_PROPERTY_RGAA_STATEMENT, valueToSet);
        }
    }

    /**
     * Normalize the dlnuf column to numeric or empty values.
     *
     * @param data the data read from the file and mapped, ready to be imported
     * @param mapping the current mapping, to be able to verify that the dlnuf is mapped and only intervene if there is
     *            a mapping
     */
    protected void processDLNUF(Map<String, String> data, Map<String, String> mapping)
    {
        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_DLNUF))) {
            logger.debug("Processing DLNUF");
            String value = data.get(DEMARCHE_PROPERTY_DLNUF);
            // TODO: remove me when we'll have the column without colors
            // replace all characters except ascii letters (uppercase and lowercase), numbers, the character -, the
            // fancy
            // dash –, the slash and empty space with nothing.
            value = value.replaceAll("[^a-zA-Z0-9\\-\\–\\/\\s]+", "").trim();
            // processing of the remaining value
            if ("n/a".equals(value)) {
                logger.debug("Processing DLNUF: normalizing n/a as -1");
                value = "-1";
            }
            if ("En attente".equals(value)) {
                logger.debug("Processing DLNUF: normalizing 'En attente' as empty value");
                value = "";
            }
            if (isDash(value)) {
                logger.debug("Processing DLNUF: normalizing dash as empty value");
                value = "";
            }
            data.put(DEMARCHE_PROPERTY_DLNUF, value);
        }
    }

    /**
     * Sets a flag value (to {@code trueValueToSet} or empty) in the data, for the given property, if the current data
     * (cleaned up of whitespaces) matches regardless of the case the {@code spacelessCaselessTrueValue} parameter.
     *
     * @param data the data read from the file and mapped, ready to be imported
     * @param propertyName the property to make the processing for
     * @param spacelessCaselessTrueValue the value of the data that would trigger the flag to be set to the value
     *            indicated by {@code trueValueToSet} (otherwise will be set to empty)
     * @param trueValueToSet the value to set in case the current matches spacelessCaselessTrueValue (empty string will
     *            be set otherwise)
     */
    protected void processFlagForProperty(Map<String, String> data, String propertyName,
        String spacelessCaselessTrueValue, String trueValueToSet)
    {
        logger.debug("Processing flag " + propertyName);
        // we'll not set any value if the flag is not to be set, by the principle that the data should be left "as
        // is" if there is no flag to set. However, I think this import is done with "honor empty values" so in
        // practice it is the same thing as setting 0.
        String valueToSet = "";
        String fileValue = data.get(propertyName);
        // test without whitespace, to try to cover a little more cases
        if (spacelessCaselessTrueValue.equalsIgnoreCase(fileValue.replaceAll("\\s", ""))) {
            valueToSet = trueValueToSet;
        }
        logger.debug("Setting flag " + propertyName + ": " + valueToSet);
        data.put(propertyName, valueToSet);
    }

    protected void processEdiOnlyFlag(Map<String, String> data, Map<String, String> mapping)
    {
        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_EDI_ONLY))) {
            // ediOnly is set either from EDI values of the selected field or from the "checked" value when the selected
            // column is coming from a checkbox column in Airtable
            logger.debug("Processing ediOnly from  " + mapping.get(DEMARCHE_PROPERTY_EDI_ONLY));
            // we'll not set any value if the flag is not to be set, by the principle that the data should be left "as
            // is" if there is no flag to set. However, I think this import is done with "honor empty values" so in
            // practice it is the same thing as setting 0.
            String valueToSet = "";
            String fileValue = data.get(DEMARCHE_PROPERTY_EDI_ONLY);
            // test without whitespace, to try to cover a little more cases
            String valueWithoutSpaces = fileValue.replaceAll("\\s", "");
            if (valueWithoutSpaces.equalsIgnoreCase("EDI") || valueWithoutSpaces.equalsIgnoreCase("checked")) {
                valueToSet = "1";
            }
            logger.debug("Setting ediOnly: " + valueToSet);
            data.put(DEMARCHE_PROPERTY_EDI_ONLY, valueToSet);
        }
    }

    /**
     * Set higher priority than the listidentifier postprocessor in particular so that values get trimmed and
     * preprocessed (hence lower value).
     *
     * @see RowDataPostprocessor#getPriority()
     */
    @Override
    public double getPriority()
    {
        return 10;
    }
}
