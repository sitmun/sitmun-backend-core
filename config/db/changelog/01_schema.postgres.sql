--liquibase formatted sql
--changeset sitmun:1 dbms:postgresql

-- ===================================================================
-- Application
-- ===================================================================

CREATE TABLE STM_APP
(
  APP_ID                      INT4         NOT NULL,
  APP_NAME                    VARCHAR(50)  NOT NULL,
  APP_TYPE                    VARCHAR(50),
  APP_TITLE                   VARCHAR(250),
  APP_THEME                   VARCHAR(30),
  APP_LOGO                    VARCHAR(4000),
  APP_DESCRIPTION             VARCHAR(4000),
  APP_SCALES                  VARCHAR(250),
  APP_PROJECT                 VARCHAR(50),
  APP_TEMPLATE                VARCHAR(250),
  APP_REFRESH                 BOOLEAN,
  APP_ENTRYS                  BOOLEAN,
  APP_ENTRYM                  BOOLEAN,
  APP_GGIID                   INT4,
  APP_CREATED                 TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  APP_MAINTENANCE_INFORMATION VARCHAR(4000),
  APP_UNAVAILABLE             BOOLEAN               DEFAULT FALSE NOT NULL,
  APP_LAST_UPDATE             TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  APP_CREATORID               INTEGER,
  APP_PRIVATE                 BOOLEAN               DEFAULT FALSE NOT NULL,
  APP_HEADERPARAMS            TEXT,
  PRIMARY KEY (APP_ID)
);

-- ===================================================================
-- Application Background
-- ===================================================================

CREATE TABLE STM_APP_BCKG
(
  ABC_ID     INT4 NOT NULL,
  ABC_APPID  INT4 NOT NULL,
  ABC_BACKID INT4 NOT NULL,
  ABC_ORDER  INT4,
  PRIMARY KEY (ABC_ID)
);

ALTER TABLE STM_APP_BCKG
  ADD CONSTRAINT STM_APF_UK UNIQUE (ABC_APPID, ABC_BACKID);

-- ===================================================================
-- Application Territory
-- ===================================================================

CREATE TABLE STM_APP_TER
(
  ATE_ID     INT4 NOT NULL,
  ATE_APPID  INT4 NOT NULL,
  ATE_TERID  INT4 NOT NULL,
  ATE_INIEXT VARCHAR(250),
  PRIMARY KEY (ATE_ID)
);

ALTER TABLE STM_APP_TER
  ADD CONSTRAINT STM_APT_UK UNIQUE (ATE_APPID, ATE_TERID);

-- ===================================================================
-- Application Role
-- ===================================================================

CREATE TABLE STM_APP_ROL
(
  ARO_ROLEID INT4 NOT NULL,
  ARO_APPID  INT4 NOT NULL,
  PRIMARY KEY (ARO_APPID, ARO_ROLEID)
);

-- ===================================================================
-- Application Tree
-- ===================================================================

CREATE TABLE STM_APP_TREE
(
  ATR_TREEID INT4 NOT NULL,
  ATR_APPID  INT4 NOT NULL,
  PRIMARY KEY (ATR_APPID, ATR_TREEID)
);

-- ===================================================================
-- Available Geographic Information
-- ===================================================================

CREATE TABLE STM_AVAIL_GI
(
  AGI_ID      INT4 NOT NULL,
  AGI_CREATED TIMESTAMP(6),
  AGI_OWNER   VARCHAR(50),
  AGI_GIID    INT4 NOT NULL,
  AGI_TERID   INT4 NOT NULL,
  PRIMARY KEY (AGI_ID)
);

ALTER TABLE STM_AVAIL_GI
  ADD CONSTRAINT STM_AGI_UK UNIQUE (AGI_TERID, AGI_GIID);

-- ===================================================================
-- Available Task
-- ===================================================================

CREATE TABLE STM_AVAIL_TSK
(
  ATS_ID      INT4 NOT NULL,
  ATS_CREATED TIMESTAMP(6),
  ATS_TASKID  INT4 NOT NULL,
  ATS_TERID   INT4 NOT NULL,
  PRIMARY KEY (ATS_ID)
);

ALTER TABLE STM_AVAIL_TSK
  ADD CONSTRAINT STM_ATS_UK UNIQUE (ATS_TERID, ATS_TASKID);

-- ===================================================================
-- Background
-- ===================================================================

CREATE TABLE STM_BACKGRD
(
  BAC_ID      INT4 NOT NULL,
  BAC_NAME    VARCHAR(50) NOT NULL,
  BAC_IMAGE   VARCHAR(4000),
  BAC_DESC    VARCHAR(250),
  BAC_ACTIVE  BOOLEAN,
  BAC_GGIID   INT4,
  BAC_CREATED TIMESTAMP(6),
  PRIMARY KEY (BAC_ID)
);

-- ===================================================================
-- Code List
-- ===================================================================

