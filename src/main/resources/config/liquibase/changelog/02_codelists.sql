--liquibase formatted sql
--changeset sitmun:2

-- STM_TERRITORY.TER_SCOPE
INSERT INTO STM_CODELIST
VALUES (1, 'territory.scope', 'M', 'Municipality');
INSERT INTO STM_CODELIST
VALUES (2, 'territory.scope', 'R', 'Regional');
INSERT INTO STM_CODELIST
VALUES (3, 'territory.scope', 'T', 'Total');

-- STM_USER.TER_IDENTTYPE
INSERT INTO STM_CODELIST
VALUES (4, 'user.identificationType', 'DNI', 'DNI');
INSERT INTO STM_CODELIST
VALUES (5, 'user.identificationType', 'NIE', 'NIE');
INSERT INTO STM_CODELIST
VALUES (6, 'user.identificationType', 'PAS', 'Passport');

-- STM_GEOINFO.GEO_LEGENDTIP
INSERT INTO STM_CODELIST
VALUES (7, 'cartography.legendType', 'LINK', 'Static link');
INSERT INTO STM_CODELIST
VALUES (8, 'cartography.legendType', 'LEGENDGRAPHIC', 'Get Legend Graphic');
INSERT INTO STM_CODELIST
VALUES (9, 'cartography.legendType', 'CAPABILITIES', 'GetCapabilities');

-- STM_GEOINFO.GEO_GEOMTYPE
INSERT INTO STM_CODELIST
VALUES (10, 'cartography.geometryType', 'POINT', 'Point');
INSERT INTO STM_CODELIST
VALUES (11, 'cartography.geometryType', 'LINE', 'Line');
INSERT INTO STM_CODELIST
VALUES (12, 'cartography.geometryType', 'POLYGON', 'Polygon');

-- STM_FIL_GI.FGI_TYPE
INSERT INTO STM_CODELIST
VALUES (13, 'cartographyFilter.type', 'C', 'Custom');
INSERT INTO STM_CODELIST
VALUES (14, 'cartographyFilter.type', 'D', 'Defined');

-- STM_FIL_GI.FGI_VALUETYPE
INSERT INTO STM_CODELIST
VALUES (15, 'cartographyFilter.valueType', 'A', 'Alphanumeric');
INSERT INTO STM_CODELIST
VALUES (16, 'cartographyFilter.valueType', 'N', 'Numeric');
INSERT INTO STM_CODELIST
VALUES (17, 'cartographyFilter.valueType', 'D', 'Date');

-- STM_PAR_GI.PGI_FORMAT
-- TODO Validate value and description of code list cartographyParameter.format
INSERT INTO STM_CODELIST
VALUES (18, 'cartographyParameter.format', 'Imagen', 'Imagen');
INSERT INTO STM_CODELIST
VALUES (19, 'cartographyParameter.format', 'número', 'número');
INSERT INTO STM_CODELIST
VALUES (20, 'cartographyParameter.format', 'porcentaje', 'porcentaje');
INSERT INTO STM_CODELIST
VALUES (21, 'cartographyParameter.format', 'texto', 'texto');
INSERT INTO STM_CODELIST
VALUES (22, 'cartographyParameter.format', 'URL', 'URL');
INSERT INTO STM_CODELIST
VALUES (23, 'cartographyParameter.format', 'fecha', 'fecha');

-- STM_PAR_GI.PGI_TYPE
-- TODO Validate description of code list cartographyParameter.type
INSERT INTO STM_CODELIST
VALUES (24, 'cartographyParameter.type', 'INFO', 'INFO');
INSERT INTO STM_CODELIST
VALUES (25, 'cartographyParameter.type', 'SELECT', 'SELECT');
INSERT INTO STM_CODELIST
VALUES (26, 'cartographyParameter.type', 'INFOSELECT', 'INFOSELECT');
INSERT INTO STM_CODELIST
VALUES (27, 'cartographyParameter.type', 'FILTRO_INFO', 'FILTRO_INFO');
INSERT INTO STM_CODELIST
VALUES (28, 'cartographyParameter.type', 'FILTRO_ESPACIAL', 'FILTRO_SPACIAL');
INSERT INTO STM_CODELIST
VALUES (29, 'cartographyParameter.type', 'FILTRO', 'FILTRO');
INSERT INTO STM_CODELIST
VALUES (30, 'cartographyParameter.type', 'EDIT', 'EDIT');

