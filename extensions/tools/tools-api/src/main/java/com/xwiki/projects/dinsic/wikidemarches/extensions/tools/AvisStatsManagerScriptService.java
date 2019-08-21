package com.xwiki.projects.dinsic.wikidemarches.extensions.tools;

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

    public void computeAvisStats(DocumentReference demarcheReference)
        throws QueryException, XWikiException
    {
        this.avisStatsComponent.computeAvisStats(demarcheReference, true);
    }

    // TODO: this should not be exposed in a ScriptService since it allows any person with Script rights to
    // launch the computation of the stats
    public void computeAvisStats() throws QueryException, XWikiException
    {
        this.avisStatsComponent.computeAvisStats();
    }
}
