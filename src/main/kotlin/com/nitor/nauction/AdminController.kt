package com.nitor.nauction

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminController(private val auctionDao: AuctionDao) {

    @GetMapping("/admin")
    fun admin(model: Model): String {
        model.addAttribute("items", auctionDao.findAllAdmin());
        return "admin"
    }
}