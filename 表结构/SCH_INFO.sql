#drop table if exists SCH_INFO;

create table SCH_INFO
(
dte varchar(8) not null,
jobid varchar(20) not null,
jobname varchar(64),
classname varchar(256) not null,
classmethod varchar(128) not null,
methodparas varchar(256) not null,
pjobids varchar(256),
status varchar(1) not null,
exectimes int not null,
begtime datetime(6),
endtime datetime(6),
createtime datetime(6),
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, jobid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
