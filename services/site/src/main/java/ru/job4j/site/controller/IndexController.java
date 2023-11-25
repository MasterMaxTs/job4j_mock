package ru.job4j.site.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.job4j.site.dto.CategoryDTO;
import ru.job4j.site.dto.ProfileDTO;
import ru.job4j.site.service.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.job4j.site.controller.RequestResponseTools.getToken;

@Controller
@AllArgsConstructor
@Slf4j
public class IndexController {
    private final CategoriesService categoriesService;
    private final TopicsService topicsService;
    private final InterviewsService interviewsService;
    private final AuthService authService;
    private final ProfilesService profilesService;
    private final NotificationService notifications;

    @GetMapping({"/", "index"})
    public String getIndexPage(Model model, HttpServletRequest req) throws JsonProcessingException {
        RequestResponseTools.addAttrBreadcrumbs(model,
                "Главная", "/"
        );
        try {
            model.addAttribute("categories", categoriesService.getMostPopular());
            var token = getToken(req);
            if (token != null) {
                var userInfo = authService.userInfo(token);
                model.addAttribute("userInfo", userInfo);
                model.addAttribute("userDTO", notifications.findCategoriesByUserId(userInfo.getId()));
                RequestResponseTools.addAttrCanManage(model, userInfo);
            }
        } catch (Exception e) {
            log.error("Remote application not responding. Error: {}. {}, ", e.getCause(), e.getMessage());
        }
        var interviews = interviewsService.getByType(1);
        model.addAttribute("new_interviews", interviews);
        model.addAttribute("authors",
                interviews.stream()
                        .map(i -> {
                            String username = "";
                            Optional<ProfileDTO> profileOptional = profilesService.getProfileById(i.getSubmitterId());
                            return profileOptional.isPresent() ? profileOptional.get().getUsername()
                                    : username;
                        }).collect(Collectors.toList()));
        model.addAttribute("map_category_id_counts_by_new_interview",
                interviews.stream()
                        .map(i ->
                                topicsService.getById(i.getTopicId()).getCategory())
                        .collect(Collectors.groupingBy(CategoryDTO::getId,
                                Collectors.counting())));
        return "index";
    }
}