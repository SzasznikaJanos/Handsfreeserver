package com.example.handsfree_server.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.handsfree_server.R
import kotlinx.android.synthetic.main.item_topic_recyclerview.view.*

class TopicAdapter(val onClick: (topicName: String) -> Unit) :
    RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {

    private var topics = listOf<String>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_topic_recyclerview, parent, false)
        return TopicViewHolder(view)
    }

    override fun getItemCount(): Int = topics.size

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) = holder.bind(topics[position])

    fun setTopics(topics: List<String>) {
        this.topics = topics
        notifyDataSetChanged()
    }

    inner class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(topic: String) {
            itemView.setOnClickListener {
                onClick(topic)
            }
            itemView.topic_name_textView.text = topic
        }

    }
}