CREATE TABLE STM_CODELIST
(
  COD_ID          INT4 NOT NULL,
  COD_LIST        VARCHAR(250) NOT NULL,
  COD_VALUE       VARCHAR(250) NOT NULL,
  COD_SYSTEM      BOOLEAN NOT NULL,
  COD_DEFAULT     BOOLEAN NOT NULL,
  COD_DESCRIPTION VARCHAR(250) NOT NULL,
  PRIMARY KEY (COD_ID)
);

ALTER TABLE STM_CODELIST
  ADD CONSTRAINT STM_COD_UK UNIQUE (COD_LIST, COD_VALUE);

-- ===================================================================
-- Comment
-- ===================================================================

CREATE TABLE STM_COMMENT
(
  COM_ID      INT4 NOT NULL,
  COM_COORD_X FLOAT8 NOT NULL,
  COM_COORD_Y FLOAT8 NOT NULL,
  COM_NAME    VARCHAR(250),
  COM_EMAIL   VARCHAR(250),
  COM_TITLE   VARCHAR(500),
  COM_DESC    VARCHAR(1000),
  COM_CREATED TIMESTAMP(6),
  COM_APPID   INT4 NOT NULL,
  COM_USERID  INT4 NOT NULL,
  PRIMARY KEY (COM_ID)
);

-- ===================================================================
-- Configuration
-- ===================================================================

CREATE TABLE STM_CONF
(
  CNF_ID    INT4 NOT NULL,
  CNF_NAME  VARCHAR(50) NOT NULL ,
  CNF_VALUE VARCHAR(250),
  PRIMARY KEY (CNF_ID)
);

ALTER TABLE STM_CONF
  ADD CONSTRAINT STM_CONF_NAME_UK UNIQUE (CNF_NAME);

-- ===================================================================
-- Connection
-- ===================================================================

CREATE TABLE STM_CONNECT
(
  CON_ID         INT4 NOT NULL,
  CON_NAME       VARCHAR(80) NOT NULL,
  CON_DRIVER     VARCHAR(50) NOT NULL,
  CON_USER       VARCHAR(50),
  CON_PWD        VARCHAR(50),
  CON_CONNECTION VARCHAR(250),
  PRIMARY KEY (CON_ID)
);

-- ===================================================================
-- Filter Geographic Information
-- ===================================================================

CREATE TABLE STM_FIL_GI
(
  FGI_ID        INT4 NOT NULL,
  FGI_COLUMN    VARCHAR(250),
  FGI_NAME      VARCHAR(80) NOT NULL,
  FGI_REQUIRED  BOOLEAN NOT NULL,
  FGI_TYPE      VARCHAR(50) NOT NULL,
  FGI_TYPID     INT4,
  FGI_VALUE     VARCHAR(4000),
  FGI_VALUETYPE VARCHAR(30),
  FGI_GIID      INT4 NOT NULL,
  PRIMARY KEY (FGI_ID)
);

-- ===================================================================
-- Geographic Information
-- ===================================================================

CREATE TABLE STM_GEOINFO
(
  GEO_ID         INT4    NOT NULL,
  GEO_NAME       VARCHAR(100) NOT NULL,
  GEO_ABSTRACT   VARCHAR(4000),
  GEO_LAYERS     VARCHAR(800) NOT NULL,
  GEO_MINSCALE   INT4,
  GEO_MAXSCALE   INT4,
  GEO_ORDER      INT4,
  GEO_TRANSP     INT4,
  GEO_FILTER_GM  BOOLEAN,
  GEO_QUERYABL   BOOLEAN NOT NULL,
  GEO_QUERYACT   BOOLEAN NOT NULL,
  GEO_QUERYLAY   VARCHAR(500),
  GEO_FILTER_GFI BOOLEAN,
  GEO_TYPE       VARCHAR(30),
  GEO_SERID      INT4,
  GEO_SELECTABL  BOOLEAN,
  GEO_SELECTLAY  VARCHAR(500),
  GEO_FILTER_SS  BOOLEAN,
  GEO_SERSELID   INT4,
  GEO_LEGENDTIP  VARCHAR(50),
  GEO_LEGENDURL  VARCHAR(4000),
  GEO_CREATED    TIMESTAMP(6),
  GEO_CONNID     INT4,
  GEO_METAURL    VARCHAR(4000),
  GEO_DATAURL    VARCHAR(4000),
  GEO_THEMATIC   BOOLEAN,
  GEO_GEOMTYPE   VARCHAR(50),
  GEO_SOURCE     VARCHAR(50),
  GEO_STYID      INT4,
  GEO_STYUSEALL  BOOLEAN NOT NULL DEFAULT FALSE,
  GEO_BLOCKED    BOOLEAN NOT NULL,
  PRIMARY KEY (GEO_ID)
);

-- ===================================================================
-- Geographic Information Group
-- ===================================================================

CREATE TABLE STM_GGI_GI
(
  GGG_GGIID INT4 NOT NULL,
  GGG_GIID  INT4 NOT NULL,
  PRIMARY KEY (GGG_GGIID, GGG_GIID)
);

-- ===================================================================
-- Geographic Information Group
-- ===================================================================

