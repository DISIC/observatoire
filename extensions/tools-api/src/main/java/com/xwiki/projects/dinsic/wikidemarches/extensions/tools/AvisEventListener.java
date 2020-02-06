package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener firing the computation of AvisStats objects when Avis objects get created, updated or deleted. AvisStats
 * objects contain statistics about existing Avis objects related to a given Demarche, such as: number of occurrences,
 * average value. <br>
 * Note: this listener is disabled (removed from components.txt) since mid august 2019, the avis stats cache is updated
 * only once a day, during the night.
 *
 * @version $Id$
 */
@Component
@Named(AvisEventListener.LISTENER_NAME)
@Singleton
public class AvisEventListener extends AbstractEventListener
{
    /**
     * The name of the event listener.
     */
    static final String LISTENER_NAME = "wikidemarches.listeners.avis";

    static final EntityReference AVIS_CLASS_REFERENCE =
        new LocalDocumentReference(Arrays.asList("Avis", "Code"), "AvisClass");

    static final String DEMARCHE_PROPERTY_NAME = "demarche";

    @Inject
    protected Logger logger;

    @Inject
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    protected DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private AvisStatsManager avisStatsComponent;

    /**
     * This is the default constructor.
     */
    public AvisEventListener()
    {
        // - Voir si on pourrait utiliser XObjectPropertyUpdatedEvent pour savoir directement si c'est le champ
        // "demarche" qui
        // a été mis à jour.
        // EntityReference scoreObjectPropertyReference =
        // new ObjectPropertyReference(
        // documentReferenceResolver.resolve("wiki:space.page^object.prop", EntityType.OBJECT_PROPERTY));
        // reference =
        // new ObjectPropertyReference(resolver.resolve("wiki:space.page^x.wiki.class[0].prop",
        // EntityType.OBJECT_PROPERTY));
        // a priori pas de possibilité d'être notifié de la mise à jour spécifique d'une propriété, pas d'un objet
        // donné, mais de tous les objets
        super(LISTENER_NAME, new XObjectAddedEvent(), new XObjectUpdatedEvent(), new XObjectDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        logger.debug("Event: [{}] - Source: [{}] - Data: [{}]", LISTENER_NAME, event, source, data);

        XWikiContext context = (XWikiContext) data;

        XWikiDocument document = (XWikiDocument) source;
        BaseObject avis = document.getXObject(AVIS_CLASS_REFERENCE);
        BaseObject originalAvis = document.getOriginalDocument().getXObject(AVIS_CLASS_REFERENCE);
        try {

            if (avis != null) {
                // 'avis' is not null, hence the Avis object was either created or updated.
                String demarcheId = avis.getStringValue(DEMARCHE_PROPERTY_NAME);
                if (demarcheId != null) {
                    DocumentReference demarcheReference =
                        documentReferenceResolver.resolve(demarcheId, document.getDocumentReference());

                    if (event instanceof XObjectAddedEvent) {
                        avisStatsComponent.computeAvisStats(demarcheReference, true);
                    } else if (event instanceof XObjectUpdatedEvent) {
                        // Update event
                        // Check if the avis was attributed to a distinct a demarche, or if it's another property
                        // that was updated.
                        String originalDemarcheId = originalAvis.getStringValue(DEMARCHE_PROPERTY_NAME);
                        if (!demarcheId.equals(originalDemarcheId)) {
                            // the demarche property was updated, so we need to recompute the AvisStats of both
                            // demarches
                            avisStatsComponent.computeAvisStats(demarcheReference, true);
                            // also update the other demarche's stats, if it exists
                            if (originalDemarcheId != null) {
                                avisStatsComponent
                                    .computeAvisStats(documentReferenceResolver.resolve(originalDemarcheId), true);
                            }
                        } else {
                            // other properties than the demarche one were updated, so we just need to recompute the
                            // AvisStats
                            // of the demarche of the updated Avis object
                            avisStatsComponent.computeAvisStats(demarcheReference, true);
                        }
                    }
                } else {
                    logger.warn("An avis with a null demarche was added, cannot update avis stats");
                }

            } else if (originalAvis != null) {
                // 'avis' is null, 'originalAvis' is not: an Avis was deleted. We double check this is the case by
                // checking
                // that the event is an instance of XObjectDeletedEvent
                if (event instanceof XObjectDeletedEvent) {
                    String originalDemarcheId = originalAvis.getStringValue(DEMARCHE_PROPERTY_NAME);
                    avisStatsComponent.computeAvisStats(documentReferenceResolver.resolve(originalDemarcheId), true);
                }
            } else {
                // No Avis or original Avis was found -> the XObject event relates to another XWiki class: there's
                // nothing to be done.
            }
        } catch (QueryException | XWikiException e) {
            logger.error("Error while updating AvisStats following a change on [{}].",
                compactWikiSerializer.serialize(avis.getDocumentReference()), e);
        }
    }
}
