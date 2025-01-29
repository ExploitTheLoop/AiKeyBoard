package net.animetone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView

class PredictionAdapter(
    private val data: List<String>,
    private val onItemSelected: (String) -> Unit
) : RecyclerView.Adapter<PredictionAdapter.PredictionViewHolder>() {

    // Store the position of the selected item
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prediction, parent, false)
        return PredictionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        holder.bind(data[position], position)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    
    inner class PredictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textView)
        private val container: LinearLayout = itemView.findViewById(R.id.container)
        private val lottiePulse: LottieAnimationView = itemView.findViewById(R.id.lottiepulse)

        fun bind(item: String, position: Int) {
            textView.text = item

            // Change background color for the selected item
            if (position == selectedPosition) {

                lottiePulse.visibility = View.VISIBLE

            } else {

                lottiePulse.visibility = View.GONE
            }

            // Handle item click to select it
            itemView.setOnClickListener {
                selectedPosition = position  // Update selected position
                notifyDataSetChanged()  // Notify RecyclerView to rebind items
                onItemSelected(item)  // Pass the selected item back to the activity/fragment
            }
        }
    }

}
