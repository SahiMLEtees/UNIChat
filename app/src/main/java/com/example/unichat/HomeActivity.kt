package com.example.unichat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.unichat.data.database.AppDatabase
import com.example.unichat.data.entities.Contact
import com.example.unichat.ui.theme.UNIChatTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private val firestore by lazy { FirebaseFirestore.getInstance() }

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
                    firestore = firestore,
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
    firestore: FirebaseFirestore,
    contactList: List<Contact>,
    onContactAdded: (Contact) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()
    var showGdprPopup by remember { mutableStateOf(true) } // State to control GDPR popup visibility

    if (showGdprPopup) {
        GDPRConsentPopup(onAccept = { showGdprPopup = false })
    } else {
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
                    onTabSelected = { index ->
                        selectedTab = index
                        when (index) {
                            0 -> navController.navigate("chats") { popUpTo(navController.graph.startDestinationId) }
                            1 -> navController.navigate("addContact") { popUpTo(navController.graph.startDestinationId) }
                            2 -> navController.navigate("settings") { popUpTo(navController.graph.startDestinationId) }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = "chats"
                ) {
                    // Chats screen route
                    composable("chats") {
                        ChatsScreen(
                            contactList = contactList,
                            firestore = firestore,
                            onContactClick = { contact ->
                                navController.navigate("chatDetail/${contact.name}/${contact.phoneNumber}")
                            }
                        )
                    }

                    // Add contact screen route
                    composable("addContact") {
                        AddContactScreen(
                            database = database,
                            firestore = firestore,
                            onContactAdded = onContactAdded
                        )
                    }

                    // Settings screen route
                    composable("settings") {
                        SettingsScreen()
                    }

                    // Chat detail screen route
                    composable(
                        route = "chatDetail/{name}/{phoneNumber}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType },
                            navArgument("phoneNumber") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: ""
                        val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
                        ChatDetailScreen(
                            name = name,
                            phoneNumber = phoneNumber,
                            firestore = firestore,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun GDPRConsentPopup(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Privacy Policy & GDPR Consent") },
        text = {
            Column {
                Text(
                    text = """
                        By using UNIChat, you agree to our Privacy Policy and GDPR compliance guidelines.
                        
                        - Your data is secure and encrypted.
                        - You can request data access, correction, or deletion at any time.
                        - We do not share your data with third parties without your consent.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onAccept) {
                Text("Decline")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    name: String,
    phoneNumber: String,
    firestore: FirebaseFirestore,
    navController: NavHostController
) {
    BackgroundImage {
        var messageText by remember { mutableStateOf("") }
        val messages = remember { mutableStateListOf<Pair<String, String>>() }
        val coroutineScope = rememberCoroutineScope()

        // Load messages from Firestore
        LaunchedEffect(Unit) {
            firestore.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firestore", "Error fetching messages", e)
                        return@addSnapshotListener
                    }
                    val newMessages = snapshot?.documents?.mapNotNull { doc ->
                        val sender = doc.getString("sender")
                        val content = doc.getString("content")
                        if (sender != null && content != null) sender to content else null
                    } ?: emptyList()
                    messages.clear()
                    messages.addAll(newMessages)
                }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(name) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Message") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        if (messageText.isNotBlank()) {
                            val message = mapOf(
                                "sender" to "You",
                                "content" to messageText,
                                "timestamp" to System.currentTimeMillis()
                            )
                            firestore.collection("messages").add(message)
                            messageText = ""
                        }
                    }) {
                        Text("Send")
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(messages) { (sender, content) ->
                    Column {
                        Text(
                            text = sender,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun ChatItem(name: String, phoneNumber: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(name: String, phoneNumber: String) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>() } // Temporary in-memory message list

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    if (messageText.isNotBlank()) {
                        messages.add(messageText)
                        messageText = ""
                    }
                }) {
                    Text("Send")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(messages) { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}




@Composable
fun ChatsScreen(
    contactList: List<Contact>,
    firestore: FirebaseFirestore,
    onContactClick: (Contact) -> Unit
) {
    BackgroundImage {
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
                    ChatItem(
                        name = contact.name,
                        phoneNumber = contact.phoneNumber,
                        onClick = { onContactClick(contact) }
                    )
                }
            }
        }
    }
}








@Composable
fun SettingsScreen() {
    val appDescription = """
        UNIChat is a university project chat application designed for real-time communication. 
        It supports text messaging and contact management, providing an intuitive and smooth user experience.
    """.trimIndent()

    val privacyPolicy = """
        Privacy Policy:
        - We respect your privacy and are committed to protecting your personal information.
        - UNIChat does not share any personal data with third parties without your consent.
        - All communication is encrypted, ensuring that your messages are secure.
        - You have the right to request the deletion of your data at any time.
    """.trimIndent()

    val gdprCompliance = """
        GDPR Compliance:
        - You can request access to your data, request corrections, or request data deletion.
        - By using UNIChat, you consent to our data policies.
    """.trimIndent()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "About UNIChat",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = appDescription,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify
            )
        }

        item {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = privacyPolicy,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify
            )
        }

        item {
            Text(
                text = "GDPR Compliance",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = gdprCompliance,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify
            )
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
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact") },
            label = { Text("Add Contact") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
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
fun BackgroundImage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.background), // Replace with your image resource
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        content()
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

    BackgroundImage {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f) // Slight transparency to show background
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
}



