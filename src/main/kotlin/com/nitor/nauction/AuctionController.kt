package com.nitor.nauction

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AuctionController(val dao: AuctionItemDao) {

    @GetMapping("/")
    fun index(model:Model): String {
        model.addAttribute("items", dao.findAllOpen())
        return "index"
    }
}