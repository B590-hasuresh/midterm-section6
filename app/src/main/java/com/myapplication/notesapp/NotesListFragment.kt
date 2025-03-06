package com.myapplication.notesapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.myapplication.notesapp.model.Note


class NotesListFragment : Fragment() {
    private val TAG = "NotesListFragment"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var notesList: MutableList<Note>
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register fragment result listeners
        parentFragmentManager.setFragmentResultListener("noteAdded", this) { _, _ ->
            Log.d(TAG, "Note added result received, reloading notes")
            loadNotes()
        }

        parentFragmentManager.setFragmentResultListener("noteUpdated", this) { _, _ ->
            Log.d(TAG, "Note updated result received, reloading notes")
            loadNotes()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notes_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewNotes = view.findViewById(R.id.recyclerViewNotes)
        val buttonAddNote = view.findViewById<Button>(R.id.buttonAddNote)

        // Initialize empty notes list
        notesList = mutableListOf()

        // Set up RecyclerView with LinearLayoutManager
        recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())

        // Create adapter with click handlers
        adapter = NoteAdapter(
            notesList,
            onItemClick = { note ->
                openNoteFragment(note)
            },
            onDeleteClick = { note, position ->
                showDeleteConfirmDialog(note, position)
            }
        )

        // Set adapter to RecyclerView
        recyclerViewNotes.adapter = adapter

        // Add note button click listener
        buttonAddNote.setOnClickListener {
            openNoteFragment(null)
        }

        // Initial load of notes
        Log.d(TAG, "onViewCreated complete, loading notes")
        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        // Reload notes when returning to this fragment
        Log.d(TAG, "onResume called, loading notes")
        loadNotes()
    }

    private fun loadNotes() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Current user is null, cannot load notes")
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        Log.d(TAG, "Loading notes for user: $userId")

        // Reference to notes collection
        val notesRef = db.collection("users").document(userId).collection("notes")

        // Log the path to verify it's correct
        Log.d(TAG, "Firestore query path: ${notesRef.path}")

        // Make sure RecyclerView is visible
        recyclerViewNotes.visibility = View.VISIBLE

        // Use orderBy to sort notes by timestamp
        notesRef.orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val count = result.size()
                Log.d(TAG, "Successfully loaded $count notes from Firestore")

                // Clear existing notes
                notesList.clear()

                // Process retrieved notes
                for (document in result) {
                    val id = document.id
                    val title = document.getString("title") ?: ""
                    val description = document.getString("description") ?: ""
                    val timestamp = document.getLong("timestamp") ?: 0

                    Log.d(TAG, "Note found - ID: $id, Title: $title, Timestamp: $timestamp")

                    notesList.add(Note(id = id, title = title, description = description))
                }

                // Directly update adapter
                adapter.notifyDataSetChanged()

                // Log note count for debugging
                Log.d(TAG, "Updated adapter with ${notesList.size} notes")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading notes", exception)
                Toast.makeText(requireContext(), "Error loading notes: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openNoteFragment(note: Note?) {
        val fragment = NoteFragment()
        if (note != null) {
            val args = Bundle().apply {
                putString("noteId", note.id)
                putString("noteTitle", note.title)
                putString("noteDesc", note.description)
            }
            fragment.arguments = args
            Log.d(TAG, "Opening existing note: ${note.id} - ${note.title}")
        } else {
            Log.d(TAG, "Creating new note")
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showDeleteConfirmDialog(note: Note, position: Int) {
        ConfirmDeleteDialogFragment {
            deleteNote(note, position)
        }.show(parentFragmentManager, "confirmDelete")
    }

    private fun deleteNote(note: Note, position: Int) {
        val currentUser = auth.currentUser ?: return

        val userId = currentUser.uid
        Log.d(TAG, "Deleting note: ${note.id} - ${note.title}")

        db.collection("users")
            .document(userId).collection("notes").document(note.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Note successfully deleted")
                // Update the adapter directly first for smoother UI
                adapter.removeNoteAt(position)
                Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting note", e)
                Toast.makeText(requireContext(), "Failed to delete: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}