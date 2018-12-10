package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

import java.util.Arrays;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Interface (aka Role) of the Component
 */
@Role
public interface AvisStatsManager
{
    EntityReference AVIS_STATS_CLASS_REFERENCE =
        new LocalDocumentReference(Arrays.asList("Avis", "Code"), "AvisStatsClass");

    String OCCURRENCES_PROPERTY_NAME = "occurrences";

    String SATISFACTION_INDEX_PROPERTY_NAME = "moyenne";

    String VOTES_PROPERTY_NAME = "votes";

    String DEMARCHE_CLASS_NAME = "Demarches.Code.DemarchesClass";

    String AVIS_CLASS_NAME = "Avis.Code.AvisClass";

    String DEMARCHE_TEMPLATE_NAME = "Demarches.Code.DemarchesTemplate";

    String AVIS_DEMARCHE_PROPERTY_NAME = "demarche";

    String AVIS_SCORE_PROPERTY_NAME = "score";

    String AVIS_VOTE_PROPERTY_NAME = "vote";

    void computeAvisStats(DocumentReference demarcheReference, XWikiContext context)
        throws QueryException, XWikiException;

    void computeAvisStats(XWikiContext context) throws QueryException, XWikiException;
}
