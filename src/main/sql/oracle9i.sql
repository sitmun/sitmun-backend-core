
    drop table GTA_CODIGO cascade constraints;

    drop table STM_APPARB cascade constraints;

    drop table STM_APPFON cascade constraints;

    drop table STM_APPROL cascade constraints;

    drop table STM_APPS cascade constraints;

    drop table STM_ARBOL cascade constraints;

    drop table STM_ARBOLNOD cascade constraints;

    drop table STM_ARBROL cascade constraints;

    drop table STM_CARGO cascade constraints;

    drop table STM_CARTO cascade constraints;

    drop table STM_CODIGOS cascade constraints;

    drop table STM_CONEXION cascade constraints;

    drop table STM_DISPCARTO cascade constraints;

    drop table STM_DISPTAREA cascade constraints;

    drop table STM_ETERRIT cascade constraints;

    drop table STM_FONDO cascade constraints;

    drop table STM_GCACAR cascade constraints;

    drop table STM_GRPCARTO cascade constraints;

    drop table STM_GRPTER cascade constraints;

    drop table STM_LOG cascade constraints;

    drop table STM_PARAMAPP cascade constraints;

    drop table STM_PARAMSER cascade constraints;

    drop table STM_PARAMTTA cascade constraints;

    drop table STM_ROLES cascade constraints;

    drop table STM_ROLGCA cascade constraints;

    drop table STM_ROLTAR cascade constraints;

    drop table STM_SERVICIO cascade constraints;

    drop table STM_TAREA cascade constraints;

    drop table STM_TAREA_UI cascade constraints;

    drop table STM_TIPOGRP cascade constraints;

    drop table STM_TIPOTAREA cascade constraints;

    drop table STM_USUARIO cascade constraints;

    drop table STM_USUCONF cascade constraints;

    create table GTA_CODIGO (
        GTA_CODIGO number(11,0) not null,
        GTA_NOMBRE varchar2(80 char),
        primary key (GTA_CODIGO)
    );

    create table STM_APPARB (
        APA_CODAPP number(11,0) not null,
        APA_CODARB number(11,0) not null,
        primary key (APA_CODAPP, APA_CODARB)
    );

    create table STM_APPFON (
        APF_CODIGO number(11,0) not null,
        APF_ORDEN number(6,0),
        APF_CODAPP number(11,0),
        APF_CODFON number(11,0),
        primary key (APF_CODIGO)
    );

    create table STM_APPROL (
        APR_CODAPP number(11,0) not null,
        APR_CODROL number(11,0) not null,
        primary key (APR_CODAPP, APR_CODROL)
    );

    create table STM_APPS (
        APP_CODIGO number(11,0) not null,
        APP_F_ALTA timestamp,
        APP_NOMBRE varchar2(80 char),
        APP_PROJECT varchar2(250 char),
        APP_ESCALAS varchar2(250 char),
        APP_TEMA varchar2(30 char),
        APP_TITULO varchar2(250 char),
        APP_AUTOREFR number(1,0),
        APP_TIPO varchar2(250 char),
        APP_CODGCA number(11,0),
        primary key (APP_CODIGO)
    );

    create table STM_ARBOL (
        ARB_CODIGO number(11,0) not null,
        ARB_NOMBRE varchar2(100 char),
        primary key (ARB_CODIGO)
    );

    create table STM_ARBOLNOD (
        ANR_CODIGO number(11,0) not null,
        ARN_ACTIVO number(1,0),
        ARN_NOMBRE varchar2(80 char),
        ARN_ORDEN number(6,0),
        ARN_TOOLTIP varchar2(100 char),
        ARN_CODCAR number(11,0),
        ARN_CODPADRE number(11,0),
        ARN_CODARB number(11,0),
        primary key (ANR_CODIGO)
    );

    create table STM_ARBROL (
        ARR_CODARB number(11,0) not null,
        ARR_CORROL number(11,0) not null,
        primary key (ARR_CODARB, ARR_CORROL)
    );

    create table STM_CARGO (
        CGO_CODIGO number(11,0) not null,
        CGO_F_ALTA timestamp,
        CGO_F_CADUC timestamp,
        CGO_CORREO varchar2(250 char),
        CGO_CARGO varchar2(250 char),
        CGO_ORG varchar2(250 char),
        CGO_CODTER number(11,0),
        CGO_CODUSU number(11,0),
        primary key (CGO_CODIGO)
    );

    create table STM_CARTO (
        CAR_CODIGO number(11,0) not null,
        CAR_F_ALTA timestamp,
        CAR_EDITABLE number(1,0),
        CAR_TIPOGEOM varchar2(255 char),
        CAR_CAPAS varchar2(500 char),
        CAR_LEYENDTIP varchar2(50 char),
        CAR_LEYENDURL varchar2(250 char),
        CAR_ESC_MAX number(11,0),
        CAR_METAURL varchar2(255 char),
        CAR_ESC_MIN number(11,0),
        CAR_NOMBRE varchar2(100 char),
        CAR_ORDEN number(11,0),
        CAR_QUERYACT number(1,0),
        CAR_QUERYLAY varchar2(500 char),
        CAR_QUERYABL number(1,0),
        CAR_SELECTABL number(1,0),
        CAR_SELECTLAY varchar2(500 char),
        CAR_TEMATIZABLE number(1,0),
        CAR_TRANSP number(11,0),
        CAR_TIPO varchar2(30 char),
        CAR_VISIBLE number(1,0),
        CAR_CODCON number(11,0),
        CAR_CODSERSEL number(11,0),
        CAR_CODSER number(11,0),
        primary key (CAR_CODIGO)
    );

    create table STM_CODIGOS (
        GEN_CODIGO varchar2(255 char) not null,
        GEN_VALOR number(19,0),
        primary key (GEN_CODIGO)
    );

    create table STM_CONEXION (
        CON_CODIGO number(11,0) not null,
        CON_CONSTRING varchar2(250 char),
        CON_NOMBRE varchar2(80 char),
        CON_PASSWORD varchar2(50 char),
        CON_DRIVER varchar2(50 char),
        CON_USUARIO varchar2(50 char),
        primary key (CON_CODIGO)
    );

    create table STM_DISPCARTO (
        DCA_CODIGO number(11,0) not null,
        DCA_F_ALTA timestamp,
        DCA_CODCAR number(11,0),
        DCA_CODTER number(11,0),
        primary key (DCA_CODIGO)
    );

    create table STM_DISPTAREA (
        DTA_CODIGO number(11,0) not null,
        DTA_F_ALTA timestamp,
        DTA_CODTAR number(11,0),
        DTA_CODTER number(11,0),
        primary key (DTA_CODIGO)
    );

    create table STM_ETERRIT (
        TER_CODIGO number(11,0) not null,
        TER_DIRECC varchar2(250 char),
        TER_BLOQ number(1,0),
        TER_OBSERV varchar2(250 char),
        TER_F_ALTA timestamp,
        TER_CORREO varchar2(250 char),
        TER_EXT varchar2(250 char),
        TER_LOGO varchar2(250 char),
        TER_NOMBRE varchar2(250 char) not null,
        TER_NADMIN varchar2(250 char),
        TER_AMBITO varchar2(250 char),
        TER_CODTGR number(11,0),
        primary key (TER_CODIGO)
    );

    create table STM_FONDO (
        FON_CODIGO number(11,0) not null,
        FON_ACTIVO number(1,0),
        FON_F_ALTA timestamp,
        FON_DESC varchar2(250 char),
        FON_NOMBRE varchar2(30 char),
        FON_CODGCA number(11,0),
        primary key (FON_CODIGO)
    );

    create table STM_GCACAR (
        GCC_CODGCA number(11,0) not null,
        GCC_CODCAR number(11,0) not null,
        primary key (GCC_CODGCA, GCC_CODCAR)
    );

    create table STM_GRPCARTO (
        GCA_CODIGO number(11,0) not null,
        GCA_NOMBRE varchar2(80 char),
        GCA_TIPO varchar2(30 char),
        primary key (GCA_CODIGO)
    );

    create table STM_GRPTER (
        GRT_CODTER number(11,0) not null,
        GRT_CODTERM number(11,0) not null,
        primary key (GRT_CODTERM, GRT_CODTER)
    );

    create table STM_LOG (
        LOG_CODIGO number(11,0) not null,
        LOG_CODAPP number(11,0),
        LOG_CODTAR number(11,0),
        LOG_CODTER number(11,0),
        LOG_CODUSU number(11,0),
        LOG_CONT number(11,0),
        LOG_FECHA timestamp,
        LOG_TIPO varchar2(50 char),
        primary key (LOG_CODIGO)
    );

    create table STM_PARAMAPP (
        PAP_CODIGO number(11,0) not null,
        PAP_NOMBRE varchar2(30 char),
        PAP_TIPO varchar2(250 char),
        PAP_VALOR varchar2(250 char),
        PAP_CODAPP number(11,0),
        primary key (PAP_CODIGO)
    );

    create table STM_PARAMSER (
        PSE_CODIGO number(11,0) not null,
        PSE_NOMBRE varchar2(30 char),
        PSE_TIPO varchar2(250 char),
        PSE_VALOR varchar2(250 char),
        PSE_CODSER number(11,0),
        primary key (PSE_CODIGO)
    );

    create table STM_PARAMTTA (
        PTT_CODIGO number(11,0) not null,
        PTT_NOMBRE varchar2(50 char),
        PTT_ORDEN number(6,0),
        PTT_TIPO varchar2(30 char),
        PTT_VALOR varchar2(512 char),
        PTT_CODTAR number(11,0),
        primary key (PTT_CODIGO)
    );

    create table STM_ROLES (
        ROL_CODIGO number(11,0) not null,
        ROL_OBSERV varchar2(500 char),
        ROL_NOMBRE varchar2(250 char) not null,
        primary key (ROL_CODIGO)
    );

    create table STM_ROLGCA (
        RGC_CODROL number(11,0) not null,
        RGC_CODGCA number(11,0) not null,
        primary key (RGC_CODROL, RGC_CODGCA)
    );

    create table STM_ROLTAR (
        RTA_CODROL number(11,0) not null,
        RTA_CODTAR number(11,0) not null,
        primary key (RTA_CODROL, RTA_CODTAR)
    );

    create table STM_SERVICIO (
        SER_CODIGO number(11,0) not null,
        SER_F_ALTA timestamp,
        SER_INFOURL varchar2(250 char),
        SER_LEYENDA varchar2(250 char),
        SER_NOMBRE varchar2(30 char),
        SER_PROJECTS varchar2(1000 char),
        SER_TIPO varchar2(30 char),
        SER_URL varchar2(250 char),
        SER_CODCON number(11,0),
        primary key (SER_CODIGO)
    );

    create table STM_TAREA (
        TAR_CODIGO number(11,0) not null,
        TAR_F_ALTA timestamp,
        TAR_NOMBRE varchar2(250 char),
        TAR_ORDEN number(6,0),
        TAR_CODCON number(11,0),
        TAR_CODGTA number(11,0),
        TAR_CODTTA number(11,0),
        TAR_CODTUI number(11,0),
        primary key (TAR_CODIGO)
    );

    create table STM_TAREA_UI (
        TUI_CODIGO number(11,0) not null,
        TUI_NOMBRE varchar2(30 char),
        TUI_ORDEN number(6,0),
        TUI_TOOLTIP varchar2(100 char),
        TUI_TIPO varchar2(30 char),
        primary key (TUI_CODIGO)
    );

    create table STM_TIPOGRP (
        TGR_CODIGO number(11,0) not null,
        TGR_NOMBRE varchar2(250 char) not null,
        primary key (TGR_CODIGO)
    );

    create table STM_TIPOTAREA (
        TTA_CODIGO number(11,0) not null,
        TTA_NOMBRE varchar2(30 char),
        primary key (TTA_CODIGO)
    );

    create table STM_USUARIO (
        USU_CODIGO number(11,0) not null,
        USU_ADM number(1,0),
        USU_BLOQ number(1,0),
        USU_NOMBRE varchar2(30 char),
        USU_APELLIDOS varchar2(40 char),
        USU_PASSWORD varchar2(128 char),
        USU_USUARIO varchar2(30 char) not null,
        primary key (USU_CODIGO)
    );

    create table STM_USUCONF (
        UCF_CODIGO number(11,0) not null,
        UCF_CODROL number(11,0),
        UCF_CODTER number(11,0),
        UCF_CODUSU number(11,0),
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
