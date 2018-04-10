--作业初始化信息
insert into JOB_INFO values('STK_SH_001','上交所A股股票列表获取','0','D','cn.idaze.stockfunny.stock.database.corp.ShAStockCorpBreifInfoImporter','execJob','$DTE', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_002','上交所B股股票列表获取','0','D','cn.idaze.stockfunny.stock.database.corp.ShBStockCorpBreifInfoImporter','execJob','$DTE', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_003','生成上交所公司概要信息获取作业','STK_SH_001','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpGenelInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_003', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_003','生成上交所公司概要信息获取作业','STK_SH_002','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpGenelInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_003', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_003','生成上交所公司概要信息获取作业','STK_SH_005','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpGenelInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_003', '20180325','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_003','生成上交所公司概要信息获取作业','STK_SH_006','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpGenelInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_003', '20180326','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_004','生成上交所公司董秘信息获取作业','STK_SH_001','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpSecrInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_004', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_004','生成上交所公司董秘信息获取作业','STK_SH_002','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpSecrInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_004', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_004','生成上交所公司董秘信息获取作业','STK_SH_005','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpSecrInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_004', '20180325','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_004','生成上交所公司董秘信息获取作业','STK_SH_006','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpSecrInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_004', '20180326','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_005','上交所终止上市公司列表获取','0','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpEndInfoImporter','execJob','$DTE', '20180325','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_006','上交所暂停上市公司列表获取','0','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpPauseInfoImporter','execJob','$DTE', '20180325','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_007','生成上交所公司高管信息获取作业','STK_SH_001','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpSeniorInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_007', '20180325','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_007','生成上交所公司高管信息获取作业','STK_SH_002','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpSeniorInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_007', '20180325','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_007','生成上交所公司高管信息获取作业','STK_SH_005','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpSeniorInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_007', '20180325','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SH_007','生成上交所公司高管信息获取作业','STK_SH_006','D','cn.idaze.stockfunny.stock.database.corp.ShStockCorpSeniorInfoJobsGenerator','execJob','$DTE,JOBID=STK_SH_007', '20180325','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_001','深交所股票列表获取','0','D','cn.idaze.stockfunny.stock.database.corp.SzStockCorpBreifInfoImporter','execJob','$DTE', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_002','深交所股票全称变更历史','0','D','cn.idaze.stockfunny.stock.database.corp.SzCorpFullNameChgHisImporter','execJob','$DTE', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_003','深交所股票简称变更历史','0','D','cn.idaze.stockfunny.stock.database.corp.SzCorpShortNameChgHisImporter','execJob','$DTE', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_004','深交所可转债列表获取','0','D','cn.idaze.stockfunny.stock.database.corp.SzBondChgablInfoImporter','execJob','$DTE', '20180301','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_005','深交所会员列表获取','0','D','cn.idaze.stockfunny.stock.database.corp.SzMemberInfoImporter','execJob','$DTE', '20180328','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_006','深交所营业部列表获取','0','D','cn.idaze.stockfunny.stock.database.corp.SzSaleDepartInfoImporter','execJob','$DTE', '20180328','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_007','深交所大宗交易_协议交易获取','0','D','cn.idaze.stockfunny.stock.database.corp.SzBlockTradeAgInfoImporter','execJob','$DTE', '20180329','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_008','生成深交所股票交易价格信息获取作业','STK_SZ_009','D','cn.idaze.stockfunny.stock.database.price.SzStockTradePriceJobsGenerator','execJob','$DTE,JOBID=STK_SZ_008', '20180409','99991231','N',sysdate(6),sysdate(6),'');
insert into JOB_INFO values('STK_SZ_009','深交所股票交易摘要信息获取','0','D','cn.idaze.stockfunny.stock.database.price.SzStockTradeAbsImporter','execJob','$DTE', '20180409','99991231','N',sysdate(6),sysdate(6),'');


