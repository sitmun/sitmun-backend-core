--liquibase formatted sql
--changeset sitmun:2

-- STM_TERRITORY.TER_SCOPE
INSERT INTO STM_CODELIST
VALUES (1, 'territory.scope', 'M', false, 'Municipality');
INSERT INTO STM_CODELIST
VALUES (2, 'territory.scope', 'R', false, 'Regional');
INSERT INTO STM_CODELIST
VALUES (3, 'territory.scope', 'T', false, 'Total');

-- STM_USER.TER_IDENTTYPE
INSERT INTO STM_CODELIST
VALUES (4, 'user.identificationType', 'DNI', false, 'DNI');
INSERT INTO STM_CODELIST
VALUES (5, 'user.identificationType', 'NIE', false, 'NIE');
INSERT INTO STM_CODELIST
VALUES (6, 'user.identificationType', 'PAS', false, 'Passport');

-- STM_GEOINFO.GEO_LEGENDTIP
INSERT INTO STM_CODELIST
VALUES (7, 'cartography.legendType', 'LINK', false, 'Static link');
INSERT INTO STM_CODELIST
VALUES (8, 'cartography.legendType', 'LEGENDGRAPHIC', false, 'Get Legend Graphic');
INSERT INTO STM_CODELIST
VALUES (9, 'cartography.legendType', 'CAPABILITIES', false, 'GetCapabilities');

-- STM_GEOINFO.GEO_GEOMTYPE
INSERT INTO STM_CODELIST
VALUES (10, 'cartography.geometryType', 'POINT', false, 'Point');
INSERT INTO STM_CODELIST
VALUES (11, 'cartography.geometryType', 'LINE', false, 'Line');
INSERT INTO STM_CODELIST
VALUES (12, 'cartography.geometryType', 'POLYGON', false, 'Polygon');

-- STM_FIL_GI.FGI_TYPE
INSERT INTO STM_CODELIST
VALUES (13, 'cartographyFilter.type', 'C', false, 'Custom');
INSERT INTO STM_CODELIST
VALUES (14, 'cartographyFilter.type', 'D', false, 'Defined');

-- STM_FIL_GI.FGI_VALUETYPE
INSERT INTO STM_CODELIST
VALUES (15, 'cartographyFilter.valueType', 'A', false, 'Alphanumeric');
INSERT INTO STM_CODELIST
VALUES (16, 'cartographyFilter.valueType', 'N', false, 'Number');
INSERT INTO STM_CODELIST
VALUES (17, 'cartographyFilter.valueType', 'D', false, 'Date');

-- STM_PAR_GI.PGI_FORMAT
-- TODO Validate value and description of code list cartographyParameter.format
INSERT INTO STM_CODELIST
VALUES (18, 'cartographyParameter.format', 'I', false, 'Image');
INSERT INTO STM_CODELIST
VALUES (19, 'cartographyParameter.format', 'N', false, 'Number');
INSERT INTO STM_CODELIST
VALUES (20, 'cartographyParameter.format', 'P', false, 'Percentage');
INSERT INTO STM_CODELIST
VALUES (21, 'cartographyParameter.format', 'T', false, 'Text');
INSERT INTO STM_CODELIST
VALUES (22, 'cartographyParameter.format', 'U', false, 'URL');
INSERT INTO STM_CODELIST
VALUES (23, 'cartographyParameter.format', 'F', false, 'Date');

-- STM_PAR_GI.PGI_TYPE
-- TODO Validate description of code list cartographyParameter.type
INSERT INTO STM_CODELIST
VALUES (24, 'cartographyParameter.type', 'INFO', false, 'INFO');
INSERT INTO STM_CODELIST
VALUES (25, 'cartographyParameter.type', 'SELECT', false, 'SELECT');
INSERT INTO STM_CODELIST
VALUES (26, 'cartographyParameter.type', 'INFOSELECT', false, 'INFOSELECT');
INSERT INTO STM_CODELIST
VALUES (27, 'cartographyParameter.type', 'FILTRO_INFO', false, 'FILTRO_INFO');
INSERT INTO STM_CODELIST
VALUES (28, 'cartographyParameter.type', 'FILTRO_ESPACIAL', false, 'FILTRO_SPACIAL');
INSERT INTO STM_CODELIST
VALUES (29, 'cartographyParameter.type', 'FILTRO', false, 'FILTRO');
INSERT INTO STM_CODELIST
VALUES (30, 'cartographyParameter.type', 'EDIT', false, 'EDIT');

