package it.polito.wa2.login_service.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.login_service.dtos.*
import it.polito.wa2.login_service.entities.Activation
import it.polito.wa2.login_service.entities.Role
import it.polito.wa2.login_service.entities.User
import it.polito.wa2.login_service.exceptions.*
import it.polito.wa2.login_service.repositories.ActivationRepository
import it.polito.wa2.login_service.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

import java.security.Key
import java.time.LocalDate
import java.util.*

@Service
class UserServiceImpl : UserService {
    @Autowired
    private lateinit var emailService: EmailService
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var activationRepository: ActivationRepository

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var errorExplanation: String

    @Value("\${jwt.authorization.signature-key-base64}")
    private lateinit var jwtSecretB64Key: String
    @Value("\${jwt.authorization.expiration-time-ms}")
    private var jwtExpirationTimeMs: Int = 0

    private fun checkEmailFormat(email: String): Boolean {
        val emailRegex = Regex(
            "^" +
            "[a-zA-Z0-9._\\-]{4,16}" +
            "@" +
            "[a-zA-Z0-9\\-]{2,8}" +
            "(" +
                "\\." +
                "[a-zA-Z0-9]{2,8}" +
            ")" +
            "$"
        )
        val emailCheck = emailRegex.matches(email)
        if (!emailCheck)
            errorExplanation = "wrong email format"
        return emailCheck
    }

    private fun checkPasswordStrength(password: String): Boolean {
        val passwordRegex = Regex(
            "^" +
            "(?=.*[a-z])" +         // at least 1 lowercase letter
            "(?=.*[A-Z])" +         // at least 1 uppercase letter
            "(?=.*[0-9])" +         // at least 1 digit
            "(?=.*[@#$%^&+=])" +    // at least 1 special character
            "(?=\\S+$)" +           // no whitespaces
            ".{8,}" +               // at least 8 characters
            "$"
        )
        val passwordCheck = passwordRegex.matches(password)
        if (!passwordCheck)
            errorExplanation = "password not strong enough"
        return passwordCheck
    }

    private fun checkTravelerCredentials(username: String, password: String, email: String) {
        if (username.isNotEmpty() && !username.contains(" ") && password.isNotEmpty() && email.isNotEmpty()) {
            if (checkEmailFormat(email) && checkPasswordStrength(password)) {
                if (userRepository.getUserByUsernameOrEmail(username, email) == null) {
                    return
                }
                errorExplanation = "username or email already used"
            }
        }
        else
            errorExplanation = "fields cannot be empty"
        throw InvalidUserException("Traveler not valid: $errorExplanation")
    }

    private fun checkAdminCredentials(username: String, password: String) {
        if (username.isNotEmpty() && !username.contains(" ") && password.isNotEmpty()) {
            if (checkPasswordStrength(password)) {
                if (userRepository.getUserByUsername(username) == null) {
                    return
                }
                errorExplanation = "username already used"
            }
        }
        else
            errorExplanation = "fields cannot be empty"
        throw InvalidUserException(errorExplanation)
    }

    private fun getRandomActivationCode(length: Int = 6): String {
        return (1..length)
            .map { "0123456789".random() }
            .joinToString("")
    }

    /*
     * checking that the submitted user's properties are suitable
     *
     * if data (username, password, email) pass the checks,
     * a new user and a new activation are saved in the db,
     * then it returns an object containing
     * the new activation's provisional id and the new user's email,
     * otherwise it throws an InvalidUserException
     */
    override fun registerTraveler(username: String, password: String, email: String): ActivationOutputDTO {
        checkTravelerCredentials(username, password, email)
        val newTraveler = userRepository.save(
            User().apply {
                this.username = username
                this.password = passwordEncoder.encode(password)
                this.email = email
                roles = mutableSetOf(Role.CUSTOMER)
            }
        )
        val newActivation = activationRepository.save(
            Activation().apply {
                activationCode = getRandomActivationCode()
                user = newTraveler
            }
        )
        emailService.sendEmail(
            username, email,
            newActivation.provisionalId.toString(), newActivation.activationCode
        )
        return ActivationOutputDTO(
            newActivation.provisionalId,
            newActivation.user!!.email
        )
    }

    private fun decrementAttemptCounter(attemptCounter: Int, provisionalId: UUID, userId: Long) {
        if (attemptCounter > 1)
            activationRepository.decrementAttemptCounterByProvisionalId(provisionalId)
        else {
            // it's no longer necessary to delete activations (because of ON DELETE CASCADE)
            //activationRepository.deleteActivationByProvisionalId(provisionalId)
            userRepository.deleteById(userId)
        }
    }

    private fun checkActivationTraveler(provisionalId: UUID, activationCode: String): User {
        val retrievedActivation = activationRepository.getActivationByProvisionalId(provisionalId)
        if (retrievedActivation != null && retrievedActivation.deadline.isAfter(LocalDate.now())) {
            if (retrievedActivation.user != null) {
                if (Regex("[0-9]{6}").matches(activationCode)) {
                    if (activationCode == retrievedActivation.activationCode) {
                        return retrievedActivation.user!!
                    }
                }
                errorExplanation = "wrong activation code"
                decrementAttemptCounter(
                    retrievedActivation.attemptCounter,
                    retrievedActivation.provisionalId,
                    retrievedActivation.user!!.id
                )
            }
        }
        else
            errorExplanation = "activation not found or expired"
        throw InvalidActivationException(errorExplanation)
    }

