#drop table if exists HELP_SEQ;

create table HELP_SEQ
(
seq int not null,
primary key(seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
