package it.polito.wa2.ticket_catalogue_service.controllers

import it.polito.wa2.ticket_catalogue_service.dtos.OrderDTO
import it.polito.wa2.ticket_catalogue_service.dtos.TicketDTO
import it.polito.wa2.ticket_catalogue_service.services.OrderServiceImpl
import it.polito.wa2.ticket_catalogue_service.services.TicketServiceImpl
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.sql.Date
import java.sql.Timestamp

@RestController
@RequestMapping("/admin")
class AdminController {
    @Autowired
    private lateinit var ticketService: TicketServiceImpl
    @Autowired
    private lateinit var orderService: OrderServiceImpl

    @PostMapping("/tickets", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    suspend fun postNewTicket(
        @RequestBody newTicketDTO: TicketDTO,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<TicketDTO> {
        val retrievedTicket = ticketService.addNewTicket(newTicketDTO)
        return ResponseEntity.ok(retrievedTicket)
    }

    @GetMapping("/orders", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    suspend fun getAllUsersOrders(
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<Flow<OrderDTO>> {
        val retrievedOrders = orderService.getAllOrders()
        return ResponseEntity.ok(retrievedOrders)
    }

    @GetMapping("/orders/{userId}", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    suspend fun getSpecificUserOrders(
        @PathVariable("userId") userId: Long,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<Flow<OrderDTO>> {
        val retrievedOrders = orderService.getAllOrdersByUserId(userId, authorizationHeader)
        return ResponseEntity.ok(retrievedOrders)
    }

    @GetMapping("/orders/date", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    suspend fun getAllOrdersByDate(
        @RequestParam("start") startDate: Date,
        @RequestParam("end") endDate: Date,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<Flow<OrderDTO>> {
        val startTimestamp = Timestamp(startDate.time)
        val endTimestamp = Timestamp(endDate.time)
        val retrievedOrders = orderService.getAllOrdersByDate(
            startTimestamp,
            endTimestamp
        )
        return ResponseEntity.ok(retrievedOrders)
    }

    @GetMapping("/orders/{userId}/date", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    suspend fun getUserOrdersByDate(
        @PathVariable("userId") userId: Long,
        @RequestParam("start") startDate: Date,
        @RequestParam("end") endDate: Date,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<Flow<OrderDTO>> {
        val startTimestamp = Timestamp(startDate.time)
        val endTimestamp = Timestamp(endDate.time)
        val retrievedOrders = orderService.getAllUserOrdersByDate(
            userId,
            startTimestamp,
            endTimestamp,
            authorizationHeader
        )
        return ResponseEntity.ok(retrievedOrders)
    }
}
