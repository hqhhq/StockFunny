#drop table if exists MEMBER_LIST_SZ;

create table MEMBER_LIST_SZ
(
dte varchar(8) not null,
memberid varchar(6) not null,
membername varchar(256),
province varchar(20),
city varchar(20),
regaddr varchar(256),
regcap decimal(30,6),
website varchar(128),
membertype varchar(20),
datasource varchar(10) not null,
lastupdatetime datetime(6),
remarks varchar(256),
primary key(dte, memberid, datasource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

