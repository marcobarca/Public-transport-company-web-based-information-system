package it.polito.wa2.ticket_catalogue_service.services

import it.polito.wa2.ticket_catalogue_service.dtos.BillingInformationDTO
import it.polito.wa2.ticket_catalogue_service.dtos.OrderDTO
import kotlinx.coroutines.flow.Flow
import java.sql.Timestamp

interface OrderService {
    suspend fun addNewOrder(username: String, billingInformationDTO: BillingInformationDTO, authorizationHeader: String): Long?
    suspend fun getOrderByIdAndUsername(orderId: Long, username: String, authorizationHeader: String): OrderDTO?
    fun getAllOrdersByUsername(username: String): Flow<OrderDTO>
    fun getAllOrders(): Flow<OrderDTO>
    suspend fun getAllOrdersByUserId(userId: Long, authorizationHeader: String): Flow<OrderDTO>
    fun updateOrderByTransactionInfo(transactionJson: String)
    fun getAllOrdersByDate(startDate: Timestamp, endDate: Timestamp): Flow<OrderDTO>
    suspend fun getAllUserOrdersByDate(userId: Long, startDate: Timestamp, endDate: Timestamp, authorizationHeader: String): Flow<OrderDTO>
}
