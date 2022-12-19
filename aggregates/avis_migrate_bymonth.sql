INSERT IGNORE INTO avis 
SELECT
  xwikiobjects.XWO_ID AS xwo_id,
  sessionId.XWS_VALUE AS avis_session_id,
  apiKey.XWS_VALUE AS avis_api_key,
  dem.XWS_VALUE AS avis_demarche,
  modalite.XWS_VALUE AS avis_modalite,
  score.XWI_VALUE AS avis_score,
  vote.XWS_VALUE AS avis_vote,
  voteInput.XWL_VALUE AS avis_vote_input,
  facile.XWI_VALUE AS avis_facile,
  comprehensible.XWI_VALUE AS avis_comprehensible,
  autreDifficulte.XWS_VALUE AS avis_autre_difficulte,
  autreAide.XWS_VALUE AS avis_autre_aide,
  autre.XWL_VALUE AS avis_autre,
  email.XWS_VALUE AS avis_email,
  src.XWS_VALUE AS avis_source,
  doc.XWD_CREATION_DATE AS avis_date,
  doc.XWD_NAME AS avis_id
FROM
  xwoids, xwikiobjects
JOIN xwikistrings AS dem ON
  ( xwikiobjects.XWO_ID = dem.XWS_ID
  AND dem.XWS_NAME = 'demarche')
LEFT OUTER JOIN xwikistrings AS apiKey ON
  ( xwikiobjects.XWO_ID = apiKey.XWS_ID
  AND apiKey.XWS_NAME = 'apiKey')
LEFT OUTER JOIN xwikistrings AS sessionId ON
  ( xwikiobjects.XWO_ID = sessionId.XWS_ID
  AND sessionId.XWS_NAME = 'sessionId')
LEFT OUTER JOIN xwikistrings AS modalite ON
  ( xwikiobjects.XWO_ID = modalite.XWS_ID
  AND modalite.XWS_NAME = 'modalite')
LEFT OUTER JOIN xwikiintegers AS score ON
  ( xwikiobjects.XWO_ID = score.XWI_ID
  AND score.XWI_NAME = 'score')
LEFT OUTER JOIN xwikistrings AS vote ON
  ( xwikiobjects.XWO_ID = vote.XWS_ID
  AND vote.XWS_NAME = 'vote')
LEFT OUTER JOIN xwikistrings AS src ON
  ( xwikiobjects.XWO_ID = src.XWS_ID
  AND src.XWS_NAME = 'source')
LEFT OUTER JOIN xwikilargestrings AS voteInput ON
    ( xwikiobjects.XWO_ID = voteInput.XWL_ID
  AND voteInput.XWL_NAME = 'voteInput')
LEFT OUTER JOIN xwikiintegers AS facile ON
  ( xwikiobjects.XWO_ID = facile.XWI_ID
  AND facile.XWI_NAME = 'facile')
LEFT OUTER JOIN xwikiintegers AS comprehensible ON
  ( xwikiobjects.XWO_ID = comprehensible.XWI_ID
  AND comprehensible.XWI_NAME = 'comprehensible')
LEFT OUTER JOIN xwikistrings AS autreDifficulte ON
  ( xwikiobjects.XWO_ID = autreDifficulte.XWS_ID
  AND autreDifficulte.XWS_NAME = 'autreDifficulte')
LEFT OUTER JOIN xwikistrings AS autreAide ON
  ( xwikiobjects.XWO_ID = autreAide.XWS_ID
  AND autreAide.XWS_NAME = 'autreAide')
LEFT OUTER JOIN xwikilargestrings AS autre ON
    ( xwikiobjects.XWO_ID = autre.XWL_ID
  AND autre.XWL_NAME = 'autre')
LEFT OUTER JOIN xwikistrings AS email ON
  ( xwikiobjects.XWO_ID = email.XWS_ID
  AND email.XWS_NAME = 'email')
LEFT OUTER JOIN xwikidoc AS doc ON
  ( xwikiobjects.XWO_NAME = doc.XWD_FULLNAME)
WHERE
  XWO_CLASSNAME = 'Avis.Code.AvisClass'
  AND xwikiobjects.XWO_ID = xwoids.XWO_ID 
  AND xwoids.month=@MONTH;
INSERT IGNORE INTO avis_difficultes
  SELECT
    difficulte.XWL_ID AS avis_id,
    difficulte.XWL_VALUE AS value,
    difficulte.XWL_NUMBER As idx
  FROM
    xwoids,
    xwikilistitems AS difficulte
  JOIN xwikistrings AS dem ON
    ( difficulte.XWL_ID = dem.XWS_ID
    AND dem.XWS_NAME = 'demarche')
  WHERE
    difficulte.XWL_NAME = 'difficultes'
    AND difficulte.XWL_ID = xwoids.XWO_ID 
    AND xwoids.month=@MONTH;
INSERT IGNORE INTO avis_aide
    SELECT
      aide.XWL_ID AS avis_id,
      aide.XWL_VALUE AS value,
      aide.XWL_NUMBER As idx
    FROM
      xwoids,
      xwikilistitems AS aide
    JOIN xwikistrings AS dem ON
      ( aide.XWL_ID = dem.XWS_ID
      AND dem.XWS_NAME = 'demarche')
    WHERE
      aide.XWL_NAME = 'aide'
      AND aide.XWL_ID = xwoids.XWO_ID 
      AND xwoids.month=@MONTH;
