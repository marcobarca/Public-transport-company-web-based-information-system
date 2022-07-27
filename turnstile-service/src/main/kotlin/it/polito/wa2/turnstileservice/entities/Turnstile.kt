package it.polito.wa2.turnstileservice.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("turnstile")
class Turnstile {
    @Id
    var id: Long = 0L
    var turnstileId: Long = 0L
    var zid: String = ""
}