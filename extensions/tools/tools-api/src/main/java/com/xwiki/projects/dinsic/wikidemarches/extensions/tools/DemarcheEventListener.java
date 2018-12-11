package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
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
    public static final List<String> DIGITIZATION_LEVELS = Arrays.asList("teleprocedure", "formulaire");

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
        super(LISTENER_NAME, new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        logger.debug("[%s] - Event: [%s] - Source: [%s] - Data: [%s]", LISTENER_NAME, event, source, data);

        XWikiContext context = (XWikiContext) data;

        XWikiDocument newDocument = (XWikiDocument) source;
        BaseObject demarche = newDocument.getXObject(DEMARCHE_CLASS_REFERENCE);
        XWikiDocument previousDocument = newDocument.getOriginalDocument();

        if (demarche != null) {
            maybeUpdateDigitizationDate(previousDocument, newDocument);
            if (event instanceof DocumentCreatingEvent) {
                try {
                    // In case of a DocumentCreatingEvent, initialize the AvisStats
                    avisStatsComponent.computeAvisStats(newDocument.getDocumentReference(), false, context);
                } catch (QueryException | XWikiException e) {
                    logger.error("Error while adding an AvisStats object to a Demarche: [%s].",
                            compactWikiSerializer.serialize(newDocument.getDocumentReference()), e);
                }
            }
        }
    }

    protected boolean isDigitized(BaseObject demarche)
    {
        if (demarche == null) {
            return false;
        }
        List niveauDemat = demarche.getListValue("niveauDemat");
        if (niveauDemat == null || niveauDemat.size() == 0) {
            return false;
        }
        for (String level : DIGITIZATION_LEVELS) {
            if (niveauDemat.contains(level)) {
                return true;
            }
        }
        return false;
    }

    protected boolean demarcheWasDigitized(BaseObject previousDemarche, BaseObject newDemarche)
    {
        return !isDigitized(previousDemarche) && isDigitized(newDemarche);
    }

    protected boolean demarcheWasUndigitized(BaseObject previousDemarche, BaseObject newDemarche)
    {
        return isDigitized(previousDemarche) && !isDigitized(newDemarche);
    }

    /**
     * Computes the digitization date and returns true if it has changed.
     */
    public boolean maybeUpdateDigitizationDate(XWikiDocument previousDocument, XWikiDocument newDocument)
    {
        if (newDocument == null) {
            return false;
        }

        BaseObject newDemarche = newDocument.getXObject(DEMARCHE_CLASS_REFERENCE);
        BaseObject previousDemarche = null;
        if (previousDocument != null) {
            previousDemarche = previousDocument.getXObject(DEMARCHE_CLASS_REFERENCE);
        }

        if (demarcheWasDigitized(previousDemarche, newDemarche)) {
            //Update the date only in case it was not set already
            Date dateDemat = newDemarche.getDateValue("dateDemat");
            if (dateDemat == null) {
                newDemarche.setDateValue("dateDemat", new Date());
            }
            return true;
        } else if (demarcheWasUndigitized(previousDemarche, newDemarche)) {
            newDemarche.setDateValue("dateDemat", null);
            return true;
        }
        return false;
    }
}
