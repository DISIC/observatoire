delete from avis_byday where day >= cast(date_add(now(), interval -1 day) as date);
insert ignore into avis_byday 
select cast(avis.avis_date as date) as day, avis.avis_demarche as demarche, count(*) as count, 
sum(CASE WHEN avis.avis_score=3 THEN 1 ELSE 0 END) as avis3, 
sum(CASE WHEN avis.avis_score=2 THEN 1 ELSE 0 END) as avis2, 
sum(CASE WHEN avis.avis_score=1 THEN 1 ELSE 0 END) as avis1, 
sum(CASE WHEN avis.avis_facile=3 THEN 1 ELSE 0 END) as facile3, 
sum(CASE WHEN avis.avis_facile=2 THEN 1 ELSE 0 END) as facile2, 
sum(CASE WHEN avis.avis_facile=1 THEN 1 ELSE 0 END) as facile1, 
sum(CASE WHEN avis.avis_comprehensible=3 THEN 1 ELSE 0 END) as compr3, 
sum(CASE WHEN avis.avis_comprehensible=2 THEN 1 ELSE 0 END) as compr2, 
sum(CASE WHEN avis.avis_comprehensible=1 THEN 1 ELSE 0 END) as compr1, 
null, null, null, null, null, null, null, 
null, null, null, null, null, null, 
null, null, null, 
null 
from avis 
where date_format(avis.avis_date,'%Y%m%d') in (date_format(now(), '%Y%m%d'), date_format(date_add(now(), interval -1 day), '%Y%m%d')) 
group by 1,2;
update avis_byday agg JOIN (
select cast(avis.avis_date as date) as day, avis.avis_demarche as demarche,
sum(CASE WHEN diff.value='manque-d-informations' THEN 1 ELSE 0 END) as diff_manquedinformations,
sum(CASE WHEN diff.value='dysfonctionnement' THEN 1 ELSE 0 END) as diff_dysfonctionnement,
sum(CASE WHEN diff.value='mobile' THEN 1 ELSE 0 END) as diff_mobile,
sum(CASE WHEN diff.value='pieces-jointes' THEN 1 ELSE 0 END) as diff_piecesjointes,
sum(CASE WHEN diff.value='suite' THEN 1 ELSE 0 END) as diff_suite,
sum(CASE WHEN diff.value='autre' THEN 1 ELSE 0 END) as diff_autre
from avis
LEFT OUTER JOIN avis_difficultes diff on (avis.xwo_id=diff.avis_id)
where date_format(avis.avis_date,'%Y%m%d') in (date_format(now(), '%Y%m%d'), date_format(date_add(now(), interval -1 day), '%Y%m%d'))  
group by 1,2) data 
ON agg.day = data.day AND agg.demarche = data.demarche
SET agg.diff_manquedinformations = data.diff_manquedinformations,
agg.diff_dysfonctionnement = data.diff_dysfonctionnement,
agg.diff_mobile = data.diff_mobile,
agg.diff_piecesjointes = data.diff_piecesjointes,
agg.diff_suite = data.diff_suite,
agg.diff_autre = data.diff_autre;
update avis_byday agg JOIN (
select cast(avis.avis_date as date) as day, avis.avis_demarche as demarche,
sum(CASE WHEN aide.value='proche' THEN 1 ELSE 0 END) as aide_proche,
sum(CASE WHEN aide.value='association' THEN 1 ELSE 0 END) as aide_association,
sum(CASE WHEN aide.value='agent' THEN 1 ELSE 0 END) as aide_agent,
sum(CASE WHEN aide.value='internet' THEN 1 ELSE 0 END) as aide_internet,
sum(CASE WHEN aide.value='autre' THEN 1 ELSE 0 END) as aide_autre,
sum(CASE WHEN aide.value='aucune' THEN 1 ELSE 0 END) as aide_aucune
from avis
LEFT OUTER JOIN avis_aide aide on (avis.xwo_id=aide.avis_id)
where date_format(avis.avis_date,'%Y%m%d') in (date_format(now(), '%Y%m%d'), date_format(date_add(now(), interval -1 day), '%Y%m%d')) 
group by 1,2) data 
ON agg.day = data.day AND agg.demarche = data.demarche
SET agg.aide_proche = data.aide_proche,
agg.aide_association = data.aide_association,
agg.aide_agent = data.aide_agent,
agg.aide_internet = data.aide_internet,
agg.aide_autre = data.aide_autre,
agg.aide_aucune = data.aide_aucune;