CREATE TABLE STM_GRP_GI
(
  GGI_ID   INT4 NOT NULL,
  GGI_NAME VARCHAR(80) NOT NULL,
  GGI_TYPE VARCHAR(30),
  PRIMARY KEY (GGI_ID)
);

-- ===================================================================
-- Geographic Information Group Territory
-- ===================================================================

CREATE TABLE STM_GRP_TER
(
  GTE_TERID  INT4 NOT NULL,
  GTE_TERMID INT4 NOT NULL,
  PRIMARY KEY (GTE_TERMID, GTE_TERID)
);

-- ===================================================================
-- Geographic Information Group Task
-- ===================================================================

CREATE TABLE STM_GRP_TSK
(
  GTS_ID   INT4 NOT NULL,
  GTS_NAME VARCHAR(80) NOT NULL,
  PRIMARY KEY (GTS_ID)
);

-- ===================================================================
-- Geographic Information Group Type
-- ===================================================================

CREATE TABLE STM_GTER_TYP
(
  GTT_ID   INT4 NOT NULL,
  GTT_NAME VARCHAR(250) NOT NULL,
  PRIMARY KEY (GTT_ID)
);

ALTER TABLE STM_GTER_TYP
  ADD CONSTRAINT STM_GTT_NAME_UK UNIQUE (GTT_NAME);

-- ===================================================================
-- Language
-- ===================================================================

CREATE TABLE STM_LANGUAGE
(
  LAN_ID        INT4 NOT NULL,
  LAN_NAME      VARCHAR(50) NOT NULL,
  LAN_SHORTNAME VARCHAR(20) NOT NULL,
  PRIMARY KEY (LAN_ID)
);

ALTER TABLE STM_LANGUAGE
  ADD CONSTRAINT STM_LAN_UK UNIQUE (LAN_SHORTNAME);

-- ===================================================================
-- Log
-- ===================================================================

CREATE TABLE STM_LOG
(
  LOG_ID     INT4 NOT NULL,
  LOG_DATE   TIMESTAMP(6),
  LOG_TYPE   VARCHAR(50),
  LOG_USERID INT4,
  LOG_APPID  INT4,
  LOG_TERID  INT4,
  LOG_TASKID INT4,
  LOG_COUNT  INT4,
  LOG_TER    VARCHAR(250),
  LOG_TEREXT VARCHAR(250),
  LOG_DATA   VARCHAR(250),
  LOG_SRS    VARCHAR(250),
  LOG_FORMAT VARCHAR(250),
  LOG_BUFFER BOOLEAN,
  LOG_EMAIL  VARCHAR(250),
  LOG_OTHER  VARCHAR(4000),
  LOG_GIID   INT4,
  PRIMARY KEY (LOG_ID)
);

-- ===================================================================
-- Parameter Application
-- ===================================================================

CREATE TABLE STM_PAR_APP
(
  PAP_ID    INT4 NOT NULL,
  PAP_NAME  VARCHAR(50) NOT NULL,
  PAP_VALUE VARCHAR(250) NOT NULL,
  PAP_TYPE  VARCHAR(250) NOT NULL,
  PAP_APPID INT4,
  PRIMARY KEY (PAP_ID)
);

-- ===================================================================
-- Parameter Geographic Information
-- ===================================================================

CREATE TABLE STM_PAR_GI
(
  PGI_ID     INT4 NOT NULL,
  PGI_NAME   VARCHAR(250) NOT NULL,
  PGI_VALUE  VARCHAR(250) NOT NULL,
  PGI_FORMAT VARCHAR(250),
  PGI_TYPE   VARCHAR(250) NOT NULL,
  PGI_GIID   INT4 NOT NULL,
  PGI_ORDER  INT4,
  PRIMARY KEY (PGI_ID)
);

-- ===================================================================
-- Parameter Geographic Information (Spatial query)
-- ===================================================================

CREATE TABLE STM_PAR_SGI
(
  PSG_ID     INT4 NOT NULL,
  PSG_NAME   VARCHAR(250) NOT NULL,
  PSG_VALUE  VARCHAR(250) NOT NULL,
  PSG_FORMAT VARCHAR(250),
  PSG_TYPE   VARCHAR(250) NOT NULL,
  PSG_GIID   INT4 NOT NULL,
  PSG_ORDER  INT4,
  PRIMARY KEY (PSG_ID)
);

-- ===================================================================
-- Parameter Service
-- ===================================================================

CREATE TABLE STM_PAR_SER
(
  PSE_ID    INT4 NOT NULL,
  PSE_SERID INT4 NOT NULL,
  PSE_VALUE VARCHAR(250) NOT NULL,
  PSE_NAME  VARCHAR(30) NOT NULL,
  PSE_TYPE  VARCHAR(250),
  PRIMARY KEY (PSE_ID)
);

-- ===================================================================
-- User Position
-- ===================================================================

CREATE TABLE STM_POST
(
  POS_ID         INT4 NOT NULL,
  POS_CREATED    TIMESTAMP(6),
  POS_UPDATED    TIMESTAMP(6),
  POS_EMAIL      VARCHAR(250),
  POS_EXPIRATION TIMESTAMP(6),
  POS_POST       VARCHAR(250),
  POS_ORG        VARCHAR(250),
  POS_TYPE       VARCHAR(50),
  POS_TERID      INT4 NOT NULL,
  POS_USERID     INT4 NOT NULL,
  PRIMARY KEY (POS_ID)
);

