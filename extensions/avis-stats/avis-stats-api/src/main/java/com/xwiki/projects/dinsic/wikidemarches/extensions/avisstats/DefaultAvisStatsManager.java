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
// - Ok: initialisation des démarches qui n'ont pas encore reçu d'avis : il faut leur affecter un AvisStats malgré tout,
// pour pouvoir afficher les statistques pour toutes les démarches, pas seulement celles qui ont reçu un ou plusieurs
// avis
// - Créer un listener qui affecte un objet AvisStats directement à une démarche lors de sa création (pour que les
// statistiques soient valides pour toutes les démarches, sans avoir à passer par le AvisStatsManager pour initialiser
// les AvisStats des démarches sans avis).
// - Voir pourquoi on reçoit null pour "avg(score.value)" quand aucun avis
// - AvisStatsManager : cas où on soumet une démarche inexistante
// - Ok: Scénario problématique : suppression de AvisStats d'une démarche puis suppression d'un Avis lié à cette démarche
// "could not send event to listener"
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
     * Computes the AvisStats values and saves the AvisStats object for the demarche passed as parameter. We assume
     * that the demarcheReference is valid, no check is performed.
     * TODO: see if we should check that the demarcheReference corresponds to an actual Demarche indeed.
     */
    public synchronized void computeAvisStats(DocumentReference demarcheReference, XWikiContext context)
            throws QueryException, XWikiException
    {

        //TODO: we may have to update the query below if the value '0' gets authorized for scores.
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

        //TODO: in theory we should set a wiki name
        //it works fine when computing the wiki name from the demarcheReference, not from through
        // context.getWiki().getName()
        //query.setWiki(wikiName);
        List results = query.execute();
        Object[] result = (Object[]) results.get(0);
        long occurrences = (long) result[0];
        //TODO: check why these value can be null: when no Avis is attached to a Demarche
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

        //TODO: we will have to update the query below if the value '0' gets authorized for scores.
        //TODO: not completely sure we need to use condition "score.value > 0"
        //The query below will return values for Demarches having at least one Avis.
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
        //TODO: see if it's ok to set an 'average' to 0 for Demaches without Avis.

        query = this.queryManager.createQuery(
                "select distinct doc.fullName as fullName from XWikiDocument as doc, BaseObject as obj "
                        + "where obj.className = :demarcheClass and doc.fullName = obj.name and "
                        + "doc.fullName != :demarcheTemplate and doc.fullName not in "
                        + "(select distinct demarche.value from BaseObject as avis, StringProperty demarche "
                        + "where avis.className = :avisClass and demarche.id.id = avis.id and "
                        + "demarche.id.name = :avisDemarcheProperty order by demarche.value)",
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
    }
}




