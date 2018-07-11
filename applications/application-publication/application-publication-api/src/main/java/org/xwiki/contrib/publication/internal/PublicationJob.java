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
package org.xwiki.contrib.publication.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.publication.AbstractPublicationJob;
import org.xwiki.contrib.publication.PublicationConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.PropertyClass;

//TODO: publish attachments

/**
 * Publication job.
 *
 * @version $Id$
 */
@Component
@Named(PublicationJob.JOB_TYPE)
public class PublicationJob extends AbstractPublicationJob
{
    /**
     * The job type.
     */
    public static final String JOB_TYPE = "publication";

    protected void doPublish(PublicationConfiguration config) throws QueryException, XWikiException
    {
        //TODO: add progress tracking via services.progress
        progressManager.startStep("Read input");
        Map<String, List<String>> skipFieldsMap = buildClassFieldsMap(config.getExcludeFields());
        Map<String, List<String>> denormalizeFieldsMap = buildClassFieldsMap(config.getDenormalizeFields());
        List list = this.executeQuery(config.getSourceWiki(), config.getQuery());
        if (list.size() > 0) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                String docFullName = (String) it.next();
                XWikiDocument sourceDocument = getDocument(config.getSourceWiki(), docFullName);
                XWikiDocument targetDocument = getDocument(config.getTargetWiki(), docFullName).clone();
                publishDocument(sourceDocument, targetDocument, skipFieldsMap, denormalizeFieldsMap);
            }
        }
    }

    protected void publishDocument(XWikiDocument sourceDocument, XWikiDocument targetDocument,
            Map<String, List<String>> skipFieldsMap, Map<String, List<String>> denormalizeFieldsMap)
            throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();
        XWiki xwiki = xcontext.getWiki();
        // copy the title and the content of the source document to the target document
        targetDocument.setTitle(sourceDocument.getTitle());
        targetDocument.setContent(sourceDocument.getContent());
        //Set default language of the target document: this must absolutely be done here and nowhere else (after having
        //set stuff on the document, so that the clone of the document is already created before we call .getDocument()
        //and this acts on the clone)
        targetDocument.setDefaultLocale(sourceDocument.getDefaultLocale());
        publishObjects(sourceDocument, targetDocument, skipFieldsMap, denormalizeFieldsMap);
        XWikiDocument originalTargetDocument = xwiki.getDocument(targetDocument.getDocumentReference(), xcontext);
        if (!originalTargetDocument.equals(targetDocument)) {
            xwiki.saveDocument(targetDocument, "publication-job", xcontext);
            logger.info("Document {} was saved.", serializer.serialize(targetDocument.getDocumentReference()));
//            System.out.println("Document was saved: " + serializer.serialize(targetDocument.getDocumentReference()));
        } else {
            logger.info("Document was not updated since the last publication: {}.",
                    serializer.serialize(targetDocument.getDocumentReference()));
//            System.out.println("Document was not updated since the last publication: " + serializer
//                    .serialize(targetDocument.getDocumentReference()));
        }
    }

    protected void publishObjects(XWikiDocument sourceDocument, XWikiDocument targetDocument,
            Map<String, List<String>> skipFieldsMap, Map<String, List<String>> denormalizeFieldsMap)
            throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();

        //copy the objects of this document, paying attention to the skip list and to the denormalize list
        //TODO: this script does not handle non-contiguous objects, it always adds them contiguous, so if there is a
        //hole in the objects of the source document and it's published twice, this will create as many objects in the
        //target documents as there are holes in the source document. I'm not handling this case now because it's
        //overcomplicated
        Set<Map.Entry<DocumentReference, List<BaseObject>>> objectsByClass = sourceDocument.getXObjects().entrySet();
        Iterator<Map.Entry<DocumentReference, List<BaseObject>>> itObjectsByClass = objectsByClass.iterator();
        while (itObjectsByClass.hasNext()) {
            Map.Entry<DocumentReference, List<BaseObject>> objectsByClassEntry = itObjectsByClass.next();
            DocumentReference classToCopy = objectsByClassEntry.getKey();
            logger.info("Copying all objects of class {}", classToCopy);
            LocalDocumentReference localClassToCopy = classToCopy.getLocalDocumentReference();
            String className = serializer.serialize(localClassToCopy);
            List<String> excludeFields = skipFieldsMap.get(className);
            List<String> denormalizeFields = denormalizeFieldsMap.get(className);
            List<BaseObject> objectsToCopy = objectsByClassEntry.getValue();
            Iterator<BaseObject> itObjectToCopy = objectsToCopy.iterator();
            while (itObjectToCopy.hasNext()) {
                BaseObject objectToCopy = itObjectToCopy.next();
                if (objectToCopy != null) {
                    BaseObject targetObject = targetDocument.getXObject(localClassToCopy, objectToCopy.getNumber());
                    if (targetObject == null) {
                        targetObject = targetDocument.newXObject(localClassToCopy, xcontext);
                    }
                    publishObject(objectToCopy, targetObject, excludeFields, denormalizeFields);
                }
            }
        }
    }

    protected void publishObject(BaseObject objectToCopy, BaseObject targetObject, List<String> excludeFields,
            List<String> denormalizeFields) throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();

        //copy all properties except for the skips and denormalize the declared properties
        Object[] propertyClasses = objectToCopy.getXClass(xcontext).getProperties();
        for (int i = 0; i < propertyClasses.length; i++) {
            PropertyClass propertyClass = (PropertyClass) propertyClasses[i];
            if (excludeFields == null || !excludeFields.contains(propertyClass.getName())) {
                PropertyInterface objectPropertyInterface = objectToCopy.get(propertyClass.getName());
                if (objectPropertyInterface instanceof BaseProperty) {

                    BaseProperty objectBaseProperty = (BaseProperty) objectPropertyInterface;
                    Object objectPropertyValue = objectBaseProperty.getValue();

                    if (denormalizeFields != null && denormalizeFields.contains(propertyClass.getName())) {
                        targetObject.set(propertyClass.getName(), denormalize(objectPropertyValue, propertyClass),
                                xcontext);
                    } else {
                        //no denormalization, plain value copy
                        //TODO: why can't we simply do set(propertyClass.getName(), objectPropertyValue, xcontext)
                        //whatever type is objectPropertyValue: a list or not?
                        if ((objectPropertyValue instanceof List) && (((List) objectPropertyValue).size() > 0)) {
                            //we have a multi-valued property
                            List<Object> values = new ArrayList<>();
                            Iterator itValues = ((List) objectPropertyValue).iterator();
                            while (itValues.hasNext()) {
                                Object value = itValues.next();
                                values.add(value);
                            }
                            targetObject.set(propertyClass.getName(), values, xcontext);
                        } else {
                            targetObject.set(propertyClass.getName(), objectPropertyValue, xcontext);
                        }
                    }
                } else {
                    //value for propertyClass was null -> no targetObject update required
                }
            }
        }
    }

    protected Object denormalize(Object objectPropertyValue, PropertyClass propertyClass)
    {

        XWikiContext xcontext = getXWikiContext();
        Map<String, ListItem> mapValues = null;

        if (propertyClass instanceof ListClass) {
            ListClass listClass = (ListClass) propertyClass;
            //TODO: what happens if the DBList field contains millions of entries? Do we load all of them in memory?
            mapValues = listClass.getMap(xcontext);
        } else {
            //for safety purpose
            mapValues = new HashMap<String, ListItem>();
        }

        if ((objectPropertyValue instanceof List) && (((List) objectPropertyValue).size() > 0)) {
            //we have a multi-valued property
            List<Object> denormalizedValues = new ArrayList<>();
            Iterator itNormalizedValues = ((List) objectPropertyValue).iterator();
            while (itNormalizedValues.hasNext()) {
                Object normalizedValue = itNormalizedValues.next();
                ListItem denormalizedValue = mapValues.get(normalizedValue);
                if (denormalizedValue != null) {
                    denormalizedValues.add(denormalizedValue.getValue());
                } else {
                    // if the normalized value is not found in the map of values, copy it as is
                    // without denormalization
                    denormalizedValues.add(normalizedValue);
                }
            }
            return denormalizedValues;
        } else {
            //we have a single-valued property
            ListItem denormalizedValue = mapValues.get(objectPropertyValue);
            if (denormalizedValue != null) {
                return denormalizedValue.getValue();
            } else {
                return objectPropertyValue;
            }
        }
    }

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }
}
