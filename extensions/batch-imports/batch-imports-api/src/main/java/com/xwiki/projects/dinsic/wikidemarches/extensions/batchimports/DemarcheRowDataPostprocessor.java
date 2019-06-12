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
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.batchimport.BatchImport;
import org.xwiki.batchimport.BatchImportConfiguration;
import org.xwiki.batchimport.RowDataPostprocessor;
import org.xwiki.batchimport.internal.DefaultBatchImport;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;

// TODO: consider firing an error that blocks the import as soon as an unexpected value is found, in order to avoid
// importing invalid data
// NB: the property values that get marshalled need to be in the mapping. So we need to map properties such as
// "accompagnement", "moyensDeContact", "remarques" to a fake column, since they are not present in the input file. We
// could also consider modifying the mapping dynamically.

@Component("demarche")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DemarcheRowDataPostprocessor implements RowDataPostprocessor
{
    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private Logger logger;

    @Inject
    protected BatchImport batchImport;

    @Inject
    @Named("current")

    private DocumentReferenceResolver<String> resolver;

    public static String HEADER_SUPPORT_DE_QUALITE = "Support de qualité ?";

    public static String HEADER_COMMENT_1 = "Commentaires administration + annonce CITP";

    public static String HEADER_COMMENT_2 = "Commentaire DINSIC";

    public static String HEADER_COMMENT_3 = "Commentaires UX/ test";

    public static String HEADER_URL = "URL de la démarches";

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
        processNumbers(data);
        normalizeStaticListValue(DEMARCHE_PROPERTY_STATUT_INTEGRATION, data);
        normalizeStaticListValue(DEMARCHE_PROPERTY_FRANCE_CONNECT, data);
        normalizeStaticListValue(DEMARCHE_PROPERTY_ADAPTE_MOBILE, data);
        processSupportDeQualite(data, row, headers);
        processComments(data, row, rowIndex, headers, config);
        processUrl(data, row, headers);
        logger.debug("New data after processing: ", data);
    }

    // Trim all values, since some input files sometimes contain values with surrounding spaces (e.g. directions)
    protected void trimAllValues(Map<String, String> data)
    {
        Set<Map.Entry<String, String>> entries = data.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (StringUtils.isNotEmpty(entry.getValue())) {
                entry.setValue(entry.getValue().trim());
            }
        }
    }

    // Process column "Date d'ouverture du service dématérialisé" and convert its values to 2 property values:
    // dateMiseEnLigne and dateMiseEnLigneTexte, according to the following conversion rules:
    // - "n/c" -> "", ""
    // - "Ouvert" -> "", "Ouvert"
    // - "2022" -> "01/12/2022", "2022"
    // - "Sep-2019" -> "01/09/2019", ""
    // - "2020-2021" -> "01/12/2020", "2020-2021"
    // - "2020?" -> "01/12/2020", "2020"

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
            data.put(DEMARCHE_PROPERTY_DATE_MISE_EN_LIGNE_TEXTE, dateMiseEnLigneTexte);
        }
    }

    protected void processComments(Map<String, String> data, List<String> row, int rowIndex, List<String> headers,
            BatchImportConfiguration config)
    {
        // Concatenate all comments to the possibly existing "remarques" property value
        DefaultBatchImport defaultBatchImport = (DefaultBatchImport) batchImport;
        DocumentReference reference = defaultBatchImport.getPageName(data, rowIndex, config, null);
        if (reference != null) {
            String remarques = "";
            String comment1 = maybeAddLabel(getRowDataByHeader(row, HEADER_COMMENT_1, headers), HEADER_COMMENT_1);
            String comment2 = maybeAddLabel(getRowDataByHeader(row, HEADER_COMMENT_2, headers), HEADER_COMMENT_2);
            String comment3 = maybeAddLabel(getRowDataByHeader(row, HEADER_COMMENT_3, headers), HEADER_COMMENT_3);

            String joined = Stream.of(remarques, comment1, comment2, comment3)
                    .filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.joining("\n\n")).trim();
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

    protected void processNumbers(Map<String, String> data)
    {
        String[] propertyNames =
                new String[]{ DEMARCHE_PROPERTY_VOLUMETRIE, DEMARCHE_PROPERTY_VOLUMETRIE_DEMATERIALISATION };
        for (String propertyName : propertyNames) {
            String value = data.get(propertyName);
            if ("n/c".equalsIgnoreCase(value) || "na".equalsIgnoreCase(value) || "n/a".equalsIgnoreCase(value)) {
                data.put(propertyName, "");
            }
        }
    }

    // Replace common static list values by their corresponding key in the démarche property: replace "n/c" by
    // "nr" ("non renseigné"), "n/a" by "na", "Oui" by "oui", "Non" by "non", "A venir" by "nr"
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

    // Infers the values of "accompagnement" and "moyens de contact" using the following rule:
    // - support de qualité = oui -> accompagnement = oui and moyens de contact = oui
    // - support de qualité = partiel -> accompagnement = oui and moyens de contact = non
    // - support de qualité = non -> accompagnement = non and moyens de contact = non
    // - support de qualité = na -> accompagnement = na and moyens de contact = na
    // - support de qualité = nr -> accompagnement = nr and moyens de contact = nr
    // - summary: "accompagnement" and "moyens de contact" have the same value as "support de qualité" except
    // when value is "partiel"

    protected void processSupportDeQualite(Map<String, String> data, List<String> row, List<String> headers)
    {
        String value = getRowDataByHeader(row, HEADER_SUPPORT_DE_QUALITE, headers);
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

    protected void processUrl(Map<String, String> data, List<String> row, List<String> headers)
    {
        String value = getRowDataByHeader(row, HEADER_URL, headers);
        if (value == null || "?".equals(value) || "-".equals(value)) {
            data.put(DEMARCHE_PROPERTY_URL, "");
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
