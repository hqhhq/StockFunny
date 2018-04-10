--存储过程：生成辅助序列
CREATE PROCEDURE `genSeq` ()
BEGIN
    declare i int;
    set i = 1;
    while i < 1000 do
    insert into HELP_SEQ values(i);
    set i = i + 1;
    end while;
    
END
