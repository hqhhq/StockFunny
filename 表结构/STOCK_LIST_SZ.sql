#drop table if exists STOCK_LIST_SZ;

create table STOCK_LIST_SZ
(
dte varchar(8) not null,
corpid varchar(6) not null,
corpabbrname varchar(20),
corpfullname varchar(256),
engname varchar(256),
regaddr varchar(256),
stockid_a varchar(6),
stockabbrname_a varchar(20),
ipodate_a varchar(8),
capitalstock_a decimal(30,6),
tradableshares_a decimal(30,6),
stockid_b varchar(6),
stockabbrname_b varchar(20),
ipodate_b varchar(8),
capitalstock_b decimal(30,6),
tradableshares_b decimal(30,6),
area varchar(20),
province varchar(20),
city varchar(20),
industry varchar(32),
website varchar(128),
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, corpid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
