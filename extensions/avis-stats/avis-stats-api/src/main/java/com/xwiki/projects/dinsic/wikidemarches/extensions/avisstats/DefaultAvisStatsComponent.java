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

//Component -> Manager
//        script / internal
//        requête dans tous les cas (c'est le principe d'un cache): ajout, suppression, mise à jour. Attention arfois il faut recalculer pour deux démarches (lorsque réaffectation).
//        cas de la réaffectation : utiliser getOriginalDocument qui donne le document précédent en mémoire (par opposition au document qui est dans le cache)
//        requête: enlever le join sur XWikiDocument car 1 document par objet count(star)
//        vérifier ce qu'il se passe quand il n'y a pas d'avis et vérifier la cohérence entre les deux computeAvisStats
//        déplacer "anAvisWasAdded" dans le listener, et n'exposer que deux méthodes : computeAvisStats()


/**
 * Implementation of a <tt>AvisStatsComponent</tt> component.
 */
@Component
@Singleton
public class DefaultAvisStatsComponent implements AvisStatsComponent
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

    //TODO: check that synchronized is ok here to make the operation thread safe
    //TODO: see how to handle exception
    public synchronized void anAvisWasAdded(BaseObject avis, XWikiContext context)
    {
        //TODO: check why we have 9.11.2 / 9.11.3 in the root pom.xml
        //TODO: in addition to the case where an avis gets created, take into account:
        // - the deletion of an avis
        // - the reattribution of an avis to a distinct demarche
        // - the rename of an avis

        XWiki wiki = context.getWiki();

        String demarcheId = avis.getStringValue("demarche");
        if (demarcheId != null && demarcheId.indexOf(":") < 0) {
            String wikiName = avis.getDocumentReference().getWikiReference().getName();
            demarcheId = wikiName + ":" + demarcheId;
        }

        DocumentReference demarcheReference = documentReferenceResolver.resolve(demarcheId);
        //TODO: what if the demarcheReference is invalid or null

        //TODO: check the default value if no score is set
        int score = avis.getIntValue("score", 2);

        try {
            //No need to clone the document here yet, since this block does not update it.
            XWikiDocument demarche = wiki.getDocument(demarcheReference, context);
            BaseObject avisStats = demarche.getXObject(AVIS_STATS_CLASS_REFERENCE);

            //If an AvisStatsClass object is already present, update it, otherwise, create it and initialize it.
            if (avisStats != null) {
                long occurrences = avisStats.getLongValue(OCCURRENCES_FIELD);
                double average = avisStats.getDoubleValue(AVERAGE_FIELD);
                double newAverage = (occurrences * average + score) / (occurrences + 1);
                setAvisStatsValues(demarcheReference, occurrences + 1, newAverage, false, context);
            } else {
                //No AvisStatsClass object found, hence create one and initialize it.
                setAvisStatsValues(demarcheReference, 1, score, false, context);
            }
        } catch (XWikiException e) {
            logger.error("Error while updating AvisStats following a change on [%s].",
                    compactWikiSerializer.serialize(avis.getDocumentReference()), e);
        }
    }

    /**
     * Initializes or updates AvisStats values for a given demarche. TODO: check if synchronized is needed here as
     * well.
     */
    private synchronized void setAvisStatsValues(DocumentReference demarcheReference, long occurrences, double average,
            boolean addVersionEntry, XWikiContext context) throws XWikiException
    {
        XWiki wiki = context.getWiki();
        XWikiDocument demarche = wiki.getDocument(demarcheReference, context).clone();
        BaseObject avisStats = demarche.getXObject(AVIS_STATS_CLASS_REFERENCE);
        if (avisStats == null) {
            avisStats = demarche.newXObject(AVIS_STATS_CLASS_REFERENCE, context);
        }
        avisStats.setLongValue(OCCURRENCES_FIELD, occurrences);
        avisStats.setDoubleValue(AVERAGE_FIELD, average);

        demarche.setMetaDataDirty(addVersionEntry);
        if (!addVersionEntry) {
            wiki.saveDocument(demarche, context);
        } else {
            //TODO: localize the version comment message
            wiki.saveDocument(demarche, "Updating AvisStats object", context);
        }
    }

    /**
     * Computes the AvisStats values and saves the AvisStats object for the demarche passed as parameter.
     */
    public void computeAvisStats(DocumentReference demarcheReference, XWikiContext context)
            throws QueryException, XWikiException
    {

        //TODO: we may have to update the query below if the value '0' gets authorized for scores.
        Query query = this.queryManager.createQuery(
                "select avg(score.value), count(doc.fullName) from XWikiDocument as doc, BaseObject as avis,"
                        + "IntegerProperty as score, StringProperty demarche where doc.fullName = avis.name and "
                        + "avis.className='Avis.Code.AvisClass' and avis.id=score.id.id and score.id.name='score' "
                        + "and demarche.id.id = avis.id and demarche.id.name = 'demarche' and demarche.value = "
                        + ":demarche and score.value > 0",
                Query.HQL);
        query = query.bindValue("demarche", compactWikiSerializer.serialize(demarcheReference));
        //TODO: in theory we should set a wiki name
        //it works fine when computing the wiki name from the demarcheReference, not from through
        // context.getWiki().getName()
        //query.setWiki(wikiName);
        List results = query.execute();
        Object[] result = (Object[]) results.get(0);
        double average = (double) result[0];
        long occurrences = (long) result[1];
        setAvisStatsValues(demarcheReference, occurrences, average, true, context);
    }

    /**
     * Computes the AvisStats values and saves the AvisStats objects for all demarches.
     */
    public void computeAvisStats(XWikiContext context) throws QueryException, XWikiException
    {

        //TODO: clarify what to do for demarches having no Avis: set occurrence to 0, and leave the average field
        //empty.

        //TODO: we may have to update the query below if the value '0' gets authorized for scores.

        Query query = this.queryManager.createQuery("select demarche.value, avg(score.value), "
                + "count(doc.fullName) from XWikiDocument as doc, BaseObject as avis,IntegerProperty as score, "
                + "StringProperty demarche where doc.fullName = avis.name and avis.className = 'Avis.Code.AvisClass' "
                + "and avis.id = score.id.id and score.id.name = 'score' and demarche.id.id = avis.id and "
                + "demarche.id.name = 'demarche' and score.value > 0 group by demarche.value order "
                + "by demarche.value", Query.HQL
        );

        List results = query.execute();
        for (Object result : results) {
            Object[] values = (Object[]) result;
            String demarcheId = (String) values[0];
            DocumentReference demarcheReference = documentReferenceResolver.resolve(demarcheId);
            double average = (double) values[1];
            long occurrences = (long) values[2];
            setAvisStatsValues(demarcheReference, occurrences, average, true, context);
        }
    }
}




