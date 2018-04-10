#drop table if exists SALE_DEPART_SZ;

create table SALE_DEPART_SZ
(
dte varchar(8) not null,
departid varchar(6) not null,
departname varchar(256),
postaddr varchar(256),
province varchar(20),
city varchar(20),
phone varchar(20),
zipcode varchar(6),
membername varchar(256),
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, departid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


