package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Listener handling:
 * - the creation of an AvisStats object in a Demarche page when such a page gets created,
 * - the update of a  Demarche rights when a property "proprietaires" gets updated,
 * - the update of a Demarche digitalization date.
 *
 * @version $Id$
 */
@Component
@Named(DemarcheEventListener.LISTENER_NAME)
@Singleton
public class DemarcheEventListener extends AbstractEventListener {

    static final EntityReference RIGHT_CLASS_REFERENCE = new EntityReference("XWikiRights", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    static final String ADMINISTRATEURS_MINISTERIELS_GROUP = "XWiki.AdministrateursMinisterielsGroup";

    static final List<String> DIGITIZATION_LEVELS = Arrays.asList("teleprocedure", "formulaire");

    /**
     * The name of the event listener.
     */
    static final String LISTENER_NAME = "wikidemarches.listeners.demarche";

    static final EntityReference DEMARCHE_CLASS_REFERENCE =
            new LocalDocumentReference(Arrays.asList("Demarches", "Code"), "DemarchesClass");

    static final String DEMARCHE_PROPERTY_OWNERS = "proprietaires";
    static final String DEMARCHE_PROPERTY_DIGITALIZATION_DATE = "dateDemat";
    static final String DEMARCHE_PROPERTY_DIGITALIZATION_LEVEL = "niveauDemat";

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
    public DemarcheEventListener() {
        super(LISTENER_NAME, new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data) {
        logger.debug("[%s] - Event: [%s] - Source: [%s] - Data: [%s]", LISTENER_NAME, event, source, data);

        XWikiContext context = (XWikiContext) data;

        XWikiDocument pageV2 = (XWikiDocument) source;
        BaseObject demarche = pageV2.getXObject(DEMARCHE_CLASS_REFERENCE);
        XWikiDocument pageV1 = pageV2.getOriginalDocument();

        if (demarche != null) {
            maybeUpdateDigitizationDate(pageV1, pageV2);
            try {
                maybeUpdateAccessRights(pageV1, pageV2, context);
            } catch (XWikiException e) {
                logger.error("Error while updating rights on: [%s].",
                        compactWikiSerializer.serialize(pageV2.getDocumentReference()), e);
            }
            if (event instanceof DocumentCreatingEvent) {
                try {
                    // In case of a DocumentCreatingEvent, initialize the AvisStats
                    avisStatsComponent.computeAvisStats(pageV2.getDocumentReference(), false, context);
                } catch (QueryException | XWikiException e) {
                    logger.error("Error while adding an AvisStats object to a Demarche: [%s].",
                            compactWikiSerializer.serialize(pageV2.getDocumentReference()), e);
                }
            }
        }
    }

    protected boolean isDigitized(BaseObject demarche) {
        if (demarche == null) {
            return false;
        }
        List niveauDemat = demarche.getListValue(DEMARCHE_PROPERTY_DIGITALIZATION_LEVEL);
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

    protected boolean demarcheWasDigitized(BaseObject demarcheV1, BaseObject demarcheV2) {
        return !isDigitized(demarcheV1) && isDigitized(demarcheV2);
    }

    protected boolean demarcheWasUndigitized(BaseObject demarcheV1, BaseObject demarcheV2) {
        return isDigitized(demarcheV1) && !isDigitized(demarcheV2);
    }

    /**
     * Computes the digitization date and returns true if it has changed.
     */
    public boolean maybeUpdateDigitizationDate(XWikiDocument demarchePageV1, XWikiDocument demarchePageV2) {
        if (demarchePageV2 == null) {
            return false;
        }

        BaseObject demarcheV2 = demarchePageV2.getXObject(DEMARCHE_CLASS_REFERENCE);
        BaseObject demarcheV1 = null;
        if (demarchePageV1 != null) {
            demarcheV1 = demarchePageV1.getXObject(DEMARCHE_CLASS_REFERENCE);
        }

        if (demarcheWasDigitized(demarcheV1, demarcheV2)) {
            //Update the date only in case it was not set already
            Date dateDemat = demarcheV2.getDateValue(DEMARCHE_PROPERTY_DIGITALIZATION_DATE);
            if (dateDemat == null) {
                demarcheV2.setDateValue(DEMARCHE_PROPERTY_DIGITALIZATION_DATE, new Date());
            }
            return true;
        } else if (demarcheWasUndigitized(demarcheV1, demarcheV2)) {
            demarcheV2.setDateValue(DEMARCHE_PROPERTY_DIGITALIZATION_DATE, null);
            return true;
        }
        return false;
    }

    /**
     * Checks if property "proprietaires" has changed, and update the current demarche access rights accordingly by
     * removing all rights related to users, and allowing edit right to owners and to administrateurs ministeriels.
     */
    public boolean maybeUpdateAccessRights(XWikiDocument demarchePageV1, XWikiDocument demarchePageV2, XWikiContext context) throws XWikiException {
        if (demarchePageV2 == null) {
            return false;
        }

        BaseObject demarcheV2 = demarchePageV2.getXObject(DEMARCHE_CLASS_REFERENCE);
        BaseObject demarcheV1 = null;
        if (demarchePageV1 != null) {
            demarcheV1 = demarchePageV1.getXObject(DEMARCHE_CLASS_REFERENCE);
        }

        if (ownersWereUpdated(demarcheV1, demarcheV2)) {
            // Remove all rights related to users
            List<BaseObject> rights = demarchePageV2.getXObjects(RIGHT_CLASS_REFERENCE);
            if (rights != null) {
                List<BaseObject> toBeRemovedRights = new ArrayList<>();
                for (BaseObject right : rights) {
                    if (right != null && StringUtils.isNotEmpty(right.getLargeStringValue("users"))) {
                        toBeRemovedRights.add(right);
                    }
                }
                for (BaseObject right : toBeRemovedRights) {
                    demarchePageV2.removeXObject(right);
                }
            }

            // Grant edit rights to the new owners, if not empty, and also to the administrateurs ministeriels
            String ownerString = demarcheV2.getLargeStringValue(DEMARCHE_PROPERTY_OWNERS);
            if (StringUtils.isNotEmpty(ownerString)) {
                BaseObject right = demarchePageV2.newXObject(RIGHT_CLASS_REFERENCE, context);
                right.setLargeStringValue("groups", ADMINISTRATEURS_MINISTERIELS_GROUP);
                right.setLargeStringValue("users", ownerString);
                right.setStringValue("levels", "edit");
                right.setIntValue("allow", 1);
            }

            return true;

        }

        return false;
    }

    protected boolean ownersWereUpdated(BaseObject demarcheV1, BaseObject demarcheV2) {
        String ownersV1 = demarcheV1.getLargeStringValue(DEMARCHE_PROPERTY_OWNERS);
        String ownersV2 = demarcheV2.getLargeStringValue(DEMARCHE_PROPERTY_OWNERS);
        if ((ownersV2 != null && !ownersV2.equals(ownersV1)) || (ownersV2 == null && ownersV1 != null)) {
            return true;
        }
        return false;
    }

}