-- ===================================================================
-- Relationship Role Geographic Information
-- ===================================================================

CREATE TABLE STM_ROL_GGI
(
  RGG_ROLEID INT4 NOT NULL,
  RGG_GGIID  INT4 NOT NULL,
  PRIMARY KEY (RGG_GGIID, RGG_ROLEID)
);

-- ===================================================================
-- Relationship Role Task
-- ===================================================================

CREATE TABLE STM_ROL_TSK
(
  RTS_TASKID INT4 NOT NULL,
  RTS_ROLEID INT4 NOT NULL,
  PRIMARY KEY (RTS_ROLEID, RTS_TASKID)
);

-- ===================================================================
-- Role
-- ===================================================================

CREATE TABLE STM_ROLE
(
  ROL_ID   INT4 NOT NULL,
  ROL_NAME VARCHAR(250) NOT NULL,
  ROL_NOTE VARCHAR(500),
  PRIMARY KEY (ROL_ID)
);

ALTER TABLE STM_ROLE
  ADD CONSTRAINT STM_ROL_NAME_UK UNIQUE (ROL_NAME);

-- ===================================================================
-- Sequence
-- ===================================================================

CREATE TABLE STM_SEQUENCE
(
  SEQ_NAME  VARCHAR(255) NOT NULL,
  SEQ_COUNT INT8,
  PRIMARY KEY (SEQ_NAME)
);

-- ===================================================================
-- Service
-- ===================================================================

CREATE TABLE STM_SERVICE
(
  SER_ID       INT4                  NOT NULL,
  SER_NAME     VARCHAR(60) NOT NULL,
  SER_ABSTRACT VARCHAR(4000),
  SER_URL      VARCHAR(4000) NOT NULL,
  SER_PROJECTS VARCHAR(1000),
  SER_LEGEND   VARCHAR(4000),
  SER_INFOURL  VARCHAR(4000),
  SER_CREATED  TIMESTAMP(6),
  SER_PROTOCOL VARCHAR(30) NOT NULL,
  SER_NAT_PROT VARCHAR(50),
  SER_BLOCKED  BOOLEAN NOT NULL,
  SER_PROXIED  BOOLEAN DEFAULT FALSE NOT NULL,
  SER_AUTH_MOD VARCHAR(50),
  SER_USER     VARCHAR(50),
  SER_PWD      VARCHAR(50),
  PRIMARY KEY (SER_ID)
);

-- ===================================================================
-- Style Geographic Information
-- ===================================================================

CREATE TABLE STM_STY_GI
(
  SGI_ID          INT4    NOT NULL,
  SGI_NAME        VARCHAR(80) NOT NULL,
  SGI_TITLE       VARCHAR(250),
  SGI_ABSTRACT    VARCHAR(250),
  SGI_LURL_WIDTH  INT4,
  SGI_LURL_HEIGHT INT4,
  SGI_LURL_FORMAT VARCHAR(80),
  SGI_LURL_URL    VARCHAR(4000),
  SGI_DEFAULT     BOOLEAN NOT NULL DEFAULT FALSE,
  SGI_GIID        INT4,
  PRIMARY KEY (SGI_ID)
);

-- ===================================================================
-- Task
-- ===================================================================

CREATE TABLE STM_TASK
(
  TAS_ID      INT4 NOT NULL,
  TAS_NAME    VARCHAR(512) NOT NULL,
  TAS_CREATED TIMESTAMP(6),
  TAS_ORDER   INT4,
  TAS_GIID    INT4,
  TAS_SERID   INT4,
  TAS_GTASKID INT4,
  TAS_TTASKID INT4,
  TAS_TUIID   INT4,
  TAS_CONNID  INT4,
  TAS_PARAMS  TEXT,
  PRIMARY KEY (TAS_ID)
);

-- ===================================================================
-- Task Relationship
-- ===================================================================

CREATE TABLE STM_TASKREL
(
  TAR_ID        INT4 NOT NULL,
  TAR_TYPE      VARCHAR(50) NOT NULL,
  TAR_TASKID    INT4 NOT NULL,
  TAR_TASKRELID INT4 NOT NULL,
  PRIMARY KEY (TAR_ID)
);

-- ===================================================================
-- Territory Type
-- ===================================================================

CREATE TABLE STM_TER_TYP
(
  TET_ID       INT4    NOT NULL,
  TET_NAME     VARCHAR(50) NOT NULL,
  TET_OFFICIAL BOOLEAN NOT NULL,
  TET_TOP      BOOLEAN NOT NULL,
  TET_BOTTOM   BOOLEAN NOT NULL,
  PRIMARY KEY (TET_ID)
);

ALTER TABLE STM_TER_TYP
  ADD CONSTRAINT STM_TET_NAME_UK UNIQUE (TET_NAME);

