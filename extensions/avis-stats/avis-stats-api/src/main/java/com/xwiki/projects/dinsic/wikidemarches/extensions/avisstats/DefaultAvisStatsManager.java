package com.xwiki.projects.dinsic.wikidemarches.extensions.avisstats;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Implementation of a <tt>AvisStatsManager</tt> component.
 */
@Component
@Singleton
public class DefaultAvisStatsManager implements AvisStatsManager
{
    @Inject
    protected Logger logger;

    @Inject
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    protected DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    protected QueryManager queryManager;

    /**
     * Computes the AvisStats values and saves the AvisStats object for the demarche passed as parameter. We assume
     * that the demarcheReference is valid, no check is performed.
     * TODO: is synchronized enough to cover the thread safety?
     */
    public synchronized void computeAvisStats(DocumentReference demarcheReference, XWikiContext context)
            throws QueryException, XWikiException
    {

        Query query = this.queryManager.createQuery(
                "select count(*), avg(score.value), sum(case when vote.value='true' then 1 else 0 end) from "
                        + "BaseObject as avis, IntegerProperty as score, StringProperty as vote, "
                        + "StringProperty as demarche where avis.className = :avisClass "
                        + "and avis.id = score.id.id and score.id.name = :scoreProperty "
                        + "and avis.id = vote.id.id and vote.id.name = :voteProperty "
                        + "and demarche.id.id = avis.id and demarche.id.name = :demarcheProperty and demarche.value = "
                        + ":demarche and score.value > 0",
                Query.HQL);
        query = query.bindValue("demarche", compactWikiSerializer.serialize(demarcheReference));
        query.bindValue("avisClass", AVIS_CLASS_NAME);
        query.bindValue("scoreProperty", AVIS_SCORE_PROPERTY_NAME);
        query.bindValue("voteProperty", AVIS_VOTE_PROPERTY_NAME);
        query.bindValue("demarcheProperty", AVIS_DEMARCHE_PROPERTY_NAME);

        List results = query.execute();
        Object[] result = (Object[]) results.get(0);
        long occurrences = (long) result[0];
        double average = result[1] != null ? (double) result[1] : 0;
        long votes = result[2] != null ? (long) result[2] :  0;
        setAvisStatsValues(demarcheReference, occurrences, average, votes, true, context);
    }

    /**
     * Computes the AvisStats values and saves the AvisStats objects for all demarches. Demarches which have not
     * received any Avis yet will get an AvisStats object initialized to its default values.
     */
    public void computeAvisStats(XWikiContext context) throws QueryException, XWikiException
    {

        //The query below will return values for Demarches having at least one Avis.
        // TODO: test if the 'vote' can be null.
        Query query = this.queryManager.createQuery("select demarche.value, count(*), avg(score.value), "
                + "sum(case when vote.value='true' then 1 else 0 end) from BaseObject as avis, "
                + "IntegerProperty as score, StringProperty as vote, StringProperty as demarche where "
                + "avis.className = :avisClass "
                + "and avis.id = score.id.id and score.id.name = :scoreProperty and demarche.id.id = avis.id "
                + "and avis.id = vote.id.id and vote.id.name = :voteProperty and demarche.id.id = avis.id and "
                + "demarche.id.name = :demarcheProperty and score.value > 0 group by demarche.value order "
                + "by demarche.value", Query.HQL
        );

        query.bindValue("avisClass", AVIS_CLASS_NAME);
        query.bindValue("scoreProperty", AVIS_SCORE_PROPERTY_NAME);
        query.bindValue("voteProperty", AVIS_VOTE_PROPERTY_NAME);
        query.bindValue("demarcheProperty", AVIS_DEMARCHE_PROPERTY_NAME);

        List results = query.execute();
        for (Object result : results) {
            Object[] values = (Object[]) result;
            String demarcheId = (String) values[0];
            DocumentReference demarcheReference = documentReferenceResolver.resolve(demarcheId);
            long occurrences = (long) values[1];
            double average = (double) values[2];
            long votes = (long) values[3];
            setAvisStatsValues(demarcheReference, occurrences, average, votes, true, context);
        }

        //Handle Demarches which have not received any Avis yet.
        //We set an average of 0 for demarches without 'Avis', we'll handle on display

        query = this.queryManager.createQuery(
                "select distinct doc.fullName as fullName from XWikiDocument as doc, BaseObject as obj "
                        + "where obj.className = :demarcheClass and doc.fullName = obj.name and "
                        + "doc.fullName != :demarcheTemplate and doc.fullName not in "
                        + "(select distinct demarche.value from BaseObject as avis, StringProperty demarche "
                        + "where avis.className = :avisClass and demarche.id.id = avis.id and "
                        + "demarche.id.name = :avisDemarcheProperty)",
                Query.HQL);

        query.bindValue("demarcheClass", DEMARCHE_CLASS_NAME);
        query.bindValue("avisClass", AVIS_CLASS_NAME);
        query.bindValue("demarcheTemplate", DEMARCHE_TEMPLATE_NAME);
        query.bindValue("avisDemarcheProperty", AVIS_DEMARCHE_PROPERTY_NAME);
        results = query.execute();
        for (Object result : results) {
            String demarcheId = (String) result;
            DocumentReference demarcheReference = documentReferenceResolver.resolve(demarcheId);
            setAvisStatsValues(demarcheReference, 0, 0, 0, true, context);
        }
    }

    /**
     * Initializes or updates AvisStats values for a given demarche.
     */
    private synchronized void setAvisStatsValues(DocumentReference demarcheReference, long occurrences, double average,
            long votes, boolean addVersionEntry, XWikiContext context) throws XWikiException
    {
        XWiki wiki = context.getWiki();
        XWikiDocument demarche = wiki.getDocument(demarcheReference, context).clone();
        if (!demarche.isNew()) {
            BaseObject avisStats = demarche.getXObject(AVIS_STATS_CLASS_REFERENCE);
            if (avisStats == null) {
                avisStats = demarche.newXObject(AVIS_STATS_CLASS_REFERENCE, context);
            }
            avisStats.setLongValue(OCCURRENCES_PROPERTY_NAME, occurrences);
            avisStats.setDoubleValue(AVERAGE_PROPERTY_NAME, average);
            avisStats.setLongValue(VOTES_PROPERTY_NAME, votes);

            demarche.setMetaDataDirty(addVersionEntry);
            if (!addVersionEntry) {
                wiki.saveDocument(demarche, context);
            } else {
                wiki.saveDocument(demarche, "Initialisation ou mise à jour de l'objet 'AvisStats'.", context);
            }
        } else {
            logger.warn("Avis update was received for demarche " + demarcheReference + " which does not exist!");
        }
    }
}




