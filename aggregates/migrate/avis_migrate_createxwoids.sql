insert ignore into xwoids select @MONTH, xwo_id from xwikiobjects where xwo_name in (select xwd_fullname from xwikidoc where xwd_web='Avis' and date_format(xwd_creation_date,'%Y%m') = @MONTH) and XWO_CLASSNAME='Avis.Code.AvisClass';