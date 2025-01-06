package com.example.unichat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.unichat.data.database.AppDatabase
import com.example.unichat.data.entities.Contact
import com.example.unichat.ui.theme.UNIChatTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private val firestore by lazy { FirebaseFirestore.getInstance() } // Firestore initialization

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        setContent {
            UNIChatTheme {
                var contactList by remember { mutableStateOf<List<Contact>>(emptyList()) }

                // Load contacts from Room
                LaunchedEffect(Unit) {
                    contactList = database.contactDao().getAllContacts()
                    Log.d("HomeActivity", "Loaded contacts: $contactList")
                }

                HomeScreen(
                    onLogout = {
                        auth.signOut()
                        finish()
                    },
                    database = database,
                    firestore = firestore, // Pass Firestore to the HomeScreen
                    contactList = contactList,
                    onContactAdded = { newContact ->
                        contactList = contactList + newContact
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    database: AppDatabase,
    firestore: FirebaseFirestore, // Firestore parameter added
    contactList: List<Contact>,
    onContactAdded: (Contact) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UNIChat") },
                actions = {
                    IconButton(onClick = { onLogout() }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { index -> selectedTab = index }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ChatsScreen(contactList, firestore) // Pass Firestore to ChatsScreen
                1 -> TranslateScreen()
                2 -> AddContactScreen(database, firestore, onContactAdded) // Pass Firestore to AddContactScreen
                3 -> SettingsScreen()
            }
        }
    }
}


@Composable
fun ChatItem(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click to open chat details */ },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}


@Composable
fun ChatsScreen(contactList: List<Contact>, firestore: FirebaseFirestore) {
    var firestoreContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Load contacts from Firestore
    LaunchedEffect(Unit) {
        firestore.collection("contacts")
            .get()
            .addOnSuccessListener { documents ->
                val contactsFromFirestore = documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    val phoneNumber = doc.getString("phoneNumber")
                    if (name != null && phoneNumber != null) {
                        Contact(name = name, phoneNumber = phoneNumber)
                    } else null
                }
                firestoreContacts = contactsFromFirestore
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching contacts", e)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to fetch contacts from Firestore.")
                }
            }
    }

    // Combine Room and Firestore contacts, avoiding duplicates
    val allContacts = (contactList + firestoreContacts).distinctBy { it.phoneNumber }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(allContacts) { contact ->
                ChatItem(name = contact.name, phoneNumber = contact.phoneNumber)
            }
        }
    }
}





@Composable
fun TranslateScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Translate Screen", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun SettingsScreen() {
    val settingsOptions = listOf(
        "Account" to listOf("Security notifications", "Change number"),
        "Privacy" to listOf("Block contacts", "Disappearing messages"),
        "Notifications" to listOf("Message alerts", "Group alerts")
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        settingsOptions.forEach { (category, options) ->
            // Category Header
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Options under the category
            items(options) { option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle click for specific settings */ },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
            label = { Text("Chats") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Translate, contentDescription = "Translate") },
            label = { Text("Translate") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact") },
            label = { Text("Add Contact") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) }
        )
    }
}

@Composable
fun ContactCard(name: String, phoneNumber: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Contact Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Display the name of the contact
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )

                // Display the phone number
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}






@Composable
fun AddContactScreen(
    database: AppDatabase,
    firestore: FirebaseFirestore,
    onContactAdded: (Contact) -> Unit
) {
    var contactName by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+1") }
    var phoneNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) } // State to control dropdown menu
    val countryCodes = listOf("+1", "+91", "+44", "+61", "+81") // Sample country codes
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Add Contact", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Contact Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .clickable { expanded = true } // Open dropdown on click
                ) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = {},
                        label = { Text("Country Code") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        countryCodes.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(code) },
                                onClick = {
                                    countryCode = code // Update selected country code
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.weight(0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (contactName.isBlank() || phoneNumber.isBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please fill in both fields.")
                        }
                    } else {
                        coroutineScope.launch {
                            val fullPhoneNumber = "$countryCode $phoneNumber"
                            val newContact = Contact(
                                name = contactName,
                                phoneNumber = fullPhoneNumber
                            )
                            // Save to Room database
                            database.contactDao().insertContact(newContact)
                            onContactAdded(newContact)

                            // Save to Firestore
                            firestore.collection("contacts")
                                .add(
                                    mapOf(
                                        "name" to contactName,
                                        "phoneNumber" to fullPhoneNumber
                                    )
                                )
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Contact added to Firestore")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error adding contact", e)
                                }

                            snackbarHostState.showSnackbar("Contact added successfully!")

                            // Reset fields
                            contactName = ""
                            phoneNumber = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Contact")
            }
        }
    }
}