-- STM_GRP_GI.GGI_TYPE
-- TODO Validate value and description of code list cartographyPermission.type
INSERT INTO STM_CODELIST
VALUES (31, 'cartographyPermission.type', 'C', 'Cartography group');
INSERT INTO STM_CODELIST
VALUES (32, 'cartographyPermission.type', 'F', 'Background map');
INSERT INTO STM_CODELIST
VALUES (33, 'cartographyPermission.type', 'M', 'Location map');
INSERT INTO STM_CODELIST
VALUES (34, 'cartographyPermission.type', 'I', 'Report');

-- STM_PAR_APP.PAP_TYPE
-- TODO Validate value and description of code list applicationParameter.type
INSERT INTO STM_CODELIST
VALUES (35, 'applicationParameter.type', 'MOBILE', 'Mobile app parameter');
INSERT INTO STM_CODELIST
VALUES (36, 'applicationParameter.type', 'Nomenclator', 'Nomenclator parameter');
INSERT INTO STM_CODELIST
VALUES (37, 'applicationParameter.type', 'PRINT_TEMPLATE', 'Print template');

-- STM_SERVICE.SER_PROTOCOL
-- TODO Validate value and description of code list service.type
INSERT INTO STM_CODELIST
VALUES (38, 'service.type', 'AIMS', 'AIMS');
INSERT INTO STM_CODELIST
VALUES (39, 'service.type', 'FME', 'FME');
INSERT INTO STM_CODELIST
VALUES (40, 'service.type', 'TC', 'TC');
INSERT INTO STM_CODELIST
VALUES (41, 'service.type', 'WFS', 'WFS');
INSERT INTO STM_CODELIST
VALUES (42, 'service.type', 'WMS', 'WMS');

-- STM_SERVICE.SER_NAT_PROT
-- TODO Provide value and description for code list service.nativeProtocol

-- STM_PAR_SER.PSE_TYPE
INSERT INTO STM_CODELIST
VALUES (43, 'serviceParameter.type', 'INFO', 'GetFeatureInfo');
INSERT INTO STM_CODELIST
VALUES (44, 'serviceParameter.type', 'WMS', 'GetMap');
INSERT INTO STM_CODELIST
VALUES (45, 'serviceParameter.type', 'OLPARAM', 'OpenLayers');

-- STM_PAR_TSK.PTT_TYPE
-- TODO Validate description of code list taskParameter.type
INSERT INTO STM_CODELIST
VALUES (46, 'taskParameter.type', 'CAMPO', 'CAMPO');
INSERT INTO STM_CODELIST
VALUES (47, 'taskParameter.type', 'CAPA', 'CAPA');
INSERT INTO STM_CODELIST
VALUES (48, 'taskParameter.type', 'EDIT', 'EDIT');
INSERT INTO STM_CODELIST
VALUES (49, 'taskParameter.type', 'FILTRO', 'FILTRO');
INSERT INTO STM_CODELIST
VALUES (50, 'taskParameter.type', 'FME', 'FME');
INSERT INTO STM_CODELIST
VALUES (51, 'taskParameter.type', 'GEOM', 'GEOM');
INSERT INTO STM_CODELIST
VALUES (52, 'taskParameter.type', 'LABEL', 'LABEL');
INSERT INTO STM_CODELIST
VALUES (53, 'taskParameter.type', 'RELM', 'RELM');
INSERT INTO STM_CODELIST
VALUES (54, 'taskParameter.type', 'RELS', 'RELS');
INSERT INTO STM_CODELIST
VALUES (55, 'taskParameter.type', 'SQL', 'SQL');
INSERT INTO STM_CODELIST
VALUES (56, 'taskParameter.type', 'TIPO', 'TIPO');
INSERT INTO STM_CODELIST
VALUES (57, 'taskParameter.type', 'VISTA', 'VISTA');

