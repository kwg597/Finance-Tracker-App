package com.example.financetrackerapp.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetrackerapp.R
import com.example.financetrackerapp.databinding.ActivityAddTransactionBinding
import com.example.financetrackerapp.models.Transaction
import com.example.financetrackerapp.storage.SharedPrefManager
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var binding: ActivityAddTransactionBinding
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefManager = SharedPrefManager(this)

        val categories = arrayOf("Food", "Transport", "Shopping", "Bills", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spCategory.adapter = adapter

        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSaveTransaction.setOnClickListener {
            saveTransaction()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.etDate.setText(sdf.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString().trim()
        val amountText = binding.etAmount.text.toString().trim()
        val amount = amountText.toDoubleOrNull()
        val date = binding.etDate.text.toString()
        val category = binding.spCategory.selectedItem.toString()

        // Get the selected transaction type
        val type = when (binding.transactionTypeGroup.checkedRadioButtonId) {
            R.id.radioIncome -> "Income"
            R.id.radioExpense -> "Expense"
            else -> ""
        }

        // Validate inputs
        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Amount must be a positive number", Toast.LENGTH_SHORT).show()
            return
        }
        if (date.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        if (type.isEmpty()) {
            Toast.makeText(this, "Please select a transaction type", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new transaction
        val newTransaction = Transaction(
            title = title,
            amount = amount,
            category = category,
            date = date,
            type = type // Pass the type here
        )

        val transactions = sharedPrefManager.getTransactions().toMutableList()
        transactions.add(newTransaction)
        sharedPrefManager.saveTransactions(transactions)

        Toast.makeText(this, "Transaction Added", Toast.LENGTH_SHORT).show()
        finish()
    }
}
