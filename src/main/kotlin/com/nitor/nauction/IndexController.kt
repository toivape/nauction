package com.nitor.nauction

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController(val dao: AuctionItemDao) {

    @GetMapping("/")
    fun index(model:Model): String {
        model.addAttribute("items", dao.findAllActive())
        return "index"
    }
}