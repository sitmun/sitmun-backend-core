--liquibase formatted sql
--changeset sitmun:1 dbms:postgresql

create table stm_app
(
  app_id       int4 not null,
  app_entrym   boolean,
  app_entrys   boolean,
  app_created  timestamp,
  app_template varchar(250),
  app_name     varchar(50),
  app_scales   varchar(250),
  app_project  varchar(50),
  app_theme    varchar(30),
  app_title    varchar(250),
  app_refresh  boolean,
  app_type     varchar(50),
  app_ggiid    int4,
  primary key (app_id)
);

create table stm_app_bckg
(
  abc_id     int4 not null,
  abc_order  int4,
  abc_appid  int4,
  abc_backid int4,
  primary key (abc_id)
);

create table stm_app_ter
(
  ate_id     int4 not null,
  ate_appid  int4,
  ate_terid  int4,
  ate_iniext varchar(250),
  primary key (ate_id)
);

create table stm_app_rol
(
  aro_roleid int4 not null,
  aro_appid  int4 not null,
  primary key (aro_appid, aro_roleid)
);
create table stm_app_tree
(
  atr_treeid int4 not null,
  atr_appid  int4 not null,
  primary key (atr_appid, atr_treeid)
);
create table stm_avail_gi
(
  agi_id      int4 not null,
  agi_created timestamp,
  agi_owner   varchar(50),
  agi_giid    int4,
  agi_terid   int4,
  primary key (agi_id)
);
create table stm_avail_tsk
(
  ats_id      int4 not null,
  ats_created timestamp,
  ats_taskid  int4,
  ats_terid   int4,
  primary key (ats_id)
);
create table stm_backgrd
(
  bac_id      int4 not null,
  bac_active  boolean,
  bac_created timestamp,
  bac_desc    varchar(250),
  bac_image   varchar(4000),
  bac_name    varchar(50),
  bac_ggiid   int4,
  primary key (bac_id)
);
create table stm_codelist
(
  cod_id          int4 not null,
  cod_list        varchar(50),
  cod_description varchar(250),
  cod_system      boolean,
  cod_default     boolean,
  cod_value       varchar(50),
  primary key (cod_id)
);
create table stm_comment
(
  com_id      int4 not null,
  com_coord_x float8,
  com_coord_y float8,
  com_created timestamp,
  com_desc    varchar(1000),
  com_email   varchar(250),
  com_name    varchar(250),
  com_title   varchar(500),
  com_appid   int4 not null,
  com_userid  int4 not null,
  primary key (com_id)
);
create table stm_conf
(
  cnf_id    int4 not null,
  cnf_name  varchar(50),
  cnf_value varchar(250),
  primary key (cnf_id)
);
create table stm_connect
(
  con_id         int4 not null,
  con_driver     varchar(50),
  con_name       varchar(50),
  con_pwd        varchar(50),
  con_connection varchar(250),
  con_user       varchar(50),
  primary key (con_id)
);
create table stm_download
(
  dow_id   int4 not null,
  dow_ext  varchar(50),
  dow_path varchar(4000),
  dow_type varchar(50),
  primary key (dow_id)
);
create table stm_fil_gi
(
  fgi_id        int4 not null,
  fgi_column    varchar(50),
  fgi_name      varchar(50),
  fgi_required  boolean,
  fgi_type      varchar(50),
  fgi_valuetype varchar(50),
  fgi_value     varchar(4000),
  fgi_giid      int4,
  fgi_typid     int4,
  primary key (fgi_id)
);
create table stm_geoinfo
(
  geo_id         int4    not null,
  geo_filter_gfi boolean,
  geo_filter_gm  boolean,
  geo_filter_ss  boolean,
  geo_blocked    boolean,
  geo_created    timestamp,
  geo_dataurl    varchar(4000),
  geo_abstract   varchar(250),
  geo_geomtype   varchar(50),
  geo_layers     varchar(800),
  geo_legendtip  varchar(50),
  geo_legendurl  varchar(4000),
  geo_maxscale   int4,
  geo_metaurl    varchar(4000),
  geo_minscale   int4,
  geo_name       varchar(100),
  geo_order      int4,
  geo_queryabl   boolean,
  geo_queryact   boolean,
  geo_querylay   varchar(500),
  geo_selectabl  boolean,
  geo_selectlay  varchar(500),
  geo_source     varchar(50),
  geo_thematic   boolean,
  geo_transp     int4,
  geo_type       varchar(50),
  geo_styid      int4,
  geo_serid      int4,
  geo_styuseall  boolean not null default false,
  geo_connid     int4,
  geo_serselid   int4,
  primary key (geo_id)
);
create table stm_ggi_gi
(
  ggg_ggiid int4 not null,
  ggg_giid  int4 not null,
  primary key (ggg_giid, ggg_ggiid)
);
create table stm_grp_gi
(
  ggi_id   int4 not null,
  ggi_name varchar(50),
  ggi_type varchar(50),
  primary key (ggi_id)
);
create table stm_grp_ter
(
  gte_terid  int4 not null,
  gte_termid int4 not null,
  primary key (gte_termid, gte_terid)
);
create table stm_grp_tsk
(
  gts_id   int4 not null,
  gts_name varchar(50),
  primary key (gts_id)
);
create table stm_gter_typ
(
  gtt_id   int4 not null,
  gtt_name varchar(250),
  primary key (gtt_id)
);
create table stm_language
(
  lan_id        int4 not null,
  lan_name      varchar(50),
  lan_shortname varchar(20),
  primary key (lan_id)
);
create table stm_log
(
  log_id     int4 not null,
  log_buffer boolean,
  log_count  int4,
  log_data   varchar(250),
  log_date   timestamp,
  log_email  varchar(250),
  log_format varchar(50),
  log_other  varchar(4000),
  log_srs    varchar(50),
  log_terext varchar(250),
  log_ter    varchar(50),
  log_type   varchar(50),
  log_appid  int4,
  log_giid   int4,
  log_taskid int4,
  log_terid  int4,
  log_userid int4,
  primary key (log_id)
);
create table stm_par_app
(
  pap_id    int4 not null,
  pap_name  varchar(50),
  pap_type  varchar(50),
  pap_value varchar(250),
  pap_appid int4,
  primary key (pap_id)
);
create table stm_par_gi
(
  pgi_id     int4 not null,
  pgi_format varchar(50),
  pgi_name   varchar(50),
  pgi_order  int4,
  pgi_type   varchar(50),
  pgi_value  varchar(250),
  pgi_giid   int4,
  primary key (pgi_id)
);
create table stm_par_sgi
(
  psg_id     int4 not null,
  psg_format varchar(50),
  psg_name   varchar(50),
  psg_order  int4,
  psg_type   varchar(50),
  psg_value  varchar(250),
  psg_giid   int4,
  primary key (psg_id)
);
create table stm_par_ser
(
  pse_id    int4 not null,
  pse_name  varchar(50),
  pse_type  varchar(50),
  pse_value varchar(250),
  pse_serid int4,
  primary key (pse_id)
);
create table stm_par_tsk
(
  ptt_id        int4 not null,
  ptt_default   varchar(250),
  ptt_editable  boolean,
  ptt_format    varchar(50),
  ptt_help      varchar(250),
  ptt_maxlen    int4,
  ptt_name      varchar(50),
  ptt_order     int4,
  ptt_valuerel  varchar(512),
  ptt_filterrel varchar(512),
  ptt_required  boolean,
  ptt_select    varchar(1500),
  ptt_selectabl boolean,
  ptt_type      varchar(50),
  ptt_value     varchar(4000),
  ptt_taskid    int4,
  primary key (ptt_id)
);
create table stm_post
(
  pos_id         int4 not null,
  pos_created    timestamp,
  pos_updated    timestamp,
  pos_email      varchar(250),
  pos_expiration timestamp,
  pos_post       varchar(250),
  pos_org        varchar(250),
  pos_type       varchar(50),
  pos_terid      int4,
  pos_userid     int4,
  primary key (pos_id)
);
create table stm_query
(
  que_id      int4 not null,
  que_command varchar(250),
  que_desc    varchar(250),
  que_type    varchar(50),
  que_taskid  int4,
  primary key (que_id)
);
create table stm_rol_ggi
(
  rgg_roleid int4 not null,
  rgg_ggiid  int4 not null,
  primary key (rgg_ggiid, rgg_roleid)
);
create table stm_rol_tsk
(
  rts_taskid int4 not null,
  rts_roleid int4 not null,
  primary key (rts_roleid, rts_taskid)
);
create table stm_role
(
  rol_id   int4 not null,
  rol_note varchar(500),
  rol_name varchar(50),
  primary key (rol_id)
);
create table stm_sequence
(
  SEQ_NAME  varchar(255) not null,
  SEQ_COUNT int8,
  primary key (SEQ_NAME)
);
create table stm_service
(
  ser_id       int4 not null,
  ser_blocked  boolean,
  ser_created  timestamp,
  ser_abstract varchar(250),
  ser_infourl  varchar(4000),
  ser_legend   varchar(4000),
  ser_name     varchar(60),
  ser_nat_prot varchar(50),
  ser_url      varchar(4000),
  ser_projects varchar(1000),
  ser_protocol varchar(50),
  ser_auth_mod varchar(50),
  ser_user     varchar(50),
  ser_pwd      varchar(50),
  primary key (ser_id)
);
create table stm_sty_gi
(
  sgi_id          int4    not null,
  sgi_abstract    varchar(250),
  sgi_lurl_format varchar(255),
  sgi_lurl_height int4,
  sgi_lurl_url    varchar(255),
  sgi_lurl_width  int4,
  sgi_name        varchar(50),
  sgi_title       varchar(50),
  sgi_default     boolean not null default false,
  sgi_giid        int4,
  primary key (sgi_id)
);
create table stm_task
(
  tas_id      int4 not null,
  tas_name    varchar(512),
  tas_created timestamp,
  tas_order   int4,
  tas_giid    int4,
  tas_serid   int4,
  tas_gtaskid int4,
  tas_ttaskid int4,
  tas_tuiid   int4,
  tas_connid  int4,
  tas_params  text,
  primary key (tas_id)
);
create table stm_taskrel
(
  tar_id        int4 not null,
  tar_type      varchar(50),
  tar_taskid    int4,
  tar_taskrelid int4,
  primary key (tar_id)
);
create table stm_ter_typ
(
  tet_id       int4    not null,
  tet_name     varchar(50),
  tet_official boolean not null,
  tet_top      boolean not null,
  tet_bottom   boolean not null,
  primary key (tet_id)
);
create table stm_territory
(
  ter_id      int4 not null,
  ter_blocked boolean,
  ter_codter  varchar(50),
  ter_created timestamp,
  ter_extent  varchar(250),
  ter_center  varchar(250),
  ter_legal   varchar(50),
  ter_zoom    int4,
  ter_name    varchar(250),
  ter_note    varchar(250),
  ter_scope   varchar(50),
  ter_address varchar(250),
  ter_email   varchar(50),
  ter_logo    varchar(4000),
  ter_admname varchar(250),
  ter_gtypid  int4,
  ter_typid   int4,
  primary key (ter_id)
);
create table stm_the_rank
(
  trk_position int4 not null,
  trk_valuenul boolean,
  trk_color    varchar(30),
  trk_size     int4,
  trk_style    varchar(30),
  trk_desc     varchar(250),
  trk_colorint varchar(30),
  trk_styleint varchar(30),
  trk_valuemax numeric(19, 11),
  trk_valuemin numeric(19, 11),
  trk_name     varchar(50),
  trk_value    varchar(30),
  trk_theid    int4 not null,
  primary key (trk_theid, trk_position)
);
create table stm_thematic
(
  the_id           int4 not null,
  the_sizemax      int4,
  the_sizemin      int4,
  the_desc         varchar(250),
  the_destination  varchar(50),
  the_colormax     varchar(250),
  the_expiration   timestamp,
  the_name         varchar(50),
  the_ranknum      int4,
  the_rankrec      boolean,
  the_dataref      boolean,
  the_colormin     varchar(250),
  the_taggable     boolean,
  the_transparency int4,
  the_ranktype     varchar(50),
  the_urlws        varchar(4000),
  the_valuetype    varchar(50),
  the_giid         int4,
  the_taskid       int4,
  the_userid       int4,
  primary key (the_id)
);
create table stm_translation
(
  tra_id     int4 not null,
  tra_column varchar(50),
  tra_eleid  int4,
  tra_name   varchar(250),
  tra_lanid  int4,
  primary key (tra_id)
);
create table stm_tree
(
  tre_id       int4 not null,
  tre_abstract varchar(250),
  tre_image    varchar(4000),
  tre_name     varchar(50),
  tre_userid   int4,
  primary key (tre_id)
);
create table stm_tree_nod
(
  tno_id         int4 not null,
  tno_active     boolean,
  tno_dataurl    varchar(4000),
  tno_abstract   varchar(250),
  tno_filter_gfi boolean,
  tno_filter_gm  boolean,
  tno_filter_se  boolean,
  tno_metaurl    varchar(4000),
  tno_name       varchar(80),
  tno_order      int4,
  tno_queryact   boolean,
  tno_radio      boolean,
  tno_tooltip    varchar(100),
  tno_giid       int4,
  tno_parentid   int4,
  tno_treeid     int4,
  tno_style      varchar(50),
  primary key (tno_id)
);
create table stm_tree_rol
(
  tro_treeid int4 not null,
  tro_roleid int4 not null,
  primary key (tro_roleid, tro_treeid)
);
create table stm_tsk_typ
(
  tty_id       int4 not null,
  tty_enabled  boolean,
  tty_name     varchar(50),
  tty_order    int4,
  tty_spec     text,
  tty_title    varchar(50),
  tty_parentid int4,
  primary key (tty_id)
);
create table stm_tsk_ui
(
  tui_id      int4 not null,
  tui_name    varchar(50),
  tui_order   int4,
  tui_tooltip varchar(100),
  tui_type    varchar(30),
  primary key (tui_id)
);
create table stm_user
(
  use_id        int4 not null,
  use_adm       boolean,
  use_blocked   boolean,
  use_created   timestamp,
  use_updated   timestamp,
  use_name      varchar(30),
  use_generic   boolean,
  use_ident     varchar(50),
  use_identtype varchar(50),
  use_surname   varchar(40),
  use_pwd       varchar(128),
  use_user      varchar(50),
  primary key (use_id)
);
create table stm_usr_conf
(
  uco_id      int4 not null,
  uco_rolem   boolean,
  uco_created timestamp,
  uco_roleid  int4,
  uco_terid   int4,
  uco_userid  int4,
  primary key (uco_id)
);

