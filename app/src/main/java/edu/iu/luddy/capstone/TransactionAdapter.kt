package edu.iu.luddy.capstone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private var transactions: MutableList<Transaction>,
    private val onEdit: (Transaction) -> Unit,
    private val onDelete: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
        val categoryText: MaterialTextView = itemView.findViewById(R.id.categoryText)
        val descriptionText: MaterialTextView = itemView.findViewById(R.id.descriptionText)
        val amountText: MaterialTextView = itemView.findViewById(R.id.amountText)
        val dateText: MaterialTextView = itemView.findViewById(R.id.dateText)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // Set category icon based on category
        holder.categoryIcon.setImageResource(getCategoryIcon(transaction.category))

        // Set texts
        holder.categoryText.text = transaction.category
        holder.descriptionText.text = transaction.description.ifEmpty { "No description" }
        holder.dateText.text = transaction.date

        // Set amount with color coding
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        val amountStr = format.format(transaction.amount)

        holder.amountText.text = if (transaction.type == TransactionType.INCOME) {
            "+$amountStr"
        } else {
            "-$amountStr"
        }

        // Color code amount
        val context = holder.itemView.context
        holder.amountText.setTextColor(
            if (transaction.type == TransactionType.INCOME) {
                context.getColor(R.color.success)
            } else {
                context.getColor(R.color.error)
            }
        )

        // Set click listeners
        holder.editButton.setOnClickListener {
            onEdit(transaction)
        }

        holder.deleteButton.setOnClickListener {
            onDelete(transaction)
        }

        // Optional: Add click listener for entire item
        holder.itemView.setOnClickListener {
            onEdit(transaction)
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newTransactions: MutableList<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    private fun getCategoryIcon(category: String): Int {
        // Map categories to drawable resources
        // Use ic_other as fallback for missing icons
        return try {
            when (category.lowercase()) {
                "food & dining", "food" -> R.drawable.ic_food
                "transportation", "transport" -> R.drawable.ic_transport
                "shopping" -> R.drawable.ic_shopping
                "entertainment" -> R.drawable.ic_entertainment
                "bills & utilities", "bills" -> R.drawable.ic_bills
                "healthcare", "health" -> R.drawable.ic_other
                "education" -> R.drawable.ic_education
                "salary", "income" -> R.drawable.ic_income
                "freelance" -> R.drawable.ic_other
                "investment" -> R.drawable.ic_other
                "gift" -> R.drawable.ic_other
                else -> R.drawable.ic_other
            }
        } catch (e: Exception) {
            R.drawable.ic_other
        }
    }
}