-- STM_GRP_GI.GGI_TYPE
-- TODO Validate value and description of code list cartographyPermission.type
INSERT INTO STM_CODELIST
VALUES (31, 'cartographyPermission.type', 'C', true, 'Cartography group');
INSERT INTO STM_CODELIST
VALUES (32, 'cartographyPermission.type', 'F', true, 'Background map');
INSERT INTO STM_CODELIST
VALUES (33, 'cartographyPermission.type', 'M', true, 'Location map');
INSERT INTO STM_CODELIST
VALUES (34, 'cartographyPermission.type', 'I', true, 'Report');

-- STM_PAR_APP.PAP_TYPE
-- TODO Validate value and description of code list applicationParameter.type
INSERT INTO STM_CODELIST
VALUES (35, 'applicationParameter.type', 'MOBILE', false, 'Mobile app parameter');
INSERT INTO STM_CODELIST
VALUES (36, 'applicationParameter.type', 'Nomenclator', false, 'Nomenclator parameter');
INSERT INTO STM_CODELIST
VALUES (37, 'applicationParameter.type', 'PRINT_TEMPLATE', false, 'Print template');

-- STM_SERVICE.SER_PROTOCOL
-- TODO Validate value and description of code list service.type
INSERT INTO STM_CODELIST
VALUES (38, 'service.type', 'AIMS', false, 'AIMS');
INSERT INTO STM_CODELIST
VALUES (39, 'service.type', 'FME', false, 'FME');
INSERT INTO STM_CODELIST
VALUES (40, 'service.type', 'TC', false, 'TC');
INSERT INTO STM_CODELIST
VALUES (41, 'service.type', 'WFS', false, 'WFS');
INSERT INTO STM_CODELIST
VALUES (42, 'service.type', 'WMS', false, 'WMS');

-- STM_SERVICE.SER_NAT_PROT
-- TODO Provide value and description for code list service.nativeProtocol

-- STM_PAR_SER.PSE_TYPE
INSERT INTO STM_CODELIST
VALUES (43, 'serviceParameter.type', 'INFO', false, 'GetFeatureInfo');
INSERT INTO STM_CODELIST
VALUES (44, 'serviceParameter.type', 'WMS', false, 'GetMap');
INSERT INTO STM_CODELIST
VALUES (45, 'serviceParameter.type', 'OLPARAM', false, 'OpenLayers');

-- STM_PAR_TSK.PTT_TYPE
-- TODO Validate description of code list taskParameter.type
INSERT INTO STM_CODELIST
VALUES (46, 'taskParameter.type', 'CAMPO', false, 'CAMPO');
INSERT INTO STM_CODELIST
VALUES (47, 'taskParameter.type', 'CAPA', false, 'CAPA');
INSERT INTO STM_CODELIST
VALUES (48, 'taskParameter.type', 'EDIT', false, 'EDIT');
INSERT INTO STM_CODELIST
VALUES (49, 'taskParameter.type', 'FILTRO', false, 'FILTRO');
INSERT INTO STM_CODELIST
VALUES (50, 'taskParameter.type', 'FME', false, 'FME');
INSERT INTO STM_CODELIST
VALUES (51, 'taskParameter.type', 'GEOM', false, 'GEOM');
INSERT INTO STM_CODELIST
VALUES (52, 'taskParameter.type', 'LABEL', false, 'LABEL');
INSERT INTO STM_CODELIST
VALUES (53, 'taskParameter.type', 'RELM', false, 'RELM');
INSERT INTO STM_CODELIST
VALUES (54, 'taskParameter.type', 'RELS', false, 'RELS');
INSERT INTO STM_CODELIST
VALUES (55, 'taskParameter.type', 'SQL', false, 'SQL');
INSERT INTO STM_CODELIST
VALUES (56, 'taskParameter.type', 'TIPO', false, 'TIPO');
INSERT INTO STM_CODELIST
VALUES (57, 'taskParameter.type', 'VISTA', false, 'VISTA');
INSERT INTO STM_CODELIST
VALUES (87, 'taskParameter.type', 'DATAINPUT', false, 'DATAINPUT');
INSERT INTO STM_CODELIST
VALUES (88, 'taskParameter.type', 'VALOR', false, 'VALOR');

