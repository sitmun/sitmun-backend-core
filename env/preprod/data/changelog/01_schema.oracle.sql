--liquibase formatted sql
--changeset sitmun:1 dbms:oracle

create table stm_app
(
  app_id       number(10, 0) not null,
  app_entrym   number(1, 0),
  app_entrys   number(1, 0),
  app_created  timestamp,
  app_template varchar2(250 char),
  app_name     varchar2(50 char),
  app_scales   varchar2(250 char),
  app_project  varchar2(50 char),
  app_theme    varchar2(30 char),
  app_title    varchar2(250 char),
  app_refresh  number(1, 0),
  app_type     varchar2(50 char),
  app_ggiid    number(10, 0),
  primary key (app_id) using index tablespace ${index_tablespace}
);

create table stm_app_bckg
(
  abc_id     number(10, 0) not null,
  abc_order  number(10, 0),
  abc_appid  number(10, 0),
  abc_backid number(10, 0),
  primary key (abc_id) using index tablespace ${index_tablespace}
);

create table stm_app_ter
(
  ate_id     number(10, 0) not null,
  ate_appid  number(10, 0),
  ate_terid  number(10, 0),
  ate_iniext varchar2(250 char),
  primary key (ate_id) using index tablespace ${index_tablespace}
);

create table stm_app_rol
(
  aro_roleid number(10, 0) not null,
  aro_appid  number(10, 0) not null,
  primary key (aro_appid, aro_roleid) using index tablespace ${index_tablespace}
);

create table stm_app_tree
(
  atr_treeid number(10, 0) not null,
  atr_appid  number(10, 0) not null,
  primary key (atr_appid, atr_treeid) using index tablespace ${index_tablespace}
);

create table stm_avail_gi
(
  agi_id      number(10, 0) not null,
  agi_created timestamp,
  agi_owner   varchar2(50 char),
  agi_giid    number(10, 0),
  agi_terid   number(10, 0),
  primary key (agi_id) using index tablespace ${index_tablespace}
);

create table stm_avail_tsk
(
  ats_id      number(10, 0) not null,
  ats_created timestamp,
  ats_taskid  number(10, 0),
  ats_terid   number(10, 0),
  primary key (ats_id) using index tablespace ${index_tablespace}
);

create table stm_backgrd
(
  bac_id      number(10, 0) not null,
  bac_active  number(1, 0),
  bac_created timestamp,
  bac_desc    varchar2(250 char),
  bac_image   varchar2(4000 char),
  bac_name    varchar2(50 char),
  bac_ggiid   number(10, 0),
  primary key (bac_id) using index tablespace ${index_tablespace}
);

create table stm_codelist
(
  cod_id          number(10, 0) not null,
  cod_list        varchar2(50 char),
  cod_description varchar2(250 char),
  cod_system      number(1, 0),
  cod_default     number(1, 0),
  cod_value       varchar2(50 char),
  primary key (cod_id) using index tablespace ${index_tablespace}
);

create table stm_comment
(
  com_id      number(10, 0) not null,
  com_coord_x double precision,
  com_coord_y double precision,
  com_created timestamp,
  com_desc    varchar2(1000 char),
  com_email   varchar2(250 char),
  com_name    varchar2(250 char),
  com_title   varchar2(500 char),
  com_appid   number(10, 0) not null,
  com_userid  number(10, 0) not null,
  primary key (com_id) using index tablespace ${index_tablespace}
);

create table stm_conf
(
  cnf_id    number(10, 0) not null,
  cnf_name  varchar2(50 char),
  cnf_value varchar2(250 char),
  primary key (cnf_id) using index tablespace ${index_tablespace}
);

create table stm_connect
(
  con_id         number(10, 0) not null,
  con_driver     varchar2(50 char),
  con_name       varchar2(50 char),
  con_pwd        varchar2(50 char),
  con_connection varchar2(250 char),
  con_user       varchar2(50 char),
  primary key (con_id) using index tablespace ${index_tablespace}
);

