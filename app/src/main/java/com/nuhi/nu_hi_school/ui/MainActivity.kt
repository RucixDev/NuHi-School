package com.nuhi.nu_hi_school.ui

import android.app.Activity
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.*
import com.nuhi.nu_hi_school.R
import com.nuhi.nu_hi_school.data.model.Note
import com.nuhi.nu_hi_school.data.repository.NoteRepository
import com.nuhi.nu_hi_school.databinding.ActivityMainBinding
import com.nuhi.nu_hi_school.ui.canvas.CanvasActivity
import com.nuhi.nu_hi_school.ui.math.MathActivity
import com.nuhi.nu_hi_school.ui.settings.SettingsActivity
import com.nuhi.nu_hi_school.ui.translation.TranslationActivity
import com.nuhi.nu_hi_school.ui.training.TrainingActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val notes = mutableListOf<Note>()
    private lateinit var adapter: NotesAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var noteRepository: NoteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteRepository = NoteRepository(this)
        setupToolbar()
        setupNotesList()
        setupFab()
        setupSchoolFeatures()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "NuHi School"
    }

    private fun setupNotesList() {
        adapter = NotesAdapter(notes, this, { note -> openCanvas(note) }, { note -> deleteNote(note) })
        binding.notesList.layoutManager = LinearLayoutManager(this)
        binding.notesList.adapter = adapter
    }

    private fun setupFab() {
        binding.fabNewNote.setOnClickListener {
            val note = Note(
                id = System.currentTimeMillis(),
                title = "Untitled",
                contentPath = "",
                createdAt = System.currentTimeMillis(),
                backgroundColor = "#FFFFFF"
            )
            notes.add(0, note)
            adapter.notifyItemInserted(0)
            openCanvas(note)
        }
    }

    private fun setupSchoolFeatures() {
        binding.cardMath.setOnClickListener {
            startActivity(Intent(this, MathActivity::class.java))
        }
        binding.cardTranslation.setOnClickListener {
            startActivity(Intent(this, TranslationActivity::class.java))
        }
        binding.cardTraining.setOnClickListener {
            startActivity(Intent(this, TrainingActivity::class.java))
        }
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        // Subject cards
        binding.cardBiology.setOnClickListener {
            openSubjectCanvas("Biology", "#4CAF50")
        }
        binding.cardPhysics.setOnClickListener {
            openSubjectCanvas("Physics", "#9C27B0")
        }
        binding.cardChemistry.setOnClickListener {
            openSubjectCanvas("Chemistry", "#FF5722")
        }
        binding.cardHistory.setOnClickListener {
            openSubjectCanvas("History", "#795548")
        }
    }

    private fun openSubjectCanvas(subject: String, color: String) {
        val note = Note(
            id = System.currentTimeMillis(),
            title = subject,
            contentPath = "",
            createdAt = System.currentTimeMillis(),
            backgroundColor = "#FFFFFF"
        )
        notes.add(0, note)
        adapter.notifyItemInserted(0)
        openCanvas(note)
    }

    private fun loadNotes() {
        scope.launch {
            val loaded = noteRepository.getAllNotes()
            notes.clear()
            notes.addAll(loaded)
            adapter.notifyDataSetChanged()
        }
    }

    private fun openCanvas(note: Note) {
        val intent = Intent(this, CanvasActivity::class.java).apply {
            putExtra("note_id", note.id)
        }
        startActivityForResult(intent, REQUEST_CANVAS)
    }

    private fun deleteNote(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Delete \"${note.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                scope.launch {
                    noteRepository.deleteNote(note.id)
                    notes.remove(note)
                    adapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CANVAS) {
            loadNotes()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    class NotesAdapter(
        private val notes: List<Note>,
        private val activity: Activity,
        private val onNoteClick: (Note) -> Unit,
        private val onNoteDelete: (Note) -> Unit
    ) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.note_title)
            val preview: TextView = view.findViewById(R.id.note_preview)
            val card: CardView = view.findViewById(R.id.note_card)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val note = notes[position]
            holder.title.text = note.title
            val date = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(note.updatedAt))
            holder.preview.text = "Last edited: $date"
            holder.card.setOnClickListener { onNoteClick(note) }
            holder.card.setOnLongClickListener {
                onNoteDelete(note)
                true
            }
        }

        override fun getItemCount() = notes.size
    }

    companion object {
        private const val REQUEST_CANVAS = 1
    }
}
