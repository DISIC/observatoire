package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailResult;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.*;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.*;
import java.util.*;

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

    static final String ADMINISTRATEURS_MINISTERIELS_GROUP = "XWiki.AdministrateursMinisteriels";

    static final List<String> DIGITIZATION_LEVELS = Arrays.asList("teleprocedure", "formulaire");

    /**
     * The name of the event listener.
     */
    static final String LISTENER_NAME = "wikidemarches.listeners.demarche";

    static final EntityReference DEMARCHE_CLASS_REFERENCE =
            new LocalDocumentReference(Arrays.asList("Demarches", "Code"), "DemarchesClass");

    static final String DEMARCHE_PROPERTY_OWNERS = "proprietaires";
    static final String DEMARCHE_PROPERTY_DIRECTION = "direction";
    static final String DEMARCHE_PROPERTY_DIGITALIZATION_DATE = "dateDemat";
    static final String DEMARCHE_PROPERTY_DIGITALIZATION_LEVEL = "niveauDemat";

    @Inject
    protected Logger logger;

    @Inject
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    @Named("currentmixed")
    protected DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private AvisStatsManager avisStatsComponent;

    @Inject
    private MailSenderConfiguration configuration;
    @Inject
    private MailSender mailSender;
    @Inject
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    /**
     * This is the default constructor.
     */
    public DemarcheEventListener() {
        super(LISTENER_NAME, new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data) {
        logger.debug("Event: [{}] - Source: [{}] - Data: [{}]", LISTENER_NAME, event, source, data);

        XWikiContext context = (XWikiContext) data;
        XWikiDocument pageV2 = (XWikiDocument) source, pageV1 = null;
        BaseObject demarcheV1 = null, demarcheV2 = null;
        if (pageV2 != null) {
            pageV1 = pageV2.getOriginalDocument();
            demarcheV2 = pageV2.getXObject(DEMARCHE_CLASS_REFERENCE);
        }

        if (pageV1 != null) {
            demarcheV1 = pageV1.getXObject(DEMARCHE_CLASS_REFERENCE);
        }

        if (demarcheV2 != null) {
            maybeUpdateDigitizationDate(demarcheV1, demarcheV2);
            try {
                boolean ownerChanged = maybeUpdateAccessRights(demarcheV1, demarcheV2, context);
                if (ownerChanged) {
                    sendNotification(demarcheV1, demarcheV2, context);
                }

            } catch (XWikiException | MessagingException e) {
                logger.error("Error while updating rights on: [{}].",
                        compactWikiSerializer.serialize(pageV2.getDocumentReference()), e);
            }
            if (event instanceof DocumentCreatingEvent) {
                try {
                    // In case of a DocumentCreatingEvent, initialize the AvisStats
                    avisStatsComponent.computeAvisStats(pageV2.getDocumentReference(), false, context);
                } catch (QueryException | XWikiException e) {
                    logger.error("Error while adding an AvisStats object to a Demarche: [{}].",
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
    public boolean maybeUpdateDigitizationDate(BaseObject demarcheV1, BaseObject demarcheV2) {
        if (demarcheV2 == null) {
            return false;
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
    public boolean maybeUpdateAccessRights(BaseObject demarcheV1, BaseObject demarcheV2, XWikiContext context) throws XWikiException {
        if (demarcheV2 == null) {
            return false;
        }

        XWikiDocument pageV2 = demarcheV2.getOwnerDocument();
        if (ownersWereUpdated(demarcheV1, demarcheV2) && pageV2 != null) {
            // Remove all rights related to users
            List<BaseObject> rights = pageV2.getXObjects(RIGHT_CLASS_REFERENCE);
            if (rights != null) {
                List<BaseObject> toBeRemovedRights = new ArrayList<>();
                for (BaseObject right : rights) {
                    if (right != null && StringUtils.isNotEmpty(right.getLargeStringValue("users"))) {
                        toBeRemovedRights.add(right);
                    }
                }
                for (BaseObject right : toBeRemovedRights) {
                    pageV2.removeXObject(right);
                }
            }

            // Grant edit rights to the new owners, if not empty, and also to the administrateurs ministeriels
            String ownerString = demarcheV2.getLargeStringValue(DEMARCHE_PROPERTY_OWNERS);
            if (StringUtils.isNotEmpty(ownerString)) {
                BaseObject right = pageV2.newXObject(RIGHT_CLASS_REFERENCE, context);
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

    protected void sendNotification(BaseObject demarcheV1, BaseObject demarcheV2, XWikiContext context) throws MessagingException, XWikiException {
        String ownerStringV1 = null, ownerStringV2 = null;
        ownerStringV1 = demarcheV1.getLargeStringValue(DEMARCHE_PROPERTY_OWNERS);
        ownerStringV2 = demarcheV2.getLargeStringValue(DEMARCHE_PROPERTY_OWNERS);

        if (ownerStringV1 != null || ownerStringV2 != null) {

            List<User> ownersV1 = getUsers(ownerStringV1, context);
            List<User> ownersV2 = getUsers(ownerStringV2, context);
            Session session = Session.getInstance(configuration.getAllProperties());

            MailListener mailListener = mailListenerProvider.get();
            DocumentReference demarcheV2Reference = demarcheV2.getDocumentReference();
            // TODO: get localized message
            String subject = "Mise à jour des porteurs de la démarche " + demarcheV2Reference.getName();

            String directionId = demarcheV2.getStringValue(DEMARCHE_PROPERTY_DIRECTION);
            XWiki wiki = context.getWiki();
            String groupId = "XWiki.Groups." + directionId;
            Collection<String> directionUserIds = wiki.getGroupService(context).getAllMembersNamesForGroup(groupId, 0, 0, context);
            Collection<String> administrateursMinisterielsUserIds = wiki.getGroupService(context).getAllMembersNamesForGroup(ADMINISTRATEURS_MINISTERIELS_GROUP, 0, 0, context);
            logger.debug("Direction: [{}]. Group: [{}]. Administrateurs ministériels: [{}].", directionId, groupId, administrateursMinisterielsUserIds);
            directionUserIds.retainAll(administrateursMinisterielsUserIds);
            logger.debug("Direction: [{}]. Group: [{}]. Administrateurs ministériels: [{}].", directionId, groupId, directionUserIds);
            List<User> directionMembers = getUsers(StringUtils.join(directionUserIds, ","), context);
            logger.debug("Direction: [{}]. Group: [{}]. Administrateurs ministériels: [{}].", directionId, groupId, directionMembers);

            MimeMessage message = createMimeMessage(session, subject, demarcheV2Reference.getName(), ownersV1, ownersV2, directionMembers);
            logger.debug("Sending notification about changes regarding owners of [{}].", demarcheV2Reference.getName());
            MailResult result = mailSender.sendAsynchronously(Arrays.asList(message), session, mailListener);
        }

    }

    protected List<User> getUsers(String concatenatedUserIdentifiers, XWikiContext context) {
        List<User> users = new ArrayList<User>();
        XWiki wiki = context.getWiki();
        for (String userName : StringUtils.split(concatenatedUserIdentifiers, ",")) {
            if (userName != null) {
                User user = wiki.getUser(userName, context);
                if (user != null) {
                    users.add(user);
                }
            }
        }
        return users;

    }

    protected String convertUserListToString(List<User> users) {
        List<String> list = new ArrayList<>();
        for (User user : users) {
            if (user != null && user.getUser() != null && !StringUtils.isEmpty(user.getUser().getUser())) {
                list.add(user.getUser().getUser());
            }
        }
        if (list.size() == 0) {
            return "aucun";
        }
        return StringUtils.join(list, ", ");

    }

    protected MimeMessage createMimeMessage(Session session, String subject, String demarcheId, List<User> ownersV1, List<User> ownersV2, List<User> directionMembers) throws MessagingException {


        MimeMessage message = new MimeMessage(session);
        // TODO: set from correctly
        message.setFrom(new InternetAddress("info@nosdemarches.gouv.fr"));
        Map<DocumentReference, User> recipients = new HashMap<>();
        for (User user : ownersV1) {
            DocumentReference key = referenceResolver.resolve(user.getUser().getUser());
            recipients.put(key, user);
        }

        for (User user : ownersV2) {
            DocumentReference key = referenceResolver.resolve(user.getUser().getUser());
            if (!recipients.containsKey(key)) {
                recipients.put(key, user);
            }
        }
        for (User user : directionMembers) {
            DocumentReference key = referenceResolver.resolve(user.getUser().getUser());
            if (!recipients.containsKey(key)) {
                recipients.put(key, user);
            }
        }
        for (User user : recipients.values()) {
            String email = user.getEmail();
            if (StringUtils.isNotEmpty(email)) {
                message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(email));
            }
        }


        MimeBodyPart bodyPart = new MimeBodyPart();
        // TODO: get message from template
        // TODO: add demarche URL
        String text = "Madame, Monsieur,\n\nLa liste des porteurs de la démarche ci-dessous a été mise à jour.\n\n";
        text += "- Démarche : " + demarcheId + "\n";
        text += "- Ancien(s) porteur(s) : " + convertUserListToString(ownersV1) + "\n";
        text += "- Nouveau(x) porteur(s) : " + convertUserListToString(ownersV2) + "\n";
        message.setSubject(subject);
        bodyPart.setText(text);
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(bodyPart);
        message.setContent(multipart);
        return message;

    }

}
