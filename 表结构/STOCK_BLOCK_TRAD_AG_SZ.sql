#drop table if exists STOCK_BLOCK_TRAD_AG_SZ;

create table STOCK_BLOCK_TRAD_AG_SZ
(
dte varchar(8) not null,
seq int not null,
trdte varchar(8),
stockid varchar(6),
abbrname varchar(20),
price decimal(30,6),
volumn decimal(30,6),
amt decimal(30,6),
buydepart varchar(256),
selldepart varchar(256),
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, seq, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;





