package com.youdao.course.myapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.youdao.course.myapplication.R
import com.youdao.course.myapplication.databinding.ItemStickerBinding
import com.youdao.course.myapplication.model.Sticker

/**
 * 贴纸选择器适配器
 */
class StickerAdapter(
    private val onStickerSelected: (Sticker) -> Unit
) : ListAdapter<Sticker, StickerAdapter.StickerViewHolder>(StickerDiffCallback()) {

    // 当前选中的贴纸ID
    private var selectedStickerId: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val binding = ItemStickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val sticker = getItem(position)
        holder.bind(sticker, sticker.id == selectedStickerId)
    }

    /**
     * 设置选中的贴纸
     */
    fun setSelectedSticker(stickerId: Int) {
        val oldSelectedId = selectedStickerId
        selectedStickerId = stickerId
        
        // 刷新之前选中的和现在选中的项
        currentList.forEachIndexed { index, sticker ->
            if (sticker.id == oldSelectedId || sticker.id == stickerId) {
                notifyItemChanged(index)
            }
        }
    }

    inner class StickerViewHolder(
        private val binding: ItemStickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val sticker = getItem(position)
                    setSelectedSticker(sticker.id)
                    onStickerSelected(sticker)
                }
            }
        }

        fun bind(sticker: Sticker, isSelected: Boolean) {
            binding.apply {
                ivStickerPreview.setImageResource(sticker.drawableRes)
                tvStickerName.text = sticker.name

                // 设置选中状态
                val strokeColor = if (isSelected) {
                    ContextCompat.getColor(root.context, R.color.selected_sticker)
                } else {
                    ContextCompat.getColor(root.context, R.color.transparent)
                }
                cardSticker.strokeColor = strokeColor
                cardSticker.strokeWidth = if (isSelected) 4 else 0

                // 设置背景色
                val backgroundColor = if (isSelected) {
                    ContextCompat.getColor(root.context, R.color.purple_200)
                } else {
                    ContextCompat.getColor(root.context, R.color.white)
                }
                cardSticker.setCardBackgroundColor(backgroundColor)
            }
        }
    }

    /**
     * DiffUtil回调
     */
    class StickerDiffCallback : DiffUtil.ItemCallback<Sticker>() {
        override fun areItemsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
            return oldItem == newItem
        }
    }
}
