package com.myapplication.notesapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myapplication.notesapp.model.Note

class NoteAdapter(
    private val notes: MutableList<Note>,
    private val onItemClick: (Note) -> Unit,
    private val onDeleteClick: (Note, Int) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private val TAG = "NoteAdapter"

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textNoteTitle)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        Log.d(TAG, "onCreateViewHolder called")
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        Log.d(TAG, "Binding note at position $position: ${note.title}")

        holder.titleTextView.text = note.title

        holder.itemView.setOnClickListener {
            Log.d(TAG, "Note clicked: ${note.title}")
            onItemClick(note)
        }

        holder.btnDelete.setOnClickListener {
            Log.d(TAG, "Delete button clicked for note: ${note.title}")
            onDeleteClick(note, position)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount called, count: ${notes.size}")
        return notes.size
    }

    fun removeNoteAt(pos: Int) {
        if (pos >= 0 && pos < notes.size) {
            Log.d(TAG, "Removing note at position $pos")
            notes.removeAt(pos)
            notifyItemRemoved(pos)
            // Notify that data set changed to update any views that depend on the size
            notifyItemRangeChanged(pos, notes.size - pos)
        } else {
            Log.e(TAG, "Attempted to remove note at invalid position: $pos, size: ${notes.size}")
        }
    }

    fun setNotes(newNotes: List<Note>) {
        Log.d(TAG, "Setting ${newNotes.size} new notes")
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }
}