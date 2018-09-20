package com.xwiki.projects.dinsic.wikidemarches.extensions.avisstats;

import java.util.Arrays;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Interface (aka Role) of the Component
 */
@Role
public interface AvisStatsManager
{
    EntityReference AVIS_STATS_CLASS_REFERENCE =
            new LocalDocumentReference(Arrays.asList("Avis", "Code"), "AvisStatsClass");
    String OCCURRENCES_PROPERTY_NAME = "occurrences";
    String AVERAGE_PROPERTY_NAME = "moyenne";
    String VOTES_PROPERTY_NAME = "votes";

    void computeAvisStats(DocumentReference demarcheReference, XWikiContext context)
            throws QueryException, XWikiException;

    void computeAvisStats(XWikiContext context)
            throws QueryException, XWikiException;
}



