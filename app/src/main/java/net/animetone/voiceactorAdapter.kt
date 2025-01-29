package net.animetone

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView

class voiceactorAdapter(
    private var items: List<ListItem>,
    private val onItemClick: (ListItem) -> Unit
) : RecyclerView.Adapter<voiceactorAdapter.ViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION // Track the selected position

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemAnimation: LottieAnimationView = itemView.findViewById(R.id.itemAnimation)
        private val itemText: TextView = itemView.findViewById(R.id.itemText)

        fun bind(item: ListItem, isSelected: Boolean) {
            // Set animation programmatically
            itemAnimation.setAnimation(item.animationResId)
            itemText.text = item.text

            itemView.setBackgroundColor(
                if (isSelected) itemView.context.getColor(R.color.purple_500) else Color.TRANSPARENT
            )

            // Handle click
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition

                notifyItemChanged(previousPosition) // Update the previous item
                notifyItemChanged(selectedPosition) // Update the current item

                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val isSelected = position == selectedPosition
        holder.bind(items[position], isSelected)
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ListItem>) {
        items = newItems
        selectedPosition = RecyclerView.NO_POSITION // Reset the selected position
        notifyDataSetChanged()
    }
}

