update avis_byday agg JOIN (
select cast(xwd_creation_date as date) as day, xws_value as demarche,
sum(CASE WHEN diff.xwl_value='manque-d-informations' THEN 1 ELSE 0 END) as diff_manquedinformations,
sum(CASE WHEN diff.xwl_value='dysfonctionnement' THEN 1 ELSE 0 END) as diff_dysfonctionnement,
sum(CASE WHEN diff.xwl_value='mobile' THEN 1 ELSE 0 END) as diff_mobile,
sum(CASE WHEN diff.xwl_value='pieces-jointes' THEN 1 ELSE 0 END) as diff_piecesjointes,
sum(CASE WHEN diff.xwl_value='suite' THEN 1 ELSE 0 END) as diff_suite,
sum(CASE WHEN diff.xwl_value='autre' THEN 1 ELSE 0 END) as diff_autre
from xwikidoc use index (doc_creation_date), xwikistrings demarche,
xwikiobjects avis left outer join xwikilistitems diff on (xwo_id=xwl_id and xwl_name='difficultes')
where xwd_fullname=xwo_name and xwo_classname='Avis.Code.AvisClass'
and xwo_id=xws_id and xws_name='demarche'
and date_format(xwd_creation_date,'%Y%m') = @MONTH
group by 1,2) data ON agg.day = data.day AND agg.demarche = data.demarche
SET agg.diff_manquedinformations = data.diff_manquedinformations,
agg.diff_dysfonctionnement = data.diff_dysfonctionnement,
agg.diff_mobile = data.diff_mobile,
agg.diff_piecesjointes = data.diff_piecesjointes,
agg.diff_suite = data.diff_suite,
agg.diff_autre = data.diff_autre;
update avis_byday agg JOIN (
select cast(xwd_creation_date as date) as day, xws_value as demarche,
sum(CASE WHEN diff.xwl_value='proche' THEN 1 ELSE 0 END) as aide_proche,
sum(CASE WHEN diff.xwl_value='association' THEN 1 ELSE 0 END) as aide_association,
sum(CASE WHEN diff.xwl_value='agent' THEN 1 ELSE 0 END) as aide_agent,
sum(CASE WHEN diff.xwl_value='internet' THEN 1 ELSE 0 END) as aide_internet,
sum(CASE WHEN diff.xwl_value='autre' THEN 1 ELSE 0 END) as aide_autre,
sum(CASE WHEN diff.xwl_value='aucune' THEN 1 ELSE 0 END) as aide_aucune
from xwikidoc use index (doc_creation_date), xwikistrings demarche,
xwikiobjects avis left outer join xwikilistitems diff on (xwo_id=xwl_id and xwl_name='aide')
where xwd_fullname=xwo_name and xwo_classname='Avis.Code.AvisClass'
and xwo_id=xws_id and xws_name='demarche'
and date_format(xwd_creation_date,'%Y%m') = @MONTH
group by 1,2) data ON agg.day = data.day AND agg.demarche = data.demarche
SET agg.aide_proche = data.aide_proche,
agg.aide_association = data.aide_association,
agg.aide_agent = data.aide_agent,
agg.aide_internet = data.aide_internet,
agg.aide_autre = data.aide_autre,
agg.aide_aucune = data.aide_aucune;
