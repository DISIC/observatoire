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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static String DEMARCHE_PROPERTY_STATUT_DEMATERIALISATION = "statutDemat";

    public static String DEMARCHE_PROPERTY_DATE_MISE_EN_LIGNE = "dateMiseEnLigne";

    public static String DEMARCHE_PROPERTY_DATE_MISE_EN_LIGNE_TEXTE = "dateMiseEnLigneTexte";

    public static String DEMARCHE_PROPERTY_VOLUMETRIE = "volumetrie";

    public static String DEMARCHE_PROPERTY_VOLUMETRIE_DEMATERIALISATION = "volumetrieDemat";

    public static String DEMARCHE_PROPERTY_STATUT_INTEGRATION = "statutIntegration";

    public static String DEMARCHE_PROPERTY_FRANCE_CONNECT = "franceConnect";

    public static String DEMARCHE_PROPERTY_ADAPTE_MOBILE = "adapteMobile";

    public static String DEMARCHE_PROPERTY_ACCOMPAGNEMENT = "accompagnement";

    public static String DEMARCHE_PROPERTY_MOYENS_DE_CONTACT = "moyensDeContact";

    public static String DEMARCHE_PROPERTY_URL = "urlDemarche";

    public static String DEMARCHE_PROPERTY_REMARQUES = "remarques";

    public static String HEADER_EXTRA_REMARQUES_1 = "Commentaire DINSIC";

    public static String HEADER_EXTRA_REMARQUES_2 = "Commentaires UX/ test";

    public static SimpleDateFormat FORMATTER_DATE_MISE_EN_LIGNE_INPUT = new SimpleDateFormat("MMM-yy");

    public static SimpleDateFormat FORMATTER_DATE_MISE_EN_LIGNE_OUTPUT = new SimpleDateFormat("MM/yyyy");

    /**
     * {@inheritDoc}
     *
     * @see RowDataPostprocessor#postProcessRow(Map, List, int, Map, List, BatchImportConfiguration)
     */
    @Override
    public void postProcessRow(Map<String, String> data, List<String> row, int rowIndex, Map<String, String> mapping,
        List<String> headers, BatchImportConfiguration config)
    {
        trimAllValues(data);
        normalizeStaticListValue(DEMARCHE_PROPERTY_STATUT_DEMATERIALISATION, data);
        processOpeningDate(data);
        processVolumetrieNumbers(data);
        normalizeStaticListValue(DEMARCHE_PROPERTY_STATUT_INTEGRATION, data);
        normalizeStaticListValue(DEMARCHE_PROPERTY_FRANCE_CONNECT, data);
        normalizeStaticListValue(DEMARCHE_PROPERTY_ADAPTE_MOBILE, data);
        processSupportDeQualite(data, mapping);
        processComments(data, mapping, row, headers);
        processUrl(data, mapping);
        logger.debug("New data after processing: ", data);
    }

    /**
     * Trim all values, since some input files sometimes contain values with surrounding spaces (e.g. directions)
     *
     * @param data the data read from the file and mapped, ready to be imported
     */
    protected void trimAllValues(Map<String, String> data)
    {
        Set<Map.Entry<String, String>> entries = data.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (StringUtils.isNotEmpty(entry.getValue())) {
                entry.setValue(entry.getValue().trim());
            }
        }
    }

    /**
     * TODO: I don't like the logic of this function, it conditions the set of the text date with the non-text value, to
     * redo.<br>
     * Process column "Date d'ouverture du service dématérialisé" and convert its values to 2 property values:
     * dateMiseEnLigne and dateMiseEnLigneTexte, according to the following conversion rules:
     * <ul>
     * <li>"n/c" -> "", ""</li>
     * <li>"Ouvert" -> "", "Ouvert"</li>
     * <li>"2022" -> "01/12/2022", "2022"</li>
     * <li>"Sep-2019" -> "01/09/2019", ""</li>
     * <li>"2020-2021" -> "01/12/2020", "2020-2021"</li>
     * <li>"2020?" -> "01/12/2020", "2020"</li>
     * </ul>
     *
     * @param data the data read from the file and mapped, ready to be imported
     */
    protected void processOpeningDate(Map<String, String> data)
    {
        String value = data.get(DEMARCHE_PROPERTY_DATE_MISE_EN_LIGNE);
        if (StringUtils.isNotEmpty(value)) {

            String dateMiseEnLigneAsString = "";
            String dateMiseEnLigneTexte = "";

            if (value.matches("[^-]*-\\d{2}")) {
                // The value is a real date
                try {
                    Date date = FORMATTER_DATE_MISE_EN_LIGNE_INPUT.parse(value);
                    dateMiseEnLigneAsString = FORMATTER_DATE_MISE_EN_LIGNE_OUTPUT.format(date);
                    dateMiseEnLigneTexte = "";
                } catch (Exception e) {
                    // TODO: handle errors properly. For now we throw a runtime exception to block the import and avoid
                    // importing wrong data
                    throw new RuntimeException("Invalid date input");
                }
            } else {

                dateMiseEnLigneAsString = value.replaceAll("^(?i)ouvert$", "");
                dateMiseEnLigneAsString = dateMiseEnLigneAsString.replaceAll("^(?i)n/c$", "");
                dateMiseEnLigneAsString = dateMiseEnLigneAsString.replaceAll("^(?i)n/a$", "");
                dateMiseEnLigneAsString = dateMiseEnLigneAsString.replaceAll("^(\\d{4})\\?$", "12/$1");
                dateMiseEnLigneAsString = dateMiseEnLigneAsString.replaceAll("^(\\d{4})$", "12/$1");
                dateMiseEnLigneAsString = dateMiseEnLigneAsString.replaceAll("(\\d{4})-(\\d{4})", "12/$2");

                // We store the value as such in dateMiseEnLigneTexte to keep the information as it was provided,
                // typically when value is: "2020-2021"
                // No need to store information "ouvert" or "nr" because:
                // - "ouvert" can be inferred from "statutDemat=enLigneEntierement"
                // - "nr" can be inferred from "dateMiseEnLigne=''"
                if (!value.matches("(?i)ouvert") && !value.matches("(?i)n/c")) {
                    dateMiseEnLigneTexte = value;
                }
            }

            data.put(DEMARCHE_PROPERTY_DATE_MISE_EN_LIGNE, dateMiseEnLigneAsString);
            // In addition, if the dateMiseEnLigneTexte is also mapped, set the value to dateMiseEnLigneTexte as well
            if (StringUtils.isNotEmpty(data.get(DEMARCHE_PROPERTY_DATE_MISE_EN_LIGNE_TEXTE))) {
                data.put(DEMARCHE_PROPERTY_DATE_MISE_EN_LIGNE_TEXTE, dateMiseEnLigneTexte);
            }
        }
    }

    /**
     * Appends the values of fields "Commentaire DINSIC" and "Commentaires UX/ test" to the value mapped on the
     * remarques property, if any.
     *
     * @param data the data read from the file and mapped, ready to be imported
     * @param mapping the current mapping, to be able to check that the property is mapped and intervene only if it's
     *            the case
     * @param row the current row being processed
     * @param headers the full headers of the rows being processed
     */
    protected void processComments(Map<String, String> data, Map<String, String> mapping, List<String> row,
        List<String> headers)
    {
        if (StringUtils.isNotEmpty(mapping.get(DEMARCHE_PROPERTY_REMARQUES))) {
            String comment1 =
                maybeAddLabel(data.get(DEMARCHE_PROPERTY_REMARQUES), mapping.get(DEMARCHE_PROPERTY_REMARQUES));
            String comment2 =
                maybeAddLabel(getRowDataByHeader(row, HEADER_EXTRA_REMARQUES_1, headers), HEADER_EXTRA_REMARQUES_1);
            String comment3 =
                maybeAddLabel(getRowDataByHeader(row, HEADER_EXTRA_REMARQUES_2, headers), HEADER_EXTRA_REMARQUES_2);

            String joined = Stream.of(comment1, comment2, comment3).filter(s -> StringUtils.isNotEmpty(s))
                .collect(Collectors.joining("\n\n")).trim();
            data.put(DEMARCHE_PROPERTY_REMARQUES, joined);
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
        String[] propertyNames =
            new String[] {DEMARCHE_PROPERTY_VOLUMETRIE, DEMARCHE_PROPERTY_VOLUMETRIE_DEMATERIALISATION};
        // The logic below only handles specific values of the data, so we don't check if the properties are mapped or
        // not.
        for (String propertyName : propertyNames) {
            String value = data.get(propertyName);
            if ("n/c".equalsIgnoreCase(value) || "na".equalsIgnoreCase(value) || "n/a".equalsIgnoreCase(value)) {
                data.put(propertyName, "");
            }
        }
    }

    /**
     * Replace common static list values by their corresponding key in the démarche property: replace "n/c" by "nr"
     * ("non renseigné"), "n/a" by "na", "Oui" by "oui", "Non" by "non", "A venir" by "nr", "sans identification" by
     * "sansIdentification" .
     *
     * @param value the value to normalize
     * @return the normalized value
     */
    protected String normalizeStaticListValue(String value)
    {
        if (StringUtils.isNotEmpty(value)) {
            value = value.replaceAll("(?i)^n/c$", "nr");
            value = value.replaceAll("(?i)^n/a$", "na");
            value = value.replaceAll("(?i)^partiel$", "partiel");
            value = value.replaceAll("(?i)^a venir$", "nr");
            value = value.replaceAll("(?i)^à venir$", "nr");
            value = value.replaceAll("(?i)^oui$", "oui");
            value = value.replaceAll("(?i)^non$", "non");
        }
        return value;
    }

    protected void normalizeStaticListValue(String propertyName, Map<String, String> data)
    {
        String value = data.get(propertyName);
        String normalizedValue = normalizeStaticListValue(value);
        if (normalizedValue != null && !normalizedValue.equals(value)) {
            data.put(propertyName, normalizedValue);
        }
    }

    /**
     * Infers the values of "accompagnement" and "moyens de contact", only if they are mapped and mapped to the same
     * header, using the following rule:
     * <ul>
     * <li>support de qualité = oui -> accompagnement = oui and moyens de contact = oui</li>
     * <li>support de qualité = partiel -> accompagnement = oui and moyens de contact = non</li>
     * <li>support de qualité = non -> accompagnement = non and moyens de contact = non</li>
     * <li>support de qualité = na -> accompagnement = na and moyens de contact = na</li>
     * <li>support de qualité = nr -> accompagnement = nr and moyens de contact = nr</li>
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
            // if so, get the value as fetched in the data and work it
            String value = data.get(DEMARCHE_PROPERTY_ACCOMPAGNEMENT);
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
            if (value == null || "?".equals(value) || "-".equals(value)) {
                data.put(DEMARCHE_PROPERTY_URL, "");
            }
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
