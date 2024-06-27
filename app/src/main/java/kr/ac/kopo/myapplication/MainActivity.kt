package kr.ac.kopo.myapplication

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var editText: EditText
    private lateinit var addButton: Button
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerPriority: Spinner
    private lateinit var dateButton: Button
    private lateinit var searchView: SearchView
    private val todoList = mutableListOf<TodoItem>()
    private lateinit var adapter: TodoAdapter
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        editText = findViewById(R.id.editText)
        addButton = findViewById(R.id.addButton)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        dateButton = findViewById(R.id.dateButton)
        searchView = findViewById(R.id.searchView)

        adapter = TodoAdapter(this, todoList)
        listView.adapter = adapter

        setupSpinners()
        setupDatePicker()
        setupAddButton()
        setupListViewItemClick()
        setupSearchView()
        createNotificationChannel()
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(
            this,
            R.array.categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.priorities,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPriority.adapter = adapter
        }
    }

    private fun setupDatePicker() {
        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                updateDateButtonText()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun updateDateButtonText() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateButton.text = dateFormat.format(calendar.time)
    }

    private fun setupAddButton() {
        addButton.setOnClickListener {
            val task = editText.text.toString()
            if (task.isNotEmpty()) {
                val category = spinnerCategory.selectedItem.toString()
                val priority = spinnerPriority.selectedItem.toString()
                val dueDate = calendar.time
                val todoItem = TodoItem(task, category, priority, dueDate)
                todoList.add(todoItem)
                adapter.notifyDataSetChanged()
                editText.text.clear()
                scheduleNotification(todoItem)
            }
        }
    }

    private fun setupListViewItemClick() {
        listView.setOnItemClickListener { _, _, position, _ ->
            val completedItem = todoList.removeAt(position)
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Task completed: ${completedItem.task}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return true
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Todo Notifications"
            val descriptionText = "Channel for Todo notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("TODO_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleNotification(todoItem: TodoItem) {
        val builder = NotificationCompat.Builder(this, "TODO_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Todo Reminder")
            .setContentText("Don't forget: ${todoItem.task}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(todoItem.hashCode(), builder.build())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                recreate()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

data class TodoItem(
    val task: String,
    val category: String,
    val priority: String,
    val dueDate: Date
)

class TodoAdapter(context: Context, private val items: List<TodoItem>) : ArrayAdapter<TodoItem>(context, 0, items), Filterable {
    private val allItems = ArrayList(items)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.todo_item, parent, false)
        val item = getItem(position)
        view.findViewById<TextView>(R.id.taskTextView).text = item?.task
        view.findViewById<TextView>(R.id.categoryTextView).text = item?.category
        view.findViewById<TextView>(R.id.priorityTextView).text = item?.priority
        view.findViewById<TextView>(R.id.dueDateTextView).text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(item?.dueDate)
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList = if (constraint.isNullOrEmpty()) {
                    allItems
                } else {
                    allItems.filter { it.task.contains(constraint, ignoreCase = true) }
                }
                return FilterResults().apply { values = filteredList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                addAll(results?.values as? List<TodoItem> ?: emptyList())
                notifyDataSetChanged()
            }
        }
    }
}