-- ===================================================================
-- Territory
-- ===================================================================

CREATE TABLE STM_TERRITORY
(
  TER_ID      INT4 NOT NULL,
  TER_CODTER  VARCHAR(50) NOT NULL,
  TER_NAME    VARCHAR(250) NOT NULL,
  TER_ADMNAME VARCHAR(250),
  TER_ADDRESS VARCHAR(250),
  TER_EMAIL   VARCHAR(250),
  TER_SCOPE   VARCHAR(250),
  TER_LOGO    VARCHAR(4000),
  TER_EXTENT  VARCHAR(250),
  TER_CENTER  VARCHAR(250),
  TER_LEGAL   VARCHAR(50),
  TER_ZOOM    INT4,
  TER_BLOCKED BOOLEAN NOT NULL,
  TER_TYPID   INT4,
  TER_NOTE    VARCHAR(250),
  TER_CREATED TIMESTAMP(6),
  TER_GTYPID  INT4,
  TER_PROJECT VARCHAR(250),
  TER_DESCRIPTION VARCHAR(4000),
  PRIMARY KEY (TER_ID)
);

ALTER TABLE STM_TERRITORY
  ADD CONSTRAINT STM_TER_NAME_UK UNIQUE (TER_NAME);

-- ===================================================================
-- Translation
-- ===================================================================

CREATE TABLE STM_TRANSLATION
(
  TRA_ID     INT4 NOT NULL,
  TRA_ELEID  INT4 NOT NULL,
  TRA_COLUMN VARCHAR(100) NOT NULL,
  TRA_LANID  INT4 NOT NULL,
  TRA_NAME   VARCHAR(4000) NOT NULL,
  PRIMARY KEY (TRA_ID)
);

ALTER TABLE STM_TRANSLATION
  ADD CONSTRAINT STM_TRA_UK UNIQUE (TRA_ELEID, TRA_COLUMN, TRA_LANID);

-- ===================================================================
-- Tree
-- ===================================================================

CREATE TABLE STM_TREE
(
  TRE_ID         INT4 NOT NULL,
  TRE_NAME       VARCHAR(100) NOT NULL,
  TRE_ABSTRACT   VARCHAR(250),
  TRE_TYPE       VARCHAR(50),
  TRE_IMAGE      TEXT,
  TRE_IMAGE_NAME VARCHAR(250),
  TRE_USERID     INT4,
  PRIMARY KEY (TRE_ID)
);

-- ===================================================================
-- Tree Node
-- ===================================================================

CREATE TABLE STM_TREE_NOD
(
  TNO_ID         INT4    NOT NULL,
  TNO_PARENTID   INT4,
  TNO_NAME       VARCHAR(80) NOT NULL,
  TNO_ABSTRACT   VARCHAR(250),
  TNO_TOOLTIP    VARCHAR(100),
  TNO_ACTIVE     BOOLEAN,
  TNO_RADIO      BOOLEAN,
  TNO_LOAD_DATA  BOOLEAN NOT NULL DEFAULT FALSE,
  TNO_ORDER      INT4,
  TNO_METAURL    VARCHAR(4000),
  TNO_DATAURL    VARCHAR(4000),
  TNO_FILTER_GM  BOOLEAN,
  TNO_FILTER_GFI BOOLEAN,
  TNO_QUERYACT   BOOLEAN,
  TNO_FILTER_SE  BOOLEAN,
  TNO_STYLE      VARCHAR(50),
  TNO_TREEID     INT4 NOT NULL,
  TNO_GIID       INT4,
  TNO_TYPE       VARCHAR(50),
  TNO_IMAGE      TEXT,
  TNO_IMAGE_NAME VARCHAR(4000),
  TNO_VIEW_MODE  VARCHAR(50),
  TNO_TASKID     INTEGER,
  TNO_FILTERABLE BOOLEAN          DEFAULT FALSE,
  TNO_MAPPING    TEXT,
  PRIMARY KEY (TNO_ID)
);

-- ===================================================================
-- Tree Role
-- ===================================================================

CREATE TABLE STM_TREE_ROL
(
  TRO_TREEID INT4 NOT NULL,
  TRO_ROLEID INT4 NOT NULL,
  PRIMARY KEY (TRO_ROLEID, TRO_TREEID)
);

-- ===================================================================
-- Task Type
-- ===================================================================

CREATE TABLE STM_TSK_TYP
(
  TTY_ID       INT4 NOT NULL,
  TTY_NAME     VARCHAR(50),
  TTY_TITLE    VARCHAR(50),
  TTY_ENABLED  BOOLEAN NOT NULL,
  TTY_PARENTID INT4,
  TTY_ORDER    INT4,
  TTY_SPEC     TEXT,
  PRIMARY KEY (TTY_ID)
);

-- ===================================================================
-- Task UI
-- ===================================================================

CREATE TABLE STM_TSK_UI
(
  TUI_ID      INT4 NOT NULL,
  TUI_NAME    VARCHAR(50) NOT NULL,
  TUI_TOOLTIP VARCHAR(100),
  TUI_ORDER   INT4,
  TUI_TYPE    VARCHAR(30),
  PRIMARY KEY (TUI_ID)
);