alter table if exists stm_app_bckg
  add constraint STM_APF_UK unique (abc_appid, abc_backid);

alter table if exists stm_app_ter
  add constraint STM_APT_UK unique (ate_appid, ate_terid);

alter table if exists stm_avail_gi
  add constraint STM_DCA_UK unique (agi_terid, agi_giid);
alter table if exists stm_avail_tsk
  add constraint STM_DTA_UK unique (ats_terid, ats_taskid);
alter table if exists stm_codelist
  add constraint UKh4jn7sfgmudoc896blvfqbv3b unique (cod_list, cod_value);
alter table if exists stm_conf
  add constraint STM_CONF_NAME_UK unique (cnf_name);
alter table if exists stm_gter_typ
  add constraint STM_GTT_NOM_UK unique (gtt_name);
alter table if exists stm_language
  add constraint UKpia9a18thhka1ncr9cnyjl0j8 unique (lan_shortname);
alter table if exists stm_post
  add constraint UKt67t88dokixqn9vehtt1aej1x unique (pos_userid, pos_terid);
alter table if exists stm_role
  add constraint STM_ROL_NOM_UK unique (rol_name);
alter table if exists stm_ter_typ
  add constraint STM_TET_NOM_UK unique (tet_name);
alter table if exists stm_territory
  add constraint STM_TER_NOM_UK unique (ter_name);