-- STM_PAR_TSK.PTT_FORMAT
-- TODO Validate description of code list taskParameter.format
INSERT INTO STM_CODELIST
VALUES (58, 'taskParameter.format', 'T', false, 'Text');
INSERT INTO STM_CODELIST
VALUES (59, 'taskParameter.format', 'F', false, 'Date');
INSERT INTO STM_CODELIST
VALUES (60, 'taskParameter.format', 'N', false, 'Number');
INSERT INTO STM_CODELIST
VALUES (61, 'taskParameter.format', 'L', false, 'List (from a query)');
INSERT INTO STM_CODELIST
VALUES (62, 'taskParameter.format', 'U', false, 'URL');
INSERT INTO STM_CODELIST
VALUES (63, 'taskParameter.format', 'I', false, 'Image');
INSERT INTO STM_CODELIST
VALUES (64, 'taskParameter.format', 'C', false, 'Email');
INSERT INTO STM_CODELIST
VALUES (65, 'taskParameter.format', 'R', false, 'Relation attribute between tables');
INSERT INTO STM_CODELIST
VALUES (66, 'taskParameter.format', 'S', false, 'Select for assigning a value');
INSERT INTO STM_CODELIST
VALUES (67, 'taskParameter.format', 'B', false, 'Database (trigger)');

-- STM_DOWNLOAD.DOW_EXT
-- TODO Provide value and description for code list downloadTask.format

-- STM_DOWNLOAD.DOW_TYPE
INSERT INTO STM_CODELIST
VALUES (68, 'downloadTask.scope', 'U', false, 'Isolated');
INSERT INTO STM_CODELIST
VALUES (69, 'downloadTask.scope', 'A', false, 'Application');
INSERT INTO STM_CODELIST
VALUES (70, 'downloadTask.scope', 'C', false, 'Layer');

-- STM_QUERY.QUE_TYPE
-- TODO Validate description of code list queryTask.scope
INSERT INTO STM_CODELIST
VALUES (71, 'queryTask.scope', 'URL', false, 'URL');
INSERT INTO STM_CODELIST
VALUES (72, 'queryTask.scope', 'SQL', false, 'SQL');
INSERT INTO STM_CODELIST
VALUES (73, 'queryTask.scope', 'WS', false, 'WS');
INSERT INTO STM_CODELIST
VALUES (74, 'queryTask.scope', 'INFORME', false, 'INFORME');
INSERT INTO STM_CODELIST
VALUES (75, 'queryTask.scope', 'TAREA', false, 'TAREA');

-- STM_POST.POS_TYPE
-- TODO Validate description of code list userPosition.type
INSERT INTO STM_CODELIST
VALUES (76, 'userPosition.type', 'RE', false, 'RE');

-- STM_THEMATIC.THE_RANKTYPE
INSERT INTO STM_CODELIST
VALUES (77, 'thematicMap.type', 'VU', false, 'Unique values');
INSERT INTO STM_CODELIST
VALUES (78, 'thematicMap.type', 'RE', false, 'Equal record count');
INSERT INTO STM_CODELIST
VALUES (79, 'thematicMap.type', 'RL', false, 'Equal interval size');

-- STM_THEMATIC.THE_VALUETYPE
INSERT INTO STM_CODELIST
VALUES (80, 'thematicMap.valueType', 'STR', false, 'String');
INSERT INTO STM_CODELIST
VALUES (81, 'thematicMap.valueType', 'DOU', false, 'Double');

-- STM_THEMATIC.THE_DESTINATION
INSERT INTO STM_CODELIST
VALUES (82, 'thematicMap.destination', 'WS', false, 'Web Service');
INSERT INTO STM_CODELIST
VALUES (83, 'thematicMap.destination', 'WS_HERMES', false, 'Hermes Web Service');
INSERT INTO STM_CODELIST
VALUES (84, 'thematicMap.destination', 'UPLOADED', false, 'Uploaded JSON file');

-- STM_THE_RANK.TRK_STYLEINT
-- STM_THE_RANK.TRK_STYLE
-- TODO Provide value and description for code list thematicMapRange.style

-- STM_APPLICATION.APP_TYPE
INSERT INTO STM_CODELIST
VALUES (85, 'application.type', 'I', false, 'Internal');
INSERT INTO STM_CODELIST
VALUES (86, 'application.type', 'E', false, 'External');

INSERT INTO STM_SEQUENCE(SEQ_NAME, SEQ_COUNT)
VALUES ('COD_ID', 88);