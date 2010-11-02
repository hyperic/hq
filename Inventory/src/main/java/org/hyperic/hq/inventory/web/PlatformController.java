package org.hyperic.hq.inventory.web;

import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.hyperic.hq.inventory.domain.Platform;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@RooWebScaffold(path = "platforms", formBackingObject = Platform.class)
@RequestMapping("/platforms")
@Controller
public class PlatformController {
}
