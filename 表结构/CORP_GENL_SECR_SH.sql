#drop table if exists CORP_GENL_SECR_SH;

create table CORP_GENL_SECR_SH
(
dte varchar(8) not null,
corpid varchar(6) not null,
secrname varchar(64) not null,
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, corpid, secrname, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
