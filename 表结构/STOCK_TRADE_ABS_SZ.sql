#drop table if exists STOCK_TRADE_ABS_SZ;

create table STOCK_TRADE_ABS_SZ
(
dte varchar(8) not null,
trdte varchar(8) not null,
stockid varchar(6) not null,
abbrname varchar(20),
preclose decimal(30,6),
close decimal(30,6),
risepercent decimal(30,6),
totalmoney decimal(30,6),
pe decimal(30,6),
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, trdte, stockid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

