#drop table if exists BOND_CHGABL_SZ;

create table BOND_CHGABL_SZ
(
dte varchar(8) not null,
bondid varchar(6) not null,
abbrname varchar(20),
ipodate varchar(8),
issuenum decimal(30,6),
chgprice decimal(30,6),
unchgnum decimal(30,6),
unchgper decimal(30,6),
begdate varchar(8),
enddate varchar(8),
stockid varchar(6),
validdate varchar(8),
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, bondid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
