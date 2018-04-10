#drop table if exists JOB_INFO;

create table JOB_INFO
(
jobid varchar(20) not null,
jobname varchar(64),
pjobid varchar(20) not null,
freq varchar(3) not null,
classname varchar(256) not null,
classmethod varchar(128) not null,
methodparas varchar(256) not null,
begdate varchar(8) not null,
enddate varchar(8) not null,
status varchar(1) not null,
createtime datetime(6),
lastupdatetime datetime(6),
remarks varchar(256),
primary key(jobid, pjobid, begdate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
