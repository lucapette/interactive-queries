package me.lucapette.interactivequeries

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class IngestionController {

    @PostMapping("/accept")
    fun accept(@RequestBody() input: IngestionRequest) {
        println(input.word)
    }
}

data class IngestionRequest(val word: String)