create table stm_fil_gi
(
  fgi_id        number(10, 0) not null,
  fgi_column    varchar2(50 char),
  fgi_name      varchar2(50 char),
  fgi_required  number(1, 0),
  fgi_type      varchar2(50 char),
  fgi_valuetype varchar2(50 char),
  fgi_value     varchar2(4000 char),
  fgi_giid      number(10, 0),
  fgi_typid     number(10, 0),
  primary key (fgi_id) using index tablespace ${index_tablespace}
);

create table stm_geoinfo
(
  geo_id         number(10, 0) not null,
  geo_filter_gfi number(1, 0),
  geo_filter_gm  number(1, 0),
  geo_filter_ss  number(1, 0),
  geo_blocked    number(1, 0),
  geo_created    timestamp,
  geo_dataurl    varchar2(4000 char),
  geo_abstract   varchar2(250 char),
  geo_geomtype   varchar2(50 char),
  geo_layers     varchar2(800 char),
  geo_legendtip  varchar2(50 char),
  geo_legendurl  varchar2(4000 char),
  geo_maxscale   number(10, 0),
  geo_metaurl    varchar2(4000 char),
  geo_minscale   number(10, 0),
  geo_name       varchar2(100 char),
  geo_order      number(10, 0),
  geo_queryabl   number(1, 0),
  geo_queryact   number(1, 0),
  geo_querylay   varchar2(500 char),
  geo_selectabl  number(1, 0),
  geo_selectlay  varchar2(500 char),
  geo_source     varchar2(50 char),
  geo_thematic   number(1, 0),
  geo_transp     number(10, 0),
  geo_type       varchar2(50 char),
  geo_styid      number(10, 0),
  geo_styuseall  number(1, 0) default 0 not null,
  geo_serid      number(10, 0),
  geo_connid     number(10, 0),
  geo_serselid   number(10, 0),
  primary key (geo_id) using index tablespace ${index_tablespace}
);

create table stm_ggi_gi
(
  ggg_ggiid number(10, 0) not null,
  ggg_giid  number(10, 0) not null,
  primary key (ggg_giid, ggg_ggiid) using index tablespace ${index_tablespace}
);

create table stm_grp_gi
(
  ggi_id   number(10, 0) not null,
  ggi_name varchar2(50 char),
  ggi_type varchar2(50 char),
  primary key (ggi_id) using index tablespace ${index_tablespace}
);

create table stm_grp_ter
(
  gte_terid  number(10, 0) not null,
  gte_termid number(10, 0) not null,
  primary key (gte_termid, gte_terid) using index tablespace ${index_tablespace}
);

create table stm_grp_tsk
(
  gts_id   number(10, 0) not null,
  gts_name varchar2(50 char),
  primary key (gts_id) using index tablespace ${index_tablespace}
);

create table stm_gter_typ
(
  gtt_id   number(10, 0) not null,
  gtt_name varchar2(250 char),
  primary key (gtt_id) using index tablespace ${index_tablespace}
);

create table stm_language
(
  lan_id        number(10, 0) not null,
  lan_name      varchar2(50 char),
  lan_shortname varchar2(20 char),
  primary key (lan_id) using index tablespace ${index_tablespace}
);

create table stm_log
(
  log_id     number(10, 0) not null,
  log_buffer number(1, 0),
  log_count  number(10, 0),
  log_data   varchar2(250 char),
  log_date   timestamp,
  log_email  varchar2(250 char),
  log_format varchar2(50 char),
  log_other  varchar2(4000 char),
  log_srs    varchar2(50 char),
  log_terext varchar2(250 char),
  log_ter    varchar2(50 char),
  log_type   varchar2(50 char),
  log_appid  number(10, 0),
  log_giid   number(10, 0),
  log_taskid number(10, 0),
  log_terid  number(10, 0),
  log_userid number(10, 0),
  primary key (log_id) using index tablespace ${index_tablespace}
);