alter table if exists stm_translation
  add constraint UK2k84kv6jxbmsdvwew4lj4ro0g unique (tra_eleid, tra_column, tra_lanid);
alter table if exists stm_user
  add constraint STM_USU_USU_UK unique (use_user);
alter table if exists stm_usr_conf
  add constraint STM_UCF_UK unique (uco_userid, uco_terid, uco_roleid, uco_rolem);
alter table if exists stm_app
  add constraint STM_APP_FK_GGI foreign key (app_ggiid) references stm_grp_gi;

alter table if exists stm_app_ter
  add constraint STM_ATE_FK_APP foreign key (ate_appid) references stm_app on delete cascade;

alter table if exists stm_app_ter
  add constraint STM_ATE_FK_TER foreign key (ate_terid) references stm_territory on delete cascade;

alter table if exists stm_app_rol
  add constraint STM_ARO_FK_APP foreign key (aro_appid) references stm_app;
alter table if exists stm_app_rol
  add constraint STM_ARO_FK_ROL foreign key (aro_roleid) references stm_role;
alter table if exists stm_app_tree
  add constraint STM_ATR_FK_APP foreign key (atr_appid) references stm_app;
alter table if exists stm_app_tree
  add constraint STM_ATR_FK_TRE foreign key (atr_treeid) references stm_tree;
