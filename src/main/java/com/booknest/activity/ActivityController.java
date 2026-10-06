package com.booknest.activity;

import java.security.Principal;

import com.booknest.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

	private final ActivityService activityService;

	public ActivityController(ActivityService activityService) {
		this.activityService = activityService;
	}

	@GetMapping
	public PageResponse<ActivityResponse> findAll(
			Principal principal,
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "") String type,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return activityService.findAll(principal.getName(), q, type, page, size);
	}
}
