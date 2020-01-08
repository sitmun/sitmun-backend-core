package org.sitmun.plugin.core.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.webjars.RequireJS;

@Controller
@RequestMapping("app")
public class CoreJSController {
  @ResponseBody
  @RequestMapping(value = "config", produces = "application/javascript")
  public String webjarjs() {
    return RequireJS.getSetupJavaScript("/webjars/");
  }
}