create table stm_par_app
(
  pap_id    number(10, 0) not null,
  pap_name  varchar2(50 char),
  pap_type  varchar2(50 char),
  pap_value varchar2(250 char),
  pap_appid number(10, 0),
  primary key (pap_id) using index tablespace ${index_tablespace}
);

create table stm_par_gi
(
  pgi_id     number(10, 0) not null,
  pgi_format varchar2(50 char),
  pgi_name   varchar2(50 char),
  pgi_order  number(10, 0),
  pgi_type   varchar2(50 char),
  pgi_value  varchar2(250 char),
  pgi_giid   number(10, 0),
  primary key (pgi_id) using index tablespace ${index_tablespace}
);

create table stm_par_sgi
(
  psg_id     number(10, 0) not null,
  psg_format varchar2(50 char),
  psg_name   varchar2(50 char),
  psg_order  number(10, 0),
  psg_type   varchar2(50 char),
  psg_value  varchar2(250 char),
  psg_giid   number(10, 0),
  primary key (psg_id) using index tablespace ${index_tablespace}
);

create table stm_par_ser
(
  pse_id    number(10, 0) not null,
  pse_name  varchar2(50 char),
  pse_type  varchar2(50 char),
  pse_value varchar2(250 char),
  pse_serid number(10, 0),
  primary key (pse_id) using index tablespace ${index_tablespace}
);

create table stm_post
(
  pos_id         number(10, 0) not null,
  pos_created    timestamp,
  pos_updated    timestamp,
  pos_email      varchar2(250 char),
  pos_expiration timestamp,
  pos_post       varchar2(250 char),
  pos_org        varchar2(250 char),
  pos_type       varchar2(50 char),
  pos_terid      number(10, 0),
  pos_userid     number(10, 0),
  primary key (pos_id) using index tablespace ${index_tablespace}
);

create table stm_query
(
  que_id      number(10, 0) not null,
  que_command varchar2(250 char),
  que_desc    varchar2(250 char),
  que_type    varchar2(50 char),
  que_taskid  number(10, 0),
  primary key (que_id) using index tablespace ${index_tablespace}
);

create table stm_rol_ggi
(
  rgg_roleid number(10, 0) not null,
  rgg_ggiid  number(10, 0) not null,
  primary key (rgg_ggiid, rgg_roleid) using index tablespace ${index_tablespace}
);

create table stm_rol_tsk
(
  rts_taskid number(10, 0) not null,
  rts_roleid number(10, 0) not null,
  primary key (rts_roleid, rts_taskid) using index tablespace ${index_tablespace}
);

create table stm_role
(
  rol_id   number(10, 0) not null,
  rol_note varchar2(500 char),
  rol_name varchar2(50 char),
  primary key (rol_id) using index tablespace ${index_tablespace}
);

create table stm_sequence
(
  SEQ_NAME  varchar2(255 char) not null,
  SEQ_COUNT number(19, 0),
  primary key (SEQ_NAME) using index tablespace ${index_tablespace}
);

create table stm_service
(
  ser_id       number(10, 0) not null,
  ser_blocked  number(1, 0),
  ser_created  timestamp,
  ser_abstract varchar2(250 char),
  ser_infourl  varchar2(4000 char),
  ser_legend   varchar2(4000 char),
  ser_name     varchar2(60 char),
  ser_nat_prot varchar2(50 char),
  ser_url      varchar2(4000 char),
  ser_projects varchar2(1000 char),
  ser_protocol varchar2(50 char),
  ser_auth_mod varchar2(50 char),
  ser_user     varchar2(50 char),
  ser_pwd      varchar2(50 char),
  primary key (ser_id) using index tablespace ${index_tablespace}
);

create table stm_sty_gi
(
  sgi_id          number(10, 0) not null,
  sgi_abstract    varchar2(250 char),
  sgi_lurl_format varchar2(255 char),
  sgi_lurl_height number(10, 0),
  sgi_lurl_url    varchar2(255 char),
  sgi_lurl_width  number(10, 0),
  sgi_name        varchar2(50 char),
  sgi_title       varchar2(50 char),
  sgi_giid        number(10, 0),
  sgi_default     number(1, 0) default 0 not null,
  primary key (sgi_id) using index tablespace ${index_tablespace}
);

