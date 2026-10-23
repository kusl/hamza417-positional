package app.simple.positional.adapters.bottombar

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import app.simple.positional.R
import app.simple.positional.model.BottomBar
import app.simple.positional.preferences.FragmentPreferences

class BottomBarAdapter(private var list: ArrayList<BottomBar>, val onClick: (View) -> Unit) : RecyclerView.Adapter<BottomBarAdapter.Holder>() {
    private var currentTag = FragmentPreferences.getCurrentTag()
    private var lastItem = FragmentPreferences.getCurrentPage()
    private var bottomBarCallbacks: BottomBarCallbacks? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_bottom_bar, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, @SuppressLint("RecyclerView") position: Int) {
        holder.icon.setImageResource(list[position].icon)

        holder.icon.setOnClickListener {
            onClick(it)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setBottomBarItems(list: ArrayList<BottomBar>) {
        this.list = list

        lastItem = list.size - 1

        notifyDataSetChanged()
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.bottom_bar_item)
    }
}
