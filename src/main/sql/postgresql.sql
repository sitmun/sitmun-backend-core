
    alter table STM_APPARB 
        drop constraint STM_APA_FK_ARB;

    alter table STM_APPARB 
        drop constraint STM_APA_FK_APP;

    alter table STM_APPFON 
        drop constraint STM_APF_FK_APP;

    alter table STM_APPFON 
        drop constraint STM_APF_FK_FON;

    alter table STM_APPROL 
        drop constraint STM_APR_FK_ROL;

    alter table STM_APPROL 
        drop constraint STM_APR_FK_APP;

    alter table STM_APPS 
        drop constraint STM_APP_FK_GCA;

    alter table STM_ARBOLNOD 
        drop constraint STM_ARN_FK_CAR;

    alter table STM_ARBOLNOD 
        drop constraint STM_ARN_FK_ARN;

    alter table STM_ARBOLNOD 
        drop constraint STM_ARN_FK_ARB;

    alter table STM_ARBROL 
        drop constraint STM_ARR_FK_ROL;

    alter table STM_ARBROL 
        drop constraint STM_ARR_FK_ARB;

    alter table STM_CARGO 
        drop constraint STM_CGO_FK_TER;

    alter table STM_CARGO 
        drop constraint STM_CGO_FK_USU;

    alter table STM_CARTO 
        drop constraint STM_CAR_FK_CON;

    alter table STM_CARTO 
        drop constraint STM_CAR_FK_SERSEL;

    alter table STM_CARTO 
        drop constraint STM_CAR_FK_SER;

    alter table STM_DISPCARTO 
        drop constraint STM_DCA_FK_CAR;

    alter table STM_DISPCARTO 
        drop constraint STM_DCA_FK_TER;

    alter table STM_DISPTAREA 
        drop constraint STM_DTA_FK_TAR;

    alter table STM_DISPTAREA 
        drop constraint STM_DTA_FK_TER;

    alter table STM_ETERRIT 
        drop constraint STM_TER_FK_TGR;

    alter table STM_FONDO 
        drop constraint STM_FON_FK_GCA;

    alter table STM_GCACAR 
        drop constraint STM_GCC_FK_CAR;

    alter table STM_GCACAR 
        drop constraint STM_GCC_FK_GCA;

    alter table STM_GRPTER 
        drop constraint STM_GRT_FK_TERM;

    alter table STM_GRPTER 
        drop constraint STM_GRT_FK_TER;

    alter table STM_PARAMAPP 
        drop constraint STM_PAP_FK_APP;

    alter table STM_PARAMSER 
        drop constraint STM_PSE_FK_SER;

    alter table STM_PARAMTTA 
        drop constraint STM_PTT_FK_TAR;

    alter table STM_ROLGCA 
        drop constraint STM_RGC_FK_GCA;

    alter table STM_ROLGCA 
        drop constraint STM_RGC_FK_ROL;

    alter table STM_ROLTAR 
        drop constraint STM_RTA_FK_T;

    alter table STM_ROLTAR 
        drop constraint STM_RTA_FK_ROL;

    alter table STM_SERVICIO 
        drop constraint STM_SER_FK_CON;

    alter table STM_TAREA 
        drop constraint STM_TAR_FK_CON;

    alter table STM_TAREA 
        drop constraint STM_TAR_FK_GTA;

    alter table STM_TAREA 
        drop constraint STM_TAR_FK_TTA;

    alter table STM_TAREA 
        drop constraint STM_TAR_FK_TUI;

    alter table STM_USUCONF 
        drop constraint STM_UCF_FK_ROL;

    alter table STM_USUCONF 
        drop constraint STM_UCF_FK_TER;

    alter table STM_USUCONF 
        drop constraint STM_UCF_FK_USU;

    drop table if exists GTA_CODIGO cascade;

    drop table if exists STM_APPARB cascade;

    drop table if exists STM_APPFON cascade;

    drop table if exists STM_APPROL cascade;

    drop table if exists STM_APPS cascade;

    drop table if exists STM_ARBOL cascade;

    drop table if exists STM_ARBOLNOD cascade;

    drop table if exists STM_ARBROL cascade;

    drop table if exists STM_CARGO cascade;

    drop table if exists STM_CARTO cascade;

    drop table if exists STM_CODIGOS cascade;

    drop table if exists STM_CONEXION cascade;

    drop table if exists STM_DISPCARTO cascade;

    drop table if exists STM_DISPTAREA cascade;

    drop table if exists STM_ETERRIT cascade;

    drop table if exists STM_FONDO cascade;

    drop table if exists STM_GCACAR cascade;

    drop table if exists STM_GRPCARTO cascade;

    drop table if exists STM_GRPTER cascade;

    drop table if exists STM_LOG cascade;

    drop table if exists STM_PARAMAPP cascade;

    drop table if exists STM_PARAMSER cascade;

    drop table if exists STM_PARAMTTA cascade;

    drop table if exists STM_ROLES cascade;

    drop table if exists STM_ROLGCA cascade;

    drop table if exists STM_ROLTAR cascade;

    drop table if exists STM_SERVICIO cascade;

    drop table if exists STM_TAREA cascade;

    drop table if exists STM_TAREA_UI cascade;

    drop table if exists STM_TIPOGRP cascade;

    drop table if exists STM_TIPOTAREA cascade;

    drop table if exists STM_USUARIO cascade;

    drop table if exists STM_USUCONF cascade;

    create table GTA_CODIGO (
        GTA_CODIGO numeric(11, 0) not null,
        GTA_NOMBRE varchar(80),
        primary key (GTA_CODIGO)
    );

    create table STM_APPARB (
        APA_CODAPP numeric(11, 0) not null,
        APA_CODARB numeric(11, 0) not null,
        primary key (APA_CODAPP, APA_CODARB)
    );

    create table STM_APPFON (
        APF_CODIGO numeric(11, 0) not null,
        APF_ORDEN numeric(6, 0),
        APF_CODAPP numeric(11, 0),
        APF_CODFON numeric(11, 0),
        primary key (APF_CODIGO)
    );

    create table STM_APPROL (
        APR_CODAPP numeric(11, 0) not null,
        APR_CODROL numeric(11, 0) not null,
        primary key (APR_CODAPP, APR_CODROL)
    );

    create table STM_APPS (
        APP_CODIGO numeric(11, 0) not null,
        APP_F_ALTA timestamp,
        APP_NOMBRE varchar(80),
        APP_PROJECT varchar(250),
        APP_ESCALAS varchar(250),
        APP_TEMA varchar(30),
        APP_TITULO varchar(250),
        APP_AUTOREFR boolean,
        APP_TIPO varchar(250),
        APP_CODGCA numeric(11, 0),
        primary key (APP_CODIGO)
    );

    create table STM_ARBOL (
        ARB_CODIGO numeric(11, 0) not null,
        ARB_NOMBRE varchar(100),
        primary key (ARB_CODIGO)
    );

    create table STM_ARBOLNOD (
        ANR_CODIGO numeric(11, 0) not null,
        ARN_ACTIVO boolean,
        ARN_NOMBRE varchar(80),
        ARN_ORDEN numeric(6, 0),
        ARN_TOOLTIP varchar(100),
        ARN_CODCAR numeric(11, 0),
        ARN_CODPADRE numeric(11, 0),
        ARN_CODARB numeric(11, 0),
        primary key (ANR_CODIGO)
    );

    create table STM_ARBROL (
        ARR_CODARB numeric(11, 0) not null,
        ARR_CORROL numeric(11, 0) not null,
        primary key (ARR_CODARB, ARR_CORROL)
    );

    create table STM_CARGO (
        CGO_CODIGO numeric(11, 0) not null,
        CGO_F_ALTA timestamp,
        CGO_F_CADUC timestamp,
        CGO_CORREO varchar(250),
        CGO_CARGO varchar(250),
        CGO_ORG varchar(250),
        CGO_CODTER numeric(11, 0),
        CGO_CODUSU numeric(11, 0),
        primary key (CGO_CODIGO)
    );

    create table STM_CARTO (
        CAR_CODIGO numeric(11, 0) not null,
        CAR_F_ALTA timestamp,
        CAR_EDITABLE boolean,
        CAR_TIPOGEOM varchar(255),
        CAR_CAPAS varchar(500),
        CAR_LEYENDTIP varchar(50),
        CAR_LEYENDURL varchar(250),
        CAR_ESC_MAX numeric(11, 0),
        CAR_METAURL varchar(255),
        CAR_ESC_MIN numeric(11, 0),
        CAR_NOMBRE varchar(100),
        CAR_ORDEN numeric(11, 0),
        CAR_QUERYACT boolean,
        CAR_QUERYLAY varchar(500),
        CAR_QUERYABL boolean,
        CAR_SELECTABL boolean,
        CAR_SELECTLAY varchar(500),
        CAR_TEMATIZABLE boolean,
        CAR_TRANSP numeric(11, 0),
        CAR_TIPO varchar(30),
        CAR_VISIBLE boolean,
        CAR_CODCON numeric(11, 0),
        CAR_CODSERSEL numeric(11, 0),
        CAR_CODSER numeric(11, 0),
        primary key (CAR_CODIGO)
    );

    create table STM_CODIGOS (
        GEN_CODIGO varchar(255) not null,
        GEN_VALOR int8,
        primary key (GEN_CODIGO)
    );

    create table STM_CONEXION (
        CON_CODIGO numeric(11, 0) not null,
        CON_CONSTRING varchar(250),
        CON_NOMBRE varchar(80),
        CON_PASSWORD varchar(50),
        CON_DRIVER varchar(50),
        CON_USUARIO varchar(50),
        primary key (CON_CODIGO)
    );

    create table STM_DISPCARTO (
        DCA_CODIGO numeric(11, 0) not null,
        DCA_F_ALTA timestamp,
        DCA_CODCAR numeric(11, 0),
        DCA_CODTER numeric(11, 0),
        primary key (DCA_CODIGO)
    );

    create table STM_DISPTAREA (
        DTA_CODIGO numeric(11, 0) not null,
        DTA_F_ALTA timestamp,
        DTA_CODTAR numeric(11, 0),
        DTA_CODTER numeric(11, 0),
        primary key (DTA_CODIGO)
    );

    create table STM_ETERRIT (
        TER_CODIGO numeric(11, 0) not null,
        TER_DIRECC varchar(250),
        TER_BLOQ boolean,
        TER_OBSERV varchar(250),
        TER_F_ALTA timestamp,
        TER_CORREO varchar(250),
        TER_EXT varchar(250),
        TER_LOGO varchar(250),
        TER_NOMBRE varchar(250) not null,
        TER_NADMIN varchar(250),
        TER_AMBITO varchar(250),
        TER_CODTGR numeric(11, 0),
        primary key (TER_CODIGO)
    );

    create table STM_FONDO (
        FON_CODIGO numeric(11, 0) not null,
        FON_ACTIVO boolean,
        FON_F_ALTA timestamp,
        FON_DESC varchar(250),
        FON_NOMBRE varchar(30),
        FON_CODGCA numeric(11, 0),
        primary key (FON_CODIGO)
    );

    create table STM_GCACAR (
        GCC_CODGCA numeric(11, 0) not null,
        GCC_CODCAR numeric(11, 0) not null,
        primary key (GCC_CODGCA, GCC_CODCAR)
    );

    create table STM_GRPCARTO (
        GCA_CODIGO numeric(11, 0) not null,
        GCA_NOMBRE varchar(80),
        GCA_TIPO varchar(30),
        primary key (GCA_CODIGO)
    );

    create table STM_GRPTER (
        GRT_CODTER numeric(11, 0) not null,
        GRT_CODTERM numeric(11, 0) not null,
        primary key (GRT_CODTERM, GRT_CODTER)
    );

    create table STM_LOG (
        LOG_CODIGO numeric(11, 0) not null,
        LOG_CODAPP numeric(11, 0),
        LOG_CODTAR numeric(11, 0),
        LOG_CODTER numeric(11, 0),
        LOG_CODUSU numeric(11, 0),
        LOG_CONT numeric(11, 0),
        LOG_FECHA timestamp,
        LOG_TIPO varchar(50),
        primary key (LOG_CODIGO)
    );

    create table STM_PARAMAPP (
        PAP_CODIGO numeric(11, 0) not null,
        PAP_NOMBRE varchar(30),
        PAP_TIPO varchar(250),
        PAP_VALOR varchar(250),
        PAP_CODAPP numeric(11, 0),
        primary key (PAP_CODIGO)
    );

    create table STM_PARAMSER (
        PSE_CODIGO numeric(11, 0) not null,
        PSE_NOMBRE varchar(30),
        PSE_TIPO varchar(250),
        PSE_VALOR varchar(250),
        PSE_CODSER numeric(11, 0),
        primary key (PSE_CODIGO)
    );

    create table STM_PARAMTTA (
        PTT_CODIGO numeric(11, 0) not null,
        PTT_NOMBRE varchar(50),
        PTT_ORDEN numeric(6, 0),
        PTT_TIPO varchar(30),
        PTT_VALOR varchar(512),
        PTT_CODTAR numeric(11, 0),
        primary key (PTT_CODIGO)
    );

    create table STM_ROLES (
        ROL_CODIGO numeric(11, 0) not null,
        ROL_OBSERV varchar(500),
        ROL_NOMBRE varchar(250) not null,
        primary key (ROL_CODIGO)
    );

    create table STM_ROLGCA (
        RGC_CODROL numeric(11, 0) not null,
        RGC_CODGCA numeric(11, 0) not null,
        primary key (RGC_CODROL, RGC_CODGCA)
    );

    create table STM_ROLTAR (
        RTA_CODROL numeric(11, 0) not null,
        RTA_CODTAR numeric(11, 0) not null,
        primary key (RTA_CODROL, RTA_CODTAR)
    );

    create table STM_SERVICIO (
        SER_CODIGO numeric(11, 0) not null,
        SER_F_ALTA timestamp,
        SER_INFOURL varchar(250),
        SER_LEYENDA varchar(250),
        SER_NOMBRE varchar(30),
        SER_PROJECTS varchar(1000),
        SER_TIPO varchar(30),
        SER_URL varchar(250),
        SER_CODCON numeric(11, 0),
        primary key (SER_CODIGO)
    );

    create table STM_TAREA (
        TAR_CODIGO numeric(11, 0) not null,
        TAR_F_ALTA timestamp,
        TAR_NOMBRE varchar(250),
        TAR_ORDEN numeric(6, 0),
        TAR_CODCON numeric(11, 0),
        TAR_CODGTA numeric(11, 0),
        TAR_CODTTA numeric(11, 0),
        TAR_CODTUI numeric(11, 0),
        primary key (TAR_CODIGO)
    );

    create table STM_TAREA_UI (
        TUI_CODIGO numeric(11, 0) not null,
        TUI_NOMBRE varchar(30),
        TUI_ORDEN numeric(6, 0),
        TUI_TOOLTIP varchar(100),
        TUI_TIPO varchar(30),
        primary key (TUI_CODIGO)
    );

    create table STM_TIPOGRP (
        TGR_CODIGO numeric(11, 0) not null,
        TGR_NOMBRE varchar(250) not null,
        primary key (TGR_CODIGO)
    );

    create table STM_TIPOTAREA (
        TTA_CODIGO numeric(11, 0) not null,
        TTA_NOMBRE varchar(30),
        primary key (TTA_CODIGO)
    );

    create table STM_USUARIO (
        USU_CODIGO numeric(11, 0) not null,
        USU_ADM boolean,
        USU_BLOQ boolean,
        USU_NOMBRE varchar(30),
        USU_APELLIDOS varchar(40),
        USU_PASSWORD varchar(128),
        USU_USUARIO varchar(30) not null,
        primary key (USU_CODIGO)
    );

    create table STM_USUCONF (
        UCF_CODIGO numeric(11, 0) not null,
        UCF_CODROL numeric(11, 0),
        UCF_CODTER numeric(11, 0),
        UCF_CODUSU numeric(11, 0),
        primary key (UCF_CODIGO)
    );

    alter table STM_APPFON 
        add constraint STM_APF_UK unique (APF_CODAPP, APF_CODFON);

    alter table STM_DISPCARTO 
        add constraint STM_DCA_UK unique (DCA_CODTER, DCA_CODCAR);

    alter table STM_DISPTAREA 
        add constraint STM_DTA_UK unique (DTA_CODTER, DTA_CODTAR);

    alter table STM_ETERRIT 
        add constraint STM_TER_NOM_UK unique (TER_NOMBRE);

    alter table STM_ROLES 
        add constraint STM_ROL_NOM_UK unique (ROL_NOMBRE);

    alter table STM_TIPOGRP 
        add constraint STM_TGR_NOM_UK unique (TGR_NOMBRE);

    alter table STM_USUARIO 
        add constraint STM_USU_USU_UK unique (USU_USUARIO);

    alter table STM_USUCONF 
        add constraint STM_UCF_UK unique (UCF_CODUSU, UCF_CODTER, UCF_CODROL);

    alter table STM_APPARB 
        add constraint STM_APA_FK_ARB 
        foreign key (APA_CODARB) 
        references STM_ARBOL;

    alter table STM_APPARB 
        add constraint STM_APA_FK_APP 
        foreign key (APA_CODAPP) 
        references STM_APPS;

    alter table STM_APPFON 
        add constraint STM_APF_FK_APP 
        foreign key (APF_CODAPP) 
        references STM_APPS;

    alter table STM_APPFON 
        add constraint STM_APF_FK_FON 
        foreign key (APF_CODFON) 
        references STM_FONDO;

    alter table STM_APPROL 
        add constraint STM_APR_FK_ROL 
        foreign key (APR_CODROL) 
        references STM_ROLES;

    alter table STM_APPROL 
        add constraint STM_APR_FK_APP 
        foreign key (APR_CODAPP) 
        references STM_APPS;

    alter table STM_APPS 
        add constraint STM_APP_FK_GCA 
        foreign key (APP_CODGCA) 
        references STM_GRPCARTO;

    alter table STM_ARBOLNOD 
        add constraint STM_ARN_FK_CAR 
        foreign key (ARN_CODCAR) 
        references STM_CARTO;

    alter table STM_ARBOLNOD 
        add constraint STM_ARN_FK_ARN 
        foreign key (ARN_CODPADRE) 
        references STM_ARBOLNOD;

    alter table STM_ARBOLNOD 
        add constraint STM_ARN_FK_ARB 
        foreign key (ARN_CODARB) 
        references STM_ARBOL;

    alter table STM_ARBROL 
        add constraint STM_ARR_FK_ROL 
        foreign key (ARR_CORROL) 
        references STM_ROLES;

    alter table STM_ARBROL 
        add constraint STM_ARR_FK_ARB 
        foreign key (ARR_CODARB) 
        references STM_ARBOL;

    alter table STM_CARGO 
        add constraint STM_CGO_FK_TER 
        foreign key (CGO_CODTER) 
        references STM_ETERRIT;

    alter table STM_CARGO 
        add constraint STM_CGO_FK_USU 
        foreign key (CGO_CODUSU) 
        references STM_USUARIO;

    alter table STM_CARTO 
        add constraint STM_CAR_FK_CON 
        foreign key (CAR_CODCON) 
        references STM_CONEXION;

    alter table STM_CARTO 
        add constraint STM_CAR_FK_SERSEL 
        foreign key (CAR_CODSERSEL) 
        references STM_SERVICIO;

    alter table STM_CARTO 
        add constraint STM_CAR_FK_SER 
        foreign key (CAR_CODSER) 
        references STM_SERVICIO;

    alter table STM_DISPCARTO 
        add constraint STM_DCA_FK_CAR 
        foreign key (DCA_CODCAR) 
        references STM_CARTO;

    alter table STM_DISPCARTO 
        add constraint STM_DCA_FK_TER 
        foreign key (DCA_CODTER) 
        references STM_ETERRIT 
        on delete cascade;

    alter table STM_DISPTAREA 
        add constraint STM_DTA_FK_TAR 
        foreign key (DTA_CODTAR) 
        references STM_TAREA;

    alter table STM_DISPTAREA 
        add constraint STM_DTA_FK_TER 
        foreign key (DTA_CODTER) 
        references STM_ETERRIT;

    alter table STM_ETERRIT 
        add constraint STM_TER_FK_TGR 
        foreign key (TER_CODTGR) 
        references STM_TIPOGRP;

    alter table STM_FONDO 
        add constraint STM_FON_FK_GCA 
        foreign key (FON_CODGCA) 
        references STM_GRPCARTO;

    alter table STM_GCACAR 
        add constraint STM_GCC_FK_CAR 
        foreign key (GCC_CODCAR) 
        references STM_CARTO;

    alter table STM_GCACAR 
        add constraint STM_GCC_FK_GCA 
        foreign key (GCC_CODGCA) 
        references STM_GRPCARTO;

    alter table STM_GRPTER 
        add constraint STM_GRT_FK_TERM 
        foreign key (GRT_CODTERM) 
        references STM_ETERRIT;

    alter table STM_GRPTER 
        add constraint STM_GRT_FK_TER 
        foreign key (GRT_CODTER) 
        references STM_ETERRIT;

    alter table STM_PARAMAPP 
        add constraint STM_PAP_FK_APP 
        foreign key (PAP_CODAPP) 
        references STM_APPS;

    alter table STM_PARAMSER 
        add constraint STM_PSE_FK_SER 
        foreign key (PSE_CODSER) 
        references STM_SERVICIO;

    alter table STM_PARAMTTA 
        add constraint STM_PTT_FK_TAR 
        foreign key (PTT_CODTAR) 
        references STM_TAREA;

    alter table STM_ROLGCA 
        add constraint STM_RGC_FK_GCA 
        foreign key (RGC_CODGCA) 
        references STM_ROLES;

    alter table STM_ROLGCA 
        add constraint STM_RGC_FK_ROL 
        foreign key (RGC_CODROL) 
        references STM_GRPCARTO;

    alter table STM_ROLTAR 
        add constraint STM_RTA_FK_T 
        foreign key (RTA_CODTAR) 
        references STM_ROLES;

    alter table STM_ROLTAR 
        add constraint STM_RTA_FK_ROL 
        foreign key (RTA_CODROL) 
        references STM_TAREA;

    alter table STM_SERVICIO 
        add constraint STM_SER_FK_CON 
        foreign key (SER_CODCON) 
        references STM_CONEXION;

    alter table STM_TAREA 
        add constraint STM_TAR_FK_CON 
        foreign key (TAR_CODCON) 
        references STM_CONEXION;

    alter table STM_TAREA 
        add constraint STM_TAR_FK_GTA 
        foreign key (TAR_CODGTA) 
        references GTA_CODIGO;

    alter table STM_TAREA 
        add constraint STM_TAR_FK_TTA 
        foreign key (TAR_CODTTA) 
        references STM_TIPOTAREA;

    alter table STM_TAREA 
        add constraint STM_TAR_FK_TUI 
        foreign key (TAR_CODTUI) 
        references STM_TAREA_UI;

    alter table STM_USUCONF 
        add constraint STM_UCF_FK_ROL 
        foreign key (UCF_CODROL) 
        references STM_ROLES 
        on delete cascade;

    alter table STM_USUCONF 
        add constraint STM_UCF_FK_TER 
        foreign key (UCF_CODTER) 
        references STM_ETERRIT 
        on delete cascade;

    alter table STM_USUCONF 
        add constraint STM_UCF_FK_USU 
        foreign key (UCF_CODUSU) 
        references STM_USUARIO;