create table stm_task
(
  tas_id      number(10, 0) not null,
  tas_name    varchar2(512 char),
  tas_created timestamp,
  tas_order   number(10, 0),
  tas_giid    number(10, 0),
  tas_serid   number(10, 0),
  tas_gtaskid number(10, 0),
  tas_ttaskid number(10, 0),
  tas_tuiid   number(10, 0),
  tas_connid  number(10, 0),
  tas_params  clob,
  primary key (tas_id) using index tablespace ${index_tablespace}
);

create table stm_taskrel
(
  tar_id        number(10, 0) not null,
  tar_type      varchar2(50 char),
  tar_taskid    number(10, 0),
  tar_taskrelid number(10, 0),
  primary key (tar_id) using index tablespace ${index_tablespace}
);

create table stm_ter_typ
(
  tet_id       number(10, 0) not null,
  tet_name     varchar2(50 char),
  tet_official number(1, 0)  not null,
  tet_top      number(1, 0)  not null,
  tet_bottom   number(1, 0)  not null,
  primary key (tet_id) using index tablespace ${index_tablespace}
);

create table stm_territory
(
  ter_id      number(10, 0) not null,
  ter_blocked number(1, 0),
  ter_codter  varchar2(50 char),
  ter_created timestamp,
  ter_extent  varchar2(250 char),
  ter_center  varchar2(250 char),
  ter_legal   varchar2(50 char),
  ter_zoom    number(10, 0),
  ter_name    varchar2(250 char),
  ter_note    varchar2(250 char),
  ter_scope   varchar2(50 char),
  ter_address varchar2(250 char),
  ter_email   varchar2(50 char),
  ter_logo    varchar2(4000 char),
  ter_admname varchar2(250 char),
  ter_gtypid  number(10, 0),
  ter_typid   number(10, 0),
  primary key (ter_id) using index tablespace ${index_tablespace}
);

create table stm_translation
(
  tra_id     number(10, 0) not null,
  tra_column varchar2(50 char),
  tra_eleid  number(10, 0),
  tra_name   varchar2(250 char),
  tra_lanid  number(10, 0),
  primary key (tra_id) using index tablespace ${index_tablespace}
);

create table stm_tree
(
  tre_id       number(10, 0) not null,
  tre_abstract varchar2(250 char),
  tre_image      CLOB,
  tre_image_name varchar2(4000),
  tre_name     varchar2(50 char),
  tre_userid   number(10, 0),
  tre_type     varchar2(50, char),
  primary key (tre_id) using index tablespace ${index_tablespace}
);

create table stm_tree_nod
(
  tno_id         number(10, 0) not null,
  tno_active     number(1, 0),
  tno_dataurl    varchar2(4000 char),
  tno_abstract   varchar2(250 char),
  tno_filter_gfi number(1, 0),
  tno_filter_gm  number(1, 0),
  tno_filter_se  number(1, 0),
  tno_metaurl    varchar2(4000 char),
  tno_name       varchar2(80 char),
  tno_order      number(10, 0),
  tno_queryact   number(1, 0),
  tno_radio      number(1, 0),
  tno_tooltip    varchar2(100 char),
  tno_giid       number(10, 0),
  tno_parentid   number(10, 0),
  tno_treeid     number(10, 0),
  tno_style      varchar2(50 char),
  tno_image      CLOB,
  tno_image_name varchar2(4000),
  tno_view_mode  varchar2(50),
  tno_taskid     number(10, 0),
  tno_filterable number(1, 0),
  primary key (tno_id) using index tablespace ${index_tablespace}
);

create table stm_tree_rol
(
  tro_treeid number(10, 0) not null,
  tro_roleid number(10, 0) not null,
  primary key (tro_roleid, tro_treeid) using index tablespace ${index_tablespace}
);

