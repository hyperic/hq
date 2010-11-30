package org.hyperic.hq.web.inventory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

@Controller
@RequestMapping("/inventory")
public class PlatformController {
//	private static final String BASE_INTERNAL_PATH = "inventory/platform";
//	private static final String BASE_URL_PATH = "/app/" + BASE_INTERNAL_PATH;
//	
//	public PlatformController() {
//	}
//	
//	@RequestMapping(method = RequestMethod.GET, value = "/platform/driver")
//	public String getDriver() {
//		return BASE_INTERNAL_PATH + "/driver";
//	}
//	
//	// Browser-based Requests
//	@RequestMapping(method = RequestMethod.GET, value = "/platform/create")
//	public String getPlatformForCreate(Model model) {
//		model.addAttribute("platform", new PlatformNG());
//		
//		return BASE_INTERNAL_PATH + "/create";
//	}
//
//	@RequestMapping(method = RequestMethod.POST, value = "/platform/create")
//	public String createPlatform(PlatformNG platform, BindingResult result, Model model) {
//		if (result.hasErrors()) {
//			model.addAttribute("platform", platform);
//            return BASE_INTERNAL_PATH + "/new";
//		}
//		
//		PlatformNG p = createPlatform(platform);
//
//		Map<String, String> links = new HashMap<String, String>();
//		
//		links.put("view", BASE_URL_PATH + "/" + p.getId() + "/view");
//		links.put("edit", BASE_URL_PATH + "/" + p.getId() + "/edit");
//		links.put("delete", BASE_URL_PATH + "/delete");
//		links.put("list", BASE_URL_PATH + "/list");
//		model.addAttribute("links", links);
//				
//		return "redirect:" + BASE_URL_PATH + "/" + p.getId() + "/view";
//	}
//
//	@RequestMapping(method = RequestMethod.GET, value = "/platform/{id}/edit")
//	public String getPlatformForEdit(@PathVariable Long id, Model model) {
//		PlatformNG p = getPlatform(id);
//		
//		model.addAttribute("platform", p);
//
//		Map<String, String> links = new HashMap<String, String>();
//		
//		links.put("edit", BASE_URL_PATH + "/edit");
//		links.put("delete", BASE_URL_PATH + "/delete");
//		links.put("list", BASE_URL_PATH + "/list");
//		model.addAttribute("links", links);
//		
//		return BASE_INTERNAL_PATH + "/edit";
//	}
//
//	@RequestMapping(method = RequestMethod.POST, value = "/platform/edit")
//	public String updatePlatform(PlatformNG platform, BindingResult result, Model model) {
//		if (result.hasErrors()) {
//			model.addAttribute("platform", platform);
//            return BASE_INTERNAL_PATH + "/edit";
//		}
//		
//		PlatformNG p = updatePlatform(platform);
//
//		Map<String, String> links = new HashMap<String, String>();
//		
//		links.put("view", BASE_URL_PATH + "/" + p.getId() + "/view");
//		links.put("edit", BASE_URL_PATH + "/" + p.getId() + "/edit");
//		links.put("delete", BASE_URL_PATH + "/delete");
//		links.put("list", BASE_URL_PATH + "/list");
//		model.addAttribute("links", links);
//		
//		return "redirect:" + BASE_URL_PATH + "/" + p.getId() + "/view";
//	}
//
//	@RequestMapping(method = RequestMethod.POST, value = "/platform/delete")
//	public String deletePlatforms(@RequestParam Long[] ids, @RequestParam String redirect) {
//		for (Long id : ids) {
//			// this will be slow, not for production, only prototyping...
//			deletePlatform(id);
//		}
//		
//		return "redirect:" + redirect;
//	}
//
//	@RequestMapping(method = RequestMethod.GET, value = "/platform/{id}/view")
//	public String getPlatform(@PathVariable Long id, Model model) {
//		PlatformNG p = getPlatform(id);
//		
//		model.addAttribute("platform", p);
//		
//		Map<String, String> links = new HashMap<String, String>();
//		
//		links.put("view", BASE_URL_PATH + "/" + p.getId() + "/view");
//		links.put("edit", BASE_URL_PATH + "/" + p.getId() + "/edit");
//		links.put("delete", BASE_URL_PATH + "/delete");
//		links.put("list", BASE_URL_PATH + "/list");
//		model.addAttribute("links", links);
//
//		return BASE_INTERNAL_PATH + "/view";
//	}
//
//	@RequestMapping(method = RequestMethod.GET, value = "/platform/list")
//	public String getPlatforms(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String sort, @RequestParam(required = false) Integer direction, Model model) {
//		List<PlatformNG> platforms = getPlatforms(page, pageSize);
//		long count = (page == null && pageSize == null) ? count = platforms.size() : PlatformNG.countPlatformNGs();
//		
//		model.addAttribute("platforms", platforms);
//		model.addAttribute("page", page);
//		model.addAttribute("pageSize", pageSize);
//		model.addAttribute("count", count);
//		
//		Map<String, String> links = new HashMap<String, String>();
//		
//		if (page != null && page > 1) {
//			links.put("previous", BASE_URL_PATH + "/list?page=" + (page-1) + "&pageSize=" + pageSize);
//		}
//		
//		if (page != null && pageSize != null && count > ((page * pageSize) + pageSize)) {
//			links.put("next", BASE_URL_PATH + "/list?page=" + (page+1) + "&pageSize=" + pageSize);
//		}
//		
//		model.addAttribute("links", links);
//		
//		return BASE_INTERNAL_PATH + "/list";
//	}
//	
//	@RequestMapping(method = RequestMethod.GET, value = "/platform/list/{searchText}")
//	public String getPlatformsByText(@PathVariable String searchText, @RequestParam(required = false) Boolean unavailable, @RequestParam(required = false) Boolean owned, @RequestParam(required = false) Boolean any, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String sort, @RequestParam(required = false) Integer direction, Model model) {
//		List<PlatformNG> platforms = getPlatformsByText(searchText, page, pageSize);
//		long count = (page == null && pageSize == null) ? count = platforms.size() : PlatformNG.countPlatformNGs();
//		
//		model.addAttribute("platforms", platforms);
//		model.addAttribute("page", page);
//		model.addAttribute("pageSize", pageSize);
//		model.addAttribute("count", count);
//		
//		Map<String, String> links = new HashMap<String, String>();
//		
//		if (page != null && page > 1) {
//			links.put("previous", BASE_URL_PATH + "/list/" + searchText +"?page=" + (page-1) + "&pageSize=" + pageSize);
//		}
//		
//		if (page != null && pageSize != null && count > ((page * pageSize) + pageSize)) {
//			links.put("next", BASE_URL_PATH + "/list/" + searchText +"?page=" + (page+1) + "&pageSize=" + pageSize);
//		}
//		
//		model.addAttribute("links", links);
//				
//		return BASE_INTERNAL_PATH + "/list";
//	}
//
//	// API Requests
//	@RequestMapping(method = RequestMethod.POST, value = "/platform")
//	@ResponseStatus(HttpStatus.CREATED)
//	public @ResponseBody PlatformNG createPlatform(@RequestBody PlatformNG platform, HttpServletResponse response) {
//		PlatformNG p = createPlatform(platform);
//
//		response.setHeader("Location", BASE_URL_PATH + "/" + p.getId());
//		
//		return p;
//	}
//
//	@RequestMapping(method = RequestMethod.PUT, value = "/platform/{id}")
//	public @ResponseBody PlatformNG updatePlatform(@PathVariable Long id, @RequestBody PlatformNG platform, HttpServletResponse response) {
//		PlatformNG p = updatePlatform(platform);
//
//		response.setHeader("Location", BASE_URL_PATH + "/" + p.getId());
//		
//		return p;
//	}
//
//	@RequestMapping(method = RequestMethod.GET, value = "/platform/{id}")
//	public @ResponseBody PlatformNG getPlatform(@PathVariable Long id, HttpServletResponse response) {
//		return getPlatform(id);
//	}
//
//	@RequestMapping(method = RequestMethod.DELETE, value = "/platform/{id}")
//	public @ResponseBody void deleteSinglePlatform(@PathVariable Long id) {
//		deletePlatform(id);
//	}
//	
//	@RequestMapping(method = RequestMethod.GET, value = "/platforms")
//	public @ResponseBody List<PlatformNG> getPlatforms(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String sort, @RequestParam(required = false) Integer direction, HttpServletResponse response) {
//		return getPlatforms(page, pageSize);		
//	}
//	
//	@RequestMapping(method = RequestMethod.GET, value = "/platforms/{searchText}")
//	public @ResponseBody List<PlatformNG> getPlatformsByText(@RequestParam(required = false) String searchText, @RequestParam(required = false) Boolean unavailable, @RequestParam(required = false) Boolean owned, @RequestParam(required = false) Boolean any, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String sort, @RequestParam(required = false) Integer direction, HttpServletResponse response) {
//		return getPlatformsByText(searchText, page, pageSize);
//	}
//	
//	// Core Functions
//	protected PlatformNG createPlatform(PlatformNG platform) {
//		platform.persist();
//		/*
//		platform.setId(nextId.getAndIncrement());
//		localStore.put(platform.getId(), platform);
//		*/
//		return platform;
//	}
//	
//	protected PlatformNG updatePlatform(PlatformNG platform) {
//		platform.merge();
//	
//		// localStore.put(platform.getId(), platform);
//		
//		return platform;
//	}
//	
//	protected PlatformNG getPlatform(Long id) {
//		return PlatformNG.findPlatformNG(id);
//		
//		// return localStore.get(id);
//	}
//	
//	protected void deletePlatform(Long id) {
//		PlatformNG platform = PlatformNG.findPlatformNG(id);
//		
//		platform.remove();
//		
//		// localStore.remove(id);
//	}
//	
//	protected List<PlatformNG> getPlatforms(Integer page, Integer pageSize) {
//		return (page == null && pageSize == null) ? PlatformNG.findAllPlatformNGs() : PlatformNG.findPlatformNGEntries(page, pageSize);
//		/*
//		List<Platform> result = new ArrayList<Platform>(localStore.values());
//		
//		if (page == null && pageSize == null) {
//			return result;
//		}
//		
//		int fromIndex = page * pageSize;
//		int toIndex = fromIndex + pageSize;
//		
//		if (toIndex > localStore.size() - 1) {
//			toIndex = localStore.size() - 1;
//		}
//		
//		return result.subList(fromIndex, toIndex);
//		*/
//	}
//	
//	protected List<PlatformNG> getPlatformsByText(String searchText, Integer page, Integer pageSize) {
//		// return PlatformNG.findPlatformNGsByTextLike(searchText, page, pageSize);
//		return new ArrayList<PlatformNG>();
//	}
//	
//	// @RequestMapping(method = RequestMethod.GET, value = "/platforms/types")
//	// @RequestMapping(method = RequestMethod.GET, value = "/platforms/type/{platformTypeId}/{searchText}")
}