package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Listener handling:
 * - the restriction of a user page when it gets created, to XWikiAllGroup in view mode
 * - the restriction of a group page when it gets created, to XWikiAllGroup in view mode
 *
 * @version $Id$
 */
@Component
@Named(UserAndGroupEventListener.LISTENER_NAME)
@Singleton
public class UserAndGroupEventListener extends AbstractEventListener {

    static final EntityReference RIGHT_CLASS_REFERENCE = new EntityReference("XWikiRights", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    static final EntityReference USER_CLASS_REFERENCE = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    static final EntityReference GROUP_CLASS_REFERENCE = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    static final EntityReference ALL_GROUP_REFERENCE = new EntityReference("XWikiAllGroup", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    static String VIEW_RIGHT = "view";
    static int ALLOW = 1;

    static final String LISTENER_NAME = "wikidemarches.listeners.users-and-groups";


    @Inject
    protected Logger logger;

    @Inject
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    @Named("currentmixed")
    protected DocumentReferenceResolver<String> referenceResolver;

    public UserAndGroupEventListener() {
        super(LISTENER_NAME, new DocumentCreatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data) {
        logger.debug("Event: [{}] - Source: [{}] - Data: [{}]", LISTENER_NAME, event, source, data);

        XWikiContext context = (XWikiContext) data;
        XWikiDocument page = (XWikiDocument) source;
        if (page != null) {
            BaseObject userOrGroup = page.getXObject(USER_CLASS_REFERENCE);
            if (userOrGroup == null) {
                userOrGroup = page.getXObject(GROUP_CLASS_REFERENCE);
            }
            if (userOrGroup != null) {
                try {
                    updateAccessRights(userOrGroup, context);
                } catch (XWikiException e) {
                    logger.error("Error while updating access rights for: [{}].",
                            compactWikiSerializer.serialize(page.getDocumentReference()), e);

                }
            }

        }


    }

    /**
     * Grant view right to XWikiAllGroup on userOrGroup page
     *
     * @param userOrGroup
     * @param context
     * @throws XWikiException
     */
    public void updateAccessRights(BaseObject userOrGroup, XWikiContext context) throws XWikiException {
        if (userOrGroup != null) {

            XWikiDocument page = userOrGroup.getOwnerDocument();
            if (page != null) {

                BaseObject right = page.newXObject(RIGHT_CLASS_REFERENCE, context);
                right.setLargeStringValue("groups", compactWikiSerializer.serialize(ALL_GROUP_REFERENCE));
                right.setStringValue("levels", VIEW_RIGHT);
                right.setIntValue("allow", ALLOW);
            }

        }


    }


}