alter table if exists stm_avail_gi
  add constraint STM_AGI_FK_GEO foreign key (agi_giid) references stm_geoinfo on delete cascade;
alter table if exists stm_avail_gi
  add constraint STM_AGI_FK_TER foreign key (agi_terid) references stm_territory on delete cascade;
alter table if exists stm_avail_tsk
  add constraint STM_ATS_FK_TAS foreign key (ats_taskid) references stm_task on delete cascade;
alter table if exists stm_avail_tsk
  add constraint STM_ATS_FK_TER foreign key (ats_terid) references stm_territory on delete cascade;
alter table if exists stm_backgrd
  add constraint STM_BAC_FK_GGI foreign key (bac_ggiid) references stm_grp_gi;
alter table if exists stm_comment
  add constraint STM_COM_FK_APP foreign key (com_appid) references stm_app on delete cascade;
alter table if exists stm_comment
  add constraint STM_COM_FK_USE foreign key (com_userid) references stm_user on delete cascade;
alter table if exists stm_fil_gi
  add constraint STM_FGI_FK_GEO foreign key (fgi_giid) references stm_geoinfo on delete cascade;
alter table if exists stm_fil_gi
  add constraint STM_FGI_FK_TET foreign key (fgi_typid) references stm_ter_typ;
alter table if exists stm_geoinfo
  add constraint STM_GEO_FK_SGI foreign key (geo_styid) references stm_sty_gi;
