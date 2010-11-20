package org.hyperic.hq.web;

import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.hyperic.hq.inventory.domain.Config;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@RooWebScaffold(path = "configs", formBackingObject = Config.class)
@RequestMapping("/configs")
@Controller
public class ConfigController {
}
