package com.xwiki.projects.dinsic.wikidemarches.extensions.avisstats;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Make the AviStatsComponent API available to scripting. <br>
 * TODO: add rights verification
 */
@Component
@Named("avisStats")
@Singleton
public class AvisStatsManagerScriptService implements ScriptService
{
    @Inject
    private AvisStatsManager avisStatsComponent;

    public void computeAvisStats(DocumentReference demarcheReference, XWikiContext context)
        throws QueryException, XWikiException
    {
        this.avisStatsComponent.computeAvisStats(demarcheReference, context);
    }

    public void computeAvisStats(XWikiContext context) throws QueryException, XWikiException
    {
        this.avisStatsComponent.computeAvisStats(context);
    }
}
