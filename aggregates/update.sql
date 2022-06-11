delete from avis_byday where day >= cast(date_add(now(), interval -1 day) as date);
insert ignore into avis_byday 
select cast(xwd_creation_date as date) as day, xws_value as demarche, count(*) as count, 
sum(CASE WHEN score.xwi_value=3 THEN 1 ELSE 0 END) as avis3, 
sum(CASE WHEN score.xwi_value=2 THEN 1 ELSE 0 END) as avis2, 
sum(CASE WHEN score.xwi_value=1 THEN 1 ELSE 0 END) as avis1, 
sum(CASE WHEN facile.xwi_value=3 THEN 1 ELSE 0 END) as facile3, 
sum(CASE WHEN facile.xwi_value=2 THEN 1 ELSE 0 END) as facile2, 
sum(CASE WHEN facile.xwi_value=1 THEN 1 ELSE 0 END) as facile1, 
sum(CASE WHEN compr.xwi_value=3 THEN 1 ELSE 0 END) as compr3, 
sum(CASE WHEN compr.xwi_value=2 THEN 1 ELSE 0 END) as compr2, 
sum(CASE WHEN compr.xwi_value=1 THEN 1 ELSE 0 END) as compr1, 
null, null, null, null, null, null, null, 
null, null, null, null, null, null, 
null, null, null, 
null 
from xwikidoc use index (doc_creation_date), xwikistrings demarche, 
xwikiobjects avis left outer join xwikiintegers score on (xwo_id=score.xwi_id and score.xwi_name='score' )
left outer join xwikiintegers facile on (xwo_id=facile.xwi_id and facile.xwi_name='facile')
left outer join xwikiintegers compr on (xwo_id=compr.xwi_id and compr.xwi_name='comprehensible')
where xwd_fullname=xwo_name and xwo_classname='Avis.Code.AvisClass' 
and xwo_id=xws_id and xws_name='demarche' 
and xwd_creation_date > date_add(now(), interval -2 day) 
group by 1,2;
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
and xwd_creation_date > date_add(now(), interval -2 day) 
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
and xwd_creation_date > date_add(now(), interval -2 day) 
group by 1,2) data ON agg.day = data.day AND agg.demarche = data.demarche
SET agg.aide_proche = data.aide_proche,
agg.aide_association = data.aide_association,
agg.aide_agent = data.aide_agent,
agg.aide_internet = data.aide_internet,
agg.aide_autre = data.aide_autre,
agg.aide_aucune = data.aide_aucune;
