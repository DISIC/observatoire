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

import java.util.HashMap;

/**
 * Holds the configuration of a publication in XWiki objects in documents.
 *
 * @version $Id$
 */
public class PublicationConfiguration extends HashMap<Object, Object>
{
    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 2517122337320988698L;

    /**
     * DENORMALIZE_FIELDS.
     */
    private static final String DENORMALIZE_FIELDS = "denormalizeFields";

    /**
     * EXCLUDE_FIELDS.
     */
    private static final String EXCLUDE_FIELDS = "excludeFields";

    /**
     * QUERY.
     */
    private static final String QUERY = "query";

    /**
     * SOURCE_WIKI.
     */
    private static final String SOURCE_WIKI = "sourceWiki";

    /**
     * TARGET_WIKI.
     */
    private static final String TARGET_WIKI = "targetWiki";

    /**
     * Checks whether the passed string is empty.
     * @param s String
     * @return boolean
     */
    public static boolean isEmpty(String s)
    {
        return s == null || s.trim().isEmpty();
    }

    /**
     * @return String
     */
    public String getQuery()
    {
        return (String) this.get(QUERY);
    }

    /**
     * @param query Query
     */
    public void setQuery(String query)
    {
        this.put(QUERY, query);
    }

    /**
     * @return String[]
     */
    public String[] getExcludeFields()
    {
        return (String[]) this.get(EXCLUDE_FIELDS);
    }

    /**
     * @param fields String[]
     */
    public void setExcludeFields(String[] fields)
    {
        this.put(EXCLUDE_FIELDS, fields);
    }

    /**
     * @return String[]
     */
    public String[] getDenormalizeFields()
    {
        return (String[]) this.get(DENORMALIZE_FIELDS);
    }

    /**
     * @param fields String[]
     */
    public void setDenormalizeFields(String[] fields)
    {
        this.put(DENORMALIZE_FIELDS, fields);
    }

    /**
     * @return The wiki from which the documents should be published: if empty, the current wiki will be used.
     */
    public String getSourceWiki()
    {
        return (String) this.get(SOURCE_WIKI);
    }

    /**
     * @param wikiName The wiki from which the documents should be published: if empty, the current wiki will be used.
     */
    public void setSourceWiki(String wikiName)
    {
        this.put(SOURCE_WIKI, wikiName);
    }

    /**
     * @return The wiki to which the documents should be published: if empty, the current wiki will be used.
     */
    public String getTargetWiki()
    {
        return (String) this.get(TARGET_WIKI);
    }

    /**
     * @param wikiName The wiki to which the documents should be published: if empty, the current wiki will be used.
     */
    public void setTargetWiki(String wikiName)
    {
        this.put(TARGET_WIKI, wikiName);
    }

    /**
     * Checks that the configuration is valid: it has a non-empty query and target wiki.
     * @return boolean
     */
    public boolean isValid()
    {
        return !isEmpty(this.getQuery()) && !isEmpty(this.getTargetWiki());
    }
}
