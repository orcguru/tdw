drop table t1;
drop table t2;
create table if not exists t1(r string, c string, v string);
create table if not exists t2(s string, c string, v string);

explain 
SELECT s.r, s.c, sum(s.v) 
FROM ( 
  SELECT a.r AS r, a.c AS c, a.v AS v FROM t1 a 
  UNION 
  SELECT b.s AS r, b.c AS c, 0 + b.v AS v FROM t2 b
) s 
GROUP BY s.r, s.c;
