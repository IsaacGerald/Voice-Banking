package com.example.mywallet.feature_wallet.presentation.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mywallet.databinding.TransactionLayoutBinding
import com.example.mywallet.feature_wallet.domain.model.Transaction
import com.example.mywallet.feature_wallet.presentation.ui.TransactionAdapter.TransactionViewHolder.Companion.from

class TransactionAdapter :
    ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        return from(parent)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class TransactionViewHolder(private var binding: TransactionLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): TransactionViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = TransactionLayoutBinding.inflate(inflater, parent, false)
                return TransactionViewHolder(view)
            }
        }


        fun bind(item: Transaction) {

            binding.transaction = item

            val amount = item.amount
            val newAmount = "%,d".format(amount)
            val builder = StringBuilder()

            if (item.type == "Sent") {
                builder.append("-")
                builder.append(newAmount)
                binding.txtAmount.text = builder.toString()
            } else if (item.type == "Receive") {
                builder.append("+")
                builder.append(newAmount)
                binding.txtAmount.text = builder.toString()
            }

            binding.invalidateAll()
        }

    }


}

object TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }

}