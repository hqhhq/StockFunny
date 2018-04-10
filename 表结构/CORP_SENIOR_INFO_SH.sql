#drop table if exists CORP_SENIOR_INFO_SH;

create table CORP_SENIOR_INFO_SH
(
dte varchar(8) not null,
corpid varchar(6) not null,
position varchar(64),	
name varchar(64),
starttime varchar(8),
seq int not null,	
datasource varchar(10) not null,
lastupdatetime datetime(6),	
remarks varchar(256),
primary key(dte, corpid, seq, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
