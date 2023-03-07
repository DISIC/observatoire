package com.xwiki.projects.dinsic.wikidemarches.rest;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

import com.xwiki.projects.dinsic.wikidemarches.rest.model.jaxb.DemarcheAvisstats;
import com.xwiki.projects.dinsic.wikidemarches.rest.model.jaxb.DemarchesAvisstats;

/**
 * Provides access to stats of avis by day, based on the stored aggregated data.
 * 
 * @version $Id$
 */
@Path("/observatoire/avistats/aggregatebyday")
@Unstable
public interface AvisStatsAggregateByDayResource
{
    /**
     * Endpoint for getting the stats by day for a list of demarches.
     * 
     * @param demarcheIds the list of demarches
     * @param dateStart the start date of the period to get stats for, from the query string
     * @param dateEnd the end date of the period to get stats for, from the query string
     * @param questions the questions to get the stats for, can be 'satisfaction', 'easy' or 'comprehensible', or any
     *            combination, from the query string
     * @return the results, as an object, to be serialized to result by the rest framework
     * @throws XWikiRestException in case anything goes wrong or when verifications (such as rights) don't pass
     */
    @GET
    DemarchesAvisstats getAvisStatsForDemarches(@QueryParam("demarchesIds") List<String> demarcheIds,
        @QueryParam("date_start") Long dateStart, @QueryParam("date_end") Long dateEnd,
        @QueryParam("question") List<String> questions) throws XWikiRestException;

    /**
     * Endpoint for getting the stats by day for a single demarche.
     * 
     * @param demarcheId the demarche to get the stats for, from the path
     * @param dateStart the start date of the period to get stats for, from the query string
     * @param dateEnd the end date of the period to get stats for, from the query string
     * @param questions the questions to get the stats for, can be 'satisfaction', 'easy' or 'comprehensible', or any
     *            combination, from the query string
     * @return the results, as an object, to be serialized to result by the rest framework
     * @throws XWikiRestException in case anything goes wrong or when verifications (such as rights) don't pass
     */
    @GET
    @Path("/demarche/{demarcheId}")
    DemarcheAvisstats getAvisStats(@PathParam("demarcheId") String demarcheId, @QueryParam("date_start") Long dateStart,
        @QueryParam("date_end") Long dateEnd,
        @QueryParam("question") @DefaultValue("satisfaction") List<String> questions) throws XWikiRestException;
}
