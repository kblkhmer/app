package com.lotterykh.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lotterykh.app.databinding.ItemPredictionNumberBinding

class PredictionNumberAdapter(
    private val numbers: List<String>
) : RecyclerView.Adapter<PredictionNumberAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPredictionNumberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPredictionNumberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvNumber.text = numbers[position]
    }

    override fun getItemCount() = numbers.size
}
