delete from avis_byday where date_format(day,'%Y%m') = @MONTH;
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
and date_format(xwd_creation_date,'%Y%m') = @MONTH
group by 1,2;
