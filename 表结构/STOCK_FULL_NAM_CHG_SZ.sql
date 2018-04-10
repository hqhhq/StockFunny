#drop table if exists STOCK_FULL_NAM_CHG_SZ;

create table STOCK_FULL_NAM_CHG_SZ
(
dte varchar(8) not null,
chgdte varchar(8) not null,
stockid varchar(6) not null,
abbrname varchar(20),
oldfullname varchar(256),
newfullname varchar(256),
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, chgdte, stockid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