alter table if exists stm_geoinfo
  add constraint STM_GEO_FK_SER foreign key (geo_serid) references stm_service;
alter table if exists stm_geoinfo
  add constraint STM_GEO_FK_CON foreign key (geo_connid) references stm_connect;
alter table if exists stm_geoinfo
  add constraint STM_GEO_FK_SERSEL foreign key (geo_serselid) references stm_service;
alter table if exists stm_ggi_gi
  add constraint STM_GGG_FK_GEO foreign key (ggg_giid) references stm_geoinfo;
alter table if exists stm_ggi_gi
  add constraint STM_GGG_FK_GGI foreign key (ggg_ggiid) references stm_grp_gi;
alter table if exists stm_grp_ter
  add constraint STM_GTE_FK_TERM foreign key (gte_termid) references stm_territory;
alter table if exists stm_grp_ter
  add constraint STM_GTE_FK_TER foreign key (gte_terid) references stm_territory;
alter table if exists stm_log
  add constraint FKkevxkue4e8ua7nqisdf9qtt9u foreign key (log_appid) references stm_app;
alter table if exists stm_log
  add constraint FKd40r7bqkxmosokqr544elcyrt foreign key (log_giid) references stm_geoinfo;
alter table if exists stm_log
  add constraint FKcw6u4jp3wvnkh7ypdl81uyfwc foreign key (log_taskid) references stm_task;
alter table if exists stm_log
  add constraint FKm77t0wvsov0pqy99pfbyedip7 foreign key (log_terid) references stm_territory;
alter table if exists stm_log
  add constraint FK2p47bcwkdbn91h2oxuf5csir8 foreign key (log_userid) references stm_user;
alter table if exists stm_par_app
  add constraint STM_PAP_FK_APP foreign key (pap_appid) references stm_app on delete cascade;
alter table if exists stm_par_gi
  add constraint STM_PGI_FK_GEO foreign key (pgi_giid) references stm_geoinfo on delete cascade;
alter table if exists stm_par_sgi
  add constraint STM_PSG_FK_GEO foreign key (psg_giid) references stm_geoinfo on delete cascade;
alter table if exists stm_par_ser
  add constraint STM_PSE_FK_SER foreign key (pse_serid) references stm_service on delete cascade;
alter table if exists stm_par_tsk
  add constraint STM_PTT_FK_TAS foreign key (ptt_taskid) references stm_task on delete cascade;
alter table if exists stm_post
  add constraint STM_POS_FK_TER foreign key (pos_terid) references stm_territory on delete cascade;
