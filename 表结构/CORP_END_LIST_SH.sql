#drop table if exists CORP_END_LIST_SH;

create table CORP_END_LIST_SH
(
dte varchar(8) not null,
corpid varchar(6) not null,
corpabbrname varchar(20),	
ipodate varchar(8),
enddate varchar(8),	
transid varchar(6),
mainbroker varchar(128),
vicebroker varchar(128),
datasource varchar(10) not null,
lastupdatetime datetime(6),	
remarks varchar(256),
primary key(dte, corpid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
