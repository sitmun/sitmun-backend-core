--------------------------------------------------
-- REPOSITORIO
-- Versión compatible Oracle 11 y 12
--------------------------------------------------

--------------------------------------------------
-- Eliminación de tablas
--------------------------------------------------
PROMPT
Eliminando tablas

drop table STM_USR_CONF CASCADE CONSTRAINTS;
drop table STM_COMMENT CASCADE CONSTRAINTS;
drop table STM_LOG CASCADE CONSTRAINTS;

drop table STM_GGI_GI CASCADE CONSTRAINTS;
drop table STM_ROL_TSK CASCADE CONSTRAINTS;
drop table STM_ROL_GGI CASCADE CONSTRAINTS;
drop table STM_AVAIL_TSK CASCADE CONSTRAINTS;
drop table STM_TREE_ROL CASCADE CONSTRAINTS;
drop table STM_APP_BCKG CASCADE CONSTRAINTS;
drop table STM_APP_TREE CASCADE CONSTRAINTS;
drop table STM_APP_ROL CASCADE CONSTRAINTS;

drop table STM_ROLE CASCADE CONSTRAINTS;

drop table STM_BACKGRD CASCADE CONSTRAINTS;
drop table STM_PAR_APP CASCADE CONSTRAINTS;
drop table STM_APP CASCADE CONSTRAINTS;
drop table STM_GRP_GI CASCADE CONSTRAINTS;
drop table STM_AVAIL_GI CASCADE CONSTRAINTS;

drop table STM_TASK CASCADE CONSTRAINTS;

drop table STM_TREE_NOD CASCADE CONSTRAINTS;
drop table STM_TREE CASCADE CONSTRAINTS;

drop table STM_PAR_GI CASCADE CONSTRAINTS;
drop table STM_GEOINFO CASCADE CONSTRAINTS;

drop table STM_PAR_SER CASCADE CONSTRAINTS;
drop table STM_SERVICE CASCADE CONSTRAINTS;
drop table STM_CONNECT CASCADE CONSTRAINTS;

drop table STM_GRP_TSK CASCADE CONSTRAINTS;

drop table STM_TSK_TYP CASCADE CONSTRAINTS;
drop table STM_TSK_UI CASCADE CONSTRAINTS;

drop table STM_POST CASCADE CONSTRAINTS;

drop table STM_USER CASCADE CONSTRAINTS;
drop table STM_GRP_TER CASCADE CONSTRAINTS;
drop table STM_TERRITORY CASCADE CONSTRAINTS;
drop table STM_TER_TYP CASCADE CONSTRAINTS;
drop table STM_GTER_TYP CASCADE CONSTRAINTS;

drop table STM_FIL_GI CASCADE CONSTRAINTS;
drop table STM_STY_GI CASCADE CONSTRAINTS;

drop table STM_LANGUAGE CASCADE CONSTRAINTS;
drop table STM_TRANSLATION CASCADE CONSTRAINTS;

drop table STM_AUXMIGRATION CASCADE CONSTRAINTS;


--------------------------------------------------
-- Creación de tablas
--------------------------------------------------


