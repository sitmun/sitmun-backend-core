package org.sitmun.administration.service.template.childdata;

public interface TemplateChildDataPort {

  ChildDataResult executeSql(ChildDataRequest request);

  ChildDataResult executeApi(ChildDataRequest request);

  ChildDataResult resolveDirect(ChildDataRequest request);
}
