package com.xwiki.projects.dinsic.wikidemarches.extensions.avisstats;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener firing the computation of AvisStats objects when Avis objects get created. AvisStats objects contain
 * statistics about existing Avis objects related to a given Demarche, such as: number of occurrences, average value.
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
    public static final String LISTENER_NAME = "wikidemarches.listeners.avis";

    protected static final EntityReference AVIS_CLASS_REFERENCE =
            new LocalDocumentReference(Arrays.asList("Avis", "Code"), "AvisClass");

    @Inject
    protected Logger logger;

    /* EntityReference scoreObjectPropertyReference =
             new ObjectPropertyReference(
                     documentReferenceResolver.resolve("wiki:space.page^object.prop", EntityType.OBJECT_PROPERTY));
         reference =
             new ObjectPropertyReference(resolver.resolve("wiki:space.page^x.wiki.class[0].prop",
                 EntityType.OBJECT_PROPERTY));

a priori pas de possibilité d'être notifié de la mise à jour spécifique d'une propriété, pas d'un objet donné, mais de tous les objets

 */
    @Inject
    private AvisStatsComponent avisStatsComponent;

    /**
     * This is the default constructor.
     */
    public AvisEventListener()
    {
        super(LISTENER_NAME, new XObjectAddedEvent(), new XObjectUpdatedEvent(), new XObjectDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        logger.debug("[%s] - Event: [%s] - Source: [%s] - Data: [%s]", LISTENER_NAME, event, source, data);

        XWikiContext context = (XWikiContext) data;

        XWikiDocument document = (XWikiDocument) source;

        BaseObject avis = document.getXObject(AVIS_CLASS_REFERENCE);

        if (event instanceof XObjectAddedEvent) {
            //Creation event
            if (avis != null) {
                avisStatsComponent.anAvisWasAdded(avis, context);
            }
        } else if (event instanceof XObjectUpdatedEvent) {
            //Update event
            //TODO: see how to handle this case. Either the score

        } else if (event instanceof XObjectDeletedEvent) {
            //Deletion event
        }
    }
}
