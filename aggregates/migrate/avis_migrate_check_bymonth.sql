SELECT
  count(*)
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
LEFT OUTER JOIN avis ON
 (
  avis.XWO_ID = xwikiobjects.XWO_ID
 )
LEFT OUTER JOIN xwikidoc AS doc ON
  ( xwikiobjects.XWO_NAME = doc.XWD_FULLNAME)
WHERE
  xwoids.XWO_ID = xwikiobjects.XWO_ID
  AND (sessionId.XWS_VALUE != avis.avis_session_id OR
  apiKey.XWS_VALUE != avis.avis_api_key OR
  dem.XWS_VALUE != avis.avis_demarche OR
  modalite.XWS_VALUE != avis.avis_modalite OR
  score.XWI_VALUE != avis.avis_score OR
  vote.XWS_VALUE != avis.avis_vote OR
  voteInput.XWL_VALUE != avis.avis_vote_input OR
  facile.XWI_VALUE != avis.avis_facile OR
  comprehensible.XWI_VALUE != avis.avis_comprehensible OR
  autreDifficulte.XWS_VALUE != avis.avis_autre_difficulte OR
  autreAide.XWS_VALUE != avis.avis_autre_aide OR
  autre.XWL_VALUE != avis.avis_autre OR
  email.XWS_VALUE != avis.avis_email OR
  src.XWS_VALUE != avis.avis_source OR
  doc.xwd_creation_date != avis.avis_date OR
  doc.xwd_name != avis.avis_id)
  AND xwoids.month=@MONTH;

