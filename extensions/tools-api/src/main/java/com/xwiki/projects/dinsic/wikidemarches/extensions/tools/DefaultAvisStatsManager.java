package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import java.util.Calendar;
import java.util.Date;
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
     * Returns a date interval which starts one year from now plus one day and ends now.
     * @return An array of two dates representing the past year.
     */
    public Date[] getCurrentRollingYear() {
        Calendar maxDate = Calendar.getInstance();
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -1);
        minDate.add(Calendar.DATE, 1);
        return new Date[]{minDate.getTime(), maxDate.getTime()};
    }

    /**
     * Creates a query for computing avis statistics either for a single demarche (if the demarche argument is not null),
     * either for all demarches (if the demarche argument is null), optionally between two dates if the corresponding
     * dateBounds argument is not null.
     * @param demarche DocumentReference of a demarche for which the statistics need to be computed, or null if the query should cover all demarches.
     * @param dateBounds Date interval within with the avis were created, or null if all avis from day 1 should be counted.
     * @return a Query object that can be executed
     * @throws QueryException
     */
    protected Query createAvisStatsQuery(DocumentReference demarche, Date[] dateBounds) throws QueryException {
        // The query below will return values for Demarches having at least one Avis.
        // for now the 'vote' of an avis cannot be null (it's either empty value, or true or false),
        // and thus we can compute both score average and vote count in the same request.
        // However, in theory the vote can be null and thus should be computed in a separate query (or outer joined), so
        // that the join with the vote value does not exclude avis from the counting of avis.
        String hql = "select demarche.value, count(*), avg(score.value), " +
                "sum(case when vote.value = 'true' then 1 else 0 end) from XWikiDocument as doc, BaseObject as avis, " +
                "IntegerProperty as score, StringProperty as vote, StringProperty as demarche " +
                "where avis.name = doc.fullName and avis.className = :avisClass and score.id.id = avis.id " +
                "and score.id.name = :scoreProperty and demarche.id.id = avis.id and demarche.id.name = :demarcheProperty " +
                "and vote.id.id = avis.id and vote.id.name = :voteProperty and score.value > 0";
        if (demarche != null) {
            hql += " and demarche.value = :demarche";
        }
        if (dateBounds != null) {
            hql += " and doc.creationDate >= :minDate and doc.creationDate <= :maxDate";
        }
        hql += " group by demarche.value order by demarche.value";
        Query query = this.queryManager.createQuery(hql, Query.HQL);
        query.bindValue("avisClass", AVIS_CLASS_NAME);
        query.bindValue("scoreProperty", AVIS_SCORE_PROPERTY_NAME);
        query.bindValue("voteProperty", AVIS_VOTE_PROPERTY_NAME);
        query.bindValue("demarcheProperty", AVIS_DEMARCHE_PROPERTY_NAME);
        if (demarche != null) {
            query.bindValue("demarche", compactWikiSerializer.serialize(demarche));
        }
        if (dateBounds != null) {
            query.bindValue("minDate", dateBounds[0]);
            query.bindValue("maxDate", dateBounds[1]);
        }
        return query;
    }

    public synchronized void computeAvisStats(DocumentReference demarcheReference, boolean save)
            throws QueryException, XWikiException
    {
        computeAvisStats(getDocument(demarcheReference), save);
    }

    /**
     * Computes the AvisStats values for the current rolling year, and saves the AvisStats object for
     * the demarche passed as parameter. We assume that the demarcheReference is valid, no check is performed.
     * <br> TODO: is synchronized enough to cover the thread safety?
     */
    public synchronized void computeAvisStats(XWikiDocument demarche, boolean save)
            throws QueryException, XWikiException
    {
        Query query = createAvisStatsQuery(demarche.getDocumentReference(), getCurrentRollingYear());
        List results = query.execute();
        Object[] result = (Object[]) results.get(0);
        long occurrences = (long) result[1];
        double satisfactionIndex = result[2] != null ? (double) result[2] : 0;
        long votes = result[3] != null ? (long) result[3] : 0;
        logger.debug("Computing stats for demarche {} - occurences: {} satisfaction index: {} votes: {}",
                demarche.getDocumentReference(), occurrences, satisfactionIndex, votes);
        setAvisStatsValues(demarche, occurrences, satisfactionIndex, votes, save, false);
    }

    /**
     * Computes the AvisStats values for the current rolling year and saves the AvisStats objects for all demarches
     * having at least one avis. For those which have no avis, the AvisStats object is in principle initialized upon
     * demarche creation by a {@link DemarcheEventListener}.
     */
    public void computeAvisStats() throws QueryException, XWikiException
    {
        logger.debug("Starting to compute stats for all demarches");
        Query query = createAvisStatsQuery(null, getCurrentRollingYear());
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

        logger.debug("Computing statistics has completed for all demarches having at least one avis.");
    }

    private void setAvisStatsValues(DocumentReference demarcheReference, long occurrences,
            double satisfactionIndex,
            long votes, boolean save, boolean addVersionEntry) throws XWikiException
    {
        setAvisStatsValues(getDocument(demarcheReference), occurrences, satisfactionIndex, votes, save,
                addVersionEntry);
    }

    private XWikiDocument getDocument(DocumentReference reference) throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(reference, context);
    }

    /**
     * Initializes or updates AvisStats values for a given demarche.
     */
    private synchronized void setAvisStatsValues(XWikiDocument demarche, long occurrences,
            double satisfactionIndex,
            long votes, boolean save, boolean addVersionEntry) throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        if (save) {
            demarche = demarche.clone();
        }
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
    }
}
