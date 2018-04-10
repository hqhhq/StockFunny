#drop table if exists STOCK_LIST_A_SH;

create table STOCK_LIST_A_SH
(
dte varchar(8) not null,
corpid varchar(6) not null,
corpabbrname varchar(20),
stockid varchar(6),
stockabbrname varchar(20),
ipodate varchar(8),
capitalstock decimal(30,6),
tradableshares decimal(30,6),
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, corpid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