-- ===================================================================
-- User
-- ===================================================================

CREATE TABLE STM_USER
(
  USE_ID        INT4 NOT NULL,
  USE_USER      VARCHAR(50),
  USE_PWD       VARCHAR(128),
  USE_NAME      VARCHAR(50),
  USE_SURNAME   VARCHAR(50),
  USE_IDENT     VARCHAR(50),
  USE_IDENTTYPE VARCHAR(50),
  USE_EMAIL     VARCHAR(50),
  USE_ADM       BOOLEAN NOT NULL,
  USE_BLOCKED   BOOLEAN NOT NULL,
  USE_CREATED   TIMESTAMP(6),
  USE_UPDATED   TIMESTAMP(6),
  USE_LAST_PASSWORD_CHANGE TIMESTAMP(6),
  PRIMARY KEY (USE_ID)
);

ALTER TABLE STM_USER
  ADD CONSTRAINT STM_USE_NAME_UK UNIQUE (USE_USER);

-- ===================================================================
-- User Configuration
-- ===================================================================

CREATE TABLE STM_USR_CONF
(
  UCO_ID      INT4 NOT NULL,
  UCO_USERID  INT4 NOT NULL,
  UCO_TERID   INT4 NOT NULL,
  UCO_ROLEID  INT4 NOT NULL,
  UCO_ROLEM   BOOLEAN NOT NULL,
  UCO_CREATED TIMESTAMP(6),
  PRIMARY KEY (UCO_ID)
);

ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT STM_UCO_UK UNIQUE (UCO_USERID, UCO_TERID, UCO_ROLEID, UCO_ROLEM);

-- ===================================================================
-- User Token
-- ===================================================================

CREATE TABLE STM_TOKEN_USER
(
  USER_TOKEN_ID INTEGER NOT NULL,
  USER_ID       INTEGER NOT NULL,
  CODE_OTP      VARCHAR(150) NOT NULL,
  EXPIRE_AT     TIMESTAMP(6),
  ATTEMPT_COUNTER INTEGER NOT NULL,
  ACTIVE BOOLEAN NOT NULL,
  PRIMARY KEY (USER_TOKEN_ID)
);

ALTER TABLE STM_TOKEN_USER
  ADD CONSTRAINT STM_TUS_UK UNIQUE (USER_ID);



-- ===================================================================
-- Foreign keys
-- ===================================================================

-- Application

ALTER TABLE STM_APP
  ADD CONSTRAINT STM_APP_FK_GGI FOREIGN KEY (APP_GGIID) REFERENCES STM_GRP_GI;

ALTER TABLE STM_APP
  ADD CONSTRAINT STM_APP_FK_USE FOREIGN KEY (APP_CREATORID) REFERENCES STM_USER;

-- Application Background

ALTER TABLE STM_APP_BCKG
  ADD CONSTRAINT STM_ABC_FK_APP FOREIGN KEY (ABC_APPID) REFERENCES STM_APP ON DELETE CASCADE;

ALTER TABLE STM_APP_BCKG
  ADD CONSTRAINT STM_ABC_FK_FON FOREIGN KEY (ABC_BACKID) REFERENCES STM_BACKGRD ON DELETE CASCADE;

-- Application Territory

ALTER TABLE STM_APP_TER
  ADD CONSTRAINT STM_ATE_FK_APP FOREIGN KEY (ATE_APPID) REFERENCES STM_APP ON DELETE CASCADE;

ALTER TABLE STM_APP_TER
  ADD CONSTRAINT STM_ATE_FK_TER FOREIGN KEY (ATE_TERID) REFERENCES STM_TERRITORY ON DELETE CASCADE;

-- Application Role

ALTER TABLE STM_APP_ROL
  ADD CONSTRAINT STM_ARO_FK_APP FOREIGN KEY (ARO_APPID) REFERENCES STM_APP ON DELETE CASCADE;

ALTER TABLE STM_APP_ROL
  ADD CONSTRAINT STM_ARO_FK_ROL FOREIGN KEY (ARO_ROLEID) REFERENCES STM_ROLE ON DELETE CASCADE ;

-- Application Tree

ALTER TABLE STM_APP_TREE
  ADD CONSTRAINT STM_ATR_FK_APP FOREIGN KEY (ATR_APPID) REFERENCES STM_APP ON DELETE CASCADE ;

ALTER TABLE STM_APP_TREE
  ADD CONSTRAINT STM_ATR_FK_TRE FOREIGN KEY (ATR_TREEID) REFERENCES STM_TREE ON DELETE CASCADE ;

-- Available Geographic Information

ALTER TABLE STM_AVAIL_GI
  ADD CONSTRAINT STM_AGI_FK_GEO FOREIGN KEY (AGI_GIID) REFERENCES STM_GEOINFO ON DELETE CASCADE;

ALTER TABLE STM_AVAIL_GI
  ADD CONSTRAINT STM_AGI_FK_TER FOREIGN KEY (AGI_TERID) REFERENCES STM_TERRITORY ON DELETE CASCADE;

