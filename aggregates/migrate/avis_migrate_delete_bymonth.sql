delete from xwikiproperties where xwp_id in 
  (select xwo_id from xwoids xwoids.month=@MONTH);
delete from xwikistrings where xws_id in 
  (select xwo_id from xwoids xwoids.month=@MONTH);
delete from xwikiintegers where xwi_id in 
  (select xwo_id from xwoids xwoids.month=@MONTH);
delete from xwikilists where xwl_id in
  (select xwo_id from xwoids xwoids.month=@MONTH);
delete from xwikilistitems where xwl_id in
  (select xwo_id from xwoids xwoids.month=@MONTH);

