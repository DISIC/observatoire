/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.publication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.DefaultRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Superclass of publication jobs defining general behaviour and utility methods.
 *
 * @version $Id$
 */
public abstract class AbstractPublicationJob extends AbstractJob<DefaultRequest, DefaultJobStatus<DefaultRequest>>
{
    /**
     * Job configuration key used in the Job request, typically in a Groovy script for setting configuration objects in
     * the request.
     */
    public static final String PUBLICATION_JOB_CONFIGURATION_KEY = "publication.configuration.key";

    @Inject
    protected Execution execution;

    @Inject
    protected DocumentReferenceResolver<String> resolver;

    @Inject
    protected EntityReferenceSerializer<String> serializer;

    @Inject
    protected QueryManager queryManager;

    /**
     * Build a map of classes / field lists from the property value of propName declared as a list of strings
     * ClassFullName:FieldName (one field per row).
     */
    protected static final Map<String, List<String>> buildClassFieldsMap(String[] fields)
    {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (int i = 0; i < fields.length; i++) {
            String[] ePair = fields[i].trim().split(":");
            if (ePair.length == 2) {
                String className = ePair[0].trim();
                String fieldName = ePair[1].trim();
                if (className.length() > 0 && fieldName.length() > 0) {
                    List classFields = map.get(className);
                    if (classFields == null) {
                        classFields = new ArrayList<String>();
                        map.put(className, classFields);
                    }
                    classFields.add(fieldName);
                }
            }
        }
        return map;
    }

    @Override
    protected void runInternal()
    {
        try {

            logger.info("Running PublicationJob");
            PublicationConfiguration config = getRequest().getProperty(PUBLICATION_JOB_CONFIGURATION_KEY);
            if (config != null && config.isValid()) {
                this.doPublish(config);
            } else {
                logger.warn("Invalid configuration found %s:", config);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception while running job", e);
        }
    }

    protected abstract void doPublish(PublicationConfiguration config) throws QueryException, XWikiException;

    protected XWikiDocument getDocument(String wiki, String docFullName) throws XWikiException
    {

        DocumentReference documentReference = resolver.resolve(docFullName, new WikiReference(wiki));
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();
        return xwiki.getDocument(documentReference, xcontext);
    }

    protected List<String> executeQuery(String wikiName, String xwql) throws QueryException
    {

        Query query = this.queryManager.createQuery(xwql, Query.XWQL);
        query.setWiki(wikiName);
        List<String> entries = query.execute();
        return entries;
    }

    protected XWikiContext getXWikiContext()
    {
        ExecutionContext ec = execution.getContext();
        XWikiContext xcontext = (XWikiContext) ec.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        return xcontext;
    }
}
