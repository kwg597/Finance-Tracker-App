package com.example.financetrackerapp.ui

import TransactionAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financetrackerapp.databinding.ActivityMainBinding
import com.example.financetrackerapp.storage.SharedPrefManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationHelper: NotificationHelper

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefManager = SharedPrefManager(this)
        notificationHelper = NotificationHelper(this)

        updateBudgetUI()
        setupRecyclerView()
        setupPieChart()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.example.financetrackerapp.R.id.add_transaction -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    true
                }
                com.example.financetrackerapp.R.id.manage_budget -> {
                    startActivity(Intent(this, BudgetManagementActivity::class.java))
                    true
                }
                com.example.financetrackerapp.R.id.backup -> {
                    startActivity(Intent(this, BackupRestoreActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupPieChart() {
        val transactions = sharedPrefManager.getTransactions()
        
        // Only consider expense transactions for the pie chart
        val expenseTransactions = transactions.filter { it.type == "Expense" }

        if (expenseTransactions.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        // Group expenses by category and sum their amounts
        val categoryMap = mutableMapOf<String, Float>()
        expenseTransactions.forEach { transaction ->
            categoryMap[transaction.category] = categoryMap.getOrDefault(transaction.category, 0f) + transaction.amount.toFloat()
        }

        // Create Pie Entries
        val pieEntries = categoryMap.map { PieEntry(it.value, it.key) }

        val dataSet = PieDataSet(pieEntries, "Expense Categories")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()

        val pieData = PieData(dataSet)
        binding.pieChart.data = pieData
        binding.pieChart.description.isEnabled = false
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate() // Refresh the chart
    }

    private fun updateBudgetUI() {
        val budget = sharedPrefManager.getBudget()
        val transactions = sharedPrefManager.getTransactions()
        
        // Only consider expenses for budget tracking
        val spentAmount = transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        // Use your stored currency symbol or default to $
        val currencySymbol = sharedPrefManager.getCurrencySymbol() ?: "$"
        
        binding.tvTotalBudgetValue.text = "$currencySymbol${budget.totalBudget}"
        binding.tvSpentValue.text = "$currencySymbol${spentAmount}"

        sharedPrefManager.saveBudget(budget.copy(spentAmount = spentAmount))
        checkBudgetThreshold(budget.totalBudget!!, spentAmount)
        setupRecyclerView()
    }

    private fun checkBudgetThreshold(totalBudget: Double, spentAmount: Double) {
        val threshold = totalBudget * 0.85
        when {
            spentAmount >= totalBudget -> {
                notificationHelper.showBudgetAlertNotification("⚠️ Budget Exceeded! You've spent $spentAmount out of $totalBudget.")
            }
            spentAmount >= threshold -> {
                notificationHelper.showBudgetAlertNotification("⚠️ Warning: You're nearing your budget limit! Spent: $spentAmount / $totalBudget.")
            }
        }
    }

    private fun setupRecyclerView() {
        val transactions = sharedPrefManager.getTransactions().toMutableList()
        val sortedTransactions = transactions.sortedByDescending { it.date }.toMutableList()

        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.setHasFixedSize(true)
        binding.rvTransactions.adapter = TransactionAdapter(
            transactions = sortedTransactions,
            sharedPrefManager = sharedPrefManager,
            onUpdateClick = { transaction ->
                Toast.makeText(this, "Update clicked for ${transaction.title}", Toast.LENGTH_SHORT).show()
            },
            onTransactionDeleted = {
                // Refresh the UI when a transaction is deleted
                updateBudgetUI()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setupPieChart()
                }
            }
        )
    }

    private fun storeEncryptedClientName() {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Generate a secret key (store it securely in production)
        val secretKey = EncryptionUtils.generateKey()
        val keyString = EncryptionUtils.keyToString(secretKey)

        // Encrypt the client's name
        val clientName = "Your Client's Name"
        val encryptedName = EncryptionUtils.encrypt(clientName, secretKey)

        // Store the encrypted name and key in SharedPreferences
        editor.putString("encrypted_client_name", encryptedName)
        editor.putString("encryption_key", keyString)
        editor.apply()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onResume() {
        super.onResume()
        updateBudgetUI()
        setupPieChart() // Refresh pie chart with new transactions
    }
}