create table stm_tsk_typ
(
  tty_id       number(10, 0) not null,
  tty_enabled  number(1, 0),
  tty_name     varchar2(50 char),
  tty_order    number(10, 0),
  tty_spec     clob,
  tty_title    varchar2(50 char),
  tty_parentid number(10, 0),
  primary key (tty_id) using index tablespace ${index_tablespace}
);

create table stm_tsk_ui
(
  tui_id      number(10, 0) not null,
  tui_name    varchar2(50 char),
  tui_order   number(10, 0),
  tui_tooltip varchar2(100 char),
  tui_type    varchar(30),
  primary key (tui_id) using index tablespace ${index_tablespace}
);

create table stm_user
(
  use_id        number(10, 0) not null,
  use_adm       number(1, 0),
  use_blocked   number(1, 0),
  use_created   timestamp,
  use_updated   timestamp,
  use_name      varchar2(30 char),
  use_generic   number(1, 0),
  use_ident     varchar2(50 char),
  use_identtype varchar2(50 char),
  use_surname   varchar2(40 char),
  use_pwd       varchar2(128 char),
  use_user      varchar2(50 char),
  primary key (use_id) using index tablespace ${index_tablespace}
);

create table stm_usr_conf
(
  uco_id      number(10, 0) not null,
  uco_rolem   number(1, 0),
  uco_created timestamp,
  uco_roleid  number(10, 0),
  uco_terid   number(10, 0),
  uco_userid  number(10, 0),
  primary key (uco_id) using index tablespace ${index_tablespace}
);

alter table stm_app_bckg
  add constraint STM_APF_UK unique (abc_appid, abc_backid)
  using index tablespace ${index_tablespace};

alter table stm_app_ter
  add constraint STM_APT_UK unique (ate_appid, ate_terid)
  using index tablespace ${index_tablespace};

alter table stm_avail_gi
  add constraint STM_DCA_UK unique (agi_terid, agi_giid)
  using index tablespace ${index_tablespace};

alter table stm_avail_tsk
  add constraint STM_DTA_UK unique (ats_terid, ats_taskid)
  using index tablespace ${index_tablespace};

alter table stm_codelist
  add constraint STM_CDL_UK unique (cod_list, cod_value)
  using index tablespace ${index_tablespace};

alter table stm_conf
  add constraint STM_CONF_NAME_UK unique (cnf_name)
  using index tablespace ${index_tablespace};

alter table stm_gter_typ
  add constraint STM_GTT_NOM_UK unique (gtt_name)
  using index tablespace ${index_tablespace};

alter table stm_language
  add constraint STM_LAN_UK unique (lan_shortname)
  using index tablespace ${index_tablespace};

alter table stm_post
  add constraint STM_POST_UK unique (pos_userid, pos_terid)
  using index tablespace ${index_tablespace};

alter table stm_role
  add constraint STM_ROL_NOM_UK unique (rol_name)
  using index tablespace ${index_tablespace};

alter table stm_ter_typ
  add constraint STM_TET_NOM_UK unique (tet_name)
  using index tablespace ${index_tablespace};

alter table stm_territory
  add constraint STM_TER_NOM_UK unique (ter_name)
  using index tablespace ${index_tablespace};

alter table stm_translation
  add constraint STM_TRA_UK unique (tra_eleid, tra_column, tra_lanid)
  using index tablespace ${index_tablespace};

alter table stm_user
  add constraint STM_USU_USU_UK unique (use_user)
  using index tablespace ${index_tablespace};

alter table stm_usr_conf
  add constraint STM_UCF_UK unique (uco_userid, uco_terid, uco_roleid, uco_rolem)
  using index tablespace ${index_tablespace};

alter table stm_app
  add constraint STM_APP_FK_GGI foreign key (app_ggiid) references stm_grp_gi;

alter table stm_app_bckg
  add constraint STM_ABC_FK_APP foreign key (abc_appid) references stm_app
  on delete cascade;

alter table stm_app_bckg
  add constraint STM_ABC_FK_FON foreign key (abc_backid) references stm_backgrd
  on delete cascade;