alter table if exists stm_post
  add constraint STM_POS_FK_USE foreign key (pos_userid) references stm_user on delete cascade;
alter table if exists stm_query
  add constraint STM_QUE_FK_TASM foreign key (que_taskid) references stm_task;
alter table if exists stm_rol_ggi
  add constraint STM_RGG_FK_GGI foreign key (rgg_ggiid) references stm_grp_gi;
alter table if exists stm_rol_ggi
  add constraint STM_RGG_FK_ROL foreign key (rgg_roleid) references stm_role;
alter table if exists stm_rol_tsk
  add constraint STM_RTS_FK_ROL foreign key (rts_roleid) references stm_role;
alter table if exists stm_rol_tsk
  add constraint STM_RTS_FK_TAS foreign key (rts_taskid) references stm_task;
alter table if exists stm_sty_gi
  add constraint STM_SGI_FK_GEO foreign key (sgi_giid) references stm_geoinfo;

alter table if exists stm_task
  add constraint STM_TAS_FK_GEO foreign key (tas_giid) references stm_geoinfo;
alter table if exists stm_task
  add constraint STM_TAS_FK_SER foreign key (tas_serid) references stm_service;
alter table if exists stm_task
  add constraint STM_TAS_FK_GTS foreign key (tas_gtaskid) references stm_grp_tsk;
alter table if exists stm_task
  add constraint STM_TAS_FK_TTY foreign key (tas_ttaskid) references stm_tsk_typ;
alter table if exists stm_task
  add constraint STM_TAS_FK_TUI foreign key (tas_tuiid) references stm_tsk_ui;
alter table if exists stm_task
  add constraint STM_TAS_FK_CON foreign key (tas_connid) references stm_connect;

alter table if exists stm_taskrel
  add constraint STM_TAR_FK_TAS foreign key (tar_taskid) references stm_task on
    delete
    cascade;
alter table if exists stm_taskrel
  add constraint STM_TAR_FK_TAS_REL foreign key (tar_taskrelid) references stm_task;

alter table if exists stm_territory
  add constraint STM_TER_FK_TET foreign key (ter_gtypid) references stm_gter_typ;
alter table if exists stm_territory
  add constraint STM_TER_FK_TGR foreign key (ter_typid) references stm_ter_typ;
alter table if exists stm_the_rank
  add constraint STM_TRK_FK_THE foreign key (trk_theid) references stm_thematic on delete cascade;
alter table if exists stm_thematic
  add constraint STM_THE_FK_GEO foreign key (the_giid) references stm_geoinfo;
alter table if exists stm_thematic
  add constraint STM_THE_FK_TAS foreign key (the_taskid) references stm_task on delete cascade;
alter table if exists stm_thematic
  add constraint STM_THE_FK_USE foreign key (the_userid) references stm_user;
alter table if exists stm_translation
  add constraint STM_TRA_FK_LAN foreign key (tra_lanid) references stm_language;
alter table if exists stm_tree
  add constraint STM_TRE_FK_USE foreign key (tre_userid) references stm_user;
alter table if exists stm_tree_nod
  add constraint STM_TNO_FK_GEO foreign key (tno_giid) references stm_geoinfo;
alter table if exists stm_tree_nod
  add constraint STM_TNO_FK_TNO foreign key (tno_parentid) references stm_tree_nod;
alter table if exists stm_tree_nod
  add constraint STM_TNO_FK_TRE foreign key (tno_treeid) references stm_tree on delete cascade;
alter table if exists stm_tree_rol
  add constraint STM_TRO_FK_ROL foreign key (tro_roleid) references stm_role;
alter table if exists stm_tree_rol
  add constraint STM_TRO_FK_TRE foreign key (tro_treeid) references stm_tree;
alter table if exists stm_tsk_typ
  add constraint STM_TSK_TYP_TTY foreign key (tty_parentid) references stm_tsk_typ on delete cascade;
alter table if exists stm_usr_conf
  add constraint STM_UCF_FK_ROL foreign key (uco_roleid) references stm_role on delete cascade;
alter table if exists stm_usr_conf
  add constraint STM_UCF_FK_TER foreign key (uco_terid) references stm_territory on delete cascade;
alter table if exists stm_usr_conf
  add constraint STM_UCF_FK_USU foreign key (uco_userid) references stm_user on delete cascade
