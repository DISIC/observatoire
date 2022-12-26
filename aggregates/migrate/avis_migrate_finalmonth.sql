delete from xwoids where month=202213;
insert ignore into xwoids select 202213, xwo_id from xwikidoc, xwikiobjects where xwd_web='Avis' and xwd_creation_date>'2022-12-25' and xwo_name=xwd_fullname and xwo_classname='Avis.Code.AvisClass';