alter table stm_app_ter
  add constraint STM_ATE_FK_APP foreign key (ate_appid) references stm_app
  on delete cascade;

alter table stm_app_ter
  add constraint STM_ATE_FK_FON foreign key (ate_terid) references stm_territory
  on delete cascade;

alter table stm_app_rol
  add constraint STM_ARO_FK_APP foreign key (aro_appid) references stm_app;

alter table stm_app_rol
  add constraint STM_ARO_FK_ROL foreign key (aro_roleid) references stm_role;

alter table stm_app_tree
  add constraint STM_ATR_FK_APP foreign key (atr_appid) references stm_app;

alter table stm_app_tree
  add constraint STM_ATR_FK_TRE foreign key (atr_treeid) references stm_tree;

alter table stm_avail_gi
  add constraint STM_AGI_FK_GEO foreign key (agi_giid) references stm_geoinfo
  on delete cascade;

alter table stm_avail_gi
  add constraint STM_AGI_FK_TER foreign key (agi_terid) references stm_territory
  on delete cascade;

alter table stm_avail_tsk
  add constraint STM_ATS_FK_TAS foreign key (ats_taskid) references stm_task
  on delete cascade;

alter table stm_avail_tsk
  add constraint STM_ATS_FK_TER foreign key (ats_terid) references stm_territory
  on delete cascade;

alter table stm_backgrd
  add constraint STM_BAC_FK_GGI foreign key (bac_ggiid) references stm_grp_gi;

alter table stm_comment
  add constraint STM_COM_FK_APP foreign key (com_appid) references stm_app
  on delete cascade;

alter table stm_comment
  add constraint STM_COM_FK_USE foreign key (com_userid) references stm_user
  on delete cascade;

alter table stm_fil_gi
  add constraint STM_FGI_FK_GEO foreign key (fgi_giid) references stm_geoinfo
  on delete cascade;

alter table stm_fil_gi
  add constraint STM_FGI_FK_TET foreign key (fgi_typid) references stm_ter_typ;

alter table stm_geoinfo
  add constraint STM_GEO_FK_SGI foreign key (geo_styid) references stm_sty_gi;

alter table stm_geoinfo
  add constraint STM_GEO_FK_SER foreign key (geo_serid) references stm_service;

alter table stm_geoinfo
  add constraint STM_GEO_FK_CON foreign key (geo_connid) references stm_connect;

alter table stm_geoinfo
  add constraint STM_GEO_FK_SERSEL foreign key (geo_serselid) references stm_service;

alter table stm_ggi_gi
  add constraint STM_GGG_FK_GEO foreign key (ggg_giid) references stm_geoinfo;

alter table stm_ggi_gi
  add constraint STM_GGG_FK_GGI foreign key (ggg_ggiid) references stm_grp_gi;

alter table stm_grp_ter
  add constraint STM_GTE_FK_TERM foreign key (gte_termid) references stm_territory;

alter table stm_grp_ter
  add constraint STM_GTE_FK_TER foreign key (gte_terid) references stm_territory;

alter table stm_log
  add constraint STM_LOG_FK_APP foreign key (log_appid) references stm_app;

alter table stm_log
  add constraint STM_LOG_FK_GEO foreign key (log_giid) references stm_geoinfo;

alter table stm_log
  add constraint STM_LOG_FK_TSK foreign key (log_taskid) references stm_task;

alter table stm_log
  add constraint STM_LOG_FK_TER foreign key (log_terid) references stm_territory;

alter table stm_log
  add constraint STM_LOG_FK_USR foreign key (log_userid) references stm_user;

alter table stm_par_app
  add constraint STM_PAP_FK_APP foreign key (pap_appid) references stm_app
  on delete cascade;

alter table stm_par_gi
  add constraint STM_PGI_FK_GEO foreign key (pgi_giid) references stm_geoinfo
  on delete cascade;