    /*
     * checking that the submitted activation's properties are suitable
     *
     * if data (provisionalId, activationCode) pass the checks,
     * the pending activation is removed from the db and the correlated user is set to active,
     * then it returns an object containing the activated user's id, username, email,
     * otherwise it decrements the existing activation's attempts counter
     * (and if its value reaches the zero, both the existing user
     * and the correlated activation are removed from the db)
     * and throws an InvalidActivationException
     */
    override fun validateTraveler(provisionalId: UUID, activationCode: String): TravelerOutputDTO {
        val retrievedTraveler = checkActivationTraveler(provisionalId, activationCode)
        activationRepository.deleteActivationByProvisionalId(provisionalId)
        userRepository.activateById(retrievedTraveler.id)
        return TravelerOutputDTO(
            retrievedTraveler.id,
            retrievedTraveler.username,
            retrievedTraveler.email,
            retrievedTraveler.roles
        )
    }

    override fun loginUser(username: String, password: String): AuthorizationTokenDTO {
        val retrievedUser = userRepository.findByUsername(username)
        if (retrievedUser == null ||
            !passwordEncoder.matches(password, retrievedUser.password) ||
            retrievedUser.active == 0) // we need that the user is active to login // TODO: change to 0
            throw LoginException("login rejected")
        val jwtSecretByteKey = Base64.getDecoder().decode(jwtSecretB64Key)
        val jwtSecretKey: Key = Keys.hmacShaKeyFor(jwtSecretByteKey)
        val accessToken = Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setSubject(username)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + jwtExpirationTimeMs))
            .claim("roles", retrievedUser.roles)
            .signWith(jwtSecretKey)
            .compact()
        return AuthorizationTokenDTO(accessToken)
    }

    override fun changePasswordUser(username: String, oldPassword: String, newPassword: String) {
        val retrievedUser = userRepository.findByUsername(username)
        if (!passwordEncoder.matches(oldPassword, retrievedUser?.password))
            throw InvalidPasswordException("wrong old password")
        if (!checkPasswordStrength(newPassword))
            throw InvalidPasswordException("new password not strong enough")
        if (oldPassword == newPassword)
            throw InvalidPasswordException("old password and new password can't be the same")
        userRepository.save(
            User().apply {
                this.id = retrievedUser!!.id
                this.username = retrievedUser.username
                this.password = passwordEncoder.encode(newPassword)
                this.email = retrievedUser.email
                this.active = retrievedUser.active
                this.enrollingCapability = retrievedUser.enrollingCapability
            }
        )
    }

    override fun deleteAccountTraveler(username: String) {
        userRepository.deleteByUsername(username)
    }

    override fun enrollAdmin(
        loggedUsername: String,
        newAdminUsername: String,
        newAdminPassword: String,
        newAdminEnrollingCapability: Int
    ): AdminOutputDTO {
        val retrievedAdmin = userRepository.findByUsername(loggedUsername)
        if (retrievedAdmin?.enrollingCapability == 0)
            throw EnrollingCapabilityException("forbidden")
        checkAdminCredentials(newAdminUsername, newAdminPassword)
        val newAdmin = userRepository.save(
            User().apply {
                this.username = newAdminUsername
                this.password = passwordEncoder.encode(newAdminPassword)
                this.active = 1
                roles = mutableSetOf(Role.ADMIN)
                this.enrollingCapability = newAdminEnrollingCapability
            }
        ).toAdminDTO()
        return AdminOutputDTO(
            newAdmin.id,
            newAdmin.username,
            newAdmin.enrollingCapability,
            newAdmin.roles
        )
    }

    override fun enrollDefaultAdmin(
        newAdminUsername: String,
        newAdminPassword: String,
        newAdminEnrollingCapability: Int
    ) {
        checkAdminCredentials(newAdminUsername, newAdminPassword)
        userRepository.save(
            User().apply {
                this.username = newAdminUsername
                this.password = passwordEncoder.encode(newAdminPassword)
                this.active = 1
                roles = mutableSetOf(Role.ADMIN)
                this.enrollingCapability = newAdminEnrollingCapability
            }
        )
    }

    override fun disableAccountAdmin(username: String, userId: Long) {
        val retrievedAdmin = userRepository.findByUsername(username)
        if (retrievedAdmin?.enrollingCapability == 0)
            throw EnrollingCapabilityException("forbidden")
        val retrievedUser = userRepository.findById(userId)
        if (retrievedUser.isEmpty)
            throw DisableAccountException("user id does not exist")
        if (retrievedAdmin?.id == retrievedUser.get().id)
            throw DisableAccountException("logged user account can't be disabled")
        userRepository.deactivateById(userId)
    }

    // TODO: remove this
    fun addTraveler(
        username: String,
        password: String,
        email: String,
        active: Int,
        role: Role
    ){
        userRepository.save(
            User().apply {
                this.username = username
                this.password = passwordEncoder.encode(password)
                this.email = email
                this.active = active
                this.roles = setOf(role)
                this.enrollingCapability = 0
            }
        )
    }
}