--------------------------------------------------
-- Tipos de agrupaciones de entidades territoriales
PROMPT
Creando Table 'STM_GTER_TYP'
--------------------------------------------------
CREATE TABLE STM_GTER_TYP
(
  GTT_ID   NUMBER(11) NOT NULL,
  GTT_NAME VARCHAR2(250) NOT NULL
);
PROMPT
Creando Primary Key on STM_GTER_TYP
ALTER TABLE STM_GTER_TYP
  ADD CONSTRAINT STM_GTT_PK PRIMARY KEY (GTT_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_GTER_TYP
  ADD CONSTRAINT STM_GTT_NAME_UK UNIQUE (GTT_NAME) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Tipos de territoriales
PROMPT
Creando Table 'STM_TER_TYP'
--------------------------------------------------
CREATE TABLE STM_TER_TYP
(
  TET_ID   NUMBER(11) NOT NULL,
  TET_NAME VARCHAR2(250) NOT NULL
);
PROMPT
Creando Primary Key on STM_TER_TYP
ALTER TABLE STM_TER_TYP
  ADD CONSTRAINT STM_TET_PK PRIMARY KEY (TET_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_TER_TYP
  ADD CONSTRAINT STM_TET_NAME_UK UNIQUE (TET_NAME) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Entidades territoriales
PROMPT
Creando Table 'STM_TERRITORY'
--------------------------------------------------
CREATE TABLE STM_TERRITORY
(
  TER_ID      NUMBER(11) NOT NULL,
  TER_CODMUN  VARCHAR2(10) NOT NULL,
  TER_NAME    VARCHAR2(250) NOT NULL,
  TER_ADMNAME VARCHAR2(250),
  TER_ADDRESS VARCHAR2(250),
  TER_EMAIL   VARCHAR2(250),
  TER_SCOPE   VARCHAR2(250), -- TER_SCOPE (M,R,T)
  TER_LOGO    VARCHAR2(250),
  TER_EXTENT  VARCHAR2(250),
  TER_BLOCKED NUMBER(1) CHECK ("TER_BLOCKED" IN (0, 1)) NOT NULL,
  TER_TYPID   NUMBER(11),    -- tipologia del territorio
  TER_NOTE    VARCHAR2(250),
  TER_CREATED TIMESTAMP(6),--NOT NULL
  TER_GTYPID  NUMBER(11)
);
PROMPT
Creando Primary Key on 'STM_TERRITORY'
ALTER TABLE STM_TERRITORY
  ADD CONSTRAINT STM_TER_PK PRIMARY KEY (TER_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_TERRITORY
  ADD CONSTRAINT STM_TER_NAME_UK UNIQUE (TER_NAME) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_GRP_TER'
--------------------------------------------------
CREATE TABLE STM_GRP_TER
(
  GTE_TERID  NUMBER(11) NOT NULL,
  GTE_TERMID NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_GRP_TER'
ALTER TABLE STM_GRP_TER
  ADD CONSTRAINT STM_GTE_PK PRIMARY KEY (GTE_TERID, GTE_TERMID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Roles de acceso al sistema
PROMPT
Creando Table 'STM_ROLE'
--------------------------------------------------
CREATE TABLE STM_ROLE
(
  ROL_ID   NUMBER(11) NOT NULL,
  ROL_NAME VARCHAR2(250) NOT NULL,
  ROL_NOTE VARCHAR2(500)
);
PROMPT
Creando Primary Key on 'STM_ROLE'
ALTER TABLE STM_ROLE
  ADD CONSTRAINT STM_ROL_PK PRIMARY KEY (ROL_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_ROLE
  ADD CONSTRAINT STM_ROL_NAME_UK UNIQUE (ROL_NAME) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Usuarios del sistema SITMUN
PROMPT
Creando Table 'STM_USER'
--------------------------------------------------
CREATE TABLE STM_USER
(
  USE_ID        NUMBER(11) NOT NULL,
  USE_USER      VARCHAR2(30),
  USE_PWD       VARCHAR2(128),
  USE_NAME      VARCHAR2(30),
  USE_SURNAME   VARCHAR2(40),
  USE_IDENT     VARCHAR2(20), -- número de identificación de la persona
  USE_IDENTTYPE VARCHAR2(3),  -- tipo de identificación (DNI, NIE, Passaport, etc.)
  USE_ADM       NUMBER(1) CHECK ("USE_ADM" IN (0, 1)) NOT NULL,
  USE_BLOCKED   NUMBER(1) CHECK ("USE_BLOCKED" IN (0, 1)) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_USER'
ALTER TABLE STM_USER
  ADD CONSTRAINT STM_USE_PK PRIMARY KEY (USE_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_USER
  ADD CONSTRAINT STM_USE_NAME_UK UNIQUE (USE_USER) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Configuración de usuarios
PROMPT
Creando Table 'STM_USR_CONF'
--------------------------------------------------
CREATE TABLE STM_USR_CONF
(
  UCO_ID      NUMBER(11) NOT NULL,
  UCO_USERID  NUMBER(11) NOT NULL,
  UCO_TERID   NUMBER(11) NOT NULL,
  UCO_ROLEID  NUMBER(11) NOT NULL,
  UCO_ROLEMID NUMBER(11) -- si territori es padre y tiene hijos, y los hijos seran seleccionables
);
PROMPT
Creando Primary Key on 'STM_USR_CONF'
ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT STM_UCO_PK PRIMARY KEY (UCO_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT STM_UCO_UK UNIQUE (UCO_USERID, UCO_TERID, UCO_ROLEID, UCO_ROLEMID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_AVAIL_TSK'
--------------------------------------------------
CREATE TABLE STM_AVAIL_TSK
(
  ATS_ID      NUMBER(11) NOT NULL,
  ATS_CREATED TIMESTAMP(6),
  ATS_TERID   NUMBER(11) NOT NULL,
  ATS_TASKID  NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_AVAIL_TSK'
ALTER TABLE STM_AVAIL_TSK
  ADD CONSTRAINT STM_ATS_PK PRIMARY KEY (ATS_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_AVAIL_TSK
  ADD CONSTRAINT STM_ATS_UK UNIQUE (ATS_TERID, ATS_TASKID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_AVAIL_GI'
--------------------------------------------------
CREATE TABLE STM_AVAIL_GI
(
  AGI_ID        NUMBER(11) NOT NULL,
  AGI_CREATED   TIMESTAMP(6),
  AGI_PROPRIETA VARCHAR2(50),
  AGI_TERID     NUMBER(11) NOT NULL,
  AGI_GIID      NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_AVAIL_GI'
ALTER TABLE STM_AVAIL_GI
  ADD CONSTRAINT STM_AGI_PK PRIMARY KEY (AGI_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_AVAIL_GI
  ADD CONSTRAINT STM_AGI_UK UNIQUE (AGI_TERID, AGI_GIID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Tabla de cartografías
PROMPT
Creando Table 'STM_GEOINFO'
--------------------------------------------------
CREATE TABLE STM_GEOINFO
(
  GEO_ID         NUMBER(11) NOT NULL,
  GEO_NAME       VARCHAR2(100) NOT NULL,
  GEO_ABSTRACT   VARCHAR2(250),                                                                     -- Descripción multi-idioma
  GEO_LAYERS     VARCHAR2(800) NOT NULL,                                                            -- LAYERs
  GEO_MINSCALE   NUMBER(11),
  GEO_MAXSCALE   NUMBER(11),
  GEO_ORDER      NUMBER(6),
  GEO_TRANSP     NUMBER(6),                                                                         -- valor 0..100. 0:opaco
  GEO_FILTER_GM  NUMBER(1) NOT NULL CHECK ("GEO_FILTER_GM" IN (0, 1)),                              -- aplicar filtros para GetMap?
  GEO_QUERYABL   NUMBER(1) CHECK ("GEO_QUERYABL" IN (0, 1)),                                        -- capa identificable?
  GEO_QUERYACT   NUMBER(1) CHECK ("GEO_QUERYACT" IN (0, 1)),                                        -- Info Activada? Inicialmente activado. solo aplica si QUERYABLE=1
  GEO_QUERYLAY   VARCHAR2(500),                                                                     -- lista de capas para resolver una petición de info
  GEO_FILTER_GFI NUMBER(1) NOT NULL CHECK ("GEO_FILTER_GFI" IN (0, 1)),                             -- aplicar filtros para FeatureInfo?
  GEO_TYPE       VARCHAR2(30),                                                                      -- BASE o SITUACION o null
  GEO_SERID      NUMBER(11) NOT NULL,-- FK al servicio para la visualización
  GEO_SELECTABL  NUMBER(1) CHECK ("GEO_SELECTABL" IN (0, 1)),                                       -- capa seleccionable?
  GEO_SELECTLAY  VARCHAR2(500),                                                                     -- nombre de la capa para la selección espacial. Solo 1 capa.
  GEO_FILTER_SS  NUMBER(1) NOT NULL CHECK ("GEO_FILTER_SS" IN (0, 1)),                              -- aplicar filtros para selección?
  GEO_SERSELID   NUMBER(11),                                                                        -- FK al servicio para la selección.
  GEO_LEGENDTIP  VARCHAR2(50) CHECK ("GEO_LEGENDTIP" IN ('LINK', 'LEGENDGRAPHIC', 'CAPABILITIES')), -- tipo de leyenda: link estático, getlegendgraphic o getcapabilities
  GEO_LEGENDURL  VARCHAR2(250),                                                                     -- url de la leyenda de esta cartografia.
  GEO_CREATED    TIMESTAMP(6),
  GEO_EDITABLE   NUMBER(1) CHECK ("GEO_EDITABLE" IN (0, 1)),                                        -- capa editable?
  GEO_CONNID     NUMBER(11),                                                                        -- FK a la conexión para la edición
  GEO_METAURL    VARCHAR2(250),                                                                     -- url de metadatos
  GEO_DATAURL    VARCHAR2(4000),                                                                    -- Url zip para descarga
  GEO_THEMATIC   NUMBER(1) CHECK ("GEO_THEMATIC" IN (0, 1)),                                        -- capa tematizable?
  GEO_GEOMTYPE   VARCHAR2(50) CHECK ("GEO_GEOMTYPE" IN ('POINT', 'LINE', 'POLYGON')),               -- tipo de geometria: punto, linea, o poligono
  GEO_SOURCE     VARCHAR2(80),                                                                      -- Nombre agrupación fuente
  GEO_STYID      NUMBER(11)                                                                         -- Estilo por defecto
);
PROMPT
Creando Primary Key on 'STM_GEOINFO'
ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT STM_GEO_PK PRIMARY KEY (GEO_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Filtros para cartografía
PROMPT
Creando Table 'STM_FIL_GI'
--------------------------------------------------
CREATE TABLE STM_FIL_GI
(
  FGI_ID        NUMBER(11) NOT NULL,
  FGI_NAME      VARCHAR2(80) NOT NULL,
  FGI_REQUIRED  NUMBER(1) CHECK ("FGI_REQUIRED" IN (0, 1)) NOT NULL,     -- obligatorio informar
  FGI_TYPE      VARCHAR2(1) CHECK ("FGI_TYPE" IN ('D', 'C')) NOT NULL,   -- tipo D=Defined Definido por la aplicación, C=Custom personalizado por el usuario
  FGI_TYPID     NUMBER(11),                                              -- tipologia del territorio
  FGI_COLUMN    VARCHAR2(250),                                           -- Atributo a filtrar
  FGI_VALUE     VARCHAR2(4000),                                          -- Valor o lista de valores
  FGI_VALUETYPE VARCHAR2(30) CHECK ("FGI_VALUETYPE" IN ('A', 'D', 'N')), -- Tipologia del valor (A=Alfanumérico,D=Fecha,N=Numérico)
  FGI_GIID      NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_FIL_GI'
ALTER TABLE STM_FIL_GI
  ADD CONSTRAINT STM_FGI_PK PRIMARY KEY (FGI_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Estilos para cartografía
PROMPT
Creando Table 'STM_STY_GI'
--------------------------------------------------
CREATE TABLE STM_STY_GI
(
  SGI_ID          NUMBER(11) NOT NULL,
  SGI_NAME        VARCHAR2(80) NOT NULL,
  SGI_TITLE       VARCHAR2(250),
  SGI_ABSTRACT    VARCHAR2(250), -- Descripción
  SGI_LURL_WIDTH  NUMBER(6),     -- LegendURL.width
  SGI_LURL_HEIGHT NUMBER(6),     -- LegendURL.height
  SGI_LURL_FORMAT VARCHAR2(80),  -- LegendURL.Format
  SGI_LURL_URL    VARCHAR2(250), -- LegendURL.OnlineResource
  SGI_GIID        NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_STY_GI'
ALTER TABLE STM_STY_GI
  ADD CONSTRAINT STM_SGI_PK PRIMARY KEY (SGI_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_PAR_GI'
--------------------------------------------------
CREATE TABLE STM_PAR_GI
(
  PGI_ID     NUMBER(11) NOT NULL,
  PGI_NAME   VARCHAR2(250) NOT NULL,
  PGI_VALUE  VARCHAR2(250) NOT NULL,
  PGI_FORMAT VARCHAR2(250),
  PGI_TYPE   VARCHAR2(250) NOT NULL,
  PGI_GIID   NUMBER(11) NOT NULL,
  PGI_ORDER  NUMBER(6)
);
PROMPT
Creando Primary Key on 'STM_PAR_GI'
ALTER TABLE STM_PAR_GI
  ADD CONSTRAINT STM_PGI_PK PRIMARY KEY (PGI_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_ROL_GGI'
--------------------------------------------------
CREATE TABLE STM_ROL_GGI
(
  RGG_ROLEID NUMBER(11) NOT NULL,
  RGG_GGIID  NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_ROL_GGI'
ALTER TABLE STM_ROL_GGI
  ADD CONSTRAINT STM_RGG_PK PRIMARY KEY (RGG_ROLEID, RGG_GGIID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_GRP_GI'
--------------------------------------------------
CREATE TABLE STM_GRP_GI
(
  GGI_ID   NUMBER(11) NOT NULL,
  GGI_NAME VARCHAR2(80) NOT NULL,
  GGI_TYPE VARCHAR2(30) -- C, F=Fondo, M=Mapa de situación, Si TIPO = F, se trata de grupos de cartografia para selección de fondo
);
PROMPT
Creando Primary Key on 'STM_GRP_GI'
ALTER TABLE STM_GRP_GI
  ADD CONSTRAINT STM_GGI_PK PRIMARY KEY (GGI_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_GGI_GI'
--------------------------------------------------
CREATE TABLE STM_GGI_GI
(
  GGG_GGIID NUMBER(11) NOT NULL,
  GGG_GIID  NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_GGI_GI'
ALTER TABLE STM_GGI_GI
  ADD CONSTRAINT STM_GGG_PK PRIMARY KEY (GGG_GGIID, GGG_GIID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_APP'
--------------------------------------------------
CREATE TABLE STM_APP
(
  APP_ID       NUMBER(11) NOT NULL,
  APP_NAME     VARCHAR2(80) NOT NULL,
  APP_TYPE     VARCHAR2(250),
  APP_TITLE    VARCHAR2(250),                             -- a mostrar en la interficie
  APP_THEME    VARCHAR2(30),                              -- tema a aplicar a la interfície. define el css a utilizar.
  APP_SCALES   VARCHAR2(250),                             -- lista de escalas separadas por comas
  APP_PROJECT  VARCHAR2(250),                             -- la proyecciona a utilizar en esta aplicacion.
  APP_TEMPLATE VARCHAR2(250),                             -- el visor jsp a cargar para esta aplicación
  APP_REFRESH  NUMBER(1) CHECK ("APP_REFRESH" IN (0, 1)), -- indica si la aplicación tiene un boton de "actualizar mapa" o si al activar un layer se actualiza autom.
  APP_ENTRYS   NUMBER(1) CHECK ("APP_ENTRYS" IN (0, 1)),  -- supramunicipal entrar por padre
  APP_ENTRYM   NUMBER(1) CHECK ("APP_ENTRYM" IN (0, 1)),  -- supramunicipal entrar por hijo
  APP_GGIID    NUMBER(11),
  APP_CREATED  TIMESTAMP(6)
);
PROMPT
Creando Primary Key on 'STM_APP'
ALTER TABLE STM_APP
  ADD CONSTRAINT STM_APP_PK PRIMARY KEY (APP_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_BACKGRD'
--------------------------------------------------
CREATE TABLE STM_BACKGRD
(
  BAC_ID      NUMBER(11) NOT NULL,
  BAC_NAME    VARCHAR2(30) NOT NULL,
  BAC_DESC    VARCHAR2(250),
  BAC_ACTIVE  NUMBER(1) CHECK ("BAC_ACTIVE" IN (0, 1)), -- inicialmente seleccionado
  BAC_GGIID   NUMBER(11),
  BAC_CREATED TIMESTAMP(6)
);
PROMPT
Creando Primary Key on 'STM_BACKGRD'
ALTER TABLE STM_BACKGRD
  ADD CONSTRAINT STM_BAC_PK PRIMARY KEY (BAC_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_APP_BCKG'
--------------------------------------------------
CREATE TABLE STM_APP_BCKG
(
  ABC_ID     NUMBER(11) NOT NULL,
  ABC_APPID  NUMBER(11) NOT NULL,
  ABC_BACKID NUMBER(11) NOT NULL,
  ABC_ORDER  NUMBER(6)
);
PROMPT
Creando Primary Key on 'STM_APP_BCKG'
ALTER TABLE STM_APP_BCKG
  ADD CONSTRAINT STM_ABC_PK PRIMARY KEY (ABC_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_APP_BCKG
  ADD CONSTRAINT STM_ABC_UK UNIQUE (ABC_APPID, ABC_BACKID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_APP_TREE'
--------------------------------------------------
CREATE TABLE STM_APP_TREE
(
  ATR_APPID  NUMBER(11) NOT NULL,
  ATR_TREEID NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_APP_TREE'
ALTER TABLE STM_APP_TREE
  ADD CONSTRAINT STM_ATR_PK PRIMARY KEY (ATR_APPID, ATR_TREEID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_APP_ROL'
--------------------------------------------------
CREATE TABLE STM_APP_ROL
(
  ARO_APPID  NUMBER(11) NOT NULL,
  ARO_ROLEID NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_APP_ROL'
ALTER TABLE STM_APP_ROL
  ADD CONSTRAINT STM_ARO_PK PRIMARY KEY (ARO_APPID, ARO_ROLEID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_PAR_APP'
--------------------------------------------------
CREATE TABLE STM_PAR_APP
(
  PAP_ID    NUMBER(11) NOT NULL,
  PAP_NAME  VARCHAR2(30) NOT NULL,
  PAP_VALUE VARCHAR2(250) NOT NULL,
  PAP_TYPE  VARCHAR2(250) NOT NULL, --CHECK ("PAP_TYPE" IN ('PRINT','PRINT_TEMPLATE')),
  PAP_APPID NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_PAR_APP'
ALTER TABLE STM_PAR_APP
  ADD CONSTRAINT STM_PAP_PK PRIMARY KEY (PAP_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_TREE'
--------------------------------------------------
CREATE TABLE STM_TREE
(
  TRE_ID     NUMBER(11) NOT NULL,
  TRE_NAME   VARCHAR2(100) NOT NULL,
  TRE_USERID NUMBER(11) -- si está informado indica que es privado
);
PROMPT
Creando Primary Key on STM_TREE
ALTER TABLE STM_TREE
  ADD CONSTRAINT STM_TRE_PK PRIMARY KEY (TRE_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_TREE_NOD'
--------------------------------------------------
CREATE TABLE STM_TREE_NOD
(
  TNO_ID         NUMBER(11) NOT NULL,
  TNO_PARENTID   NUMBER(11),
  TNO_NAME       VARCHAR2(80) NOT NULL,
  TNO_ABSTRACT   VARCHAR2(250),                                -- Descripción multi-idioma
  TNO_TOOLTIP    VARCHAR2(100),
  TNO_ACTIVE     NUMBER(1) CHECK ("TNO_ACTIVE" IN (0, 1)),     -- inicialmente seleccionado
  TNO_RADIO      NUMBER(1) CHECK ("TNO_RADIO" IN (0, 1)),      -- solo aplicable a carpetas
  TNO_ORDER      NUMBER(6),
  TNO_METAURL    VARCHAR2(2000),                               -- Url metadatos
  TNO_DATAURL    VARCHAR2(2000),                               -- Url zip para descarga
  TNO_FILTER_GM  NUMBER(1) CHECK ("TNO_FILTER_GM" IN (0, 1)),  -- aplicar filtros para GetMap?
  TNO_FILTER_GFI NUMBER(1) CHECK ("TNO_FILTER_GFI" IN (0, 1)), -- aplicar filtros para FeatureInfo?
  TNO_QUERYACT   NUMBER(1) CHECK ("TNO_QUERYACT" IN (0, 1)),   -- si es nulo, utiliza el valor de cartografía
  TNO_FILTER_SE  NUMBER(1) CHECK ("TNO_FILTER_SE" IN (0, 1)),  -- aplicar filtros para selección?
  TNO_TREEID     NUMBER(11) NOT NULL,
  TNO_GIID       NUMBER(11)
);
PROMPT
Creando Primary Key on STM_TREE_NOD
ALTER TABLE STM_TREE_NOD
  ADD CONSTRAINT STM_TNO_PK PRIMARY KEY (TNO_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_TREE_ROL'
--------------------------------------------------
CREATE TABLE STM_TREE_ROL
(
  TRO_TREEID NUMBER(11) NOT NULL,
  TRO_ROLEID NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_TREE_ROL'
ALTER TABLE STM_TREE_ROL
  ADD CONSTRAINT STM_TRO_PK PRIMARY KEY (TRO_TREEID, TRO_ROLEID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_SERVICE'
--------------------------------------------------
CREATE TABLE STM_SERVICE
(
  SER_ID       NUMBER(11) NOT NULL,
  SER_NAME     VARCHAR2(60) NOT NULL,
  SER_ABSTRACT VARCHAR2(250),         -- Descripción multi-idioma
  SER_URL      VARCHAR2(250) NOT NULL,
  SER_PROJECTS VARCHAR2(250),         -- lista de proyecciones que suporta este servicio.ej: EPSG:4326,EPSG:23031,...
  SER_LEGEND   VARCHAR2(250),
  SER_INFOURL  VARCHAR2(250),         -- para servicios com tilecache, AGS, ... que necesitan un url alternativa para resolver infos.
  SER_CREATED  TIMESTAMP(6),
  SER_PROTOCOL VARCHAR2(30) NOT NULL, -- tipo de servicio. (WMS, WFS, TILECACHE, AGS, WMTS)
  SER_NAT_PROT VARCHAR2(10),          -- protocolo nativo del servidor
  SER_CONNID   NUMBER(11)
);
PROMPT
Creando Primary Key on STM_SERVICE
ALTER TABLE STM_SERVICE
  ADD CONSTRAINT STM_SER_PK PRIMARY KEY (SER_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_PAR_SER'
--------------------------------------------------
CREATE TABLE STM_PAR_SER
(
  PSE_ID    NUMBER(11) NOT NULL,
  PSE_SERID NUMBER(11) NOT NULL,
  PSE_TYPE  VARCHAR2(250) NOT NULL, -- Info,
  PSE_NAME  VARCHAR2(30) NOT NULL,
  PSE_VALUE VARCHAR2(250)
);
PROMPT
Creando Primary Key on 'STM_PAR_SER'
ALTER TABLE STM_PAR_SER
  ADD CONSTRAINT STM_PSE_PK PRIMARY KEY (PSE_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Conexiones a bases de datos
PROMPT
Creando Table 'STM_CONNECT'
--------------------------------------------------
CREATE TABLE STM_CONNECT
(
  CON_ID         NUMBER(11) NOT NULL,
  CON_NAME       VARCHAR2(80) NOT NULL,
  CON_DRIVER     VARCHAR2(50) NOT NULL,
  CON_USER       VARCHAR2(50),
  CON_PWD        VARCHAR2(50),
  CON_CONNECTION VARCHAR2(250)
);
PROMPT
Creando Primary Key on 'STM_CONNECT'
ALTER TABLE STM_CONNECT
  ADD CONSTRAINT STM_CON_PK PRIMARY KEY (CON_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_TASK'
--------------------------------------------------
CREATE TABLE STM_TASK
(
  TAS_ID       NUMBER(11) NOT NULL,
  TAS_PARENTID NUMBER(11),
  TAS_NAME     VARCHAR2(512) NOT NULL,
  TAS_CREATED  TIMESTAMP(6),
  TAS_ORDER    NUMBER(6),
  TAS_GIID     NUMBER(11),
  TAS_SERID    NUMBER(11),
  TAS_GTASKID  NUMBER(11),
  TAS_TTASKID  NUMBER(11),
  TAS_TUIID    NUMBER(11),
  TAS_CONNID   NUMBER(11)
);
PROMPT
Creando Primary Key on STM_TASK
ALTER TABLE STM_TASK
  ADD CONSTRAINT STM_TAS_PK PRIMARY KEY (TAS_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_GRP_TSK'
--------------------------------------------------
CREATE TABLE STM_GRP_TSK
(
  GTS_ID   NUMBER(11) NOT NULL,
  GTS_NAME VARCHAR2(80) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_GRP_TSK'
ALTER TABLE STM_GRP_TSK
  ADD CONSTRAINT STM_GTS_PK PRIMARY KEY (GTS_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_TSK_TYP'
--------------------------------------------------
CREATE TABLE STM_TSK_TYP
(
  TTY_ID   NUMBER(11) NOT NULL,
  TTY_NAME VARCHAR2(30) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_TSK_TYP'
ALTER TABLE STM_TSK_TYP
  ADD CONSTRAINT STM_TTY_PK PRIMARY KEY (TTY_ID) USING INDEX TABLESPACE STM3_NDX;



--------------------------------------------------
PROMPT
Creando Table 'STM_TSK_UI'
--------------------------------------------------
CREATE TABLE STM_TSK_UI
(
  TUI_ID      NUMBER(11) NOT NULL,
  TUI_NAME    VARCHAR2(30) NOT NULL,
  TUI_TOOLTIP VARCHAR2(100),
  TUI_ORDER   NUMBER(6),
  TUI_TYPE    VARCHAR2(30)
);
PROMPT
Creando Primary Key on 'STM_TSK_UI'
ALTER TABLE STM_TSK_UI
  ADD CONSTRAINT STM_TUI_PK PRIMARY KEY (TUI_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_ROL_TSK'
--------------------------------------------------
CREATE TABLE STM_ROL_TSK
(
  RTS_ROLEID NUMBER(11) NOT NULL,
  RTS_TASKID NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_ROL_TSK'
ALTER TABLE STM_ROL_TSK
  ADD CONSTRAINT STM_RTS_PK PRIMARY KEY (RTS_ROLEID, RTS_TASKID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Sugerencias de los usuarios
PROMPT
Creando Table 'STM_COMMENT'
--------------------------------------------------
CREATE TABLE STM_COMMENT
(
  COM_ID      NUMBER(11) NOT NULL,
  COM_COORD_X FLOAT(126) NOT NULL,
  COM_COORD_Y FLOAT(126) NOT NULL,
  COM_NAME    VARCHAR2(250),
  COM_EMAIL   VARCHAR2(250),
  COM_TITLE   VARCHAR2(500),
  COM_DESC    VARCHAR2(1000),
  COM_CREATED TIMESTAMP(6),
  COM_USERID  NUMBER(11),
  COM_APPID   NUMBER(11)
);
PROMPT
Creando Primary Key on 'STM_COMMENT'
ALTER TABLE STM_COMMENT
  ADD CONSTRAINT STM_COM_PK PRIMARY KEY (COM_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
PROMPT
Creando Table 'STM_LOG'
--------------------------------------------------
CREATE TABLE STM_LOG
(
  LOG_ID     NUMBER(11) NOT NULL,
  LOG_DATE   TIMESTAMP(6),
  LOG_TYPE   VARCHAR2(50),
  LOG_USERID NUMBER(11),
  LOG_APPID  NUMBER(11),
  LOG_TERID  NUMBER(11),
  LOG_TASKID NUMBER(11),
  LOG_COUNT  NUMBER(11),
  LOG_TER    VARCHAR2(250),
  LOG_TEREXT VARCHAR2(250),
  LOG_DATA   VARCHAR2(250),
  LOG_SRS    VARCHAR2(250),
  LOG_FORMAT VARCHAR2(250),
  LOG_BUFFER NUMBER(1) CHECK ("LOG_BUFFER" IN (0, 1)),
  LOG_EMAIL  VARCHAR2(250),
  LOG_OTHER  VARCHAR2(4000),
  LOG_GIID   NUMBER(11)
);
PROMPT
Creando Primary Key on 'STM_LOG'
ALTER TABLE STM_LOG
  ADD CONSTRAINT STM_LOG_PK PRIMARY KEY (LOG_ID) USING INDEX TABLESPACE STM3_NDX;



--------------------------------------------------
-- Cargo
PROMPT
Creando Table 'STM_POST'
--------------------------------------------------
CREATE TABLE STM_POST
(
  POS_ID         NUMBER(11) NOT NULL,
  POS_POST       VARCHAR2(250),
  POS_ORG        VARCHAR2(250),
  POS_EMAIL      VARCHAR2(250),
  POS_CREATED    TIMESTAMP(6),
  POS_EXPIRATION TIMESTAMP(6),
  POS_TYPE       VARCHAR2(2),
  POS_USERID     NUMBER(11) NOT NULL,
  POS_TERID      NUMBER(11) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_POST'
ALTER TABLE STM_POST
  ADD CONSTRAINT STM_POS_PK PRIMARY KEY (POS_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_POST
  ADD CONSTRAINT STM_POS_UK UNIQUE (POS_USERID, POS_TERID) USING INDEX TABLESPACE STM3_NDX;




--------------------------------------------------
-- Idiomas disponibles
PROMPT
Creando Table 'STM_LANGUAGE'
--------------------------------------------------
CREATE TABLE STM_LANGUAGE
(
  LAN_ID        NUMBER(11) NOT NULL,
  LAN_SHORTNAME VARCHAR2(3) NOT NULL, -- (CAT,ESP,ENG,...)
  LAN_NAME      VARCHAR2(80) NOT NULL -- Català, Español, English, ...
);
PROMPT
Creando Primary Key on 'STM_LANGUAGE'
ALTER TABLE STM_LANGUAGE
  ADD CONSTRAINT STM_LAN_PK PRIMARY KEY (LAN_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_LANGUAGE
  ADD CONSTRAINT STM_LAN_UK UNIQUE (LAN_SHORTNAME) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Tabla de traducciones
PROMPT
Creando Table 'STM_TRANSLATION'
--------------------------------------------------
CREATE TABLE STM_TRANSLATION
(
  TRA_ID     NUMBER(11) NOT NULL,
  TRA_ELEID  NUMBER(11) NOT NULL,
  TRA_COLUMN VARCHAR2(30) NOT NULL, -- nombre de columna incluye el prefijo de la tabla
  TRA_LANID  NUMBER(11) NOT NULL,
  TRA_NAME   VARCHAR2(250) NOT NULL
);
PROMPT
Creando Primary Key on 'STM_TRANSLATION'
ALTER TABLE STM_TRANSLATION
  ADD CONSTRAINT STM_TRA_PK PRIMARY KEY (TRA_ID) USING INDEX TABLESPACE STM3_NDX;
ALTER TABLE STM_TRANSLATION
  ADD CONSTRAINT STM_TRA_UK UNIQUE (TRA_ELEID, TRA_COLUMN, TRA_LANID) USING INDEX TABLESPACE STM3_NDX;

--------------------------------------------------
-- Tabla auxiliar para migración de STM2
PROMPT
Creando Table 'STM_AUXMIGRATION'
--------------------------------------------------
CREATE TABLE STM_AUXMIGRATION
(
  AUX_ID        NUMBER(11) NOT NULL,
  AUX_ELEID     NUMBER(11) NOT NULL,
  AUX_COLUMN    VARCHAR2(30) NOT NULL,                                 -- nombre de columna incluye el prefijo de la tabla
  AUX_VALUE_A   VARCHAR2(4000),                                        -- Valor alfanumérico
  AUX_VALUE_D   DATE,                                                  -- Valor fecha
  AUX_VALUE_N   NUMBER,                                                -- Valor numérico
  AUX_VALUETYPE VARCHAR2(1) CHECK ("AUX_VALUETYPE" IN ('A', 'D', 'N')) -- Tipo del valor (A=Alfanumérico,D=Fecha,N=Numérico)
);
PROMPT
Creando Primary Key on 'STM_TRANSLATION'
ALTER TABLE STM_AUXMIGRATION
  ADD CONSTRAINT STM_AUX_PK PRIMARY KEY (AUX_ID) USING INDEX TABLESPACE STM3_NDX;


--------------------------------------------------
-- Integridad referencial
--------------------------------------------------

PROMPT
Creando Foreign Key on 'STM_TREE'
ALTER TABLE STM_TREE
  ADD CONSTRAINT
    STM_TRE_FK_USE FOREIGN KEY (TRE_USERID) REFERENCES STM_USER (USE_ID);

PROMPT
Creando Foreign Key on 'STM_USR_CONF'
ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT
    STM_UCO_FK_USE FOREIGN KEY (UCO_USERID) REFERENCES STM_USER (USE_ID);
ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT
    STM_UCO_FK_TER FOREIGN KEY (UCO_TERID) REFERENCES STM_TERRITORY (TER_ID);
ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT
    STM_UCO_FK_ROL FOREIGN KEY (UCO_ROLEID) REFERENCES STM_ROLE (ROL_ID);
ALTER TABLE STM_USR_CONF
  ADD CONSTRAINT
    STM_UCO_FK_ROLM FOREIGN KEY (UCO_ROLEMID) REFERENCES STM_ROLE (ROL_ID);

PROMPT
Creando Foreign Key on 'STM_AVAIL_TSK'
ALTER TABLE STM_AVAIL_TSK
  ADD CONSTRAINT
    STM_ATS_FK_TER FOREIGN KEY (ATS_TERID) REFERENCES STM_TERRITORY (TER_ID);
ALTER TABLE STM_AVAIL_TSK
  ADD CONSTRAINT
    STM_ATS_FK_TAS FOREIGN KEY (ATS_TASKID) REFERENCES STM_TASK (TAS_ID);

PROMPT
Creando Foreign Key on 'STM_AVAIL_GI'
ALTER TABLE STM_AVAIL_GI
  ADD CONSTRAINT
    STM_AGI_FK_TER FOREIGN KEY (AGI_TERID) REFERENCES STM_TERRITORY (TER_ID);
ALTER TABLE STM_AVAIL_GI
  ADD CONSTRAINT
    STM_AGI_FK_GEO FOREIGN KEY (AGI_GIID) REFERENCES STM_GEOINFO (GEO_ID);

PROMPT
Creando Foreign Key on 'STM_TREE_NOD'
ALTER TABLE STM_TREE_NOD
  ADD CONSTRAINT
    STM_TNO_FK_TNO FOREIGN KEY (TNO_PARENTID) REFERENCES STM_TREE_NOD (TNO_ID);
ALTER TABLE STM_TREE_NOD
  ADD CONSTRAINT
    STM_TNO_FK_GEO FOREIGN KEY (TNO_GIID) REFERENCES STM_GEOINFO (GEO_ID);
ALTER TABLE STM_TREE_NOD
  ADD CONSTRAINT
    STM_TNO_FK_TRE FOREIGN KEY (TNO_TREEID) REFERENCES STM_TREE (TRE_ID);

PROMPT
Creando Foreign Key on 'STM_PAR_SER'
ALTER TABLE STM_PAR_SER
  ADD CONSTRAINT
    STM_PSE_FK_SER FOREIGN KEY (PSE_SERID) REFERENCES STM_SERVICE (SER_ID);

PROMPT
Creando Foreign Key on 'STM_TREE_ROL'
ALTER TABLE STM_TREE_ROL
  ADD CONSTRAINT
    STM_TRO_FK_TRE FOREIGN KEY (TRO_TREEID) REFERENCES STM_TREE (TRE_ID);
ALTER TABLE STM_TREE_ROL
  ADD CONSTRAINT
    STM_TRO_FK_ROL FOREIGN KEY (TRO_ROLEID) REFERENCES STM_ROLE (ROL_ID);

PROMPT
Creando Foreign Key on 'STM_TASK'
ALTER TABLE STM_TASK
  ADD CONSTRAINT
    STM_TAS_FK_GEO FOREIGN KEY (TAS_GIID) REFERENCES STM_GEOINFO (GEO_ID);
ALTER TABLE STM_TASK
  ADD CONSTRAINT
    STM_TAS_FK_SER FOREIGN KEY (TAS_SERID) REFERENCES STM_SERVICE (SER_ID);
ALTER TABLE STM_TASK
  ADD CONSTRAINT
    STM_TAS_FK_GTS FOREIGN KEY (TAS_GTASKID) REFERENCES STM_GRP_TSK (GTS_ID);
ALTER TABLE STM_TASK
  ADD CONSTRAINT
    STM_TAS_FK_TTY FOREIGN KEY (TAS_TTASKID) REFERENCES STM_TSK_TYP (TTY_ID);
ALTER TABLE STM_TASK
  ADD CONSTRAINT
    STM_TAS_FK_TUI FOREIGN KEY (TAS_TUIID) REFERENCES STM_TSK_UI (TUI_ID);
ALTER TABLE STM_TASK
  ADD CONSTRAINT
    STM_TAS_FK_CON FOREIGN KEY (TAS_CONNID) REFERENCES STM_CONNECT (CON_ID);
ALTER TABLE STM_TASK
  ADD CONSTRAINT
    STM_TAS_FK_TAS FOREIGN KEY (TAS_PARENTID) REFERENCES STM_TASK (TAS_ID);

PROMPT
Creando Foreign Key on 'STM_GEOINFO'
ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT
    STM_GEO_FK_SER FOREIGN KEY (GEO_SERID) REFERENCES STM_SERVICE (SER_ID);
ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT
    STM_GEO_FK_SERSEL FOREIGN KEY (GEO_SERSELID) REFERENCES STM_SERVICE (SER_ID);
ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT
    STM_GEO_FK_CON FOREIGN KEY (GEO_CONNID) REFERENCES STM_CONNECT (CON_ID);
ALTER TABLE STM_GEOINFO
  ADD CONSTRAINT
    STM_GEO_FK_SGI FOREIGN KEY (GEO_STYID) REFERENCES STM_STY_GI (SGI_ID);

PROMPT
Creando Foreign Key on 'STM_PAR_GI'
ALTER TABLE STM_PAR_GI
  ADD CONSTRAINT
    STM_PGI_FK_GEO FOREIGN KEY (PGI_GIID) REFERENCES STM_GEOINFO (GEO_ID);


PROMPT
Creando Foreign Key on 'STM_ROL_GGI'
ALTER TABLE STM_ROL_GGI
  ADD CONSTRAINT
    STM_RGG_FK_ROL FOREIGN KEY (RGG_ROLEID) REFERENCES STM_ROLE (ROL_ID);
ALTER TABLE STM_ROL_GGI
  ADD CONSTRAINT
    STM_RGG_FK_GGI FOREIGN KEY (RGG_GGIID) REFERENCES STM_GRP_GI (GGI_ID);

PROMPT
Creando Foreign Key on 'STM_APP'
ALTER TABLE STM_APP
  ADD CONSTRAINT
    STM_APP_FK_GGI FOREIGN KEY (APP_GGIID) REFERENCES STM_GRP_GI (GGI_ID);

PROMPT
Creando Foreign Key on 'STM_PAR_APP'
ALTER TABLE STM_PAR_APP
  ADD CONSTRAINT
    STM_PAP_FK_APP FOREIGN KEY (PAP_APPID) REFERENCES STM_APP (APP_ID);

PROMPT
Creando Foreign Key on 'STM_BACKGRD'
ALTER TABLE STM_BACKGRD
  ADD CONSTRAINT
    STM_BAC_FK_GGI FOREIGN KEY (BAC_GGIID) REFERENCES STM_GRP_GI (GGI_ID);

PROMPT
Creando Foreign Key on 'STM_APP_BCKG'
ALTER TABLE STM_APP_BCKG
  ADD CONSTRAINT
    STM_ABC_FK_APP FOREIGN KEY (ABC_APPID) REFERENCES STM_APP (APP_ID);
ALTER TABLE STM_APP_BCKG
  ADD CONSTRAINT
    STM_ABC_FK_FON FOREIGN KEY (ABC_BACKID) REFERENCES STM_BACKGRD (BAC_ID);

PROMPT
Creando Foreign Key on 'STM_APP_TREE'
ALTER TABLE STM_APP_TREE
  ADD CONSTRAINT
    STM_ATR_FK_APP FOREIGN KEY (ATR_APPID) REFERENCES STM_APP (APP_ID);
ALTER TABLE STM_APP_TREE
  ADD CONSTRAINT
    STM_ATR_FK_TRE FOREIGN KEY (ATR_TREEID) REFERENCES STM_TREE (TRE_ID);

PROMPT
Creando Foreign Key on 'STM_APP_ROL'
ALTER TABLE STM_APP_ROL
  ADD CONSTRAINT
    STM_ARO_FK_APP FOREIGN KEY (ARO_APPID) REFERENCES STM_APP (APP_ID);
ALTER TABLE STM_APP_ROL
  ADD CONSTRAINT
    STM_ARO_FK_ROL FOREIGN KEY (ARO_ROLEID) REFERENCES STM_ROLE (ROL_ID);

PROMPT
Creando Foreign Key on 'STM_ROL_TSK'
ALTER TABLE STM_ROL_TSK
  ADD CONSTRAINT
    STM_RTS_FK_ROL FOREIGN KEY (RTS_ROLEID) REFERENCES STM_ROLE (ROL_ID);
ALTER TABLE STM_ROL_TSK
  ADD CONSTRAINT
    STM_RTS_FK_TAS FOREIGN KEY (RTS_TASKID) REFERENCES STM_TASK (TAS_ID);

PROMPT
Creando Foreign Key on 'STM_FIL_GI'
ALTER TABLE STM_FIL_GI
  ADD CONSTRAINT
    STM_FGI_FK_GEO FOREIGN KEY (FGI_GIID) REFERENCES STM_GEOINFO (GEO_ID);
ALTER TABLE STM_FIL_GI
  ADD CONSTRAINT
    STM_FGI_FK_TET FOREIGN KEY (FGI_TYPID) REFERENCES STM_TER_TYP (TET_ID);

PROMPT
Creando Foreign Key on 'STM_STY_GI'
ALTER TABLE STM_STY_GI
  ADD CONSTRAINT
    STM_SGI_FK_GEO FOREIGN KEY (SGI_GIID) REFERENCES STM_GEOINFO (GEO_ID);

PROMPT
Creando Foreign Key on 'STM_GGI_GI'
ALTER TABLE STM_GGI_GI
  ADD CONSTRAINT
    STM_GGG_FK_GGI FOREIGN KEY (GGG_GGIID) REFERENCES STM_GRP_GI (GGI_ID);
ALTER TABLE STM_GGI_GI
  ADD CONSTRAINT
    STM_GGG_FK_GEO FOREIGN KEY (GGG_GIID) REFERENCES STM_GEOINFO (GEO_ID);

PROMPT
Creando Foreign Key on 'STM_TERRITORY'
ALTER TABLE STM_TERRITORY
  ADD CONSTRAINT
    STM_TER_FK_GTT FOREIGN KEY (TER_GTYPID) REFERENCES STM_GTER_TYP (GTT_ID);
ALTER TABLE STM_TERRITORY
  ADD CONSTRAINT
    STM_TER_FK_TET FOREIGN KEY (TER_TYPID) REFERENCES STM_TER_TYP (TET_ID);

PROMPT
Creando Foreign Key on 'STM_GRP_TER'
ALTER TABLE STM_GRP_TER
  ADD CONSTRAINT
    STM_GTE_FK_TER FOREIGN KEY (GTE_TERID) REFERENCES STM_TERRITORY (TER_ID);
ALTER TABLE STM_GRP_TER
  ADD CONSTRAINT
    STM_GTE_FK_TERM FOREIGN KEY (GTE_TERMID) REFERENCES STM_TERRITORY (TER_ID);

PROMPT
Creando Foreign Key on 'STM_POST'
ALTER TABLE STM_POST
  ADD CONSTRAINT
    STM_POS_FK_USE FOREIGN KEY (POS_USERID) REFERENCES STM_USER (USE_ID);
ALTER TABLE STM_POST
  ADD CONSTRAINT
    STM_POS_FK_TER FOREIGN KEY (POS_TERID) REFERENCES STM_TERRITORY (TER_ID);

PROMPT
Creando Foreign Key on 'STM_SERVICE'
ALTER TABLE STM_SERVICE
  ADD CONSTRAINT
    STM_SER_FK_CON FOREIGN KEY (SER_CONNID) REFERENCES STM_CONNECT (CON_ID);

PROMPT
Creando Foreign Key on 'STM_TRANSLATION'
ALTER TABLE STM_TRANSLATION
  ADD CONSTRAINT
    STM_TRA_FK_LAN FOREIGN KEY (TRA_LANID) REFERENCES STM_LANGUAGE (LAN_ID);

--------------------------------------------------
PROMPT
this is the
end
