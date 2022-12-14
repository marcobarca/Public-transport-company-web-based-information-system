package it.polito.wa2.ticket_catalogue_service.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.ticket_catalogue_service.dtos.Role
import it.polito.wa2.ticket_catalogue_service.dtos.UserDetailsDTO
import java.security.Key
import java.util.*

class JwtUtils(base64: String) {
    private val signatureKey: Key

    init {
        val byteKey = Base64.getDecoder().decode(base64)
        signatureKey = Keys.hmacShaKeyFor(byteKey)
    }

    fun validateJwt(authToken: String): Boolean {
        try {
            val decodedJwt = Jwts.parserBuilder()
                .setSigningKey(signatureKey)
                .build()
                .parseClaimsJws(authToken)
        } catch (ex: Throwable) {
            return false
        }
        return true
    }

    fun getDetailsJwt(authToken: String): UserDetailsDTO {
        val decodedJwt = Jwts.parserBuilder()
            .setSigningKey(signatureKey)
            .build()
            .parseClaimsJws(authToken)

        val username = decodedJwt.body["sub"] as String
        val rolesString: List<String> = decodedJwt.body["roles"] as List<String>
        val roles: MutableSet<Role> = mutableSetOf()
        rolesString.map { roleString ->
            roles += when (roleString) {
                "CUSTOMER" -> Role.CUSTOMER
                else -> Role.ADMIN
            }
        }
        return UserDetailsDTO(username, roles, null, null, null)
    }

}
