#drop table if exists CORP_PAUSE_LIST_SH;

create table CORP_PAUSE_LIST_SH
(
dte varchar(8) not null,
corpid varchar(6) not null,
corpabbrname varchar(20),	
ipodate varchar(8),
pausedate varchar(8),	
datasource varchar(10) not null,
lastupdatetime datetime(6),	
remarks varchar(256),
primary key(dte, corpid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