-- Available Task

ALTER TABLE STM_AVAIL_TSK
  ADD CONSTRAINT STM_ATS_FK_TAS FOREIGN KEY (ATS_TASKID) REFERENCES STM_TASK ON DELETE CASCADE;

ALTER TABLE STM_AVAIL_TSK
  ADD CONSTRAINT STM_ATS_FK_TER FOREIGN KEY (ATS_TERID) REFERENCES STM_TERRITORY ON DELETE CASCADE;

-- Background

ALTER TABLE STM_BACKGRD
  ADD CONSTRAINT STM_BAC_FK_GGI FOREIGN KEY (BAC_GGIID) REFERENCES STM_GRP_GI;

-- Comment

ALTER TABLE STM_COMMENT
  ADD CONSTRAINT STM_COM_FK_APP FOREIGN KEY (COM_APPID) REFERENCES STM_APP ON DELETE CASCADE;

ALTER TABLE STM_COMMENT
  ADD CONSTRAINT STM_COM_FK_USE FOREIGN KEY (COM_USERID) REFERENCES STM_USER ON DELETE CASCADE;

-- Filter Geographic Information

ALTER TABLE STM_FIL_GI
  ADD CONSTRAINT STM_FGI_FK_GEO FOREIGN KEY (FGI_GIID) REFERENCES STM_GEOINFO ON DELETE CASCADE;

ALTER TABLE STM_FIL_GI
  ADD CONSTRAINT STM_FGI_FK_TET FOREIGN KEY (FGI_TYPID) REFERENCES STM_TER_TYP;

-- Geographic Information

ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT STM_GEO_FK_SGI FOREIGN KEY (GEO_STYID) REFERENCES STM_STY_GI;

ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT STM_GEO_FK_SER FOREIGN KEY (GEO_SERID) REFERENCES STM_SERVICE;

ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT STM_GEO_FK_CON FOREIGN KEY (GEO_CONNID) REFERENCES STM_CONNECT;

ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT STM_GEO_FK_SERSEL FOREIGN KEY (GEO_SERSELID) REFERENCES STM_SERVICE;

-- Geographic Information Group

ALTER TABLE STM_GGI_GI
  ADD CONSTRAINT STM_GGG_FK_GEO FOREIGN KEY (GGG_GIID) REFERENCES STM_GEOINFO ON DELETE CASCADE;

ALTER TABLE STM_GGI_GI
  ADD CONSTRAINT STM_GGG_FK_GGI FOREIGN KEY (GGG_GGIID) REFERENCES STM_GRP_GI ON DELETE CASCADE;

-- Geographic Information Group Territory

ALTER TABLE STM_GRP_TER
  ADD CONSTRAINT STM_GTE_FK_TERM FOREIGN KEY (GTE_TERMID) REFERENCES STM_TERRITORY ON DELETE CASCADE;

ALTER TABLE STM_GRP_TER
  ADD CONSTRAINT STM_GTE_FK_TER FOREIGN KEY (GTE_TERID) REFERENCES STM_TERRITORY ON DELETE CASCADE;

-- Parameter Application

ALTER TABLE STM_PAR_APP
  ADD CONSTRAINT STM_PAP_FK_APP FOREIGN KEY (PAP_APPID) REFERENCES STM_APP ON DELETE CASCADE;

-- Parameter Geographic Information

ALTER TABLE STM_PAR_GI
  ADD CONSTRAINT STM_PGI_FK_GEO FOREIGN KEY (PGI_GIID) REFERENCES STM_GEOINFO ON DELETE CASCADE;

-- Parameter Geographic Information (Spatial query)

ALTER TABLE STM_PAR_SGI
  ADD CONSTRAINT STM_PSG_FK_GEO FOREIGN KEY (PSG_GIID) REFERENCES STM_GEOINFO ON DELETE CASCADE;

-- Parameter Service

ALTER TABLE STM_PAR_SER
  ADD CONSTRAINT STM_PSE_FK_SER FOREIGN KEY (PSE_SERID) REFERENCES STM_SERVICE ON DELETE CASCADE;

-- User Position

ALTER TABLE STM_POST
  ADD CONSTRAINT STM_POS_FK_TER FOREIGN KEY (POS_TERID) REFERENCES STM_TERRITORY ON DELETE CASCADE;

ALTER TABLE STM_POST
  ADD CONSTRAINT STM_POS_FK_USE FOREIGN KEY (POS_USERID) REFERENCES STM_USER ON DELETE CASCADE;

-- Relationship Role Geographic Information

ALTER TABLE STM_ROL_GGI
  ADD CONSTRAINT STM_RGG_FK_GGI FOREIGN KEY (RGG_GGIID) REFERENCES STM_GRP_GI ON DELETE CASCADE ;

ALTER TABLE STM_ROL_GGI
  ADD CONSTRAINT STM_RGG_FK_ROL FOREIGN KEY (RGG_ROLEID) REFERENCES STM_ROLE ON DELETE CASCADE ;

-- Relationship Role Task

