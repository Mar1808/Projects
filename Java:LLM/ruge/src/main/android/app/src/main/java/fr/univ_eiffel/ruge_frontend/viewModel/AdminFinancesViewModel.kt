package fr.univ_eiffel.ruge_frontend.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.univ_eiffel.ruge_frontend.dto.FinancesDashboardDto
import fr.univ_eiffel.ruge_frontend.dto.TransactionDto
import fr.univ_eiffel.ruge_frontend.services.AdminService
import kotlinx.coroutines.launch

class AdminFinancesViewModel : ViewModel() {
    private var allTransactions = emptyList<TransactionDto>()
    var isLoading by mutableStateOf(true)
        private set
    var dashboardData by mutableStateOf<FinancesDashboardDto?>(null)
        private set
    var filteredTransactions by mutableStateOf<List<TransactionDto>>(emptyList())
        private set
    var aiPriceText by mutableStateOf("")
        private set
    var superLikePriceText by mutableStateOf("")
        private set

    private var maxLimitFilter: Int? = 10
    private var startDateFilter: String = ""
    private var endDateFilter: String = ""
    private var sourceIdFilter: Long? = null
    private var destIdFilter: Long? = null

    init {
        loadFinancesDashboard()
    }


    fun loadFinancesDashboard() {
        viewModelScope.launch {
            isLoading = true
            val data = AdminService.getFinancesDashboard()
            if (data != null) {
                dashboardData = data
                allTransactions = data.transactions
                aiPriceText = data.aiSearchPrice.toString()
                superLikePriceText = data.superLikePrice.toString()
                applyFilters()
            }
            isLoading = false
        }
    }

    fun onAiPriceChanged(newValue: String) {
        aiPriceText = newValue
    }

    fun onSuperLikePriceChanged(newValue: String) {
        superLikePriceText = newValue
    }

    fun updatePrices() {
        viewModelScope.launch {
            val aiPrice = aiPriceText.toDoubleOrNull() ?: return@launch
            val slPrice = superLikePriceText.toDoubleOrNull() ?: return@launch
            
            val isSuccess = AdminService.updatePrices(aiPrice, slPrice)
            if (isSuccess) {
                println("Prices updated successfully.")
                loadFinancesDashboard() 
            } else {
                println("Error while updating prices.")
            }
        }
    }

    fun runRedistribution() {
        viewModelScope.launch {
            val isSuccess = AdminService.runRedistribution()
            if (isSuccess) {
                println("Redistribution completed successfully.")
                loadFinancesDashboard()
            } else {
                println("Error while running redistribution.")
            }
        }
    }

    fun applyNewFilters(max: Int?, start: String, end: String, source: Long?, dest: Long?) {
        maxLimitFilter = max
        startDateFilter = start
        endDateFilter = end
        sourceIdFilter = source
        destIdFilter = dest
        applyFilters()
    }

    fun resetFilters() {
        maxLimitFilter = 10
        startDateFilter = ""
        endDateFilter = ""
        sourceIdFilter = null
        destIdFilter = null
        applyFilters()
    }

    private fun applyFilters() {
        var list = allTransactions

        if (startDateFilter.isNotBlank()) {
            list = list.filter { it.date >= startDateFilter }
        }
        if (endDateFilter.isNotBlank()) {
            list = list.filter { it.date <= endDateFilter }
        }
        if (sourceIdFilter != null) {
            list = list.filter { it.sourceUserId == sourceIdFilter }
        }
        if (destIdFilter != null) {
            list = list.filter { it.targetUserId == destIdFilter }
        }
        if (maxLimitFilter != null) {
            list = list.take(maxLimitFilter!!)
        }

        filteredTransactions = list
    }
}