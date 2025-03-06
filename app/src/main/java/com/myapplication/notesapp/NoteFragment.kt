package com.myapplication.notesapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NoteFragment : Fragment() {
    private val TAG = "NoteFragment"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle back button press
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Just go back without saving
                Log.d(TAG, "Back pressed, returning without saving")
                parentFragmentManager.popBackStack()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleField = view.findViewById<EditText>(R.id.editTextTitle)
        val descField = view.findViewById<EditText>(R.id.editTextDescription)
        val saveButton = view.findViewById<Button>(R.id.buttonSave)

        noteId = arguments?.getString("noteId")
        val existingTitle = arguments?.getString("noteTitle")
        val existingDesc = arguments?.getString("noteDesc")

        Log.d(TAG, "Note ID: $noteId, Title: $existingTitle")

        existingTitle?.let { titleField.setText(it) }
        existingDesc?.let { descField.setText(it) }

        saveButton.setOnClickListener {
            val titleText = titleField.text.toString().trim()
            val descText = descField.text.toString().trim()

            if (titleText.isEmpty()) {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show a "Saving..." toast
            Toast.makeText(requireContext(), "Saving note...", Toast.LENGTH_SHORT).show()

            saveNote(currentUser.uid, titleText, descText)
        }
    }

    private fun saveNote(userId: String, title: String, description: String) {
        val notesCollection = db.collection("users").document(userId).collection("notes")

        Log.d(TAG, "Saving note: Title=$title, Description=${if (description.length > 20) description.substring(0, 20) + "..." else description}")

        val noteData = hashMapOf(
            "title" to title,
            "description" to description,
            "timestamp" to System.currentTimeMillis()
        )

        if (noteId == null) {
            // Add new note
            Log.d(TAG, "Creating new note")
            notesCollection.add(noteData)
                .addOnSuccessListener { documentReference ->
                    val newId = documentReference.id
                    Log.d(TAG, "Note added with ID: $newId")
                    Toast.makeText(requireContext(), "Note added successfully", Toast.LENGTH_SHORT).show()

                    // Verify the note exists in Firestore
                    notesCollection.document(newId).get()
                        .addOnSuccessListener { docSnapshot ->
                            if (docSnapshot.exists()) {
                                Log.d(TAG, "Verified note was saved in Firestore: ${docSnapshot.data}")
                            } else {
                                Log.e(TAG, "Note was not found in Firestore after saving!")
                            }
                        }

                    // Set a flag to indicate a successful save
                    Log.d(TAG, "Setting fragment result: noteAdded=true")
                    parentFragmentManager.setFragmentResult("noteAdded", Bundle().apply {
                        putBoolean("success", true)
                    })

                    // Navigate back
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding note", e)
                    Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // Update existing note
            Log.d(TAG, "Updating existing note: $noteId")
            notesCollection.document(noteId!!)
                .set(noteData)
                .addOnSuccessListener {
                    Log.d(TAG, "Note updated successfully")
                    Toast.makeText(requireContext(), "Note updated successfully", Toast.LENGTH_SHORT).show()

                    // Set a flag to indicate a successful save
                    Log.d(TAG, "Setting fragment result: noteUpdated=true")
                    parentFragmentManager.setFragmentResult("noteUpdated", Bundle().apply {
                        putBoolean("success", true)
                    })

                    // Navigate back
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating note", e)
                    Toast.makeText(requireContext(), "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}