ALTER TABLE STM_ROL_TSK
  ADD CONSTRAINT STM_RTS_FK_ROL FOREIGN KEY (RTS_ROLEID) REFERENCES STM_ROLE ON DELETE CASCADE ;

ALTER TABLE STM_ROL_TSK
  ADD CONSTRAINT STM_RTS_FK_TAS FOREIGN KEY (RTS_TASKID) REFERENCES STM_TASK ON DELETE CASCADE ;

-- Style Geographic Information

ALTER TABLE STM_STY_GI
  ADD CONSTRAINT STM_SGI_FK_GEO FOREIGN KEY (SGI_GIID) REFERENCES STM_GEOINFO;

-- Task

ALTER TABLE STM_TASK
  ADD CONSTRAINT STM_TAS_FK_GEO FOREIGN KEY (TAS_GIID) REFERENCES STM_GEOINFO;

ALTER TABLE STM_TASK
  ADD CONSTRAINT STM_TAS_FK_SER FOREIGN KEY (TAS_SERID) REFERENCES STM_SERVICE;

ALTER TABLE STM_TASK
  ADD CONSTRAINT STM_TAS_FK_GTS FOREIGN KEY (TAS_GTASKID) REFERENCES STM_GRP_TSK;

ALTER TABLE STM_TASK
  ADD CONSTRAINT STM_TAS_FK_TTY FOREIGN KEY (TAS_TTASKID) REFERENCES STM_TSK_TYP;

ALTER TABLE STM_TASK
  ADD CONSTRAINT STM_TAS_FK_TUI FOREIGN KEY (TAS_TUIID) REFERENCES STM_TSK_UI;

ALTER TABLE STM_TASK
  ADD CONSTRAINT STM_TAS_FK_CON FOREIGN KEY (TAS_CONNID) REFERENCES STM_CONNECT;

-- Task Relationship

ALTER TABLE STM_TASKREL
  ADD CONSTRAINT STM_TAR_FK_TAS FOREIGN KEY (TAR_TASKID) REFERENCES STM_TASK ON DELETE CASCADE;

ALTER TABLE STM_TASKREL
  ADD CONSTRAINT STM_TAR_FK_TAS_REL FOREIGN KEY (TAR_TASKRELID) REFERENCES STM_TASK;

-- Territory

ALTER TABLE STM_TERRITORY
  ADD CONSTRAINT STM_TER_FK_GTT FOREIGN KEY (TER_GTYPID) REFERENCES STM_GTER_TYP;

ALTER TABLE STM_TERRITORY
  ADD CONSTRAINT STM_TER_FK_TET FOREIGN KEY (TER_TYPID) REFERENCES STM_TER_TYP;

-- Translation

ALTER TABLE STM_TRANSLATION
  ADD CONSTRAINT STM_TRA_FK_LAN FOREIGN KEY (TRA_LANID) REFERENCES STM_LANGUAGE;

-- Tree

ALTER TABLE STM_TREE
  ADD CONSTRAINT STM_TRE_FK_USE FOREIGN KEY (TRE_USERID) REFERENCES STM_USER;

-- Tree Node

ALTER TABLE STM_TREE_NOD
  ADD CONSTRAINT STM_TNO_FK_GEO FOREIGN KEY (TNO_GIID) REFERENCES STM_GEOINFO;

ALTER TABLE STM_TREE_NOD
  ADD CONSTRAINT STM_TNO_FK_TAS FOREIGN KEY (TNO_TASKID) REFERENCES STM_TASK;

ALTER TABLE STM_TREE_NOD
  ADD CONSTRAINT STM_TNO_FK_TNO FOREIGN KEY (TNO_PARENTID) REFERENCES STM_TREE_NOD;

ALTER TABLE STM_TREE_NOD
  ADD CONSTRAINT STM_TNO_FK_TRE FOREIGN KEY (TNO_TREEID) REFERENCES STM_TREE ON DELETE CASCADE;

-- Tree Role

ALTER TABLE STM_TREE_ROL
  ADD CONSTRAINT STM_TRO_FK_ROL FOREIGN KEY (TRO_ROLEID) REFERENCES STM_ROLE ON DELETE CASCADE ;

ALTER TABLE STM_TREE_ROL
  ADD CONSTRAINT STM_TRO_FK_TRE FOREIGN KEY (TRO_TREEID) REFERENCES STM_TREE ON DELETE CASCADE ;

-- Task Type

ALTER TABLE STM_TSK_TYP
  ADD CONSTRAINT STM_TTY_FK_TTY FOREIGN KEY (TTY_PARENTID) REFERENCES STM_TSK_TYP ON DELETE CASCADE;

-- User Configuration

ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT STM_UCO_FK_ROL FOREIGN KEY (UCO_ROLEID) REFERENCES STM_ROLE ON DELETE CASCADE;

ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT STM_UCO_FK_TER FOREIGN KEY (UCO_TERID) REFERENCES STM_TERRITORY ON DELETE CASCADE;

ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT STM_UCO_FK_USU FOREIGN KEY (UCO_USERID) REFERENCES STM_USER ON DELETE CASCADE
