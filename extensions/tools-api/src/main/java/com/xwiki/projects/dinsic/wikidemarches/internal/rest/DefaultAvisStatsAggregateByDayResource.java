package com.xwiki.projects.dinsic.wikidemarches.internal.rest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xwiki.projects.dinsic.wikidemarches.rest.AvisStatsAggregateByDayResource;
import com.xwiki.projects.dinsic.wikidemarches.rest.model.jaxb.DemarcheAvisstats;
import com.xwiki.projects.dinsic.wikidemarches.rest.model.jaxb.DemarchesAvisstats;
import com.xwiki.projects.dinsic.wikidemarches.rest.model.jaxb.ObjectFactory;

/**
 * Default implementation of the avis stats resource.
 * 
 * @version $Id$
 */
@Component
@Named("com.xwiki.projects.dinsic.wikidemarches.internal.rest.DefaultAvisStatsAggregateByDayResource")
@Singleton
public class DefaultAvisStatsAggregateByDayResource extends XWikiResource implements AvisStatsAggregateByDayResource
{
    private static final String QUESTION_SATISFACTION = "satisfaction";

    private static final String QUESTION_EASY = "easy";

    private static final String QUESTION_COMPREHENSIBLE = "comprehensible";

    private static final String DEMARCHES_SPACE_PREFIX = "Demarches.";

    private static Calendar DEFAULT_START_DATE = Calendar.getInstance();

    @Inject
    protected Logger logger;

    @Inject
    protected QueryManager queryManager;

    @Inject
    ContextualAuthorizationManager authorizationManager;

    @Inject
    protected DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public void initialize() throws InitializationException
    {
        logger.debug("Initializing the avis stats ressource");
        super.initialize();
        DEFAULT_START_DATE.set(2018, Calendar.JUNE, 15, 0, 0, 0);
        DEFAULT_START_DATE.set(Calendar.MILLISECOND, 0);
    }

    @Override
    public DemarchesAvisstats getAvisStatsForDemarches(List<String> demarcheIds, Long dateStart, Long dateEnd,
        List<String> questions) throws XWikiRestException
    {
        logger.warn("Access to not implemented method DefaultAvisStatsAggregateByDayResource.getAvisStatsForDemarches");
        throw new XWikiRestException("Not implemented yet", new NotImplementedException());
    }