-- STM_PAR_TSK.PTT_FORMAT
-- TODO Validate description of code list taskParameter.format
INSERT INTO STM_CODELIST
VALUES (58, 'taskParameter.format', 'T', 'Text');
INSERT INTO STM_CODELIST
VALUES (59, 'taskParameter.format', 'F', 'Date');
INSERT INTO STM_CODELIST
VALUES (60, 'taskParameter.format', 'N', 'Number');
INSERT INTO STM_CODELIST
VALUES (61, 'taskParameter.format', 'L', 'List (from a query)');
INSERT INTO STM_CODELIST
VALUES (62, 'taskParameter.format', 'U', 'URL');
INSERT INTO STM_CODELIST
VALUES (63, 'taskParameter.format', 'I', 'Image');
INSERT INTO STM_CODELIST
VALUES (64, 'taskParameter.format', 'C', 'Email');
INSERT INTO STM_CODELIST
VALUES (65, 'taskParameter.format', 'R', 'Relation attribute between tables');
INSERT INTO STM_CODELIST
VALUES (66, 'taskParameter.format', 'S', 'Select for assigning a value');
INSERT INTO STM_CODELIST
VALUES (67, 'taskParameter.format', 'B', 'Database (trigger)');

-- STM_DOWNLOAD.DOW_EXT
-- TODO Provide value and description for code list downloadTask.format

-- STM_DOWNLOAD.DOW_TYPE
INSERT INTO STM_CODELIST
VALUES (68, 'downloadTask.scope', 'U', 'Isolated');
INSERT INTO STM_CODELIST
VALUES (69, 'downloadTask.scope', 'A', 'Application');
INSERT INTO STM_CODELIST
VALUES (70, 'downloadTask.scope', 'C', 'Layer');

-- STM_QUERY.QUE_TYPE
-- TODO Validate description of code list queryTask.scope
INSERT INTO STM_CODELIST
VALUES (71, 'queryTask.scope', 'URL', 'URL');
INSERT INTO STM_CODELIST
VALUES (72, 'queryTask.scope', 'SQL', 'SQL');
INSERT INTO STM_CODELIST
VALUES (73, 'queryTask.scope', 'WS', 'WS');
INSERT INTO STM_CODELIST
VALUES (74, 'queryTask.scope', 'INFORME', 'INFORME');
INSERT INTO STM_CODELIST
VALUES (75, 'queryTask.scope', 'TAREA', 'TAREA');

-- STM_POST.POS_TYPE
-- TODO Validate description of code list userPosition.type
INSERT INTO STM_CODELIST
VALUES (76, 'userPosition.type', 'RE', 'RE');

-- STM_THEMATIC.THE_RANKTYPE
INSERT INTO STM_CODELIST
VALUES (77, 'thematicMap.type', 'VU', 'Unique values');
INSERT INTO STM_CODELIST
VALUES (78, 'thematicMap.type', 'RE', 'Equal record count');
INSERT INTO STM_CODELIST
VALUES (79, 'thematicMap.type', 'RL', 'Equal interval size');

-- STM_THEMATIC.THE_VALUETYPE
INSERT INTO STM_CODELIST
VALUES (80, 'thematicMap.valueType', 'STR', 'String');
INSERT INTO STM_CODELIST
VALUES (81, 'thematicMap.valueType', 'DOU', 'Double');

-- STM_THEMATIC.THE_DESTINATION
INSERT INTO STM_CODELIST
VALUES (82, 'thematicMap.destination', 'WS', 'Web Service');
INSERT INTO STM_CODELIST
VALUES (83, 'thematicMap.destination', 'WS_HERMES', 'Hermes Web Service');
INSERT INTO STM_CODELIST
VALUES (84, 'thematicMap.destination', 'UPLOADED', 'Uploaded JSON file');

-- STM_THE_RANK.TRK_STYLEINT
-- STM_THE_RANK.TRK_STYLE
-- TODO Provide value and description for code list thematicMapRange.style

-- STM_APPLICATION.APP_TYPE
INSERT INTO STM_CODELIST
VALUES (85, 'application.type', 'I', 'Internal');
INSERT INTO STM_CODELIST
VALUES (86, 'application.type', 'E', 'External');

INSERT INTO STM_SEQUENCE(SEQ_NAME, SEQ_COUNT)
VALUES ('COD_ID', 86);