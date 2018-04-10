--存储过程：设置作业为就绪状态R
CREATE DEFINER=`root`@`localhost` PROCEDURE `SET_JOB_READY`(in dateStr varchar(8) )
BEGIN
#1.生成作业与父作业的对应关系
drop table if exists `T_JOB_PJOB`; 
create table `T_JOB_PJOB`
(
dte varchar(8) not null,
jobid varchar(20) not null,
pjobid varchar(20) not null,
primary key(dte, jobid, pjobid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#2.生成调度作业中间表
drop table if exists `T_SCH_INFO`; 
create table `T_SCH_INFO`
(
dte varchar(8) not null,
jobid varchar(20) not null,
pjobids varchar(256) not null,
size int,
primary key(dte, jobid, pjobids)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#3.插入调度作业中间表
insert into T_SCH_INFO
select dte, jobid, concat(pjobids, ',') as pjobids, length(pjobids)-length(replace(pjobids,',',''))+1 as size
from SCH_INFO where dte=dateStr and status='S';

#4.插入作业与父作业的对应关系
insert into T_JOB_PJOB
select b.dte, b.jobid, replace(substring_index(b.pjobids, ',', a.seq), concat(substring_index(b.pjobids, ',', a.seq -1), ','), '') as pjobid
from HELP_SEQ a
inner join
T_SCH_INFO  b 
on a.seq <= b.size;

#5.设置作业状态为就绪R
update SCH_INFO a set status='R'  where dte=dateStr and jobid in
(select aa.jobid from (
select a.dte, a.jobid, count(0) cnt, sum(case when b.status='N' or a.pjobid='0' then 1 else 0 end) successno
from T_JOB_PJOB a left join (select * from SCH_INFO where dte=dateStr) b
on a.pjobid=b.jobid
group by a.dte, a.jobid) aa where aa.cnt=aa.successno);

END
