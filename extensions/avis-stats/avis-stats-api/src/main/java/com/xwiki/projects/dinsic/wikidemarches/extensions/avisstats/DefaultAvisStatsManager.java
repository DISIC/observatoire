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

// TODO:
// - Ajouter la propriété de comptage des votes et vérifier si la requête est ok du point de vue performances ou s'il
// vaut mieux recoder la propriété 'vote' en booléen
// - Voir ce qu'on fait avec les démarches qui n'ont pas encore reçu d'avis : il faut leur affecter un AvisStats malgré
// tout
// - Voir si on pourrait utiliser XObjectPropertyUpdatedEvent pour savoir directement si c'est le champ "demarche" qui
//   a été mis à jour.
//      EntityReference scoreObjectPropertyReference =
//        new ObjectPropertyReference(
//        documentReferenceResolver.resolve("wiki:space.page^object.prop", EntityType.OBJECT_PROPERTY));
//        reference =
//        new ObjectPropertyReference(resolver.resolve("wiki:space.page^x.wiki.class[0].prop",
//        EntityType.OBJECT_PROPERTY));
//        a priori pas de possibilité d'être notifié de la mise à jour spécifique d'une propriété, pas d'un objet donné, mais de tous les objets
// - Vérifier la thread safety
//  - Vérifier si le "synchronized" est réellement nécessaire sur la méthode "setAvisStatsValues".
// - Prise en compte de / du:
//  - Renommage d'un avis
// - Etudier l'annotation "@NotNull XWikiContext context" sur "setAvisStatsValues"
// - Vérifier ce qu'il se passe quand il n'y a pas d'avis et vérifier la cohérence entre les deux computeAvisStats
// - Ecrire des scénarios de tests
// - Ecrire des tests

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
     * Computes the AvisStats values and saves the AvisStats object for the demarche passed as parameter.
     */
    public synchronized void computeAvisStats(DocumentReference demarcheReference, XWikiContext context)
            throws QueryException, XWikiException
    {

        //TODO: we may have to update the query below if the value '0' gets authorized for scores.
        Query query = this.queryManager.createQuery(
                "select count(*), avg(score.value), sum(case when vote.value='true' then 1 else 0 end) from "
                        + "BaseObject as avis, IntegerProperty as score, StringProperty as vote, "
                        + "StringProperty demarche where avis.className = 'Avis.Code.AvisClass' "
                        + "and avis.id = score.id.id and score.id.name = 'score' "
                        + "and avis.id = vote.id.id and vote.id.name = 'vote' "
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
        long occurrences = (long) result[0];
        double average = (double) result[1];
        long votes = (long) result[2];
        setAvisStatsValues(demarcheReference, occurrences, average, votes,true, context);
    }

    /**
     * Computes the AvisStats values and saves the AvisStats objects for all demarches.
     */
    public void computeAvisStats(XWikiContext context) throws QueryException, XWikiException
    {

        //TODO: clarify what to do for demarches having no Avis: set occurrence to 0, and leave the average field
        //empty.

        //TODO: we may have to update the query below if the value '0' gets authorized for scores.

        Query query = this.queryManager.createQuery("select demarche.value, count(*), avg(score.value), "
                + "sum(case when vote.value='true' then 1 else 0 end) from BaseObject as avis, "
                + "IntegerProperty as score, StringProperty as vote, StringProperty demarche where doc.fullName "
                + "= avis.name and avis.className = 'Avis.Code.AvisClass' "
                + "and avis.id = score.id.id and score.id.name = 'score' and demarche.id.id = avis.id and "
                + "and avis.id = vote.id.id and vote.id.name = 'vote' and demarche.id.id = avis.id and "
                + "demarche.id.name = 'demarche' and score.value > 0 group by demarche.value order "
                + "by demarche.value", Query.HQL
        );

        List results = query.execute();
        for (Object result : results) {
            Object[] values = (Object[]) result;
            String demarcheId = (String) values[0];
            DocumentReference demarcheReference = documentReferenceResolver.resolve(demarcheId);
            long occurrences = (long) values[1];
            double average = (double) values[2];
            long votes = (long) values[3];
            setAvisStatsValues(demarcheReference, occurrences, average, votes,true, context);
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
            wiki.saveDocument(demarche, "Recalcul des valeurs de l'objet 'AvisStats'.", context);
        }
    }
}