    @Override
    public DemarcheAvisstats getAvisStats(String demarcheId, Long dateStart, Long dateEnd, List<String> questions)
        throws XWikiRestException
    {
        // check access to avis of demarche before anything
        if (!canReadAvisForDemarche(demarcheId)) {
            logger.warn("An access was attempted to the stats rest service with an unauthorized user "
                + this.xcontextProvider.get().getUserReference() + " to demarche " + demarcheId);
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        // start preparing data
        ObjectFactory objFactory = new ObjectFactory();
        DemarcheAvisstats demarcheStats = objFactory.createDemarcheAvisstats();

        // set dates, for the query but also in the response we'll send back
        Calendar startDateCalendar = Calendar.getInstance();
        if (dateStart != null && dateStart != 0) {
            startDateCalendar.setTimeInMillis(dateStart);
        } else {
            startDateCalendar = DEFAULT_START_DATE;
        }
        // set the start date at the beginnnig of the day
        startDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startDateCalendar.set(Calendar.MINUTE, 0);
        startDateCalendar.set(Calendar.SECOND, 0);
        startDateCalendar.set(Calendar.MILLISECOND, 0);

        Calendar endDateCalendar = Calendar.getInstance();
        if (dateEnd != null && dateEnd != 0) {
            endDateCalendar.setTimeInMillis(dateEnd);
            // set the end date to the end of the day for all dates which are not today, since today is not yet over
            endDateCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endDateCalendar.set(Calendar.MINUTE, 59);
            endDateCalendar.set(Calendar.SECOND, 59);
            endDateCalendar.set(Calendar.MILLISECOND, 999);
        } else {
            // nothing, end date calendar is now, as initialized
        }

        // set the dates in the result
        demarcheStats.withDateStart(startDateCalendar.getTimeInMillis()).withDateEnd(endDateCalendar.getTimeInMillis());

        // query and push data in the response from the query
        try {
            Query demarchesQuery =
                buildStatsQuery(demarcheId, questions, startDateCalendar.getTime(), endDateCalendar.getTime());
            List results = demarchesQuery.execute();
            for (Object result : results) {
                Object[] values = (Object[]) result;
                long totalAvis = (long) values[0];
                demarcheStats.withAnswersTotal(totalAvis);
                int valuesRead = 1;
                if (questions.contains(QUESTION_SATISFACTION)) {
                    long bads = (long) values[valuesRead++];
                    long mediums = (long) values[valuesRead++];
                    long goods = (long) values[valuesRead++];
                    demarcheStats.withSatisfaction(objFactory.createSmileyQuestionAnswerStats().withNegative(bads)
                        .withNeutral(mediums).withPositive(goods));
                }
                if (questions.contains(QUESTION_EASY)) {
                    long bads = (long) values[valuesRead++];
                    long mediums = (long) values[valuesRead++];
                    long goods = (long) values[valuesRead++];
                    demarcheStats.withEasy(objFactory.createSmileyQuestionAnswerStats().withNegative(bads)
                        .withNeutral(mediums).withPositive(goods));
                }
                if (questions.contains(QUESTION_COMPREHENSIBLE)) {
                    long bads = (long) values[valuesRead++];
                    long mediums = (long) values[valuesRead++];
                    long goods = (long) values[valuesRead++];
                    demarcheStats.withComprehensible(objFactory.createSmileyQuestionAnswerStats().withNegative(bads)
                        .withNeutral(mediums).withPositive(goods));
                }
            }
            return demarcheStats;
        } catch (QueryException e) {
            logger.error("Error while fetching aggregate data from the database ", e);
            throw new XWikiRestException("Error while fetching aggregate data from the database", e);
        }
    }

    /**
     * Builds the query to get requested avis data about this demarche.
     * 
     * @param demarcheId the id of the demarche (without "Demarches." particle)
     * @param questions the questions to get the stats for, can be 'satisfaction', 'easy' or 'comprehensible', or any
     *            combination.
     * @param dateStart the start date for the query
     * @param dateEnd the end date for the query
     * @return a built query, that just needs to be executed
     * @throws QueryException if anything goes wrong while building the query.
     */
    protected Query buildStatsQuery(String demarcheId, List<String> questions, Date dateStart, Date dateEnd)
        throws QueryException
    {
        String selectHQL = "sum(nbAvis)";
        if (questions.contains(QUESTION_SATISFACTION)) {
            selectHQL += ", sum(avis1), sum(avis2), sum(avis3)";
        }
        if (questions.contains(QUESTION_EASY)) {
            selectHQL += ", sum(facile1), sum(facile2), sum(facile3)";
        }
        if (questions.contains(QUESTION_COMPREHENSIBLE)) {
            selectHQL += ", sum(comprehension1), sum(comprehension2), sum(comprehension3)";
        }
        String whereHQL = "demarche = :demarcheId";
        if (dateStart != null) {
            whereHQL += " and day >= :dateStart";
        }
        if (dateEnd != null) {
            whereHQL += " and day <= :dateEnd";
        }
        Query query = this.queryManager
            .createQuery("select " + selectHQL + " from AvisAggregateByDay where " + whereHQL, Query.HQL);
        query.bindValue("demarcheId", DEMARCHES_SPACE_PREFIX + demarcheId);
        if (dateStart != null) {
            query.bindValue("dateStart", dateStart);
        }
        if (dateEnd != null) {
            query.bindValue("dateEnd", dateEnd);
        }
        return query;
    }

    /**
     * Implements rights verification on the avis of a demarche, for the purpose of this service.
     * 
     * @param demarcheId the id of the demarche (without the "Demarches." part)
     * @return true if the current user can read the avis, false otherwise
     */
    protected boolean canReadAvisForDemarche(String demarcheId)
    {
        EntityReference demarcheReference = documentReferenceResolver.resolve(DEMARCHES_SPACE_PREFIX + demarcheId);
        // simple implementation for now, check admin right only.
        // TODO: implement proper check by checking porteurs and admin ministeriel
        return authorizationManager.hasAccess(Right.ADMIN, demarcheReference);
    }
}
