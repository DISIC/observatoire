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
package com.xwiki.projects.dinsic.wikidemarches.aggregates;

import java.io.Serializable;
import java.util.Date;

/**
 * Class describing an aggregate entry in the database.
 * 
 * @version $Id$
 */
public class AvisAggregateByDay implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String demarche;

    private Date day;

    private int nbAvis;

    private int avis3;

    private int avis2;

    private int avis1;

    private int facile3;

    private int facile2;

    private int facile1;

    private int comprehension3;

    private int comprehension2;

    private int comprehension1;

    private int diffManquedinformations;

    private int diffDysfonctionnement;

    private int diffMobile;

    private int diffPiecesjointes;

    private int diffSuite;

    private int diffAutre;

    private int diffAucune;

    private int aideProche;

    private int aideAssociation;

    private int aideAgent;

    private int aideInternet;

    private int aideAutre;

    private int aideAucune;

    private int modaliteEnligneentierement;

    private int modaliteEnlignepartielement;

    private int modaliteAutrement;

    private int commentaire;

    public String getDemarche()
    {
        return demarche;
    }

    public void setDemarche(String demarche)
    {
        this.demarche = demarche;
    }

    public Date getDay()
    {
        return day;
    }

    public void setDay(Date day)
    {
        this.day = day;
    }

    public int getNbAvis()
    {
        return nbAvis;
    }

    public void setNbAvis(int nbAvis)
    {
        this.nbAvis = nbAvis;
    }

    public int getAvis3()
    {
        return avis3;
    }

    public void setAvis3(int avis3)
    {
        this.avis3 = avis3;
    }

    public int getAvis2()
    {
        return avis2;
    }

    public void setAvis2(int avis2)
    {
        this.avis2 = avis2;
    }

    public int getAvis1()
    {
        return avis1;
    }

    public void setAvis1(int avis1)
    {
        this.avis1 = avis1;
    }

    public int getFacile3()
    {
        return facile3;
    }

    public void setFacile3(int facile3)
    {
        this.facile3 = facile3;
    }

    public int getFacile2()
    {
        return facile2;
    }

    public void setFacile2(int facile2)
    {
        this.facile2 = facile2;
    }

    public int getFacile1()
    {
        return facile1;
    }

    public void setFacile1(int facile1)
    {
        this.facile1 = facile1;
    }

    public int getComprehension3()
    {
        return comprehension3;
    }

    public void setComprehension3(int comprehension3)
    {
        this.comprehension3 = comprehension3;
    }

    public int getComprehension2()
    {
        return comprehension2;
    }

    public void setComprehension2(int comprehension2)
    {
        this.comprehension2 = comprehension2;
    }

    public int getComprehension1()
    {
        return comprehension1;
    }

    public void setComprehension1(int comprehension1)
    {
        this.comprehension1 = comprehension1;
    }

    public int getDiffManquedinformations()
    {
        return diffManquedinformations;
    }

    public void setDiffManquedinformations(int diffManquedinformations)
    {
        this.diffManquedinformations = diffManquedinformations;
    }

    public int getDiffDysfonctionnement()
    {
        return diffDysfonctionnement;
    }

    public void setDiffDysfonctionnement(int diffDysfonctionnement)
    {
        this.diffDysfonctionnement = diffDysfonctionnement;
    }

    public int getDiffMobile()
    {
        return diffMobile;
    }

    public void setDiffMobile(int diffMobile)
    {
        this.diffMobile = diffMobile;
    }

    public int getDiffPiecesjointes()
    {
        return diffPiecesjointes;
    }

    public void setDiffPiecesjointes(int diffPiecesjointes)
    {
        this.diffPiecesjointes = diffPiecesjointes;
    }

    public int getDiffSuite()
    {
        return diffSuite;
    }

    public void setDiffSuite(int diffSuite)
    {
        this.diffSuite = diffSuite;
    }

    public int getDiffAutre()
    {
        return diffAutre;
    }

    public void setDiffAutre(int diffAutre)
    {
        this.diffAutre = diffAutre;
    }

    public int getDiffAucune()
    {
        return diffAucune;
    }

    public void setDiffAucune(int diffAucune)
    {
        this.diffAucune = diffAucune;
    }

    public int getAideProche()
    {
        return aideProche;
    }

    public void setAideProche(int aideProche)
    {
        this.aideProche = aideProche;
    }

    public int getAideAssociation()
    {
        return aideAssociation;
    }

    public void setAideAssociation(int aideAssociation)
    {
        this.aideAssociation = aideAssociation;
    }

    public int getAideAgent()
    {
        return aideAgent;
    }

    public void setAideAgent(int aideAgent)
    {
        this.aideAgent = aideAgent;
    }

    public int getAideInternet()
    {
        return aideInternet;
    }

    public void setAideInternet(int aideInternet)
    {
        this.aideInternet = aideInternet;
    }

    public int getAideAutre()
    {
        return aideAutre;
    }

    public void setAideAutre(int aideAutre)
    {
        this.aideAutre = aideAutre;
    }

    public int getAideAucune()
    {
        return aideAucune;
    }

    public void setAideAucune(int aideAucune)
    {
        this.aideAucune = aideAucune;
    }

    public int getModaliteEnligneentierement()
    {
        return modaliteEnligneentierement;
    }

    public void setModaliteEnligneentierement(int modaliteEnligneentierement)
    {
        this.modaliteEnligneentierement = modaliteEnligneentierement;
    }

    public int getModaliteEnlignepartielement()
    {
        return modaliteEnlignepartielement;
    }

    public void setModaliteEnlignepartielement(int modaliteEnlignepartielement)
    {
        this.modaliteEnlignepartielement = modaliteEnlignepartielement;
    }

    public int getModaliteAutrement()
    {
        return modaliteAutrement;
    }

    public void setModaliteAutrement(int modaliteAutrement)
    {
        this.modaliteAutrement = modaliteAutrement;
    }

    public int getCommentaire()
    {
        return commentaire;
    }

    public void setCommentaire(int commentaire)
    {
        this.commentaire = commentaire;
    }

}
