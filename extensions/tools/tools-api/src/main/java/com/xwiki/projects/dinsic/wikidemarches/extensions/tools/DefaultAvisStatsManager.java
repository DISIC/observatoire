package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
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

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Computes the AvisStats values and saves the AvisStats object for the demarche passed as parameter. We assume that
     * the demarcheReference is valid, no check is performed. <br>
     * TODO: is synchronized enough to cover the thread safety?
     */
    public synchronized void computeAvisStats(DocumentReference demarcheReference, boolean save)
        throws QueryException, XWikiException
    {

        Query query = this.queryManager
            .createQuery("select count(*), avg(score.value), sum(case when vote.value='true' then 1 else 0 end) from "
                + "BaseObject as avis, IntegerProperty as score, StringProperty as vote, "
                + "StringProperty as demarche where avis.className = :avisClass "
                + "and avis.id = score.id.id and score.id.name = :scoreProperty "
                + "and avis.id = vote.id.id and vote.id.name = :voteProperty "
                + "and demarche.id.id = avis.id and demarche.id.name = :demarcheProperty and demarche.value = "
                + ":demarche and score.value > 0", Query.HQL);
        query = query.bindValue("demarche", compactWikiSerializer.serialize(demarcheReference));
        query.bindValue("avisClass", AVIS_CLASS_NAME);
        query.bindValue("scoreProperty", AVIS_SCORE_PROPERTY_NAME);
        query.bindValue("voteProperty", AVIS_VOTE_PROPERTY_NAME);
        query.bindValue("demarcheProperty", AVIS_DEMARCHE_PROPERTY_NAME);

        List results = query.execute();
        Object[] result = (Object[]) results.get(0);
        long occurrences = (long) result[0];
        double satisfactionIndex = result[1] != null ? (double) result[1] : 0;
        long votes = result[2] != null ? (long) result[2] : 0;
        logger.debug("Computing stats for demarche {} - occurences: {} satisfaction index: {} votes: {}",
            demarcheReference, occurrences, satisfactionIndex, votes);
        setAvisStatsValues(demarcheReference, occurrences, satisfactionIndex, votes, save,false);
    }

    /**
     * Computes the AvisStats values and saves the AvisStats objects for all demarches. Demarches which have not
     * received any Avis yet will get an AvisStats object initialized to its default values.
     */
    public void computeAvisStats() throws QueryException, XWikiException
    {
        logger.debug("Starting to compute stats for all demarches");
        // The query below will return values for Demarches having at least one Avis.
        // for now the 'vote' of an avis cannot be null (it's either empty value, or true or false),
        // and thus we can compute both score average and vote count in the same request.
        // However, in theory the vote can be null and thus should be computed in a separate query (or outer joined), so
        // that the join with the vote value does not exclude avis from the counting of avis.
        Query query = this.queryManager.createQuery("select demarche.value, count(*), avg(score.value), "
            + "sum(case when vote.value='true' then 1 else 0 end) from BaseObject as avis, "
            + "IntegerProperty as score, StringProperty as vote, StringProperty as demarche where "
            + "avis.className = :avisClass "
            + "and avis.id = score.id.id and score.id.name = :scoreProperty and demarche.id.id = avis.id "
            + "and avis.id = vote.id.id and vote.id.name = :voteProperty and demarche.id.id = avis.id and "
            + "demarche.id.name = :demarcheProperty and score.value > 0 group by demarche.value order "
            + "by demarche.value", Query.HQL);

        query.bindValue("avisClass", AVIS_CLASS_NAME);
        query.bindValue("scoreProperty", AVIS_SCORE_PROPERTY_NAME);
        query.bindValue("voteProperty", AVIS_VOTE_PROPERTY_NAME);
        query.bindValue("demarcheProperty", AVIS_DEMARCHE_PROPERTY_NAME);

        int handledDemarches = 0;

        List results = query.execute();
        for (Object result : results) {
            Object[] values = (Object[]) result;
            String demarcheId = (String) values[0];
            DocumentReference demarcheReference = documentReferenceResolver.resolve(demarcheId);
            long occurrences = (long) values[1];
            double satisfactionIndex = values[2] != null ? (double) values[2] : 0;
            long votes = (long) values[3];
            logger.debug("#{} Computing stats for demarche {} - occurences: {} satisfaction index: {} votes: {}",
                    ++handledDemarches, demarcheId, occurrences, satisfactionIndex, votes);
            setAvisStatsValues(demarcheReference, occurrences, satisfactionIndex, votes, true, false);
        }

        // Handle Demarches which have not received any Avis yet.
        // We set an average of 0 for demarches without 'Avis', we'll handle on display

        query = this.queryManager
            .createQuery("select distinct doc.fullName as fullName from XWikiDocument as doc, BaseObject as obj "
                + "where obj.className = :demarcheClass and doc.fullName = obj.name and "
                + "doc.fullName != :demarcheTemplate and doc.fullName not in "
                + "(select distinct demarche.value from BaseObject as avis, StringProperty demarche "
                + "where avis.className = :avisClass and demarche.id.id = avis.id and "
                + "demarche.id.name = :avisDemarcheProperty)", Query.HQL);

        query.bindValue("demarcheClass", DEMARCHE_CLASS_NAME);
        query.bindValue("avisClass", AVIS_CLASS_NAME);
        query.bindValue("demarcheTemplate", DEMARCHE_TEMPLATE_NAME);
        query.bindValue("avisDemarcheProperty", AVIS_DEMARCHE_PROPERTY_NAME);
        results = query.execute();
        for (Object result : results) {
            String demarcheId = (String) result;
            DocumentReference demarcheReference = documentReferenceResolver.resolve(demarcheId);
            logger.debug("#{} Setting stats to 0 for demarche {}", ++handledDemarches, demarcheId);
            setAvisStatsValues(demarcheReference, 0, 0, 0, true,false);
        }
        logger.debug("Done computing stats for all demarches");
    }

    /**
     * Initializes or updates AvisStats values for a given demarche.
     */
    private synchronized void setAvisStatsValues(DocumentReference demarcheReference, long occurrences, double satisfactionIndex,
        long votes, boolean save, boolean addVersionEntry) throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        XWikiDocument demarche = wiki.getDocument(demarcheReference, context).clone();
        if (!demarche.isNew()) {
            BaseObject avisStats = demarche.getXObject(AVIS_STATS_CLASS_REFERENCE);
            if (avisStats == null) {
                avisStats = demarche.newXObject(AVIS_STATS_CLASS_REFERENCE, context);
            }
            avisStats.setLongValue(OCCURRENCES_PROPERTY_NAME, occurrences);
            avisStats.setDoubleValue(SATISFACTION_INDEX_PROPERTY_NAME, satisfactionIndex);
            avisStats.setLongValue(VOTES_PROPERTY_NAME, votes);

            demarche.setMetaDataDirty(addVersionEntry);
            if (save) {
                if (!addVersionEntry) {
                    wiki.saveDocument(demarche, context);
                } else {
                    wiki.saveDocument(demarche, "Initialisation ou mise à jour de l'objet 'AvisStats'.", context);
                }
            }
        } else {
            logger.warn("Avis update was received for demarche " + demarcheReference + " which does not exist!");
        }
    }
}
