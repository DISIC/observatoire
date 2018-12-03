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
package com.xwiki.projects.dinsic.wikidemarches.extensions.batchimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.batchimport.BatchImportConfiguration;
import org.xwiki.batchimport.RowDataPostprocessor;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Identifies values of DBLists of the configured class and transforms the received value in stored value. Initialized
 * per lookup so that we can add some cache of the values.
 *
 * @version $Id$
 */
@Component("dblistidentifier")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DBListIdentifierPostprocessor implements RowDataPostprocessor
{

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private Logger logger;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /**
     * Store values cache, for each list to map
     */
    Map<String, Map<String, String>> valuesCache = new HashMap<String, Map<String, String>>();

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.batchimport.RowDataPostprocessor#postProcessRow(java.util.Map, java.util.List, int, java.util.Map,
     *      java.util.List, org.xwiki.batchimport.BatchImportConfiguration)
     */
    @Override
    public void postProcessRow(Map<String, String> data, List<String> row, int rowIndex, Map<String, String> mapping,
            List<String> headers, BatchImportConfiguration config)
    {
        try {
            String classReference = config.getMappingClassName();
            XWikiContext xcontext = this.xwikiContextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            BaseClass mappedClass = xwiki.getXClass(resolver.resolve(classReference), xcontext);
            for (Map.Entry<String, String> me : mapping.entrySet()) {
                String xwikiField = me.getKey();
                PropertyInterface pi = mappedClass.get(xwikiField);
                if (pi instanceof PropertyClass) {
                    PropertyClass prop = (PropertyClass) pi;
                    if (prop instanceof DBListClass) {
                        logger
                                .debug("Found column mapped to " + pi.getName() + " which is a dblist, reverse mapping...");
                        Map<String, String> values = getValuesForProperty(mappedClass, (DBListClass) prop);
                        // find the value corresponding to the current value in the file
                        String dataInFile = data.get(xwikiField);
                        logger.debug("Value to reverse map: " + dataInFile);
                        // split with separator
                        List<String> dataToStore = new ArrayList<>();
                        for (String v : StringUtils.split(dataInFile, config.getListSeparator())) {
                            String valueToAdd = values.get(v);
                            if (StringUtils.isNotBlank(valueToAdd)) {
                                dataToStore.add(valueToAdd);
                            }
                        }
                        // TODO: should escape the list separator, somehow, or call this function once the list parsing
                        // was done...
                        String valueToStore = StringUtils.join(dataToStore, config.getListSeparator());
                        logger.debug("Value reverse mapped to: " + valueToStore);
                        data.put(xwikiField, valueToStore);
                    }
                }

            }
        } catch (XWikiException e) {
            logger.warn("Exception while post processing dblists for mapping " + mapping + " and config " + config
                    + " for row " + rowIndex, e);
        }
    }

    /**
     * Gets the values for the passed class, passed properties (from cache, if they were already cached, or builds the
     * cache and then returns the built list).
     *
     * @param className the class name
     * @param property the property to get values for
     * @return the map with properties
     */
    private Map<String, String> getValuesForProperty(BaseClass xClass, ListClass prop)
    {
        if (this.valuesCache.get(prop.getName()) != null) {
            return this.valuesCache.get(prop.getName());
        } else {
            Map<String, String> propValuesCache = this.buildCacheForProperty(xClass, prop);
            this.valuesCache.put(prop.getName(), propValuesCache);
            return propValuesCache;
        }
    }

    private Map<String, String> buildCacheForProperty(BaseClass className, ListClass property)
    {
        Map<String, String> mapResult = new HashMap<String, String>();
        for (Map.Entry<String, ListItem> me : property.getMap(this.xwikiContextProvider.get()).entrySet()) {
            String displayValue = me.getValue().getValue();
            String existingValue = mapResult.get(displayValue);
            if (existingValue == null) {
                mapResult.put(displayValue, me.getKey());
            }
        }
        return mapResult;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.batchimport.RowDataPostprocessor#getPriority()
     */
    @Override
    public double getPriority()
    {
        return 60;
    }

}