alter table stm_par_sgi
  add constraint STM_PSG_FK_GEO foreign key (psg_giid) references stm_geoinfo
  on delete cascade;

alter table stm_par_ser
  add constraint STM_PSE_FK_SER foreign key (pse_serid) references stm_service
  on delete cascade;

alter table stm_post
  add constraint STM_POS_FK_TER foreign key (pos_terid) references stm_territory
  on delete cascade;

alter table stm_post
  add constraint STM_POS_FK_USE foreign key (pos_userid) references stm_user
  on delete cascade;

alter table stm_query
  add constraint STM_QUE_FK_TASM foreign key (que_taskid) references stm_task;

alter table stm_rol_ggi
  add constraint STM_RGG_FK_GGI foreign key (rgg_ggiid) references stm_grp_gi;

alter table stm_rol_ggi
  add constraint STM_RGG_FK_ROL foreign key (rgg_roleid) references stm_role;

alter table stm_rol_tsk
  add constraint STM_RTS_FK_ROL foreign key (rts_roleid) references stm_role;

alter table stm_rol_tsk
  add constraint STM_RTS_FK_TAS foreign key (rts_taskid) references stm_task;

alter table stm_sty_gi
  add constraint STM_SGI_FK_GEO foreign key (sgi_giid) references stm_geoinfo;

alter table stm_task
  add constraint STM_TAS_FK_GEO foreign key (tas_giid) references stm_geoinfo;

alter table stm_task
  add constraint STM_TAS_FK_SER foreign key (tas_serid) references stm_service;

alter table stm_task
  add constraint STM_TAS_FK_GTS foreign key (tas_gtaskid) references stm_grp_tsk;

alter table stm_task
  add constraint STM_TAS_FK_TTY foreign key (tas_ttaskid) references stm_tsk_typ;

alter table stm_task
  add constraint STM_TAS_FK_TUI foreign key (tas_tuiid) references stm_tsk_ui;

alter table stm_task
  add constraint STM_TAS_FK_CON foreign key (tas_connid) references stm_connect;

alter table stm_taskrel
  add constraint STM_TAR_FK_TAS foreign key (tar_taskid) references stm_task (tas_id)
  on delete cascade;

alter table stm_taskrel
  add constraint STM_TAR_FK_TAS_REL foreign key (tar_taskrelid) references stm_task (tas_id);

alter table stm_territory
  add constraint STM_TER_FK_TET foreign key (ter_gtypid) references stm_gter_typ;

alter table stm_territory
  add constraint STM_TER_FK_TGR foreign key (ter_typid) references stm_ter_typ;

alter table stm_translation
  add constraint STM_TRA_FK_LAN foreign key (tra_lanid) references stm_language;

alter table stm_tree
  add constraint STM_TRE_FK_USE foreign key (tre_userid) references stm_user;

alter table stm_tree_nod
  add constraint STM_TNO_FK_GEO foreign key (tno_giid) references stm_geoinfo;

alter table stm_tree_nod
  add constraint STM_TNO_FK_TNO foreign key (tno_parentid) references stm_tree_nod;

alter table stm_tree_nod
  add constraint STM_TNO_FK_TRE foreign key (tno_treeid) references stm_tree
  on delete cascade;

alter table stm_tree_rol
  add constraint STM_TRO_FK_ROL foreign key (tro_roleid) references stm_role;

alter table stm_tree_rol
  add constraint STM_TRO_FK_TRE foreign key (tro_treeid) references stm_tree;

alter table stm_tsk_typ
  add constraint STM_TSK_TYP_TTY foreign key (tty_parentid) references stm_tsk_typ
  on delete cascade;

alter table stm_usr_conf
  add constraint STM_UCF_FK_ROL foreign key (uco_roleid) references stm_role
  on delete cascade;

alter table stm_usr_conf
  add constraint STM_UCF_FK_TER foreign key (uco_terid) references stm_territory
  on delete cascade;

alter table stm_usr_conf
  add constraint STM_UCF_FK_USU foreign key (uco_userid) references stm_user
  on delete cascade;
