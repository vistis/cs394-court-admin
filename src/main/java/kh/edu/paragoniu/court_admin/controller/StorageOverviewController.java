package kh.edu.paragoniu.court_admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import kh.edu.paragoniu.court_admin.service.StorageOverviewService;

@Controller
public class StorageOverviewController {

    @Autowired
    private StorageOverviewService storageOverviewService;

    @GetMapping("/admin/storage")
    public String storageOverview(Model model) {
        model.addAttribute("activeNav", "storage");
        model.addAttribute("overview", storageOverviewService.getOverview());
        return "admin/storage-overview";
    }
}