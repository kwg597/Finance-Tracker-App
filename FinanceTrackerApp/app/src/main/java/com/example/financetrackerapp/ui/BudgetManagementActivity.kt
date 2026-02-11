package com.example.financetrackerapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetrackerapp.databinding.ActivityBudgetManagementBinding
import com.example.financetrackerapp.models.Budget
import com.example.financetrackerapp.storage.SharedPrefManager

class BudgetManagementActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var binding: ActivityBudgetManagementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefManager = SharedPrefManager(this)

        binding.btnSaveBudget.setOnClickListener {
            val budgetAmount = binding.etSetBudget.text.toString().toDoubleOrNull()
            if (budgetAmount == null || budgetAmount <= 0) {
                Toast.makeText(this, "Invalid budget amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // **Fix applied here**
            val currentSpent = sharedPrefManager.getBudget().spentAmount ?: 0.0
            val budget = Budget(budgetAmount, currentSpent)

            sharedPrefManager.saveBudget(budget)

            Toast.makeText(this, "Budget Updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
