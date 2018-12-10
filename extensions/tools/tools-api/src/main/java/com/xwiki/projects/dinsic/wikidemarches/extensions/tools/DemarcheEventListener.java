package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener firing the creation of an AvisStats object in a Demarche page when such a page gets created.
 *
 * @version $Id$
 */
@Component
@Named(DemarcheEventListener.LISTENER_NAME)
@Singleton
public class DemarcheEventListener extends AbstractEventListener
{
    /**
     * The name of the event listener.
     */
    static final String LISTENER_NAME = "wikidemarches.listeners.demarche";

    static final EntityReference DEMARCHE_CLASS_REFERENCE =
        new LocalDocumentReference(Arrays.asList("Demarches", "Code"), "DemarchesClass");

    @Inject
    protected Logger logger;

    @Inject
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    private AvisStatsManager avisStatsComponent;

    /**
     * This is the default constructor.
     */
    public DemarcheEventListener()
    {
        super(LISTENER_NAME, new DocumentCreatedEvent(), new XObjectPropertyUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        logger.debug("[%s] - Event: [%s] - Source: [%s] - Data: [%s]", LISTENER_NAME, event, source, data);

        XWikiContext context = (XWikiContext) data;

        XWikiDocument document = (XWikiDocument) source;
        BaseObject demarche = document.getXObject(DEMARCHE_CLASS_REFERENCE);
        if (event instanceof  DocumentCreatedEvent) {
            // Initialize the cache for the Demarche, and cache the digitization date in case the Demarche
            // is already digitized.
            try {
                if (demarche != null) {
                    avisStatsComponent.computeAvisStats(document.getDocumentReference(), context);
                }
            } catch (QueryException | XWikiException e) {
                logger.error("Error while adding an AvisStats object to a Demarche: [%s].",
                        compactWikiSerializer.serialize(document.getDocumentReference()), e);
            }
        } else if (event instanceof XObjectPropertyUpdatedEvent) {
            // In case the Demarche property "niveauDemat" was updated to a value representing digitization,
            // update the digitization date value in the cache.
            Version currentVersion = null;
            try {
                currentVersion = document.getDocumentArchive(context).getLatestVersion();
                Version previousVersion = document.getDocumentArchive(context).getPrevVersion(currentVersion);
            } catch (XWikiException e) {
                e.printStackTrace();
            }


        }
    }
}
